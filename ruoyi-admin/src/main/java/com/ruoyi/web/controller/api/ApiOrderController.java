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
import com.ruoyi.mall.domain.MallMember;
import com.ruoyi.mall.domain.MallOrder;
import com.ruoyi.mall.domain.MallOrderItem;
import com.ruoyi.mall.domain.MallRunnerRating;
import com.ruoyi.mall.domain.MallRunnerApplication;
import com.ruoyi.mall.domain.MallRefundRequest;
import com.ruoyi.mall.service.IMallAddressService;
import com.ruoyi.mall.service.IMallMemberService;
import com.ruoyi.mall.service.IMallOrderService;
import com.ruoyi.mall.service.IMallRunnerService;
import com.ruoyi.mall.service.IMallStoreService;
import com.ruoyi.mall.domain.MallStore;
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

    @Autowired
    private IMallMemberService memberService;

    @Autowired
    private IMallStoreService storeService;

    @Autowired
    private com.ruoyi.mall.service.TelegramNotifyService telegramNotifyService;

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
            return AjaxResult.error("GCash 支付暂不可用");
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
                return AjaxResult.error("请设置收货地址");
            }
            addressId = defaultAddr.getAddressId();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawItems = (List<Map<String, Object>>) body.get("items");
        if (rawItems == null || rawItems.isEmpty())
        {
            return AjaxResult.error("商品项不能为空");
        }

        List<MallOrderItem> items = new ArrayList<>();
        for (Map<String, Object> raw : rawItems)
        {
            Object productIdObj = raw.get("productId");
            Object quantityObj  = raw.get("quantity");
            if (productIdObj == null || quantityObj == null)
            {
                return AjaxResult.error("每个商品项必须包含 productId 和 quantity");
            }
            int qty = Integer.valueOf(quantityObj.toString());
            if (qty <= 0)
            {
                return AjaxResult.error("数量必须大于 0");
            }
            MallOrderItem item = new MallOrderItem();
            item.setProductId(Long.valueOf(productIdObj.toString()));
            item.setQuantity(qty);
            // 单层属性所选值快照（如「苹果味」），无属性商品为空
            Object specObj = raw.get("spec");
            if (specObj != null && !specObj.toString().trim().isEmpty())
            {
                item.setSpec(specObj.toString().trim());
            }
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

        // 配送费（顾客侧）取系统参数 app.delivery.fee，下单时固化进订单；到店/免配送在 service 里归 0。
        java.math.BigDecimal deliveryFee = java.math.BigDecimal.ZERO;
        try
        {
            String df = configService.selectConfigByKey("app.delivery.fee");
            if (df != null && !df.trim().isEmpty())
            {
                deliveryFee = new java.math.BigDecimal(df.trim());
            }
        }
        catch (Exception ignored) {}

        // 归属门店（模型C）：App 结账带 storeId。校验为营业中门店，缺失/非法则回退到默认门店，
        // 保证配送单一定有 store_id（否则骑手抢单池按店过滤会漏单）。到店单不参与配送，不强制。
        Long storeId = null;
        Object storeObj = body.get("storeId");
        if (storeObj != null)
        {
            try { storeId = Long.parseLong(storeObj.toString()); } catch (NumberFormatException ignored) {}
        }
        MallStore store = (storeId != null) ? storeService.selectStoreById(storeId) : null;
        boolean storeUsable = store != null && "0".equals(store.getStatus()) && !"2".equals(store.getDelFlag());
        if (!isStore && !storeUsable)
        {
            store = storeService.selectNearestStore(null, null); // 兜底：排序最前的营业门店
            storeUsable = store != null;
        }
        storeId = storeUsable ? store.getStoreId() : null;

        try
        {
            MallOrder order = orderService.createOrder(memberId, addressId, items, remark, paymentMethod, pointsToUse, memberCouponId, deliveryFee, storeId);

            // Telegram 后台提醒：新订单
            try
            {
                MallMember buyer = memberService.selectMemberById(memberId);
                String name = buyer != null ? buyer.getNickName() : ("#" + memberId);
                String storeName = (store != null) ? store.getName() : "-";
                telegramNotifyService.notify("🛒 New Order " + order.getOrderNo()
                        + "\nStore: " + storeName
                        + "\nCustomer: " + name
                        + "\nAmount: ₱" + order.getTotalAmount()
                        + "\nPayment: " + paymentMethod);
            }
            catch (Exception ignored) {}

            return AjaxResult.success("下单成功").put("data", order);
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
            return AjaxResult.error("订单不存在");
        }
        if (order.getRunnerMemberId() != null)
        {
            MallRunnerApplication app = runnerService.getApplication(order.getRunnerMemberId());
            if (app != null)
            {
                order.setRunnerPhone(app.getPhone());
            }
        }
        // 投递联系电话：新订单已写入地址快照(phone)，此处兜底填会员手机号，保证老订单也能展示
        try
        {
            MallMember member = memberService.selectMemberById(order.getMemberId());
            if (member != null)
            {
                order.setCustomerPhone(member.getPhone());
            }
        }
        catch (Exception ignored) {}

        // 退款申请（售后）状态 + 是否可申请（已完成 + 已付 + 3天内 + 无进行中申请）
        try
        {
            MallRefundRequest rr = orderService.getLatestRefundRequest(id);
            boolean activeReq = false;
            if (rr != null)
            {
                order.setRefundStatus(rr.getStatus());
                order.setRefundRemark(rr.getAdminRemark());
                activeReq = "PENDING".equals(rr.getStatus()) || "APPROVED".equals(rr.getStatus());
            }
            boolean within = order.getUpdateTime() != null
                    && System.currentTimeMillis() - order.getUpdateTime().getTime() <= 3L * 24 * 60 * 60 * 1000;
            order.setCanRefund("3".equals(order.getStatus())
                    && "PAID".equalsIgnoreCase(order.getPaymentStatus())
                    && within && !activeReq);
        }
        catch (Exception ignored) {}

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
            return AjaxResult.error("评分不能为空");
        }
        int score = Integer.valueOf(scoreObj.toString());
        if (score < 1 || score > 5)
        {
            return AjaxResult.error("评分必须在 1 到 5 之间");
        }
        MallRunnerRating rating = new MallRunnerRating();
        rating.setOrderId(id);
        rating.setRaterMemberId(memberId);
        rating.setScore(score);
        rating.setComment(body.get("comment") != null ? body.get("comment").toString() : null);
        try
        {
            runnerService.rateRunner(rating);
            return AjaxResult.success("评价提交成功");
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
            return AjaxResult.success("订单已取消");
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 完成后退款申请（售后）
     * Body: { "reason": "...", "images": ["url1","url2"] }
     */
    @PostMapping("/{id}/refund-request")
    public AjaxResult refundRequest(@PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        String reason = (body != null && body.get("reason") != null) ? body.get("reason").toString() : "";
        String images = null;
        if (body != null && body.get("images") instanceof List)
        {
            StringBuilder sb = new StringBuilder();
            for (Object o : (List<?>) body.get("images"))
            {
                if (o != null && !o.toString().isEmpty())
                {
                    if (sb.length() > 0) sb.append(',');
                    sb.append(o.toString());
                }
            }
            images = sb.length() > 0 ? sb.toString() : null;
        }
        else if (body != null && body.get("images") != null)
        {
            String s = body.get("images").toString();
            images = s.isEmpty() ? null : s;
        }
        try
        {
            MallRefundRequest req = orderService.createRefundRequest(id, memberId, reason, images);
            try
            {
                MallOrder order = orderService.selectOrderById(id);
                telegramNotifyService.notify("💸 New Refund Request\n"
                        + "Order: " + (order != null ? order.getOrderNo() : id) + "\n"
                        + "Reason: " + reason);
            }
            catch (Exception ignored) {}
            return AjaxResult.success("退款申请已提交").put("status", req.getStatus());
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }
}
