-- v30: C2C 市场（发帖 + 评论）
-- 使用存储过程保证幂等（多次执行不出错）

DELIMITER $$

DROP PROCEDURE IF EXISTS v30_market$$
CREATE PROCEDURE v30_market()
BEGIN

  -- 帖子表
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                 WHERE table_schema = DATABASE() AND table_name = 'mall_market_post') THEN
    CREATE TABLE mall_market_post (
      post_id       BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
      member_id     BIGINT       NOT NULL COMMENT '发帖人',
      title         VARCHAR(100) NOT NULL COMMENT '标题',
      description   TEXT                  COMMENT '描述',
      price         DECIMAL(10,2)         COMMENT '价格（0=免费/面议）',
      images        VARCHAR(2000)         COMMENT '图片URL，逗号分隔',
      category      VARCHAR(50)           COMMENT '分类',
      status        CHAR(1)      NOT NULL DEFAULT '0' COMMENT '0=在售 1=已售 2=下架',
      phone         VARCHAR(20)           COMMENT '联系电话',
      whatsapp      VARCHAR(20)           COMMENT 'WhatsApp号码',
      messenger     VARCHAR(100)          COMMENT 'Messenger链接或用户名',
      telegram      VARCHAR(50)           COMMENT 'Telegram用户名',
      view_count    INT          NOT NULL DEFAULT 0,
      comment_count INT          NOT NULL DEFAULT 0,
      del_flag      CHAR(1)      NOT NULL DEFAULT '0',
      create_by     VARCHAR(64),
      create_time   DATETIME,
      update_by     VARCHAR(64),
      update_time   DATETIME
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='C2C市场帖子';
  END IF;

  -- 评论表
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                 WHERE table_schema = DATABASE() AND table_name = 'mall_market_comment') THEN
    CREATE TABLE mall_market_comment (
      comment_id  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
      post_id     BIGINT       NOT NULL,
      member_id   BIGINT       NOT NULL COMMENT '评论人',
      content     VARCHAR(500) NOT NULL,
      del_flag    CHAR(1)      NOT NULL DEFAULT '0',
      create_time DATETIME
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='C2C市场评论';
  END IF;

END$$

CALL v30_market()$$
DROP PROCEDURE IF EXISTS v30_market$$

DELIMITER ;
