-- ----------------------------
-- App 客户端错误上报表（/api/v1/app/error-log 写入，后台「App错误日志」查看）
-- ----------------------------
DROP TABLE IF EXISTS app_error_log;
CREATE TABLE app_error_log (
  id           BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '主键',
  platform     VARCHAR(20)   DEFAULT ''   COMMENT '平台 android/ios/web',
  app_version  VARCHAR(50)   DEFAULT ''   COMMENT 'App 版本 如 1.2.0+8',
  type         VARCHAR(20)   DEFAULT ''   COMMENT '类型 flutter/async/manual',
  message      TEXT DEFAULT ''   COMMENT '错误摘要',
  stack        TEXT          NULL         COMMENT '完整堆栈',
  context      TEXT DEFAULT ''   COMMENT '上下文',
  member_id    BIGINT(20)    NULL         COMMENT '上报会员ID（匿名为空）',
  ip           VARCHAR(64)   DEFAULT ''   COMMENT '客户端IP',
  client_time  VARCHAR(40)   DEFAULT ''   COMMENT 'App 上报时间(原始字符串)',
  create_time  DATETIME      NULL         COMMENT '服务端接收时间',
  PRIMARY KEY (id),
  KEY idx_create_time (create_time),
  KEY idx_type (type),
  KEY idx_platform (platform),
  KEY idx_member (member_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='App客户端错误日志';
