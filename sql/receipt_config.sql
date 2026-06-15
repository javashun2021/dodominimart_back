-- ----------------------------
-- 订单打印小票配置（写入 sys_config，管理员可在「系统管理 → 参数设置」修改）
-- 店名、电话复用 app.store.name / app.contact.phone，无需重复添加
-- config_type='N' 表示自定义参数
-- ----------------------------

INSERT INTO sys_config VALUES(107, 'App-小票地址', 'app.receipt.address', '',                        'N', 'admin', now(), 'admin', now(), '打印小票抬头显示的门店地址，为空则不显示');
INSERT INTO sys_config VALUES(108, 'App-小票页脚', 'app.receipt.footer',  'Thank you for shopping!', 'N', 'admin', now(), 'admin', now(), '打印小票底部的感谢语/提示');
