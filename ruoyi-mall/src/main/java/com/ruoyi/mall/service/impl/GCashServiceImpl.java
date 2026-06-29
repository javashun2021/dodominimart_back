package com.ruoyi.mall.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.mall.service.IGCashService;

/**
 * 在线支付服务 —— 走 PayMongo（菲律宾），承载 App 里「GCash」在线支付项。
 *
 * 说明：菲律宾没有面向普通商户的 GCash 直连 API，PayMongo 才是真实通道；
 * 这里用 PayMongo 的 Checkout Session 模型：创建会话拿 checkout_url 让用户去付，
 * 付完 PayMongo 发带签名的 webhook 到 /api/v1/payment/callback。
 *
 * 接口名仍叫 IGCashService 只为最小化改动（控制器零改动）；实质是 PayMongo。
 * 文档：https://developers.paymongo.com
 * 配置见 application.yml 的 mall.paymongo.*
 */
@Service
public class GCashServiceImpl implements IGCashService
{
    private static final Logger log = LoggerFactory.getLogger(GCashServiceImpl.class);

    private static final String CHECKOUT_SESSIONS_URL = "https://api.paymongo.com/v1/checkout_sessions";
    private static final String PAYMENTS_URL = "https://api.paymongo.com/v1/payments/";
    private static final String REFUNDS_URL = "https://api.paymongo.com/v1/refunds";

    /** 关心的支付成功事件 */
    private static final String EVENT_CHECKOUT_PAID = "checkout_session.payment.paid";
    private static final String EVENT_PAYMENT_PAID  = "payment.paid";

    @Value("${mall.paymongo.secret-key:}")
    private String secretKey;

    @Value("${mall.paymongo.webhook-secret:}")
    private String webhookSecret;

    /** true=生产(比对签名头里的 li)，false=测试(比对 te) */
    @Value("${mall.paymongo.live:false}")
    private boolean live;

    @Value("${mall.paymongo.success-url:https://dodominimart.com/pay-success.html}")
    private String successUrl;

    @Value("${mall.paymongo.cancel-url:https://dodominimart.com/pay-cancel.html}")
    private String cancelUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ------------------------------------------------------------ createPayment

    @Override
    public String createPayment(Long orderId, BigDecimal amount, String orderNo)
    {
        try
        {
            // PayMongo 金额单位是「分」(centavos) 的整数：₱100.00 -> 10000
            long centavos = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();

            Map<String, Object> lineItem = new HashMap<>();
            lineItem.put("currency", "PHP");
            lineItem.put("amount", centavos);
            lineItem.put("name", "Order " + orderNo);
            lineItem.put("quantity", 1);

            List<Object> lineItems = new ArrayList<>();
            lineItems.add(lineItem);

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("line_items", lineItems);
            attributes.put("payment_method_types", new String[] { "gcash" });
            attributes.put("reference_number", orderNo);
            attributes.put("description", "DodoMiniMart Order " + orderNo);
            attributes.put("success_url", appendOrder(successUrl, orderNo));
            attributes.put("cancel_url", appendOrder(cancelUrl, orderNo));
            attributes.put("send_email_receipt", false);

            Map<String, Object> data = new HashMap<>();
            data.put("attributes", attributes);
            Map<String, Object> body = new HashMap<>();
            body.put("data", data);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders());
            String json = restTemplate.postForObject(CHECKOUT_SESSIONS_URL, request, String.class);

            JsonNode root = objectMapper.readTree(json);
            JsonNode urlNode = root.path("data").path("attributes").path("checkout_url");
            if (urlNode.isMissingNode() || urlNode.asText().isEmpty())
            {
                log.error("PayMongo createPayment unexpected response: {}", json);
                throw new RuntimeException("Failed to create PayMongo checkout session");
            }
            return urlNode.asText();
        }
        catch (Exception e)
        {
            log.error("PayMongo createPayment error for order {}: {}", orderId, e.getMessage());
            throw new RuntimeException("Online payment unavailable: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------- verifyCallback

    @Override
    public boolean verifyCallback(Map<String, String> headers, String rawBody)
    {
        // PayMongo 验签：头 Paymongo-Signature: t=<ts>,te=<test签名>,li=<live签名>
        // 期望签名 = HMAC_SHA256(key=webhookSecret, msg = "<ts>.<rawBody>") 的 hex
        String sigHeader = headerIgnoreCase(headers, "Paymongo-Signature");
        if (sigHeader == null || sigHeader.isEmpty())
        {
            log.warn("PayMongo callback missing Paymongo-Signature header");
            return false;
        }
        if (webhookSecret == null || webhookSecret.isEmpty())
        {
            log.error("PayMongo webhook-secret not configured (mall.paymongo.webhook-secret)");
            return false;
        }

        String t = null, te = null, li = null;
        for (String part : sigHeader.split(","))
        {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) continue;
            switch (kv[0].trim())
            {
                case "t":  t  = kv[1].trim(); break;
                case "te": te = kv[1].trim(); break;
                case "li": li = kv[1].trim(); break;
                default: break;
            }
        }

        String provided = live ? li : te;
        if (t == null || provided == null)
        {
            log.warn("PayMongo signature header malformed: {}", sigHeader);
            return false;
        }

        String expected = hmacSha256Hex(webhookSecret, t + "." + rawBody);
        boolean ok = expected != null && constantTimeEquals(expected, provided);
        if (!ok)
        {
            log.warn("PayMongo signature mismatch (live={})", live);
        }
        return ok;
    }

    // ------------------------------------------------------------ parseCallback

    @Override
    public Map<String, String> parseCallback(String rawBody)
    {
        Map<String, String> result = new HashMap<>();
        result.put("status", "FAILED");
        try
        {
            JsonNode root = objectMapper.readTree(rawBody);
            // 事件信封：data.attributes.type = 事件名；data.attributes.data = 资源对象
            JsonNode eventAttr = root.path("data").path("attributes");
            String type = eventAttr.path("type").asText("");

            JsonNode resourceAttr = eventAttr.path("data").path("attributes");

            // orderNo 来自我们传的 reference_number
            String orderNo = resourceAttr.path("reference_number").asText("");
            result.put("orderNo", orderNo);

            // paymentNo 取 payments[0].id（pay_xxx）
            JsonNode payments = resourceAttr.path("payments");
            String paymentNo = "";
            if (payments.isArray() && payments.size() > 0)
            {
                paymentNo = payments.get(0).path("id").asText("");
            }
            // payment.paid 事件的资源本身就是 payment，其 id 在 data.attributes.data.id
            if (paymentNo.isEmpty())
            {
                paymentNo = eventAttr.path("data").path("id").asText("");
            }
            result.put("paymentNo", paymentNo);

            if (EVENT_CHECKOUT_PAID.equals(type) || EVENT_PAYMENT_PAID.equals(type))
            {
                result.put("status", "SUCCESS");
            }
        }
        catch (Exception e)
        {
            log.error("PayMongo parseCallback error: {}", e.getMessage());
        }
        return result;
    }

    // ------------------------------------------------------------ verifyPayment

    @Override
    public Map<String, String> verifyPayment(String paymentId)
    {
        if (paymentId == null || paymentId.isEmpty())
        {
            return null;
        }
        try
        {
            HttpEntity<Void> request = new HttpEntity<>(buildHeaders());
            ResponseEntity<String> resp = restTemplate.exchange(
                    PAYMENTS_URL + paymentId, HttpMethod.GET, request, String.class);

            JsonNode attr = objectMapper.readTree(resp.getBody()).path("data").path("attributes");
            String gwStatus = attr.path("status").asText("");      // PayMongo: "paid" 才算成功
            long amount = attr.path("amount").asLong(-1);          // 实付金额，单位「分」

            Map<String, String> r = new HashMap<>();
            r.put("status", "paid".equalsIgnoreCase(gwStatus) ? "PAID" : "FAILED");
            r.put("amount", String.valueOf(amount));
            r.put("paymentNo", paymentId);
            return r;
        }
        catch (Exception e)
        {
            // 查不到 / 网络错 / 4xx：一律按「未确认」处理（fail closed），调用方不应置 PAID
            log.error("PayMongo verifyPayment error for {}: {}", paymentId, e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------------- refund

    @Override
    public boolean refund(String paymentNo, BigDecimal amount)
    {
        // PayMongo 退款：POST /v1/refunds，传 payment_id(pay_xxx) + amount(分) + reason。
        if (paymentNo == null || paymentNo.isEmpty() || amount == null)
        {
            log.warn("PayMongo refund missing paymentNo/amount");
            return false;
        }
        try
        {
            long centavos = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("amount", centavos);
            attributes.put("payment_id", paymentNo);
            attributes.put("reason", "requested_by_customer");

            Map<String, Object> data = new HashMap<>();
            data.put("attributes", attributes);
            Map<String, Object> body = new HashMap<>();
            body.put("data", data);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders());
            String json = restTemplate.postForObject(REFUNDS_URL, request, String.class);

            JsonNode node = objectMapper.readTree(json).path("data");
            String id = node.path("id").asText("");                          // re_xxx
            String status = node.path("attributes").path("status").asText(""); // pending/succeeded
            // GCash 等电子钱包退款多为异步：拿到退款单 id 且状态为 pending/succeeded 即视为已受理
            boolean ok = !id.isEmpty()
                    && ("succeeded".equalsIgnoreCase(status) || "pending".equalsIgnoreCase(status));
            if (!ok)
            {
                log.error("PayMongo refund unexpected response: {}", json);
            }
            return ok;
        }
        catch (Exception e)
        {
            log.error("PayMongo refund error for paymentNo {}: {}", paymentNo, e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------ helpers

    private HttpHeaders buildHeaders()
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // HTTP Basic：用户名=secretKey，密码空 -> base64("sk_xxx:")
        String token = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + token);
        return headers;
    }

    private static String appendOrder(String url, String orderNo)
    {
        if (url == null || url.isEmpty()) return url;
        String sep = url.contains("?") ? "&" : "?";
        return url + sep + "order=" + orderNo;
    }

    private static String headerIgnoreCase(Map<String, String> headers, String key)
    {
        if (headers == null) return null;
        String v = headers.get(key);
        if (v != null) return v;
        for (Map.Entry<String, String> e : headers.entrySet())
        {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(key))
            {
                return e.getValue();
            }
        }
        return null;
    }

    private static String hmacSha256Hex(String secret, String message)
    {
        try
        {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) { hex.append(String.format("%02x", b)); }
            return hex.toString();
        }
        catch (Exception e)
        {
            log.error("PayMongo HMAC error", e);
            return null;
        }
    }

    /** 常量时间比较，避免计时攻击 */
    private static boolean constantTimeEquals(String a, String b)
    {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++)
        {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
