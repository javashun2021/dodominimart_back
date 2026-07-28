package com.ruoyi.web.controller.api;

import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.mall.domain.MallMerchant;
import com.ruoyi.mall.service.IMallMerchantService;

/**
 * 会员自助开店（需 JWT）
 * GET  /api/v1/merchant/apply  — 查询本人开店申请/店铺状态（无则 data=null）
 * POST /api/v1/merchant/apply  — 提交/更新开店申请（进后台待审核）
 *
 * 审核在后台「商家管理」进行；一期通过后店铺信息与商品由平台后台维护，
 * 商家自助工作台（商家端）留到二期。
 */
@RestController
@RequestMapping("/api/v1/merchant")
public class ApiMerchantApplyController extends BaseApiController
{
    @Autowired
    private IMallMerchantService merchantService;

    /** 本人开店申请/店铺状态；status: 0待审核 1营业 2拒绝 3停业，无记录则 data=null */
    @GetMapping("/apply")
    public AjaxResult getMyApplication(HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        return AjaxResult.success("ok").put("data", merchantService.getMyMerchant(memberId));
    }

    /** 提交/更新开店申请（body 为商家对象，name 必填，其余可选） */
    @PostMapping("/apply")
    public AjaxResult apply(@RequestBody MallMerchant draft, HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        try
        {
            MallMerchant saved = merchantService.applyMerchant(draft, memberId);
            return AjaxResult.success("Submitted for review").put("data", saved);
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }
}
