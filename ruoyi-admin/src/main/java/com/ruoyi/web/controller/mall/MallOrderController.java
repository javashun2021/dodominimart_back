package com.ruoyi.web.controller.mall;

import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.page.TableDataInfo;
import com.ruoyi.framework.util.ShiroUtils;
import com.ruoyi.framework.web.base.BaseController;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.ruoyi.mall.domain.MallMemberCoupon;
import com.ruoyi.mall.domain.MallOrder;
import com.ruoyi.mall.service.IMallCouponService;
import com.ruoyi.mall.service.IMallOrderService;
import com.ruoyi.system.service.ISysConfigService;

@Controller
@RequestMapping("/mall/order")
public class MallOrderController extends BaseController
{
    private final String prefix = "mall/order";

    @Autowired
    private IMallOrderService orderService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private IMallCouponService couponService;

    @Autowired
    private com.ruoyi.web.service.ReceiptTokenService receiptTokenService;

    @RequiresPermissions("mall:order:view")
    @GetMapping
    public String order()
    {
        return prefix + "/order";
    }

    @RequiresPermissions("mall:order:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MallOrder order)
    {
        startPage();
        List<MallOrder> list = orderService.selectOrderList(order);
        return getDataTable(list);
    }

    /** 订单详情页（含明细） */
    @RequiresPermissions("mall:order:view")
    @GetMapping("/detail/{orderId}")
    public String detail(@PathVariable Long orderId, ModelMap mmap)
    {
        MallOrder order = orderService.selectOrderById(orderId);
        mmap.put("order", order);
        // 解析地址快照(JSON)为可读字段：收件人(label)/电话(phone)/地址(fullAddress)，
        // 解析失败或老订单缺字段时，模板回退展示原始快照串。
        if (order != null && order.getAddressSnapshot() != null && !order.getAddressSnapshot().isEmpty())
        {
            try
            {
                @SuppressWarnings("unchecked")
                Map<String, Object> snap = com.ruoyi.common.json.JSON.unmarshal(order.getAddressSnapshot(), Map.class);
                if (snap != null)
                {
                    mmap.put("addrLabel", snap.get("label"));
                    mmap.put("addrPhone", snap.get("phone"));
                    mmap.put("addrFull",  snap.get("fullAddress"));
                }
            }
            catch (Exception ignored) {}
        }
        // 用券订单：查出券名与类型(free_delivery/amount_off/percent_off)，让详情能显示「免配送券」等
        if (order != null && order.getMemberCouponId() != null)
        {
            try
            {
                for (MallMemberCoupon mc : couponService.getMyCoupons(order.getMemberId()))
                {
                    if (order.getMemberCouponId().equals(mc.getId()))
                    {
                        mmap.put("couponName", mc.getCouponName());
                        mmap.put("couponType", mc.getType());
                        break;
                    }
                }
            }
            catch (Exception ignored) {}
        }
        return prefix + "/detail";
    }

    /** 订单打印小票页（独立可打印页面，店员在新标签点 Print 走浏览器打印对话框） */
    @RequiresPermissions("mall:order:view")
    @GetMapping("/receipt/{orderId}")
    public String receipt(@PathVariable Long orderId, ModelMap mmap)
    {
        mmap.put("order", orderService.selectOrderById(orderId));
        mmap.put("shopName",    cfg("app.store.name",      "DODOMINIMART"));
        mmap.put("shopPhone",   cfg("app.contact.phone",   ""));
        mmap.put("shopAddress", cfg("app.receipt.address", ""));
        mmap.put("shopFooter",  cfg("app.receipt.footer",  "Thank you for shopping!"));

        // 订单二维码：一个 URL 三端通吃 —— Android/iOS 装了 App 直接拉起订单详情（App Link/Universal Link），
        // 否则浏览器打开 Token 公开订单页（/o/{id}?t=...）。Token 防止按 orderId 遍历他人订单。
        String base = cfg("app.deeplink.base", "https://dodominimart.com");
        String orderUrl = base + "/o/" + orderId + "?t=" + receiptTokenService.sign(orderId);
        mmap.put("qrDataUri", qrDataUri(orderUrl, 220));
        return prefix + "/receipt";
    }

    /** 生成二维码并返回 base64 data URI（内嵌到打印页，离线可打印，仅依赖 zxing core） */
    private String qrDataUri(String text, int size)
    {
        try
        {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints);
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < size; x++)
            {
                for (int y = 0; y < size; y++)
                {
                    img.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", bos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bos.toByteArray());
        }
        catch (Exception e)
        {
            return "";
        }
    }

    /** 读取系统参数，取不到时返回默认值 */
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

    /** 变更订单状态（确认/配送/完成/取消） */
    @RequiresPermissions("mall:order:edit")
    @Log(title = "订单管理", businessType = BusinessType.UPDATE)
    @PostMapping("/changeStatus")
    @ResponseBody
    public AjaxResult changeStatus(Long orderId, String status)
    {
        return toAjax(orderService.updateOrderStatus(orderId, status, ShiroUtils.getLoginName()));
    }

    /** 到店单确认收款 → 直接完成并发放完成奖励 */
    @RequiresPermissions("mall:order:edit")
    @Log(title = "订单管理", businessType = BusinessType.UPDATE)
    @PostMapping("/confirmStorePayment")
    @ResponseBody
    public AjaxResult confirmStorePayment(Long orderId)
    {
        try
        {
            return toAjax(orderService.confirmInStorePayment(orderId, ShiroUtils.getLoginName()));
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }
}
