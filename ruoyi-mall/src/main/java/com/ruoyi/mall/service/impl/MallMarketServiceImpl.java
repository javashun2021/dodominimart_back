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
    public List<MallMarketPost> listPosts(String category, String keyword) {
        return marketMapper.selectPostList(category, keyword);
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
    public List<MallMarketComment> listComments(Long postId) {
        return marketMapper.selectCommentsByPostId(postId);
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
}
