-- v40: 二手市场同步帖「卖家联系方式」内部留存
--   同步时正则从正文删掉的联系方式(电话/飞机号/微信/QQ)保留到 source_contact 列。
--   仅后台管理可见(AdminPostResult 映射)，App/公开接口不返回(PostResult 不映射)。
-- 幂等，可重复执行。

DELIMITER $$

DROP PROCEDURE IF EXISTS v40_market_sync_contact$$
CREATE PROCEDURE v40_market_sync_contact()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name = 'mall_market_post' AND column_name = 'source_contact') THEN
    ALTER TABLE mall_market_post
      ADD COLUMN source_contact VARCHAR(500) DEFAULT NULL
        COMMENT '同步帖原始联系方式(后台可见,对外隐藏),普通帖为空';
  END IF;
END$$

CALL v40_market_sync_contact()$$
DROP PROCEDURE IF EXISTS v40_market_sync_contact$$

DELIMITER ;
