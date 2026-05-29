package com.ruoyi.mall.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.mall.domain.MallCategory;
import com.ruoyi.mall.mapper.MallCategoryMapper;
import com.ruoyi.mall.service.IMallCategoryService;

@Service
public class MallCategoryServiceImpl implements IMallCategoryService
{
    @Autowired
    private MallCategoryMapper categoryMapper;

    @Override
    @Cacheable(value = "mall_categories", key = "#category.status ?: 'all'")
    public List<MallCategory> selectCategoryList(MallCategory category)
    {
        return categoryMapper.selectCategoryList(category);
    }

    @Override
    public MallCategory selectCategoryById(Integer categoryId)
    {
        return categoryMapper.selectCategoryById(categoryId);
    }

    @Override
    @CacheEvict(value = "mall_categories", allEntries = true)
    public int insertCategory(MallCategory category)
    {
        category.setCreateTime(DateUtils.getNowDate());
        return categoryMapper.insertCategory(category);
    }

    @Override
    @CacheEvict(value = "mall_categories", allEntries = true)
    public int updateCategory(MallCategory category)
    {
        category.setUpdateTime(DateUtils.getNowDate());
        return categoryMapper.updateCategory(category);
    }

    @Override
    @CacheEvict(value = "mall_categories", allEntries = true)
    public int deleteCategoryById(Integer categoryId)
    {
        return categoryMapper.deleteCategoryById(categoryId);
    }
}
