-- ─────────────────────────────────────────────────────────────────────────────
-- v28 商品多图
--   image_url 保持不变 = 封面（列表/卡片/购物车缩略图仍用它）。
--   新增 images = 附加图，逗号分隔的 URL；App 详情页图廊 = [封面] + images。
-- 在生产库(dodominimart@8.222.208.133)手动执行；幂等，可重复跑。
-- ─────────────────────────────────────────────────────────────────────────────

DROP PROCEDURE IF EXISTS __v28_add_col;
DELIMITER $$
CREATE PROCEDURE __v28_add_col(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl VARCHAR(512))
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

CALL __v28_add_col('mall_product', 'images',
    "images VARCHAR(2000) DEFAULT NULL COMMENT '附加图URL，逗号分隔（封面仍存 image_url）'");

DROP PROCEDURE IF EXISTS __v28_add_col;
