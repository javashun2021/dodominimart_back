-- FCM 设备推送令牌字段
ALTER TABLE mall_member
    ADD COLUMN fcm_token VARCHAR(512) DEFAULT NULL COMMENT 'FCM 设备推送令牌';
