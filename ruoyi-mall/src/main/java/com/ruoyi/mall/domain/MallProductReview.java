package com.ruoyi.mall.domain;

import java.util.Date;

public class MallProductReview
{
    private Long   reviewId;
    private Long   orderId;
    private Long   productId;
    private Long   memberId;
    private Integer score;
    private String content;
    /** JSON 数组字符串，存图片 URL */
    private String images;
    private Date   createTime;

    /** 非DB：JOIN mall_member */
    private String memberNickname;
    private String memberAvatar;
    /** 非DB：JOIN mall_product */
    private String productName;

    public Long    getReviewId()                  { return reviewId; }
    public void    setReviewId(Long v)            { reviewId = v; }
    public Long    getOrderId()                   { return orderId; }
    public void    setOrderId(Long v)             { orderId = v; }
    public Long    getProductId()                 { return productId; }
    public void    setProductId(Long v)           { productId = v; }
    public Long    getMemberId()                  { return memberId; }
    public void    setMemberId(Long v)            { memberId = v; }
    public Integer getScore()                     { return score; }
    public void    setScore(Integer v)            { score = v; }
    public String  getContent()                   { return content; }
    public void    setContent(String v)           { content = v; }
    public String  getImages()                    { return images; }
    public void    setImages(String v)            { images = v; }
    public Date    getCreateTime()                { return createTime; }
    public void    setCreateTime(Date v)          { createTime = v; }
    public String  getMemberNickname()            { return memberNickname; }
    public void    setMemberNickname(String v)    { memberNickname = v; }
    public String  getMemberAvatar()              { return memberAvatar; }
    public void    setMemberAvatar(String v)      { memberAvatar = v; }
    public String  getProductName()               { return productName; }
    public void    setProductName(String v)       { productName = v; }
}
