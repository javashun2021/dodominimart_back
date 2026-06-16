USE dodominimart;

-- App Error Logs (parent menu under 商城管理 2000, sort 16)
INSERT IGNORE INTO sys_menu VALUES(2120, 'App Error Logs', 2000, 16, '/mall/apperror', 'C', '0', 'mall:apperror:view',   'fa fa-bug', 'admin', now(), 'admin', now(), 'App client error/crash logs');
INSERT IGNORE INTO sys_menu VALUES(2121, 'Error Query',    2120, 1,  '#', 'F', '0', 'mall:apperror:list',   '#', 'admin', now(), 'admin', now(), '');
INSERT IGNORE INTO sys_menu VALUES(2122, 'Error Delete',   2120, 2,  '#', 'F', '0', 'mall:apperror:remove', '#', 'admin', now(), 'admin', now(), '');

-- Grant to admin role (role_id 1 = super admin already has all via role_menu? ensure mapping)
INSERT IGNORE INTO sys_role_menu(role_id, menu_id) VALUES(1, 2120), (1, 2121), (1, 2122);
