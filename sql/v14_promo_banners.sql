-- v14: 将首页三张轮播图更新为三个促销落地页入口
-- 同时更新 link_type 字段注释以包含 PAGE 类型

-- 1. 更新字段注释
ALTER TABLE `mall_banner`
    MODIFY COLUMN `link_type` VARCHAR(20) NOT NULL DEFAULT 'NONE'
        COMMENT '跳转类型: NONE/PRODUCT/GROUP/URL/PAGE';

-- 2. 清空旧的占位横幅，重新插入三条正式横幅
DELETE FROM `mall_banner`;

INSERT INTO `mall_banner` (`image_url`, `link_type`, `link_value`, `sort`, `status`, `create_time`)
VALUES
-- 新人专享：橙色主题
(
    '/profile/upload/2026/05/30/717e636e27718a4ed01d713c4c8aecd0.jpg',
    'PAGE',
    '/promo/new-user',
    1,
    '0',
    NOW()
),
-- 今日特惠：红色主题
(
    '/profile/upload/2026/05/30/3cd05bb7ac3069599f9ef64623afa2c4.jpg',
    'PAGE',
    '/promo/deals',
    2,
    '0',
    NOW()
),
-- 会员权益：紫色主题
(
    '/profile/upload/2026/05/30/b86304719dfe6841452e0950868305ef.jpg',
    'PAGE',
    '/promo/members',
    3,
    '0',
    NOW()
);

-- 验证
SELECT banner_id, link_type, link_value, sort, status FROM mall_banner ORDER BY sort;
