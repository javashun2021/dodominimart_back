package com.ruoyi.mall.mapper;

import java.util.List;
import com.ruoyi.mall.domain.MallProduct;

public interface MallProductMapper
{
    List<MallProduct> selectProductList(MallProduct product);

    MallProduct selectProductById(Long productId);

    int insertProduct(MallProduct product);

    int updateProduct(MallProduct product);

    int deleteProductById(Long productId);

    int deleteProductByIds(Long[] productIds);
}
