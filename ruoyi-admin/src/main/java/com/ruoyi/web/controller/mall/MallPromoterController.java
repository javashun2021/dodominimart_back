package com.ruoyi.web.controller.mall;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.page.TableDataInfo;
import com.ruoyi.framework.web.base.BaseController;
import com.ruoyi.mall.domain.MallMember;
import com.ruoyi.mall.mapper.MallMemberMapper;
import com.ruoyi.mall.service.IMallMerchantService;

/**
 * 推广员管理后台
 * 推广员 = 会员 role='promoter'；其 invite_code 即专属推广码/QR。
 * 业绩：拉新数(referrer_id 计数) + 入驻数(merchant.promoter_id 计数)。
 */
@Controller
@RequestMapping("/mall/promoter")
public class MallPromoterController extends BaseController
{
    private static final String prefix = "mall/promoter";
    private static final String ROLE_PROMOTER = "promoter";

    @Autowired
    private MallMemberMapper memberMapper;

    @Autowired
    private IMallMerchantService merchantService;

    @RequiresPermissions("mall:promoter:view")
    @GetMapping
    public String promoter()
    {
        return prefix + "/promoter";
    }

    /** 推广员列表（含拉新数/入驻数业绩） */
    @RequiresPermissions("mall:promoter:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list()
    {
        List<MallMember> promoters = memberMapper.selectMembersByRole(ROLE_PROMOTER);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (MallMember m : promoters)
        {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("memberId",      m.getMemberId());
            row.put("nickName",      m.getNickName());
            row.put("phone",         m.getPhone());
            row.put("email",         m.getEmail());
            row.put("inviteCode",    m.getInviteCode());
            row.put("referredCount", memberMapper.countReferredBy(m.getMemberId()));
            row.put("merchantCount", merchantService.countByPromoter(m.getMemberId()));
            rows.add(row);
        }
        return getDataTable(rows);
    }

    /** 设为推广员：把某会员 role 置为 promoter */
    @RequiresPermissions("mall:promoter:assign")
    @Log(title = "设为推广员", businessType = BusinessType.UPDATE)
    @PostMapping("/assign")
    @ResponseBody
    public AjaxResult assign(@RequestBody Map<String, Object> body)
    {
        Long memberId = parseLong(body.get("memberId"));
        if (memberId == null)
        {
            return AjaxResult.error("memberId 不能为空");
        }
        MallMember m = memberMapper.selectMemberById(memberId);
        if (m == null)
        {
            return AjaxResult.error("会员不存在");
        }
        return toAjax(memberMapper.updateRole(memberId, ROLE_PROMOTER));
    }

    /** 取消推广员：role 置回 customer */
    @RequiresPermissions("mall:promoter:assign")
    @Log(title = "取消推广员", businessType = BusinessType.UPDATE)
    @PostMapping("/revoke")
    @ResponseBody
    public AjaxResult revoke(@RequestBody Map<String, Object> body)
    {
        Long memberId = parseLong(body.get("memberId"));
        if (memberId == null)
        {
            return AjaxResult.error("memberId 不能为空");
        }
        return toAjax(memberMapper.updateRole(memberId, "customer"));
    }

    private static Long parseLong(Object o)
    {
        if (o == null) return null;
        try { return Long.parseLong(o.toString().trim()); }
        catch (NumberFormatException e) { return null; }
    }
}
