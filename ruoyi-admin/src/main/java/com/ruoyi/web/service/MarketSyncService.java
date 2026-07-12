package com.ruoyi.web.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
import com.ruoyi.common.config.Global;
import com.ruoyi.common.utils.DateUtils;
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
        // 加价比例：0 或未配 = 不加价；0.1 = +10%。最终价 = 原价 × (1 + markup)
        double markup = parseDouble(cfg("mall.market.sync.price-markup", "0"), 0d);
        if (markup < 0) markup = 0d;
        // 图片本地化的公网基地址（下载到本机后用自己域名拼 URL）
        String imageBaseUrl = cfg("mall.market.sync.image-base-url", "https://dodominimart.com");
        if (imageBaseUrl.endsWith("/")) imageBaseUrl = imageBaseUrl.substring(0, imageBaseUrl.length() - 1);

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

            // 2) 清洗正文：删除内联联系方式（同时留存以供后台内部查看）
            List<String> bodies = new ArrayList<>();
            List<String> contacts = new ArrayList<>();
            for (JsonNode it : fresh)
            {
                CleanResult cr = cleanAndExtract(it.path("con").asText(""));
                bodies.add(cr.cleaned);
                contacts.add(cr.contacts);
            }

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
                    // 把外部图片下载到本机、改用自己域名的地址（不用对方 CDN）
                    p.setImages(localizeImages(it.path("imglist"), imageBaseUrl));

                    BigDecimal price = parsePrice(it.path("diycon"), markup);
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
                    // 原始联系方式留后台内部查看（对外接口不返回）
                    p.setSourceContact(cut(contacts.get(i), 500));

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
     * 有数字 → × (1 + markup) 加价显示；「不限」等无数字 → null（→ 面议）。
     * @param markup 加价比例（0=不加价，0.1=+10%）
     */
    private BigDecimal parsePrice(JsonNode diycon, double markup)
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
                            return BigDecimal.valueOf(v * (1 + markup)).setScale(2, RoundingMode.HALF_UP);
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

    /**
     * 把外部 imglist 里的图片逐张下载到本机 /profile/upload/，改用自己域名的 URL。
     * 存盘路径与 App 上传一致：&lt;uploadPath&gt;/&lt;yyyy/MM/dd&gt;/&lt;uuid&gt;.&lt;ext&gt;；
     * 公网 URL = baseUrl + /profile/upload/&lt;yyyy/MM/dd&gt;/&lt;uuid&gt;.&lt;ext&gt;。下载失败的图跳过。
     */
    private String localizeImages(JsonNode imglist, String baseUrl)
    {
        if (imglist == null || !imglist.isArray()) return "";
        List<String> localUrls = new ArrayList<>();
        String datePath = DateUtils.datePath();   // yyyy/MM/dd
        for (JsonNode n : imglist)
        {
            String src = n.asText("");
            if (src.isEmpty()) continue;
            try
            {
                byte[] bytes = restTemplate.getForObject(src, byte[].class);
                if (bytes == null || bytes.length == 0) continue;

                String ext = imageExt(src);
                String name = java.util.UUID.randomUUID().toString().replace("-", "") + ext;
                Path dir = Paths.get(Global.getUploadPath(), datePath);
                Files.createDirectories(dir);
                Files.write(dir.resolve(name), bytes);

                localUrls.add(baseUrl + "/profile/upload/" + datePath + "/" + name);
            }
            catch (Exception e)
            {
                log.warn("[market-sync] download image failed, skipped: {} ({})", src, e.getMessage());
            }
        }
        return String.join(",", localUrls);
    }

    private static String imageExt(String url)
    {
        String u = url.toLowerCase();
        int q = u.indexOf('?');
        if (q > 0) u = u.substring(0, q);
        if (u.endsWith(".png"))  return ".png";
        if (u.endsWith(".webp")) return ".webp";
        if (u.endsWith(".gif"))  return ".gif";
        if (u.endsWith(".jpeg")) return ".jpeg";
        return ".jpg";   // jpg 及未知按 jpg
    }

    /** 清洗结果：cleaned=删除联系方式后的正文；contacts=被删除的联系方式（后台内部留存） */
    private static class CleanResult
    {
        String cleaned = "";
        String contacts = "";
    }

    /**
     * 删除正文里内联的电话/飞机号/微信/QQ（避免顾客绕过官方联系方式），
     * 同时把删掉的联系方式收集起来，供后台内部联系卖家用。
     */
    private CleanResult cleanAndExtract(String con)
    {
        CleanResult r = new CleanResult();
        if (con == null || con.isEmpty()) return r;

        Set<String> found = new LinkedHashSet<>();
        String s = con;
        for (Pattern p : CONTACT_PATTERNS)
        {
            Matcher m = p.matcher(s);
            StringBuffer sb = new StringBuffer();
            while (m.find())
            {
                found.add(m.group().trim());
                m.appendReplacement(sb, " ");
            }
            m.appendTail(sb);
            s = sb.toString();
        }
        r.cleaned = s.replaceAll("[ \\t]{2,}", " ").replaceAll("(\\s*\\n\\s*){2,}", "\n").trim();
        r.contacts = String.join(" | ", found);
        return r;
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

    private static double parseDouble(String s, double def)
    {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return def; }
    }
}
