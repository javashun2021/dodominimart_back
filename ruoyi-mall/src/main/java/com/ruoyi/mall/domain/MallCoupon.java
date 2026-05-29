package com.ruoyi.mall.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class MallCoupon implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long       couponId;
    private String     name;
    /** free_delivery | amount_off | percent_off */
    private String     type;
    private BigDecimal discountAmount;
    private int        discountPercent;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private int        validDays;
    private int        isFirstOrderOnly;
    /** 0启用 1停用 */
    private int        status;
    private Date       createTime;

    public Long       getCouponId()                           { return couponId; }
    public void       setCouponId(Long v)                    { this.couponId = v; }

    public String     getName()                              { return name; }
    public void       setName(String v)                      { this.name = v; }

    public String     getType()                              { return type; }
    public void       setType(String v)                      { this.type = v; }

    public BigDecimal getDiscountAmount()                    { return discountAmount; }
    public void       setDiscountAmount(BigDecimal v)        { this.discountAmount = v; }

    public int        getDiscountPercent()                   { return discountPercent; }
    public void       setDiscountPercent(int v)              { this.discountPercent = v; }

    public BigDecimal getMaxDiscountAmount()                 { return maxDiscountAmount; }
    public void       setMaxDiscountAmount(BigDecimal v)     { this.maxDiscountAmount = v; }

    public BigDecimal getMinOrderAmount()                    { return minOrderAmount; }
    public void       setMinOrderAmount(BigDecimal v)        { this.minOrderAmount = v; }

    public int        getValidDays()                         { return validDays; }
    public void       setValidDays(int v)                    { this.validDays = v; }

    public int        getIsFirstOrderOnly()                  { return isFirstOrderOnly; }
    public void       setIsFirstOrderOnly(int v)             { this.isFirstOrderOnly = v; }

    public int        getStatus()                            { return status; }
    public void       setStatus(int v)                       { this.status = v; }

    public Date       getCreateTime()                        { return createTime; }
    public void       setCreateTime(Date v)                  { this.createTime = v; }
}
