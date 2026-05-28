package com.ruoyi.web.controller.mall;

import java.util.Date;
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
import com.ruoyi.mall.domain.MallBanner;
import com.ruoyi.mall.service.IMallBannerService;

@Controller
@RequestMapping("/mall/banner")
public class MallBannerController extends BaseController
{
    private final String prefix = "mall/banner";

    @Autowired
    private IMallBannerService bannerService;

    @RequiresPermissions("mall:banner:view")
    @GetMapping
    public String banner()
    {
        return prefix + "/banner";
    }

    @RequiresPermissions("mall:banner:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MallBanner banner)
    {
        startPage();
        return getDataTable(bannerService.selectAllBanners());
    }

    @RequiresPermissions("mall:banner:add")
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    @RequiresPermissions("mall:banner:add")
    @Log(title = "Banner管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(MallBanner banner)
    {
        banner.setCreateTime(new Date());
        if (banner.getStatus() == null) banner.setStatus("0");
        return toAjax(bannerService.insertBanner(banner));
    }

    @RequiresPermissions("mall:banner:edit")
    @GetMapping("/edit/{bannerId}")
    public String edit(@PathVariable Long bannerId, ModelMap mmap)
    {
        mmap.put("banner", bannerService.selectBannerById(bannerId));
        return prefix + "/edit";
    }

    @RequiresPermissions("mall:banner:edit")
    @Log(title = "Banner管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MallBanner banner)
    {
        return toAjax(bannerService.updateBanner(banner));
    }

    @RequiresPermissions("mall:banner:remove")
    @Log(title = "Banner管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        for (String id : ids.split(","))
        {
            bannerService.deleteBannerById(Long.parseLong(id.trim()));
        }
        return success();
    }
}
