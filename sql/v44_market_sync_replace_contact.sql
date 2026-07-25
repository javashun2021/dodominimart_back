-- v44: 二手市场同步「联系人替换」开关。true=清洗联系方式+官方电话覆盖（原行为）；false=暂不替换，保留卖家原始联系方式。
-- 当前先设 false（暂停替换），想恢复改成 true 即可（改配置即时生效，无需发版）。
-- 幂等，可重复执行。
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '市场同步-联系人替换', 'mall.market.sync.replace-contact', 'false', 'Y', 'admin', NOW(),
       'true=清洗正文联系方式并用官方电话覆盖；false=暂不替换，保留卖家原始联系方式。改配置即时生效'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'mall.market.sync.replace-contact');
