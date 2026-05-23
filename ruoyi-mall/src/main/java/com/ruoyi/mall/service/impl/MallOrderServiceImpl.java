package com.ruoyi.mall.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.mall.domain.MallAddress;
import com.ruoyi.mall.domain.MallOrder;
import com.ruoyi.mall.domain.MallOrderItem;
import com.ruoyi.mall.domain.MallProduct;
import java.math.BigDecimal;
import com.ruoyi.mall.domain.MallPaymentRecord;
import com.ruoyi.mall.mapper.MallAddressMapper;
import com.ruoyi.mall.mapper.MallFlashSaleMapper;
import com.ruoyi.mall.mapper.MallOrderItemMapper;
import com.ruoyi.mall.mapper.MallOrderMapper;
import com.ruoyi.mall.mapper.MallPaymentRecordMapper;
import com.ruoyi.mall.mapper.MallProductMapper;
import com.ruoyi.mall.service.IMallFlashSaleService;
import com.ruoyi.mall.service.IMallOrderService;

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

    @Override
    public List<MallOrder> selectOrderList(MallOrder order)
    {
        List<MallOrder> list = orderMapper.selectOrderList(order);
        for (MallOrder o : list)
        {
            o.setItems(orderItemMapper.selectItemsByOrderId(o.getOrderId()));
        }
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
    public MallOrder createOrder(Long memberId, Long addressId, List<MallOrderItem> items, String remark, String paymentMethod)
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
            if (product.getStock() < item.getQuantity())
            {
                throw new RuntimeException("Insufficient stock: " + product.getName());
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

            // 扣减库存
            product.setStock(product.getStock() - item.getQuantity());
            productMapper.updateProduct(product);
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
        order.setCreateTime(new Date());
        orderMapper.insertOrder(order);

        for (MallOrderItem item : items)
        {
            item.setOrderId(order.getOrderId());
        }
        orderItemMapper.insertOrderItemBatch(items);

        order.setItems(items);
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
        return orderMapper.updateOrder(order);
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
                address.getAddressId(), address.getLabel(), address.getFullAddress());
    }
}
