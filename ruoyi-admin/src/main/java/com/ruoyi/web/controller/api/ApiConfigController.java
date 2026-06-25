package com.ruoyi.web.controller.api;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.system.service.ISysConfigService;

/**
 * App 初始化配置接口（无需登录）
 * GET /api/v1/config
 */
@RestController
@RequestMapping("/api/v1/config")
public class ApiConfigController
{
    @Autowired
    private ISysConfigService configService;

    @GetMapping
    @Cacheable(value = "mall_app_config", key = "'v1'")
    public AjaxResult getAppConfig()
    {
        Map<String, Object> data = new LinkedHashMap<>();

        // 店铺基本信息
        data.put("storeName",       val("app.store.name",       "Dodominimart"));
        data.put("storeHours",      val("app.store.hours",      ""));
        data.put("contactPhone",    val("app.contact.phone",    ""));

        // 社交/客服链接
        data.put("messengerLink",   val("app.messenger.link",   ""));

        // 首页公告（空字符串表示无公告，App 端据此决定是否显示横幅）
        data.put("announcement",    val("app.announcement",     ""));

        // 订单规则
        data.put("deliveryFee",     val("app.delivery.fee",     "0"));
        data.put("minOrderAmount",  val("app.min.order.amount", "0"));

        // 支付方式开关
        data.put("gcashEnabled", "true".equalsIgnoreCase(val("mall.gcash.enabled", "false")));

        // 搜索热词（逗号分隔）
        data.put("searchHotwords", val("mall.search.hotwords", ""));

        // 预计配送时长（分钟）
        data.put("deliveryMinutes", val("app.delivery.minutes", "30"));

        // App 版本升级（全为 0/空 表示不提醒；App 端据此决定是否弹更新窗）
        data.put("latestVersionCode", intVal("app.version.latest.code", 0)); // 最新版 build number
        data.put("latestVersionName", val("app.version.latest.name", ""));   // 最新版展示名，如 3.0.1
        data.put("minVersionCode",    intVal("app.version.min.code",    0)); // 低于它=强制更新
        data.put("updateNotes",       val("app.update.notes",           "")); // 更新内容文案
        data.put("androidStoreUrl",   val("app.store.android.url",      "")); // Play 商店页
        data.put("iosStoreUrl",       val("app.store.ios.url",          "")); // App Store 页

        return AjaxResult.success("ok").put("data", data);
    }

    /** 读取配置值，取不到时返回 defaultValue */
    private String val(String key, String defaultValue)
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

    /** 读取整型配置值，取不到或非数字时返回 defaultValue */
    private int intVal(String key, int defaultValue)
    {
        try
        {
            return Integer.parseInt(val(key, String.valueOf(defaultValue)).trim());
        }
        catch (Exception e)
        {
            return defaultValue;
        }
    }
}
