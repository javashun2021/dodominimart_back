-- v11: 积分系统
ALTER TABLE mall_member
    ADD COLUMN points INT NOT NULL DEFAULT 0 COMMENT '积分余额';

ALTER TABLE mall_order
    ADD COLUMN points_used INT NOT NULL DEFAULT 0 COMMENT '本单使用积分数';

CREATE TABLE IF NOT EXISTS mall_points_log (
    log_id       BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    member_id    BIGINT(20)   NOT NULL                COMMENT '会员ID',
    delta        INT          NOT NULL                COMMENT '变动值（正=获得 负=消耗）',
    balance_after INT         NOT NULL                COMMENT '变动后余额',
    source       TINYINT(1)   NOT NULL                COMMENT '1=订单完成 2=评价奖励 3=注册赠送 4=积分抵扣',
    source_id    VARCHAR(64)  DEFAULT NULL            COMMENT '来源ID（订单ID等）',
    remark       VARCHAR(200) DEFAULT NULL            COMMENT '备注',
    create_time  DATETIME     DEFAULT NULL            COMMENT '记录时间',
    PRIMARY KEY (log_id),
    INDEX idx_member_time (member_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分流水';
