-- ----------------------------
-- 订单明细记录商品原价（划线价），用于展示优惠了多少
-- price = 实付单价（限时价/拼团价/原价）；original_price = 下单时的商品原价
-- 老数据 original_price 为 NULL，前端/后台据此判断是否有优惠（NULL 或等于 price 则不展示划线）
-- 在生产库 dodominimart 执行
-- ----------------------------

ALTER TABLE mall_order_item
    ADD COLUMN original_price decimal(10,2) NULL COMMENT '商品原价（划线价，>= price）' AFTER price;
