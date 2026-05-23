package com.ruoyi.mall.service;

import java.util.List;
import com.ruoyi.mall.domain.MallProduct;

public interface IMallProductService
{
    List<MallProduct> selectProductList(MallProduct product);

    MallProduct selectProductById(Long productId);

    int insertProduct(MallProduct product);

    int updateProduct(MallProduct product);

    int deleteProductByIds(String ids);
}
