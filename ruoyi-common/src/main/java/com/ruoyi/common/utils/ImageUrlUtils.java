package com.ruoyi.common.utils;

import java.util.regex.Pattern;

/**
 * 图片/资源 URL 归一化：统一存/返相对路径（如 /profile/upload/...），
 * 剥掉任意 http(s)://host[:port] 前缀。
 *
 * 背景：后台上传图片曾被存成内网绝对地址 http://8.222.208.133:8080/profile/upload/xxx.jpg，
 * 该地址不对外（无 HTTPS、内网 IP、安卓禁明文），导致 App 图片不显示。
 * App 端约定只吃相对路径（自行拼 baseUrl），资源地址一律由后端归一化为相对路径。
 */
public class ImageUrlUtils
{
    /** 匹配开头的 http:// 或 https:// + host[:port]（到第一个 '/' 之前） */
    private static final Pattern ABSOLUTE_PREFIX = Pattern.compile("(?i)^https?://[^/]+");

    /**
     * 把绝对地址归一化为相对路径；已是相对路径则原样返回。
     * 支持逗号分隔的多图（如 merchant.images），逐个归一化后重新拼接、去空。
     */
    public static String toRelative(String url)
    {
        if (url == null || url.isEmpty())
        {
            return url;
        }
        // 多图：逗号分隔逐个处理
        if (url.indexOf(',') >= 0)
        {
            String[] parts = url.split(",");
            StringBuilder sb = new StringBuilder();
            for (String part : parts)
            {
                String one = toRelative(part.trim());
                if (one == null || one.isEmpty())
                {
                    continue;
                }
                if (sb.length() > 0)
                {
                    sb.append(',');
                }
                sb.append(one);
            }
            return sb.toString();
        }
        String u = url.trim();
        java.util.regex.Matcher m = ABSOLUTE_PREFIX.matcher(u);
        if (m.find())
        {
            // 剥掉 scheme://host[:port]，保留 /path（如 /profile/upload/...）
            return u.substring(m.end());
        }
        return u;
    }
}
