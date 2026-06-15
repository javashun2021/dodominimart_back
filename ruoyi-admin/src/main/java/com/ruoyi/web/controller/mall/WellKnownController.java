package com.ruoyi.web.controller.mall;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.system.service.ISysConfigService;

/**
 * App Link / Universal Link 的域名验证文件。
 *
 * <p>必须以本域名 HTTPS、{@code application/json}、无重定向地提供：
 * <ul>
 *   <li>Android：{@code /.well-known/assetlinks.json} —— 声明 {@code dodominimart.com} 由该 App 处理，
 *       需要填入 App 签名证书的 SHA-256 指纹（配置项 {@code app.android.sha256}，多个用逗号分隔）。</li>
 *   <li>iOS：{@code /.well-known/apple-app-site-association} —— 声明 Universal Link，
 *       需要填入 {@code TeamID.bundleId}（配置项 {@code app.ios.appid}）。</li>
 * </ul>
 * 这两个值在「系统管理 → 参数设置」里改，改完无需重新打包后端。Shiro 已放行 {@code /.well-known/**}。</p>
 */
@RestController
public class WellKnownController
{
    @Autowired
    private ISysConfigService configService;

    /** Android App Links 验证 */
    @GetMapping(value = "/.well-known/assetlinks.json", produces = "application/json;charset=UTF-8")
    public String assetLinks()
    {
        String pkg = cfg("app.android.package", "com.dodominimart.app");
        String shaCsv = cfg("app.android.sha256", "REPLACE_WITH_APP_SIGNING_SHA256_FINGERPRINT");

        StringBuilder fps = new StringBuilder();
        String[] parts = shaCsv.split(",");
        for (int i = 0; i < parts.length; i++)
        {
            String fp = parts[i].trim();
            if (fp.isEmpty())
            {
                continue;
            }
            if (fps.length() > 0)
            {
                fps.append(",");
            }
            fps.append("\"").append(fp).append("\"");
        }

        return "[{"
            + "\"relation\":[\"delegate_permission/common.handle_all_urls\"],"
            + "\"target\":{"
            + "\"namespace\":\"android_app\","
            + "\"package_name\":\"" + pkg + "\","
            + "\"sha256_cert_fingerprints\":[" + fps + "]"
            + "}}]";
    }

    /** iOS Universal Links 验证（apple-app-site-association，无扩展名） */
    @GetMapping(value = "/.well-known/apple-app-site-association", produces = "application/json;charset=UTF-8")
    public String appleAppSiteAssociation()
    {
        String appId = cfg("app.ios.appid", "TEAMID.com.dodominimart.app");
        return "{"
            + "\"applinks\":{"
            + "\"apps\":[],"
            + "\"details\":[{"
            + "\"appID\":\"" + appId + "\","
            + "\"paths\":[\"/o/*\"]"
            + "}]"
            + "}}";
    }

    private String cfg(String key, String defaultValue)
    {
        try
        {
            String v = configService.selectConfigByKey(key);
            return (v != null && !v.isEmpty()) ? v : defaultValue;
        }
        catch (Exception e)
        {
            return defaultValue;
        }
    }
}
