package com.ruoyi.web.controller.openapi;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.mall.service.IMossPayService;
import com.ruoyi.mall.service.IPayOpenService;

import javax.servlet.http.HttpServletRequest;

/**
 * 聚合支付-下游商户开放 API（协议参照 亿林/直付通 api.txt）。
 *
 * 商户对接基址 = {我方域名}/openapi ，即：
 *   下单  POST {base}/api/payment
 *   查单  GET  {base}/api/query/{payOrderNo}
 * 鉴权：Header Request-Channel-Id / Request-Site-Code / Request-Id + 体内 MD5 sign。
 * Shiro 对 /openapi/** 放行(anon)，鉴权在业务层按商户 app_secret 验签。
 */
@RestController
@RequestMapping("/openapi")
public class OpenPayController
{
    private static final Logger log = LoggerFactory.getLogger(OpenPayController.class);

    @Autowired private IPayOpenService payOpenService;
    @Autowired private IMossPayService mossPayService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // -------------------------------------------------------------- 下单

    @PostMapping(value = "/api/payment", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> payment(
            @RequestHeader(value = "Request-Channel-Id", required = false) String channelId,
            @RequestHeader(value = "Request-Site-Code", required = false) String siteCode,
            @RequestHeader(value = "Request-Id", required = false) String requestId,
            @RequestBody(required = false) String rawBody)
    {
        try
        {
            JsonNode root = objectMapper.readTree(rawBody == null ? "{}" : rawBody);
            String sign = root.path("sign").asText("");
            // 保留商户原始字符串值参与验签（不做数字规整，避免 10.00 -> 10.0）
            Map<String, String> data = new LinkedHashMap<>();
            JsonNode dataNode = root.path("data");
            if (dataNode.isObject())
            {
                Iterator<String> it = dataNode.fieldNames();
                while (it.hasNext())
                {
                    String k = it.next();
                    data.put(k, dataNode.path(k).asText(""));
                }
            }
            return payOpenService.createPayment(channelId, siteCode, requestId, data, sign);
        }
        catch (Exception e)
        {
            log.error("[OpenPay] 下单异常: {}", e.getMessage());
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("code", "5000");
            r.put("msg", "请求解析失败");
            return r;
        }
    }

    // -------------------------------------------------------------- 查单

    @GetMapping(value = "/api/query/{payOrderNo}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> query(@PathVariable("payOrderNo") String payOrderNo)
    {
        return payOpenService.query(payOrderNo);
    }

    // ------------------------------------------------- 上游 MOSS 异步通知

    @PostMapping("/upstream/moss/notify")
    public String mossNotify(HttpServletRequest request) throws IOException
    {
        byte[] bodyBytes = StreamUtils.copyToByteArray(request.getInputStream());
        String rawBody = new String(bodyBytes, "UTF-8");
        log.info("[OpenPay] 收到 MOSS 通知: {}", rawBody);

        // 收集参数（表单 + 便于后续 RSA 验签）
        Map<String, String> params = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet())
        {
            if (e.getValue() != null && e.getValue().length > 0)
            {
                params.put(e.getKey(), e.getValue()[0]);
            }
        }
        // JSON 报文也解析进 params
        try
        {
            JsonNode root = objectMapper.readTree(rawBody.isEmpty() ? "{}" : rawBody);
            Iterator<String> it = root.fieldNames();
            while (it.hasNext())
            {
                String k = it.next();
                if (root.path(k).isValueNode()) params.putIfAbsent(k, root.path(k).asText(""));
            }
        }
        catch (Exception ignore) {}

        if (!mossPayService.verifyNotify(params))
        {
            log.warn("[OpenPay] MOSS 通知验签失败");
            return "FAIL";
        }
        Map<String, String> parsed = mossPayService.parseNotify(params);
        if (!"SUCCESS".equals(parsed.get("status")))
        {
            return "success"; // 非成功也应答，避免上游重推
        }
        String platformNo = parsed.get("outTradeNo");
        payOpenService.handleUpstreamPaid(platformNo, parsed.get("tradeNo"), rawBody);
        return "success";
    }

    // ------------------------------------------------- Mock 联调辅助（mock 模式）

    /** mock 收银台页面 */
    @GetMapping(value = "/mock/cashier", produces = "text/html;charset=UTF-8")
    public String mockCashier(HttpServletRequest request)
    {
        String no = request.getParameter("no");
        String amount = request.getParameter("amount");
        if (!mossPayService.isMock())
        {
            return "<h3>非 mock 模式</h3>";
        }
        return "<!DOCTYPE html><html><head><meta charset='utf-8'><title>Mock 收银台</title></head>"
             + "<body style='font-family:sans-serif;text-align:center;margin-top:60px'>"
             + "<h2>Mock 收银台</h2>"
             + "<p>平台订单号: <b>" + esc(no) + "</b></p>"
             + "<p>金额: <b>¥" + esc(amount) + "</b></p>"
             + "<button style='padding:12px 28px;font-size:16px' "
             + "onclick=\"fetch('/openapi/mock/paid/" + esc(no) + "',{method:'POST'})"
             + ".then(r=>r.json()).then(j=>{document.getElementById('r').innerText=JSON.stringify(j)})\">"
             + "确认支付</button>"
             + "<pre id='r' style='margin-top:20px;color:green'></pre>"
             + "</body></html>";
    }

    /** mock 模拟支付成功：触发置单+回调下游 */
    @PostMapping(value = "/mock/paid/{platformNo}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> mockPaid(@PathVariable("platformNo") String platformNo)
    {
        Map<String, Object> r = new LinkedHashMap<>();
        if (!mossPayService.isMock())
        {
            r.put("code", "5000");
            r.put("msg", "非 mock 模式，禁止");
            return r;
        }
        boolean changed = payOpenService.handleUpstreamPaid(platformNo, "MOCK" + platformNo, "{\"mock\":true}");
        r.put("code", "0000");
        r.put("msg", changed ? "已置为支付成功并回调下游" : "订单不存在或已支付(幂等)");
        return r;
    }

    private static String esc(String s)
    {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
