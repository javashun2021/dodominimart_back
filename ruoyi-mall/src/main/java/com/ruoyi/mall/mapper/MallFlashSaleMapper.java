package com.ruoyi.mall.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.mall.domain.MallFlashSale;

public interface MallFlashSaleMapper
{
    List<MallFlashSale> selectActiveFlashSales();

    List<MallFlashSale> selectFlashSaleList(MallFlashSale flashSale);

    MallFlashSale selectFlashSaleById(Long saleId);

    /** 查询商品当前进行中的活动（下单时调用） */
    MallFlashSale selectActiveByProductId(Long productId);

    int insertFlashSale(MallFlashSale flashSale);

    int updateFlashSale(MallFlashSale flashSale);

    /** 乐观锁：sold_count+quantity，仅当剩余库存充足时更新 */
    int incrementSoldCount(@Param("saleId") Long saleId, @Param("quantity") int quantity);

    int deleteFlashSaleById(Long saleId);
}