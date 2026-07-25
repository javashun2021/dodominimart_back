package com.ruoyi.web.controller.mall;

import java.util.List;
import java.util.Map;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
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
import com.ruoyi.mall.domain.MallMerchant;
import com.ruoyi.mall.service.IMallMerchantService;

/**
 * 商家/门店（入驻）管理后台
 * 列表 / 新增 / 编辑 / 删除 + 入驻审核(通过/拒绝)
 */
@Controller
@RequestMapping("/mall/merchant")
public class MallMerchantController extends BaseController
{
    private final String prefix = "mall/merchant";

    @Autowired
    private IMallMerchantService merchantService;

    @RequiresPermissions("mall:merchant:view")
    @GetMapping
    public String merchant()
    {
        return prefix + "/merchant";
    }

    @RequiresPermissions("mall:merchant:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MallMerchant merchant)
    {
        startPage();
        List<MallMerchant> list = merchantService.selectMerchantList(merchant);
        return getDataTable(list);
    }

    @RequiresPermissions("mall:merchant:add")
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    @RequiresPermissions("mall:merchant:add")
    @Log(title = "商家", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(MallMerchant merchant)
    {
        merchant.setCreateBy(ShiroUtils.getLoginName());
        return toAjax(merchantService.insertMerchant(merchant));
    }

    @RequiresPermissions("mall:merchant:edit")
    @GetMapping("/edit/{merchantId}")
    public String edit(@PathVariable Long merchantId, ModelMap mmap)
    {
        mmap.put("merchant", merchantService.selectMerchantById(merchantId));
        return prefix + "/edit";
    }

    @RequiresPermissions("mall:merchant:edit")
    @Log(title = "商家", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MallMerchant merchant)
    {
        merchant.setUpdateBy(ShiroUtils.getLoginName());
        return toAjax(merchantService.updateMerchant(merchant));
    }

    @RequiresPermissions("mall:merchant:remove")
    @Log(title = "商家", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(Long merchantId)
    {
        return toAjax(merchantService.deleteMerchantById(merchantId));
    }

    /** 详情（查看代录门店照片/坐标等） */
    @RequiresPermissions("mall:merchant:view")
    @GetMapping("/detail/{merchantId}")
    public String detail(@PathVariable Long merchantId, ModelMap mmap)
    {
        mmap.put("merchant", merchantService.selectMerchantById(merchantId));
        return prefix + "/detail";
    }

    /** 审核通过 → 营业 */
    @RequiresPermissions("mall:merchant:review")
    @Log(title = "商家入驻审核", businessType = BusinessType.UPDATE)
    @PostMapping("/approve/{merchantId}")
    @ResponseBody
    public AjaxResult approve(@PathVariable Long merchantId)
    {
        merchantService.approveMerchant(merchantId, ShiroUtils.getLoginName());
        return AjaxResult.success();
    }

    /** 审核拒绝 */
    @RequiresPermissions("mall:merchant:review")
    @Log(title = "商家入驻拒绝", businessType = BusinessType.UPDATE)
    @PostMapping("/reject/{merchantId}")
    @ResponseBody
    public AjaxResult reject(@PathVariable Long merchantId, @RequestBody Map<String, String> body)
    {
        String reason = body != null ? body.getOrDefault("rejectReason", "") : "";
        merchantService.rejectMerchant(merchantId, ShiroUtils.getLoginName(), reason);
        return AjaxResult.success();
    }
}
