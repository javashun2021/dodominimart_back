package com.ruoyi.mall.service.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.mall.service.IGCashService;

/**
 * GCash for Business 支付服务
 *
 * 文档参考：https://developers.gcash.com.ph
 * 需在 application.yml 配置 mall.gcash.* 相关参数
 */
@Service
public class GCashServiceImpl implements IGCashService
{
    private static final Logger log = LoggerFactory.getLogger(GCashServiceImpl.class);

    @Value("${mall.gcash.api-base-url:https://api.gcash.com/}")
    private String apiBaseUrl;

    @Value("${mall.gcash.api-key:}")
    private String apiKey;

    @Value("${mall.gcash.api-secret:}")
    private String apiSecret;

    @Value("${mall.gcash.merchant-id:}")
    private String merchantId;

    @Value("${mall.gcash.callback-url:}")
    private String callbackUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String createPayment(Long orderId, BigDecimal amount, String orderNo)
    {
        try
        {
            HttpHeaders headers = buildHeaders();
            Map<String, Object> body = new HashMap<>();
            body.put("merchantId", merchantId);
            body.put("referenceId", orderNo);
            body.put("amount", amount);
            body.put("currency", "PHP");
            body.put("redirectUrl", callbackUrl);
            body.put("webhookUrl", callbackUrl);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    apiBaseUrl + "v1/payments/create", request, Map.class);

            if (response != null && response.containsKey("paymentUrl"))
            {
                return (String) response.get("paymentUrl");
            }
            log.error("GCash createPayment unexpected response: {}", response);
            throw new RuntimeException("Failed to create GCash payment");
        }
        catch (Exception e)
        {
            log.error("GCash createPayment error for order {}: {}", orderId, e.getMessage());
            throw new RuntimeException("GCash payment unavailable: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyCallback(Map<String, String> headers, String rawBody)
    {
        // GCash 回调签名验证：HMAC-SHA256(rawBody, apiSecret)
        // 实际签名算法以 GCash 官方文档为准
        String signature = headers.get("X-GCash-Signature");
        if (signature == null || signature.isEmpty())
        {
            return false;
        }
        try
        {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(apiSecret.getBytes(), "HmacSHA256"));
            byte[] hash = mac.doFinal(rawBody.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) { hex.append(String.format("%02x", b)); }
            return hex.toString().equals(signature);
        }
        catch (Exception e)
        {
            log.error("GCash signature verification error", e);
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> parseCallback(String rawBody)
    {
        Map<String, String> result = new HashMap<>();
        try
        {
            Map<String, Object> data = objectMapper.readValue(rawBody, Map.class);
            result.put("orderNo", String.valueOf(data.getOrDefault("referenceId", "")));
            Object status = data.get("status");
            result.put("status", "SUCCESS".equalsIgnoreCase(String.valueOf(status)) ? "SUCCESS" : "FAILED");
            result.put("paymentNo", String.valueOf(data.getOrDefault("transactionId", "")));
        }
        catch (Exception e)
        {
            log.error("GCash parseCallback error: {}", e.getMessage());
            result.put("status", "FAILED");
        }
        return result;
    }

    @Override
    public boolean refund(String paymentNo, BigDecimal amount)
    {
        try
        {
            HttpHeaders headers = buildHeaders();
            Map<String, Object> body = new HashMap<>();
            body.put("transactionId", paymentNo);
            body.put("amount", amount);
            body.put("reason", "Order cancelled");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    apiBaseUrl + "v1/refunds", request, Map.class);

            return response != null && "SUCCESS".equals(response.get("status"));
        }
        catch (Exception e)
        {
            log.error("GCash refund error for paymentNo {}: {}", paymentNo, e.getMessage());
            return false;
        }
    }

    private HttpHeaders buildHeaders()
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-GCash-Api-Key", apiKey);
        headers.set("X-GCash-Merchant-Id", merchantId);
        return headers;
    }
}
