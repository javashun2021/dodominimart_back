-- Phase 2: GCash支付 + 限时优惠 + 拼团
-- 执行前请确保已执行 ry_20190215.sql 和 mall.sql

-- ─────────────────────────────────────────────
-- 1. mall_order 新增支付字段
-- ─────────────────────────────────────────────
ALTER TABLE mall_order
    ADD COLUMN payment_method VARCHAR(10)   NOT NULL DEFAULT 'COD'    COMMENT '支付方式 COD/GCASH' AFTER remark,
    ADD COLUMN payment_status VARCHAR(10)   NOT NULL DEFAULT 'UNPAID'  COMMENT '支付状态 UNPAID/PAID/REFUNDED' AFTER payment_method,
    ADD COLUMN payment_no     VARCHAR(64)   DEFAULT NULL               COMMENT 'GCash Reference ID' AFTER payment_status,
    ADD COLUMN paid_amount    DECIMAL(10,2) DEFAULT NULL               COMMENT '实际支付金额' AFTER payment_no,
    ADD COLUMN payment_time   DATETIME      DEFAULT NULL               COMMENT '支付完成时间' AFTER paid_amount;

-- ─────────────────────────────────────────────
-- 2. 支付流水表
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS mall_payment_record (
    record_id   BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '支付记录ID',
    order_id    BIGINT(20)    NOT NULL                COMMENT '订单ID',
    member_id   BIGINT(20)    NOT NULL                COMMENT '会员ID',
    amount      DECIMAL(10,2) NOT NULL                COMMENT '支付金额',
    payment_no  VARCHAR(64)   DEFAULT NULL            COMMENT 'GCash Reference ID',
    gcash_raw   LONGTEXT      DEFAULT NULL            COMMENT '原始回调JSON',
    status      VARCHAR(10)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED/REFUNDED',
    create_time DATETIME      NOT NULL                COMMENT '创建时间',
    update_time DATETIME      NOT NULL                COMMENT '更新时间',
    PRIMARY KEY (record_id),
    UNIQUE  KEY uk_payment_no (payment_no),
    KEY         idx_order_id  (order_id),
    KEY         idx_member_id (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

-- ─────────────────────────────────────────────
-- 3. 限时优惠表
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS mall_flash_sale (
    sale_id     BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '活动ID',
    product_id  BIGINT(20)    NOT NULL                COMMENT '商品ID',
    title       VARCHAR(100)  NOT NULL                COMMENT '活动名称',
    flash_price DECIMAL(10,2) NOT NULL                COMMENT '活动价',
    stock_limit INT(11)       NOT NULL                COMMENT '活动限量',
    sold_count  INT(11)       NOT NULL DEFAULT 0      COMMENT '已售数量',
    per_limit   INT(11)       NOT NULL DEFAULT 1      COMMENT '每人限购数量',
    start_time  DATETIME      NOT NULL                COMMENT '开始时间',
    end_time    DATETIME      NOT NULL                COMMENT '结束时间',
    status      CHAR(1)       NOT NULL DEFAULT '0'    COMMENT '0未开始 1进行中 2已结束',
    create_time DATETIME      NOT NULL                COMMENT '创建时间',
    PRIMARY KEY (sale_id),
    KEY idx_product (product_id),
    KEY idx_status_time (status, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限时优惠活动表';

-- ─────────────────────────────────────────────
-- 4. 拼团活动表
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS mall_group_activity (
    activity_id    BIGINT(20)  NOT NULL AUTO_INCREMENT COMMENT '活动ID',
    product_id     BIGINT(20)  NOT NULL                COMMENT '商品ID',
    title          VARCHAR(100) NOT NULL               COMMENT '活动名称',
    min_group_size INT(11)     NOT NULL DEFAULT 2      COMMENT '最小成团人数',
    duration_hours INT(11)     NOT NULL DEFAULT 24     COMMENT '成团时限(小时)',
    start_time     DATETIME    NOT NULL                COMMENT '活动开始时间',
    end_time       DATETIME    NOT NULL                COMMENT '活动结束时间',
    status         CHAR(1)     NOT NULL DEFAULT '0'    COMMENT '0进行中 1已结束',
    create_time    DATETIME    NOT NULL                COMMENT '创建时间',
    PRIMARY KEY (activity_id),
    KEY idx_product (product_id),
    KEY idx_status_time (status, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团活动表';

-- ─────────────────────────────────────────────
-- 5. 拼团价格阶梯表
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS mall_group_tier (
    tier_id      BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '阶梯ID',
    activity_id  BIGINT(20)    NOT NULL                COMMENT '活动ID',
    min_quantity INT(11)       NOT NULL                COMMENT '达到该档最少人数',
    max_quantity INT(11)       DEFAULT NULL            COMMENT '该档最多人数，null表示无上限',
    price        DECIMAL(10,2) NOT NULL                COMMENT '该档单价',
    PRIMARY KEY (tier_id),
    KEY idx_activity (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团价格阶梯表';

-- ─────────────────────────────────────────────
-- 6. 拼团单表
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS mall_group_order (
    group_order_id      BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '拼团单ID',
    activity_id         BIGINT(20)    NOT NULL                COMMENT '活动ID',
    product_id          BIGINT(20)    NOT NULL                COMMENT '商品ID',
    initiator_member_id BIGINT(20)    NOT NULL                COMMENT '发起人会员ID',
    invite_code         VARCHAR(16)   NOT NULL                COMMENT '邀请码',
    current_size        INT(11)       NOT NULL DEFAULT 0      COMMENT '当前参与人数',
    current_price       DECIMAL(10,2) DEFAULT NULL            COMMENT '当前档位单价',
    status              CHAR(1)       NOT NULL DEFAULT '0'    COMMENT '0拼团中 1成功 2失败',
    expire_time         DATETIME      NOT NULL                COMMENT '过期时间',
    success_time        DATETIME      DEFAULT NULL            COMMENT '成团时间',
    create_time         DATETIME      NOT NULL                COMMENT '创建时间',
    PRIMARY KEY (group_order_id),
    UNIQUE KEY uk_invite_code (invite_code),
    KEY idx_activity (activity_id),
    KEY idx_initiator (initiator_member_id),
    KEY idx_status_expire (status, expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团单表';

-- ─────────────────────────────────────────────
-- 7. 拼团成员表
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS mall_group_member (
    id             BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    group_order_id BIGINT(20) NOT NULL                COMMENT '拼团单ID',
    member_id      BIGINT(20) NOT NULL                COMMENT '会员ID',
    quantity       INT(11)    NOT NULL DEFAULT 1      COMMENT '购买数量',
    order_id       BIGINT(20) DEFAULT NULL            COMMENT '成团后生成的订单ID',
    address_id     BIGINT(20) DEFAULT NULL            COMMENT '收货地址ID（成团后创建订单用）',
    joined_time    DATETIME   NOT NULL                COMMENT '加入时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_member (group_order_id, member_id),
    KEY idx_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团成员表';