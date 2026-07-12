-- v39: 二手市场「外部帖每日同步」
--   1) mall_market_post 加去重列 source / external_id + 唯一索引
--   2) sys_config 种子：开关 / 接口地址 / 发帖会员 / 联系电话 / 最大页数
--   3) sys_job 种子：marketSyncTask 每天 03:00（初始暂停，配好后在后台启用）
-- 全部幂等，可重复执行。

-- ── 1) 表结构：加列 + 唯一索引（存储过程判存在，幂等） ──────────────────────
DELIMITER $$

DROP PROCEDURE IF EXISTS v39_market_sync$$
CREATE PROCEDURE v39_market_sync()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name = 'mall_market_post' AND column_name = 'source') THEN
    ALTER TABLE mall_market_post
      ADD COLUMN source VARCHAR(32) DEFAULT NULL COMMENT '外部同步来源(如 ext)，普通帖为空';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name = 'mall_market_post' AND column_name = 'external_id') THEN
    ALTER TABLE mall_market_post
      ADD COLUMN external_id VARCHAR(64) DEFAULT NULL COMMENT '外部来源唯一ID(去重)，普通帖为空';
  END IF;

  -- source/external_id 均可空：普通用户帖 (NULL,NULL) 不受唯一约束影响（MySQL 允许多个 NULL）
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                 WHERE table_schema = DATABASE()
                   AND table_name = 'mall_market_post' AND index_name = 'uk_source_external') THEN
    ALTER TABLE mall_market_post
      ADD UNIQUE KEY uk_source_external (source, external_id);
  END IF;
END$$

CALL v39_market_sync()$$
DROP PROCEDURE IF EXISTS v39_market_sync$$

DELIMITER ;

-- ── 2) 系统参数（config_key 无唯一键，用 NOT EXISTS 保证幂等） ────────────────
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '市场同步-开关', 'mall.market.sync.enabled', 'false', 'Y', 'admin', NOW(),
       'true=启用二手市场外部帖每日同步；默认关'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'mall.market.sync.enabled');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '市场同步-接口地址', 'mall.market.sync.api-url',
       'https://www.flw.ph/plugin.php?id=zimu_fenlei&model=list&ids=2&cid1=0', 'Y', 'admin', NOW(),
       '外部接口地址；翻页时程序自动追加 &page=N'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'mall.market.sync.api-url');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '市场同步-发帖会员', 'mall.market.sync.member-id', '1001', 'Y', 'admin', NOW(),
       '导入帖归属的官方会员ID'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'mall.market.sync.member-id');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '市场同步-联系电话', 'mall.market.sync.contact-phone', '09474386306', 'Y', 'admin', NOW(),
       '导入帖统一显示的官方联系电话'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'mall.market.sync.contact-phone');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '市场同步-最大页数', 'mall.market.sync.max-pages', '50', 'Y', 'admin', NOW(),
       '每次同步最多翻多少页(安全上限，防翻页失控)'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'mall.market.sync.max-pages');

-- ── 3) 定时任务：每天 03:00；初始 status=1(暂停)，配好 key 后在「系统监控→定时任务」启用 ──
INSERT INTO sys_job (job_name, job_group, method_name, method_params, cron_expression,
                     misfire_policy, status, create_by, create_time, remark)
SELECT 'marketSyncTask', 'DEFAULT', 'run', '', '0 0 3 * * ?',
       '3', '1', 'admin', NOW(), '二手市场外部帖每日同步(拉取+翻译+入库)'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE job_name = 'marketSyncTask');
