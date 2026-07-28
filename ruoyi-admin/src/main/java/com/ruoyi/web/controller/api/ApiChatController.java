package com.ruoyi.web.controller.api;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.github.pagehelper.PageInfo;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.mall.service.IMallChatService;

/**
 * App 站内聊天 REST（1:1 私聊）。所有接口需 JWT。
 * 实时收发走 WS /ws/chat；这里覆盖会话列表/历史/发消息(兜底)/已读/未读。
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ApiChatController extends BaseApiController
{
    @Autowired
    private IMallChatService chatService;

    /** 我的会话列表 */
    @GetMapping("/conversations")
    public AjaxResult conversations(@RequestParam(defaultValue = "1") int pageNum,
                                    @RequestParam(defaultValue = "20") int pageSize,
                                    HttpServletRequest request)
    {
        Long me = getCurrentMemberId(request);
        startPage(pageNum, pageSize);
        List<Map<String, Object>> list = chatService.listConversations(me);
        return pageResult(new PageInfo<>(list));
    }

    /** 取或建与某会员的会话（点「聊一聊」时调） */
    @PostMapping("/conversations")
    public AjaxResult openConversation(@RequestBody Map<String, Object> body, HttpServletRequest request)
    {
        Long me = getCurrentMemberId(request);
        Long targetMemberId = toLong(body.get("targetMemberId"));
        Long postId = toLong(body.get("postId"));
        if (targetMemberId == null) return AjaxResult.error("targetMemberId is required");
        try
        {
            Long conversationId = chatService.getOrCreateConversation(me, targetMemberId, postId);
            return AjaxResult.success().put("conversationId", conversationId);
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /** 历史消息（message_id 逆序翻页） */
    @GetMapping("/messages")
    public AjaxResult messages(@RequestParam Long conversationId,
                               @RequestParam(required = false) Long beforeId,
                               @RequestParam(defaultValue = "30") int pageSize,
                               HttpServletRequest request)
    {
        Long me = getCurrentMemberId(request);
        try
        {
            List<Map<String, Object>> list = chatService.history(me, conversationId, beforeId, pageSize);
            return AjaxResult.success().put("data", list);
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /** 发消息（WS 之外的兜底路径，与 WS send 同一收口） */
    @PostMapping("/messages")
    public AjaxResult send(@RequestBody Map<String, Object> body, HttpServletRequest request)
    {
        Long me = getCurrentMemberId(request);
        try
        {
            Map<String, Object> dto = chatService.sendMessage(
                    me,
                    toLong(body.get("conversationId")),
                    toLong(body.get("targetMemberId")),
                    toStr(body.get("contentType")),
                    toStr(body.get("text")),
                    toStr(body.get("imageUrl")),
                    toStr(body.get("sticker")),
                    toLong(body.get("refPostId")),
                    toStr(body.get("clientMsgId")));
            return AjaxResult.success().put("data", dto);
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /** 标记会话已读 */
    @PostMapping("/read")
    public AjaxResult read(@RequestBody Map<String, Object> body, HttpServletRequest request)
    {
        Long me = getCurrentMemberId(request);
        Long conversationId = toLong(body.get("conversationId"));
        if (conversationId == null) return AjaxResult.error("conversationId is required");
        try
        {
            chatService.markRead(me, conversationId);
            return AjaxResult.success();
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /** 全部未读总数（App 角标） */
    @GetMapping("/unread-total")
    public AjaxResult unreadTotal(HttpServletRequest request)
    {
        Long me = getCurrentMemberId(request);
        return AjaxResult.success().put("data", chatService.unreadTotal(me));
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private static Long toLong(Object v)
    {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString().trim()); }
        catch (Exception e) { return null; }
    }

    private static String toStr(Object v)
    {
        return v == null ? null : v.toString();
    }
}
