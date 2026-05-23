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
import com.ruoyi.mall.domain.MallCategory;
import com.ruoyi.mall.service.IMallCategoryService;

@Controller
@RequestMapping("/mall/category")
public class MallCategoryController extends BaseController
{
    private final String prefix = "mall/category";

    @Autowired
    private IMallCategoryService categoryService;

    @RequiresPermissions("mall:category:view")
    @GetMapping
    public String category()
    {
        return prefix + "/category";
    }

    @RequiresPermissions("mall:category:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MallCategory category)
    {
        startPage();
        List<MallCategory> list = categoryService.selectCategoryList(category);
        return getDataTable(list);
    }

    @RequiresPermissions("mall:category:add")
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    @RequiresPermissions("mall:category:add")
    @Log(title = "商品分类", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(MallCategory category)
    {
        category.setCreateBy(ShiroUtils.getLoginName());
        return toAjax(categoryService.insertCategory(category));
    }

    @RequiresPermissions("mall:category:edit")
    @GetMapping("/edit/{categoryId}")
    public String edit(@PathVariable Integer categoryId, ModelMap mmap)
    {
        mmap.put("category", categoryService.selectCategoryById(categoryId));
        return prefix + "/edit";
    }

    @RequiresPermissions("mall:category:edit")
    @Log(title = "商品分类", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MallCategory category)
    {
        category.setUpdateBy(ShiroUtils.getLoginName());
        return toAjax(categoryService.updateCategory(category));
    }

    @RequiresPermissions("mall:category:remove")
    @Log(title = "商品分类", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(Integer categoryId)
    {
        return toAjax(categoryService.deleteCategoryById(categoryId));
    }
}
