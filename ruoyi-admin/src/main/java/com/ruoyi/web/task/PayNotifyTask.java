package com.ruoyi.web.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.mall.service.IPayOpenService;

/**
 * 聚合支付-下游商户回调补发定时任务。
 * 由 sys_job 调度：job_name=payNotifyTask（本分支用 job_name 作 bean 名），method_name=retryPending。
 */
@Component("payNotifyTask")
public class PayNotifyTask
{
    @Autowired
    private IPayOpenService payOpenService;

    /** 补发一批待回调订单（每次最多 100 单） */
    public void retryPending()
    {
        payOpenService.retryPendingNotify(100);
    }
}
