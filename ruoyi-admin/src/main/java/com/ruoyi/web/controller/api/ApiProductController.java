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
import com.ruoyi.mall.domain.MallMerchant;
import com.ruoyi.mall.domain.MallProduct;
import com.ruoyi.mall.service.IMallCategoryService;
import com.ruoyi.mall.service.IMallMerchantService;
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
    private IMallMerchantService merchantService;

    @Autowired
    private com.ruoyi.mall.service.IPlatformToggleService platformToggleService;

    @Autowired
    private com.ruoyi.mall.mapper.MallProductStockMapper productStockMapper;

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
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "false") boolean onlyFlashSale,
            @RequestParam(defaultValue = "false") boolean onlyGroupBuy,
            @RequestParam(defaultValue = "false") boolean inStockOnly,
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize)
    {
        startPage(pageNum, pageSize);
        java.util.Set<String> allowed = new java.util.HashSet<>(java.util.Arrays.asList("price_asc", "price_desc", "newest"));
        String safeSortBy = (sortBy != null && allowed.contains(sortBy)) ? sortBy : null;
        MallProduct query = new MallProduct();
        query.setCategoryId(categoryId);
        query.setName(keyword);
        query.setStatus("0");
        query.setSortBy(safeSortBy);
        query.setOnlyFlashSale(onlyFlashSale);
        query.setOnlyGroupBuy(onlyGroupBuy);
        query.setInStockOnly(inStockOnly);
        // 平台首页/自营目录 = 自营商家(mall.self.merchant.id，即 DodoMiniMart 自家门店)的商品；
        // 未配置则回退旧语义「平台自营 merchant_id IS NULL」。入驻商家商品仍走 /merchants。
        Long selfMerchantId = platformToggleService.getSelfMerchantId();
        if (selfMerchantId != null)
        {
            query.setMerchantId(selfMerchantId);
        }
        else
        {
            query.setSelfOperatedOnly(true);
        }
        List<MallProduct> list = productService.selectProductList(query);
        // 门店级库存覆盖：传了 storeId 就把有独立库存的商品 stock 换成本店库存（无覆盖仍用总库存）
        applyStoreStock(list, storeId);
        return pageResult(new PageInfo<>(list));
    }

    /**
     * 跨店聚合商品列表：自营 + 附近商户商品,一次性返回,每个商品带 storeName/storeDistanceKm。
     * 替代 App 客户端「先取商户列表、再逐个商户拉详情」的 N+1 聚合。
     * onlyFeatured=true 时只回首页精选(is_featured=1),供首页精选板块用。
     * ?lat=&lng=&categoryId=&keyword=&sortBy=&onlyFlashSale=&onlyGroupBuy=&inStockOnly=&onlyFeatured=&pageNum=&pageSize=
     */
    @GetMapping("/products/nearby")
    public AjaxResult listNearbyProducts(
            @RequestParam(required = false) java.math.BigDecimal lat,
            @RequestParam(required = false) java.math.BigDecimal lng,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "false") boolean onlyFlashSale,
            @RequestParam(defaultValue = "false") boolean onlyGroupBuy,
            @RequestParam(defaultValue = "false") boolean inStockOnly,
            @RequestParam(defaultValue = "false") boolean onlyFeatured,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize)
    {
        java.util.Set<String> allowed = new java.util.HashSet<>(java.util.Arrays.asList("price_asc", "price_desc", "newest"));
        String safeSortBy = (sortBy != null && allowed.contains(sortBy)) ? sortBy : null;
        Long selfMerchantId = platformToggleService.getSelfMerchantId();

        java.util.List<MallProduct> merged = new java.util.ArrayList<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();

        // 1) 自营商品(服务端已按分类/关键字/筛选过滤)。自营距离置 0,永远排最前。
        MallProduct selfQuery = new MallProduct();
        selfQuery.setCategoryId(categoryId);
        selfQuery.setName(keyword);
        selfQuery.setStatus("0");
        selfQuery.setSortBy(safeSortBy);
        selfQuery.setOnlyFlashSale(onlyFlashSale);
        selfQuery.setOnlyGroupBuy(onlyGroupBuy);
        selfQuery.setInStockOnly(inStockOnly);
        if (selfMerchantId != null) selfQuery.setMerchantId(selfMerchantId);
        else selfQuery.setSelfOperatedOnly(true);
        for (MallProduct p : productService.selectProductList(selfQuery))
        {
            if (seen.add(p.getProductId()))
            {
                p.setStoreDistanceKm(java.math.BigDecimal.ZERO);
                merged.add(p);
            }
        }

        // 2) 附近商户商品。团购/限时筛选时跳过(入驻商户暂无这两类活动)。
        if (!onlyFlashSale && !onlyGroupBuy)
        {
            // 商户按距离升序;keyword 用于过滤「商品名」而非店名,故这里 selectNearby 不传 keyword。
            java.util.List<MallMerchant> merchants = merchantService.selectNearby(lat, lng, null, null);
            int cap = 0;
            for (MallMerchant m : merchants)
            {
                if (m.getMerchantId() == null) continue;
                if (selfMerchantId != null && selfMerchantId.equals(m.getMerchantId())) continue; // 自营已加
                if (++cap > 50) break; // 兜底上限,防商户过多拖慢
                MallProduct q = new MallProduct();
                q.setMerchantId(m.getMerchantId());
                q.setCategoryId(categoryId);
                q.setName(keyword);
                q.setStatus("0");
                q.setInStockOnly(inStockOnly);
                for (MallProduct p : productService.selectProductList(q))
                {
                    if (seen.add(p.getProductId()))
                    {
                        p.setStoreName(m.getName());
                        p.setStoreDistanceKm(m.getDistanceKm());
                        merged.add(p);
                    }
                }
            }
        }

        // 3) 首页精选:只保留 is_featured=1
        if (onlyFeatured)
        {
            merged.removeIf(p -> !Integer.valueOf(1).equals(p.getIsFeatured()));
        }

        // 4) 排序:价格排序按价格,否则按店铺距离(自营=0 最前;无坐标视为 0)
        if ("price_asc".equals(safeSortBy))
        {
            merged.sort(java.util.Comparator.comparing(ApiProductController::priceOf));
        }
        else if ("price_desc".equals(safeSortBy))
        {
            merged.sort(java.util.Comparator.comparing(ApiProductController::priceOf).reversed());
        }
        else
        {
            merged.sort(java.util.Comparator.comparing(ApiProductController::distOf));
        }

        // 5) 内存分页
        int total = merged.size();
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 200) pageSize = 200;
        int from = Math.min((pageNum - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        return AjaxResult.success("ok")
                .put("total", (long) total)
                .put("pageNum", pageNum)
                .put("pageSize", pageSize)
                .put("list", new java.util.ArrayList<>(merged.subList(from, to)));
    }

    /** 聚合排序用:限时价优先,否则原价(null 视为 0)。 */
    private static java.math.BigDecimal priceOf(MallProduct p)
    {
        java.math.BigDecimal fp = p.getFlashPrice();
        if (fp != null) return fp;
        return p.getPrice() == null ? java.math.BigDecimal.ZERO : p.getPrice();
    }

    /** 聚合排序用:店铺距离(null 视为 0,自营/无坐标排最前)。 */
    private static java.math.BigDecimal distOf(MallProduct p)
    {
        return p.getStoreDistanceKm() == null ? java.math.BigDecimal.ZERO : p.getStoreDistanceKm();
    }

    /** 商品详情（可带 storeId 取本店库存） */
    @GetMapping("/products/{id}")
    public AjaxResult getProduct(@PathVariable Long id,
                                 @RequestParam(required = false) Long storeId)
    {
        MallProduct product = productService.selectProductById(id);
        if (product == null)
        {
            return AjaxResult.error("Product not found");
        }
        if (storeId != null)
        {
            Integer s = productStockMapper.selectStock(id, storeId);
            if (s != null) product.setStock(s);
        }
        return AjaxResult.success("ok").put("data", product);
    }

    /** 把列表里「该门店配了独立库存」的商品 stock 覆盖为本店库存（storeId 为空则原样返回总库存）。 */
    private void applyStoreStock(List<MallProduct> list, Long storeId)
    {
        if (storeId == null || list == null || list.isEmpty()) return;
        java.util.Map<Long, Integer> overrides = new java.util.HashMap<>();
        for (com.ruoyi.mall.domain.MallProductStock ps : productStockMapper.selectByStore(storeId))
        {
            overrides.put(ps.getProductId(), ps.getStock());
        }
        if (overrides.isEmpty()) return;
        for (MallProduct p : list)
        {
            Integer s = overrides.get(p.getProductId());
            if (s != null) p.setStock(s);
        }
    }
}
