package com.ruoyi.web.ws;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.mall.service.IChatPushService;

/**
 * 聊天 WS 会话注册表 + 在线投递。实现 mall 侧 {@link IChatPushService}，
 * 让 mall 业务层无 Web 依赖地推送。单实例内存保存（照抄 SseService 结构）。
 */
@Service
public class ChatWsService implements IChatPushService
{
    private static final Logger log = LoggerFactory.getLogger(ChatWsService.class);

    /** memberId → 该会员的所有在线连接（多设备） */
    private final Map<Long, CopyOnWriteArrayList<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    @Autowired
    private ObjectMapper objectMapper;

    public void register(Long memberId, WebSocketSession session)
    {
        sessions.computeIfAbsent(memberId, k -> new CopyOnWriteArrayList<>()).add(session);
        log.debug("WS chat connected: memberId={} conns={}", memberId, sessions.get(memberId).size());
    }

    public void unregister(Long memberId, WebSocketSession session)
    {
        CopyOnWriteArrayList<WebSocketSession> list = sessions.get(memberId);
        if (list != null)
        {
            list.remove(session);
            if (list.isEmpty()) sessions.remove(memberId);
        }
    }

    @Override
    public boolean isOnline(Long memberId)
    {
        CopyOnWriteArrayList<WebSocketSession> list = sessions.get(memberId);
        return list != null && !list.isEmpty();
    }

    @Override
    public void pushToMember(Long memberId, Object frame)
    {
        CopyOnWriteArrayList<WebSocketSession> list = sessions.get(memberId);
        if (list == null || list.isEmpty()) return;
        String json;
        try { json = objectMapper.writeValueAsString(frame); }
        catch (Exception e) { log.warn("WS chat serialize failed: {}", e.getMessage()); return; }
        for (WebSocketSession s : list) sendText(s, json);
    }

    /** 线程安全地发送文本帧（WebSocketSession.sendMessage 非并发安全，需同步）。 */
    public void sendText(WebSocketSession session, String json)
    {
        if (session == null || !session.isOpen()) return;
        try
        {
            synchronized (session) { session.sendMessage(new TextMessage(json)); }
        }
        catch (Exception e)
        {
            log.debug("WS chat send failed, dropping: {}", e.getMessage());
        }
    }

    /** 心跳：周期 ping，清理死连接（保活穿过反代）。 */
    @Scheduled(fixedDelay = 30000)
    public void heartbeat()
    {
        sessions.forEach((memberId, list) -> {
            for (WebSocketSession s : list)
            {
                if (!s.isOpen()) { list.remove(s); continue; }
                try { synchronized (s) { s.sendMessage(new PingMessage()); } }
                catch (Exception e) { list.remove(s); }
            }
            if (list.isEmpty()) sessions.remove(memberId);
        });
    }
}
