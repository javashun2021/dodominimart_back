package com.ruoyi.web.controller.api;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.mall.domain.MallStore;
import com.ruoyi.mall.service.IMallStoreService;

/**
 * 门店接口（无需登录）
 * GET /api/v1/stores          — 营业中的门店列表
 * GET /api/v1/stores/nearest  — 按经纬度就近取一家门店
 */
@RestController
@RequestMapping("/api/v1/stores")
public class ApiStoreController
{
    @Autowired
    private IMallStoreService storeService;

    /** 营业中的门店列表 */
    @GetMapping
    public AjaxResult list()
    {
        List<MallStore> stores = storeService.listActiveStores();
        return AjaxResult.success("ok").put("data", stores);
    }

    /** 就近门店：传 lat/lng 取最近一家（distanceKm 已填充）；缺参则返回排序最前一家 */
    @GetMapping("/nearest")
    public AjaxResult nearest(@RequestParam(required = false) BigDecimal lat,
                              @RequestParam(required = false) BigDecimal lng)
    {
        MallStore store = storeService.selectNearestStore(lat, lng);
        if (store == null)
        {
            return AjaxResult.error("暂无可用门店");
        }
        return AjaxResult.success("ok").put("data", store);
    }
}
