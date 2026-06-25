-- ----------------------------
-- App 版本升级配置（写入 sys_config，管理员可在「系统管理 → 参数设置」修改）
-- /api/v1/config 下发这些字段，App 端据此决定是否弹更新窗。
-- 全为 0 / 空 表示不提醒。改完需清 mall_app_config 缓存或重启服务才生效。
-- 不指定 config_id，走自增，避免与现有参数主键冲突。
-- ----------------------------

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
VALUES ('App-最新版本号',   'app.version.latest.code', '0',  'N', 'admin', now(), '最新版 build number（versionCode），<=当前安装版本则不提醒');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
VALUES ('App-最新版本名',   'app.version.latest.name', '',   'N', 'admin', now(), '最新版展示名，如 3.0.1');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
VALUES ('App-最低版本号',   'app.version.min.code',    '0',  'N', 'admin', now(), '低于此 build number 强制更新（不可关弹窗）');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
VALUES ('App-更新说明',     'app.update.notes',        '',   'N', 'admin', now(), '更新弹窗正文文案，可多行');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
VALUES ('App-安卓商店链接', 'app.store.android.url',   'https://play.google.com/store/apps/details?id=com.dodominimart.app', 'N', 'admin', now(), '点「立即更新」跳转的 Google Play 页');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
VALUES ('App-iOS商店链接',  'app.store.ios.url',       '',   'N', 'admin', now(), '点「立即更新」跳转的 App Store 页');
