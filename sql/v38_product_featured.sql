-- v38: 商品「首页精选」标记 — mall_product 加 is_featured 字段
-- App 首页 Featured 板块只展示 is_featured=1 的商品；后台商品列表可一键切换。

DELIMITER $$

DROP PROCEDURE IF EXISTS v38_product_featured$$
CREATE PROCEDURE v38_product_featured()
BEGIN

  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name = 'mall_product'
                   AND column_name = 'is_featured') THEN
    ALTER TABLE mall_product
      ADD COLUMN is_featured TINYINT NOT NULL DEFAULT 0
        COMMENT '1=首页精选推荐位';
  END IF;

END$$

CALL v38_product_featured()$$
DROP PROCEDURE IF EXISTS v38_product_featured$$

DELIMITER ;
