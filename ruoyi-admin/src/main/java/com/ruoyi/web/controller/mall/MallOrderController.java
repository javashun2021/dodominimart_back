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
import com.ruoyi.mall.domain.MallOrder;
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
        mmap.put("order", orderService.selectOrderById(orderId));
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
        return prefix + "/receipt";
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
