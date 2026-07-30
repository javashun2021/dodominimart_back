-- v50 平台自营商家标识：把 DODOMINIMART(merchant_id=17) 认定为「平台自家门店」
-- 平台化后 DodoMiniMart 以 mall_merchant 行(17)存在。此参数让后端把它当自营处理：
--   1. /api/v1/products 首页/目录 = 该商家商品(替代原 merchant_id IS NULL，避免自营目录变空)
--   2. 下单时该商家的单按自营走：可在线支付(GCash)、归属自营网点(store_id)、订单不打商家标记
-- 改这个值即可切换「自家门店」是哪个商家(如将来换主体)。后台「系统管理→参数设置」可改。
-- 注意 mall.self.merchant.id 的值必须与 v49 实际插入的 merchant_id 一致(生产已确认=17)。
-- 幂等：NOT EXISTS 防重复。生产库手动执行。

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '平台自营商家ID', 'mall.self.merchant.id', '17', 'Y', 'admin', NOW(),
       'DodoMiniMart 自家门店对应的 mall_merchant.merchant_id。其商品=App首页/自营目录，其订单按自营处理(可在线支付/走自营网点)。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'mall.self.merchant.id');
