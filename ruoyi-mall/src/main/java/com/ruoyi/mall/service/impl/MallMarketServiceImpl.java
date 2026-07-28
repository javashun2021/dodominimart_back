package com.ruoyi.mall.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.mall.domain.MallMarketComment;
import com.ruoyi.mall.domain.MallMarketPost;
import com.ruoyi.mall.domain.MallMarketReport;
import com.ruoyi.mall.mapper.MallMarketMapper;
import com.ruoyi.mall.service.IMallMarketService;

@Service
public class MallMarketServiceImpl implements IMallMarketService {

    @Autowired
    private MallMarketMapper marketMapper;

    @Override
    public List<MallMarketPost> listPosts(String category, String keyword, Long viewerId) {
        return marketMapper.selectPostList(category, keyword, viewerId);
    }

    @Override
    public List<MallMarketPost> listMyPosts(Long memberId) {
        return marketMapper.selectMyPosts(memberId);
    }

    @Override
    public List<MallMarketPost> listPendingPosts() {
        return marketMapper.selectPendingPosts();
    }

    @Override
    public MallMarketPost getPost(Long postId) {
        MallMarketPost post = marketMapper.selectPostById(postId);
        if (post != null) {
            marketMapper.incrementViewCount(postId);
        }
        return post;
    }

    @Override
    public MallMarketPost createPost(MallMarketPost post, Long memberId) {
        post.setMemberId(memberId);
        marketMapper.insertPost(post);
        return marketMapper.selectPostById(post.getPostId());
    }

    @Override
    public void markSold(Long postId, Long memberId) {
        MallMarketPost post = new MallMarketPost();
        post.setPostId(postId);
        post.setMemberId(memberId);
        post.setStatus("1");
        marketMapper.updatePost(post);
    }

    @Override
    public void deletePost(Long postId, Long memberId) {
        marketMapper.deletePost(postId, memberId);
    }

    @Override
    public List<MallMarketComment> listComments(Long postId, Long viewerId) {
        return marketMapper.selectCommentsByPostId(postId, viewerId);
    }

    @Override
    public void blockUser(Long blockerId, Long blockedId) {
        if (blockerId == null || blockedId == null || blockerId.equals(blockedId)) {
            return; // 不能拉黑自己
        }
        marketMapper.insertBlock(blockerId, blockedId);
    }

    @Override
    public void unblockUser(Long blockerId, Long blockedId) {
        marketMapper.deleteBlock(blockerId, blockedId);
    }

    @Override
    public List<java.util.Map<String, Object>> listBlockedMembers(Long blockerId) {
        return marketMapper.selectBlockedMembers(blockerId);
    }

    @Override
    public boolean isBlocked(Long a, Long b) {
        if (a == null || b == null) return false;
        return marketMapper.countBlockBetween(a, b) > 0;
    }

    @Override
    public MallMarketComment addComment(Long postId, Long memberId, String content) {
        MallMarketComment comment = new MallMarketComment();
        comment.setPostId(postId);
        comment.setMemberId(memberId);
        comment.setContent(content);
        marketMapper.insertComment(comment);
        marketMapper.incrementCommentCount(postId);
        return marketMapper.selectCommentById(comment.getCommentId());
    }

    @Override
    public void deleteComment(Long commentId, Long memberId) {
        MallMarketComment comment = marketMapper.selectCommentById(commentId);
        if (comment != null) {
            marketMapper.deleteComment(commentId, memberId);
            marketMapper.decrementCommentCount(comment.getPostId());
        }
    }

    @Override
    public void reportPost(Long postId, Long reporterId, String reason, String detail) {
        MallMarketReport r = new MallMarketReport();
        r.setTargetType("post");
        r.setTargetId(postId);
        r.setReporterId(reporterId);
        r.setReason(reason);
        r.setDetail(detail);
        marketMapper.insertReport(r);
        marketMapper.incrementReportCount(postId);
    }

    @Override
    public void reportComment(Long commentId, Long reporterId, String reason, String detail) {
        MallMarketReport r = new MallMarketReport();
        r.setTargetType("comment");
        r.setTargetId(commentId);
        r.setReporterId(reporterId);
        r.setReason(reason);
        r.setDetail(detail);
        marketMapper.insertReport(r);
    }

    @Override
    public List<MallMarketReport> listPendingReports() {
        return marketMapper.selectPendingReports();
    }

    @Override
    public void handleReport(Long reportId, String status, String handledBy, String note) {
        marketMapper.handleReport(reportId, status, handledBy, note);
    }

    @Override
    public void moderatePost(Long postId) {
        marketMapper.moderatePost(postId);
    }

    @Override
    public void moderateComment(Long commentId) {
        marketMapper.moderateComment(commentId);
    }

    @Override
    public void banMember(Long memberId) {
        marketMapper.updateMemberStatus(memberId, "1");
    }

    @Override
    public void unbanMember(Long memberId) {
        marketMapper.updateMemberStatus(memberId, "0");
    }

    @Override
    public void approvePost(Long postId) {
        marketMapper.approvePost(postId);
    }

    @Override
    public void rejectPost(Long postId, String note) {
        marketMapper.rejectPost(postId, note);
    }

    @Override
    public List<MallMarketPost> listAdminPosts(MallMarketPost query) {
        return marketMapper.selectAdminPostList(query);
    }

    @Override
    public List<MallMarketComment> listAdminComments(MallMarketComment query) {
        return marketMapper.selectAdminCommentList(query);
    }

    @Override
    public List<MallMarketReport> listAdminReports(MallMarketReport query) {
        return marketMapper.selectAdminReportList(query);
    }

    @Override
    public boolean isExternalImported(String source, String externalId) {
        return marketMapper.countByExternalId(source, externalId) > 0;
    }

    @Override
    public void importExternalPost(MallMarketPost post) {
        marketMapper.insertImportedPost(post);
    }

    @Override
    public boolean toggleFavorite(Long memberId, Long postId) {
        if (marketMapper.isFavorited(memberId, postId) > 0) {
            marketMapper.deleteFavorite(memberId, postId);
            return false;
        } else {
            marketMapper.insertFavorite(memberId, postId);
            return true;
        }
    }

    @Override
    public List<Long> favoritePostIds(Long memberId) {
        return marketMapper.selectFavoritePostIds(memberId);
    }

    @Override
    public List<MallMarketPost> myFavorites(Long memberId) {
        return marketMapper.selectMyFavorites(memberId);
    }
}
