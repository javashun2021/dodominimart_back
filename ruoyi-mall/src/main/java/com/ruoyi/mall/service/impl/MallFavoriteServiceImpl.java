package com.ruoyi.mall.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.mall.domain.MallProduct;
import com.ruoyi.mall.mapper.MallFavoriteMapper;
import com.ruoyi.mall.service.IMallFavoriteService;

@Service
public class MallFavoriteServiceImpl implements IMallFavoriteService
{
    @Autowired
    private MallFavoriteMapper favoriteMapper;

    @Override
    public List<MallProduct> getFavoriteProducts(Long memberId)
    {
        return favoriteMapper.selectFavoriteProducts(memberId);
    }

    @Override
    public List<Long> getFavoriteProductIds(Long memberId)
    {
        return favoriteMapper.selectFavoriteProductIds(memberId);
    }

    @Override
    public boolean isFavorited(Long memberId, Long productId)
    {
        return favoriteMapper.isFavorited(memberId, productId) > 0;
    }

    @Override
    public boolean toggle(Long memberId, Long productId)
    {
        if (isFavorited(memberId, productId))
        {
            favoriteMapper.deleteFavorite(memberId, productId);
            return false;
        }
        else
        {
            favoriteMapper.insertFavorite(memberId, productId);
            return true;
        }
    }
}
