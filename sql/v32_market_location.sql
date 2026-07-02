-- v32: 市场帖子加位置标注字段

DELIMITER $$

DROP PROCEDURE IF EXISTS v32_market_location$$
CREATE PROCEDURE v32_market_location()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name = 'mall_market_post'
                   AND column_name = 'location_label') THEN
    ALTER TABLE mall_market_post
      ADD COLUMN location_label VARCHAR(100) DEFAULT NULL COMMENT '位置标注（如 Makati, Manila）',
      ADD COLUMN latitude  DECIMAL(10,6) DEFAULT NULL,
      ADD COLUMN longitude DECIMAL(10,6) DEFAULT NULL;
  END IF;
END$$

CALL v32_market_location()$$
DROP PROCEDURE IF EXISTS v32_market_location$$

DELIMITER ;
