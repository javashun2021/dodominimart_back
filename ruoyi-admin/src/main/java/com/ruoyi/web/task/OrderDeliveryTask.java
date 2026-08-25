package com.ruoyi.web.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.mall.service.IMallOrderService;

/**
 * 外部导入单-配送到点完成 定时任务。
 * 由 sys_job 调度：job_group=orderDeliveryTask，method_name=advanceArrived。
 * 扫描到达时间已过、仍处于配送中的导入单，推进为已完成。
 */
@Component("orderDeliveryTask")
public class OrderDeliveryTask
{
    @Autowired
    private IMallOrderService mallOrderService;

    /** 每次最多推进 200 单 */
    public void advanceArrived()
    {
        mallOrderService.advanceArrivedImports(200);
    }
}
