-- ----------------------------
-- 测试者批量邀请菜单
-- 列顺序：menu_id, menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark
-- 说明：admin（超级管理员）会自动加载全部菜单；如需分配给其他角色，请在“角色管理”中勾选。
-- ----------------------------

-- 菜单：测试者邀请（挂在“商城管理” 2000 下；若 2200/2201 已被占用，请改用其它未使用的 id）
INSERT INTO sys_menu VALUES(2200, '测试者邀请', 2000, 9, '/tool/invite', 'C', '0', 'tool:invite:view', 'fa fa-paper-plane', 'admin', now(), 'admin', now(), 'Google Play 内测批量邀请');

-- 按钮权限：发送邀请
INSERT INTO sys_menu VALUES(2201, '发送邀请', 2200, 1, '#', 'F', '0', 'tool:invite:send', '#', 'admin', now(), 'admin', now(), '');
