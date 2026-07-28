package com.ruoyi.web.ws;

import java.net.URLDecoder;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.ruoyi.framework.jwt.JwtUtils;

/**
 * WS 握手拦截：从 ?token=<jwt> 校验并把 memberId 放进握手 attributes。
 * （WS 客户端常无法自定义 Header，故与 SSE 一样走 query param；Shiro 该路径 anon。）
 */
@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor
{
    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes)
    {
        String token = extractToken(request);
        if (token == null || token.isEmpty() || !jwtUtils.validateToken(token))
        {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        Long memberId = jwtUtils.getMemberIdFromToken(token);
        if (memberId == null)
        {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put("memberId", memberId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) { }

    private String extractToken(ServerHttpRequest request)
    {
        String query = request.getURI().getQuery();
        if (query == null) return null;
        for (String pair : query.split("&"))
        {
            if (pair.startsWith("token="))
            {
                try { return URLDecoder.decode(pair.substring("token=".length()), "UTF-8"); }
                catch (Exception e) { return pair.substring("token=".length()); }
            }
        }
        return null;
    }
}
