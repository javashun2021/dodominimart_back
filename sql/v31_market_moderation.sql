-- v31: 市场内容审核 — 举报表 + 帖子/评论状态字段

DELIMITER $$

DROP PROCEDURE IF EXISTS v31_market_moderation$$
CREATE PROCEDURE v31_market_moderation()
BEGIN

  -- 举报表
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                 WHERE table_schema = DATABASE() AND table_name = 'mall_market_report') THEN
    CREATE TABLE mall_market_report (
      report_id    BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
      target_type  VARCHAR(10) NOT NULL COMMENT 'post | comment',
      target_id    BIGINT      NOT NULL COMMENT '被举报的 post_id 或 comment_id',
      reporter_id  BIGINT      NOT NULL COMMENT '举报人 member_id',
      reason       VARCHAR(50) NOT NULL COMMENT '举报原因',
      detail       VARCHAR(300)         COMMENT '补充说明',
      status       CHAR(1)     NOT NULL DEFAULT '0' COMMENT '0=待审 1=已处理 2=忽略',
      handled_by   VARCHAR(64)          COMMENT '处理人',
      handled_note VARCHAR(300)         COMMENT '处理备注',
      create_time  DATETIME,
      handle_time  DATETIME
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='市场内容举报';
  END IF;

  -- 帖子表加 moderated/report_count/review_status 字段
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name = 'mall_market_post'
                   AND column_name = 'moderated') THEN
    ALTER TABLE mall_market_post
      ADD COLUMN moderated TINYINT NOT NULL DEFAULT 0 COMMENT '1=管理员已下架',
      ADD COLUMN report_count INT NOT NULL DEFAULT 0 COMMENT '举报次数（冗余）';
  END IF;

  -- 发帖前置审核状态：0=待审 1=已批准 2=已拒绝
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name = 'mall_market_post'
                   AND column_name = 'review_status') THEN
    ALTER TABLE mall_market_post
      ADD COLUMN review_status CHAR(1) NOT NULL DEFAULT '0'
        COMMENT '0=待审核 1=已批准 2=已拒绝',
      ADD COLUMN review_note VARCHAR(200) DEFAULT NULL
        COMMENT '拒绝原因';
  END IF;

  -- 评论表加 moderated 字段
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name = 'mall_market_comment'
                   AND column_name = 'moderated') THEN
    ALTER TABLE mall_market_comment
      ADD COLUMN moderated TINYINT NOT NULL DEFAULT 0 COMMENT '1=管理员已删除';
  END IF;

END$$

CALL v31_market_moderation()$$
DROP PROCEDURE IF EXISTS v31_market_moderation$$

DELIMITER ;
