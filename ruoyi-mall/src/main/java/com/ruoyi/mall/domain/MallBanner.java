package com.ruoyi.mall.domain;

import java.util.Date;

public class MallBanner
{
    private Long   bannerId;
    private String imageUrl;
    /** NONE / PRODUCT / GROUP / URL */
    private String linkType;
    private String linkValue;
    private Integer sort;
    /** 0启用 1停用 */
    private String status;
    private Date   createTime;
    private Date   updateTime;

    public Long    getBannerId()              { return bannerId; }
    public void    setBannerId(Long v)        { bannerId = v; }
    public String  getImageUrl()              { return imageUrl; }
    public void    setImageUrl(String v)      { imageUrl = v; }
    public String  getLinkType()              { return linkType; }
    public void    setLinkType(String v)      { linkType = v; }
    public String  getLinkValue()             { return linkValue; }
    public void    setLinkValue(String v)     { linkValue = v; }
    public Integer getSort()                  { return sort; }
    public void    setSort(Integer v)         { sort = v; }
    public String  getStatus()                { return status; }
    public void    setStatus(String v)        { status = v; }
    public Date    getCreateTime()            { return createTime; }
    public void    setCreateTime(Date v)      { createTime = v; }
    public Date    getUpdateTime()            { return updateTime; }
    public void    setUpdateTime(Date v)      { updateTime = v; }
}
