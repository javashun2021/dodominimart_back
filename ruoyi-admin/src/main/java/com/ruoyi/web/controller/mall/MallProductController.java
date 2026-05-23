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
import com.ruoyi.mall.domain.MallProduct;
import com.ruoyi.mall.service.IMallCategoryService;
import com.ruoyi.mall.service.IMallProductService;

@Controller
@RequestMapping("/mall/product")
public class MallProductController extends BaseController
{
    private final String prefix = "mall/product";

    @Autowired
    private IMallProductService productService;

    @Autowired
    private IMallCategoryService categoryService;

    @RequiresPermissions("mall:product:view")
    @GetMapping
    public String product()
    {
        return prefix + "/product";
    }

    @RequiresPermissions("mall:product:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MallProduct product)
    {
        startPage();
        List<MallProduct> list = productService.selectProductList(product);
        return getDataTable(list);
    }

    @RequiresPermissions("mall:product:add")
    @GetMapping("/add")
    public String add(ModelMap mmap)
    {
        mmap.put("categories", categoryService.selectCategoryList(new MallCategory()));
        return prefix + "/add";
    }

    @RequiresPermissions("mall:product:add")
    @Log(title = "商品管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(MallProduct product)
    {
        product.setCreateBy(ShiroUtils.getLoginName());
        return toAjax(productService.insertProduct(product));
    }

    @RequiresPermissions("mall:product:edit")
    @GetMapping("/edit/{productId}")
    public String edit(@PathVariable Long productId, ModelMap mmap)
    {
        mmap.put("product", productService.selectProductById(productId));
        mmap.put("categories", categoryService.selectCategoryList(new MallCategory()));
        return prefix + "/edit";
    }

    @RequiresPermissions("mall:product:edit")
    @Log(title = "商品管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MallProduct product)
    {
        product.setUpdateBy(ShiroUtils.getLoginName());
        return toAjax(productService.updateProduct(product));
    }

    @RequiresPermissions("mall:product:remove")
    @Log(title = "商品管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(productService.deleteProductByIds(ids));
    }
}
