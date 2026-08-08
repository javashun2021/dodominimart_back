package com.ruoyi.mall.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.mall.domain.ImspayMerchant;
import com.ruoyi.mall.domain.PayOrder;
import com.ruoyi.mall.mapper.ImspayMerchantMapper;
import com.ruoyi.mall.mapper.OrderBlockMapper;
import com.ruoyi.mall.mapper.PayOrderMapper;
import com.ruoyi.mall.service.IMossPayService;
import com.ruoyi.mall.service.IPayOpenService;
import com.ruoyi.mall.util.PaySignUtil;

/**
 * 下游商户开放 API 编排实现。参照 亿林/直付通 协议。
 */
@Service
public class PayOpenServiceImpl implements IPayOpenService
{
    private static final Logger log = LoggerFactory.getLogger(PayOpenServiceImpl.class);

    // 返回码（对齐亿林语义）
    private static final String CODE_CREATE_OK = "4001"; // 下单成功
    private static final String CODE_SIGN_ERR  = "4002"; // 验签失败
    private static final String CODE_MERCHANT  = "4003"; // 商户不存在/停用
    private static final String CODE_PARAM     = "4004"; // 参数缺失
    private static final String CODE_BLOCKED   = "4005"; // 命中拉黑
    private static final String CODE_SYS       = "5000"; // 系统/上游异常
    private static final String CODE_QUERY_OK  = "0000"; // 查单成功
    private static final String CODE_QUERY_NF  = "0001"; // 订单不存在

    @Autowired private ImspayMerchantMapper merchantMapper;
    @Autowired private PayOrderMapper payOrderMapper;
    @Autowired private OrderBlockMapper orderBlockMapper;
    @Autowired private IMossPayService mossPayService;

    /** 拉黑校验开关（默认开） */
    @Value("${mall.pay.block-check:true}")
    private boolean blockCheck;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate()
    {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(8000);
        f.setReadTimeout(8000);
        return new RestTemplate(f);
    }

    // ----------------------------------------------------------------- 下单

    @Override
    public Map<String, Object> createPayment(String channelId, String siteCode, String requestId,
                                             Map<String, String> data, String sign)
    {
        // 1) 商户
        if (isBlank(siteCode))
        {
            return err(CODE_MERCHANT, "缺少商户号(Request-Site-Code)");
        }
        ImspayMerchant merchant = merchantMapper.selectByCode(siteCode);
        if (merchant == null || !merchant.isEnabled())
        {
            return err(CODE_MERCHANT, "商户不存在或已停用");
        }

        // 2) 验签（MD5，key=商户 app_secret）
        if (!PaySignUtil.verify(data, merchant.getAppSecret(), sign))
        {
            log.warn("[OpenPay] 验签失败 merchant={} orderNo={}", siteCode, data.get("orderNo"));
            return err(CODE_SIGN_ERR, "验签失败");
        }

        // 3) 必填校验
        String amountStr = trim(data.get("amount"));
        String outTradeNo = trim(data.get("orderNo"));
        String notifyUrl = trim(data.get("notifyUrl"));
        String clientIp = trim(data.get("clientIp"));
        String userId = trim(data.get("userId"));
        String currency = defaultIfBlank(trim(data.get("currency")), "CNY");
        String extra = trim(data.get("extra"));
        if (isBlank(amountStr) || isBlank(outTradeNo) || isBlank(notifyUrl) || isBlank(clientIp))
        {
            return err(CODE_PARAM, "缺少必填参数(amount/orderNo/notifyUrl/clientIp)");
        }
        BigDecimal amount;
        try
        {
            amount = new BigDecimal(amountStr).setScale(2, BigDecimal.ROUND_HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) <= 0)
            {
                return err(CODE_PARAM, "金额非法");
            }
        }
        catch (Exception e)
        {
            return err(CODE_PARAM, "金额格式错误");
        }

        // 4) 拉黑校验
        if (blockCheck && orderBlockMapper.countBlocked(userId, clientIp, null) > 0)
        {
            log.warn("[OpenPay] 命中拉黑 merchant={} userId={} ip={}", siteCode, userId, clientIp);
            return err(CODE_BLOCKED, "该用户已被拦截");
        }

        // 5) 幂等：同商户同 orderNo 已存在则复用
        PayOrder exist = payOrderMapper.selectByMerchantAndOutTradeNo(siteCode, outTradeNo);
        if (exist != null)
        {
            if (!isBlank(exist.getPayUrl()))
            {
                return okCreate(exist.getPayUrl());
            }
            return err(CODE_SYS, "订单已存在但无支付链接，请稍后重试");
        }

        // 6) 建单
        PayOrder order = new PayOrder();
        order.setPlatformNo(genPlatformNo());
        order.setMerchantId(merchant.getId());
        order.setMerchantCode(siteCode);
        order.setChannelId(channelId);
        order.setOutTradeNo(outTradeNo);
        order.setAmount(amount);
        order.setCurrency(currency);
        order.setUserId(userId);
        order.setClientIp(clientIp);
        order.setExtra(extra);
        order.setNotifyUrl(notifyUrl);
        order.setRequestId(requestId);
        order.setStatus("CREATED");
        order.setNotifyStatus(0);
        order.setNotifyCount(0);
        Date now = new Date();
        order.setCreateTime(now);
        order.setUpdateTime(now);

        // 7) 调上游拿 payurl
        String payUrl;
        try
        {
            String upNotify = buildUpstreamNotifyUrl();
            payUrl = mossPayService.createPayment(order.getPlatformNo(), amount,
                    "Order " + outTradeNo, upNotify, clientIp);
        }
        catch (Exception e)
        {
            log.error("[OpenPay] 上游下单失败 merchant={} orderNo={}: {}", siteCode, outTradeNo, e.getMessage());
            return err(CODE_SYS, "上游下单失败: " + e.getMessage());
        }
        order.setPayUrl(payUrl);

        try
        {
            payOrderMapper.insertPayOrder(order);
        }
        catch (org.springframework.dao.DuplicateKeyException dup)
        {
            // 并发下重复下单，复用已存在的
            PayOrder again = payOrderMapper.selectByMerchantAndOutTradeNo(siteCode, outTradeNo);
            if (again != null && !isBlank(again.getPayUrl()))
            {
                return okCreate(again.getPayUrl());
            }
            return err(CODE_SYS, "订单创建冲突");
        }

        return okCreate(payUrl);
    }

    // ----------------------------------------------------------------- 查单

    @Override
    public Map<String, Object> query(String platformNo)
    {
        PayOrder o = payOrderMapper.selectByPlatformNo(platformNo);
        if (o == null)
        {
            return err(CODE_QUERY_NF, "订单不存在");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", o.getId());
        data.put("orderNo", o.getOutTradeNo());
        data.put("platformOrderNo", o.getPlatformNo());
        data.put("tradeNo", o.getUpstreamNo());
        data.put("merchantCode", o.getMerchantCode());
        data.put("payPlatformChannelId", o.getChannelId());
        data.put("userId", o.getUserId());
        data.put("currency", o.getCurrency());
        data.put("amount", money(o.getAmount()));
        data.put("amountUnit", "Yuan_Two_Decimal");
        data.put("status", o.isPaid() ? "Paid" : "Unpaid");
        data.put("notifyStatus", o.getNotifyStatus() != null && o.getNotifyStatus() == 1 ? "Notify_Success" : "Notify_Pending");
        data.put("clientIp", o.getClientIp());
        data.put("createTime", o.getCreateTime());
        data.put("updateTime", o.getUpdateTime());

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("code", CODE_QUERY_OK);
        r.put("msg", "成功");
        r.put("data", data);
        return r;
    }

    // ------------------------------------------------------ 上游支付成功入口

    @Override
    public boolean handleUpstreamPaid(String platformNo, String upstreamNo, String rawBody)
    {
        int rows = payOrderMapper.markPaid(platformNo, upstreamNo, rawBody);
        if (rows == 0)
        {
            log.info("[OpenPay] handleUpstreamPaid 幂等跳过 platformNo={}", platformNo);
            return false;
        }
        // 触发回调下游（同步一次，失败留给定时任务补发）
        try
        {
            pushNotify(platformNo);
        }
        catch (Exception e)
        {
            log.error("[OpenPay] 支付成功后回调异常 platformNo={}: {}", platformNo, e.getMessage());
        }
        return true;
    }

    // ------------------------------------------------------------ 回调下游

    @Override
    public boolean pushNotify(String platformNo)
    {
        PayOrder o = payOrderMapper.selectByPlatformNo(platformNo);
        if (o == null || !o.isPaid() || isBlank(o.getNotifyUrl()))
        {
            return false;
        }
        ImspayMerchant merchant = merchantMapper.selectByCode(o.getMerchantCode());
        if (merchant == null)
        {
            log.warn("[OpenPay] 回调找不到商户 merchant={}", o.getMerchantCode());
            payOrderMapper.updateNotifyResult(platformNo, 2);
            return false;
        }

        // 组回调 data（亿林格式）
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("amount", money(o.getAmount()));
        data.put("channelId", o.getChannelId());
        data.put("orderNo", o.getOutTradeNo());
        data.put("payOrderNo", o.getPlatformNo());
        data.put("status", "Success");
        data.put("userId", o.getUserId());
        String sign = PaySignUtil.sign(data, merchant.getAppSecret());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", data);
        body.put("sign", sign);

        boolean ok = false;
        try
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String json = objectMapper.writeValueAsString(body);
            HttpEntity<String> req = new HttpEntity<>(json, headers);
            String resp = restTemplate.postForObject(o.getNotifyUrl(), req, String.class);
            ok = resp != null && resp.toLowerCase().contains("success");
            log.info("[OpenPay] 回调商户 platformNo={} url={} resp={} ok={}", platformNo, o.getNotifyUrl(), resp, ok);
        }
        catch (Exception e)
        {
            log.warn("[OpenPay] 回调商户失败 platformNo={} url={}: {}", platformNo, o.getNotifyUrl(), e.getMessage());
        }
        payOrderMapper.updateNotifyResult(platformNo, ok ? 1 : 2);
        return ok;
    }

    @Override
    public int retryPendingNotify(int limit)
    {
        java.util.List<PayOrder> pending = payOrderMapper.selectPendingNotify(limit);
        int ok = 0;
        for (PayOrder o : pending)
        {
            try
            {
                if (pushNotify(o.getPlatformNo()))
                {
                    ok++;
                }
            }
            catch (Exception e)
            {
                log.warn("[OpenPay] 补发回调异常 platformNo={}: {}", o.getPlatformNo(), e.getMessage());
            }
        }
        if (!pending.isEmpty())
        {
            log.info("[OpenPay] 回调补发：待补发 {} 单，本次成功 {} 单", pending.size(), ok);
        }
        return ok;
    }

    // ------------------------------------------------------------- helpers

    private Map<String, Object> okCreate(String payUrl)
    {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("code", CODE_CREATE_OK);
        r.put("data", payUrl);
        return r;
    }

    private Map<String, Object> err(String code, String msg)
    {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("code", code);
        r.put("msg", msg);
        return r;
    }

    /** 平台订单号：OL + 毫秒时间戳 + 6位随机 */
    private String genPlatformNo()
    {
        return "OL" + System.currentTimeMillis() + (100000 + ThreadLocalRandom.current().nextInt(900000));
    }

    /** 我方接收上游 MOSS 通知的地址 */
    @Value("${mall.pay.upstream-notify-url:http://localhost:8080/openapi/upstream/moss/notify}")
    private String upstreamNotifyUrl;

    private String buildUpstreamNotifyUrl()
    {
        return upstreamNotifyUrl;
    }

    private static String money(BigDecimal v)
    {
        if (v == null) return "0.00";
        return v.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String trim(String s) { return s == null ? null : s.trim(); }
    private static String defaultIfBlank(String s, String d) { return isBlank(s) ? d : s; }
}
