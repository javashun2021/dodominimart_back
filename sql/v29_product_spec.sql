-- ─────────────────────────────────────────────────────────────────────────────
-- v29 商品单层属性（口味/规格）
--   仅一层：商品有一个属性名(spec_name，如「口味」)+ 若干选项(spec_options，逗号分隔)。
--   纯标签：不分库存/价格，顾客下单选一个，快照写到订单明细 mall_order_item.spec。
--   spec_name/spec_options 为空 = 该商品无属性，行为不变。
-- 在生产库(dodominimart@8.222.208.133)手动执行；幂等，可重复跑。
-- ─────────────────────────────────────────────────────────────────────────────

DROP PROCEDURE IF EXISTS __v29_add_col;
DELIMITER $$
CREATE PROCEDURE __v29_add_col(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl VARCHAR(512))
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

CALL __v29_add_col('mall_product', 'spec_name',
    "spec_name VARCHAR(32) DEFAULT NULL COMMENT '属性名，如 口味/Flavor（空=无属性）'");
CALL __v29_add_col('mall_product', 'spec_options',
    "spec_options VARCHAR(1000) DEFAULT NULL COMMENT '属性选项，逗号分隔，如 苹果味,水蜜桃味,原味'");
CALL __v29_add_col('mall_order_item', 'spec',
    "spec VARCHAR(64) DEFAULT NULL COMMENT '下单所选属性值快照，如 苹果味'");

DROP PROCEDURE IF EXISTS __v29_add_col;
