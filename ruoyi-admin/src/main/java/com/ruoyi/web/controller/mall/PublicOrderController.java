package com.ruoyi.web.controller.mall;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import com.ruoyi.mall.domain.MallOrder;
import com.ruoyi.mall.service.IMallOrderService;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.web.service.ReceiptTokenService;

/**
 * 订单二维码的公开落地页（无需登录，Token 鉴权）。
 *
 * <p>二维码编码 {@code https://域名/o/{orderId}?t={token}}：
 * <ul>
 *   <li>Android/iOS 装了 App 且校验通过 → 系统直接用 App 打开订单详情（不会走到这里）。</li>
 *   <li>浏览器/桌面扫码器/未装 App → 命中这里，渲染只读订单详情页，并提供「用 App 打开」按钮。</li>
 * </ul>
 * Shiro 已对 {@code /o/**} 放行（anon）。</p>
 */
@Controller
public class PublicOrderController
{
    @Autowired
    private IMallOrderService orderService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private ReceiptTokenService receiptTokenService;

    @GetMapping("/o/{orderId}")
    public String publicOrder(@PathVariable Long orderId,
                              @RequestParam(value = "t", required = false) String token,
                              ModelMap mmap)
    {
        if (!receiptTokenService.verify(orderId, token))
        {
            return "mall/order/public_invalid";
        }
        MallOrder order = orderService.selectOrderById(orderId);
        if (order == null)
        {
            return "mall/order/public_invalid";
        }
        mmap.put("order", order);
        mmap.put("shopName",    cfg("app.store.name",      "DODOMINIMART"));
        mmap.put("shopPhone",   cfg("app.contact.phone",   ""));
        mmap.put("shopAddress", cfg("app.receipt.address", ""));
        mmap.put("shopFooter",  cfg("app.receipt.footer",  "Thank you for shopping!"));
        // 浏览器里若已装 App，可点此用自定义 scheme 拉起原生订单详情
        mmap.put("appScheme", "dodominimart://orders/" + orderId);
        return "mall/order/public";
    }

    private String cfg(String key, String defaultValue)
    {
        try
        {
            String v = configService.selectConfigByKey(key);
            return (v != null && !v.isEmpty()) ? v : defaultValue;
        }
        catch (Exception e)
        {
            return defaultValue;
        }
    }
}
