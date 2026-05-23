-- v3 跑腿功能迁移脚本

-- 1. 跑腿资格申请表
CREATE TABLE IF NOT EXISTS mall_runner_application (
    app_id          BIGINT(20)   NOT NULL AUTO_INCREMENT,
    member_id       BIGINT(20)   NOT NULL              COMMENT '申请人会员ID',
    real_name       VARCHAR(50)  NOT NULL              COMMENT '真实姓名',
    id_number       VARCHAR(30)  NOT NULL              COMMENT '证件号码',
    phone           VARCHAR(20)  NOT NULL              COMMENT '联系电话',
    id_photo_url    VARCHAR(255) DEFAULT NULL          COMMENT '证件照片路径',
    status          CHAR(1)      NOT NULL DEFAULT '0'  COMMENT '0待审核 1通过 2拒绝',
    reject_reason   VARCHAR(200) DEFAULT NULL          COMMENT '拒绝原因',
    apply_time      DATETIME     NOT NULL              COMMENT '申请时间',
    review_time     DATETIME     DEFAULT NULL          COMMENT '审核时间',
    reviewer        VARCHAR(50)  DEFAULT NULL          COMMENT '审核人',
    PRIMARY KEY (app_id),
    UNIQUE KEY uk_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跑腿资格申请表';

-- 2. mall_order 新增跑腿字段
ALTER TABLE mall_order
    ADD COLUMN runner_member_id     BIGINT(20)    DEFAULT NULL COMMENT '接单跑腿人ID',
    ADD COLUMN runner_accepted_time DATETIME      DEFAULT NULL COMMENT '接单时间',
    ADD COLUMN delivery_fee         DECIMAL(10,2) DEFAULT 0   COMMENT '跑腿费（接单时写入）',
    ADD COLUMN runner_fee_settled   CHAR(1)       DEFAULT '0' COMMENT '跑腿费是否已结算 0否1是（仅GCash订单需关注）',
    ADD INDEX  idx_runner_member (runner_member_id);

-- 3. 跑腿评价表
CREATE TABLE IF NOT EXISTS mall_runner_rating (
    rating_id        BIGINT(20)   NOT NULL AUTO_INCREMENT,
    order_id         BIGINT(20)   NOT NULL              COMMENT '订单ID',
    runner_member_id BIGINT(20)   NOT NULL              COMMENT '被评跑腿人',
    rater_member_id  BIGINT(20)   NOT NULL              COMMENT '评价人（顾客）',
    score            TINYINT      NOT NULL              COMMENT '评分 1-5',
    comment          VARCHAR(200) DEFAULT NULL          COMMENT '评价文字',
    create_time      DATETIME     NOT NULL,
    PRIMARY KEY (rating_id),
    UNIQUE KEY uk_order_rating (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跑腿评价表';
