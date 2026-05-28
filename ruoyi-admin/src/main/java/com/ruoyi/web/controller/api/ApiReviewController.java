package com.ruoyi.web.controller.api;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.mall.domain.MallProductReview;
import com.ruoyi.mall.service.IMallReviewService;

@RestController
@RequestMapping("/api/v1")
public class ApiReviewController extends BaseApiController
{
    @Autowired
    private IMallReviewService reviewService;

    /**
     * POST /api/v1/orders/{orderId}/reviews
     * body: { "reviews": [ { "productId":1, "score":5, "content":"..." } ] }
     */
    @PostMapping("/orders/{orderId}/reviews")
    public AjaxResult submitReviews(@PathVariable Long orderId,
                                    @RequestBody Map<String, Object> body,
                                    HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> reviews =
                (List<Map<String, Object>>) body.get("reviews");
        if (reviews == null || reviews.isEmpty())
            return AjaxResult.error("reviews list is required");
        try
        {
            reviewService.submitReviews(orderId, memberId, reviews);
            return AjaxResult.success("Reviews submitted");
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * GET /api/v1/orders/{orderId}/reviewed-ids  — 已评价商品 ID 列表
     * 用于前端判断哪些商品已评价（JWT 可选，防止误操作即可）
     */
    @GetMapping("/orders/{orderId}/reviewed-ids")
    public AjaxResult reviewedIds(@PathVariable Long orderId,
                                  HttpServletRequest request)
    {
        // 仅登录用户可查自己的
        getCurrentMemberId(request);
        List<Long> ids = reviewService.getReviewedProductIds(orderId);
        return AjaxResult.success().put("data", ids);
    }

    /**
     * GET /api/v1/products/{productId}/reviews — 商品评价列表（公开）
     */
    @GetMapping("/products/{productId}/reviews")
    public AjaxResult productReviews(@PathVariable Long productId)
    {
        List<MallProductReview> reviews = reviewService.getProductReviews(productId);
        return AjaxResult.success().put("data", reviews);
    }
}
