-- ============================================================
-- v37 设置对外深链域名 app.deeplink.base
-- 用途:① 导入订单接口返回的免登录订单详情链接 orderUrl 的域名前缀;
--       ② 后台订单小票二维码 /o/{id}?t=... 的域名前缀(同一配置)。
-- 之前该配置为空 → 代码 fallback 到 https://dodominimart.com(非行知域名),链接指向错误。
-- 域名按生产实际改;下方用部署冒烟所用的 http://zxzwl.top。若已上 HTTPS 改成 https://zxzwl.top。
-- 幂等:存在则更新,不存在则插入。
-- ============================================================
UPDATE sys_config SET config_value='http://zxzwl.top' WHERE config_key='app.deeplink.base';

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '对外深链域名', 'app.deeplink.base', 'http://zxzwl.top', 'Y', 'admin', NOW(), '订单公开页/小票二维码域名前缀'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='app.deeplink.base');
