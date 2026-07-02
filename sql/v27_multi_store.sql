-- ─────────────────────────────────────────────────────────────────────────────
-- v27 多店（模型 C：一个中心仓 + 多个发货网点 / 自提点）
--   商品 / 库存 / 价格 全局共享，不分店；store 只固化在「订单 / 骑手 / 收银员」上。
--   App 端 GPS 就近自动选店、可手动改；下单把 store_id 带进订单。
-- 在生产库(dodominimart@8.222.208.133)手动执行；下面所有语句均做了幂等保护，可重复跑。
-- ─────────────────────────────────────────────────────────────────────────────

-- ── 1. 门店表 ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS mall_store (
    store_id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '门店ID',
    name              VARCHAR(100) NOT NULL                COMMENT '门店名称',
    address           VARCHAR(255)          DEFAULT NULL   COMMENT '门店地址',
    lat               DECIMAL(10,7)         DEFAULT NULL   COMMENT '纬度',
    lng               DECIMAL(10,7)         DEFAULT NULL   COMMENT '经度',
    phone             VARCHAR(32)           DEFAULT NULL   COMMENT '联系电话',
    service_radius_km DECIMAL(6,2) NOT NULL DEFAULT 5.00   COMMENT '配送/覆盖半径(km)',
    business_hours    VARCHAR(64)           DEFAULT NULL   COMMENT '营业时间，如 08:00-22:00',
    status            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0营业 1停业)',
    sort              INT          NOT NULL DEFAULT 0      COMMENT '显示排序',
    del_flag          CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 2删除)',
    create_by         VARCHAR(64)           DEFAULT NULL   COMMENT '创建者',
    create_time       DATETIME              DEFAULT NULL   COMMENT '创建时间',
    update_by         VARCHAR(64)           DEFAULT NULL   COMMENT '更新者',
    update_time       DATETIME              DEFAULT NULL   COMMENT '更新时间',
    remark            VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
    PRIMARY KEY (store_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店/发货网点';

-- ── 2. 给订单 / 会员 / 骑手 加 store_id（幂等：列已存在则跳过）────────────────
-- MySQL 的 ADD COLUMN 不支持 IF NOT EXISTS，这里用 information_schema 判存在再 PREPARE。
DROP PROCEDURE IF EXISTS __v27_add_col;
DELIMITER $$
CREATE PROCEDURE __v27_add_col(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl VARCHAR(512))
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = tbl AND column_name = col
    ) THEN
        SET @s = CONCAT('ALTER TABLE ', tbl, ' ADD COLUMN ', ddl);
        PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
    END IF;
END $$
DELIMITER ;

CALL __v27_add_col('mall_order',              'store_id', "store_id BIGINT DEFAULT NULL COMMENT '归属门店ID'");
CALL __v27_add_col('mall_member',   'preferred_store_id', "preferred_store_id BIGINT DEFAULT NULL COMMENT '常用/上次选择门店ID'");
CALL __v27_add_col('mall_runner_application', 'store_id', "store_id BIGINT DEFAULT NULL COMMENT '骑手归属门店ID'");

DROP PROCEDURE IF EXISTS __v27_add_col;

-- 订单按门店过滤会用到的索引（幂等：存在则忽略错误由执行者判断，这里用 information_schema 保护）
DROP PROCEDURE IF EXISTS __v27_add_idx;
DELIMITER $$
CREATE PROCEDURE __v27_add_idx(IN tbl VARCHAR(64), IN idx VARCHAR(64), IN cols VARCHAR(256))
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = tbl AND index_name = idx
    ) THEN
        SET @s = CONCAT('ALTER TABLE ', tbl, ' ADD INDEX ', idx, ' (', cols, ')');
        PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
    END IF;
END $$
DELIMITER ;

CALL __v27_add_idx('mall_order',              'idx_store_id', 'store_id');
CALL __v27_add_idx('mall_runner_application', 'idx_store_id', 'store_id');

DROP PROCEDURE IF EXISTS __v27_add_idx;

-- ── 3. 种子门店（= 现有那家实体店）────────────────────────────────────────────
-- ⚠️ lat/lng 先放马尼拉大致坐标占位，务必在 后台→门店管理 改成真实门店坐标，
--    否则「GPS 就近选店」会算错。
INSERT INTO mall_store (name, address, lat, lng, phone, service_radius_km, business_hours, status, sort, create_by, create_time)
SELECT 'DODO MiniMart', 'Please update store address in admin', 14.5995000, 120.9842000, NULL, 5.00, '08:00-22:00', '0', 0, 'admin', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM mall_store);

-- ── 4. 回填存量数据：老订单 / 老骑手 归到第一家（最小 store_id）门店 ──────────
-- 不回填的话，按 store_id 过滤后老订单/老骑手会「消失」。
SET @seed_store_id := (SELECT MIN(store_id) FROM mall_store);
UPDATE mall_order              SET store_id = @seed_store_id WHERE store_id IS NULL;
UPDATE mall_runner_application SET store_id = @seed_store_id WHERE store_id IS NULL;

-- ── 5. 后台菜单（门店管理）────────────────────────────────────────────────────
-- 以现有「商品分类」菜单(perms='mall:category:view')为模板复制 parent_id 等。
-- 执行后授予所有「能看商品分类」的角色。
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time)
SELECT 'Stores', parent_id, 8, '/mall/store', 'C', visible, 'mall:store:view', 'fa fa-map-marker', 'admin', now()
FROM sys_menu WHERE perms = 'mall:category:view' LIMIT 1;

SET @store_menu_id := LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time) VALUES
('Store Query',  @store_menu_id, 1, '#', 'F', '0', 'mall:store:list',   '#', 'admin', now()),
('Store Add',    @store_menu_id, 2, '#', 'F', '0', 'mall:store:add',    '#', 'admin', now()),
('Store Edit',   @store_menu_id, 3, '#', 'F', '0', 'mall:store:edit',   '#', 'admin', now()),
('Store Remove', @store_menu_id, 4, '#', 'F', '0', 'mall:store:remove', '#', 'admin', now());

-- 把新菜单授予所有已拥有「商品分类查看」权限的角色
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, @store_menu_id
FROM sys_role_menu rm
JOIN sys_menu m ON m.menu_id = rm.menu_id AND m.perms = 'mall:category:view';
