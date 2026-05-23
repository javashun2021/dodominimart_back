package com.ruoyi.mall.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.support.Convert;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.mall.domain.MallProduct;
import com.ruoyi.mall.mapper.MallProductMapper;
import com.ruoyi.mall.service.IMallProductService;

@Service
public class MallProductServiceImpl implements IMallProductService
{
    @Autowired
    private MallProductMapper productMapper;

    @Override
    public List<MallProduct> selectProductList(MallProduct product)
    {
        return productMapper.selectProductList(product);
    }

    @Override
    public MallProduct selectProductById(Long productId)
    {
        return productMapper.selectProductById(productId);
    }

    @Override
    public int insertProduct(MallProduct product)
    {
        product.setDelFlag("0");
        product.setCreateTime(DateUtils.getNowDate());
        return productMapper.insertProduct(product);
    }

    @Override
    public int updateProduct(MallProduct product)
    {
        product.setUpdateTime(DateUtils.getNowDate());
        return productMapper.updateProduct(product);
    }

    @Override
    public int deleteProductByIds(String ids)
    {
        return productMapper.deleteProductByIds(Convert.toLongArray(ids));
    }
}
