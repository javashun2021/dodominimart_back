package com.ruoyi.web.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.ruoyi.framework.jwt.JwtUtils;
import com.ruoyi.web.sse.SseService;

/**
 * SSE 推送订阅（Web 端替代 FCM）
 * GET /api/v1/sse/subscribe?token=<jwt>
 *
 * 浏览器 EventSource 不支持自定义 Header，JWT 通过 query param 传入。
 * Shiro 该路径设为 anon，Controller 内部手动验证 token。
 */
@RestController
@RequestMapping("/api/v1/sse")
public class ApiSseController
{
    @Autowired
    private SseService sseService;

    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam(required = false) String token)
    {
        if (token == null || token.isEmpty())
        {
            throw new RuntimeException("缺少登录令牌");
        }
        if (!jwtUtils.validateToken(token))
        {
            throw new RuntimeException("登录令牌无效或已过期");
        }
        Long memberId = jwtUtils.getMemberIdFromToken(token);
        return sseService.subscribe(memberId);
    }
}
