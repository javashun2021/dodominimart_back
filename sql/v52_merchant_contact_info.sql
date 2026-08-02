-- v52: 商户其它联系方式(contact_info) —— 区别于 phone，存 JSON
-- 例：{"messenger":"m.me/xxx","telegram":"@xxx","whatsapp":"+63...","viber":"+63...","wechat":"xxx"}
ALTER TABLE mall_merchant
    ADD COLUMN IF NOT EXISTS contact_info VARCHAR(1000) DEFAULT NULL
    COMMENT 'JSON 各类联系方式(messenger/telegram/whatsapp/viber/wechat 等)，区别于 phone';
