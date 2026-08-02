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

    // 方法级缓存 mall_app_config;改参数(SysConfigController 增删改/刷新缓存)时会 @CacheEvict 清空,
    // 保证改公告等参数即时生效。
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

        // 积分功能开关（默认关闭；App 据此决定是否显示积分入口/结算抵扣）
        data.put("pointsEnabled", "true".equalsIgnoreCase(val("mall.points.enabled", "false")));

        // 优惠券功能开关（默认关闭；App 据此显示我的优惠券/结算选券/折扣行）
        data.put("couponsEnabled", "true".equalsIgnoreCase(val("mall.coupons.enabled", "false")));

        // 联系我们开关（默认关闭；平台化下自营 Call Us / Messenger 入口不外露）
        data.put("contactEnabled", "true".equalsIgnoreCase(val("mall.contact.enabled", "false")));

        // 平台自营商家ID（0=无）。App 据此把该商家的商品/订单当自营处理（可在线支付/走自营网点），
        // 而非入驻商家单。对应后端 mall.self.merchant.id。
        data.put("selfMerchantId", intVal("mall.self.merchant.id", 0));

        // 站内聊天 WebSocket 地址（App 连 wss://.../ws/chat?token=<jwt>）
        data.put("chatWsUrl", val("app.chat.ws.url", ""));

        // 搜索热词（逗号分隔）
        data.put("searchHotwords", val("mall.search.hotwords", ""));

        // 市场联系方式（逗号分隔的 key，如 phone,whatsapp,telegram,viber,wechat）
        data.put("marketContactMethods",
            val("mall.market.contact.methods", "phone,whatsapp,messenger,telegram"));

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
