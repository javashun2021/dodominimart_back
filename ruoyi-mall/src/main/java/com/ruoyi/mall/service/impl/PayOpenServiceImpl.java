package com.ruoyi.mall.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
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
import com.ruoyi.mall.domain.MallMember;
import com.ruoyi.mall.domain.MallOrder;
import com.ruoyi.mall.domain.MallOrderItem;
import com.ruoyi.mall.domain.MallProduct;
import com.ruoyi.mall.domain.PayOrder;
import com.ruoyi.mall.mapper.ImspayMerchantMapper;
import com.ruoyi.mall.mapper.MallMemberMapper;
import com.ruoyi.mall.mapper.MallProductMapper;
import com.ruoyi.mall.mapper.OrderBlockMapper;
import com.ruoyi.mall.mapper.PayOrderMapper;
import com.ruoyi.mall.service.IMallOrderService;
import com.ruoyi.mall.service.IMossPayService;
import com.ruoyi.mall.service.IPayOpenService;
import com.ruoyi.mall.util.AmountComposer;
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
    private static final String CODE_NOMATCH   = "4006"; // 商品凑单失败
    private static final String CODE_SYS       = "5000"; // 系统/上游异常
    private static final String CODE_QUERY_OK  = "0000"; // 查单成功
    private static final String CODE_QUERY_NF  = "0001"; // 订单不存在

    @Autowired private ImspayMerchantMapper merchantMapper;
    @Autowired private PayOrderMapper payOrderMapper;
    @Autowired private OrderBlockMapper orderBlockMapper;
    @Autowired private IMossPayService mossPayService;
    @Autowired private IMallOrderService mallOrderService;
    @Autowired private MallMemberMapper memberMapper;
    @Autowired private MallProductMapper productMapper;

    /** 拉黑校验开关（默认开） */
    @Value("${mall.pay.block-check:true}")
    private boolean blockCheck;

    /** 撮合最多件数（含数量） */
    @Value("${mall.pay.compose-max-items:4}")
    private int composeMaxItems;

    /** 浮动补贴上限（元）：实付 = 名义额 − rand(0, floatMax)；≤0 表示不浮动 */
    @Value("${mall.pay.float-max:0.50}")
    private BigDecimal floatMax;

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

        // 6) 商品撮合：从商品库凑出等于名义额的组合
        long targetCents = amount.movePointRight(2).setScale(0, BigDecimal.ROUND_HALF_UP).longValueExact();
        Map<Long, Integer> pool = loadProductPool();
        List<AmountComposer.Pick> picks = AmountComposer.compose(pool, targetCents, composeMaxItems);
        if (picks == null || picks.isEmpty())
        {
            log.info("[OpenPay] 凑单失败 merchant={} amount={} 商品池={}", siteCode, amount, pool.size());
            return err(CODE_NOMATCH, "无法用现有商品组成该金额");
        }
        List<MallOrderItem> items = new ArrayList<>();
        for (AmountComposer.Pick p : picks)
        {
            MallOrderItem it = new MallOrderItem();
            it.setProductId(p.productId);
            it.setQuantity(p.quantity);
            items.add(it);
        }

        // 7) 会员 find-or-create（按 商户+userId）
        Long memberId = findOrCreateMember(siteCode, userId);

        // 8) 浮动金额（向下补贴）
        BigDecimal payAmount = floatDown(amount);
        BigDecimal subsidy = amount.subtract(payAmount);

        // 9) 建商城订单（真实扣库存）；失败即拒单
        MallOrder mallOrder;
        try
        {
            mallOrder = mallOrderService.createAggregateOrder(memberId, items, outTradeNo, subsidy);
        }
        catch (Exception e)
        {
            log.error("[OpenPay] 建商城订单失败 merchant={} orderNo={}: {}", siteCode, outTradeNo, e.getMessage());
            return err(CODE_SYS, "建单失败: " + e.getMessage());
        }

        // 10) 调上游 MOSS：outTradeNo=商城订单号，金额=浮动后实付
        String payUrl;
        try
        {
            String upNotify = buildUpstreamNotifyUrl();
            payUrl = mossPayService.createPayment(mallOrder.getOrderNo(), payAmount,
                    "Order " + outTradeNo, upNotify, clientIp);
        }
        catch (Exception e)
        {
            log.error("[OpenPay] 上游下单失败 merchant={} mallOrderNo={}: {}", siteCode, mallOrder.getOrderNo(), e.getMessage());
            // 补偿：还原库存 + 取消商城订单
            try { mallOrderService.cancelAggregateOrder(mallOrder.getOrderNo(), "MOSS下单失败"); }
            catch (Exception ex) { log.error("[OpenPay] 补偿取消失败 {}: {}", mallOrder.getOrderNo(), ex.getMessage()); }
            return err(CODE_SYS, "上游下单失败: " + e.getMessage());
        }

        // 11) pay_order 落库
        PayOrder order = new PayOrder();
        order.setPlatformNo(genPlatformNo());
        order.setMerchantId(merchant.getId());
        order.setMerchantCode(siteCode);
        order.setChannelId(channelId);
        order.setOutTradeNo(outTradeNo);
        order.setAmount(amount);            // 名义额（回调给商户）
        order.setPayAmount(payAmount);      // 实付 MOSS
        order.setSubsidy(subsidy);
        order.setMallOrderId(mallOrder.getOrderId());
        order.setMallOrderNo(mallOrder.getOrderNo());
        order.setCurrency(currency);
        order.setUserId(userId);
        order.setClientIp(clientIp);
        order.setExtra(extra);
        order.setNotifyUrl(notifyUrl);
        order.setRequestId(requestId);
        order.setPayUrl(payUrl);
        order.setStatus("CREATED");
        order.setNotifyStatus(0);
        order.setNotifyCount(0);
        Date now = new Date();
        order.setCreateTime(now);
        order.setUpdateTime(now);

        try
        {
            payOrderMapper.insertPayOrder(order);
        }
        catch (org.springframework.dao.DuplicateKeyException dup)
        {
            // 并发下重复下单：作废本次商城订单，复用已存在的 payurl
            try { mallOrderService.cancelAggregateOrder(mallOrder.getOrderNo(), "重复下单，作废"); } catch (Exception ignore) {}
            PayOrder again = payOrderMapper.selectByMerchantAndOutTradeNo(siteCode, outTradeNo);
            if (again != null && !isBlank(again.getPayUrl()))
            {
                return okCreate(again.getPayUrl());
            }
            return err(CODE_SYS, "订单创建冲突");
        }

        return okCreate(payUrl);
    }

    /** 活跃商品池：productId -> 单价(分)；同价保留库存高者（供撮合去重用） */
    private Map<Long, Integer> loadProductPool()
    {
        MallProduct q = new MallProduct();
        q.setStatus("0");
        q.setInStockOnly(true);
        List<MallProduct> list = productMapper.selectProductList(q);
        // 库存降序：撮合按 putIfAbsent 保留先出现者 → 同价优先高库存
        list.sort(Comparator.comparingInt((MallProduct p) -> p.getStock() == null ? 0 : p.getStock()).reversed());
        Map<Long, Integer> pool = new LinkedHashMap<>();
        for (MallProduct p : list)
        {
            if (p.getPrice() == null) continue;
            int cents = p.getPrice().movePointRight(2).setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
            if (cents > 0) pool.put(p.getProductId(), cents);
        }
        return pool;
    }

    /** 按 (商户,userId) 找或建商城会员，返回 memberId */
    private Long findOrCreateMember(String siteCode, String userId)
    {
        String uid = isBlank(userId) ? "anon" : userId.trim();
        String email = ("agg_" + siteCode + "_" + uid + "@pay.local").toLowerCase();
        MallMember m = memberMapper.selectMemberByEmail(email);
        if (m != null)
        {
            return m.getMemberId();
        }
        m = new MallMember();
        m.setEmail(email);
        m.setNickName(uid);
        m.setStatus("0");
        m.setCreateTime(new Date());
        memberMapper.insertMember(m);
        return m.getMemberId();
    }

    /** 名义额向下浮动：减去 [0, floatMax] 的随机补贴，至少保留 1 分 */
    private BigDecimal floatDown(BigDecimal amount)
    {
        if (floatMax == null || floatMax.compareTo(BigDecimal.ZERO) <= 0)
        {
            return amount;
        }
        long amtC = amount.movePointRight(2).setScale(0, BigDecimal.ROUND_HALF_UP).longValueExact();
        long maxSub = floatMax.movePointRight(2).setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
        maxSub = Math.min(maxSub, Math.max(0, amtC - 1)); // 至少留 1 分
        if (maxSub <= 0)
        {
            return amount;
        }
        long sub = ThreadLocalRandom.current().nextLong(0, maxSub + 1);
        return BigDecimal.valueOf(amtC - sub, 2);
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
    public boolean handleUpstreamPaid(String mallOrderNo, String upstreamNo, String rawBody)
    {
        // MOSS 回来的 outTradeNo = 商城订单号
        PayOrder o = payOrderMapper.selectByMallOrderNo(mallOrderNo);
        if (o == null)
        {
            log.warn("[OpenPay] handleUpstreamPaid 找不到聚合订单 mallOrderNo={}", mallOrderNo);
            return false;
        }
        // 先结算商城订单（浮动实付，绕开严格等额校验）
        try
        {
            mallOrderService.settleAggregateOrderPaid(mallOrderNo, upstreamNo, o.getPayAmount());
        }
        catch (Exception e)
        {
            log.error("[OpenPay] 结算商城订单失败 mallOrderNo={}: {}", mallOrderNo, e.getMessage());
        }
        // 置 pay_order=PAID（幂等：已支付返回 0）
        int rows = payOrderMapper.markPaid(o.getPlatformNo(), upstreamNo, rawBody);
        if (rows == 0)
        {
            log.info("[OpenPay] handleUpstreamPaid 幂等跳过 platformNo={}", o.getPlatformNo());
            return false;
        }
        // 触发回调下游（同步一次，失败留给定时任务补发）
        try
        {
            pushNotify(o.getPlatformNo());
        }
        catch (Exception e)
        {
            log.error("[OpenPay] 支付成功后回调异常 platformNo={}: {}", o.getPlatformNo(), e.getMessage());
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
