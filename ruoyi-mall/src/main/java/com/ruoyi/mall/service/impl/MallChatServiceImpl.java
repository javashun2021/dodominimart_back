package com.ruoyi.mall.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.mall.domain.MallChatConversation;
import com.ruoyi.mall.domain.MallChatMessage;
import com.ruoyi.mall.domain.MallMember;
import com.ruoyi.mall.mapper.MallChatConversationMapper;
import com.ruoyi.mall.mapper.MallChatMessageMapper;
import com.ruoyi.mall.mapper.MallMemberMapper;
import com.ruoyi.mall.service.FcmService;
import com.ruoyi.mall.service.IChatPushService;
import com.ruoyi.mall.service.IMallChatService;
import com.ruoyi.mall.service.IMallMarketService;

@Service
public class MallChatServiceImpl implements IMallChatService
{
    @Autowired private MallChatConversationMapper conversationMapper;
    @Autowired private MallChatMessageMapper messageMapper;
    @Autowired private MallMemberMapper memberMapper;
    @Autowired private IMallMarketService marketService;
    @Autowired private IChatPushService chatPush;
    @Autowired private FcmService fcmService;
    @Autowired private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    /** 同步帖机器人号：不可与其站内聊天 */
    @Value("${mall.market.sync.member-id:1001}")
    private Long syncBotId;

    // ── 会话 ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Long getOrCreateConversation(Long me, Long targetMemberId, Long originPostId)
    {
        if (me == null || targetMemberId == null) throw new RuntimeException("Invalid target");
        if (me.equals(targetMemberId)) throw new RuntimeException("You cannot chat with yourself");
        if (targetMemberId.equals(syncBotId)) throw new RuntimeException("This seller is not available for in-app chat");

        MallMember target = memberMapper.selectMemberById(targetMemberId);
        if (target == null) throw new RuntimeException("User not found");
        if ("1".equals(target.getStatus())) throw new RuntimeException("This user is unavailable");
        if (marketService.isBlocked(me, targetMemberId)) throw new RuntimeException("Messaging is unavailable with this user");

        Long a = Math.min(me, targetMemberId);
        Long b = Math.max(me, targetMemberId);
        MallChatConversation existing = conversationMapper.selectByPair(a, b);
        if (existing != null) return existing.getConversationId();

        MallChatConversation conv = new MallChatConversation();
        conv.setMemberAId(a);
        conv.setMemberBId(b);
        conv.setOriginPostId(originPostId);
        try
        {
            conversationMapper.insert(conv);
            return conv.getConversationId();
        }
        catch (DuplicateKeyException e)
        {
            // 并发下另一端已建，回查
            MallChatConversation again = conversationMapper.selectByPair(a, b);
            if (again != null) return again.getConversationId();
            throw e;
        }
    }

    // ── 发消息 ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Map<String, Object> sendMessage(Long senderId, Long conversationId, Long targetMemberId,
                                           String contentType, String text, String imageUrl, String sticker,
                                           Long refPostId, String clientMsgId)
    {
        if (senderId == null) throw new RuntimeException("Not authenticated");

        // 1. 解析会话与接收方
        MallChatConversation conv;
        if (conversationId != null)
        {
            conv = conversationMapper.selectById(conversationId);
            if (conv == null) throw new RuntimeException("Conversation not found");
            if (!isParticipant(conv, senderId)) throw new RuntimeException("Not a participant of this conversation");
        }
        else if (targetMemberId != null)
        {
            conversationId = getOrCreateConversation(senderId, targetMemberId, refPostId);
            conv = conversationMapper.selectById(conversationId);
        }
        else
        {
            throw new RuntimeException("conversationId or targetMemberId is required");
        }
        Long recipientId = otherOf(conv, senderId);

        // 2. 归一化类型 + 校验内容
        String type = (contentType == null || contentType.trim().isEmpty()) ? "text" : contentType.trim().toLowerCase();
        String content;
        switch (type)
        {
            case "text":
                if (text == null || text.trim().isEmpty()) throw new RuntimeException("Message text is empty");
                content = text.trim();
                break;
            case "image":
                if (imageUrl == null || imageUrl.trim().isEmpty()) throw new RuntimeException("Image URL is empty");
                content = imageUrl.trim();
                break;
            case "sticker":
                if (sticker == null || sticker.trim().isEmpty()) throw new RuntimeException("Sticker is empty");
                content = sticker.trim();
                break;
            default:
                throw new RuntimeException("Unsupported content type: " + type);
        }

        // 3. 拉黑门禁
        if (marketService.isBlocked(senderId, recipientId)) throw new RuntimeException("Messaging is unavailable with this user");

        // 4. 幂等：clientMsgId 已存在则直接返回原消息（不重复推送）
        if (clientMsgId != null && !clientMsgId.trim().isEmpty())
        {
            MallChatMessage dup = messageMapper.selectByClientMsgId(conversationId, senderId, clientMsgId.trim());
            if (dup != null) return toDto(dup);
        }

        // 5. 落库
        MallChatMessage msg = new MallChatMessage();
        msg.setConversationId(conversationId);
        msg.setSenderId(senderId);
        msg.setRecipientId(recipientId);
        msg.setClientMsgId(clientMsgId != null && !clientMsgId.trim().isEmpty() ? clientMsgId.trim() : null);
        msg.setContentType(type);
        msg.setContent(content);
        msg.setRefPostId(refPostId);
        msg.setCreateTime(new Date());
        try
        {
            messageMapper.insert(msg);
        }
        catch (DuplicateKeyException e)
        {
            MallChatMessage dup = messageMapper.selectByClientMsgId(conversationId, senderId, msg.getClientMsgId());
            if (dup != null) return toDto(dup);
            throw e;
        }

        // 6. 更新会话预览 + 接收方未读+1
        String preview = previewOf(type, content);
        conversationMapper.touchOnSend(conversationId, preview, msg.getCreateTime() != null ? msg.getCreateTime() : new Date(), recipientId);

        // 7. 投递
        Map<String, Object> dto = toDto(msg);
        deliver(recipientId, senderId, dto, preview, conversationId);
        return dto;
    }

    /** 在线走 WS，离线走 FCM（异步）。 */
    private void deliver(Long recipientId, Long senderId, Map<String, Object> dto, String preview, Long conversationId)
    {
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("type", "message");
        frame.put("message", dto);
        if (chatPush.isOnline(recipientId))
        {
            chatPush.pushToMember(recipientId, frame);
            return;
        }
        // 离线：FCM
        final Long rid = recipientId;
        final Long sid = senderId;
        threadPoolTaskExecutor.execute(() -> {
            try
            {
                MallMember recipient = memberMapper.selectMemberById(rid);
                if (recipient == null || recipient.getFcmToken() == null || recipient.getFcmToken().isEmpty()) return;
                MallMember sender = memberMapper.selectMemberById(sid);
                String title = (sender != null && sender.getNickName() != null) ? sender.getNickName() : "New message";
                Map<String, String> data = new LinkedHashMap<>();
                data.put("type", "chat");
                data.put("conversationId", String.valueOf(conversationId));
                fcmService.sendToToken(recipient.getFcmToken(), title, preview, data);
            }
            catch (Exception ignored) {}
        });
    }

    // ── 列表 / 历史 / 已读 / 未读 / typing ────────────────────────────────────

    @Override
    public List<Map<String, Object>> listConversations(Long me)
    {
        return conversationMapper.selectConversationsForMember(me);
    }

    @Override
    public List<Map<String, Object>> history(Long me, Long conversationId, Long beforeId, int limit)
    {
        MallChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) throw new RuntimeException("Conversation not found");
        if (!isParticipant(conv, me)) throw new RuntimeException("Not a participant of this conversation");
        if (limit <= 0 || limit > 100) limit = 30;
        List<MallChatMessage> msgs = messageMapper.selectHistory(conversationId, beforeId, limit);
        List<Map<String, Object>> out = new ArrayList<>(msgs.size());
        for (MallChatMessage m : msgs) out.add(toDto(m));
        return out;
    }

    @Override
    @Transactional
    public void markRead(Long me, Long conversationId)
    {
        MallChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) return;
        if (!isParticipant(conv, me)) throw new RuntimeException("Not a participant of this conversation");
        messageMapper.markRead(conversationId, me);
        conversationMapper.clearUnread(conversationId, me);
        // 推读回执给对方
        Long other = otherOf(conv, me);
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("type", "read");
        frame.put("conversationId", conversationId);
        frame.put("byMemberId", me);
        if (chatPush.isOnline(other)) chatPush.pushToMember(other, frame);
    }

    @Override
    public int unreadTotal(Long me)
    {
        return conversationMapper.selectUnreadTotal(me);
    }

    @Override
    public void forwardTyping(Long me, Long conversationId)
    {
        MallChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null || !isParticipant(conv, me)) return;
        Long other = otherOf(conv, me);
        if (!chatPush.isOnline(other)) return;
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("type", "typing");
        frame.put("conversationId", conversationId);
        frame.put("fromMemberId", me);
        chatPush.pushToMember(other, frame);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private boolean isParticipant(MallChatConversation conv, Long memberId)
    {
        return conv.getMemberAId().equals(memberId) || conv.getMemberBId().equals(memberId);
    }

    private Long otherOf(MallChatConversation conv, Long memberId)
    {
        return conv.getMemberAId().equals(memberId) ? conv.getMemberBId() : conv.getMemberAId();
    }

    private String previewOf(String type, String content)
    {
        if ("image".equals(type)) return "[Photo]";
        if ("sticker".equals(type)) return "[Sticker]";
        return content.length() > 500 ? content.substring(0, 500) : content;
    }

    /** 序列化成 App ChatMessage 契约。 */
    private Map<String, Object> toDto(MallChatMessage m)
    {
        String type = m.getContentType();
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", String.valueOf(m.getMessageId()));
        dto.put("messageId", m.getMessageId());
        dto.put("clientMsgId", m.getClientMsgId());
        dto.put("conversationId", m.getConversationId());
        dto.put("senderId", m.getSenderId());
        dto.put("contentType", type);
        dto.put("text", "text".equals(type) ? m.getContent() : null);
        dto.put("sticker", "sticker".equals(type) ? m.getContent() : null);
        dto.put("imageUrl", "image".equals(type) ? m.getContent() : null);
        dto.put("refPostId", m.getRefPostId());
        dto.put("createdAt", m.getCreateTime() != null
                ? new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(m.getCreateTime()) : null);
        return dto;
    }
}
