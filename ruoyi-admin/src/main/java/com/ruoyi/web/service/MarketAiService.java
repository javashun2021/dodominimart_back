package com.ruoyi.web.service;

import java.math.BigDecimal;
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
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 二手市场「AI 帮卖家优化帖子」：结合图片 + 草稿(标题/描述/价格/分类)，
 * 用 Claude 视觉产出优化后的英文标题/描述/分类 + 建议价格区间 + 改进 tips，返回给 App 预览套用。
 *
 * 复用 TranslationService 相同的 Claude 配置(mall.translate.*)与 endpoint/header，
 * 唯一新增能力是多模态(把图片抓取后转 base64 塞进 messages.content)。不改动 TranslationService。
 */
@Service
public class MarketAiService
{
    private static final Logger log = LoggerFactory.getLogger(MarketAiService.class);

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    /** 每次最多送几张图给视觉模型(控成本/延迟) */
    private static final int MAX_IMAGES = 4;
    /** 标题识别时最多送几张图(单条帖，控成本) */
    private static final int MAX_TITLE_IMAGES = 3;

    private static final String[] CATEGORIES = {
            "Electronics", "Clothing", "Food", "Furniture", "Books", "Vehicles", "Baby & Kids", "Other" };

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
        factory.setReadTimeout(60_000);
        return new RestTemplate(factory);
    }

    /** AI 优化结果（返回给 App 展示，卖家自行套用） */
    public static class OptimizeResult
    {
        public String title;
        public String description;
        public String category;
        public String priceRange;
        public List<String> tips = new ArrayList<>();
    }

    /** Key 是否已配置（未配置时接口应返回不可用，避免空调用） */
    public boolean isConfigured()
    {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.contains("PLACEHOLDER");
    }

    /**
     * 结合图片与草稿生成优化建议。任一字段可空；图片可无（无图则纯文本优化）。
     * 失败抛 RuntimeException，由 controller 兜成友好错误。
     */
    public OptimizeResult optimize(String title, String description, BigDecimal price,
                                   String priceType, String category, String imagesCsv)
    {
        try
        {
            List<Object> content = new ArrayList<>();

            // 1) 图片块：抓取 URL → base64（最多 MAX_IMAGES 张，失败的跳过）
            int used = 0;
            if (imagesCsv != null && !imagesCsv.trim().isEmpty())
            {
                for (String raw : imagesCsv.split(","))
                {
                    if (used >= MAX_IMAGES) break;
                    String url = raw.trim();
                    if (url.isEmpty()) continue;
                    Map<String, Object> imgBlock = buildImageBlock(url);
                    if (imgBlock != null) { content.add(imgBlock); used++; }
                }
            }

            // 2) 文本块：草稿 + 指令
            Map<String, Object> textBlock = new HashMap<>();
            textBlock.put("type", "text");
            textBlock.put("text", buildPrompt(title, description, price, priceType, category, used));
            content.add(textBlock);

            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", content);

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("max_tokens", 1024);
            payload.put("messages", new Object[] { userMsg });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            String respBody = restTemplate.postForObject(
                    API_URL, new HttpEntity<>(mapper.writeValueAsString(payload), headers), String.class);

            JsonNode root = mapper.readTree(respBody);
            String text = root.path("content").path(0).path("text").asText("");
            JsonNode j = mapper.readTree(stripFences(text));

            OptimizeResult r = new OptimizeResult();
            r.title = j.path("title").asText("");
            r.description = j.path("description").asText("");
            r.category = normalizeCategory(j.path("category").asText("Other"));
            r.priceRange = j.path("priceRange").asText("");
            JsonNode tips = j.path("tips");
            if (tips.isArray())
            {
                for (JsonNode t : tips)
                {
                    String s = t.asText("");
                    if (!s.isEmpty()) r.tips.add(s);
                }
            }
            return r;
        }
        catch (Exception e)
        {
            log.warn("Market AI optimize failed: {}", e.getMessage());
            throw new RuntimeException("optimize failed", e);
        }
    }

    /**
     * 结合商品图片 + 卖家正文，双重确认商品名称，产出简洁英文标题（供同步帖用）。
     * 以图片识别为主、正文交叉核对为辅。无图 / 未配置 / 失败时返回空串，由调用方回退。
     *
     * @param imagesCsv 逗号分隔的图片 URL（最多取前 {@link #MAX_TITLE_IMAGES} 张）
     * @param bodyText  卖家原始正文（可为中文，仅作交叉核对，不翻译）
     */
    public String identifyProductTitle(String imagesCsv, String bodyText)
    {
        if (!isConfigured()) return "";
        // 无图不做图片识别，交由调用方回退到翻译标题
        if (imagesCsv == null || imagesCsv.trim().isEmpty()) return "";
        try
        {
            List<Object> content = new ArrayList<>();
            int used = 0;
            for (String raw : imagesCsv.split(","))
            {
                if (used >= MAX_TITLE_IMAGES) break;
                String url = raw.trim();
                if (url.isEmpty()) continue;
                Map<String, Object> imgBlock = buildImageBlock(url);
                if (imgBlock != null) { content.add(imgBlock); used++; }
            }
            if (used == 0) return "";   // 图都抓失败 → 回退

            Map<String, Object> textBlock = new HashMap<>();
            textBlock.put("type", "text");
            textBlock.put("text", buildTitlePrompt(bodyText, used));
            content.add(textBlock);

            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", content);

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("max_tokens", 256);
            payload.put("messages", new Object[] { userMsg });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            String respBody = restTemplate.postForObject(
                    API_URL, new HttpEntity<>(mapper.writeValueAsString(payload), headers), String.class);

            JsonNode root = mapper.readTree(respBody);
            String text = root.path("content").path(0).path("text").asText("");
            JsonNode j = mapper.readTree(stripFences(text));
            return j.path("title").asText("").trim();
        }
        catch (Exception e)
        {
            log.warn("Market AI title identify failed: {}", e.getMessage());
            return "";   // 失败回退，不影响同步
        }
    }

    private String buildTitlePrompt(String bodyText, int imageCount)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("You are naming a second-hand marketplace item for its listing title.\n");
        sb.append("Look carefully at the ").append(imageCount)
          .append(" product photo(s) above to identify exactly what item is being sold.\n");
        sb.append("Then read the seller's original listing text below (it may be in Chinese) and use it to CONFIRM the item.\n");
        sb.append("The photos are the primary source of truth; the text is a cross-check. ");
        sb.append("If they disagree, trust the photos but reconcile with any concrete brand/model/spec mentioned in the text.\n");
        sb.append("\nOriginal listing text:\n").append(nz(bodyText)).append("\n");
        sb.append("\nReturn ONLY a JSON object (no markdown, no code fences): {\"title\":\"..\"}.\n");
        sb.append("title = a concise, specific English product name for the listing, max 8 words. ");
        sb.append("Include brand/model/key spec when clearly visible. ");
        sb.append("Do NOT include price, contact info, emojis or marketing fluff.\n");
        return sb.toString();
    }

    // ------------------------------------------------------------------ helpers

    /** 抓取图片 URL → base64 image 块；失败返回 null（调用方跳过该图） */
    private Map<String, Object> buildImageBlock(String url)
    {
        try
        {
            byte[] bytes = restTemplate.getForObject(url, byte[].class);
            if (bytes == null || bytes.length == 0) return null;

            Map<String, Object> source = new HashMap<>();
            source.put("type", "base64");
            source.put("media_type", mediaType(url));
            source.put("data", Base64.getEncoder().encodeToString(bytes));

            Map<String, Object> block = new HashMap<>();
            block.put("type", "image");
            block.put("source", source);
            return block;
        }
        catch (Exception e)
        {
            log.warn("fetch image failed, skipped: {} ({})", url, e.getMessage());
            return null;
        }
    }

    private String buildPrompt(String title, String description, BigDecimal price,
                               String priceType, String category, int imageCount)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("You help a seller improve their second-hand marketplace listing.\n");
        if (imageCount > 0)
        {
            sb.append("Look at the ").append(imageCount).append(" photo(s) above and the seller's draft below.\n");
        }
        else
        {
            sb.append("No photos were provided; use the draft text below.\n");
        }
        sb.append("\nSeller draft:\n");
        sb.append("- title: ").append(nz(title)).append("\n");
        sb.append("- description: ").append(nz(description)).append("\n");
        sb.append("- price: ").append(price != null ? price.toPlainString() : "").append("\n");
        sb.append("- priceType: ").append(nz(priceType)).append("\n");
        sb.append("- category: ").append(nz(category)).append("\n");
        sb.append("\nReturn ONLY a JSON object (no markdown, no code fences): ");
        sb.append("{\"title\":..,\"description\":..,\"category\":..,\"priceRange\":..,\"tips\":[..]}.\n");
        sb.append("Output English.\n");
        sb.append("title = catchy, honest, max 12 words.\n");
        sb.append("description = clear and appealing; mention visible condition/details from the photos.\n");
        sb.append("category = exactly one of [Electronics, Clothing, Food, Furniture, Books, Vehicles, Baby & Kids, Other].\n");
        sb.append("priceRange = a suggested price RANGE string in Philippine peso like \"₱800–1000\" (a hint, never a single hard number); if truly unknown, empty string.\n");
        sb.append("tips = 2-4 short actionable tips to improve the listing.\n");
        sb.append("If a draft field is empty, generate it from the photos.\n");
        return sb.toString();
    }

    private static String mediaType(String url)
    {
        String u = url.toLowerCase();
        int q = u.indexOf('?');
        if (q > 0) u = u.substring(0, q);
        if (u.endsWith(".png")) return "image/png";
        if (u.endsWith(".webp")) return "image/webp";
        if (u.endsWith(".gif")) return "image/gif";
        return "image/jpeg";   // jpg/jpeg 及未知按 jpeg
    }

    private String normalizeCategory(String c)
    {
        if (c == null) return "Other";
        String t = c.trim();
        for (String cat : CATEGORIES)
        {
            if (cat.equalsIgnoreCase(t)) return cat;
        }
        return "Other";
    }

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

    private static String nz(String s)
    {
        return s == null ? "" : s;
    }
}
