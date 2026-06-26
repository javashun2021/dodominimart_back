package com.ruoyi.web.controller.api;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.mall.domain.MallGroupActivity;
import com.ruoyi.mall.domain.MallGroupMember;
import com.ruoyi.mall.domain.MallGroupOrder;
import com.ruoyi.mall.service.IMallGroupService;

/**
 * 拼团 App 接口
 * GET  /api/v1/group-activities                  — 当前有效活动列表（公开）
 * POST /api/v1/group-orders                      — 发起拼团（需 JWT）
 * GET  /api/v1/group-orders/my                   — 我的拼团（需 JWT）
 * GET  /api/v1/group-orders/{invite_code}        — 团详情（公开，分享页）
 * POST /api/v1/group-orders/{invite_code}/join   — 加入拼团（需 JWT）
 */
@RestController
@RequestMapping("/api/v1")
public class ApiGroupController extends BaseApiController
{
    @Autowired
    private IMallGroupService groupService;

    @Autowired
    private com.ruoyi.mall.service.IMallMemberService memberService;

    @Autowired
    private com.ruoyi.mall.service.TelegramNotifyService telegramNotifyService;

    /** 拼团提醒文案（发起/加入），会员名取不到时用 #id 兜底 */
    private void notifyGroup(String action, Long memberId, com.ruoyi.mall.domain.MallGroupOrder go)
    {
        try
        {
            com.ruoyi.mall.domain.MallMember m = memberService.selectMemberById(memberId);
            String name = m != null ? m.getNickName() : ("#" + memberId);
            StringBuilder sb = new StringBuilder();
            sb.append("👥 ").append(action).append("\n会员：").append(name);
            if (go != null && go.getActivityTitle() != null && !go.getActivityTitle().isEmpty())
            {
                sb.append("\n活动：").append(go.getActivityTitle());
            }
            if (go != null && go.getInviteCode() != null)
            {
                sb.append("\n团号：").append(go.getInviteCode());
                if (go.getCurrentSize() != null)
                {
                    sb.append("（已 ").append(go.getCurrentSize()).append(" 人）");
                }
            }
            telegramNotifyService.notify(sb.toString());
        }
        catch (Exception ignored) {}
    }

    /**
     * 某活动下开放中的拼团单列表（公开，供 App 展示可加入的团）
     * GET /api/v1/group-orders?activityId=1
     */
    @GetMapping("/group-orders")
    public AjaxResult listOpenGroups(@RequestParam Long activityId)
    {
        MallGroupOrder query = new MallGroupOrder();
        query.setActivityId(activityId);
        query.setStatus("0");
        List<MallGroupOrder> list = groupService.selectGroupOrderList(query);
        for (MallGroupOrder go : list)
        {
            List<MallGroupMember> members = groupService.selectGroupMembers(go.getGroupOrderId());
            go.setMembers(members);
        }
        return AjaxResult.success().put("data", list);
    }

    /** 当前进行中的拼团活动列表（含价格阶梯） */
    @GetMapping("/group-activities")
    public AjaxResult getActiveActivities()
    {
        List<MallGroupActivity> list = groupService.getActiveActivities();
        return AjaxResult.success().put("data", list);
    }

    /**
     * 发起拼团
     * Body: { "activityId": 1, "quantity": 2, "addressId": 5 }
     */
    @PostMapping("/group-orders")
    public AjaxResult createGroup(@RequestBody Map<String, Object> body, HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        Object activityIdObj = body.get("activityId");
        if (activityIdObj == null)
        {
            return AjaxResult.error("activityId is required");
        }
        try
        {
            Long activityId = Long.valueOf(activityIdObj.toString());
            int  quantity   = Integer.parseInt(body.getOrDefault("quantity", 1).toString());
            Long addressId  = body.get("addressId") != null
                    ? Long.valueOf(body.get("addressId").toString()) : null;

            MallGroupOrder result = groupService.createGroup(activityId, memberId, quantity, addressId);
            notifyGroup("发起拼团", memberId, result);
            return AjaxResult.success("Group created").put("data", result);
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /** 我参与的拼团列表 */
    @GetMapping("/group-orders/my")
    public AjaxResult getMyGroups(HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        List<MallGroupOrder> list = groupService.getMyGroups(memberId);
        return AjaxResult.success().put("data", list);
    }

    /** 根据邀请码获取团详情（公开，用于分享页） */
    @GetMapping("/group-orders/{inviteCode}")
    public AjaxResult getGroupDetail(@PathVariable("inviteCode") String inviteCode)
    {
        MallGroupOrder detail = groupService.getGroupDetail(inviteCode);
        if (detail == null) return AjaxResult.error("Group not found");
        return AjaxResult.success().put("data", detail);
    }

    /**
     * 发起人提前结团（需 JWT）
     * 条件：调用者必须是发起人，且 current_size >= min_group_size
     */
    @PostMapping("/group-orders/{inviteCode}/close")
    public AjaxResult closeGroup(@PathVariable("inviteCode") String inviteCode,
                                 HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        try
        {
            MallGroupOrder result = groupService.closeGroup(inviteCode, memberId);
            return AjaxResult.success("Group closed successfully").put("data", result);
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 加入拼团
     * Body: { "quantity": 1, "addressId": 5 }
     */
    @PostMapping("/group-orders/{inviteCode}/join")
    public AjaxResult joinGroup(@PathVariable("inviteCode") String inviteCode,
                                @RequestBody Map<String, Object> body,
                                HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        try
        {
            int  quantity  = Integer.parseInt(body.getOrDefault("quantity", 1).toString());
            Long addressId = body.get("addressId") != null
                    ? Long.valueOf(body.get("addressId").toString()) : null;

            MallGroupOrder result = groupService.joinGroup(inviteCode, memberId, quantity, addressId);
            notifyGroup("加入拼团", memberId, result);
            return AjaxResult.success("Joined group").put("data", result);
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }
}
