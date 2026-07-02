-- v35: 市场联系方式配置项
INSERT IGNORE INTO sys_config (config_name, config_key, config_value, config_type, remark)
VALUES ('市场联系方式列表', 'mall.market.contact.methods',
        'phone,whatsapp,messenger,telegram,viber,wechat',
        'Y', '逗号分隔的联系方式 key，顺序即显示顺序');
