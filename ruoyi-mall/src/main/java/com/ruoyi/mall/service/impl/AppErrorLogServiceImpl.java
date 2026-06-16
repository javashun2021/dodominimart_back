package com.ruoyi.mall.service.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.mall.domain.AppErrorLog;
import com.ruoyi.mall.mapper.AppErrorLogMapper;
import com.ruoyi.mall.service.IAppErrorLogService;

@Service
public class AppErrorLogServiceImpl implements IAppErrorLogService
{
    @Autowired
    private AppErrorLogMapper errorLogMapper;

    @Override
    public List<AppErrorLog> selectErrorLogList(AppErrorLog log)
    {
        return errorLogMapper.selectErrorLogList(log);
    }

    @Override
    public AppErrorLog selectErrorLogById(Long id)
    {
        return errorLogMapper.selectErrorLogById(id);
    }

    @Override
    public int insertErrorLog(AppErrorLog log)
    {
        if (log.getCreateTime() == null)
        {
            log.setCreateTime(new Date());
        }
        return errorLogMapper.insertErrorLog(log);
    }

    @Override
    public int deleteErrorLogByIds(Long[] ids)
    {
        return errorLogMapper.deleteErrorLogByIds(ids);
    }

    @Override
    public int cleanErrorLogBefore(int days)
    {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, -Math.abs(days));
        return errorLogMapper.deleteErrorLogBefore(c.getTime());
    }
}
