package com.ruoyi.mall.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

@Service
public class FcmService
{
    private static final Logger log = LoggerFactory.getLogger(FcmService.class);

    private boolean initialized = false;

    /**
     * 总开关。国内部署（谷歌被墙）应置 false，彻底跳过 FCM，避免任何外网调用。
     * 配置项：mall.fcm.enabled（默认 true，兼容原有海外部署）。
     */
    @Value("${mall.fcm.enabled:true}")
    private boolean enabled;

    /**
     * 后台单线程队列：所有 FCM 外网调用都放到这里异步执行，绝不阻塞业务请求线程。
     * （此前 sendToTopic 同步执行，谷歌无法连通时会卡到 ~90s 连接超时，拖垮下单响应。）
     */
    private final ExecutorService pool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "fcm-notify");
        t.setDaemon(true);
        return t;
    });

    @PostConstruct
    public void init()
    {
        if (!enabled)
        {
            log.info("FCM: disabled by config (mall.fcm.enabled=false) — push notifications skipped");
            return;
        }
        ClassPathResource resource = new ClassPathResource("firebase-credentials.json");
        if (!resource.exists())
        {
            log.warn("FCM: firebase-credentials.json not found — push notifications disabled");
            return;
        }
        try (InputStream stream = resource.getInputStream())
        {
            if (FirebaseApp.getApps().isEmpty())
            {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(stream))
                        .build();
                FirebaseApp.initializeApp(options);
            }
            initialized = true;
            log.info("FCM: FirebaseApp initialized");
        }
        catch (IOException e)
        {
            log.warn("FCM: Failed to initialize FirebaseApp — push notifications disabled: {}", e.getMessage());
        }
    }

    /** 向单个设备 token 发送推送，失败只记 warn，不抛异常 */
    public void sendToToken(String token, String title, String body, Map<String, String> data)
    {
        if (!initialized || token == null || token.isEmpty()) return;
        pool.submit(() -> {
            try
            {
                Message.Builder builder = Message.builder()
                        .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                        .setToken(token);
                if (data != null)
                {
                    builder.putAllData(data);
                }
                String messageId = FirebaseMessaging.getInstance().send(builder.build());
                log.debug("FCM sent to token, messageId={}", messageId);
            }
            catch (Exception e)
            {
                log.warn("FCM sendToToken failed: {}", e.getMessage());
            }
        });
    }

    /** 向 FCM Topic 广播，失败只记 warn，不抛异常 */
    public void sendToTopic(String topic, String title, String body, Map<String, String> data)
    {
        if (!initialized) return;
        pool.submit(() -> {
            try
            {
                Message.Builder builder = Message.builder()
                        .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                        .setTopic(topic);
                if (data != null)
                {
                    builder.putAllData(data);
                }
                String messageId = FirebaseMessaging.getInstance().send(builder.build());
                log.debug("FCM sent to topic={}, messageId={}", topic, messageId);
            }
            catch (Exception e)
            {
                log.warn("FCM sendToTopic failed: {}", e.getMessage());
            }
        });
    }

    /** 订阅 Topic（runner 上线时调用） */
    public void subscribeToTopic(String token, String topic)
    {
        if (!initialized || token == null || token.isEmpty()) return;
        pool.submit(() -> {
            try
            {
                FirebaseMessaging.getInstance().subscribeToTopic(java.util.Collections.singletonList(token), topic);
                log.debug("FCM subscribed token to topic={}", topic);
            }
            catch (Exception e)
            {
                log.warn("FCM subscribeToTopic failed: {}", e.getMessage());
            }
        });
    }

    /** 取消订阅 Topic（runner 下线时调用） */
    public void unsubscribeFromTopic(String token, String topic)
    {
        if (!initialized || token == null || token.isEmpty()) return;
        pool.submit(() -> {
            try
            {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(java.util.Collections.singletonList(token), topic);
                log.debug("FCM unsubscribed token from topic={}", topic);
            }
            catch (Exception e)
            {
                log.warn("FCM unsubscribeFromTopic failed: {}", e.getMessage());
            }
        });
    }
}
