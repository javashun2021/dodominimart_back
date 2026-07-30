package com.ruoyi.mall.service;

/**
 * 平台级功能开关（读后台系统参数 sys_config）。
 * 接口声明在 ruoyi-mall，实现由 ruoyi-admin 提供（ruoyi-mall 不依赖 ruoyi-system）。
 */
public interface IPlatformToggleService
{
    /** 积分功能是否开启（sys_config: mall.points.enabled，缺省 false=关闭）。 */
    boolean isPointsEnabled();

    /**
     * 平台自营商家ID（sys_config: mall.self.merchant.id）。
     * 平台化后 DodoMiniMart 自家门店以商家(mall_merchant)行存在，此 ID 标识它：
     * 其商品即 App 首页/自营目录来源；其订单按自营处理（可在线支付、走自营网点、不打商家标记）。
     * 未配置返回 null（回退到旧的 merchant_id IS NULL 自营语义）。
     */
    Long getSelfMerchantId();
}
