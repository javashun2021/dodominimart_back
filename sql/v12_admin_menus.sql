USE dodominimart;

-- Banner 管理
INSERT IGNORE INTO sys_menu VALUES(2080, 'Banner管理', 2000, 9,  '/mall/banner',  'C', '0', 'mall:banner:view',   'fa fa-picture-o',   'admin', now(), 'admin', now(), '首页轮播图管理');
INSERT IGNORE INTO sys_menu VALUES(2081, 'Banner查询', 2080, 1, '#', 'F', '0', 'mall:banner:list',   '#', 'admin', now(), 'admin', now(), '');
INSERT IGNORE INTO sys_menu VALUES(2082, 'Banner新增', 2080, 2, '#', 'F', '0', 'mall:banner:add',    '#', 'admin', now(), 'admin', now(), '');
INSERT IGNORE INTO sys_menu VALUES(2083, 'Banner修改', 2080, 3, '#', 'F', '0', 'mall:banner:edit',   '#', 'admin', now(), 'admin', now(), '');
INSERT IGNORE INTO sys_menu VALUES(2084, 'Banner删除', 2080, 4, '#', 'F', '0', 'mall:banner:remove', '#', 'admin', now(), 'admin', now(), '');

-- 评价管理
INSERT IGNORE INTO sys_menu VALUES(2085, '评价管理', 2000, 10, '/mall/review',  'C', '0', 'mall:review:view',  'fa fa-star-o',      'admin', now(), 'admin', now(), '商品评价审核');
INSERT IGNORE INTO sys_menu VALUES(2086, '评价查询', 2085, 1, '#', 'F', '0', 'mall:review:list',   '#', 'admin', now(), 'admin', now(), '');
INSERT IGNORE INTO sys_menu VALUES(2087, '评价删除', 2085, 2, '#', 'F', '0', 'mall:review:remove', '#', 'admin', now(), 'admin', now(), '');

-- 积分记录
INSERT IGNORE INTO sys_menu VALUES(2090, '积分记录', 2000, 11, '/mall/points',  'C', '0', 'mall:points:view',  'fa fa-diamond',     'admin', now(), 'admin', now(), '会员积分流水');
INSERT IGNORE INTO sys_menu VALUES(2091, '积分查询', 2090, 1, '#', 'F', '0', 'mall:points:list',  '#', 'admin', now(), 'admin', now(), '');

-- 支付记录
INSERT IGNORE INTO sys_menu VALUES(2095, '支付记录', 2000, 12, '/mall/payment', 'C', '0', 'mall:payment:view', 'fa fa-credit-card', 'admin', now(), 'admin', now(), 'GCash 支付流水');
INSERT IGNORE INTO sys_menu VALUES(2096, '支付查询', 2095, 1, '#', 'F', '0', 'mall:payment:list', '#', 'admin', now(), 'admin', now(), '');
