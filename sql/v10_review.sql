CREATE TABLE IF NOT EXISTS `mall_product_review` (
    `review_id`   BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`    BIGINT(20)   NOT NULL                COMMENT '订单ID',
    `product_id`  BIGINT(20)   NOT NULL                COMMENT '商品ID',
    `member_id`   BIGINT(20)   NOT NULL                COMMENT '评价会员ID',
    `score`       TINYINT(1)   NOT NULL DEFAULT 5       COMMENT '评分 1-5',
    `content`     VARCHAR(500)          DEFAULT NULL    COMMENT '评价文字',
    `images`      VARCHAR(1000)         DEFAULT NULL    COMMENT '图片 JSON 数组（最多3张）',
    `create_time` DATETIME              DEFAULT NULL    COMMENT '评价时间',
    PRIMARY KEY (`review_id`),
    UNIQUE KEY `uq_order_product` (`order_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价';
