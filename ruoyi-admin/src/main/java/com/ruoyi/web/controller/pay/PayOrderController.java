package com.ruoyi.web.controller.pay;

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
import com.ruoyi.framework.web.base.BaseController;
import com.ruoyi.mall.domain.PayOrder;
import com.ruoyi.mall.service.IPayOpenService;
import com.ruoyi.mall.service.IPayOrderService;

/**
 * 聚合支付-订单管理（查询、手动补发回调）。
 */
@Controller
@RequestMapping("/pay/order")
public class PayOrderController extends BaseController
{
    private final String prefix = "pay/order";

    @Autowired
    private IPayOrderService payOrderService;

    @Autowired
    private IPayOpenService payOpenService;

    @RequiresPermissions("pay:order:view")
    @GetMapping
    public String order()
    {
        return prefix + "/order";
    }

    @RequiresPermissions("pay:order:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(PayOrder query)
    {
        startPage();
        return getDataTable(payOrderService.selectOrderList(query));
    }

    @RequiresPermissions("pay:order:view")
    @GetMapping("/detail/{platformNo}")
    public String detail(@PathVariable String platformNo, ModelMap mmap)
    {
        mmap.put("order", payOrderService.selectByPlatformNo(platformNo));
        return prefix + "/detail";
    }

    /** 手动补发一次商户回调 */
    @RequiresPermissions("pay:order:notify")
    @Log(title = "支付订单-补发回调", businessType = BusinessType.OTHER)
    @PostMapping("/renotify/{platformNo}")
    @ResponseBody
    public AjaxResult renotify(@PathVariable String platformNo)
    {
        boolean ok = payOpenService.pushNotify(platformNo);
        return ok ? AjaxResult.success("回调成功(商户返回 success)")
                  : AjaxResult.error("回调未成功，请检查商户 notify_url 或稍后重试");
    }
}
