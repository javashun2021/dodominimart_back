-- ─────────────────────────────────────────────────────────────────────────────
-- v45 附近商家入驻（一期）：多商家维度叠加在自营模型之上
--   1. mall_merchant     商家/门店表（经纬度 + 审核状态 + 归属地推员）
--   2. mall_product      加 merchant_id 列（NULL=平台自营，非NULL=某商家商品；不破坏现有自营）
--   3. sys_dict          mall_merchant_type 商家分类字典
--   4. sys_menu          商城管理下新增「商家管理」「推广员管理」+ 按钮 + 授权
-- 地推员不建新表：会员 role='promoter'，其 invite_code 即专属推广码/QR。
-- 在生产库(dodominimart)手动执行。表/列/字典部分幂等可重复跑；
-- 第4节菜单同 v27 为一次性脚本，请勿重复执行(会重复插入菜单行)。
-- ─────────────────────────────────────────────────────────────────────────────

-- ── 1. 商家表 ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS mall_merchant (
    merchant_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '商家ID',
    name              VARCHAR(100) NOT NULL                COMMENT '商家/门店名称',
    category          VARCHAR(50)           DEFAULT NULL   COMMENT '商家分类(字典 mall_merchant_type)',
    description       VARCHAR(500)          DEFAULT NULL   COMMENT '商家简介',
    logo_url          VARCHAR(255)          DEFAULT NULL   COMMENT '门店Logo/头图',
    images            VARCHAR(2000)         DEFAULT NULL   COMMENT '门店照片，逗号分隔URL',
    address           VARCHAR(255)          DEFAULT NULL   COMMENT '门店地址',
    lat               DECIMAL(10,7)         DEFAULT NULL   COMMENT '纬度',
    lng               DECIMAL(10,7)         DEFAULT NULL   COMMENT '经度',
    phone             VARCHAR(32)           DEFAULT NULL   COMMENT '联系电话',
    business_hours    VARCHAR(64)           DEFAULT NULL   COMMENT '营业时间，如 08:00-22:00',
    service_radius_km DECIMAL(6,2) NOT NULL DEFAULT 5.00   COMMENT '配送半径(km)，二期配送用',
    promoter_id       BIGINT                DEFAULT NULL   COMMENT '录入的地推员会员ID(业绩归属)',
    owner_member_id   BIGINT                DEFAULT NULL   COMMENT '商家App账号会员ID(代录可空)',
    status            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0待审核 1营业 2拒绝 3停业)',
    reject_reason     VARCHAR(255)          DEFAULT NULL   COMMENT '审核拒绝原因',
    review_time       DATETIME              DEFAULT NULL   COMMENT '审核时间',
    reviewer          VARCHAR(64)           DEFAULT NULL   COMMENT '审核人',
    sort              INT          NOT NULL DEFAULT 0      COMMENT '显示排序',
    del_flag          CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 2删除)',
    create_by         VARCHAR(64)           DEFAULT NULL   COMMENT '创建者',
    create_time       DATETIME              DEFAULT NULL   COMMENT '创建时间',
    update_by         VARCHAR(64)           DEFAULT NULL   COMMENT '更新者',
    update_time       DATETIME              DEFAULT NULL   COMMENT '更新时间',
    remark            VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
    PRIMARY KEY (merchant_id),
    KEY idx_status (status),
    KEY idx_promoter (promoter_id),
    KEY idx_geo (lat, lng)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家/门店(入驻)';

-- ── 2. 给 mall_product 加 merchant_id（幂等：列/索引已存在则跳过）──────────────
DROP PROCEDURE IF EXISTS __v45_add_col;
DELIMITER $$
CREATE PROCEDURE __v45_add_col(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl VARCHAR(512))
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

CALL __v45_add_col('mall_product', 'merchant_id', "merchant_id BIGINT DEFAULT NULL COMMENT '归属商家ID(NULL=平台自营)'");
DROP PROCEDURE IF EXISTS __v45_add_col;

DROP PROCEDURE IF EXISTS __v45_add_idx;
DELIMITER $$
CREATE PROCEDURE __v45_add_idx(IN tbl VARCHAR(64), IN idx VARCHAR(64), IN cols VARCHAR(256))
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

CALL __v45_add_idx('mall_product', 'idx_merchant', 'merchant_id');
DROP PROCEDURE IF EXISTS __v45_add_idx;

-- 商家商品不归属自营分类，category_id 放开为可空（自营仍会填，不受影响）。MODIFY 幂等。
ALTER TABLE mall_product MODIFY category_id INT(11) NULL COMMENT '自营分类ID(商家商品可空)';

-- ── 3. 商家分类字典 mall_merchant_type ───────────────────────────────────────
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '商家分类', 'mall_merchant_type', '0', 'admin', NOW(), '附近商家入驻分类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mall_merchant_type');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time)
SELECT * FROM (
    SELECT 1 AS dict_sort, 'Convenience Store' AS dict_label, 'convenience' AS dict_value, 'mall_merchant_type' AS dict_type, 'Y' AS is_default, '0' AS status, 'admin' AS create_by, NOW() AS create_time UNION ALL
    SELECT 2, 'Restaurant / Food',   'food',        'mall_merchant_type', 'N', '0', 'admin', NOW() UNION ALL
    SELECT 3, 'Fresh / Grocery',     'grocery',     'mall_merchant_type', 'N', '0', 'admin', NOW() UNION ALL
    SELECT 4, 'Pharmacy',            'pharmacy',    'mall_merchant_type', 'N', '0', 'admin', NOW() UNION ALL
    SELECT 5, 'Clothing',            'clothing',    'mall_merchant_type', 'N', '0', 'admin', NOW() UNION ALL
    SELECT 6, 'Electronics',         'electronics', 'mall_merchant_type', 'N', '0', 'admin', NOW() UNION ALL
    SELECT 7, 'Other',               'other',       'mall_merchant_type', 'N', '0', 'admin', NOW()
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mall_merchant_type');

-- ── 4. 后台菜单（商家管理 / 推广员管理）──────────────────────────────────────
-- 以现有「商品分类」菜单(perms='mall:category:view')为模板复制 parent_id 等，并授权给拥有该权限的角色。

-- 4.1 商家管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time)
SELECT 'Merchants', parent_id, 9, '/mall/merchant', 'C', visible, 'mall:merchant:view', 'fa fa-shopping-bag', 'admin', now()
FROM sys_menu WHERE perms = 'mall:category:view' LIMIT 1;
SET @merchant_menu_id := LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time) VALUES
('Merchant Query',   @merchant_menu_id, 1, '#', 'F', '0', 'mall:merchant:list',   '#', 'admin', now()),
('Merchant Add',     @merchant_menu_id, 2, '#', 'F', '0', 'mall:merchant:add',    '#', 'admin', now()),
('Merchant Edit',    @merchant_menu_id, 3, '#', 'F', '0', 'mall:merchant:edit',   '#', 'admin', now()),
('Merchant Remove',  @merchant_menu_id, 4, '#', 'F', '0', 'mall:merchant:remove', '#', 'admin', now()),
('Merchant Review',  @merchant_menu_id, 5, '#', 'F', '0', 'mall:merchant:review', '#', 'admin', now());

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.menu_id
FROM sys_role_menu rm
JOIN sys_menu m0 ON m0.menu_id = rm.menu_id AND m0.perms = 'mall:category:view'
JOIN sys_menu m  ON m.menu_id IN (@merchant_menu_id,
                                  @merchant_menu_id+1, @merchant_menu_id+2, @merchant_menu_id+3,
                                  @merchant_menu_id+4, @merchant_menu_id+5);

-- 4.2 推广员管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time)
SELECT 'Promoters', parent_id, 10, '/mall/promoter', 'C', visible, 'mall:promoter:view', 'fa fa-bullhorn', 'admin', now()
FROM sys_menu WHERE perms = 'mall:category:view' LIMIT 1;
SET @promoter_menu_id := LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time) VALUES
('Promoter Query',   @promoter_menu_id, 1, '#', 'F', '0', 'mall:promoter:list',   '#', 'admin', now()),
('Promoter Assign',  @promoter_menu_id, 2, '#', 'F', '0', 'mall:promoter:assign', '#', 'admin', now());

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.menu_id
FROM sys_role_menu rm
JOIN sys_menu m0 ON m0.menu_id = rm.menu_id AND m0.perms = 'mall:category:view'
JOIN sys_menu m  ON m.menu_id IN (@promoter_menu_id, @promoter_menu_id+1, @promoter_menu_id+2);
