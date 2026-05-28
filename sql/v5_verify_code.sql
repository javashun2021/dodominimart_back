-- 邮箱验证码表（Email OTP）
CREATE TABLE IF NOT EXISTS mall_verify_code (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  email       VARCHAR(100) NOT NULL,
  code        CHAR(6)      NOT NULL,
  expires_at  DATETIME     NOT NULL COMMENT '过期时间（create_time + 10分钟）',
  used        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '0未使用 1已使用',
  attempts    TINYINT      NOT NULL DEFAULT 0 COMMENT '错误尝试次数，达到5次自动作废',
  create_time DATETIME     NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_email_time (email, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮箱验证码';
