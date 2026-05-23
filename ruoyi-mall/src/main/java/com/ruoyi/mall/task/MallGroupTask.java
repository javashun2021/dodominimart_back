package com.ruoyi.mall.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.mall.service.IMallGroupService;

/**
 * 拼团过期任务
 * 在 Quartz 调度中配置：Bean 名 = mallGroupTask，方法 = expireGroups，Cron = 0 0/5 * * * ?
 */
@Component("mallGroupTask")
public class MallGroupTask
{
    @Autowired
    private IMallGroupService groupService;

    public void expireGroups()
    {
        groupService.expireGroups();
    }
}
