package com.ruoyi.web.controller.mall;

import java.util.List;
import java.util.Map;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.common.base.BaseController;
import com.ruoyi.common.utils.ShiroUtils;
import com.ruoyi.mall.domain.MallMarketPost;
import com.ruoyi.mall.domain.MallMarketReport;
import com.ruoyi.mall.service.IMallMarketService;

/**
 * 市场内容审核后台
 * /mall/market — 举报列表 & 处理
 */
@Controller
@RequestMapping("/mall/market")
public class MallMarketModerationController extends BaseController {

    @Autowired
    private IMallMarketService marketService;

    /** 待审帖子列表（人工审核主页） */
    @RequiresPermissions("mall:market:view")
    @GetMapping
    public String index(ModelMap mmap) {
        List<MallMarketPost> pendingPosts = marketService.listPendingPosts();
        List<MallMarketReport> reports = marketService.listPendingReports();
        mmap.put("pendingPosts", pendingPosts);
        mmap.put("reports", reports);
        return "mall/market/moderation";
    }

    /** 批准帖子 */
    @RequiresPermissions("mall:market:moderate")
    @PostMapping("/post/approve")
    @ResponseBody
    public AjaxResult approvePost(@RequestBody Map<String, Object> body) {
        Long postId = Long.valueOf(body.get("postId").toString());
        marketService.approvePost(postId);
        return AjaxResult.success();
    }

    /** 拒绝帖子 */
    @RequiresPermissions("mall:market:moderate")
    @PostMapping("/post/reject")
    @ResponseBody
    public AjaxResult rejectPost(@RequestBody Map<String, Object> body) {
        Long postId = Long.valueOf(body.get("postId").toString());
        String note = body.getOrDefault("note", "Does not meet community guidelines").toString();
        marketService.rejectPost(postId, note);
        return AjaxResult.success();
    }

    /** 下架帖子 */
    @RequiresPermissions("mall:market:moderate")
    @PostMapping("/post/moderate")
    @ResponseBody
    public AjaxResult moderatePost(@RequestBody Map<String, Object> body) {
        Long postId = Long.valueOf(body.get("postId").toString());
        marketService.moderatePost(postId);
        return AjaxResult.success();
    }

    /** 删除评论 */
    @RequiresPermissions("mall:market:moderate")
    @PostMapping("/comment/moderate")
    @ResponseBody
    public AjaxResult moderateComment(@RequestBody Map<String, Object> body) {
        Long commentId = Long.valueOf(body.get("commentId").toString());
        marketService.moderateComment(commentId);
        return AjaxResult.success();
    }

    /** 封禁用户 */
    @RequiresPermissions("mall:market:ban")
    @PostMapping("/member/ban")
    @ResponseBody
    public AjaxResult banMember(@RequestBody Map<String, Object> body) {
        Long memberId = Long.valueOf(body.get("memberId").toString());
        marketService.banMember(memberId);
        return AjaxResult.success();
    }

    /** 解封用户 */
    @RequiresPermissions("mall:market:ban")
    @PostMapping("/member/unban")
    @ResponseBody
    public AjaxResult unbanMember(@RequestBody Map<String, Object> body) {
        Long memberId = Long.valueOf(body.get("memberId").toString());
        marketService.unbanMember(memberId);
        return AjaxResult.success();
    }

    /** 处理举报（忽略或标记已处理） */
    @RequiresPermissions("mall:market:moderate")
    @PostMapping("/report/handle")
    @ResponseBody
    public AjaxResult handleReport(@RequestBody Map<String, Object> body) {
        Long reportId = Long.valueOf(body.get("reportId").toString());
        String status = body.getOrDefault("status", "1").toString();
        String note = body.getOrDefault("note", "").toString();
        String admin = ShiroUtils.getLoginName();
        marketService.handleReport(reportId, status, admin, note);
        return AjaxResult.success();
    }
}
