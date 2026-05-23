package com.ruoyi.web.controller.mall;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
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
import com.ruoyi.mall.domain.MallFlashSale;
import com.ruoyi.mall.service.IMallFlashSaleService;

@Controller
@RequestMapping("/mall/flashsale")
public class MallFlashSaleController extends BaseController
{
    private final String prefix = "mall/flashsale";

    @Autowired
    private IMallFlashSaleService flashSaleService;

    @InitBinder
    public void initBinder(WebDataBinder binder)
    {
        // datetime-local 格式：2026-05-23T19:39
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        sdf.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(sdf, true));
    }

    @RequiresPermissions("mall:flashsale:view")
    @GetMapping
    public String flashsale()
    {
        return prefix + "/flashsale";
    }

    @RequiresPermissions("mall:flashsale:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MallFlashSale flashSale)
    {
        startPage();
        List<MallFlashSale> list = flashSaleService.selectFlashSaleList(flashSale);
        return getDataTable(list);
    }

    @RequiresPermissions("mall:flashsale:add")
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    @RequiresPermissions("mall:flashsale:add")
    @Log(title = "限时优惠", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(MallFlashSale flashSale)
    {
        return toAjax(flashSaleService.insertFlashSale(flashSale));
    }

    @RequiresPermissions("mall:flashsale:edit")
    @GetMapping("/edit/{saleId}")
    public String edit(@PathVariable Long saleId, ModelMap mmap)
    {
        mmap.put("flashSale", flashSaleService.selectFlashSaleById(saleId));
        return prefix + "/edit";
    }

    @RequiresPermissions("mall:flashsale:edit")
    @Log(title = "限时优惠", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MallFlashSale flashSale)
    {
        return toAjax(flashSaleService.updateFlashSale(flashSale));
    }

    @RequiresPermissions("mall:flashsale:remove")
    @Log(title = "限时优惠", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(Long saleId)
    {
        return toAjax(flashSaleService.deleteFlashSaleById(saleId));
    }
}
