package com.ruoyi.web.controller.mall;

import java.util.List;
import java.util.Map;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.common.page.TableDataInfo;
import com.ruoyi.framework.util.ShiroUtils;
import com.ruoyi.framework.web.base.BaseController;
import com.ruoyi.mall.domain.MallMarketComment;
import com.ruoyi.mall.domain.MallMarketPost;
import com.ruoyi.mall.domain.MallMarketReport;
import com.ruoyi.mall.service.IMallMarketService;

@Controller
@RequestMapping("/mall/market")
public class MallMarketModerationController extends BaseController {

    @Autowired
    private IMallMarketService marketService;

    // ── Post Management ───────────────────────────────────────────────────────

    @RequiresPermissions("mall:market:post:view")
    @GetMapping("/post")
    public String postPage() {
        return "mall/market/post";
    }

    @RequiresPermissions("mall:market:post:list")
    @PostMapping("/post/list")
    @ResponseBody
    public TableDataInfo postList(MallMarketPost query) {
        startPage();
        return getDataTable(marketService.listAdminPosts(query));
    }

    @RequiresPermissions("mall:market:post:moderate")
    @PostMapping("/post/approve")
    @ResponseBody
    public AjaxResult approvePost(@RequestBody Map<String, Object> body) {
        marketService.approvePost(Long.valueOf(body.get("postId").toString()));
        return AjaxResult.success();
    }

    @RequiresPermissions("mall:market:post:moderate")
    @PostMapping("/post/reject")
    @ResponseBody
    public AjaxResult rejectPost(@RequestBody Map<String, Object> body) {
        String note = body.getOrDefault("note", "Does not meet community guidelines").toString();
        marketService.rejectPost(Long.valueOf(body.get("postId").toString()), note);
        return AjaxResult.success();
    }

    @RequiresPermissions("mall:market:post:moderate")
    @PostMapping("/post/moderate")
    @ResponseBody
    public AjaxResult moderatePost(@RequestBody Map<String, Object> body) {
        marketService.moderatePost(Long.valueOf(body.get("postId").toString()));
        return AjaxResult.success();
    }

    // ── Comment Management ────────────────────────────────────────────────────

    @RequiresPermissions("mall:market:comment:view")
    @GetMapping("/comment")
    public String commentPage() {
        return "mall/market/comment";
    }

    @RequiresPermissions("mall:market:comment:list")
    @PostMapping("/comment/list")
    @ResponseBody
    public TableDataInfo commentList(MallMarketComment query) {
        startPage();
        return getDataTable(marketService.listAdminComments(query));
    }

    @RequiresPermissions("mall:market:comment:moderate")
    @PostMapping("/comment/moderate")
    @ResponseBody
    public AjaxResult moderateComment(@RequestBody Map<String, Object> body) {
        marketService.moderateComment(Long.valueOf(body.get("commentId").toString()));
        return AjaxResult.success();
    }

    // ── Report Management ─────────────────────────────────────────────────────

    @RequiresPermissions("mall:market:report:view")
    @GetMapping("/report")
    public String reportPage() {
        return "mall/market/report";
    }

    @RequiresPermissions("mall:market:report:list")
    @PostMapping("/report/list")
    @ResponseBody
    public TableDataInfo reportList(MallMarketReport query) {
        startPage();
        return getDataTable(marketService.listAdminReports(query));
    }

    @RequiresPermissions("mall:market:report:handle")
    @PostMapping("/report/handle")
    @ResponseBody
    public AjaxResult handleReport(@RequestBody Map<String, Object> body) {
        marketService.handleReport(
            Long.valueOf(body.get("reportId").toString()),
            body.getOrDefault("status", "1").toString(),
            ShiroUtils.getLoginName(),
            body.getOrDefault("note", "").toString()
        );
        return AjaxResult.success();
    }

    // ── Member ban/unban (shared) ─────────────────────────────────────────────

    @RequiresPermissions("mall:market:post:moderate")
    @PostMapping("/member/ban")
    @ResponseBody
    public AjaxResult banMember(@RequestBody Map<String, Object> body) {
        marketService.banMember(Long.valueOf(body.get("memberId").toString()));
        return AjaxResult.success();
    }

    @RequiresPermissions("mall:market:post:moderate")
    @PostMapping("/member/unban")
    @ResponseBody
    public AjaxResult unbanMember(@RequestBody Map<String, Object> body) {
        marketService.unbanMember(Long.valueOf(body.get("memberId").toString()));
        return AjaxResult.success();
    }
}
