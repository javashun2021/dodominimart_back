package com.ruoyi.mall.mapper;

import java.util.List;
import com.ruoyi.mall.domain.MallBanner;

public interface MallBannerMapper
{
    List<MallBanner> selectActiveBanners();
    List<MallBanner> selectAllBanners();
    MallBanner       selectBannerById(Long bannerId);
    int              insertBanner(MallBanner banner);
    int              updateBanner(MallBanner banner);
    int              deleteBannerById(Long bannerId);
}
