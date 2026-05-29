-- v16: 优惠券系统
CREATE TABLE `mall_coupon` (
  `coupon_id`           BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name`                VARCHAR(100)  NOT NULL,
  `type`                VARCHAR(20)   NOT NULL COMMENT 'free_delivery|amount_off|percent_off',
  `discount_amount`     DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'amount_off 直减金额',
  `discount_percent`    INT           NOT NULL DEFAULT 0    COMMENT 'percent_off 百分比 0-100',
  `max_discount_amount` DECIMAL(10,2) DEFAULT NULL          COMMENT 'percent_off 封顶金额',
  `min_order_amount`    DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '最低使用门槛',
  `valid_days`          INT           NOT NULL DEFAULT 30   COMMENT '发放后有效天数',
  `is_first_order_only` TINYINT(1)    NOT NULL DEFAULT 0    COMMENT '1=仅首单可用',
  `status`              TINYINT(1)    NOT NULL DEFAULT 0    COMMENT '0启用 1停用',
  `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板';

CREATE TABLE `mall_member_coupon` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
  `coupon_id`   BIGINT       NOT NULL,
  `member_id`   BIGINT       NOT NULL,
  `status`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '0未使用 1已使用 2已过期',
  `expires_at`  DATETIME     NOT NULL,
  `order_no`    VARCHAR(64)  DEFAULT NULL,
  `used_time`   DATETIME     DEFAULT NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_member (`member_id`, `status`),
  INDEX idx_coupon (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员持有优惠券';

ALTER TABLE mall_order
  ADD COLUMN `member_coupon_id` BIGINT        DEFAULT NULL          COMMENT '使用的优惠券实例ID',
  ADD COLUMN `coupon_discount`  DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠券减免金额';

-- 默认新人券三联
INSERT INTO mall_coupon (name, type, discount_amount, discount_percent, max_discount_amount, min_order_amount, valid_days, is_first_order_only)
VALUES
  ('Free Delivery',       'free_delivery', 0.00,  0,  NULL,  0.00,  30, 0),
  ('New User ₱30 Off',    'amount_off',   30.00,  0,  NULL, 150.00, 30, 0),
  ('First Order 15% Off', 'percent_off',   0.00, 15, 50.00, 200.00, 30, 1);
