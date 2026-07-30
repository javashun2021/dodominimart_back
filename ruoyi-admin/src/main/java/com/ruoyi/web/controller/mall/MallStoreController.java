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
import java.util.HashMap;
import java.util.Map;
import com.ruoyi.mall.domain.MallStore;
import com.ruoyi.mall.domain.MallMerchant;
import com.ruoyi.mall.domain.MallProduct;
import com.ruoyi.mall.domain.MallProductStock;
import com.ruoyi.mall.service.IMallStoreService;
import com.ruoyi.mall.service.IMallMerchantService;
import com.ruoyi.mall.service.IMallProductService;

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

    @Autowired
    private IMallMerchantService merchantService;

    @Autowired
    private IMallProductService productService;

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
    public String add(ModelMap mmap)
    {
        mmap.put("merchants", merchantService.selectMerchantList(new MallMerchant()));
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
        mmap.put("merchants", merchantService.selectMerchantList(new MallMerchant()));
        return prefix + "/edit";
    }

    /** 门店级商品库存覆盖：配置页（列出该店所属商户的商品 + 本店独立库存） */
    @RequiresPermissions("mall:store:edit")
    @GetMapping("/stock/{storeId}")
    public String stock(@PathVariable Long storeId, ModelMap mmap)
    {
        MallStore store = storeService.selectStoreById(storeId);
        mmap.put("store", store);
        MallProduct q = new MallProduct();
        q.setStatus("0");
        if (store != null && store.getMerchantId() != null)
        {
            q.setMerchantId(store.getMerchantId());
        }
        else
        {
            q.setSelfOperatedOnly(true);
        }
        mmap.put("products", productService.selectProductList(q));
        Map<Long, Integer> overrides = new HashMap<>();
        if (store != null)
        {
            for (MallProductStock ps : storeService.listStoreStock(storeId))
            {
                overrides.put(ps.getProductId(), ps.getStock());
            }
        }
        mmap.put("overrides", overrides);
        return prefix + "/stock";
    }

    /** 门店级商品库存覆盖：保存单行（stock 留空=清除，回退用商户总库存） */
    @RequiresPermissions("mall:store:edit")
    @Log(title = "门店库存", businessType = BusinessType.UPDATE)
    @PostMapping("/stock/save")
    @ResponseBody
    public AjaxResult stockSave(Long storeId, Long productId, String stock)
    {
        if (storeId == null || productId == null)
        {
            return error("storeId and productId are required");
        }
        Integer s;
        try
        {
            s = (stock == null || stock.trim().isEmpty()) ? null : Integer.valueOf(stock.trim());
        }
        catch (NumberFormatException e)
        {
            return error("Invalid stock value");
        }
        if (s != null && s < 0)
        {
            return error("Stock cannot be negative");
        }
        storeService.saveStoreStock(productId, storeId, s);
        return success();
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
