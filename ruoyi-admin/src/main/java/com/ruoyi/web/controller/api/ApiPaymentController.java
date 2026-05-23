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
import com.ruoyi.mall.service.IMallOrderService;

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
    private MallPaymentRecordMapper paymentRecordMapper;

    /**
     * 发起 GCash 支付
     * POST /api/v1/orders/{id}/pay
     * Body: paymentMethod=GCASH（表单参数）
     */
    @PostMapping("/orders/{id}/pay")
    public AjaxResult pay(@org.springframework.web.bind.annotation.PathVariable Long id,
                          @RequestParam(defaultValue = "GCASH") String paymentMethod,
                          HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        MallOrder order = orderService.selectOrderById(id);
        if (order == null || !order.getMemberId().equals(memberId))
        {
            return AjaxResult.error("Order not found");
        }
        if (!"0".equals(order.getStatus()))
        {
            return AjaxResult.error("Order cannot be paid in current status");
        }
        if ("PAID".equals(order.getPaymentStatus()))
        {
            return AjaxResult.error("Order already paid");
        }

        String paymentUrl = gcashService.createPayment(order.getOrderId(),
                order.getTotalAmount(), order.getOrderNo());

        // 记录支付流水
        MallPaymentRecord record = new MallPaymentRecord();
        record.setOrderId(order.getOrderId());
        record.setMemberId(memberId);
        record.setAmount(order.getTotalAmount());
        record.setStatus("PENDING");
        record.setCreateTime(new Date());
        record.setUpdateTime(new Date());
        paymentRecordMapper.insertPaymentRecord(record);

        return AjaxResult.success("ok")
                .put("paymentUrl", paymentUrl)
                .put("orderId", order.getOrderId())
                .put("amount", order.getTotalAmount());
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

        if ("SUCCESS".equals(status))
        {
            try
            {
                orderService.markOrderPaid(orderNo, paymentNo, null);
            }
            catch (Exception e)
            {
                log.error("markOrderPaid failed for orderNo={}: {}", orderNo, e.getMessage());
            }
        }
        else
        {
            log.info("GCash payment failed for orderNo={}", orderNo);
        }
        return "OK";
    }
}