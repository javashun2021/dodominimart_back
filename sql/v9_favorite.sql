CREATE TABLE IF NOT EXISTS `mall_favorite` (
    `member_id`   BIGINT(20) NOT NULL COMMENT '会员ID',
    `product_id`  BIGINT(20) NOT NULL COMMENT '商品ID',
    `create_time` DATETIME   DEFAULT NULL COMMENT '收藏时间',
    PRIMARY KEY (`member_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品收藏';
