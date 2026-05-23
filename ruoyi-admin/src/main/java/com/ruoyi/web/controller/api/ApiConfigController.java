package com.ruoyi.web.controller.api;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
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
}
