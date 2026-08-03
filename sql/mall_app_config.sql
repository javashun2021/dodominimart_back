-- ----------------------------
-- App 初始化配置（写入 sys_config，管理员可在「系统管理 → 参数设置」修改）
-- config_type='N' 表示自定义参数
-- ----------------------------

INSERT INTO sys_config VALUES(100, 'App-店铺名称',       'app.store.name',         '我的商店',              'N', 'admin', now(), 'admin', now(), 'App 首页展示的店铺名称');
INSERT INTO sys_config VALUES(101, 'App-营业时间',       'app.store.hours',        '08:00 - 22:00',        'N', 'admin', now(), 'admin', now(), 'App 首页展示的营业时间');
INSERT INTO sys_config VALUES(102, 'App-联系电话',       'app.contact.phone',      '',                      'N', 'admin', now(), 'admin', now(), '客服/店主联系电话');
INSERT INTO sys_config VALUES(103, 'App-Messenger链接', 'app.messenger.link',     '',                      'N', 'admin', now(), 'admin', now(), 'Facebook Messenger 群/客服跳转链接');
INSERT INTO sys_config VALUES(104, 'App-公告',           'app.announcement',       '',                      'N', 'admin', now(), 'admin', now(), '首页公告横幅，为空则不显示');
INSERT INTO sys_config VALUES(105, 'App-配送费(PHP)',    'app.delivery.fee',       '0',                     'N', 'admin', now(), 'admin', now(), '固定配送费，0 表示免配送费');
INSERT INTO sys_config VALUES(106, 'App-最低起订金额',   'app.min.order.amount',   '0',                     'N', 'admin', now(), 'admin', now(), '最低下单金额，0 表示不限制');
