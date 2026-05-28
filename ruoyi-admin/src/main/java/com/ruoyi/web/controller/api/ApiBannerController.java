package com.ruoyi.web.controller.api;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.mall.domain.MallBanner;
import com.ruoyi.mall.service.IMallBannerService;

@RestController
@RequestMapping("/api/v1/banners")
public class ApiBannerController
{
    @Autowired
    private IMallBannerService bannerService;

    /** GET /api/v1/banners — public, no JWT */
    @GetMapping
    public AjaxResult list()
    {
        List<MallBanner> banners = bannerService.selectActiveBanners();
        return AjaxResult.success().put("data", banners);
    }
}
