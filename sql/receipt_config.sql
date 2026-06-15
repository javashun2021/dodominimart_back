-- ----------------------------
-- 订单打印小票配置（写入 sys_config，管理员可在「系统管理 → 参数设置」修改）
-- 店名、电话复用 app.store.name / app.contact.phone，无需重复添加
-- config_type='N' 表示自定义参数
-- ----------------------------

INSERT INTO sys_config VALUES(107, 'App-小票地址',   'app.receipt.address', '',                        'N', 'admin', now(), 'admin', now(), '打印小票抬头显示的门店地址，为空则不显示');
INSERT INTO sys_config VALUES(108, 'App-小票页脚',   'app.receipt.footer',  'Thank you for shopping!', 'N', 'admin', now(), 'admin', now(), '打印小票底部的感谢语/提示');
INSERT INTO sys_config VALUES(109, 'App-深链域名',   'app.deeplink.base',   'https://dodominimart.com', 'N', 'admin', now(), 'admin', now(), '订单二维码深链前缀，扫码用 App 打开订单详情（需与 App 的 App Link/Universal Link 域名一致）');
-- Android App Links / iOS Universal Links 验证（/.well-known 文件从这两个值动态生成，改完无需重打包后端）
INSERT INTO sys_config VALUES(110, 'App-Android包名',   'app.android.package', 'com.dodominimart.app',     'N', 'admin', now(), 'admin', now(), 'Android applicationId，用于 assetlinks.json');
INSERT INTO sys_config VALUES(111, 'App-Android指纹',   'app.android.sha256',  'REPLACE_WITH_APP_SIGNING_SHA256_FINGERPRINT', 'N', 'admin', now(), 'admin', now(), 'App 签名证书 SHA-256 指纹（gradle signingReport / keytool 获取，多个用逗号分隔）');
INSERT INTO sys_config VALUES(112, 'App-iOS-AppID',     'app.ios.appid',       'TEAMID.com.dodominimart.app', 'N', 'admin', now(), 'admin', now(), 'iOS Universal Link 的 TeamID.bundleId，用于 apple-app-site-association');
