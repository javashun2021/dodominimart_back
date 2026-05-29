package com.ruoyi.mall.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.ruoyi.mall.domain.MallBanner;
import com.ruoyi.mall.mapper.MallBannerMapper;
import com.ruoyi.mall.service.IMallBannerService;

@Service
public class MallBannerServiceImpl implements IMallBannerService
{
    @Autowired
    private MallBannerMapper bannerMapper;

    @Override
    @Cacheable(value = "mall_banners", key = "'active'")
    public List<MallBanner> selectActiveBanners()         { return bannerMapper.selectActiveBanners(); }

    @Override
    public List<MallBanner> selectAllBanners()            { return bannerMapper.selectAllBanners(); }

    @Override
    public MallBanner selectBannerById(Long bannerId)     { return bannerMapper.selectBannerById(bannerId); }

    @Override
    @CacheEvict(value = "mall_banners", allEntries = true)
    public int insertBanner(MallBanner banner)
    {
        banner.setCreateTime(new Date());
        if (banner.getStatus()   == null) banner.setStatus("0");
        if (banner.getLinkType() == null) banner.setLinkType("NONE");
        if (banner.getSort()     == null) banner.setSort(0);
        return bannerMapper.insertBanner(banner);
    }

    @Override
    @CacheEvict(value = "mall_banners", allEntries = true)
    public int updateBanner(MallBanner banner)
    {
        banner.setUpdateTime(new Date());
        return bannerMapper.updateBanner(banner);
    }

    @Override
    @CacheEvict(value = "mall_banners", allEntries = true)
    public int deleteBannerById(Long bannerId)            { return bannerMapper.deleteBannerById(bannerId); }
}
