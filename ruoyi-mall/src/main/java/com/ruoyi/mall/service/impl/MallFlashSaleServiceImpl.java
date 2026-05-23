package com.ruoyi.mall.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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
    public int insertFlashSale(MallFlashSale flashSale)
    {
        flashSale.setStatus("0");
        flashSale.setCreateTime(DateUtils.getNowDate());
        return flashSaleMapper.insertFlashSale(flashSale);
    }

    @Override
    public int updateFlashSale(MallFlashSale flashSale)
    {
        return flashSaleMapper.updateFlashSale(flashSale);
    }

    @Override
    public boolean occupyStock(Long saleId, int quantity)
    {
        return flashSaleMapper.incrementSoldCount(saleId, quantity) > 0;
    }

    @Override
    public int deleteFlashSaleById(Long saleId)
    {
        return flashSaleMapper.deleteFlashSaleById(saleId);
    }
}
