package com.ruoyi.web.controller.api;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.mall.domain.MallFlashSale;
import com.ruoyi.mall.service.IMallFlashSaleService;

/**
 * GET /api/v1/flash-sales  — 当前进行中的限时优惠（无需登录）
 */
@RestController
@RequestMapping("/api/v1/flash-sales")
public class ApiFlashSaleController
{
    @Autowired
    private IMallFlashSaleService flashSaleService;

    @GetMapping
    public AjaxResult list()
    {
        List<MallFlashSale> list = flashSaleService.getActiveFlashSales();
        return AjaxResult.success("ok").put("data", list);
    }
}
