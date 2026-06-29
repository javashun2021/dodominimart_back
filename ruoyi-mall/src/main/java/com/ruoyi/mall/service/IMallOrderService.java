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
     * @param addressId      配送地址 ID；到店单（STORE）传 null，无需配送地址
     * @param paymentMethod  "COD" / "GCASH" / "STORE"（到店线下支付），null 时默认 COD
     * @param pointsToUse    本单使用积分数（0 = 不使用），100分=₱10
     * @param memberCouponId 本单使用的优惠券实例 ID，null = 不使用
     */
    MallOrder createOrder(Long memberId, Long addressId, List<MallOrderItem> items, String remark, String paymentMethod, int pointsToUse, Long memberCouponId, java.math.BigDecimal deliveryFee);

    /** 变更订单状态（Admin 使用） */
    int updateOrderStatus(Long orderId, String status, String updateBy);

    /** 会员取消订单（仅 PENDING 状态可取消） */
    int cancelOrder(Long orderId, Long memberId, String reason);

    /** 标记订单支付成功（GCash 回调时调用） */
    void markOrderPaid(String orderNo, String paymentNo, BigDecimal amount);

    /**
     * 后台整单退款：线上(PayMongo)走 API 真退、现金单(COD/到店)仅标记；
     * 同时回滚库存 + 退还下单时抵扣的积分，并置订单为已退款/已取消。
     * @return 结果提示文案
     */
    String refundOrder(Long orderId, String operator);

    // ── 完成后退款申请（售后）──
    /** 顾客对已完成订单发起退款申请（理由必填，images 可选，逗号分隔 URL） */
    com.ruoyi.mall.domain.MallRefundRequest createRefundRequest(Long orderId, Long memberId, String reason, String images);

    /** 取某订单最新一条退款申请（app 详情展示 / 防重复） */
    com.ruoyi.mall.domain.MallRefundRequest getLatestRefundRequest(Long orderId);

    /** 后台退款申请列表 */
    java.util.List<com.ruoyi.mall.domain.MallRefundRequest> listRefundRequests(com.ruoyi.mall.domain.MallRefundRequest query);

    /** 后台通过退款申请 → 触发真退款 */
    void approveRefundRequest(Long requestId, String operator);

    /** 后台驳回退款申请（remark 给顾客看） */
    void rejectRefundRequest(Long requestId, String operator, String remark);

    /**
     * 发放订单完成奖励：顾客 1分/₱1 + 被邀请人首单完成给邀请人 200 积分。
     * 供 runner 送达完成 与 到店单确认收款 两条路径复用。
     */
    void awardOrderCompletionRewards(MallOrder order);

    /**
     * 店员确认到店单收款 → 订单直接完成并发奖励（仅 IN_STORE 且 status=0 时有效）。
     * @return 受影响行数，1=成功
     */
    int confirmInStorePayment(Long orderId, String operator);

    /**
     * POS：写入到店实付方式/收现/找零/收银员（在 confirmInStorePayment 之前调用）。
     * @return 受影响行数
     */
    int applyPosTender(Long orderId, String tenderType, BigDecimal cashReceived, BigDecimal changeDue, Long cashierId);

    /**
     * POS 收银开单 + 收款（单事务，原子）：
     * createOrder(STORE 到店分支) → 校验收现≥应付 → 写 tender/找零 → confirmInStorePayment 直接完成。
     * 收现不足会抛异常并整体回滚（含库存扣减）。
     * @param ownerId      下单归属会员（散客传 walk-in 账号）
     * @param cashierId    开单收银员 memberId
     * @param tenderType   实付方式（阶段1 CASH）
     * @param cashReceived 收现金额
     * @return 已完成的订单（含 tender/找零/明细）
     */
    MallOrder createPosOrder(Long ownerId, List<MallOrderItem> items, Long cashierId,
            String tenderType, BigDecimal cashReceived, int pointsToUse, Long memberCouponId);
}
