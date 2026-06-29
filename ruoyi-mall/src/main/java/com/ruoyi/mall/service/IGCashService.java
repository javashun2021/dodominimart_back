package com.ruoyi.mall.service;

import java.math.BigDecimal;
import java.util.Map;

public interface IGCashService
{
    /**
     * 创建支付请求，返回 GCash 支付 URL
     * @param orderId   订单ID
     * @param amount    支付金额
     * @param orderNo   订单号（用作 GCash referenceId）
     * @return GCash 支付页面 URL
     */
    String createPayment(Long orderId, BigDecimal amount, String orderNo);

    /**
     * 验证 GCash Webhook 回调签名
     */
    boolean verifyCallback(Map<String, String> headers, String rawBody);

    /**
     * 从回调数据中提取订单号和状态
     * @return map 含 orderNo、status（SUCCESS/FAILED）、paymentNo
     */
    Map<String, String> parseCallback(String rawBody);

    /**
     * 向支付网关反查该支付的「权威」状态与金额（不信任 webhook 报文本身）。
     * @param paymentId 网关支付号（PayMongo 的 pay_xxx）
     * @return map：status=PAID/FAILED、amount=实付金额(分)、paymentNo=权威支付号；查不到返回 null
     */
    Map<String, String> verifyPayment(String paymentId);

    /**
     * 发起退款
     * @param paymentNo GCash Reference ID
     * @param amount    退款金额
     */
    boolean refund(String paymentNo, BigDecimal amount);
}
