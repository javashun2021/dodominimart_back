-- v41: 二手市场同步帖「价格加价比例」改为可配置
--   mall.market.sync.price-markup：0 或不配 = 不加价；0.1 = 加价 10%；0.2 = 加价 20% …
--   最终价 = 原价 × (1 + 该值)。默认 0（不加价），需要加价时在后台改成 0.1 等。
-- 幂等，可重复执行。

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '市场同步-加价比例', 'mall.market.sync.price-markup', '0', 'Y', 'admin', NOW(),
       '同步帖价格加价比例：0=不加价，0.1=+10%，0.2=+20%；最终价=原价×(1+该值)'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'mall.market.sync.price-markup');
