package com.ruoyi.web.controller.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.mall.domain.MallMember;
import com.ruoyi.mall.mapper.MallMemberMapper;

/**
 * 邀请记录接口
 * GET /api/v1/referrals — 返回当前会员邀请到的所有人
 */
@RestController
@RequestMapping("/api/v1/referrals")
public class ApiReferralController extends BaseApiController
{
    @Autowired
    private MallMemberMapper memberMapper;

    @GetMapping
    public AjaxResult getReferrals(HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);

        // 查自己的邀请码
        MallMember self = memberMapper.selectMemberById(memberId);
        String inviteCode = self != null ? self.getInviteCode() : null;

        // 查被邀请的人列表
        List<MallMember> referred = memberMapper.selectReferredMembers(memberId);

        List<Map<String, Object>> list = new ArrayList<>();
        for (MallMember m : referred)
        {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("memberId",            m.getMemberId());
            item.put("nickName",            m.getNickName());
            item.put("avatarUrl",           m.getAvatarUrl());
            item.put("joinedAt",            m.getCreateTime());
            item.put("completedOrderCount", m.getCompletedOrderCount());
            list.add(item);
        }

        return AjaxResult.success("ok")
                .put("inviteCode", inviteCode)
                .put("totalInvited", list.size())
                .put("list", list);
    }
}
