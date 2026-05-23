package com.ruoyi.web.controller.api;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.mall.domain.MallCategory;
import com.ruoyi.mall.domain.MallProduct;
import com.ruoyi.mall.domain.MallFlashSale;
import com.ruoyi.mall.service.IMallCategoryService;
import com.ruoyi.mall.service.IMallFlashSaleService;
import com.ruoyi.mall.service.IMallProductService;

/**
 * 商品浏览接口（公开，无需登录）
 * GET /api/v1/categories
 * GET /api/v1/products
 * GET /api/v1/products/{id}
 */
@RestController
@RequestMapping("/api/v1")
public class ApiProductController extends BaseApiController
{
    @Autowired
    private IMallCategoryService categoryService;

    @Autowired
    private IMallProductService productService;

    @Autowired
    private IMallFlashSaleService flashSaleService;

    /** 分类列表 */
    @GetMapping("/categories")
    public AjaxResult listCategories()
    {
        MallCategory query = new MallCategory();
        query.setStatus("0");
        List<MallCategory> list = categoryService.selectCategoryList(query);
        return AjaxResult.success("ok").put("data", list);
    }

    /**
     * 商品列表（支持按分类、关键字搜索、分页）
     * ?categoryId=&keyword=&pageNum=1&pageSize=10
     */
    @GetMapping("/products")
    public AjaxResult listProducts(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize)
    {
        startPage(pageNum, pageSize);
        MallProduct query = new MallProduct();
        query.setCategoryId(categoryId);
        query.setName(keyword);
        query.setStatus("0");
        List<MallProduct> list = productService.selectProductList(query);
        return pageResult(new PageInfo<>(list));
    }

    /** 当前进行中的限时优惠列表（首页 Banner 用） */
    @GetMapping("/flash-sales")
    public AjaxResult listFlashSales()
    {
        MallFlashSale query = new MallFlashSale();
        query.setStatus("1");
        List<MallFlashSale> list = flashSaleService.selectFlashSaleList(query);
        return AjaxResult.success("ok").put("data", list);
    }

    /** 商品详情 */
    @GetMapping("/products/{id}")
    public AjaxResult getProduct(@PathVariable Long id)
    {
        MallProduct product = productService.selectProductById(id);
        if (product == null)
        {
            return AjaxResult.error("Product not found");
        }
        return AjaxResult.success("ok").put("data", product);
    }
}
