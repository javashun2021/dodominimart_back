package com.ruoyi.web.controller.api;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.mall.domain.MallMember;
import com.ruoyi.mall.domain.MallMerchant;
import com.ruoyi.mall.domain.MallProduct;
import com.ruoyi.mall.mapper.MallMemberMapper;
import com.ruoyi.mall.service.IMallMerchantService;
import com.ruoyi.mall.service.IMallProductService;

/**
 * 地推端接口（需 JWT，且会员 role=promoter）
 * GET  /api/v1/promoter/me                       — 我的推广码/QR + 业绩
 * POST /api/v1/promoter/merchants                — 代录商家（待审核）
 * GET  /api/v1/promoter/merchants                — 我录入的商家
 * POST /api/v1/promoter/merchants/{id}/products  — 给我录入的商家代录商品
 * GET  /api/v1/promoter/merchants/{id}/products  — 我录的该商家商品
 */
@RestController
@RequestMapping("/api/v1/promoter")
public class ApiPromoterController extends BaseApiController
{
    /** 推广码 QR 深链前缀：App 扫码/点击后带 referralCode 进注册 */
    private static final String REFERRAL_LINK_BASE = "https://dodominimart.com/invite?code=";

    @Autowired
    private MallMemberMapper memberMapper;

    @Autowired
    private IMallMerchantService merchantService;

    @Autowired
    private IMallProductService productService;

    @GetMapping("/me")
    public AjaxResult me(HttpServletRequest request)
    {
        MallMember me = requirePromoter(request);
        if (me == null) return AjaxResult.error(403, "Promoter access only");

        int referred = memberMapper.countReferredBy(me.getMemberId());
        int merchants = merchantService.countByPromoter(me.getMemberId());
        return AjaxResult.success("ok")
                .put("memberId", me.getMemberId())
                .put("nickName", me.getNickName())
                .put("inviteCode", me.getInviteCode())
                .put("referralLink", REFERRAL_LINK_BASE + (me.getInviteCode() == null ? "" : me.getInviteCode()))
                .put("referredCount", referred)
                .put("merchantCount", merchants);
    }

    /** 代录商家（进待审核） */
    @PostMapping("/merchants")
    public AjaxResult addMerchant(@RequestBody MallMerchant draft, HttpServletRequest request)
    {
        MallMember me = requirePromoter(request);
        if (me == null) return AjaxResult.error(403, "Promoter access only");
        if (draft == null || draft.getName() == null || draft.getName().trim().isEmpty())
        {
            return AjaxResult.error("Merchant name is required");
        }
        draft.setMerchantId(null);
        draft.setPromoterId(me.getMemberId());
        draft.setOwnerMemberId(null);
        draft.setStatus("0");        // 待审核
        draft.setDelFlag("0");
        draft.setCreateBy("promoter:" + me.getMemberId());
        merchantService.insertMerchant(draft);
        return AjaxResult.success("Submitted for review").put("merchantId", draft.getMerchantId());
    }

    /** 我录入的商家（含审核状态） */
    @GetMapping("/merchants")
    public AjaxResult myMerchants(HttpServletRequest request)
    {
        MallMember me = requirePromoter(request);
        if (me == null) return AjaxResult.error(403, "Promoter access only");
        return AjaxResult.success("ok").put("data", merchantService.selectByPromoter(me.getMemberId()));
    }

    /** 给我录入的商家代录商品 */
    @PostMapping("/merchants/{id}/products")
    public AjaxResult addProduct(@PathVariable("id") Long merchantId,
                                 @RequestBody MallProduct draft, HttpServletRequest request)
    {
        MallMember me = requirePromoter(request);
        if (me == null) return AjaxResult.error(403, "Promoter access only");
        MallMerchant merchant = merchantService.selectMerchantById(merchantId);
        if (merchant == null || !me.getMemberId().equals(merchant.getPromoterId()))
        {
            return AjaxResult.error("Merchant not found or not yours");
        }
        if (draft == null || draft.getName() == null || draft.getName().trim().isEmpty())
        {
            return AjaxResult.error("Product name is required");
        }
        draft.setProductId(null);
        draft.setMerchantId(merchantId);
        draft.setStatus("0");        // 上架
        draft.setDelFlag("0");
        draft.setCreateBy("promoter:" + me.getMemberId());
        draft.setCreateTime(new java.util.Date());
        productService.insertProduct(draft);
        return AjaxResult.success("ok").put("productId", draft.getProductId());
    }

    /** 我录的该商家商品 */
    @GetMapping("/merchants/{id}/products")
    public AjaxResult myProducts(@PathVariable("id") Long merchantId, HttpServletRequest request)
    {
        MallMember me = requirePromoter(request);
        if (me == null) return AjaxResult.error(403, "Promoter access only");
        MallMerchant merchant = merchantService.selectMerchantById(merchantId);
        if (merchant == null || !me.getMemberId().equals(merchant.getPromoterId()))
        {
            return AjaxResult.error("Merchant not found or not yours");
        }
        MallProduct query = new MallProduct();
        query.setMerchantId(merchantId);
        List<MallProduct> products = productService.selectProductList(query);
        return AjaxResult.success("ok").put("data", products);
    }

    /** 取当前会员并校验 role=promoter；不是则返回 null */
    private MallMember requirePromoter(HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        MallMember m = memberMapper.selectMemberById(memberId);
        if (m == null || !"promoter".equalsIgnoreCase(m.getRole()))
        {
            return null;
        }
        return m;
    }
}
