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
    public MallOrder createOrder(Long memberId, Long addressId, List<MallOrderItem> items, String remark, String paymentMethod, int pointsToUse)
    {
        MallAddress address = addressMapper.selectAddressById(addressId);
        if (address == null || !address.getMemberId().equals(memberId))
        {
            throw new RuntimeException("Invalid address");
        }

        BigDecimal total = BigDecimal.ZERO;
        boolean hasFlashSale = false;
        for (MallOrderItem item : items)
        {
            MallProduct product = productMapper.selectProductById(item.getProductId());
            if (product == null || !"0".equals(product.getStatus()))
            {
                throw new RuntimeException("Product not available: " + item.getProductId());
            }
            item.setProductName(product.getName());
            item.setProductImage(product.getImageUrl());

            // 检查限时优惠，有则使用活动价并占库存
            BigDecimal unitPrice = product.getPrice();
            com.ruoyi.mall.domain.MallFlashSale flashSale =
                    flashSaleService.selectActiveByProductId(product.getProductId());
            if (flashSale != null)
            {
                boolean occupied = flashSaleService.occupyStock(flashSale.getSaleId(), item.getQuantity());
                if (!occupied)
                {
                    throw new RuntimeException("Flash sale stock sold out: " + product.getName());
                }
                unitPrice = flashSale.getFlashPrice();
                hasFlashSale = true;
            }

            item.setPrice(unitPrice);
            item.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
            total = total.add(item.getSubtotal());

            // 原子扣库存：WHERE stock >= quantity，返回 0 表示库存不足
            int affected = productMapper.deductStock(product.getProductId(), item.getQuantity());
            if (affected == 0)
            {
                throw new RuntimeException("Insufficient stock: " + product.getName());
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

        MallOrder order = new MallOrder();
        order.setOrderNo(generateOrderNo());
        order.setMemberId(memberId);
        order.setAddressSnapshot(buildAddressSnapshot(address));
        order.setTotalAmount(total);
        order.setStatus("0");
        order.setPaymentMethod(paymentMethod != null ? paymentMethod.toUpperCase() : "COD");
        order.setRemark(remark != null ? remark : "");
        order.setOrderSource(hasFlashSale ? "FLASH_SALE" : "NORMAL");
        order.setPointsUsed(actualPointsUsed);
        order.setCreateTime(new Date());
        orderMapper.insertOrder(order);

        // 扣积分（下单后再扣，orderNo 已生成）
        if (actualPointsUsed > 0)
        {
            pointsService.deduct(memberId, actualPointsUsed, order.getOrderNo());
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
            fcmService.sendToTopic("runners", "New Order Available",
                    "Tap to accept a delivery",
                    java.util.Collections.singletonMap("orderId", String.valueOf(order.getOrderId())));
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
        MallOrder order = new MallOrder();
        order.setOrderId(orderId);
        order.setStatus(status);
        order.setUpdateBy(updateBy);
        order.setUpdateTime(new Date());
        return orderMapper.updateOrder(order);
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
        MallOrder order = orderMapper.selectOrderByOrderNo(orderNo);
        if (order == null)
        {
            throw new RuntimeException("Order not found: " + orderNo);
        }
        if ("PAID".equals(order.getPaymentStatus()))
        {
            return; // 幂等：已支付则忽略重复回调
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
