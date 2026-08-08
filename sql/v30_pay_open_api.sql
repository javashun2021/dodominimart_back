-- ============================================================
-- v30 聚合支付：下游商户开放收款 API 订单表 pay_order
-- 依赖：imspay_merchant(商户/密钥)、order_block_info(拉黑) 已导入
-- 下游协议参照 亿林/直付通 标准（api.txt）
-- ============================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `pay_order` (
  `id`               bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '主键',
  `platform_no`      varchar(64)   NOT NULL                COMMENT '平台订单号(payOrderNo, OL...)',
  `merchant_id`      varchar(36)   DEFAULT NULL            COMMENT '商户id(imspay_merchant.id)',
  `merchant_code`    varchar(64)   NOT NULL                COMMENT '商户号/站点码(imspay_merchant.code)',
  `channel_id`       varchar(32)   DEFAULT NULL            COMMENT '通道编码(Request-Channel-Id)',
  `out_trade_no`     varchar(64)   NOT NULL                COMMENT '商户订单号(orderNo)',
  `amount`           decimal(12,2) NOT NULL                COMMENT '订单金额(元)',
  `currency`         varchar(8)    DEFAULT 'CNY'           COMMENT '币种',
  `user_id`          varchar(64)   DEFAULT NULL            COMMENT '会员id',
  `client_ip`        varchar(64)   DEFAULT NULL            COMMENT '会员ip',
  `extra`            varchar(256)  DEFAULT NULL            COMMENT '备注/透传',
  `notify_url`       varchar(500)  DEFAULT NULL            COMMENT '商户异步回调地址',
  `request_id`       varchar(64)   DEFAULT NULL            COMMENT '请求随机串(Request-Id)',
  `pay_url`          text          DEFAULT NULL            COMMENT '上游返回的支付链接',
  `upstream_no`      varchar(64)   DEFAULT NULL            COMMENT '上游(MOSS)订单号/流水',
  `status`           varchar(16)   NOT NULL DEFAULT 'CREATED' COMMENT '状态: CREATED/PAID/CLOSED',
  `notify_status`    tinyint(4)    NOT NULL DEFAULT 0      COMMENT '回调状态: 0未通知 1成功 2失败',
  `notify_count`     int(11)       NOT NULL DEFAULT 0      COMMENT '回调次数',
  `last_notify_time` datetime      DEFAULT NULL            COMMENT '最后回调时间',
  `upstream_raw`     text          DEFAULT NULL            COMMENT '上游回调原文',
  `create_time`      datetime      DEFAULT NULL            COMMENT '创建时间',
  `update_time`      datetime      DEFAULT NULL            COMMENT '更新时间',
  `pay_time`         datetime      DEFAULT NULL            COMMENT '支付成功时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_no`        (`platform_no`),
  UNIQUE KEY `uk_merchant_out_trade` (`merchant_code`,`out_trade_no`),
  KEY `idx_status_notify` (`status`,`notify_status`),
  KEY `idx_create_time`   (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聚合支付-下游商户订单';
