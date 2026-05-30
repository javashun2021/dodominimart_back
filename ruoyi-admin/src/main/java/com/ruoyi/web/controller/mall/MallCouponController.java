package com.ruoyi.web.controller.mall;

import java.util.List;
import java.util.Map;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.page.TableDataInfo;
import com.ruoyi.framework.web.base.BaseController;
import com.ruoyi.mall.domain.MallCoupon;
import com.ruoyi.mall.domain.MallMemberCoupon;
import com.ruoyi.mall.service.IMallCouponService;

@Controller
@RequestMapping("/mall/coupon")
public class MallCouponController extends BaseController
{
    private final String prefix = "mall/coupon";

    @Autowired
    private IMallCouponService couponService;

    @RequiresPermissions("mall:coupon:view")
    @GetMapping
    public String coupon()
    {
        return prefix + "/coupon";
    }

    @RequiresPermissions("mall:coupon:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MallCoupon coupon)
    {
        startPage();
        return getDataTable(couponService.selectCouponList(coupon));
    }

    @RequiresPermissions("mall:coupon:add")
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    @RequiresPermissions("mall:coupon:add")
    @Log(title = "优惠券管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(MallCoupon coupon)
    {
        return toAjax(couponService.insertCoupon(coupon));
    }

    @RequiresPermissions("mall:coupon:edit")
    @GetMapping("/edit/{couponId}")
    public String edit(@PathVariable Long couponId, ModelMap mmap)
    {
        mmap.put("coupon", couponService.selectCouponById(couponId));
        return prefix + "/edit";
    }

    @RequiresPermissions("mall:coupon:edit")
    @Log(title = "优惠券管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MallCoupon coupon)
    {
        return toAjax(couponService.updateCoupon(coupon));
    }

    @RequiresPermissions("mall:coupon:remove")
    @Log(title = "优惠券管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        for (String id : ids.split(","))
        {
            couponService.deleteCouponById(Long.parseLong(id.trim()));
        }
        return success();
    }

    /** 会员持券列表页 */
    @RequiresPermissions("mall:coupon:view")
    @GetMapping("/member-coupons")
    public String memberCoupons()
    {
        return prefix + "/member_coupon";
    }

    /** 会员持券列表数据 */
    @RequiresPermissions("mall:coupon:view")
    @PostMapping("/member-coupons/list")
    @ResponseBody
    public TableDataInfo memberCouponList(MallMemberCoupon query)
    {
        startPage();
        return getDataTable(couponService.listForAdmin(query));
    }

    /** 手动向指定会员发放一张券 */
    @RequiresPermissions("mall:coupon:add")
    @Log(title = "优惠券管理", businessType = BusinessType.INSERT)
    @PostMapping("/issue")
    @ResponseBody
    public AjaxResult issue(@RequestBody Map<String, Object> body)
    {
        Object couponIdObj = body.get("couponId");
        Object memberIdObj = body.get("memberId");
        if (couponIdObj == null || memberIdObj == null)
        {
            return AjaxResult.error("couponId and memberId are required");
        }
        try
        {
            couponService.issueCoupon(
                    Long.parseLong(couponIdObj.toString()),
                    Long.parseLong(memberIdObj.toString()));
            return success("Coupon issued successfully");
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }
}
