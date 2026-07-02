-- v37: 市场帖子收藏

CREATE TABLE IF NOT EXISTS mall_market_favorite (
  favorite_id BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
  member_id   BIGINT   NOT NULL,
  post_id     BIGINT   NOT NULL,
  create_time DATETIME,
  UNIQUE KEY uk_member_post (member_id, post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='市场帖子收藏';
