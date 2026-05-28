package com.ruoyi.mall.service;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.mall.domain.MallOrder;
import com.ruoyi.mall.domain.MallOrderItem;

public interface IMallOrderService
{
    List<MallOrder> selectOrderList(MallOrder order);

    List<MallOrder> selectOrderListByMemberId(Long memberId);

    /** 查询订单详情（含明细） */
    MallOrder selectOrderById(Long orderId);

    MallOrder selectOrderByOrderNo(String orderNo);

    /**
     * 创建订单（事务）：校验库存 → 生成订单 → 插入明细 → 扣减库存
     * @param paymentMethod "COD" 或 "GCASH"，null 时默认 COD
     * @param pointsToUse   本单使用积分数（0 = 不使用），100分=₱10
     */
    MallOrder createOrder(Long memberId, Long addressId, List<MallOrderItem> items, String remark, String paymentMethod, int pointsToUse);

    /** 变更订单状态（Admin 使用） */
    int updateOrderStatus(Long orderId, String status, String updateBy);

    /** 会员取消订单（仅 PENDING 状态可取消） */
    int cancelOrder(Long orderId, Long memberId, String reason);

    /** 标记订单支付成功（GCash 回调时调用） */
    void markOrderPaid(String orderNo, String paymentNo, BigDecimal amount);
}
