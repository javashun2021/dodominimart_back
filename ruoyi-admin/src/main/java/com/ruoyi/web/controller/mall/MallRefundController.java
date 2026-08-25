package com.ruoyi.web.controller.mall;

import java.util.List;
import java.util.Map;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.page.TableDataInfo;
import com.ruoyi.framework.util.ShiroUtils;
import com.ruoyi.framework.web.base.BaseController;
import com.ruoyi.mall.domain.MallRefundRequest;
import com.ruoyi.mall.service.IMallOrderService;

/**
 * 退款申请（售后）审核后台
 * /mall/refund —— 列表 + 通过(真退)/驳回
 */
@Controller
@RequestMapping("/mall/refund")
public class MallRefundController extends BaseController
{
    private static final String prefix = "mall/refund";

    @Autowired
    private IMallOrderService orderService;

    @RequiresPermissions("mall:refund:view")
    @GetMapping
    public String index()
    {
        return prefix + "/refund";
    }

    @RequiresPermissions("mall:refund:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MallRefundRequest query)
    {
        startPage();
        List<MallRefundRequest> list = orderService.listRefundRequests(query);
        return getDataTable(list);
    }

    /** 通过 → 触发真退款（线上走 PayMongo / 现金标记 + 回库存 + 退积分） */
    @RequiresPermissions("mall:refund:handle")
    @Log(title = "退款申请审核", businessType = BusinessType.UPDATE)
    @PostMapping("/approve/{requestId}")
    @ResponseBody
    public AjaxResult approve(@PathVariable Long requestId)
    {
        try
        {
            orderService.approveRefundRequest(requestId, ShiroUtils.getLoginName());
            return AjaxResult.success("退款已通过并处理");
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /** 驳回（备注给顾客看） */
    @RequiresPermissions("mall:refund:handle")
    @Log(title = "退款申请驳回", businessType = BusinessType.UPDATE)
    @PostMapping("/reject/{requestId}")
    @ResponseBody
    public AjaxResult reject(@PathVariable Long requestId, @RequestBody(required = false) Map<String, String> body)
    {
        String remark = body != null ? body.getOrDefault("remark", "") : "";
        try
        {
            orderService.rejectRefundRequest(requestId, ShiroUtils.getLoginName(), remark);
            return AjaxResult.success("退款申请已驳回");
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }
}
