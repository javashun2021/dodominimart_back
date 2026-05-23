-- ----------------------------
-- 商城管理菜单（执行前请确认 sys_menu 中不存在重复数据）
-- 列顺序：menu_id, menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark
-- ----------------------------

-- 一级菜单：商城管理
INSERT INTO sys_menu VALUES(2000, '商城管理', 0,   5, '#',              'M', '0', '',                    'fa fa-shopping-cart', 'admin', now(), 'admin', now(), '');

-- 二级菜单
INSERT INTO sys_menu VALUES(2001, '商品分类', 2000, 1, '/mall/category', 'C', '0', 'mall:category:view', 'fa fa-tags',          'admin', now(), 'admin', now(), '商品分类管理');
INSERT INTO sys_menu VALUES(2002, '商品管理', 2000, 2, '/mall/product',  'C', '0', 'mall:product:view',  'fa fa-cube',          'admin', now(), 'admin', now(), '商品信息管理');
INSERT INTO sys_menu VALUES(2003, '订单管理', 2000, 3, '/mall/order',    'C', '0', 'mall:order:view',    'fa fa-file-text-o',   'admin', now(), 'admin', now(), '订单处理');
INSERT INTO sys_menu VALUES(2004, '会员管理', 2000, 4, '/mall/member',   'C', '0', 'mall:member:view',   'fa fa-users',         'admin', now(), 'admin', now(), 'App 会员查看');

-- 分类按钮权限
INSERT INTO sys_menu VALUES(2010, '分类查询', 2001, 1, '#', 'F', '0', 'mall:category:list',   '#', 'admin', now(), 'admin', now(), '');
INSERT INTO sys_menu VALUES(2011, '分类新增', 2001, 2, '#', 'F', '0', 'mall:category:add',    '#', 'admin', now(), 'admin', now(), '');
INSERT INTO sys_menu VALUES(2012, '分类修改', 2001, 3, '#', 'F', '0', 'mall:category:edit',   '#', 'admin', now(), 'admin', now(), '');
INSERT INTO sys_menu VALUES(2013, '分类删除', 2001, 4, '#', 'F', '0', 'mall:category:remove', '#', 'admin', now(), 'admin', now(), '');

-- 商品按钮权限
INSERT INTO sys_menu VALUES(2020, '商品查询', 2002, 1, '#', 'F', '0', 'mall:product:list',   '#', 'admin', now(), 'admin', now(), '');
INSERT INTO sys_menu VALUES(2021, '商品新增', 2002, 2, '#', 'F', '0', 'mall:product:add',    '#', 'admin', now(), 'admin', now(), '');
INSERT INTO sys_menu VALUES(2022, '商品修改', 2002, 3, '#', 'F', '0', 'mall:product:edit',   '#', 'admin', now(), 'admin', now(), '');
INSERT INTO sys_menu VALUES(2023, '商品删除', 2002, 4, '#', 'F', '0', 'mall:product:remove', '#', 'admin', now(), 'admin', now(), '');

-- 订单按钮权限
INSERT INTO sys_menu VALUES(2030, '订单查询', 2003, 1, '#', 'F', '0', 'mall:order:list', '#', 'admin', now(), 'admin', now(), '');
INSERT INTO sys_menu VALUES(2031, '订单处理', 2003, 2, '#', 'F', '0', 'mall:order:edit', '#', 'admin', now(), 'admin', now(), '');

-- 会员按钮权限
INSERT INTO sys_menu VALUES(2040, '会员查询', 2004, 1, '#', 'F', '0', 'mall:member:list', '#', 'admin', now(), 'admin', now(), '');
