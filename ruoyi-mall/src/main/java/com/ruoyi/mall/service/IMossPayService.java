package com.ruoyi.mall.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 上游支付通道（MOSS 支付）适配接口。
 *
 * 说明：MOSS 对接文档尚未到位，当前实现为「可 mock 的桩」——
 *   mall.moss.mock=true 时返回本地 mock 收银台链接，便于下游商户全链路联调；
 *   拿到 MOSS 文档后仅需替换 {@code MossPayServiceImpl} 的具体实现（签名/网关/字段）。
 */
public interface IMossPayService
{
    /**
     * 上游下单，返回支付链接 payurl。
     *
     * @param outTradeNo 传给上游的订单号（= 我方 platform_no）
     * @param amount     金额（元）
     * @param subject    订单标题
     * @param notifyUrl  上游异步通知我方的地址
     * @param clientIp   会员IP（MOSS 要求国内IP）
     * @return payurl
     */
    String createPayment(String outTradeNo, BigDecimal amount, String subject, String notifyUrl, String clientIp);

    /** 验证上游异步通知签名（RSA，待文档实现） */
    boolean verifyNotify(Map<String, String> params);

    /**
     * 解析上游异步通知。
     * @return {outTradeNo, status(SUCCESS/FAILED), tradeNo, amount}
     */
    Map<String, String> parseNotify(Map<String, String> params);

    /** 是否 mock 模式（无真实上游，用于本地联调） */
    boolean isMock();
}
