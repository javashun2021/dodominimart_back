package com.ruoyi.mall.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ruoyi.mall.domain.MallMember;
import com.ruoyi.mall.domain.MallOrder;
import com.ruoyi.mall.domain.MallOrderItem;
import com.ruoyi.mall.domain.MallProduct;
import com.ruoyi.mall.mapper.MallMemberMapper;
import com.ruoyi.mall.mapper.MallOrderMapper;
import com.ruoyi.mall.mapper.MallProductMapper;
import com.ruoyi.mall.service.IExternalOrderService;
import com.ruoyi.mall.service.IMallOrderService;
import com.ruoyi.mall.util.AmountComposer;

/**
 * 对外「订单导入」编排实现。
 */
@Service
public class ExternalOrderServiceImpl implements IExternalOrderService
{
    private static final Logger log = LoggerFactory.getLogger(ExternalOrderServiceImpl.class);

    private static final String CODE_OK      = "0000"; // 成功
    private static final String CODE_PARAM   = "4004"; // 参数缺失/格式错误
    private static final String CODE_NOMATCH = "4006"; // 无法匹配商品
    private static final String CODE_SYS     = "5000"; // 系统异常

    @Autowired private MallMemberMapper memberMapper;
    @Autowired private MallProductMapper productMapper;
    @Autowired private MallOrderMapper orderMapper;
    @Autowired private IMallOrderService mallOrderService;

    /** 组合兜底最多件数 */
    @Value("${mall.external.compose-max-items:6}")
    private int composeMaxItems;

    @Override
    public Map<String, Object> importOrder(String outOrderNo, String amountStr, String userId, String phone,
                                           String floatAmountStr, String orderTimeStr, String payTimeStr)
    {
        // 1) 必填
        if (isBlank(outOrderNo) || isBlank(amountStr) || isBlank(userId))
        {
            return err(CODE_PARAM, "缺少必填参数(outOrderNo/amount/userId)");
        }
        // 2) 金额
        BigDecimal amount;
        try
        {
            amount = new BigDecimal(amountStr.trim()).setScale(2, BigDecimal.ROUND_HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) return err(CODE_PARAM, "付款价非法");
        }
        catch (Exception e) { return err(CODE_PARAM, "付款价格式错误"); }

        // 平台补助 [0,1) 且 ≤ 付款价
        BigDecimal floatAmount;
        try
        {
            floatAmount = isBlank(floatAmountStr) ? BigDecimal.ZERO
                    : new BigDecimal(floatAmountStr.trim()).setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        catch (Exception e) { return err(CODE_PARAM, "平台补助格式错误"); }
        if (floatAmount.compareTo(BigDecimal.ZERO) < 0
                || floatAmount.compareTo(BigDecimal.ONE) >= 0
                || floatAmount.compareTo(amount) > 0)
        {
            return err(CODE_PARAM, "平台补助须在[0.00,1.00)且不超过付款价");
        }

        // 时间
        Date orderTime, payTime;
        try { orderTime = parseTime(orderTimeStr); } catch (Exception e) { return err(CODE_PARAM, "orderTime 格式错误(yyyy-MM-dd HH:mm:ss)"); }
        try { payTime = parseTime(payTimeStr); }   catch (Exception e) { return err(CODE_PARAM, "payTime 格式错误(yyyy-MM-dd HH:mm:ss)"); }

        // 3) 幂等：同一外部订单号已导入则直接返回
        MallOrder exist = orderMapper.selectOrderByMerchantOutTradeNo(outOrderNo.trim());
        if (exist != null)
        {
            return ok(exist.getOrderNo(), effectivePaid(amount, floatAmount));
        }

        // 4) find-or-create 会员
        Long memberId = findOrCreateMemberByExternalId(userId, phone);

        // 5) 选品：优先 ≥付款价 的最接近单品；否则多商品组合
        long payCents = amount.movePointRight(2).setScale(0, BigDecimal.ROUND_HALF_UP).longValueExact();
        Map<Long, Integer> pool = loadProductPool();
        List<MallOrderItem> items = pickItems(pool, payCents);
        if (items == null || items.isEmpty())
        {
            log.info("[Import] 无法匹配商品 outOrderNo={} amount={} 商品池={}", outOrderNo, amount, pool.size());
            return err(CODE_NOMATCH, "无法用现有商品匹配该金额");
        }

        // 6) 建单(含差价补齐 + 平台补助 + 已支付则模拟配送)
        MallOrder order;
        try
        {
            order = mallOrderService.createExternalPaidOrder(memberId, items, outOrderNo.trim(),
                    orderTime, payTime, amount, floatAmount);
        }
        catch (Exception e)
        {
            log.error("[Import] 建单失败 outOrderNo={}: {}", outOrderNo, e.getMessage());
            return err(CODE_SYS, "建单失败: " + e.getMessage());
        }

        return ok(order.getOrderNo(), effectivePaid(amount, floatAmount));
    }

    /** 选品：先取 ≥付款价 的最接近单品；无则 composeAtLeast 组合 */
    private List<MallOrderItem> pickItems(Map<Long, Integer> pool, long payCents)
    {
        // 单品：价 ≥ 付款价 中价最低者
        Long bestId = null;
        int bestCents = Integer.MAX_VALUE;
        for (Map.Entry<Long, Integer> e : pool.entrySet())
        {
            int c = e.getValue();
            if (c >= payCents && c < bestCents)
            {
                bestCents = c;
                bestId = e.getKey();
            }
        }
        if (bestId != null)
        {
            List<MallOrderItem> items = new ArrayList<>();
            items.add(item(bestId, 1));
            return items;
        }
        // 组合兜底
        List<AmountComposer.Pick> picks = AmountComposer.composeAtLeast(pool, payCents, composeMaxItems);
        if (picks == null || picks.isEmpty()) return null;
        List<MallOrderItem> items = new ArrayList<>();
        for (AmountComposer.Pick p : picks)
        {
            items.add(item(p.productId, p.quantity));
        }
        return items;
    }

    private static MallOrderItem item(Long productId, int qty)
    {
        MallOrderItem it = new MallOrderItem();
        it.setProductId(productId);
        it.setQuantity(qty);
        return it;
    }

    /** 活跃商品池：productId -> 单价(分)；同价保留库存高者 */
    private Map<Long, Integer> loadProductPool()
    {
        MallProduct q = new MallProduct();
        q.setStatus("0");
        q.setInStockOnly(true);
        List<MallProduct> list = productMapper.selectProductList(q);
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

    /**
     * 按外部会员标识(字符串) find-or-create 商城会员。
     * 主键匹配用独立列 external_id(与内部 member_id 解耦);email 仅作占位保证唯一。
     */
    private Long findOrCreateMemberByExternalId(String userId, String phone)
    {
        String uid = userId.trim();
        MallMember m = memberMapper.selectMemberByExternalId(uid);
        if (m != null)
        {
            return m.getMemberId();
        }
        m = new MallMember();
        m.setExternalId(uid);
        m.setEmail(("ext_" + uid + "@pay.local").toLowerCase()); // 占位邮箱，避免 email 唯一冲突/空值
        m.setNickName(uid);
        if (!isBlank(phone)) m.setPhone(phone.trim());
        m.setStatus("0");
        m.setCreateTime(new Date());
        memberMapper.insertMember(m);
        return m.getMemberId();
    }

    private static BigDecimal effectivePaid(BigDecimal amount, BigDecimal floatAmount)
    {
        return amount.subtract(floatAmount == null ? BigDecimal.ZERO : floatAmount).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private static Date parseTime(String s) throws Exception
    {
        if (isBlank(s)) return null;
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        fmt.setLenient(false);
        return fmt.parse(s.trim());
    }

    private Map<String, Object> ok(String orderNo, BigDecimal paidAmount)
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", orderNo);
        data.put("paidAmount", paidAmount.toPlainString());
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("code", CODE_OK);
        r.put("msg", "成功");
        r.put("data", data);
        return r;
    }

    private Map<String, Object> err(String code, String msg)
    {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("code", code);
        r.put("msg", msg);
        return r;
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
