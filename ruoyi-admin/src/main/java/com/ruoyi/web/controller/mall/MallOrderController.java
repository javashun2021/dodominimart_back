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

@Controller
@RequestMapping("/mall/order")
public class MallOrderController extends BaseController
{
    private final String prefix = "mall/order";

    @Autowired
    private IMallOrderService orderService;

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

    /** 变更订单状态（确认/配送/完成/取消） */
    @RequiresPermissions("mall:order:edit")
    @Log(title = "订单管理", businessType = BusinessType.UPDATE)
    @PostMapping("/changeStatus")
    @ResponseBody
    public AjaxResult changeStatus(Long orderId, String status)
    {
        return toAjax(orderService.updateOrderStatus(orderId, status, ShiroUtils.getLoginName()));
    }
}
