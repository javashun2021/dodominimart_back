package com.ruoyi.mall.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.mall.domain.MallMarketComment;
import com.ruoyi.mall.domain.MallMarketPost;
import com.ruoyi.mall.domain.MallMarketReport;

public interface MallMarketMapper {

    List<MallMarketPost> selectPostList(@Param("category") String category,
                                        @Param("keyword") String keyword);

    List<MallMarketPost> selectMyPosts(@Param("memberId") Long memberId);

    MallMarketPost selectPostById(@Param("postId") Long postId);

    int insertPost(MallMarketPost post);

    int updatePost(MallMarketPost post);

    int deletePost(@Param("postId") Long postId, @Param("memberId") Long memberId);

    int incrementViewCount(@Param("postId") Long postId);

    int incrementCommentCount(@Param("postId") Long postId);

    int decrementCommentCount(@Param("postId") Long postId);

    List<MallMarketComment> selectCommentsByPostId(@Param("postId") Long postId);

    int insertComment(MallMarketComment comment);

    MallMarketComment selectCommentById(@Param("commentId") Long commentId);

    int deleteComment(@Param("commentId") Long commentId, @Param("memberId") Long memberId);

    // ── 审核 ──────────────────────────────────────────────────────────────────
    int insertReport(MallMarketReport report);

    int incrementReportCount(@Param("postId") Long postId);

    /** 管理员下架帖子 */
    int moderatePost(@Param("postId") Long postId);

    /** 批准帖子 */
    int approvePost(@Param("postId") Long postId);

    /** 拒绝帖子 */
    int rejectPost(@Param("postId") Long postId, @Param("note") String note);

    /** 待审帖子列表（管理员） */
    List<MallMarketPost> selectPendingPosts();

    /** 管理员删除评论 */
    int moderateComment(@Param("commentId") Long commentId);

    /** 封禁/解封会员 */
    int updateMemberStatus(@Param("memberId") Long memberId, @Param("status") String status);

    List<MallMarketReport> selectPendingReports();

    int handleReport(@Param("reportId") Long reportId,
                     @Param("status") String status,
                     @Param("handledBy") String handledBy,
                     @Param("handledNote") String handledNote);

    List<MallMarketPost>    selectAdminPostList(MallMarketPost query);
    List<MallMarketComment> selectAdminCommentList(MallMarketComment query);
    List<MallMarketReport>  selectAdminReportList(MallMarketReport query);

    // ── 收藏 ──────────────────────────────────────────────────────────────────
    int insertFavorite(@Param("memberId") Long memberId, @Param("postId") Long postId);
    int deleteFavorite(@Param("memberId") Long memberId, @Param("postId") Long postId);
    int isFavorited(@Param("memberId") Long memberId, @Param("postId") Long postId);
    List<Long> selectFavoritePostIds(@Param("memberId") Long memberId);
    /** 收藏列表；帖子已删除/下架的记录也返回，deleted=1 供前端标识 */
    List<MallMarketPost> selectMyFavorites(@Param("memberId") Long memberId);
}
