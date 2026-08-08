package com.ruoyi.mall.service.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ruoyi.mall.service.IMossPayService;

/**
 * MOSS 上游通道实现。
 *
 * ⚠️ MOSS 正式对接文档未到，当前分两种模式：
 *  - mall.moss.mock=true（默认）：返回本地 mock 收银台链接
 *    {mall.moss.mock-cashier}?no={platformNo}，配合 /openapi/mock/paid/{platformNo} 模拟支付成功，
 *    使下游商户 API 全链路（下单→支付→回调→查单）现在即可联调。
 *  - mall.moss.mock=false：走真实 MOSS 网关（RSA 签名），
 *    createPayment/verifyNotify/parseNotify 待按 MOSS 文档补全（现抛出未实现）。
 *
 * 拿到文档后：仅改本类，下游控制器/订单流零改动。
 */
@Service
public class MossPayServiceImpl implements IMossPayService
{
    private static final Logger log = LoggerFactory.getLogger(MossPayServiceImpl.class);

    @Value("${mall.moss.mock:true}")
    private boolean mock;

    /** mock 模式下的收银台页面地址 */
    @Value("${mall.moss.mock-cashier:http://localhost:8080/openapi/mock/cashier}")
    private String mockCashier;

    @Value("${mall.moss.gateway:}")
    private String gateway;

    @Value("${mall.moss.business-channel-id:}")
    private String businessChannelId;

    @Value("${mall.moss.mer-pri-key:}")
    private String merPriKey;

    @Override
    public boolean isMock()
    {
        return mock;
    }

    @Override
    public String createPayment(String outTradeNo, BigDecimal amount, String subject, String notifyUrl, String clientIp)
    {
        if (mock)
        {
            String sep = mockCashier.contains("?") ? "&" : "?";
            String url = mockCashier + sep + "no=" + outTradeNo + "&amount=" + amount.toPlainString();
            log.info("[MOSS-MOCK] createPayment outTradeNo={} amount={} -> {}", outTradeNo, amount, url);
            return url;
        }
        // TODO: 按 MOSS 文档实现——组装参数、RSA 签名(merPriKey)、POST gateway、解析 payurl
        throw new UnsupportedOperationException("MOSS 上游未接入（请提供对接文档或开启 mall.moss.mock=true）");
    }

    @Override
    public boolean verifyNotify(Map<String, String> params)
    {
        if (mock)
        {
            return true; // mock 模式不校验
        }
        // TODO: 按 MOSS 文档做 RSA 验签（MOSS 公钥）
        throw new UnsupportedOperationException("MOSS verifyNotify 未接入");
    }

    @Override
    public Map<String, String> parseNotify(Map<String, String> params)
    {
        // TODO: 按 MOSS 文档解析真实字段
        Map<String, String> r = new HashMap<>();
        r.put("outTradeNo", params.getOrDefault("outTradeNo", params.get("orderNo")));
        r.put("status", "SUCCESS");
        r.put("tradeNo", params.getOrDefault("tradeNo", ""));
        r.put("amount", params.getOrDefault("amount", ""));
        return r;
    }
}
