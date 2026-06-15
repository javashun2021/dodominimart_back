package com.ruoyi.web.service;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 订单二维码深链的签名 Token。
 *
 * <p>二维码 URL 形如 {@code https://域名/o/{orderId}?t={token}}，token = HMAC-SHA256(secret, "order:"+orderId)
 * 的前若干字节十六进制。只有持有正确小票（正确 token）的人才能通过 web 页查看该订单，避免按 orderId 裸奏遍历。
 * 复用商城 JWT 的密钥 {@code mall.jwt.secret}，无需额外配置。</p>
 */
@Service
public class ReceiptTokenService
{
    /** 截取 HMAC 前 12 字节 → 24 位十六进制，足够防猜测又不至于太长 */
    private static final int TOKEN_BYTES = 12;

    @Value("${mall.jwt.secret}")
    private String secret;

    /** 为订单生成签名 token */
    public String sign(Long orderId)
    {
        if (orderId == null)
        {
            return "";
        }
        try
        {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] full = mac.doFinal(("order:" + orderId).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < TOKEN_BYTES && i < full.length; i++)
            {
                sb.append(String.format("%02x", full[i] & 0xff));
            }
            return sb.toString();
        }
        catch (Exception e)
        {
            return "";
        }
    }

    /** 校验 token 是否匹配该订单（常量时间比较，防时序攻击） */
    public boolean verify(Long orderId, String token)
    {
        if (orderId == null || token == null || token.isEmpty())
        {
            return false;
        }
        String expected = sign(orderId);
        if (expected.isEmpty() || expected.length() != token.length())
        {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < expected.length(); i++)
        {
            diff |= expected.charAt(i) ^ token.charAt(i);
        }
        return diff == 0;
    }
}
