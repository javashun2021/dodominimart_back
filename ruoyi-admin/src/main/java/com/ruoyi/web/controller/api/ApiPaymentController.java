package com.ruoyi.web.controller.api;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.mall.domain.MallOrder;
import com.ruoyi.mall.domain.MallPaymentRecord;
import com.ruoyi.mall.mapper.MallPaymentRecordMapper;
import com.ruoyi.mall.service.IGCashService;
import com.ruoyi.mall.service.ILakalaPayService;
import com.ruoyi.mall.service.IMallOrderService;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 支付接口
 * POST /api/v1/orders/{id}/pay   — 发起支付（需 JWT）
 * POST /api/v1/payment/callback  — GCash Webhook（anon）
 */
@RestController
@RequestMapping("/api/v1")
public class ApiPaymentController extends BaseApiController
{
    private static final Logger log = LoggerFactory.getLogger(ApiPaymentController.class);

    @Autowired
    private IMallOrderService orderService;

    @Autowired
    private IGCashService gcashService;

    @Autowired
    private ILakalaPayService lakalaService;

    @Autowired
    private MallPaymentRecordMapper paymentRecordMapper;

    @Autowired
    private ISysConfigService configService;

    /**
     * 发起线上支付
     * POST /api/v1/orders/{id}/pay
     * Body: paymentMethod（表单参数）——默认 LAKALA（拉卡拉收银台）；GCASH 保留兼容。
     */
    @PostMapping("/orders/{id}/pay")
    public AjaxResult pay(@org.springframework.web.bind.annotation.PathVariable Long id,
                          @RequestParam(defaultValue = "LAKALA") String paymentMethod,
                          HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        MallOrder order = orderService.selectOrderById(id);
        if (order == null || !order.getMemberId().equals(memberId))
        {
            return AjaxResult.error("订单不存在");
        }
        if (!"0".equals(order.getStatus()))
        {
            return AjaxResult.error("当前订单状态不可支付");
        }
        if ("PAID".equals(order.getPaymentStatus()))
        {
            return AjaxResult.error("订单已支付");
        }

        // 应付 = 商品额(totalAmount) + 配送费(下单已固化在订单上)
        BigDecimal deliveryFee = order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO;
        BigDecimal payable = order.getTotalAmount().add(deliveryFee);

        String paymentUrl;
        if ("GCASH".equalsIgnoreCase(paymentMethod))
        {
            // 旧渠道保留兼容（照旧，不改动 GCash 逻辑）
            if (!"true".equalsIgnoreCase(configService.selectConfigByKey("mall.gcash.enabled")))
            {
                return AjaxResult.error("GCash 支付暂不可用");
            }
            paymentUrl = gcashService.createPayment(order.getOrderId(), payable, order.getOrderNo());
        }
        else
        {
            // 线上支付统一走拉卡拉收银台
            if (!lakalaService.isEnabled())
            {
                return AjaxResult.error("拉卡拉支付暂不可用");
            }
            paymentUrl = lakalaService.createCounterPayment(order.getOrderNo(), payable,
                    "订单 " + order.getOrderNo(), String.valueOf(memberId));
        }

        // 记录支付流水
        MallPaymentRecord record = new MallPaymentRecord();
        record.setOrderId(order.getOrderId());
        record.setMemberId(memberId);
        record.setAmount(payable);
        record.setStatus("PENDING");
        record.setCreateTime(new Date());
        record.setUpdateTime(new Date());
        paymentRecordMapper.insertPaymentRecord(record);

        return AjaxResult.success("ok")
                .put("paymentUrl", paymentUrl)
                .put("orderId", order.getOrderId())
                .put("amount", payable);
    }

    /**
     * GCash 支付回调 Webhook（无需 JWT）
     * POST /api/v1/payment/callback
     */
    @PostMapping("/payment/callback")
    public String handleCallback(HttpServletRequest request) throws IOException
    {
        byte[] bodyBytes = StreamUtils.copyToByteArray(request.getInputStream());
        String rawBody = new String(bodyBytes, "UTF-8");

        // 获取所有请求头用于验签
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements())
        {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }

        log.info("GCash callback received: {}", rawBody);

        if (!gcashService.verifyCallback(headers, rawBody))
        {
            log.warn("GCash callback signature verification failed");
            return "INVALID_SIGNATURE";
        }

        Map<String, String> parsed = gcashService.parseCallback(rawBody);
        String orderNo    = parsed.get("orderNo");
        String status     = parsed.get("status");
        String paymentNo  = parsed.get("paymentNo");

        if (!"SUCCESS".equals(status))
        {
            log.info("PayMongo payment not success for orderNo={}", orderNo);
            return "OK";
        }

        // 不信任 webhook 报文：反查 PayMongo 拿「权威」状态 + 实付金额
        Map<String, String> verified = gcashService.verifyPayment(paymentNo);
        if (verified == null || !"PAID".equals(verified.get("status")))
        {
            log.warn("PayMongo verifyPayment not paid, refuse to mark. orderNo={}, paymentNo={}", orderNo, paymentNo);
            return "OK";
        }

        BigDecimal paidAmount;
        try
        {
            long centavos = Long.parseLong(verified.get("amount"));
            if (centavos < 0)
            {
                throw new NumberFormatException("negative amount");
            }
            paidAmount = new BigDecimal(centavos).movePointLeft(2); // 分 -> 元
        }
        catch (Exception e)
        {
            log.error("PayMongo verify amount invalid for orderNo={}: {}", orderNo, verified.get("amount"));
            return "OK";
        }

        try
        {
            // markOrderPaid 内部再做金额比对（paidAmount != 订单应付则抛错不置 PAID）
            orderService.markOrderPaid(orderNo, verified.get("paymentNo"), paidAmount);
        }
        catch (Exception e)
        {
            log.error("markOrderPaid failed for orderNo={}: {}", orderNo, e.getMessage());
        }
        return "OK";
    }

    /**
     * 拉卡拉收银台支付异步通知（无需 JWT）
     * POST /api/v1/payment/lakala/callback
     * 由 SDK 完成验签；成功需回 {"code":"SUCCESS",...} 停止重复通知。
     */
    @PostMapping("/payment/lakala/callback")
    public String handleLakalaCallback(HttpServletRequest request)
    {
        Map<String, String> parsed = lakalaService.handleNotify(request);
        if (parsed == null)
        {
            // 验签失败：回 FAIL，拉卡拉将按策略重试
            log.warn("Lakala callback signature verification failed");
            return "FAIL";
        }

        String orderNo = parsed.get("orderNo");
        String status  = parsed.get("tradeStatus");
        String tradeNo = parsed.get("tradeNo");
        String amtStr  = parsed.get("paidAmountYuan");

        if (!"SUCCESS".equals(status))
        {
            log.info("Lakala payment not success, orderNo={}, status={}", orderNo, status);
            return lakalaService.successResponseBody();
        }
        if (orderNo == null || amtStr == null)
        {
            log.warn("Lakala callback missing fields, orderNo={}, amount={}", orderNo, amtStr);
            return lakalaService.successResponseBody();
        }

        try
        {
            // markOrderPaid 内部再做金额比对（paidAmount != 订单应付则抛错不置 PAID）
            BigDecimal paidAmount = new BigDecimal(amtStr);
            orderService.markOrderPaid(orderNo, tradeNo, paidAmount);
        }
        catch (Exception e)
        {
            log.error("Lakala markOrderPaid failed for orderNo={}: {}", orderNo, e.getMessage());
        }
        return lakalaService.successResponseBody();
    }
}