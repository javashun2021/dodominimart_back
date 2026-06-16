package com.ruoyi.mall.mapper;

import java.util.Date;
import java.util.List;
import com.ruoyi.mall.domain.AppErrorLog;

public interface AppErrorLogMapper
{
    List<AppErrorLog> selectErrorLogList(AppErrorLog log);
    AppErrorLog       selectErrorLogById(Long id);
    int               insertErrorLog(AppErrorLog log);
    int               deleteErrorLogByIds(Long[] ids);
    /** 清理某时间点之前的日志，返回删除条数 */
    int               deleteErrorLogBefore(Date before);
}
