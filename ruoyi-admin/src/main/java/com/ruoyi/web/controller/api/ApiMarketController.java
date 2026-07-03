package com.ruoyi.web.controller.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.mall.domain.MallMarketComment;
import com.ruoyi.mall.domain.MallMarketPost;
import com.ruoyi.mall.service.IMallMarketService;
import com.ruoyi.mall.service.IMallMemberService;
import com.ruoyi.mall.domain.MallMember;

/**
 * C2C 市场接口
 * GET  /api/v1/market/posts                — 帖子列表（无需登录）
 * GET  /api/v1/market/posts/{id}           — 帖子详情+评论（无需登录）
 * POST /api/v1/market/posts                — 发帖（JWT）
 * PUT  /api/v1/market/posts/{id}/sold      — 标记已售（JWT，本人）
 * DELETE /api/v1/market/posts/{id}         — 删帖（JWT，本人）
 * GET  /api/v1/market/my-posts             — 我的发帖（JWT）
 * POST /api/v1/market/posts/{id}/comments  — 发评论（JWT）
 * DELETE /api/v1/market/comments/{id}      — 删评论（JWT，本人）
 */
@RestController
@RequestMapping("/api/v1/market")
public class ApiMarketController extends BaseApiController {

    @Autowired
    private IMallMarketService marketService;
    @Autowired
    private IMallMemberService memberService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private com.ruoyi.mall.service.TelegramNotifyService telegramNotifyService;

    /** 分类列表（公开） */
    @GetMapping("/categories")
    public AjaxResult listCategories() {
        List<String> names = jdbcTemplate.queryForList(
            "SELECT name FROM mall_market_category WHERE status=1 ORDER BY sort ASC",
            String.class);
        return AjaxResult.success().put("data", names);
    }

    /** 帖子列表（公开，只显示已批准） */
    @GetMapping("/posts")
    public AjaxResult listPosts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        List<MallMarketPost> posts = marketService.listPosts(category, keyword);
        return AjaxResult.success().put("data", posts);
    }

    /** 帖子详情 + 评论（公开） */
    @GetMapping("/posts/{id}")
    public AjaxResult getPost(@PathVariable("id") Long postId) {
        MallMarketPost post = marketService.getPost(postId);
        if (post == null) return AjaxResult.error("Post not found");
        List<MallMarketComment> comments = marketService.listComments(postId);
        Map<String, Object> data = new HashMap<>();
        data.put("post", post);
        data.put("comments", comments);
        return AjaxResult.success().put("data", data);
    }

    /** 发帖 */
    @PostMapping("/posts")
    public AjaxResult createPost(@RequestBody MallMarketPost post, HttpServletRequest request) {
        Long memberId = getCurrentMemberId(request);
        if (memberId == null) return AjaxResult.error("Please login first");
        MallMember member = memberService.selectMemberById(memberId);
        if (member != null && "1".equals(member.getStatus())) {
            return AjaxResult.error("Your account has been suspended. Please contact support.");
        }
        MallMarketPost created = marketService.createPost(post, memberId);
        // 新帖需人工审核：推一条纸飞机提醒到管理群（异步，失败不影响发帖）
        String who = (member != null && member.getNickName() != null)
                ? member.getNickName() : ("member#" + memberId);
        String priceText;
        if ("free".equals(created.getPriceType())) {
            priceText = "Free";
        } else if ("negotiable".equals(created.getPriceType())) {
            priceText = "Negotiable";
        } else {
            priceText = "₱" + (created.getPrice() != null ? created.getPrice().toPlainString() : "0");
        }
        telegramNotifyService.notify(
                "🆕 New Marketplace listing — pending review\n"
                + "Title: " + created.getTitle() + "\n"
                + "By: " + who + " (#" + memberId + ")\n"
                + "Category: " + (created.getCategory() != null ? created.getCategory() : "-") + "\n"
                + "Price: " + priceText + "\n"
                + "Please review & approve in the admin panel.");
        return AjaxResult.success().put("data", created);
    }

    /** 标记已售 */
    @PutMapping("/posts/{id}/sold")
    public AjaxResult markSold(@PathVariable("id") Long postId, HttpServletRequest request) {
        Long memberId = getCurrentMemberId(request);
        if (memberId == null) return AjaxResult.error("Please login first");
        marketService.markSold(postId, memberId);
        return AjaxResult.success();
    }

    /** 删帖 */
    @DeleteMapping("/posts/{id}")
    public AjaxResult deletePost(@PathVariable("id") Long postId, HttpServletRequest request) {
        Long memberId = getCurrentMemberId(request);
        if (memberId == null) return AjaxResult.error("Please login first");
        marketService.deletePost(postId, memberId);
        return AjaxResult.success();
    }

    /** 我的发帖（含待审/已拒绝） */
    @GetMapping("/my-posts")
    public AjaxResult myPosts(HttpServletRequest request) {
        Long memberId = getCurrentMemberId(request);
        if (memberId == null) return AjaxResult.error("Please login first");
        List<MallMarketPost> posts = marketService.listMyPosts(memberId);
        return AjaxResult.success().put("data", posts);
    }

    /** 切换收藏 */
    @PostMapping("/posts/{id}/favorite")
    public AjaxResult toggleFavorite(@PathVariable("id") Long postId, HttpServletRequest request) {
        Long memberId = getCurrentMemberId(request);
        if (memberId == null) return AjaxResult.error("Please login first");
        boolean now = marketService.toggleFavorite(memberId, postId);
        return AjaxResult.success().put("favorited", now);
    }

    /** 我收藏的 postId 列表 */
    @GetMapping("/favorite-ids")
    public AjaxResult favoriteIds(HttpServletRequest request) {
        Long memberId = getCurrentMemberId(request);
        if (memberId == null) return AjaxResult.error("Please login first");
        return AjaxResult.success().put("data", marketService.favoritePostIds(memberId));
    }

    /** 我的收藏列表（含已删除标识） */
    @GetMapping("/my-favorites")
    public AjaxResult myFavorites(HttpServletRequest request) {
        Long memberId = getCurrentMemberId(request);
        if (memberId == null) return AjaxResult.error("Please login first");
        return AjaxResult.success().put("data", marketService.myFavorites(memberId));
    }

    /** 发评论 */
    @PostMapping("/posts/{id}/comments")
    public AjaxResult addComment(@PathVariable("id") Long postId,
                                  @RequestBody Map<String, String> body,
                                  HttpServletRequest request) {
        Long memberId = getCurrentMemberId(request);
        if (memberId == null) return AjaxResult.error("Please login first");
        MallMember member = memberService.selectMemberById(memberId);
        if (member != null && "1".equals(member.getStatus())) {
            return AjaxResult.error("Your account has been suspended. Please contact support.");
        }
        String content = body.get("content");
        if (content == null || content.trim().isEmpty()) return AjaxResult.error("Content is required");
        MallMarketComment comment = marketService.addComment(postId, memberId, content.trim());
        return AjaxResult.success().put("data", comment);
    }

    /** 举报帖子 */
    @PostMapping("/posts/{id}/report")
    public AjaxResult reportPost(@PathVariable("id") Long postId,
                                  @RequestBody Map<String, String> body,
                                  HttpServletRequest request) {
        Long memberId = getCurrentMemberId(request);
        if (memberId == null) return AjaxResult.error("Please login first");
        String reason = body.getOrDefault("reason", "Other");
        String detail = body.get("detail");
        marketService.reportPost(postId, memberId, reason, detail);
        return AjaxResult.success("Report submitted. Our team will review it.");
    }

    /** 举报评论 */
    @PostMapping("/comments/{id}/report")
    public AjaxResult reportComment(@PathVariable("id") Long commentId,
                                     @RequestBody Map<String, String> body,
                                     HttpServletRequest request) {
        Long memberId = getCurrentMemberId(request);
        if (memberId == null) return AjaxResult.error("Please login first");
        String reason = body.getOrDefault("reason", "Other");
        String detail = body.get("detail");
        marketService.reportComment(commentId, memberId, reason, detail);
        return AjaxResult.success("Report submitted. Our team will review it.");
    }

    /** 删评论 */
    @DeleteMapping("/comments/{id}")
    public AjaxResult deleteComment(@PathVariable("id") Long commentId, HttpServletRequest request) {
        Long memberId = getCurrentMemberId(request);
        if (memberId == null) return AjaxResult.error("Please login first");
        marketService.deleteComment(commentId, memberId);
        return AjaxResult.success();
    }
}
