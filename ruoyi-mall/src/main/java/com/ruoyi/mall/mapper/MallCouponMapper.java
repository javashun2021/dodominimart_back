package com.ruoyi.mall.mapper;

import java.util.List;
import com.ruoyi.mall.domain.MallCoupon;

public interface MallCouponMapper
{
    MallCoupon selectCouponById(Long couponId);

    List<MallCoupon> selectCouponList(MallCoupon query);

    /** 查出 status=0 的新人默认模板（按 create_time asc 取前 N 张） */
    List<MallCoupon> selectNewUserTemplates();

    int insertCoupon(MallCoupon coupon);

    int updateCoupon(MallCoupon coupon);

    int deleteCouponById(Long couponId);
}
