-- v48 平台化功能开关：积分 / 优惠券 / 联系我们（Call Us、Messenger）
-- DodoMiniMart 收敛为「平台下的一家小店铺」，自营独有的积分、优惠券与客服联系入口默认隐藏。
-- App 读 /api/v1/config 的 pointsEnabled / couponsEnabled / contactEnabled 决定显隐；
-- 后台在「系统管理 → 参数设置」按 config_key 改值即可开关（改后需重启/重部署，接口有缓存）。
-- 幂等：INSERT 用 NOT EXISTS 防重复；points 用 UPDATE 幂等。可反复执行。生产库手动执行。

-- ── 1. 积分功能开关（关闭）──────────────────────────────────────────────────────
-- 该 key 已存在且当前为 true，直接置 false 隐藏「我的积分」入口与结算抵扣。
UPDATE sys_config
   SET config_value = 'false', update_by = 'admin', update_time = NOW()
 WHERE config_key = 'mall.points.enabled';

-- ── 2. 优惠券功能开关（默认关闭）────────────────────────────────────────────────
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '优惠券功能开关', 'mall.coupons.enabled', 'false', 'Y', 'admin', NOW(),
       'true=开启优惠券(我的优惠券/结算选券/折扣行)，false=全局隐藏。默认关闭。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'mall.coupons.enabled');

-- ── 3. 联系我们开关（默认关闭）──────────────────────────────────────────────────
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '联系我们开关', 'mall.contact.enabled', 'false', 'Y', 'admin', NOW(),
       'true=显示 Call Us / Messenger 入口，false=全局隐藏。默认关闭。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'mall.contact.enabled');