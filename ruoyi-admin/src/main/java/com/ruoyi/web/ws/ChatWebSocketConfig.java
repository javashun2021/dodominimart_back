package com.ruoyi.web.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
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
}
