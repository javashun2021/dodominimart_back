package com.ruoyi.web.controller.api;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.mall.domain.MallOrder;
import com.ruoyi.mall.domain.MallRunnerApplication;
import com.ruoyi.mall.domain.MallRunnerRating;
import com.ruoyi.mall.mapper.MallMemberMapper;
import com.ruoyi.mall.service.FcmService;
import com.ruoyi.mall.service.IMallRunnerService;

/**
 * 跑腿 App 接口
 * GET  /api/v1/runner/application              — 查询本人申请状态
 * POST /api/v1/runner/application              — 提交跑腿申请
 * GET  /api/v1/runner/available-orders         — 可接单列表（已审核 runner）
 * POST /api/v1/runner/orders/{id}/accept       — 我来送（接单）
 * POST /api/v1/runner/orders/{id}/complete     — 确认送达
 * GET  /api/v1/runner/my-deliveries            — 我的配送历史
 * POST /api/v1/orders/{id}/rate-runner         — 顾客评价跑腿人（在 ApiOrderController 中）
 * GET  /api/v1/runner/stats/{memberId}         — 跑腿人统计（公开）
 */
@RestController
@RequestMapping("/api/v1")
public class ApiRunnerController extends BaseApiController
{
    @Autowired
    private IMallRunnerService runnerService;

    @Autowired
    private FcmService fcmService;

    @Autowired
    private MallMemberMapper memberMapper;

    /** 查询本人申请状态 */
    @GetMapping("/runner/application")
    public AjaxResult getApplication(HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        MallRunnerApplication app = runnerService.getApplication(memberId);
        return AjaxResult.success().put("data", app);
    }

    /** 提交跑腿申请 */
    @PostMapping("/runner/application")
    public AjaxResult applyRunner(@RequestBody MallRunnerApplication app, HttpServletRequest request)
    {
        app.setMemberId(getCurrentMemberId(request));
        try
        {
            runnerService.applyRunner(app);
            return AjaxResult.success("Application submitted").put("data", runnerService.getApplication(app.getMemberId()));
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /** 可接单列表（仅已审核通过的 runner） */
    @GetMapping("/runner/available-orders")
    public AjaxResult getAvailableOrders(HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        try
        {
            List<MallOrder> list = runnerService.getAvailableOrders(memberId);
            return AjaxResult.success().put("data", list);
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /** 我来送（接单） */
    @PostMapping("/runner/orders/{orderId}/accept")
    public AjaxResult acceptOrder(@PathVariable Long orderId, HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        try
        {
            MallOrder order = runnerService.acceptOrder(orderId, memberId);
            return AjaxResult.success("Order accepted").put("data", order);
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /** 确认送达 */
    @PostMapping("/runner/orders/{orderId}/complete")
    public AjaxResult completeOrder(@PathVariable Long orderId, HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        try
        {
            MallOrder order = runnerService.completeOrder(orderId, memberId);
            return AjaxResult.success("Order completed").put("data", order);
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /** 我的配送历史 */
    @GetMapping("/runner/my-deliveries")
    public AjaxResult getMyDeliveries(HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        List<MallOrder> list = runnerService.getMyDeliveries(memberId);
        return AjaxResult.success().put("data", list);
    }

    /** 跑腿人统计（公开，无需登录） */
    @GetMapping("/runner/stats/{memberId}")
    public AjaxResult getRunnerStats(@PathVariable Long memberId)
    {
        Map<String, Object> stats = runnerService.getRunnerStats(memberId);
        return AjaxResult.success().put("data", stats);
    }

    /** 本人详细统计（含本周/本月完单 + 收益，需 JWT） */
    @GetMapping("/runner/my-stats")
    public AjaxResult getMyStats(HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        try
        {
            Map<String, Object> stats = runnerService.getMyStats(memberId);
            return AjaxResult.success().put("data", stats);
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 设置在线/离线状态（需 JWT，仅已审核 runner 可调用）
     * Body: { "isOnline": true, "fcmToken": "..." }
     */
    @PutMapping("/runner/online-status")
    public AjaxResult setOnlineStatus(@RequestBody Map<String, Object> body, HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        boolean online = Boolean.TRUE.equals(body.get("isOnline"));
        String fcmToken = (String) body.get("fcmToken");
        try
        {
            runnerService.setOnlineStatus(memberId, online);

            // 若携带 fcmToken，顺便更新存储并订阅/取消订阅 Topic
            if (fcmToken != null && !fcmToken.isEmpty())
            {
                memberMapper.updateFcmToken(memberId, fcmToken);
                if (online)
                {
                    fcmService.subscribeToTopic(fcmToken, "runners");
                }
                else
                {
                    fcmService.unsubscribeFromTopic(fcmToken, "runners");
                }
            }
            else
            {
                // 没传 token 时，尝试用已存储的 token 操作 Topic
                com.ruoyi.mall.domain.MallMember member = memberMapper.selectMemberById(memberId);
                if (member != null && member.getFcmToken() != null)
                {
                    if (online)
                    {
                        fcmService.subscribeToTopic(member.getFcmToken(), "runners");
                    }
                    else
                    {
                        fcmService.unsubscribeFromTopic(member.getFcmToken(), "runners");
                    }
                }
            }

            return AjaxResult.success(online ? "You are now online" : "You are now offline");
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }
}
