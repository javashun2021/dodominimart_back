package com.ruoyi.mall.domain;

import java.util.Date;

public class MallFavorite
{
    private Long memberId;
    private Long productId;
    private Date createTime;

    public Long getMemberId()             { return memberId; }
    public void setMemberId(Long v)       { memberId = v; }
    public Long getProductId()            { return productId; }
    public void setProductId(Long v)      { productId = v; }
    public Date getCreateTime()           { return createTime; }
    public void setCreateTime(Date v)     { createTime = v; }
}
