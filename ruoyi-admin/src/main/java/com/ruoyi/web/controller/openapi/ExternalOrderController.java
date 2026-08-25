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
import com.ruoyi.mall.service.IExternalOrderService;

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
            return externalOrderService.importOrder(outOrderNo, amount, userId, phone,
                    floatAmount, orderTime, payTime);
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
}
