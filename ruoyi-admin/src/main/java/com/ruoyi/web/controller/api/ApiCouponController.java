package com.ruoyi.web.controller.api;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.mall.domain.MallMemberCoupon;
import com.ruoyi.mall.service.IMallCouponService;

/**
 * 优惠券接口（需 JWT）
 * GET /api/v1/coupons  — 查我的优惠券（全部状态）
 */
@RestController
@RequestMapping("/api/v1/coupons")
public class ApiCouponController extends BaseApiController
{
    @Autowired
    private IMallCouponService couponService;

    @GetMapping
    public AjaxResult getMyCoupons(HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        List<MallMemberCoupon> list = couponService.getMyCoupons(memberId);
        return AjaxResult.success("ok").put("data", list);
    }
}
