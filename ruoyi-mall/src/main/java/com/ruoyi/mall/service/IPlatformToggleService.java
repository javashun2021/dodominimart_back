package com.ruoyi.mall.service;

/**
 * 平台级功能开关（读后台系统参数 sys_config）。
 * 接口声明在 ruoyi-mall，实现由 ruoyi-admin 提供（ruoyi-mall 不依赖 ruoyi-system）。
 */
public interface IPlatformToggleService
{
    /** 积分功能是否开启（sys_config: mall.points.enabled，缺省 false=关闭）。 */
    boolean isPointsEnabled();
}
