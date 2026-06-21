package com.ruoyi.web.controller.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.mall.domain.MallMember;
import com.ruoyi.mall.domain.MallOrder;
import com.ruoyi.mall.domain.MallOrderItem;
import com.ruoyi.mall.domain.MallPosHeldCart;
import com.ruoyi.mall.domain.MallProduct;
import com.ruoyi.mall.mapper.MallMemberMapper;
import com.ruoyi.mall.service.IMallOrderService;
import com.ruoyi.mall.service.IMallPosHeldCartService;
import com.ruoyi.mall.service.IMallProductService;
import com.ruoyi.mall.service.PosPrintService;
import com.ruoyi.system.service.ISysConfigService;

/**
 * POS 收银台接口（需 JWT + role=cashier）
 * 所有接口先过 requireCashier(getCurrentMemberId(request)) 网关。
 *
 * GET    /api/v1/cashier/member?phone=
 * POST   /api/v1/cashier/orders                收银开单+收款
 * POST   /api/v1/cashier/orders/{orderId}/print 补打小票
 * GET    /api/v1/cashier/held-carts            暂存列表
 * POST   /api/v1/cashier/held-carts            暂存当前单
 * GET    /api/v1/cashier/held-carts/{id}       取出某暂存(按当前价/库存重算)
 * DELETE /api/v1/cashier/held-carts/{id}       作废暂存
 * 商品检索复用公开接口 /api/v1/products，本控制器不再重复。
 */
@RestController
@RequestMapping("/api/v1/cashier")
public class ApiCashierController extends BaseApiController
{
    @Autowired
    private MallMemberMapper memberMapper;

    @Autowired
    private IMallOrderService orderService;

    @Autowired
    private IMallProductService productService;

    @Autowired
    private IMallPosHeldCartService heldCartService;

    @Autowired
    private PosPrintService printService;

    @Autowired
    private ISysConfigService configService;

    // ── 网关：仅收银员/管理员可用 ────────────────────────────────────

    private MallMember requireCashier(HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        MallMember member = memberMapper.selectMemberById(memberId);
        if (member == null || (!"cashier".equals(member.getRole()) && !"admin".equals(member.getRole())))
        {
            throw new RuntimeException("Cashier access required");
        }
        return member;
    }

    // ── 会员手机号查询（可选关联会员） ───────────────────────────────

    @GetMapping("/member")
    public AjaxResult findMember(@RequestParam String phone, HttpServletRequest request)
    {
        requireCashier(request);
        MallMember m = memberMapper.selectMemberByPhone(phone);
        if (m == null)
        {
            return AjaxResult.success("not found").put("data", null);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("memberId", m.getMemberId());
        data.put("nickName", m.getNickName());
        data.put("phone",    m.getPhone());
        data.put("points",   m.getPoints());
        return AjaxResult.success("ok").put("data", data);
    }

    // ── 收银开单 + 收款（核心） ──────────────────────────────────────

    @PostMapping("/orders")
    public AjaxResult createOrder(@RequestBody Map<String, Object> body, HttpServletRequest request)
    {
        MallMember cashier = requireCashier(request);

        List<MallOrderItem> items;
        try { items = parseItems(body.get("items")); }
        catch (RuntimeException e) { return AjaxResult.error(e.getMessage()); }
        if (items.isEmpty()) return AjaxResult.error("items cannot be empty");

        Long ownerId = parseLong(body.get("customerMemberId"));
        if (ownerId == null) ownerId = walkinMemberId();
        if (ownerId == null || ownerId <= 0)
        {
            return AjaxResult.error("Walk-in member not configured (app.pos.walkin.member.id)");
        }

        String tenderType = body.get("tenderType") != null ? body.get("tenderType").toString().toUpperCase() : "CASH";
        BigDecimal cashReceived = parseDecimal(body.get("cashReceived"));
        int pointsToUse  = (int) parseLongOr(body.get("pointsToUse"), 0L);
        Long memberCouponId = parseLong(body.get("memberCouponId"));
        Long heldCartId  = parseLong(body.get("heldCartId"));

        MallOrder order;
        try
        {
            order = orderService.createPosOrder(ownerId, items, cashier.getMemberId(),
                    tenderType, cashReceived, pointsToUse, memberCouponId);
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }

        // 暂存恢复而来 → 标记该暂存为已结算
        if (heldCartId != null)
        {
            try { heldCartService.markResolved(heldCartId); } catch (Exception ignored) {}
        }

        // 自动打印一次（失败不影响开单，前端可用补打接口重试）
        boolean printed = true;
        String printError = null;
        try { printService.printReceipt(order, buildReceiptConfig()); }
        catch (Exception e) { printed = false; printError = e.getMessage(); }

        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("changeDue", order.getChangeDue());
        data.put("printed", printed);
        if (printError != null) data.put("printError", printError);
        return AjaxResult.success("Order completed").put("data", data);
    }

    @PostMapping("/orders/{orderId}/print")
    public AjaxResult reprint(@PathVariable Long orderId, HttpServletRequest request)
    {
        requireCashier(request);
        MallOrder order = orderService.selectOrderById(orderId);
        if (order == null) return AjaxResult.error("Order not found");
        try
        {
            printService.printReceipt(order, buildReceiptConfig());
            return AjaxResult.success("Printed");
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    // ── 暂存 / 恢复（挂单） ──────────────────────────────────────────

    @GetMapping("/held-carts")
    public AjaxResult listHeldCarts(HttpServletRequest request)
    {
        requireCashier(request);
        List<MallPosHeldCart> list = heldCartService.listOpen();
        List<Map<String, Object>> out = new ArrayList<>();
        for (MallPosHeldCart c : list)
        {
            Map<String, Object> row = new HashMap<>();
            row.put("id",          c.getId());
            row.put("label",       c.getLabel());
            row.put("memberId",    c.getMemberId());
            row.put("itemCount",   c.getItemCount());
            row.put("totalAmount", c.getTotalAmount());
            row.put("createTime",  c.getCreateTime());
            out.add(row);
        }
        return AjaxResult.success("ok").put("data", out);
    }

    @PostMapping("/held-carts")
    public AjaxResult holdCart(@RequestBody Map<String, Object> body, HttpServletRequest request)
    {
        MallMember cashier = requireCashier(request);

        List<MallOrderItem> items;
        try { items = parseItems(body.get("items")); }
        catch (RuntimeException e) { return AjaxResult.error(e.getMessage()); }
        if (items.isEmpty()) return AjaxResult.error("items cannot be empty");

        // 规整 + 价格快照（仅展示用）
        List<Map<String, Object>> normalized = new ArrayList<>();
        int itemCount = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (MallOrderItem it : items)
        {
            Map<String, Object> entry = new HashMap<>();
            entry.put("productId", it.getProductId());
            entry.put("quantity",  it.getQuantity());
            normalized.add(entry);
            itemCount += it.getQuantity();
            MallProduct p = productService.selectProductById(it.getProductId());
            if (p != null && p.getPrice() != null)
            {
                total = total.add(p.getPrice().multiply(BigDecimal.valueOf(it.getQuantity())));
            }
        }

        MallPosHeldCart cart = new MallPosHeldCart();
        cart.setCashierId(cashier.getMemberId());
        cart.setMemberId(parseLong(body.get("customerMemberId")));
        cart.setLabel(body.get("label") != null ? body.get("label").toString() : null);
        cart.setRemark(body.get("remark") != null ? body.get("remark").toString() : null);
        cart.setItemCount(itemCount);
        cart.setTotalAmount(total);
        try { cart.setCartJson(com.ruoyi.common.json.JSON.marshal(normalized)); }
        catch (Exception e) { return AjaxResult.error("Failed to serialize cart"); }

        heldCartService.hold(cart);
        Map<String, Object> data = new HashMap<>();
        data.put("id",    cart.getId());
        data.put("label", cart.getLabel());
        return AjaxResult.success("Held").put("data", data);
    }

    @GetMapping("/held-carts/{id}")
    public AjaxResult getHeldCart(@PathVariable Long id, HttpServletRequest request)
    {
        requireCashier(request);
        MallPosHeldCart cart = heldCartService.get(id);
        if (cart == null || "2".equals(cart.getStatus()))
        {
            return AjaxResult.error("Held cart not found");
        }

        // 按当前商品价/库存重算，标注失效/缺货项
        List<Map<String, Object>> lines = new ArrayList<>();
        try
        {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> raw = com.ruoyi.common.json.JSON.unmarshal(cart.getCartJson(), List.class);
            if (raw != null)
            {
                for (Map<String, Object> r : raw)
                {
                    Long productId = parseLong(r.get("productId"));
                    int qty = (int) parseLongOr(r.get("quantity"), 0L);
                    MallProduct p = productId != null ? productService.selectProductById(productId) : null;
                    Map<String, Object> line = new HashMap<>();
                    line.put("productId", productId);
                    line.put("quantity",  qty);
                    if (p == null || !"0".equals(p.getStatus()))
                    {
                        line.put("available", false);
                        line.put("name", p != null ? p.getName() : null);
                    }
                    else
                    {
                        line.put("available", p.getStock() >= qty);
                        line.put("name",      p.getName());
                        line.put("price",     p.getPrice());
                        line.put("imageUrl",  p.getImageUrl());
                        line.put("stock",     p.getStock());
                    }
                    lines.add(line);
                }
            }
        }
        catch (Exception e)
        {
            return AjaxResult.error("Failed to read held cart");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id",       cart.getId());
        data.put("label",    cart.getLabel());
        data.put("memberId", cart.getMemberId());
        data.put("remark",   cart.getRemark());
        data.put("items",    lines);
        return AjaxResult.success("ok").put("data", data);
    }

    @DeleteMapping("/held-carts/{id}")
    public AjaxResult voidHeldCart(@PathVariable Long id, HttpServletRequest request)
    {
        requireCashier(request);
        heldCartService.voidCart(id);
        return AjaxResult.success("Voided");
    }

    // ── helpers ──────────────────────────────────────────────────────

    private List<MallOrderItem> parseItems(Object raw)
    {
        List<MallOrderItem> items = new ArrayList<>();
        if (!(raw instanceof List)) return items;
        for (Object o : (List<?>) raw)
        {
            if (!(o instanceof Map)) continue;
            Map<?, ?> m = (Map<?, ?>) o;
            Long productId = parseLong(m.get("productId"));
            int qty = (int) parseLongOr(m.get("quantity"), 0L);
            if (productId == null) throw new RuntimeException("Each item must have productId");
            if (qty <= 0) throw new RuntimeException("quantity must be greater than 0");
            MallOrderItem item = new MallOrderItem();
            item.setProductId(productId);
            item.setQuantity(qty);
            items.add(item);
        }
        return items;
    }

    /** 从 sys_config 组装打印参数传给 mall 层的 PosPrintService */
    private PosPrintService.ReceiptConfig buildReceiptConfig()
    {
        PosPrintService.ReceiptConfig rc = new PosPrintService.ReceiptConfig();
        rc.enabled   = "true".equalsIgnoreCase(cfg("app.pos.printer.enabled", "false"));
        rc.ip        = cfg("app.pos.printer.ip", "");
        rc.port      = (int) parseLongOr(cfg("app.pos.printer.port", "9100"), 9100L);
        rc.storeName = cfg("app.store.name", "DODOMINIMART");
        rc.address   = cfg("app.receipt.address", "");
        rc.phone     = cfg("app.contact.phone", "");
        rc.footer    = cfg("app.receipt.footer", "Thank you for shopping!");
        return rc;
    }

    private String cfg(String key, String def)
    {
        try
        {
            String v = configService.selectConfigByKey(key);
            return (v != null && !v.isEmpty()) ? v : def;
        }
        catch (Exception e) { return def; }
    }

    private Long walkinMemberId()
    {
        try { return Long.valueOf(configService.selectConfigByKey("app.pos.walkin.member.id")); }
        catch (Exception e) { return null; }
    }

    private Long parseLong(Object o)
    {
        if (o == null) return null;
        try { return Long.valueOf(o.toString().trim()); } catch (NumberFormatException e) { return null; }
    }

    private long parseLongOr(Object o, long def)
    {
        Long v = parseLong(o);
        return v != null ? v : def;
    }

    private BigDecimal parseDecimal(Object o)
    {
        if (o == null) return null;
        try { return new BigDecimal(o.toString().trim()); } catch (NumberFormatException e) { return null; }
    }
}
