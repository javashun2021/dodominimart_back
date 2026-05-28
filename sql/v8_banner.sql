CREATE TABLE IF NOT EXISTS `mall_banner` (
    `banner_id`   BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `image_url`   VARCHAR(512) NOT NULL                COMMENT '图片地址',
    `link_type`   VARCHAR(20)  NOT NULL DEFAULT 'NONE' COMMENT '跳转类型: NONE/PRODUCT/GROUP/URL',
    `link_value`  VARCHAR(512)          DEFAULT NULL   COMMENT '跳转值（productId / inviteCode / url）',
    `sort`        INT(4)       NOT NULL DEFAULT 0       COMMENT '排序（小→前）',
    `status`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态（0启用 1停用）',
    `create_time` DATETIME              DEFAULT NULL    COMMENT '创建时间',
    `update_time` DATETIME              DEFAULT NULL    COMMENT '更新时间',
    PRIMARY KEY (`banner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页轮播图';

-- 示例数据（删掉或替换为真实图片 URL）
INSERT INTO `mall_banner` (`image_url`, `link_type`, `link_value`, `sort`, `status`, `create_time`)
VALUES
('https://via.placeholder.com/800x300/E85D04/FFFFFF?text=DODO+MiniMart', 'NONE', NULL, 1, '0', NOW()),
('https://via.placeholder.com/800x300/C2410C/FFFFFF?text=Flash+Sale', 'NONE', NULL, 2, '0', NOW()),
('https://via.placeholder.com/800x300/FF8540/FFFFFF?text=Group+Buy', 'NONE', NULL, 3, '0', NOW());
