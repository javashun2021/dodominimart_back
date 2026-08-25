-- ============================================================
-- v36 后台菜单中文化(mall 模块剩余英文菜单/按钮权限名)
-- 幂等:UPDATE 按 menu_id,可重复执行。改完刷新页面即可(菜单从 DB 读)。
-- ============================================================
UPDATE sys_menu SET menu_name='轮播图管理' WHERE menu_id=2080;
UPDATE sys_menu SET menu_name='轮播图查询' WHERE menu_id=2081;
UPDATE sys_menu SET menu_name='轮播图新增' WHERE menu_id=2082;
UPDATE sys_menu SET menu_name='轮播图修改' WHERE menu_id=2083;
UPDATE sys_menu SET menu_name='轮播图删除' WHERE menu_id=2084;
UPDATE sys_menu SET menu_name='评价管理'   WHERE menu_id=2085;
UPDATE sys_menu SET menu_name='评价查询'   WHERE menu_id=2086;
UPDATE sys_menu SET menu_name='评价删除'   WHERE menu_id=2087;
UPDATE sys_menu SET menu_name='积分记录'   WHERE menu_id=2090;
UPDATE sys_menu SET menu_name='积分查询'   WHERE menu_id=2091;
UPDATE sys_menu SET menu_name='支付记录'   WHERE menu_id=2095;
UPDATE sys_menu SET menu_name='支付查询'   WHERE menu_id=2096;
UPDATE sys_menu SET menu_name='优惠券管理' WHERE menu_id=2100;
UPDATE sys_menu SET menu_name='优惠券查询' WHERE menu_id=2101;
UPDATE sys_menu SET menu_name='优惠券新增' WHERE menu_id=2102;
UPDATE sys_menu SET menu_name='优惠券修改' WHERE menu_id=2103;
UPDATE sys_menu SET menu_name='优惠券删除' WHERE menu_id=2104;
UPDATE sys_menu SET menu_name='会员优惠券' WHERE menu_id=2110;
UPDATE sys_menu SET menu_name='App错误日志' WHERE menu_id=2120;
UPDATE sys_menu SET menu_name='错误查询'   WHERE menu_id=2121;
UPDATE sys_menu SET menu_name='错误删除'   WHERE menu_id=2122;
