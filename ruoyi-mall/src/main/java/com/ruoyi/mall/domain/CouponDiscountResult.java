package com.ruoyi.mall.domain;

import java.math.BigDecimal;

public class CouponDiscountResult
{
    private final BigDecimal discountAmount;
    private final boolean    freeDelivery;
    private final String     errorMessage;

    private CouponDiscountResult(BigDecimal discountAmount, boolean freeDelivery, String errorMessage)
    {
        this.discountAmount = discountAmount;
        this.freeDelivery   = freeDelivery;
        this.errorMessage   = errorMessage;
    }

    public static CouponDiscountResult ok(BigDecimal discount, boolean freeDelivery)
    {
        return new CouponDiscountResult(discount, freeDelivery, null);
    }

    public static CouponDiscountResult error(String message)
    {
        return new CouponDiscountResult(BigDecimal.ZERO, false, message);
    }

    public boolean    isValid()           { return errorMessage == null; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public boolean    isFreeDelivery()    { return freeDelivery; }
    public String     getErrorMessage()  { return errorMessage; }
}
