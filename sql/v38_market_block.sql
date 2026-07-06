-- C2C 市场：用户拉黑（Apple/Play 1.2 UGC 合规要求「能拉黑滥用用户」）
-- 拉黑后，blocker 在帖子列表 / 评论里不再看到 blocked 用户的内容。
CREATE TABLE IF NOT EXISTS mall_market_block (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  blocker_id  BIGINT      NOT NULL COMMENT '发起拉黑的会员',
  blocked_id  BIGINT      NOT NULL COMMENT '被拉黑的会员',
  create_time DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '拉黑时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_blocker_blocked (blocker_id, blocked_id),
  KEY idx_blocker (blocker_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='C2C市场-用户拉黑';
