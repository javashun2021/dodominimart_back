package com.ruoyi.mall.service;

import java.util.List;
import java.util.Map;

/**
 * 站内聊天业务（会员 ↔ 会员 1:1 私聊）。
 * WS 帧与 REST 都收口到这里；返回的 message DTO 形状与 App ChatMessage 契约一致。
 */
public interface IMallChatService
{
    /** 取或建与 targetMemberId 的会话；含自聊/机器人/拉黑/目标有效性守卫，违规抛异常。 */
    Long getOrCreateConversation(Long me, Long targetMemberId, Long originPostId);

    /**
     * 发消息（WS send / REST 同一入口）。conversationId 与 targetMemberId 二选一。
     * 幂等：同一 (conversationId, senderId, clientMsgId) 已存在则直接返回原消息。
     * @return App 契约的 message DTO（id/messageId/clientMsgId/conversationId/senderId/contentType/text/sticker/imageUrl/createdAt）
     */
    Map<String, Object> sendMessage(Long senderId, Long conversationId, Long targetMemberId,
                                    String contentType, String text, String imageUrl, String sticker,
                                    Long refPostId, String clientMsgId);

    /** 我的会话列表（对方昵称/头像、我的未读、最后预览）。 */
    List<Map<String, Object>> listConversations(Long me);

    /** 历史消息（message_id 逆序翻页），每项=message DTO。 */
    List<Map<String, Object>> history(Long me, Long conversationId, Long beforeId, int limit);

    /** 标记会话已读：清我方未读 + 消息 is_read=1 + 推读回执给对方。 */
    void markRead(Long me, Long conversationId);

    /** 我的全部未读总数（App 角标）。 */
    int unreadTotal(Long me);

    /** 转发「正在输入」给会话对方。 */
    void forwardTyping(Long me, Long conversationId);
}
