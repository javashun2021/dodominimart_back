package com.ruoyi.mall.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class MallMemberCoupon implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long   id;
    private Long   couponId;
    private Long   memberId;
    /** 0未使用 1已使用 2已过期；null 表示不过滤 */
    private Integer status;
    private Date   expiresAt;
    private String orderNo;
    private Date   usedTime;
    private Date   createTime;

    // ── 非 DB 字段（JOIN mall_coupon + mall_member 填充） ───────────────────
    private String     nickName;
    private String     couponName;
    private String     type;
    private BigDecimal discountAmount;
    private int        discountPercent;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private int        isFirstOrderOnly;

    public Long   getId()                                    { return id; }
    public void   setId(Long v)                             { this.id = v; }

    public Long   getCouponId()                              { return couponId; }
    public void   setCouponId(Long v)                       { this.couponId = v; }

    public Long   getMemberId()                              { return memberId; }
    public void   setMemberId(Long v)                       { this.memberId = v; }

    public Integer getStatus()                               { return status; }
    public void    setStatus(Integer v)                     { this.status = v; }

    public Date   getExpiresAt()                             { return expiresAt; }
    public void   setExpiresAt(Date v)                      { this.expiresAt = v; }

    public String getOrderNo()                               { return orderNo; }
    public void   setOrderNo(String v)                      { this.orderNo = v; }

    public Date   getUsedTime()                              { return usedTime; }
    public void   setUsedTime(Date v)                       { this.usedTime = v; }

    public Date   getCreateTime()                            { return createTime; }
    public void   setCreateTime(Date v)                     { this.createTime = v; }

    public String     getNickName()                           { return nickName; }
    public void       setNickName(String v)                  { this.nickName = v; }

    public String     getCouponName()                        { return couponName; }
    public void       setCouponName(String v)               { this.couponName = v; }

    public String     getType()                              { return type; }
    public void       setType(String v)                     { this.type = v; }

    public BigDecimal getDiscountAmount()                    { return discountAmount; }
    public void       setDiscountAmount(BigDecimal v)       { this.discountAmount = v; }

    public int        getDiscountPercent()                   { return discountPercent; }
    public void       setDiscountPercent(int v)             { this.discountPercent = v; }

    public BigDecimal getMaxDiscountAmount()                 { return maxDiscountAmount; }
    public void       setMaxDiscountAmount(BigDecimal v)    { this.maxDiscountAmount = v; }

    public BigDecimal getMinOrderAmount()                    { return minOrderAmount; }
    public void       setMinOrderAmount(BigDecimal v)       { this.minOrderAmount = v; }

    public int        getIsFirstOrderOnly()                  { return isFirstOrderOnly; }
    public void       setIsFirstOrderOnly(int v)            { this.isFirstOrderOnly = v; }
}
