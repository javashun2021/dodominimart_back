package com.ruoyi.mall.service;

import java.util.List;
import com.ruoyi.mall.domain.MallMarketComment;
import com.ruoyi.mall.domain.MallMarketPost;
import com.ruoyi.mall.domain.MallMarketReport;

public interface IMallMarketService {
    List<MallMarketPost> listPosts(String category, String keyword, Long viewerId);
    List<MallMarketPost> listMyPosts(Long memberId);
    List<MallMarketPost> listPendingPosts();
    MallMarketPost getPost(Long postId);
    MallMarketPost createPost(MallMarketPost post, Long memberId);
    void markSold(Long postId, Long memberId);
    void deletePost(Long postId, Long memberId);

    List<MallMarketComment> listComments(Long postId, Long viewerId);
    MallMarketComment addComment(Long postId, Long memberId, String content);
    void deleteComment(Long commentId, Long memberId);

    // ── 拉黑（Apple/Play UGC 合规）────────────────────────────────────────────
    void blockUser(Long blockerId, Long blockedId);
    void unblockUser(Long blockerId, Long blockedId);
    List<java.util.Map<String, Object>> listBlockedMembers(Long blockerId);

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

    List<MallMarketPost>    listAdminPosts(MallMarketPost query);
    List<MallMarketComment> listAdminComments(MallMarketComment query);
    List<MallMarketReport>  listAdminReports(MallMarketReport query);

    // ── 外部同步（每日导入） ──────────────────────────────────────────────────
    /** 该外部帖是否已导入（去重） */
    boolean isExternalImported(String source, String externalId);
    /** 导入一条外部帖（自动审核通过，member_id/source/external_id 由 post 带入） */
    void importExternalPost(MallMarketPost post);

    // ── 收藏 ──────────────────────────────────────────────────────────────────
    /** 切换收藏，返回收藏后的状态（true=已收藏） */
    boolean toggleFavorite(Long memberId, Long postId);
    List<Long> favoritePostIds(Long memberId);
    List<MallMarketPost> myFavorites(Long memberId);
}
