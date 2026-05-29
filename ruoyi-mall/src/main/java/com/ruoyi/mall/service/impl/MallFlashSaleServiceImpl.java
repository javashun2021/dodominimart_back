package com.ruoyi.mall.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.mall.domain.MallFlashSale;
import com.ruoyi.mall.mapper.MallFlashSaleMapper;
import com.ruoyi.mall.service.IMallFlashSaleService;

@Service
public class MallFlashSaleServiceImpl implements IMallFlashSaleService
{
    @Autowired
    private MallFlashSaleMapper flashSaleMapper;

    @Override
    @Cacheable(value = "mall_flash_sales", key = "'active'")
    public List<MallFlashSale> getActiveFlashSales()
    {
        return flashSaleMapper.selectActiveFlashSales();
    }

    @Override
    public List<MallFlashSale> selectFlashSaleList(MallFlashSale flashSale)
    {
        return flashSaleMapper.selectFlashSaleList(flashSale);
    }

    @Override
    public MallFlashSale selectFlashSaleById(Long saleId)
    {
        return flashSaleMapper.selectFlashSaleById(saleId);
    }

    @Override
    public MallFlashSale selectActiveByProductId(Long productId)
    {
        return flashSaleMapper.selectActiveByProductId(productId);
    }

    @Override
    @CacheEvict(value = "mall_flash_sales", allEntries = true)
    public int insertFlashSale(MallFlashSale flashSale)
    {
        flashSale.setStatus("0");
        flashSale.setCreateTime(DateUtils.getNowDate());
        return flashSaleMapper.insertFlashSale(flashSale);
    }

    @Override
    @CacheEvict(value = "mall_flash_sales", allEntries = true)
    public int updateFlashSale(MallFlashSale flashSale)
    {
        return flashSaleMapper.updateFlashSale(flashSale);
    }

    @Override
    @CacheEvict(value = "mall_flash_sales", allEntries = true)
    public boolean occupyStock(Long saleId, int quantity)
    {
        return flashSaleMapper.incrementSoldCount(saleId, quantity) > 0;
    }

    @Override
    @CacheEvict(value = "mall_flash_sales", allEntries = true)
    public int deleteFlashSaleById(Long saleId)
    {
        return flashSaleMapper.deleteFlashSaleById(saleId);
    }
}
