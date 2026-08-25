package com.ruoyi.mall.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.mall.domain.MallOrder;
import com.ruoyi.mall.domain.MallProductReview;
import com.ruoyi.mall.mapper.MallOrderMapper;
import com.ruoyi.mall.mapper.MallProductReviewMapper;
import com.ruoyi.mall.service.IMallPointsService;
import com.ruoyi.mall.service.IMallReviewService;

@Service
public class MallReviewServiceImpl implements IMallReviewService
{
    @Autowired private MallProductReviewMapper reviewMapper;
    @Autowired private MallOrderMapper         orderMapper;
    @Autowired private IMallPointsService      pointsService;

    @Override
    @Transactional
    public void submitReviews(Long orderId, Long memberId, List<Map<String, Object>> reviews)
    {
        MallOrder order = orderMapper.selectOrderById(orderId);
        if (order == null)
            throw new RuntimeException("订单不存在");
        if (!memberId.equals(order.getMemberId()))
            throw new RuntimeException("该订单不属于你");
        if (!"3".equals(order.getStatus()))
            throw new RuntimeException("订单尚未送达");

        Date now = new Date();
        int newReviewCount = 0;
        for (Map<String, Object> r : reviews)
        {
            Long productId = Long.valueOf(r.get("productId").toString());
            if (reviewMapper.existsByOrderAndProduct(orderId, productId) > 0)
                continue;

            MallProductReview review = new MallProductReview();
            review.setOrderId(orderId);
            review.setProductId(productId);
            review.setMemberId(memberId);
            review.setScore(Integer.valueOf(r.getOrDefault("score", 5).toString()));
            review.setContent((String) r.getOrDefault("content", null));
            review.setImages((String) r.getOrDefault("images", null));
            review.setCreateTime(now);
            reviewMapper.insertReview(review);
            newReviewCount++;
        }

        // 每条新评价奖励 20 积分
        if (newReviewCount > 0)
        {
            try
            {
                pointsService.earn(memberId, newReviewCount * 20, 2,
                        String.valueOf(orderId), "Review bonus ×" + newReviewCount);
            }
            catch (Exception e)
            {
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("Points earn failed after review: {}", e.getMessage());
            }
        }
    }

    @Override
    public List<MallProductReview> getProductReviews(Long productId)
    {
        return reviewMapper.selectByProductId(productId);
    }

    @Override
    public List<Long> getReviewedProductIds(Long orderId)
    {
        return reviewMapper.selectReviewedProductIds(orderId);
    }

    @Override
    public List<MallProductReview> listAllReviews(MallProductReview query)
    {
        return reviewMapper.selectAllForAdmin(query);
    }

    @Override
    public int deleteReview(Long reviewId)
    {
        return reviewMapper.deleteReview(reviewId);
    }
}
