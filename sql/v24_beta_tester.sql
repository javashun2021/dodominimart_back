-- v24: Android 封闭测试「内测申请」表
-- 配合公开下载页 https://dodominimart.com/download.html 使用：
--   顾客在网页提交 Gmail -> 落库 pending + 纸飞机通知管理员
--   管理员在 Play Console 加完邮箱 -> 点纸飞机里的审批链接 -> status=approved
--   顾客回下载页查状态 -> approved 即可下载
CREATE TABLE IF NOT EXISTS mall_beta_tester (
  id            bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  email         varchar(120) NOT NULL                COMMENT '测试者 Gmail（小写存储）',
  status        varchar(16)  NOT NULL DEFAULT 'pending' COMMENT '状态 pending/approved',
  source        varchar(32)           DEFAULT 'web'   COMMENT '申请来源',
  member_id     bigint(20)            DEFAULT NULL    COMMENT '已登录会员ID（可空）',
  ip            varchar(64)           DEFAULT NULL    COMMENT '申请IP',
  create_time   datetime              DEFAULT NULL    COMMENT '申请时间',
  approve_time  datetime              DEFAULT NULL    COMMENT '审批通过时间',
  remark        varchar(255)          DEFAULT NULL    COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_beta_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Android 内测申请';
