-- v4: 邮箱注册 / 密码登录支持
ALTER TABLE mall_member
  ADD COLUMN password_hash VARCHAR(100) DEFAULT NULL COMMENT '密码哈希（邮箱注册用，BCrypt）';
