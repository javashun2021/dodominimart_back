package com.ruoyi.mall.service;

import java.util.List;
import com.ruoyi.mall.domain.MallMarketComment;
import com.ruoyi.mall.domain.MallMarketPost;
import com.ruoyi.mall.domain.MallMarketReport;

public interface IMallMarketService {
    List<MallMarketPost> listPosts(String category, String keyword);
    List<MallMarketPost> listMyPosts(Long memberId);
    List<MallMarketPost> listPendingPosts();
    MallMarketPost getPost(Long postId);
    MallMarketPost createPost(MallMarketPost post, Long memberId);
    void markSold(Long postId, Long memberId);
    void deletePost(Long postId, Long memberId);

    List<MallMarketComment> listComments(Long postId);
    MallMarketComment addComment(Long postId, Long memberId, String content);
    void deleteComment(Long commentId, Long memberId);

    // ── 举报 & 审核 ──────────────────────────────────────────────────────────
    void reportPost(Long postId, Long reporterId, String reason, String detail);
    void reportComment(Long commentId, Long reporterId, String reason, String detail);

    List<MallMarketReport> listPendingReports();
    void handleReport(Long reportId, String status, String handledBy, String note);

    void moderatePost(Long postId);
    void moderateComment(Long commentId);
    void banMember(Long memberId);
    void unbanMember(Long memberId);
    void approvePost(Long postId);
    void rejectPost(Long postId, String note);
}
