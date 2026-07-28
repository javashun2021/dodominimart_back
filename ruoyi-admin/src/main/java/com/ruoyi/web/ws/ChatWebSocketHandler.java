package com.ruoyi.web.ws;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.mall.service.IMallChatService;

/**
 * 聊天 WS 处理器：入站帧 send/typing/read/ping，委派 {@link IMallChatService}。
 * 出站 message/read/typing 帧由 service 经 {@link ChatWsService} 推送；本处只回 ack/error。
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler
{
    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    @Autowired private ChatWsService wsService;
    @Autowired private IMallChatService chatService;
    @Autowired private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception
    {
        Long memberId = memberId(session);
        if (memberId == null) { session.close(CloseStatus.POLICY_VIOLATION); return; }
        wsService.register(memberId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status)
    {
        Long memberId = memberId(session);
        if (memberId != null) wsService.unregister(memberId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
    {
        Long me = memberId(session);
        if (me == null) return;
        try
        {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String type = root.path("type").asText("");
            switch (type)
            {
                case "send":
                    handleSend(session, me, root);
                    break;
                case "typing":
                {
                    Long convId = asLong(root.get("conversationId"));
                    if (convId != null) chatService.forwardTyping(me, convId);
                    break;
                }
                case "read":
                {
                    Long convId = asLong(root.get("conversationId"));
                    if (convId != null) chatService.markRead(me, convId);
                    break;
                }
                case "ping":
                    wsService.sendText(session, "{\"type\":\"pong\"}");
                    break;
                default:
                    break;
            }
        }
        catch (Exception e)
        {
            sendError(session, null, e.getMessage());
        }
    }

    private void handleSend(WebSocketSession session, Long me, JsonNode root)
    {
        String clientMsgId = text(root, "clientMsgId");
        try
        {
            Map<String, Object> dto = chatService.sendMessage(
                    me,
                    asLong(root.get("conversationId")),
                    asLong(root.get("targetMemberId")),
                    text(root, "contentType"),
                    text(root, "text"),
                    text(root, "imageUrl"),
                    text(root, "sticker"),
                    asLong(root.get("refPostId")),
                    clientMsgId);

            Map<String, Object> ack = new LinkedHashMap<>();
            ack.put("type", "ack");
            ack.put("clientMsgId", clientMsgId);
            ack.put("messageId", dto.get("messageId"));
            ack.put("conversationId", dto.get("conversationId"));
            ack.put("createdAt", dto.get("createdAt"));
            wsService.sendText(session, objectMapper.writeValueAsString(ack));
        }
        catch (Exception e)
        {
            sendError(session, clientMsgId, e.getMessage());
        }
    }

    private void sendError(WebSocketSession session, String clientMsgId, String msg)
    {
        try
        {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("type", "error");
            if (clientMsgId != null) err.put("clientMsgId", clientMsgId);
            err.put("message", msg != null ? msg : "error");
            wsService.sendText(session, objectMapper.writeValueAsString(err));
        }
        catch (Exception ignored) { }
    }

    private Long memberId(WebSocketSession session)
    {
        Object v = session.getAttributes().get("memberId");
        return v instanceof Long ? (Long) v : null;
    }

    private static String text(JsonNode root, String field)
    {
        JsonNode n = root.get(field);
        return (n == null || n.isNull()) ? null : n.asText();
    }

    private static Long asLong(JsonNode n)
    {
        if (n == null || n.isNull()) return null;
        if (n.isNumber()) return n.asLong();
        try { return Long.parseLong(n.asText().trim()); }
        catch (Exception e) { return null; }
    }
}
