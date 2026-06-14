package com.ruoyi.mall.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.mall.domain.MallAddress;
import com.ruoyi.mall.domain.MallOrder;
import com.ruoyi.mall.domain.MallOrderItem;
import com.ruoyi.mall.domain.MallProduct;
import com.ruoyi.mall.domain.MallPaymentRecord;
import com.ruoyi.mall.mapper.MallAddressMapper;
import com.ruoyi.mall.mapper.MallFlashSaleMapper;
import com.ruoyi.mall.mapper.MallOrderItemMapper;
import com.ruoyi.mall.mapper.MallOrderMapper;
import com.ruoyi.mall.mapper.MallPaymentRecordMapper;
import com.ruoyi.mall.mapper.MallProductMapper;
import com.ruoyi.mall.service.FcmService;
import com.ruoyi.mall.domain.CouponDiscountResult;
import com.ruoyi.mall.service.IMallCouponService;
import com.ruoyi.mall.service.IMallFlashSaleService;
import com.ruoyi.mall.service.IMallOrderService;
import com.ruoyi.mall.service.IMallPointsService;
import com.ruoyi.mall.service.ISsePushService;
import com.ruoyi.mall.mapper.MallMemberMapper;

@Service
public class MallOrderServiceImpl implements IMallOrderService
{
    @Autowired
    private MallOrderMapper orderMapper;
    @Autowired
    private MallOrderItemMapper orderItemMapper;
    @Autowired
    private MallProductMapper productMapper;
    @Autowired
    private MallAddressMapper addressMapper;
    @Autowired
    private IMallFlashSaleService flashSaleService;
    @Autowired
    private MallPaymentRecordMapper paymentRecordMapper;
    @Autowired
    private FcmService fcmService;
    @Autowired
    private ISsePushService sseService;
    @Autowired
    private MallMemberMapper memberMapper;
    @Autowired
    private IMallPointsService pointsService;
    @Autowired
    private IMallCouponService couponService;

    @Override
    public List<MallOrder> selectOrderList(MallOrder order)
    {
        List<MallOrder> list = orderMapper.selectOrderList(order);
        if (list.isEmpty()) return list;
        // 批量拉取 items，1 次查询替代 N 次
        List<Long> orderIds = list.stream().map(MallOrder::getOrderId).collect(Collectors.toList());
        List<MallOrderItem> allItems = orderItemMapper.selectItemsByOrderIds(orderIds);
        Map<Long, List<MallOrderItem>> itemsMap = allItems.stream()
                .collect(Collectors.groupingBy(MallOrderItem::getOrderId));
        list.forEach(o -> o.setItems(itemsMap.getOrDefault(o.getOrderId(), new ArrayList<>())));
        return list;
    }

    @Override
    public List<MallOrder> selectOrderListByMemberId(Long memberId)
    {
        MallOrder query = new MallOrder();
        query.setMemberId(memberId);
        return orderMapper.selectOrderList(query);
    }

    @Override
    public MallOrder selectOrderById(Long orderId)
    {
        MallOrder order = orderMapper.selectOrderById(orderId);
        if (order != null)
        {
            order.setItems(orderItemMapper.selectItemsByOrderId(orderId));
        }
        return order;
    }

    @Override
    public MallOrder selectOrderByOrderNo(String orderNo)
    {
        return orderMapper.selectOrderByOrderNo(orderNo);
    }

    @Override
    @Transactional
    public MallOrder createOrder(Long memberId, Long addressId, List<MallOrderItem> items, String remark, String paymentMethod, int pointsToUse, Long memberCouponId)
    {
        boolean isStore = "STORE".equalsIgnoreCase(paymentMethod);

        // 到店单（STORE）无需配送地址；其余支付方式必须有有效地址
        MallAddress address = null;
        if (addressId != null)
        {
            address = addressMapper.selectAddressById(addressId);
            if (address == null || !address.getMemberId().equals(memberId))
            {
                throw new RuntimeException("Invalid address");
            }
        }
        else if (!isStore)
        {
            throw new RuntimeException("Invalid address");
        }

        // Pass 1: validate all items and compute prices — no stock changes yet
        Map<Long, com.ruoyi.mall.domain.MallFlashSale> flashSaleMap = new java.util.HashMap<>();
        for (MallOrderItem item : items)
        {
            MallProduct product = productMapper.selectProductById(item.getProductId());
            if (product == null || !"0".equals(product.getStatus()))
            {
                throw new RuntimeException("Product not available: " + item.getProductId());
            }
            if (product.getStock() < item.getQuantity())
            {
                throw new RuntimeException("Insufficient stock: " + product.getName());
            }
            item.setProductName(product.getName());
            item.setProductImage(product.getImageUrl());

            com.ruoyi.mall.domain.MallFlashSale flashSale =
                    flashSaleService.selectActiveByProductId(product.getProductId());
            BigDecimal unitPrice = product.getPrice();
            if (flashSale != null)
            {
                int remaining = flashSale.getStockLimit() - flashSale.getSoldCount();
                if (remaining < item.getQuantity())
                {
                    throw new RuntimeException("Flash sale stock sold out: " + product.getName());
                }
                unitPrice = flashSale.getFlashPrice();
                flashSaleMap.put(item.getProductId(), flashSale);
            }
            item.setPrice(unitPrice);
            item.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        BigDecimal total = items.stream()
                .map(MallOrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean hasFlashSale = !flashSaleMap.isEmpty();

        // Pass 2: deduct all stocks atomically within the transaction
        for (MallOrderItem item : items)
        {
            com.ruoyi.mall.domain.MallFlashSale flashSale = flashSaleMap.get(item.getProductId());
            if (flashSale != null)
            {
                boolean occupied = flashSaleService.occupyStock(flashSale.getSaleId(), item.getQuantity());
                if (!occupied)
                {
                    throw new RuntimeException("Flash sale stock sold out: " + item.getProductName());
                }
            }
            int affected = productMapper.deductStock(item.getProductId(), item.getQuantity());
            if (affected == 0)
            {
                throw new RuntimeException("Insufficient stock: " + item.getProductName());
            }
        }

        // 积分抵扣：100分=₱10，先验后扣，防止超扣
        int actualPointsUsed = 0;
        if (pointsToUse > 0)
        {
            int balance = pointsService.getBalance(memberId);
            int canUse  = Math.min(pointsToUse, balance);
            // 每100分抵₱10，取整百
            canUse = (canUse / 100) * 100;
            if (canUse > 0)
            {
                BigDecimal discount = BigDecimal.valueOf(canUse / 10L);
                total = total.subtract(discount).max(BigDecimal.ZERO);
                actualPointsUsed = canUse;
            }
        }

        // 优惠券抵扣（在积分之后、建单之前校验）
        BigDecimal couponDiscountAmt = BigDecimal.ZERO;
        boolean    couponFreeDelivery = false;
        if (memberCouponId != null)
        {
            boolean isFirstOrder = (orderMapper.countCompletedByMemberId(memberId) == 0);
            BigDecimal subtotalForCoupon = total; // total 此时已减积分，用原始 subtotal 更合适
            CouponDiscountResult cr = couponService.validate(
                    memberCouponId, memberId, subtotalForCoupon, BigDecimal.ZERO, isFirstOrder);
            if (!cr.isValid())
            {
                throw new RuntimeException(cr.getErrorMessage());
            }
            couponDiscountAmt  = cr.getDiscountAmount();
            couponFreeDelivery = cr.isFreeDelivery();
            total = total.subtract(couponDiscountAmt).max(BigDecimal.ZERO);
        }

        MallOrder order = new MallOrder();
        order.setOrderNo(generateOrderNo());
        order.setMemberId(memberId);
        order.setAddressSnapshot(address != null ? buildAddressSnapshot(address) : "");
        order.setTotalAmount(total);
        order.setStatus("0");
        order.setPaymentMethod(paymentMethod != null ? paymentMethod.toUpperCase() : "COD");
        order.setRemark(remark != null ? remark : "");
        // 到店单标识：order_source = IN_STORE，便于后台区分配送单/到店单
        order.setOrderSource(isStore ? "IN_STORE" : (hasFlashSale ? "FLASH_SALE" : "NORMAL"));
        order.setPointsUsed(actualPointsUsed);
        order.setMemberCouponId(memberCouponId);
        order.setCouponDiscount(couponDiscountAmt);
        order.setCreateTime(new Date());
        orderMapper.insertOrder(order);

        // 扣积分（下单后再扣，orderNo 已生成）
        if (actualPointsUsed > 0)
        {
            pointsService.deduct(memberId, actualPointsUsed, order.getOrderNo());
        }

        // 标记优惠券已使用
        if (memberCouponId != null)
        {
            couponService.markUsed(memberCouponId, memberId, order.getOrderNo());
        }

        // 推荐奖励：被邀请人首单下单时，给邀请人 200 积分
        try
        {
            int totalOrders = orderMapper.countAllByMemberId(memberId);
            if (totalOrders == 1)
            {
                com.ruoyi.mall.domain.MallMember memberInfo = memberMapper.selectMemberById(memberId);
                if (memberInfo != null && memberInfo.getReferrerId() != null)
                {
                    pointsService.earn(memberInfo.getReferrerId(), 200, 6, order.getOrderNo(),
                            "Referral reward – your invitee placed their first order");
                }
            }
        }
        catch (Exception e)
        {
            org.slf4j.LoggerFactory.getLogger(getClass())
                    .warn("Referral reward failed for member {}: {}", memberId, e.getMessage());
        }

        for (MallOrderItem item : items)
        {
            item.setOrderId(order.getOrderId());
        }
        orderItemMapper.insertOrderItemBatch(items);

        order.setItems(items);

        // 推送：SSE（Web）+ FCM（Mobile）
        try
        {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("orderId", order.getOrderId());
            payload.put("orderNo", order.getOrderNo());
            payload.put("status", "0");
            payload.put("title", "Order Placed");
            payload.put("body",  "Your order #" + order.getOrderNo() + " is being processed");
            sseService.push(memberId, "order_status", payload);

            com.ruoyi.mall.domain.MallMember member = memberMapper.selectMemberById(memberId);
            if (member != null && member.getFcmToken() != null)
            {
                fcmService.sendToToken(member.getFcmToken(), "Order Placed",
                        "Your order #" + order.getOrderNo() + " is being processed",
                        java.util.Collections.singletonMap("orderId", String.valueOf(order.getOrderId())));
            }
            // 到店单不走配送，不推送 runner
            if (!isStore)
            {
                fcmService.sendToTopic("runners", "New Order Available",
                        "Tap to accept a delivery",
                        java.util.Collections.singletonMap("orderId", String.valueOf(order.getOrderId())));
            }
        }
        catch (Exception e)
        {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Push failed after createOrder: {}", e.getMessage());
        }

        return order;
    }

    @Override
    public int updateOrderStatus(Long orderId, String status, String updateBy)
    {
        MallOrder existing = orderMapper.selectOrderById(orderId);
        if (existing == null) return 0;

        MallOrder update = new MallOrder();
        update.setOrderId(orderId);
        update.setStatus(status);
        update.setUpdateBy(updateBy);
        update.setUpdateTime(new Date());
        int rows = orderMapper.updateOrder(update);

        if (rows > 0)
        {
            try
            {
                String[] tb = statusTitleBody(status, existing.getOrderNo());
                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("orderId", orderId);
                payload.put("orderNo", existing.getOrderNo());
                payload.put("status",  status);
                payload.put("title",   tb[0]);
                payload.put("body",    tb[1]);
                sseService.push(existing.getMemberId(), "order_status", payload);

                com.ruoyi.mall.domain.MallMember member = memberMapper.selectMemberById(existing.getMemberId());
                if (member != null && member.getFcmToken() != null)
                {
                    fcmService.sendToToken(member.getFcmToken(), tb[0], tb[1],
                            java.util.Collections.singletonMap("orderId", String.valueOf(orderId)));
                }
            }
            catch (Exception e)
            {
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("Push failed after updateOrderStatus: {}", e.getMessage());
            }
        }
        return rows;
    }

    private String[] statusTitleBody(String status, String orderNo)
    {
        switch (status)
        {
            case "1": return new String[]{"Order Confirmed",   "Your order #" + orderNo + " has been confirmed"};
            case "2": return new String[]{"Runner On the Way", "Your order is being delivered"};
            case "3": return new String[]{"Order Delivered",   "Your order #" + orderNo + " has been delivered. Enjoy!"};
            case "4": return new String[]{"Order Cancelled",   "Your order #" + orderNo + " has been cancelled"};
            default:  return new String[]{"Order Updated",     "Your order #" + orderNo + " status has been updated"};
        }
    }

    @Override
    @Transactional
    public int cancelOrder(Long orderId, Long memberId, String reason)
    {
        MallOrder order = orderMapper.selectOrderById(orderId);
        if (order == null || !order.getMemberId().equals(memberId))
        {
            throw new RuntimeException("Order not found");
        }
        if (!"0".equals(order.getStatus()))
        {
            throw new RuntimeException("Only pending orders can be cancelled");
        }
        order.setStatus("4");
        order.setCancelReason(reason != null ? reason : "");
        order.setUpdateTime(new Date());
        int rows = orderMapper.updateOrder(order);

        // 推送：SSE（Web）+ FCM（Mobile）
        try
        {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("orderId", orderId);
            payload.put("orderNo", order.getOrderNo());
            payload.put("status", "4");
            payload.put("title", "Order Cancelled");
            payload.put("body",  "Your order #" + order.getOrderNo() + " has been cancelled");
            sseService.push(memberId, "order_status", payload);

            com.ruoyi.mall.domain.MallMember member = memberMapper.selectMemberById(memberId);
            if (member != null && member.getFcmToken() != null)
            {
                fcmService.sendToToken(member.getFcmToken(), "Order Cancelled",
                        "Your order #" + order.getOrderNo() + " has been cancelled",
                        java.util.Collections.singletonMap("orderId", String.valueOf(orderId)));
            }
        }
        catch (Exception e)
        {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Push failed after cancelOrder: {}", e.getMessage());
        }

        return rows;
    }

    @Override
    @Transactional
    public void markOrderPaid(String orderNo, String paymentNo, BigDecimal amount)
    {
        MallOrder order = orderMapper.selectOrderByOrderNoForUpdate(orderNo);
        if (order == null)
        {
            throw new RuntimeException("Order not found: " + orderNo);
        }
        if ("PAID".equals(order.getPaymentStatus()))
        {
            return; // idempotent: ignore duplicate callback
        }
        if ("4".equals(order.getStatus()))
        {
            throw new RuntimeException("Order is cancelled, cannot mark as paid: " + orderNo);
        }
        order.setPaymentStatus("PAID");
        order.setPaymentNo(paymentNo);
        order.setPaidAmount(amount);
        order.setPaymentTime(new Date());
        order.setStatus("1"); // 支付成功自动已确认
        order.setUpdateTime(new Date());
        orderMapper.updateOrder(order);

        // 更新支付流水状态
        MallPaymentRecord record = paymentRecordMapper.selectByOrderId(order.getOrderId());
        if (record != null)
        {
            paymentRecordMapper.updateStatus(record.getRecordId(), "SUCCESS", null);
        }

        // 推送：SSE（Web）+ FCM（Mobile）
        try
        {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("orderId", order.getOrderId());
            payload.put("orderNo", order.getOrderNo());
            payload.put("status", "1");
            payload.put("title", "Payment Confirmed");
            payload.put("body",  "Looking for a runner for your order");
            sseService.push(order.getMemberId(), "order_status", payload);

            com.ruoyi.mall.domain.MallMember member = memberMapper.selectMemberById(order.getMemberId());
            if (member != null && member.getFcmToken() != null)
            {
                fcmService.sendToToken(member.getFcmToken(), "Payment Confirmed",
                        "Looking for a runner for your order",
                        java.util.Collections.singletonMap("orderId", String.valueOf(order.getOrderId())));
            }
            fcmService.sendToTopic("runners", "New Order Available",
                    "Tap to accept a delivery",
                    java.util.Collections.singletonMap("orderId", String.valueOf(order.getOrderId())));
        }
        catch (Exception e)
        {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Push failed after markOrderPaid: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void awardOrderCompletionRewards(MallOrder order)
    {
        if (order == null) return;

        // 积分奖励：1分/₱1（按实付金额取整）
        try
        {
            int pts = order.getTotalAmount() != null ? order.getTotalAmount().intValue() : 0;
            if (pts > 0)
            {
                pointsService.earn(order.getMemberId(), pts, 1,
                        order.getOrderNo(), "Order #" + order.getOrderNo() + " completed");
            }
        }
        catch (Exception e)
        {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Points earn failed on order completion: {}", e.getMessage());
        }

        // 邀请返积分：被邀请人完成第一笔订单时，给邀请人 200 积分
        try
        {
            com.ruoyi.mall.domain.MallMember customer = memberMapper.selectMemberById(order.getMemberId());
            if (customer != null && customer.getReferrerId() != null)
            {
                int completedCount = orderMapper.countCompletedByMemberId(order.getMemberId());
                if (completedCount == 1)
                {
                    pointsService.earn(customer.getReferrerId(), 200, 5,
                            String.valueOf(order.getMemberId()),
                            "Referral reward: new member completed first order");
                }
            }
        }
        catch (Exception e)
        {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Referral reward failed on order completion: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public int confirmInStorePayment(Long orderId, String operator)
    {
        MallOrder order = orderMapper.selectOrderById(orderId);
        if (order == null)
        {
            throw new RuntimeException("Order not found");
        }
        if (!"IN_STORE".equals(order.getOrderSource()) || !"STORE".equalsIgnoreCase(order.getPaymentMethod()))
        {
            throw new RuntimeException("Not an in-store order");
        }
        if (!"0".equals(order.getStatus()))
        {
            // 幂等保护：非待确认状态不重复确认/发奖励
            throw new RuntimeException("Order is not awaiting payment confirmation");
        }

        Date now = new Date();
        MallOrder update = new MallOrder();
        update.setOrderId(orderId);
        update.setStatus("3");
        update.setPaymentStatus("PAID");
        update.setPaidAmount(order.getTotalAmount());
        update.setPaymentTime(now);
        update.setUpdateBy(operator);
        update.setUpdateTime(now);
        int rows = orderMapper.updateOrder(update);

        // 发放完成奖励（与配送送达一致）
        awardOrderCompletionRewards(order);

        // 推送顾客：订单完成
        try
        {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("orderId", orderId);
            payload.put("orderNo", order.getOrderNo());
            payload.put("status", "3");
            payload.put("title", "Order Completed");
            payload.put("body",  "Your in-store order #" + order.getOrderNo() + " is complete. Thank you!");
            sseService.push(order.getMemberId(), "order_status", payload);

            com.ruoyi.mall.domain.MallMember member = memberMapper.selectMemberById(order.getMemberId());
            if (member != null && member.getFcmToken() != null)
            {
                fcmService.sendToToken(member.getFcmToken(), "Order Completed",
                        "Your in-store order #" + order.getOrderNo() + " is complete. Thank you!",
                        java.util.Collections.singletonMap("orderId", String.valueOf(orderId)));
            }
        }
        catch (Exception e)
        {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Push failed after confirmInStorePayment: {}", e.getMessage());
        }

        return rows;
    }

    private String generateOrderNo()
    {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "DD" + timestamp + rand;
    }

    private String buildAddressSnapshot(MallAddress address)
    {
        return String.format("{\"addressId\":%d,\"label\":\"%s\",\"fullAddress\":\"%s\"}",
                address.getAddressId(),
                escapeJson(address.getLabel()),
                escapeJson(address.getFullAddress()));
    }

    private String escapeJson(String s)
    {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
