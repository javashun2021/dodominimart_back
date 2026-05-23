package com.ruoyi.mall.mapper;

import java.util.List;
import com.ruoyi.mall.domain.MallCategory;

public interface MallCategoryMapper
{
    List<MallCategory> selectCategoryList(MallCategory category);

    MallCategory selectCategoryById(Integer categoryId);

    int insertCategory(MallCategory category);

    int updateCategory(MallCategory category);

    int deleteCategoryById(Integer categoryId);
}
