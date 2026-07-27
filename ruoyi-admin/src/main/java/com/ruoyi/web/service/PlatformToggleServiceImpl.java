package com.ruoyi.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.mall.service.IPlatformToggleService;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 平台功能开关实现：读 sys_config（后台「系统管理→参数设置」可实时改，无需重启）。
 * ruoyi-admin 同时依赖 ruoyi-mall 与 ruoyi-system，充当二者的桥接。
 */
@Service
public class PlatformToggleServiceImpl implements IPlatformToggleService
{
    @Autowired
    private ISysConfigService configService;

    @Override
    public boolean isPointsEnabled()
    {
        try
        {
            return "true".equalsIgnoreCase(configService.selectConfigByKey("mall.points.enabled"));
        }
        catch (Exception e)
        {
            // 参数缺失/异常按关闭处理，保证「默认关闭」语义
            return false;
        }
    }
}
