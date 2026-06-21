package com.ruoyi.web.sse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

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

    // 全局线程池（建议单例）
    private static final ScheduledExecutorService HEARTBEAT_POOL =
            Executors.newScheduledThreadPool(4);


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

        // ✅ 建立连接时返回一次
        try
        {
            emitter.send(SseEmitter.event()
                    .comment("connected")
                    .reconnectTime(5000));
        }
        catch (IOException ignored) {}

        // ✅ ⭐ 核心：心跳任务
        ScheduledFuture<?> heartbeatTask = HEARTBEAT_POOL.scheduleAtFixedRate(() -> {
            try
            {
                emitter.send(SseEmitter.event()
                        .comment("heartbeat")); // 不影响前端业务
            }
            catch (IOException e)
            {
                // 发送失败说明连接断了
                emitter.complete();
            }
        }, 20, 20, TimeUnit.SECONDS); // 每20秒一次

        // ✅ 连接关闭时，取消心跳
        Runnable finalCleanup = () -> {
            cleanup.run();
            heartbeatTask.cancel(true);
        };

        emitter.onCompletion(finalCleanup);
        emitter.onTimeout(finalCleanup);
        emitter.onError(e -> finalCleanup.run());

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
