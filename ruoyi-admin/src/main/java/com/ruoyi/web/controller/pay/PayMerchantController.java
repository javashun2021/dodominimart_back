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
import com.ruoyi.mall.domain.ImspayMerchant;
import com.ruoyi.mall.service.IImspayMerchantService;

/**
 * 聚合支付-商户管理。
 */
@Controller
@RequestMapping("/pay/merchant")
public class PayMerchantController extends BaseController
{
    private final String prefix = "pay/merchant";

    @Autowired
    private IImspayMerchantService merchantService;

    @RequiresPermissions("pay:merchant:view")
    @GetMapping
    public String merchant()
    {
        return prefix + "/merchant";
    }

    @RequiresPermissions("pay:merchant:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(ImspayMerchant merchant)
    {
        startPage();
        return getDataTable(merchantService.selectMerchantList(merchant));
    }

    @RequiresPermissions("pay:merchant:add")
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    @RequiresPermissions("pay:merchant:add")
    @Log(title = "支付商户", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(ImspayMerchant merchant)
    {
        return toAjax(merchantService.insertMerchant(merchant));
    }

    @RequiresPermissions("pay:merchant:edit")
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable String id, ModelMap mmap)
    {
        mmap.put("merchant", merchantService.selectById(id));
        return prefix + "/edit";
    }

    @RequiresPermissions("pay:merchant:edit")
    @Log(title = "支付商户", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(ImspayMerchant merchant)
    {
        return toAjax(merchantService.updateMerchant(merchant));
    }

    @RequiresPermissions("pay:merchant:edit")
    @Log(title = "支付商户", businessType = BusinessType.UPDATE)
    @PostMapping("/resetKey/{id}")
    @ResponseBody
    public AjaxResult resetKey(@PathVariable String id)
    {
        String key = merchantService.resetKey(id);
        return AjaxResult.success("已重置密钥").put("appSecret", key);
    }

    @RequiresPermissions("pay:merchant:remove")
    @Log(title = "支付商户", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(merchantService.deleteByIds(ids));
    }
}
