package com.ruoyi.mall.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.mall.domain.MallProductReview;

public interface IMallReviewService
{
    /**
     * 批量提交评价。
     * @param orderId   订单ID
     * @param memberId  当前会员ID
     * @param reviews   评价列表（每项含 productId / score / content / images）
     */
    void submitReviews(Long orderId, Long memberId, List<Map<String, Object>> reviews);

    List<MallProductReview> getProductReviews(Long productId);

    /** 返回该订单已评价的 productId 列表 */
    List<Long> getReviewedProductIds(Long orderId);

    List<MallProductReview> listAllReviews(MallProductReview query);

    int deleteReview(Long reviewId);
}
