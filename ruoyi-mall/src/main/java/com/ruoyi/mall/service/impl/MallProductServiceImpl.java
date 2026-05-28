package com.ruoyi.mall.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.support.Convert;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.mall.domain.MallProduct;
import com.ruoyi.mall.mapper.MallProductMapper;
import com.ruoyi.mall.mapper.MallProductReviewMapper;
import com.ruoyi.mall.service.IMallProductService;

@Service
public class MallProductServiceImpl implements IMallProductService
{
    @Autowired
    private MallProductMapper productMapper;
    @Autowired
    private MallProductReviewMapper reviewMapper;

    @Override
    public List<MallProduct> selectProductList(MallProduct product)
    {
        return productMapper.selectProductList(product);
    }

    @Override
    public MallProduct selectProductById(Long productId)
    {
        MallProduct product = productMapper.selectProductById(productId);
        if (product != null)
        {
            Map<String, Object> stats = reviewMapper.selectAvgScoreAndCount(productId);
            if (stats != null)
            {
                Object avg = stats.get("avgScore");
                Object cnt = stats.get("reviewCount");
                product.setAvgScore(avg != null ? new BigDecimal(avg.toString()) : null);
                product.setReviewCount(cnt != null ? Integer.valueOf(cnt.toString()) : 0);
            }
        }
        return product;
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
