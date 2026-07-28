package com.ruoyi.web.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 聊天 WebSocket 注册：/ws/chat，握手校验 JWT。
 */
@Configuration
@EnableWebSocket
public class ChatWebSocketConfig implements WebSocketConfigurer
{
    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Autowired
    private ChatHandshakeInterceptor chatHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry)
    {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(chatHandshakeInterceptor)
                .setAllowedOrigins("*");
    }

    /**
     * 显式提供一个真实 TaskScheduler。
     * 加 @EnableWebSocket 后会注册一个不使用 SockJS 时为 NullBean 的 defaultSockJsTaskScheduler，
     * 会导致 @Scheduled 按类型解析 TaskScheduler 失败（BeanNotOfRequiredTypeException）。
     * 用 @Primary 的这个覆盖之，@Scheduled（SseService/ChatWsService 心跳）走它。
     */
    @Bean
    @Primary
    public TaskScheduler taskScheduler()
    {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("app-scheduler-");
        scheduler.initialize();
        return scheduler;
    }
}
