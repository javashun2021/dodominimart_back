package com.ruoyi.mall.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

/**
 * 下游商户 API 签名工具（亿林/直付通 MD5 规则）。
 *
 * 规则：业务参数按 key 的 a-z 升序排列，拼成 k1=v1&k2=v2&...，
 *      末尾追加 &key=<商户KEY>，对整串做 MD5，转大写。
 * 参与签名的参数：所有非空的业务字段，剔除 sign / sign_type 本身。
 */
public final class PaySignUtil
{
    private PaySignUtil() {}

    /**
     * 计算签名。
     * @param params 业务参数（会自动剔除 sign/sign_type 与空值）
     * @param key    商户密钥（imspay_merchant.app_secret）
     * @return 大写 MD5 签名串
     */
    public static String sign(Map<String, ?> params, String key)
    {
        TreeMap<String, String> sorted = new TreeMap<>();
        if (params != null)
        {
            for (Map.Entry<String, ?> e : params.entrySet())
            {
                String k = e.getKey();
                if (k == null || "sign".equalsIgnoreCase(k) || "sign_type".equalsIgnoreCase(k))
                {
                    continue;
                }
                Object v = e.getValue();
                if (v == null)
                {
                    continue;
                }
                String s = String.valueOf(v);
                if (s.isEmpty())
                {
                    continue;
                }
                sorted.put(k, s);
            }
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet())
        {
            if (sb.length() > 0)
            {
                sb.append('&');
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        sb.append(sb.length() > 0 ? "&" : "").append("key=").append(key == null ? "" : key);

        return md5Upper(sb.toString());
    }

    /**
     * 校验签名（常量时间比较，忽略大小写）。
     */
    public static boolean verify(Map<String, ?> params, String key, String sign)
    {
        if (sign == null || sign.isEmpty())
        {
            return false;
        }
        String expected = sign(params, key);
        return constantTimeEqualsIgnoreCase(expected, sign);
    }

    public static String md5Upper(String text)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash)
            {
                hex.append(String.format("%02X", b));
            }
            return hex.toString();
        }
        catch (Exception e)
        {
            throw new RuntimeException("MD5 error", e);
        }
    }

    private static boolean constantTimeEqualsIgnoreCase(String a, String b)
    {
        if (a == null || b == null || a.length() != b.length())
        {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++)
        {
            diff |= Character.toLowerCase(a.charAt(i)) ^ Character.toLowerCase(b.charAt(i));
        }
        return diff == 0;
    }
}
