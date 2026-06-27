-- 扩展 sys_user.login_ip 长度：50 -> 150
-- 兼容 IPv6 / 经代理转发的多段 IP（X-Forwarded-For 串接）等较长字符串
ALTER TABLE sys_user MODIFY COLUMN login_ip VARCHAR(150) DEFAULT '' COMMENT '最后登陆IP';