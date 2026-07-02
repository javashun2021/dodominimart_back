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
import com.ruoyi.mall.domain.MallRunnerApplication;
import com.ruoyi.mall.service.IMallRunnerService;
import com.ruoyi.mall.service.IMallStoreService;

/**
 * 跑腿管理后台
 * 申请审核 + GCash 跑腿费结算
 */
@Controller
@RequestMapping("/mall/runner")
public class MallRunnerController extends BaseController
{
    private static final String prefix = "mall/runner";

    @Autowired
    private IMallRunnerService runnerService;

    @Autowired
    private IMallStoreService storeService;

    @RequiresPermissions("mall:runner:view")
    @GetMapping
    public String index()
    {
        return prefix + "/application";
    }

    /** 申请列表 */
    @RequiresPermissions("mall:runner:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MallRunnerApplication query)
    {
        startPage();
        List<MallRunnerApplication> list = runnerService.listApplications(query);
        return getDataTable(list);
    }

    /** 审核通过 */
    @RequiresPermissions("mall:runner:review")
    @Log(title = "跑腿申请审核", businessType = BusinessType.UPDATE)
    @PostMapping("/approve/{appId}")
    @ResponseBody
    public AjaxResult approve(@PathVariable Long appId,
            @RequestBody(required = false) Map<String, Object> body)
    {
        Long storeId = null;
        if (body != null && body.get("storeId") != null)
        {
            try { storeId = Long.parseLong(body.get("storeId").toString()); } catch (NumberFormatException ignored) {}
        }
        runnerService.approveApplication(appId, ShiroUtils.getLoginName(), storeId);
        return AjaxResult.success();
    }

    /** 门店下拉（审核时指派归属门店用），返回营业中的门店 */
    @RequiresPermissions("mall:runner:review")
    @GetMapping("/stores")
    @ResponseBody
    public AjaxResult stores()
    {
        return AjaxResult.success().put("data", storeService.listActiveStores());
    }

    /** 审核拒绝 */
    @RequiresPermissions("mall:runner:review")
    @Log(title = "跑腿申请拒绝", businessType = BusinessType.UPDATE)
    @PostMapping("/reject/{appId}")
    @ResponseBody
    public AjaxResult reject(@PathVariable Long appId, @RequestBody Map<String, String> body)
    {
        String reason = body.getOrDefault("rejectReason", "");
        runnerService.rejectApplication(appId, ShiroUtils.getLoginName(), reason);
        return AjaxResult.success();
    }

    /** 结算管理页面 */
    @RequiresPermissions("mall:runner:settle")
    @GetMapping("/settle")
    public String settle()
    {
        return prefix + "/settle";
    }

    /** 已通过审核的跑腿员列表（结算页「姓名」下拉用：返回 memberId/realName/nickName/phone） */
    @RequiresPermissions("mall:runner:settle")
    @GetMapping("/approvedRunners")
    @ResponseBody
    public AjaxResult approvedRunners()
    {
        MallRunnerApplication query = new MallRunnerApplication();
        query.setStatus("1");
        List<MallRunnerApplication> list = runnerService.listApplications(query);
        return AjaxResult.success().put("data", list);
    }

    /** 未结算 GCash 跑腿订单（按 runner 分组） */
    @RequiresPermissions("mall:runner:settle")
    @GetMapping("/unsettled")
    @ResponseBody
    public AjaxResult unsettled(@org.springframework.web.bind.annotation.RequestParam(required = false) Long runnerMemberId)
    {
        List<Map<String, Object>> list = runnerService.getUnsettledOrders(runnerMemberId);
        return AjaxResult.success().put("data", list);
    }

    /** 批量标记已结算 */
    @RequiresPermissions("mall:runner:settle")
    @Log(title = "跑腿费结算", businessType = BusinessType.UPDATE)
    @PostMapping("/settle")
    @ResponseBody
    public AjaxResult settle(@RequestBody Map<String, Object> body)
    {
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("orderIds");
        List<Long> orderIds = new java.util.ArrayList<>();
        for (Integer id : ids) orderIds.add(id.longValue());
        runnerService.settleRunnerFee(orderIds);
        return AjaxResult.success();
    }
}
