package com.ruoyi.web.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.web.service.MarketSyncService;

/**
 * 二手市场外部帖同步定时任务。
 * Quartz 按 sys_job 的 job_name=marketSyncTask 反射调用无参方法 run()（每天一次）。
 */
@Component("marketSyncTask")
public class MarketSyncTask
{
    @Autowired
    private MarketSyncService marketSyncService;

    public void run()
    {
        marketSyncService.syncOnce();
    }
}
