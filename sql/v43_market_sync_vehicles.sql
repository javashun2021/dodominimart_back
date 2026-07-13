-- v43: 二手市场同步新增「汽车二手」来源，固定归到 Vehicles 分类
--   mall.market.sync.api-url-vehicles：汽车二手来源接口地址（不含 page，程序翻页时追加）。
--   该来源导入的帖子统一 category=Vehicles，去重用独立 source=ext-veh，不影响已导入的 ext 数据。
-- 幂等，可重复执行。

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '市场同步-汽车来源', 'mall.market.sync.api-url-vehicles',
       'https://www.flw.ph/plugin.php?id=zimu_fenlei&model=list&ids=4&cid1=0', 'Y', 'admin', NOW(),
       '汽车二手来源接口地址；该来源帖子固定归到 Vehicles 分类（去重 source=ext-veh）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'mall.market.sync.api-url-vehicles');
