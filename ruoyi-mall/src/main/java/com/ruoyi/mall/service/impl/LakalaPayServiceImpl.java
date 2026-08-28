package com.ruoyi.mall.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lkl.laop.sdk.Config;
import com.lkl.laop.sdk.LKLSDK;
import com.lkl.laop.sdk.request.V3CcssCounterOrderSpecialCreateRequest;
import com.ruoyi.mall.config.LakalaProperties;
import com.ruoyi.mall.service.ILakalaPayService;

/**
 * 拉卡拉收银台支付实现。
 * 收银台下单：/api/v3/ccss/counter/order/special_create → counter_url（H5 收银台链接）。
 * 异步通知：LKLSDK.notificationHandle 验签解析。
 */
@Service
public class LakalaPayServiceImpl implements ILakalaPayService
{
    private static final Logger log = LoggerFactory.getLogger(LakalaPayServiceImpl.class);

    /** 拉卡拉 v3 成功返回码 */
    private static final String SUCCESS_CODE = "000000";

    private static final DateTimeFormatter EFFICIENT_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private LakalaProperties props;

    private volatile boolean inited = false;

    @Override
    public boolean isEnabled()
    {
        return props.isEnabled();
    }

    /** 全局仅初始化一次 SDK 接入参数 */
    private void ensureInit()
    {
        if (inited)
        {
            return;
        }
        synchronized (this)
        {
            if (inited)
            {
                return;
            }
            try
            {
                Config config = new Config();
                config.setAppId(props.getAppId());
                config.setSerialNo(props.getSerialNo());
                config.setPriKeyPath(props.getPriKeyPath());
                config.setLklCerPath(props.getLklCerPath());
                config.setLklNotifyCerPath(props.getLklNotifyCerPath());
                config.setServerUrl(props.getServerUrl());
                if (props.getSm4Key() != null && !props.getSm4Key().isEmpty())
                {
                    config.setSm4Key(props.getSm4Key());
                }
                LKLSDK.init(config);
                inited = true;
                log.info("[Lakala] SDK 初始化完成, serverUrl={}, merchantNo={}", props.getServerUrl(), props.getMerchantNo());
            }
            catch (Exception e)
            {
                throw new RuntimeException("拉卡拉 SDK 初始化失败: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public String createCounterPayment(String orderNo, BigDecimal payableYuan, String subject, String outUserId)
    {
        ensureInit();
        long totalFen = payableYuan.movePointRight(2).setScale(0, BigDecimal.ROUND_HALF_UP).longValueExact();
        if (totalFen <= 0)
        {
            throw new IllegalArgumentException("拉卡拉下单金额非法: " + payableYuan);
        }

        V3CcssCounterOrderSpecialCreateRequest req = new V3CcssCounterOrderSpecialCreateRequest();
        req.setOutOrderNo(orderNo);
        req.setMerchantNo(props.getMerchantNo());
        req.setTermNo(props.getTermNo());
        req.setTotalAmount(totalFen);
        req.setOrderEfficientTime(LocalDateTime.now(ZONE).plusMinutes(props.getOrderEfficientMinutes()).format(EFFICIENT_FMT));
        req.setOrderInfo(subject != null && !subject.isEmpty() ? subject : ("订单 " + orderNo));
        req.setNotifyUrl(props.getNotifyUrl());
        if (props.getCallbackUrl() != null && !props.getCallbackUrl().isEmpty())
        {
            req.setCallbackUrl(props.getCallbackUrl());
        }
        req.setSupportRefund(1);
        req.setSupportCancel(0);
        if (outUserId != null && !outUserId.isEmpty())
        {
            req.setOutUserId(outUserId);
        }

        String response;
        try
        {
            response = LKLSDK.httpPost(req);
        }
        catch (Exception e)
        {
            throw new RuntimeException("拉卡拉下单请求失败: " + e.getMessage(), e);
        }

        try
        {
            JsonNode root = mapper.readTree(response);
            String code = text(root, "code");
            if (code != null && !SUCCESS_CODE.equals(code))
            {
                String msg = text(root, "msg");
                log.warn("[Lakala] 下单失败 orderNo={}, code={}, msg={}, resp={}", orderNo, code, msg, response);
                throw new RuntimeException("拉卡拉下单失败: " + (msg != null ? msg : code));
            }
            String counterUrl = findFirst(root, "counter_url");
            if (counterUrl == null || counterUrl.isEmpty())
            {
                log.warn("[Lakala] 下单响应缺少 counter_url, orderNo={}, resp={}", orderNo, response);
                throw new RuntimeException("拉卡拉下单未返回收银台链接");
            }
            log.info("[Lakala] 下单成功 orderNo={}, amountFen={}, counterUrl={}", orderNo, totalFen, counterUrl);
            return counterUrl;
        }
        catch (RuntimeException re)
        {
            throw re;
        }
        catch (Exception e)
        {
            throw new RuntimeException("拉卡拉下单响应解析失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> handleNotify(HttpServletRequest request)
    {
        ensureInit();
        String body;
        try
        {
            // SDK 内部读取请求体 + 请求头认证信息完成验签，返回明文报文；验签失败抛异常
            body = LKLSDK.notificationHandle(request);
        }
        catch (Exception e)
        {
            log.warn("[Lakala] 通知验签失败: {}", e.getMessage());
            return null;
        }

        log.info("[Lakala] 通知验签成功, body={}", body);
        try
        {
            JsonNode root = mapper.readTree(body);
            String orderNo   = firstNonEmpty(findFirst(root, "out_order_no"), findFirst(root, "out_trade_no"), findFirst(root, "merchant_out_trade_no"));
            String tradeNo   = firstNonEmpty(findFirst(root, "trade_no"), findFirst(root, "log_no"), findFirst(root, "acc_trade_no"));
            String rawStatus = firstNonEmpty(findFirst(root, "trade_status"), findFirst(root, "order_status"), findFirst(root, "trade_state"));
            String amountFen = firstNonEmpty(findFirst(root, "total_amount"), findFirst(root, "order_amount"), findFirst(root, "trade_amount"), findFirst(root, "pay_amount"));

            Map<String, String> result = new HashMap<>();
            result.put("orderNo", orderNo);
            result.put("tradeNo", tradeNo);
            result.put("tradeStatus", isSuccessStatus(rawStatus) ? "SUCCESS" : (rawStatus == null ? "UNKNOWN" : rawStatus));
            if (amountFen != null && !amountFen.isEmpty())
            {
                try
                {
                    result.put("paidAmountYuan", new BigDecimal(amountFen).movePointLeft(2).toPlainString());
                }
                catch (NumberFormatException nfe)
                {
                    log.warn("[Lakala] 通知金额解析失败: {}", amountFen);
                }
            }
            return result;
        }
        catch (Exception e)
        {
            log.error("[Lakala] 通知报文解析失败: {}", e.getMessage());
            // 验签已通过但解析异常：返回空 Map，避免上层空指针；上层据字段缺失不置 PAID
            return new HashMap<>();
        }
    }

    @Override
    public String successResponseBody()
    {
        return "{\"code\":\"SUCCESS\",\"message\":\"执行成功\"}";
    }

    private boolean isSuccessStatus(String status)
    {
        if (status == null)
        {
            return false;
        }
        return "SUCCESS".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status) || "2".equals(status);
    }

    private String firstNonEmpty(String... values)
    {
        if (values != null)
        {
            for (String v : values)
            {
                if (v != null && !v.isEmpty())
                {
                    return v;
                }
            }
        }
        return null;
    }

    /** 取顶层字段文本 */
    private String text(JsonNode node, String field)
    {
        if (node == null)
        {
            return null;
        }
        JsonNode v = node.get(field);
        return (v != null && !v.isNull()) ? v.asText() : null;
    }

    /** 在整棵 JSON 树中递归查找第一个名为 key 的字段的文本值（应对通知报文层级差异） */
    private String findFirst(JsonNode node, String key)
    {
        if (node == null)
        {
            return null;
        }
        if (node.isObject())
        {
            JsonNode direct = node.get(key);
            if (direct != null && direct.isValueNode() && !direct.isNull())
            {
                return direct.asText();
            }
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext())
            {
                String found = findFirst(it.next().getValue(), key);
                if (found != null)
                {
                    return found;
                }
            }
        }
        else if (node.isArray())
        {
            for (JsonNode child : node)
            {
                String found = findFirst(child, key);
                if (found != null)
                {
                    return found;
                }
            }
        }
        return null;
    }
}
