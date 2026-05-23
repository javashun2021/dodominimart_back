package com.ruoyi.mall.domain;

import com.ruoyi.common.base.BaseEntity;

/**
 * 商品分类表 mall_category
 */
public class MallCategory extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Integer categoryId;
    private String name;
    private String iconUrl;
    private Integer sort;
    /** 状态（0显示 1隐藏） */
    private String status;

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
