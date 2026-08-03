-- ----------------------------
-- 订单打印小票配置（写入 sys_config，管理员可在「系统管理 → 参数设置」修改）
-- 店名、电话复用 app.store.name / app.contact.phone，无需重复添加
-- config_type='N' 表示自定义参数
-- ----------------------------

-- 不写死 config_id（config_id 为自增主键，代码按 config_key 读取，id 无关）；
-- 深链域名/Android 包名/指纹/iOS AppID 为部署相关，留空由 xingzhi 自行填写。
INSERT INTO sys_config(config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark) VALUES
('App-小票地址',    'app.receipt.address', '',           'N', 'admin', now(), 'admin', now(), '打印小票抬头显示的门店地址，为空则不显示'),
('App-小票页脚',    'app.receipt.footer',  '谢谢惠顾！', 'N', 'admin', now(), 'admin', now(), '打印小票底部的感谢语/提示'),
('App-深链域名',    'app.deeplink.base',   '',           'N', 'admin', now(), 'admin', now(), '订单二维码深链前缀，扫码用 App 打开订单详情（需与 App 的 App Link/Universal Link 域名一致）'),
('App-Android包名', 'app.android.package', '',           'N', 'admin', now(), 'admin', now(), 'Android applicationId，用于 assetlinks.json'),
('App-Android指纹', 'app.android.sha256',  '',           'N', 'admin', now(), 'admin', now(), 'App 签名证书 SHA-256 指纹（gradle signingReport / keytool 获取，多个用逗号分隔）'),
('App-iOS-AppID',   'app.ios.appid',       '',           'N', 'admin', now(), 'admin', now(), 'iOS Universal Link 的 TeamID.bundleId，用于 apple-app-site-association');
