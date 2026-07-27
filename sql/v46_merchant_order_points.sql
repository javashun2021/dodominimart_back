-- ─────────────────────────────────────────────────────────────────────────────
-- v46 商家下单（二期前置）+ 积分平台开关
--   1. mall_order   加 merchant_id 列（NULL=自营，非NULL=某商家的单；一单一商家）
--   2. mall_product category_id 放开为可空（商家商品不归属自营分类；补 v45 遗留）
--   3. sys_config   积分平台开关 mall.points.enabled，默认 false（关闭）
-- 在生产库(dodominimart)手动执行，幂等可重复跑。
-- ─────────────────────────────────────────────────────────────────────────────

-- ── 1. mall_order 加 merchant_id（幂等：列/索引已存在则跳过）──────────────────
DROP PROCEDURE IF EXISTS __v46_add_col;
DELIMITER $$
CREATE PROCEDURE __v46_add_col(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl VARCHAR(512))
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

CALL __v46_add_col('mall_order', 'merchant_id', "merchant_id BIGINT DEFAULT NULL COMMENT '归属商家ID(NULL=平台自营)'");
DROP PROCEDURE IF EXISTS __v46_add_col;

DROP PROCEDURE IF EXISTS __v46_add_idx;
DELIMITER $$
CREATE PROCEDURE __v46_add_idx(IN tbl VARCHAR(64), IN idx VARCHAR(64), IN cols VARCHAR(256))
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

CALL __v46_add_idx('mall_order', 'idx_merchant', 'merchant_id');
DROP PROCEDURE IF EXISTS __v46_add_idx;

-- ── 2. mall_product.category_id 放开可空（商家商品不归属自营分类；MODIFY 幂等）──
ALTER TABLE mall_product MODIFY category_id INT(11) NULL COMMENT '自营分类ID(商家商品可空)';

-- ── 3. 积分平台开关（默认关闭）────────────────────────────────────────────────
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '积分功能开关', 'mall.points.enabled', 'false', 'Y', 'admin', NOW(),
       'true=开启积分(发放/抵扣/App显示)，false=全局关闭。默认关闭。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'mall.points.enabled');
