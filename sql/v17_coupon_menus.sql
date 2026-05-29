USE dodominimart;

-- Coupon Management (parent menu, sort 13)
INSERT IGNORE INTO sys_menu VALUES(2100, 'Coupon Management', 2000, 13, '/mall/coupon',         'C', '0', 'mall:coupon:view',   'fa fa-ticket',    'admin', now(), 'admin', now(), 'Coupon template management');
INSERT IGNORE INTO sys_menu VALUES(2101, 'Coupon Query',      2100, 1,  '#', 'F', '0', 'mall:coupon:list',   '#', 'admin', now(), 'admin', now(), '');
INSERT IGNORE INTO sys_menu VALUES(2102, 'Coupon Add',        2100, 2,  '#', 'F', '0', 'mall:coupon:add',    '#', 'admin', now(), 'admin', now(), '');
INSERT IGNORE INTO sys_menu VALUES(2103, 'Coupon Edit',       2100, 3,  '#', 'F', '0', 'mall:coupon:edit',   '#', 'admin', now(), 'admin', now(), '');
INSERT IGNORE INTO sys_menu VALUES(2104, 'Coupon Delete',     2100, 4,  '#', 'F', '0', 'mall:coupon:remove', '#', 'admin', now(), 'admin', now(), '');

-- Member Coupons (sub-menu under Coupon Management)
INSERT IGNORE INTO sys_menu VALUES(2110, 'Member Coupons',   2100, 5,  '/mall/coupon/member-coupons', 'C', '0', 'mall:coupon:view', 'fa fa-users', 'admin', now(), 'admin', now(), 'View member coupon holdings');
