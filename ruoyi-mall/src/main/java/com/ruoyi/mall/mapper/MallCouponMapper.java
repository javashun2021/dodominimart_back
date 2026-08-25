package com.ruoyi.mall.mapper;

import java.util.List;
import com.ruoyi.mall.domain.MallCoupon;

public interface MallCouponMapper
{
    MallCoupon selectCouponById(Long couponId);

    List<MallCoupon> selectCouponList(MallCoupon query);

    /** 查出 status=0 的新人默认模板（按 create_time asc 取前 N 张） */
    List<MallCoupon> selectNewUserTemplates();

    /** 按面额精确匹配一个启用中的 amount_off 补差券模板（无则返回 null） */
    MallCoupon selectAmountOffTemplate(java.math.BigDecimal discountAmount);

    int insertCoupon(MallCoupon coupon);

    int updateCoupon(MallCoupon coupon);

    int deleteCouponById(Long couponId);
}
