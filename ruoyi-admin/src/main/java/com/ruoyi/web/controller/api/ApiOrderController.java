package com.ruoyi.web.controller.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.mall.domain.MallAddress;
import com.ruoyi.mall.domain.MallOrder;
import com.ruoyi.mall.domain.MallOrderItem;
import com.ruoyi.mall.domain.MallRunnerRating;
import com.ruoyi.mall.domain.MallRunnerApplication;
import com.ruoyi.mall.service.IMallAddressService;
import com.ruoyi.mall.service.IMallOrderService;
import com.ruoyi.mall.service.IMallRunnerService;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 订单接口（需 JWT）
 * POST   /api/v1/orders
 * GET    /api/v1/orders
 * GET    /api/v1/orders/{id}
 * POST   /api/v1/orders/{id}/cancel
 */
@RestController
@RequestMapping("/api/v1/orders")
public class ApiOrderController extends BaseApiController
{
    @Autowired
    private IMallOrderService orderService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private IMallRunnerService runnerService;

    @Autowired
    private IMallAddressService addressService;

    /**
     * 下单
     * Body: { "addressId": 1, "paymentMethod": "COD|GCASH", "remark": "...", "items": [...] }
     */
    @PostMapping
    public AjaxResult createOrder(@RequestBody Map<String, Object> body, HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);

        String remark        = (String) body.getOrDefault("remark", "");
        String paymentMethod = body.get("paymentMethod") != null
                ? body.get("paymentMethod").toString().toUpperCase() : "COD";
        boolean isStore = "STORE".equals(paymentMethod);

        if ("GCASH".equals(paymentMethod) && !isGcashEnabled())
        {
            return AjaxResult.error("GCash payment is currently unavailable");
        }

        // 到店单（STORE）无需配送地址；其余支付方式需指定或使用默认地址
        Long addressId = null;
        Object addressIdObj = body.get("addressId");
        if (addressIdObj != null)
        {
            addressId = Long.valueOf(addressIdObj.toString());
        }
        else if (!isStore)
        {
            MallAddress defaultAddr = addressService.selectDefaultAddressByMemberId(memberId);
            if (defaultAddr == null)
            {
                return AjaxResult.error("Please set a delivery address");
            }
            addressId = defaultAddr.getAddressId();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawItems = (List<Map<String, Object>>) body.get("items");
        if (rawItems == null || rawItems.isEmpty())
        {
            return AjaxResult.error("items cannot be empty");
        }

        List<MallOrderItem> items = new ArrayList<>();
        for (Map<String, Object> raw : rawItems)
        {
            Object productIdObj = raw.get("productId");
            Object quantityObj  = raw.get("quantity");
            if (productIdObj == null || quantityObj == null)
            {
                return AjaxResult.error("Each item must have productId and quantity");
            }
            int qty = Integer.valueOf(quantityObj.toString());
            if (qty <= 0)
            {
                return AjaxResult.error("quantity must be greater than 0");
            }
            MallOrderItem item = new MallOrderItem();
            item.setProductId(Long.valueOf(productIdObj.toString()));
            item.setQuantity(qty);
            items.add(item);
        }

        int pointsToUse = 0;
        Object ptsObj = body.get("pointsToUse");
        if (ptsObj != null)
        {
            try { pointsToUse = Integer.parseInt(ptsObj.toString()); } catch (NumberFormatException ignored) {}
        }

        Long memberCouponId = null;
        Object couponObj = body.get("memberCouponId");
        if (couponObj != null)
        {
            try { memberCouponId = Long.parseLong(couponObj.toString()); } catch (NumberFormatException ignored) {}
        }

        try
        {
            MallOrder order = orderService.createOrder(memberId, addressId, items, remark, paymentMethod, pointsToUse, memberCouponId);
            return AjaxResult.success("Order placed successfully").put("data", order);
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    private boolean isGcashEnabled()
    {
        return "true".equalsIgnoreCase(configService.selectConfigByKey("mall.gcash.enabled"));
    }

    /**
     * 我的订单列表
     * ?status=&pageNum=1&pageSize=10
     */
    @GetMapping
    public AjaxResult listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        startPage(pageNum, pageSize);
        MallOrder query = new MallOrder();
        query.setMemberId(memberId);
        query.setStatus(status);
        List<MallOrder> list = orderService.selectOrderList(query);
        return pageResult(new PageInfo<>(list));
    }

    /** 订单详情（含明细） */
    @GetMapping("/{id}")
    public AjaxResult getOrder(@PathVariable Long id, HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        MallOrder order = orderService.selectOrderById(id);
        if (order == null || !order.getMemberId().equals(memberId))
        {
            return AjaxResult.error("Order not found");
        }
        if (order.getRunnerMemberId() != null)
        {
            MallRunnerApplication app = runnerService.getApplication(order.getRunnerMemberId());
            if (app != null)
            {
                order.setRunnerPhone(app.getPhone());
            }
        }
        return AjaxResult.success("ok").put("data", order);
    }

    /**
     * 顾客评价跑腿人
     * Body: { "score": 5, "comment": "..." }
     */
    @PostMapping("/{id}/rate-runner")
    public AjaxResult rateRunner(@PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        Object scoreObj = body.get("score");
        if (scoreObj == null)
        {
            return AjaxResult.error("score is required");
        }
        int score = Integer.valueOf(scoreObj.toString());
        if (score < 1 || score > 5)
        {
            return AjaxResult.error("score must be between 1 and 5");
        }
        MallRunnerRating rating = new MallRunnerRating();
        rating.setOrderId(id);
        rating.setRaterMemberId(memberId);
        rating.setScore(score);
        rating.setComment(body.get("comment") != null ? body.get("comment").toString() : null);
        try
        {
            runnerService.rateRunner(rating);
            return AjaxResult.success("Rating submitted");
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 取消订单（仅 PENDING 状态可取消）
     * Body: { "reason": "..." }（可选）
     */
    @PostMapping("/{id}/cancel")
    public AjaxResult cancelOrder(@PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        try
        {
            orderService.cancelOrder(id, memberId, reason);
            return AjaxResult.success("Order cancelled");
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }
}
