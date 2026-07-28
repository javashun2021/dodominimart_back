-- ─────────────────────────────────────────────────────────────────────────────
-- v47 App 站内聊天（WebSocket 1:1 私聊，一期）
--   1. mall_chat_conversation  会话（一对成员一条，规范排序去重）
--   2. mall_chat_message        消息（文本/图片/表情，读回执，clientMsgId 幂等）
--   3. sys_config               app.chat.ws.url（App 从 /api/v1/config 读 WS 地址）
-- 在生产库(dodominimart)手动执行，幂等可重复跑。
-- ─────────────────────────────────────────────────────────────────────────────

-- ── 1. 会话表 ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS mall_chat_conversation (
    conversation_id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    member_a_id        BIGINT       NOT NULL COMMENT '成员A(较小memberId)',
    member_b_id        BIGINT       NOT NULL COMMENT '成员B(较大memberId)',
    origin_post_id     BIGINT       DEFAULT NULL COMMENT '发起来源的市场帖ID(可空)',
    last_message_text  VARCHAR(500) DEFAULT NULL COMMENT '最后一条预览(图片=[Photo] 表情=[Sticker])',
    last_message_time  DATETIME     DEFAULT NULL COMMENT '最后一条时间',
    a_unread           INT          NOT NULL DEFAULT 0 COMMENT 'A的未读数',
    b_unread           INT          NOT NULL DEFAULT 0 COMMENT 'B的未读数',
    a_deleted          CHAR(1)      NOT NULL DEFAULT '0' COMMENT 'A是否软隐藏(0否2是)',
    b_deleted          CHAR(1)      NOT NULL DEFAULT '0' COMMENT 'B是否软隐藏(0否2是)',
    create_time        DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time        DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (conversation_id),
    UNIQUE KEY uk_pair (member_a_id, member_b_id),
    KEY idx_a (member_a_id),
    KEY idx_b (member_b_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内聊天会话';

-- ── 2. 消息表 ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS mall_chat_message (
    message_id       BIGINT      NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    conversation_id  BIGINT      NOT NULL COMMENT '所属会话',
    sender_id        BIGINT      NOT NULL COMMENT '发送者memberId',
    recipient_id     BIGINT      NOT NULL COMMENT '接收者memberId(冗余,便于未读/推送)',
    client_msg_id    VARCHAR(64) DEFAULT NULL COMMENT '客户端生成ID(ack对齐/去重)',
    content_type     VARCHAR(16) NOT NULL DEFAULT 'text' COMMENT '类型: text/image/sticker',
    content          TEXT        COMMENT 'text=正文 sticker=code image=URL',
    ref_post_id      BIGINT      DEFAULT NULL COMMENT '引用的市场帖ID(可空)',
    is_read          TINYINT     NOT NULL DEFAULT 0 COMMENT '接收者是否已读',
    read_time        DATETIME    DEFAULT NULL COMMENT '已读时间',
    create_time      DATETIME    DEFAULT NULL COMMENT '发送时间',
    PRIMARY KEY (message_id),
    UNIQUE KEY uk_client_msg (conversation_id, sender_id, client_msg_id),
    KEY idx_conv_time (conversation_id, create_time),
    KEY idx_recip_unread (recipient_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内聊天消息';

-- ── 3. WS 地址配置（App 从 /api/v1/config 读；默认生产 wss 地址）──────────────
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '聊天WS地址', 'app.chat.ws.url', 'wss://dodominimart.com/ws/chat', 'Y', 'admin', NOW(),
       'App 站内聊天 WebSocket 地址，下发到 /api/v1/config 的 chatWsUrl。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'app.chat.ws.url');
