-- ============================================================
-- v31 聚合支付：下游商户回调补发 定时任务
-- 调度 bean: payNotifyTask.retryPending()（每 60 秒一次）
-- 幂等：已存在同名任务则不重复插入
-- ============================================================
INSERT INTO sys_job (job_id, job_name, job_group, method_name, method_params,
    cron_expression, misfire_policy, status, create_by, create_time, remark)
SELECT 100, '支付回调补发', 'payNotifyTask', 'retryPending', '',
    '0 0/1 * * * ?', '3', '0', 'admin', NOW(), '补发下游商户支付成功回调（notify_status<>1 且未超重试上限）'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE job_group = 'payNotifyTask' AND method_name = 'retryPending');
