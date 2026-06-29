package com.ruoyi.mall.mapper;

import java.util.List;
import com.ruoyi.mall.domain.MallProduct;

public interface MallProductMapper
{
    List<MallProduct> selectProductList(MallProduct product);

    MallProduct selectProductById(Long productId);

    int insertProduct(MallProduct product);

    int updateProduct(MallProduct product);

    /** 原子扣库存：stock = stock - quantity WHERE stock >= quantity，返回影响行数（0 表示库存不足） */
    int deductStock(@org.apache.ibatis.annotations.Param("productId") Long productId,
                    @org.apache.ibatis.annotations.Param("quantity")  int quantity);

    /** 退款回补库存：stock = stock + quantity（取消/退款时把下单扣掉的库存加回） */
    int restoreStock(@org.apache.ibatis.annotations.Param("productId") Long productId,
                     @org.apache.ibatis.annotations.Param("quantity")  int quantity);

    int deleteProductById(Long productId);

    int deleteProductByIds(Long[] productIds);
}
