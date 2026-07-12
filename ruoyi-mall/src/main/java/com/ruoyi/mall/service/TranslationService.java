package com.ruoyi.mall.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Claude（Anthropic）翻译服务：把中文二手帖正文翻译成英文，并顺带产出短标题与英文分类。
 * 一次请求批量处理多条，降低成本。仅用于市场帖同步（zh→en），不做其它业务。
 *
 * 配置见 application.yml 的 mall.translate.*（api-key 走环境变量 MALL_TRANSLATE_ANTHROPIC_KEY）。
 */
@Service
public class TranslationService
{
    private static final Logger log = LoggerFactory.getLogger(TranslationService.class);

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    /** 每次请求最多翻译多少条，防止单次输出超限 */
    private static final int BATCH_SIZE = 8;

    @Value("${mall.translate.anthropic-key:}")
    private String apiKey;

    @Value("${mall.translate.model:claude-haiku-4-5}")
    private String model;

    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate()
    {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8_000);
        factory.setReadTimeout(60_000);   // LLM 响应可能较慢
        return new RestTemplate(factory);
    }

    /** 一条帖子的英文化结果 */
    public static class ListingResult
    {
        public String title;
        public String description;
        public String category;
    }

    /** Key 是否已配置（未配置时同步任务应直接跳过，避免中文入库） */
    public boolean isConfigured()
    {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.contains("PLACEHOLDER");
    }

    /**
     * 批量把中文正文翻译为英文，产出 title/description/category。
     * 返回列表与入参**等长、同序**；任一子批失败或条数不匹配则抛异常，由调用方跳过整页。
     */
    public List<ListingResult> translateListings(List<String> cleanedBodies)
    {
        if (!isConfigured())
        {
            throw new IllegalStateException("Anthropic API key not configured");
        }
        List<ListingResult> out = new ArrayList<>();
        for (int i = 0; i < cleanedBodies.size(); i += BATCH_SIZE)
        {
            List<String> chunk = cleanedBodies.subList(i, Math.min(i + BATCH_SIZE, cleanedBodies.size()));
            List<ListingResult> part = callOnce(chunk);
            if (part.size() != chunk.size())
            {
                throw new IllegalStateException("translation count mismatch: expected "
                        + chunk.size() + " got " + part.size());
            }
            out.addAll(part);
        }
        return out;
    }

    private List<ListingResult> callOnce(List<String> bodies)
    {
        try
        {
            // 用户消息：编号后的中文列表
            StringBuilder userText = new StringBuilder();
            userText.append("Translate these ").append(bodies.size())
                    .append(" Chinese second-hand marketplace listings to English.\n")
                    .append("Return ONLY a JSON array of ").append(bodies.size())
                    .append(" objects, same order, each {\"title\":..,\"description\":..,\"category\":..}.\n")
                    .append("title = concise English summary, max 8 words.\n")
                    .append("description = natural English translation of the body.\n")
                    .append("category = choose exactly one of: Electronics, Clothing, Food, Furniture, Books, Vehicles, Baby & Kids, Other. If unsure use Other.\n")
                    .append("No markdown, no code fences, JSON array only.\n\nListings:\n");
            for (int i = 0; i < bodies.size(); i++)
            {
                userText.append("[").append(i).append("] ").append(bodies.get(i)).append("\n");
            }

            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userText.toString());

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("max_tokens", 4096);
            payload.put("messages", new Object[] { userMsg });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            String respBody = restTemplate.postForObject(
                    API_URL, new HttpEntity<>(mapper.writeValueAsString(payload), headers), String.class);

            JsonNode root = mapper.readTree(respBody);
            String text = root.path("content").path(0).path("text").asText("");
            String json = stripFences(text);

            JsonNode arr = mapper.readTree(json);
            List<ListingResult> results = new ArrayList<>();
            if (arr.isArray())
            {
                for (JsonNode node : arr)
                {
                    ListingResult r = new ListingResult();
                    r.title = node.path("title").asText("");
                    r.description = node.path("description").asText("");
                    r.category = node.path("category").asText("Other");
                    results.add(r);
                }
            }
            return results;
        }
        catch (Exception e)
        {
            log.warn("Claude translate failed: {}", e.getMessage());
            throw new RuntimeException("translate failed", e);
        }
    }

    /** 去掉可能的 ```json ... ``` 代码围栏，容错 LLM 偶尔加围栏 */
    private static String stripFences(String s)
    {
        String t = s.trim();
        if (t.startsWith("```"))
        {
            int nl = t.indexOf('\n');
            if (nl > 0) t = t.substring(nl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t.trim();
    }
}
