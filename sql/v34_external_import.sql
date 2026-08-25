-- ============================================================
-- v34 对外「订单导入」接口
--   1) mall_order 增列 arrival_time（外部导入单预计/实际送达时间）+ 索引
--   2) 种子跑腿员池（5 名，审核通过 status=1 且在线 is_online=1，供导入单随机指派）
--   3) sys_job 注册 orderDeliveryTask.advanceArrived（每 5 分钟，到点完成配送中导入单）
-- 幂等：可重复执行
-- ============================================================

-- 1) mall_order.arrival_time --------------------------------------------------
DROP PROCEDURE IF EXISTS __v34_add_arrival;
DELIMITER $$
CREATE PROCEDURE __v34_add_arrival()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'mall_order'
          AND column_name = 'arrival_time') THEN
        ALTER TABLE mall_order ADD COLUMN arrival_time DATETIME NULL
            COMMENT '外部导入单预计/实际送达时间（到点由定时任务置完成）';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'mall_order'
          AND index_name = 'idx_arrival') THEN
        ALTER TABLE mall_order ADD INDEX idx_arrival (arrival_time);
    END IF;
END $$
DELIMITER ;
CALL __v34_add_arrival();
DROP PROCEDURE IF EXISTS __v34_add_arrival;

-- 1b) mall_member 增列 external_id（外部会员标识，字符串；与内部 member_id 解耦）
DROP PROCEDURE IF EXISTS __v34_add_extid;
DELIMITER $$
CREATE PROCEDURE __v34_add_extid()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'mall_member'
          AND column_name = 'external_id') THEN
        ALTER TABLE mall_member ADD COLUMN external_id VARCHAR(64) NULL
            COMMENT '外部会员标识（外部导入订单的 userId 字符串）';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'mall_member'
          AND index_name = 'idx_external_id') THEN
        ALTER TABLE mall_member ADD INDEX idx_external_id (external_id);
    END IF;
END $$
DELIMITER ;
CALL __v34_add_extid();
DROP PROCEDURE IF EXISTS __v34_add_extid;

-- 2) 种子跑腿员池 ------------------------------------------------------------
-- 2.1 骑手会员（email 作幂等键）
INSERT INTO mall_member(email, nick_name, status, create_time)
SELECT 'sys_runner1@sys.local', '骑手·小张', '0', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM mall_member WHERE email = 'sys_runner1@sys.local');
INSERT INTO mall_member(email, nick_name, status, create_time)
SELECT 'sys_runner2@sys.local', '骑手·小李', '0', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM mall_member WHERE email = 'sys_runner2@sys.local');
INSERT INTO mall_member(email, nick_name, status, create_time)
SELECT 'sys_runner3@sys.local', '骑手·小王', '0', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM mall_member WHERE email = 'sys_runner3@sys.local');
INSERT INTO mall_member(email, nick_name, status, create_time)
SELECT 'sys_runner4@sys.local', '骑手·小陈', '0', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM mall_member WHERE email = 'sys_runner4@sys.local');
INSERT INTO mall_member(email, nick_name, status, create_time)
SELECT 'sys_runner5@sys.local', '骑手·小赵', '0', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM mall_member WHERE email = 'sys_runner5@sys.local');

-- 2.2 骑手申请（审核通过 + 在线；member_id 作幂等键）
INSERT INTO mall_runner_application(member_id, real_name, id_number, phone, status, is_online, apply_time, review_time, reviewer)
SELECT m.member_id, '骑手·小张', '000000000000000001', '13800000001', '1', '1', NOW(), NOW(), 'system'
FROM mall_member m WHERE m.email = 'sys_runner1@sys.local'
  AND NOT EXISTS (SELECT 1 FROM mall_runner_application a WHERE a.member_id = m.member_id);
INSERT INTO mall_runner_application(member_id, real_name, id_number, phone, status, is_online, apply_time, review_time, reviewer)
SELECT m.member_id, '骑手·小李', '000000000000000002', '13800000002', '1', '1', NOW(), NOW(), 'system'
FROM mall_member m WHERE m.email = 'sys_runner2@sys.local'
  AND NOT EXISTS (SELECT 1 FROM mall_runner_application a WHERE a.member_id = m.member_id);
INSERT INTO mall_runner_application(member_id, real_name, id_number, phone, status, is_online, apply_time, review_time, reviewer)
SELECT m.member_id, '骑手·小王', '000000000000000003', '13800000003', '1', '1', NOW(), NOW(), 'system'
FROM mall_member m WHERE m.email = 'sys_runner3@sys.local'
  AND NOT EXISTS (SELECT 1 FROM mall_runner_application a WHERE a.member_id = m.member_id);
INSERT INTO mall_runner_application(member_id, real_name, id_number, phone, status, is_online, apply_time, review_time, reviewer)
SELECT m.member_id, '骑手·小陈', '000000000000000004', '13800000004', '1', '1', NOW(), NOW(), 'system'
FROM mall_member m WHERE m.email = 'sys_runner4@sys.local'
  AND NOT EXISTS (SELECT 1 FROM mall_runner_application a WHERE a.member_id = m.member_id);
INSERT INTO mall_runner_application(member_id, real_name, id_number, phone, status, is_online, apply_time, review_time, reviewer)
SELECT m.member_id, '骑手·小赵', '000000000000000005', '13800000005', '1', '1', NOW(), NOW(), 'system'
FROM mall_member m WHERE m.email = 'sys_runner5@sys.local'
  AND NOT EXISTS (SELECT 1 FROM mall_runner_application a WHERE a.member_id = m.member_id);

-- 3) sys_job：配送到点完成 ----------------------------------------------------
INSERT INTO sys_job (job_id, job_name, job_group, method_name, method_params,
    cron_expression, misfire_policy, status, create_by, create_time, remark)
SELECT 101, '导入单配送到点完成', 'orderDeliveryTask', 'advanceArrived', '',
    '0 0/5 * * * ?', '3', '0', 'admin', NOW(), '扫描到达时间已过、仍配送中的外部导入单并置为已完成'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE job_group = 'orderDeliveryTask' AND method_name = 'advanceArrived');
