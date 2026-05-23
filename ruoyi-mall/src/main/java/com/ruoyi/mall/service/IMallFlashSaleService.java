package com.ruoyi.mall.service;

import java.util.List;
import com.ruoyi.mall.domain.MallFlashSale;

public interface IMallFlashSaleService
{
    List<MallFlashSale> selectFlashSaleList(MallFlashSale flashSale);

    MallFlashSale selectFlashSaleById(Long saleId);

    MallFlashSale selectActiveByProductId(Long productId);

    int insertFlashSale(MallFlashSale flashSale);

    int updateFlashSale(MallFlashSale flashSale);

    /**
     * 下单时占用活动库存（乐观锁），返回 false 表示库存不足
     */
    boolean occupyStock(Long saleId, int quantity);

    int deleteFlashSaleById(Long saleId);
}
