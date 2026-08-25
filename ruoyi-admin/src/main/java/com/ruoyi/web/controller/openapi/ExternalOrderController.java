package com.ruoyi.web.controller.openapi;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.mall.domain.MallOrder;
import com.ruoyi.mall.service.IExternalOrderService;
import com.ruoyi.mall.service.IMallOrderService;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.web.service.ReceiptTokenService;

/**
 * 对外「订单导入」开放接口。
 *
 * 外部系统推送一笔已支付/未支付交易 → 平台按会员id建/找会员、按付款价匹配商品下单、
 * 差价用积分+补差券补齐、平台补助浮动实付、已支付则模拟配送闭环。
 *
 * 基址 = {我方域名}/openapi ，下单 POST {base}/api/import-order。
 * Shiro 对 /openapi/** 放行(anon)——当前暂不鉴权。
 */
@RestController
@RequestMapping("/openapi")
public class ExternalOrderController
{
    private static final Logger log = LoggerFactory.getLogger(ExternalOrderController.class);

    @Autowired private IExternalOrderService externalOrderService;
    @Autowired private IMallOrderService mallOrderService;
    @Autowired private ISysConfigService configService;
    @Autowired private ReceiptTokenService receiptTokenService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping(value = "/api/import-order", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> importOrder(@RequestBody(required = false) String rawBody)
    {
        try
        {
            JsonNode root = objectMapper.readTree(rawBody == null ? "{}" : rawBody);
            String outOrderNo   = text(root, "outOrderNo");
            String amount       = text(root, "amount");
            String userId       = text(root, "userId");
            String phone        = text(root, "phone");
            String floatAmount  = text(root, "floatAmount");
            String orderTime    = text(root, "orderTime");
            String payTime      = text(root, "payTime");
            Map<String, Object> result = externalOrderService.importOrder(outOrderNo, amount, userId, phone,
                    floatAmount, orderTime, payTime);
            enrichOrderUrl(result);
            return result;
        }
        catch (Exception e)
        {
            log.error("[Import] 请求解析失败: {}", e.getMessage());
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("code", "5000");
            r.put("msg", "请求解析失败");
            return r;
        }
    }

    private static String text(JsonNode root, String field)
    {
        JsonNode n = root.path(field);
        return n.isMissingNode() || n.isNull() ? null : n.asText(null);
    }

    /**
     * 成功时给返回 data 补一个外网免登录订单详情链接 orderUrl。
     * 复用现有公开订单页 {@code /o/{orderId}?t={token}}(Token=HMAC 签名,防按 id 遍历;Shiro 已放行 /o/**)。
     * 链接域名沿用小票二维码的 app.deeplink.base,保持一致。
     */
    @SuppressWarnings("unchecked")
    private void enrichOrderUrl(Map<String, Object> result)
    {
        try
        {
            if (result == null || !"0000".equals(result.get("code"))) return;
            Object dataObj = result.get("data");
            if (!(dataObj instanceof Map)) return;
            Map<String, Object> data = (Map<String, Object>) dataObj;
            Object orderNo = data.get("orderNo");
            if (orderNo == null) return;
            MallOrder order = mallOrderService.selectOrderByOrderNo(orderNo.toString());
            if (order == null || order.getOrderId() == null) return;
            Long orderId = order.getOrderId();
            String base = cfg("app.deeplink.base", "https://dodominimart.com");
            data.put("orderUrl", base + "/o/" + orderId + "?t=" + receiptTokenService.sign(orderId));
        }
        catch (Exception e)
        {
            log.warn("[Import] 生成订单链接失败: {}", e.getMessage());
        }
    }

    private String cfg(String key, String defaultValue)
    {
        try
        {
            String v = configService.selectConfigByKey(key);
            return (v != null && !v.isEmpty()) ? v : defaultValue;
        }
        catch (Exception e)
        {
            return defaultValue;
        }
    }
}
