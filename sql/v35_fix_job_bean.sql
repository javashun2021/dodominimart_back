-- ============================================================
-- v35 修正定时任务的 bean 解析
-- 本分支 ScheduleJob 用 job_name 作为 Spring bean 名(见示例任务 job_name='ryTask'),
-- 而 v31/v34 误把中文显示名写进 job_name、bean 名写进 job_group,导致
-- "No bean named '支付回调补发'/'导入单配送到点完成'"。此处把 job_name 改回 bean 名,
-- 显示名移到 job_group。改完后需重启应用(启动时 init() 会用新值刷新 Quartz JobDataMap)。
-- 幂等:可重复执行。
-- ============================================================

UPDATE sys_job
SET job_name = 'payNotifyTask', job_group = '聚合支付-回调补发'
WHERE method_name = 'retryPending' AND job_name = '支付回调补发';

UPDATE sys_job
SET job_name = 'orderDeliveryTask', job_group = '订单导入-配送到点'
WHERE method_name = 'advanceArrived' AND job_name = '导入单配送到点完成';
