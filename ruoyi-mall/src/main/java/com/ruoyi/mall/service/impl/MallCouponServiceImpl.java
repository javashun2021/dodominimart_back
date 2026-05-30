package com.ruoyi.mall.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.mall.domain.CouponDiscountResult;
import com.ruoyi.mall.domain.MallCoupon;
import com.ruoyi.mall.domain.MallMemberCoupon;
import com.ruoyi.mall.mapper.MallCouponMapper;
import com.ruoyi.mall.mapper.MallMemberCouponMapper;
import com.ruoyi.mall.service.IMallCouponService;

@Service
public class MallCouponServiceImpl implements IMallCouponService
{
    @Autowired
    private MallCouponMapper couponMapper;

    @Autowired
    private MallMemberCouponMapper memberCouponMapper;

    @Override
    @Transactional
    public void issueNewUserCoupons(Long memberId)
    {
        List<MallCoupon> templates = couponMapper.selectNewUserTemplates();
        for (MallCoupon tpl : templates)
        {
            issueCoupon(tpl, memberId);
        }
    }

    @Override
    @Transactional
    public void issueCoupon(Long couponId, Long memberId)
    {
        MallCoupon tpl = couponMapper.selectCouponById(couponId);
        if (tpl == null || tpl.getStatus() != 0)
        {
            throw new RuntimeException("Coupon not found or disabled");
        }
        issueCoupon(tpl, memberId);
    }

    private void issueCoupon(MallCoupon tpl, Long memberId)
    {
        MallMemberCoupon mc = new MallMemberCoupon();
        mc.setCouponId(tpl.getCouponId());
        mc.setMemberId(memberId);
        mc.setStatus(0);
        mc.setExpiresAt(addDays(new Date(), tpl.getValidDays()));
        memberCouponMapper.insertMemberCoupon(mc);
    }

    @Override
    public List<MallMemberCoupon> getMyCoupons(Long memberId)
    {
        return memberCouponMapper.selectByMemberId(memberId);
    }

    @Override
    @Transactional
    public CouponDiscountResult validate(Long memberCouponId, Long memberId,
            BigDecimal subtotal, BigDecimal deliveryFee, boolean isFirstOrder)
    {
        MallMemberCoupon mc = memberCouponMapper.selectByIdForUpdate(memberCouponId);
        if (mc == null || !mc.getMemberId().equals(memberId))
        {
            return CouponDiscountResult.error("Coupon not found");
        }
        if (mc.getStatus() == 1)
        {
            return CouponDiscountResult.error("Coupon already used");
        }
        if (mc.getStatus() == 2 || new Date().after(mc.getExpiresAt()))
        {
            return CouponDiscountResult.error("Coupon has expired");
        }
        if (mc.getIsFirstOrderOnly() == 1 && !isFirstOrder)
        {
            return CouponDiscountResult.error("This coupon is for your first order only");
        }
        BigDecimal minOrder = mc.getMinOrderAmount() != null ? mc.getMinOrderAmount() : BigDecimal.ZERO;
        if (subtotal.compareTo(minOrder) < 0)
        {
            return CouponDiscountResult.error(
                    "Minimum order amount ₱" + minOrder.toPlainString() + " required");
        }

        String type = mc.getType();
        if ("free_delivery".equals(type))
        {
            return CouponDiscountResult.ok(BigDecimal.ZERO, true);
        }
        if ("amount_off".equals(type))
        {
            BigDecimal discount = mc.getDiscountAmount() != null ? mc.getDiscountAmount() : BigDecimal.ZERO;
            // 折扣不超过 subtotal
            discount = discount.min(subtotal);
            return CouponDiscountResult.ok(discount, false);
        }
        if ("percent_off".equals(type))
        {
            BigDecimal pct = BigDecimal.valueOf(mc.getDiscountPercent()).divide(BigDecimal.valueOf(100));
            BigDecimal discount = subtotal.multiply(pct).setScale(2, RoundingMode.HALF_UP);
            if (mc.getMaxDiscountAmount() != null)
            {
                discount = discount.min(mc.getMaxDiscountAmount());
            }
            return CouponDiscountResult.ok(discount, false);
        }
        return CouponDiscountResult.error("Unknown coupon type");
    }

    @Override
    @Transactional
    public void markUsed(Long memberCouponId, Long memberId, String orderNo)
    {
        memberCouponMapper.markUsed(memberCouponId, orderNo, new Date());
    }

    // ── 管理后台 ──────────────────────────────────────────────────────────────

    @Override
    public List<MallMemberCoupon> listForAdmin(MallMemberCoupon query)
    {
        return memberCouponMapper.selectMemberCouponList(query);
    }

    @Override
    public List<MallCoupon> selectCouponList(MallCoupon query)
    {
        return couponMapper.selectCouponList(query);
    }

    @Override
    public MallCoupon selectCouponById(Long couponId)
    {
        return couponMapper.selectCouponById(couponId);
    }

    @Override
    public int insertCoupon(MallCoupon coupon)
    {
        coupon.setStatus(0);
        return couponMapper.insertCoupon(coupon);
    }

    @Override
    public int updateCoupon(MallCoupon coupon)
    {
        return couponMapper.updateCoupon(coupon);
    }

    @Override
    public int deleteCouponById(Long couponId)
    {
        return couponMapper.deleteCouponById(couponId);
    }

    private Date addDays(Date date, int days)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }
}
