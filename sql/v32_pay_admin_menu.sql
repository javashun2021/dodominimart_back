-- ============================================================
-- v32 聚合支付：后台菜单（支付平台 / 商户管理 / 订单管理）
-- sys_menu 列: menu_id,menu_name,parent_id,order_num,url,menu_type,visible,perms,icon,create_by,create_time
-- 幂等：INSERT IGNORE（menu_id 为主键）
-- ============================================================

-- 一级目录：支付平台
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time) VALUES
(2300, '支付平台', 0, 5, '#', 'M', '0', '', 'fa fa-credit-card', 'admin', NOW());

-- 商户管理
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time) VALUES
(2310, '商户管理', 2300, 1, '/pay/merchant', 'C', '0', 'pay:merchant:view', 'fa fa-users', 'admin', NOW()),
(2311, '商户查询', 2310, 1, '#', 'F', '0', 'pay:merchant:list',   '#', 'admin', NOW()),
(2312, '商户新增', 2310, 2, '#', 'F', '0', 'pay:merchant:add',    '#', 'admin', NOW()),
(2313, '商户修改', 2310, 3, '#', 'F', '0', 'pay:merchant:edit',   '#', 'admin', NOW()),
(2314, '商户删除', 2310, 4, '#', 'F', '0', 'pay:merchant:remove', '#', 'admin', NOW());

-- 订单管理
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time) VALUES
(2320, '订单管理', 2300, 2, '/pay/order', 'C', '0', 'pay:order:view', 'fa fa-list-alt', 'admin', NOW()),
(2321, '订单查询', 2320, 1, '#', 'F', '0', 'pay:order:list',   '#', 'admin', NOW()),
(2322, '补发回调', 2320, 2, '#', 'F', '0', 'pay:order:notify', '#', 'admin', NOW());

-- 授权给 admin 角色（admin 用户为超管本就可见，这里为其它 admin 角色成员补授权）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN (SELECT 2300 menu_id UNION SELECT 2310 UNION SELECT 2311 UNION SELECT 2312
      UNION SELECT 2313 UNION SELECT 2314 UNION SELECT 2320 UNION SELECT 2321 UNION SELECT 2322) m
WHERE r.role_key = 'admin';
