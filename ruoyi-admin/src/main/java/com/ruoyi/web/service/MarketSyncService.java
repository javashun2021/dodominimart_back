package com.ruoyi.web.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.mall.domain.MallMarketPost;
import com.ruoyi.mall.service.IMallMarketService;
import com.ruoyi.mall.service.TranslationService;
import com.ruoyi.mall.service.TranslationService.ListingResult;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 二手市场外部帖每日同步：拉取外部分页中文帖 → 清洗联系方式 → Claude 翻译成英文 →
 * 归属官方会员、自动审核通过入库。只做「拉取+翻译+入库」，不触发任何其它业务。
 *
 * 由定时任务 bean {@code marketSyncTask} 每天调用一次。开关/接口地址/会员/电话均读 sys_config。
 */
@Service
public class MarketSyncService
{
    private static final Logger log = LoggerFactory.getLogger(MarketSyncService.class);

    private static final String SOURCE = "ext";

    /** 允许的英文分类（LLM 判不出或返回非法值时归 Other） */
    private static final Set<String> CATEGORIES = new HashSet<>(Arrays.asList(
            "Electronics", "Clothing", "Food", "Furniture", "Books", "Vehicles", "Baby & Kids", "Other"));

    /** diycon 里的价格数字 */
    private static final Pattern PRICE = Pattern.compile("\\d+(?:\\.\\d+)?");

    /** 正文内联联系方式清洗规则（按序执行；先标签化的，再裸 @handle，最后长数字串） */
    private static final Pattern[] CONTACT_PATTERNS = new Pattern[] {
        // 微信 / vx / wx / weixin + 号
        Pattern.compile("(?i)(微信号?|加微|v信|vx|wx|weixin)\\s*[:：]?\\s*[A-Za-z0-9_\\-]{3,}"),
        // QQ / 扣扣 + 号
        Pattern.compile("(?i)(扣扣|qq)\\s*[:：]?\\s*\\d{5,}"),
        // 飞机 / 电报 / telegram / tg + @号
        Pattern.compile("(?i)(纸飞机|飞机|电报|telegram|tg)\\s*[:：]?\\s*@?[A-Za-z0-9_]{3,}"),
        // 裸 telegram/@ 用户名
        Pattern.compile("@[A-Za-z0-9_]{3,}"),
        // 电话/手机号：7 位以上连续数字（可含 空格/-），价格一般 3-4 位不会命中
        Pattern.compile("(?<!\\d)\\+?\\d[\\d\\-\\s]{5,}\\d(?!\\d)")
    };

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private TranslationService translationService;

    @Autowired
    private IMallMarketService marketService;

    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate()
    {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8_000);
        factory.setReadTimeout(20_000);
        return new RestTemplate(factory);
    }

    /** 定时任务入口：执行一次全量分页同步 */
    public void syncOnce()
    {
        if (!"true".equalsIgnoreCase(cfg("mall.market.sync.enabled", "")))
        {
            log.info("[market-sync] skipped (disabled)");
            return;
        }
        String apiUrl = cfg("mall.market.sync.api-url", "");
        if (apiUrl.isEmpty())
        {
            log.warn("[market-sync] skipped: api-url not configured");
            return;
        }
        if (!translationService.isConfigured())
        {
            log.warn("[market-sync] skipped: Anthropic key not configured");
            return;
        }
        long memberId = parseLong(cfg("mall.market.sync.member-id", "1001"), 1001L);
        String phone = cfg("mall.market.sync.contact-phone", "");
        int maxPages = (int) parseLong(cfg("mall.market.sync.max-pages", "50"), 50L);

        int synced = 0, dup = 0, failed = 0;
        for (int page = 1; page <= maxPages; page++)
        {
            JsonNode plists = fetchPlists(apiUrl, page);
            if (plists == null || !plists.isArray() || plists.size() == 0)
            {
                break;   // 空页 / 取不到 → 结束翻页
            }

            // 1) 去重，收集本页新帖
            List<JsonNode> fresh = new ArrayList<>();
            for (JsonNode it : plists)
            {
                String extId = it.path("id").asText("");
                if (extId.isEmpty()) continue;
                if (marketService.isExternalImported(SOURCE, extId)) { dup++; continue; }
                fresh.add(it);
            }
            if (fresh.isEmpty()) continue;

            // 2) 清洗正文
            List<String> bodies = new ArrayList<>();
            for (JsonNode it : fresh) bodies.add(stripContacts(it.path("con").asText("")));

            // 3) 批量翻译（失败则跳过整页新帖，靠 external_id 下次重试）
            List<ListingResult> tr;
            try
            {
                tr = translationService.translateListings(bodies);
            }
            catch (Exception e)
            {
                failed += fresh.size();
                log.warn("[market-sync] page {} translate failed, skipped {} items: {}",
                        page, fresh.size(), e.getMessage());
                continue;
            }

            // 4) 组装 + 入库
            for (int i = 0; i < fresh.size(); i++)
            {
                JsonNode it = fresh.get(i);
                ListingResult r = tr.get(i);
                try
                {
                    MallMarketPost p = new MallMarketPost();
                    p.setMemberId(memberId);
                    p.setSource(SOURCE);
                    p.setExternalId(it.path("id").asText(""));
                    p.setTitle(cut(r.title, 200));
                    p.setDescription(r.description);
                    p.setCategory(normalizeCategory(r.category));
                    p.setImages(joinImages(it.path("imglist")));

                    BigDecimal price = parsePrice(it.path("diycon"));
                    if (price != null)
                    {
                        p.setPrice(price);
                        p.setPriceType("fixed");
                    }
                    else
                    {
                        p.setPrice(null);
                        p.setPriceType("negotiable");
                    }
                    p.setPhone(phone);

                    marketService.importExternalPost(p);
                    synced++;
                }
                catch (Exception e)
                {
                    failed++;
                    log.warn("[market-sync] insert failed for external id {}: {}",
                            it.path("id").asText(""), e.getMessage());
                }
            }
        }
        log.info("[market-sync] done: synced={} dup(skip)={} failed={}", synced, dup, failed);
    }

    // ------------------------------------------------------------------ helpers

    private JsonNode fetchPlists(String apiUrl, int page)
    {
        try
        {
            String url = apiUrl + (apiUrl.contains("?") ? "&" : "?") + "page=" + page;
            String body = restTemplate.getForObject(url, String.class);
            if (body == null || body.isEmpty()) return null;
            JsonNode root = mapper.readTree(body);
            return root.path("data").path("plists");
        }
        catch (Exception e)
        {
            log.warn("[market-sync] fetch page {} failed: {}", page, e.getMessage());
            return null;
        }
    }

    /**
     * 从 diycon 数组里取「价格」项的 value 解析。diycon 形如
     * [{name:"价格",value:"不限"},{name:"新旧程度",...},{name:"交易方式",...}]。
     * 有数字 → ×1.10 加价显示；「不限」等无数字 → null（→ 面议）。
     */
    private BigDecimal parsePrice(JsonNode diycon)
    {
        if (diycon == null || !diycon.isArray()) return null;
        for (JsonNode field : diycon)
        {
            String name = field.path("name").asText("");
            if (name.contains("价格") || name.equalsIgnoreCase("price"))
            {
                Matcher m = PRICE.matcher(field.path("value").asText(""));
                if (m.find())
                {
                    try
                    {
                        double v = Double.parseDouble(m.group());
                        if (v > 0)
                        {
                            return BigDecimal.valueOf(v * 1.10).setScale(2, RoundingMode.HALF_UP);
                        }
                    }
                    catch (NumberFormatException ignored) {}
                }
                return null;   // 找到价格字段但无数字（如「不限」）→ 面议
            }
        }
        return null;
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

    private String joinImages(JsonNode node)
    {
        if (node == null || node.isMissingNode() || node.isNull()) return "";
        if (node.isArray())
        {
            List<String> urls = new ArrayList<>();
            for (JsonNode n : node)
            {
                String u = n.asText("");
                if (!u.isEmpty()) urls.add(u);
            }
            return String.join(",", urls);
        }
        return node.asText("");
    }

    /** 删除正文里内联的电话/飞机号/微信/QQ，避免绕过官方联系方式 */
    private String stripContacts(String con)
    {
        if (con == null || con.isEmpty()) return "";
        String s = con;
        for (Pattern p : CONTACT_PATTERNS)
        {
            s = p.matcher(s).replaceAll(" ");
        }
        // 收敛多余空白
        return s.replaceAll("[ \\t]{2,}", " ").replaceAll("(\\s*\\n\\s*){2,}", "\n").trim();
    }

    private static String cut(String s, int max)
    {
        if (s == null) return "";
        s = s.trim();
        return s.length() > max ? s.substring(0, max) : s;
    }

    private String cfg(String key, String def)
    {
        try
        {
            String v = configService.selectConfigByKey(key);
            return (v != null && !v.isEmpty()) ? v : def;
        }
        catch (Exception e)
        {
            return def;
        }
    }

    private static long parseLong(String s, long def)
    {
        try { return Long.parseLong(s.trim()); }
        catch (Exception e) { return def; }
    }
}
