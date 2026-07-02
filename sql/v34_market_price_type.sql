-- v34: 市场帖子价格类型

DELIMITER $$
DROP PROCEDURE IF EXISTS v34_market_price_type$$
CREATE PROCEDURE v34_market_price_type()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name = 'mall_market_post'
                   AND column_name = 'price_type') THEN
    ALTER TABLE mall_market_post
      ADD COLUMN price_type VARCHAR(20) NOT NULL DEFAULT 'fixed'
        COMMENT 'fixed=固定价格 negotiable=面议 free=免费';
  END IF;
END$$
CALL v34_market_price_type()$$
DROP PROCEDURE IF EXISTS v34_market_price_type$$
DELIMITER ;
