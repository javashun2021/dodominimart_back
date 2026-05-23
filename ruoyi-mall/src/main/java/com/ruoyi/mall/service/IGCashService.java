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
     * @return map 含 orderNo、status（SUCCESS/FAILED）
     */
    Map<String, String> parseCallback(String rawBody);

    /**
     * 发起退款
     * @param paymentNo GCash Reference ID
     * @param amount    退款金额
     */
    boolean refund(String paymentNo, BigDecimal amount);
}
