package com.ruoyi.mall.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Telegram（纸飞机）后台提醒：下单 / 接单 / 拼团 / 新注册 等事件推送到管理群。
 * 异步发送、失败只记日志，绝不影响主业务流程；未配置 token/chatId 时静默跳过。
 *
 * 配置见 application.yml 的 mall.telegram.*
 */
@Service
public class TelegramNotifyService
{
    private static final Logger log = LoggerFactory.getLogger(TelegramNotifyService.class);

    @Value("${mall.telegram.bot-token:}")
    private String botToken;

    @Value("${mall.telegram.chat-id:}")
    private String chatId;

    @Value("${mall.telegram.enabled:true}")
    private boolean enabled;

    /** 单线程后台队列：提醒按序异步发出，不阻塞业务请求 */
    private final ExecutorService pool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "telegram-notify");
        t.setDaemon(true);
        return t;
    });

    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate()
    {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(8_000);
        return new RestTemplate(factory);
    }

    /** 异步推送一条文本提醒到管理群。失败只记 warn，绝不抛异常。 */
    public void notify(String text)
    {
        if (!enabled
                || botToken == null || botToken.isEmpty()
                || chatId == null || chatId.isEmpty()
                || text == null || text.isEmpty())
        {
            return;
        }
        pool.submit(() -> {
            try
            {
                String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
                Map<String, Object> payload = new HashMap<>();
                payload.put("chat_id", chatId);
                payload.put("text", text);
                payload.put("disable_web_page_preview", true);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
            }
            catch (Exception e)
            {
                log.warn("Telegram notify failed: {}", e.getMessage());
            }
        });
    }
}
