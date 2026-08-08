package com.ruoyi.mall.service;

import java.util.Map;

/**
 * 聚合支付-下游商户开放 API 业务编排。
 * 协议参照 亿林/直付通（api.txt）。
 */
public interface IPayOpenService
{
    /**
     * 下游下单。
     * @param channelId 通道编码(Request-Channel-Id)
     * @param siteCode  商户号/站点码(Request-Site-Code)
     * @param requestId 随机串(Request-Id)
     * @param data      业务参数(amount/orderNo/extra/clientIp/notifyUrl/currency/userId)
     * @param sign      商户 MD5 签名
     * @return 亿林风格返回 {code, data|msg}
     */
    Map<String, Object> createPayment(String channelId, String siteCode, String requestId,
                                      Map<String, String> data, String sign);

    /** 查单，返回亿林风格结构 {code, msg, data{...}} */
    Map<String, Object> query(String platformNo);

    /**
     * 上游支付成功入口：置单 PAID 并触发回调下游。
     * @return 是否本次真正置为已支付（幂等：重复调用返回 false）
     */
    boolean handleUpstreamPaid(String platformNo, String upstreamNo, String rawBody);

    /** 主动向商户补发一次回调（供定时任务/后台手动调用）。返回是否收到 success */
    boolean pushNotify(String platformNo);
}
