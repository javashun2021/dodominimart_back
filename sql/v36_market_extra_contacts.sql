-- v36: 市场帖子扩展联系方式字段

DELIMITER $$
DROP PROCEDURE IF EXISTS v36_market_extra_contacts$$
CREATE PROCEDURE v36_market_extra_contacts()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name = 'mall_market_post'
                   AND column_name = 'viber') THEN
    ALTER TABLE mall_market_post
      ADD COLUMN viber     VARCHAR(30)  DEFAULT NULL,
      ADD COLUMN wechat    VARCHAR(60)  DEFAULT NULL,
      ADD COLUMN line      VARCHAR(60)  DEFAULT NULL,
      ADD COLUMN instagram VARCHAR(60)  DEFAULT NULL;
  END IF;
END$$
CALL v36_market_extra_contacts()$$
DROP PROCEDURE IF EXISTS v36_market_extra_contacts$$
DELIMITER ;
