package com.ruoyi.mall.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.mall.domain.MallProductReview;

public interface MallProductReviewMapper
{
    int insertReview(MallProductReview review);

    List<MallProductReview> selectByProductId(@Param("productId") Long productId);

    /** 返回该订单已评价的 productId 列表 */
    List<Long> selectReviewedProductIds(@Param("orderId") Long orderId);

    int existsByOrderAndProduct(@Param("orderId") Long orderId,
                                @Param("productId") Long productId);

    /** 返回 Map: avgScore (Double), reviewCount (Long) */
    Map<String, Object> selectAvgScoreAndCount(@Param("productId") Long productId);

    List<MallProductReview> selectAllForAdmin(MallProductReview query);

    int deleteReview(@Param("reviewId") Long reviewId);
}
