-- ─────────────────────────────────────────────────────────────────────────────
-- v25 商品品牌名 brand_name
--   给 mall_product 增加可选的品牌名字段（展示在商品图上）。
--   原文件曾被误写成只有 "commit;" 一行（丢失了 ALTER），此处补回。
--   幂等，可重复跑；MySQL / MariaDB 通用。
-- ─────────────────────────────────────────────────────────────────────────────

DROP PROCEDURE IF EXISTS __v25_add_col;
DELIMITER $$
CREATE PROCEDURE __v25_add_col(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl VARCHAR(512))
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

CALL __v25_add_col('mall_product', 'brand_name',
    "brand_name VARCHAR(100) DEFAULT '' COMMENT '品牌名（可选，展示在商品图上）'");

DROP PROCEDURE IF EXISTS __v25_add_col;