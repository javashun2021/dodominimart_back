package com.ruoyi.web.sse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.ruoyi.mall.service.ISsePushService;

@Service
public class SseService implements ISsePushService
{
    private static final Logger log = LoggerFactory.getLogger(SseService.class);

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long memberId)
    {
        SseEmitter emitter = new SseEmitter(0L);
        CopyOnWriteArrayList<SseEmitter> list =
                emitters.computeIfAbsent(memberId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);

        Runnable cleanup = () -> list.remove(emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        try
        {
            emitter.send(SseEmitter.event().comment("connected").reconnectTime(5000));
        }
        catch (IOException ignored) {}

        log.debug("SSE subscribed: memberId={} connections={}", memberId, list.size());
        return emitter;
    }

    @Override
    public void push(Long memberId, String event, Object data)
    {
        List<SseEmitter> list = emitters.getOrDefault(memberId, new CopyOnWriteArrayList<>());
        if (list.isEmpty()) return;

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : list)
        {
            try
            {
                emitter.send(SseEmitter.event()
                        .name(event)
                        .data(data, MediaType.APPLICATION_JSON));
            }
            catch (Exception e)
            {
                dead.add(emitter);
            }
        }
        list.removeAll(dead);
    }

    @Scheduled(fixedDelay = 25000)
    public void heartbeat()
    {
        emitters.forEach((memberId, list) -> {
            List<SseEmitter> dead = new ArrayList<>();
            for (SseEmitter emitter : list)
            {
                try { emitter.send(SseEmitter.event().comment("ping")); }
                catch (Exception e) { dead.add(emitter); }
            }
            list.removeAll(dead);
            if (list.isEmpty()) emitters.remove(memberId);
        });
    }
}
