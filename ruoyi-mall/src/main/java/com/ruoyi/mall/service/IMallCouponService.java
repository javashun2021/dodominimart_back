package com.ruoyi.mall.service;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.mall.domain.CouponDiscountResult;
import com.ruoyi.mall.domain.MallCoupon;
import com.ruoyi.mall.domain.MallMemberCoupon;

public interface IMallCouponService
{
    /** 注册时向新会员发放默认三联新人券 */
    void issueNewUserCoupons(Long memberId);

    /** 管理员手动向指定会员发一张券 */
    void issueCoupon(Long couponId, Long memberId);

    /** App 查我的优惠券（全部状态，含已用/已过期） */
    List<MallMemberCoupon> getMyCoupons(Long memberId);

    /**
     * 校验并计算优惠金额（不修改状态，仅只读）
     *
     * @param memberCouponId 会员券实例 ID
     * @param memberId       当前会员（归属校验）
     * @param subtotal       商品小计（不含运费）
     * @param deliveryFee    运费
     * @param isFirstOrder   当前下单是否是首单（completed orders == 0）
     */
    CouponDiscountResult validate(Long memberCouponId, Long memberId,
            BigDecimal subtotal, BigDecimal deliveryFee, boolean isFirstOrder);

    /** 在 createOrder 事务内调用：标记券为已使用 */
    void markUsed(Long memberCouponId, Long memberId, String orderNo);

    // ── 管理后台 ──────────────────────────────────────────────────────────────

    List<MallCoupon> selectCouponList(MallCoupon query);

    MallCoupon selectCouponById(Long couponId);

    int insertCoupon(MallCoupon coupon);

    int updateCoupon(MallCoupon coupon);

    int deleteCouponById(Long couponId);
}
