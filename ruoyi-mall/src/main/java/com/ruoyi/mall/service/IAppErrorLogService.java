package com.ruoyi.mall.service;

import java.util.List;
import com.ruoyi.mall.domain.AppErrorLog;

public interface IAppErrorLogService
{
    List<AppErrorLog> selectErrorLogList(AppErrorLog log);
    AppErrorLog       selectErrorLogById(Long id);
    int               insertErrorLog(AppErrorLog log);
    int               deleteErrorLogByIds(Long[] ids);
    /** 清理最近 days 天之前的日志，返回删除条数 */
    int               cleanErrorLogBefore(int days);
}
