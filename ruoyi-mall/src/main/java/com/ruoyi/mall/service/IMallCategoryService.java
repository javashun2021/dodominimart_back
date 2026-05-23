package com.ruoyi.mall.service;

import java.util.List;
import com.ruoyi.mall.domain.MallCategory;

public interface IMallCategoryService
{
    List<MallCategory> selectCategoryList(MallCategory category);

    MallCategory selectCategoryById(Integer categoryId);

    int insertCategory(MallCategory category);

    int updateCategory(MallCategory category);

    int deleteCategoryById(Integer categoryId);
}
