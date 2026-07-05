package com.ruoyi.web.controller.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.ruoyi.common.base.AjaxResult;

/**
 * Google Places 代理接口：App 地址搜索走后端，Places API key 只存服务端不暴露给客户端。
 *
 * GET /api/v1/places/autocomplete?input=&sessiontoken=  — 输入联想（返回候选列表）
 * GET /api/v1/places/details?placeId=&sessiontoken=     — 取某个候选的经纬度+完整地址
 *
 * 两个接口都落在 ShiroConfig 的 /api/** → jwtAuth 兜底，需登录，防止 key 被匿名滥用。
 * key 未配置（mall.google.places-key 为空）时返回空结果，不报错。
 * 计费优化：同一次搜索的 autocomplete 多次请求 + 最后一次 details 共用同一个 sessiontoken。
 */
@RestController
@RequestMapping("/api/v1/places")
public class ApiPlacesController extends BaseApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiPlacesController.class);

    @Value("${mall.google.places-key:}")
    private String placesKey;

    // 菲律宾为主市场：联想结果限定在菲律宾、英文。
    private static final String COUNTRY = "ph";
    private static final String LANGUAGE = "en";

    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(8_000);
        return new RestTemplate(factory);
    }

    /** 输入联想：返回 [{placeId, description}]（供列表展示，点选后再调 details 拿坐标）。 */
    @GetMapping("/autocomplete")
    @SuppressWarnings("unchecked")
    public AjaxResult autocomplete(@RequestParam("input") String input,
                                   @RequestParam(value = "sessiontoken", required = false) String sessionToken) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (placesKey == null || placesKey.isEmpty()) {
            log.warn("Places key not configured; returning empty autocomplete");
            return AjaxResult.success().put("data", out);
        }
        if (input == null || input.trim().isEmpty()) {
            return AjaxResult.success().put("data", out);
        }
        try {
            UriComponentsBuilder b = UriComponentsBuilder
                    .fromHttpUrl("https://maps.googleapis.com/maps/api/place/autocomplete/json")
                    .queryParam("input", input.trim())
                    .queryParam("key", placesKey)
                    .queryParam("components", "country:" + COUNTRY)
                    .queryParam("language", LANGUAGE);
            if (sessionToken != null && !sessionToken.isEmpty()) {
                b.queryParam("sessiontoken", sessionToken);
            }
            Map<String, Object> resp = restTemplate.getForObject(b.build().encode().toUri(), Map.class);
            if (resp != null) {
                Object preds = resp.get("predictions");
                if (preds instanceof List) {
                    for (Object o : (List<Object>) preds) {
                        if (!(o instanceof Map)) continue;
                        Map<String, Object> p = (Map<String, Object>) o;
                        Map<String, Object> item = new HashMap<>();
                        item.put("placeId", p.get("place_id"));
                        item.put("description", p.get("description"));
                        out.add(item);
                    }
                }
                Object status = resp.get("status");
                if (status != null && !"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
                    log.warn("Places autocomplete status={} msg={}", status, resp.get("error_message"));
                }
            }
        } catch (Exception e) {
            log.warn("Places autocomplete failed: {}", e.getMessage());
        }
        return AjaxResult.success().put("data", out);
    }

    /** 取某个候选的经纬度 + 完整地址。 */
    @GetMapping("/details")
    @SuppressWarnings("unchecked")
    public AjaxResult details(@RequestParam("placeId") String placeId,
                              @RequestParam(value = "sessiontoken", required = false) String sessionToken) {
        if (placesKey == null || placesKey.isEmpty()) {
            return AjaxResult.error("Location service unavailable");
        }
        if (placeId == null || placeId.trim().isEmpty()) {
            return AjaxResult.error("Missing placeId");
        }
        try {
            UriComponentsBuilder b = UriComponentsBuilder
                    .fromHttpUrl("https://maps.googleapis.com/maps/api/place/details/json")
                    .queryParam("place_id", placeId.trim())
                    .queryParam("key", placesKey)
                    .queryParam("fields", "geometry,formatted_address,name")
                    .queryParam("language", LANGUAGE);
            if (sessionToken != null && !sessionToken.isEmpty()) {
                b.queryParam("sessiontoken", sessionToken);
            }
            Map<String, Object> resp = restTemplate.getForObject(b.build().encode().toUri(), Map.class);
            if (resp == null || !(resp.get("result") instanceof Map)) {
                return AjaxResult.error("Place not found");
            }
            Map<String, Object> result = (Map<String, Object>) resp.get("result");
            Map<String, Object> geometry = (Map<String, Object>) result.get("geometry");
            if (geometry == null || !(geometry.get("location") instanceof Map)) {
                return AjaxResult.error("Place has no location");
            }
            Map<String, Object> loc = (Map<String, Object>) geometry.get("location");
            Number lat = (Number) loc.get("lat");
            Number lng = (Number) loc.get("lng");
            if (lat == null || lng == null) {
                return AjaxResult.error("Place has no location");
            }
            Map<String, Object> data = new HashMap<>();
            data.put("lat", lat.doubleValue());
            data.put("lng", lng.doubleValue());
            data.put("address", result.get("formatted_address"));
            data.put("name", result.get("name"));
            return AjaxResult.success().put("data", data);
        } catch (Exception e) {
            log.warn("Places details failed: {}", e.getMessage());
            return AjaxResult.error("Failed to resolve place");
        }
    }
}
