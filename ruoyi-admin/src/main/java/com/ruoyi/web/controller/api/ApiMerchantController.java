package com.ruoyi.web.controller.api;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.common.utils.ImageUrlUtils;
import com.ruoyi.mall.domain.MallMerchant;
import com.ruoyi.mall.domain.MallProduct;
import com.ruoyi.mall.service.IMallMerchantService;
import com.ruoyi.mall.service.IMallProductService;

/**
 * 附近商家接口（公开，无需登录）
 * GET /api/v1/merchants          — 附近商家，按 GPS 真实距离升序
 * GET /api/v1/merchants/{id}     — 商家详情 + 其上架商品
 */
@RestController
@RequestMapping("/api/v1/merchants")
public class ApiMerchantController
{
    @Autowired
    private IMallMerchantService merchantService;

    @Autowired
    private IMallProductService productService;

    /** 附近商家：只返营业中(status=1)，有坐标按距离升序 */
    @GetMapping
    public AjaxResult list(@RequestParam(required = false) BigDecimal lat,
                           @RequestParam(required = false) BigDecimal lng,
                           @RequestParam(required = false) String category,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(defaultValue = "1") int pageNum,
                           @RequestParam(defaultValue = "10") int pageSize)
    {
        List<MallMerchant> all = merchantService.selectNearby(lat, lng, category, keyword);
        int total = all.size();
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 200) pageSize = 200;
        int from = Math.min((pageNum - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<MallMerchant> pageList = all.subList(from, to);
        for (MallMerchant m : pageList) normalizeImages(m);
        return AjaxResult.success("ok")
                .put("total", total)
                .put("pageNum", pageNum)
                .put("pageSize", pageSize)
                .put("list", pageList);
    }

    /** 商家详情 + 其上架商品；未过审/停业的商家不对外展示 */
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        MallMerchant m = merchantService.selectMerchantById(id);
        if (m == null || !"1".equals(m.getStatus()) || "2".equals(m.getDelFlag()))
        {
            return AjaxResult.error("Merchant not available");
        }
        normalizeImages(m);
        MallProduct query = new MallProduct();
        query.setMerchantId(id);
        query.setStatus("0");
        List<MallProduct> products = productService.selectProductList(query);
        return AjaxResult.success("ok")
                .put("merchant", m)
                .put("products", products);
    }

    /** 商家 logoUrl/images 归一化为相对路径（兼容历史存量的内网绝对地址）。 */
    private void normalizeImages(MallMerchant m)
    {
        if (m == null) return;
        m.setLogoUrl(ImageUrlUtils.toRelative(m.getLogoUrl()));
        m.setImages(ImageUrlUtils.toRelative(m.getImages()));
    }
}
