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
import com.ruoyi.mall.domain.MallStore;
import com.ruoyi.mall.service.IMallStoreService;

/**
 * 门店/发货网点 管理
 */
@Controller
@RequestMapping("/mall/store")
public class MallStoreController extends BaseController
{
    private final String prefix = "mall/store";

    @Autowired
    private IMallStoreService storeService;

    @RequiresPermissions("mall:store:view")
    @GetMapping
    public String store()
    {
        return prefix + "/store";
    }

    @RequiresPermissions("mall:store:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MallStore store)
    {
        startPage();
        List<MallStore> list = storeService.selectStoreList(store);
        return getDataTable(list);
    }

    @RequiresPermissions("mall:store:add")
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    @RequiresPermissions("mall:store:add")
    @Log(title = "门店", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(MallStore store)
    {
        store.setCreateBy(ShiroUtils.getLoginName());
        return toAjax(storeService.insertStore(store));
    }

    @RequiresPermissions("mall:store:edit")
    @GetMapping("/edit/{storeId}")
    public String edit(@PathVariable Long storeId, ModelMap mmap)
    {
        mmap.put("store", storeService.selectStoreById(storeId));
        return prefix + "/edit";
    }

    @RequiresPermissions("mall:store:edit")
    @Log(title = "门店", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MallStore store)
    {
        store.setUpdateBy(ShiroUtils.getLoginName());
        return toAjax(storeService.updateStore(store));
    }

    @RequiresPermissions("mall:store:remove")
    @Log(title = "门店", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(Long storeId)
    {
        return toAjax(storeService.deleteStoreById(storeId));
    }
}
