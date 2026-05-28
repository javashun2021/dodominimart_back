package com.ruoyi.web.controller.api;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.mall.domain.MallProduct;
import com.ruoyi.mall.service.IMallFavoriteService;

@RestController
@RequestMapping("/api/v1/favorites")
public class ApiFavoriteController extends BaseApiController
{
    @Autowired
    private IMallFavoriteService favoriteService;

    /** GET /api/v1/favorites — 收藏商品列表 */
    @GetMapping
    public AjaxResult list(HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        List<MallProduct> products = favoriteService.getFavoriteProducts(memberId);
        return AjaxResult.success().put("data", products);
    }

    /** GET /api/v1/favorites/ids — 仅返回 productId 数组 */
    @GetMapping("/ids")
    public AjaxResult ids(HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        List<Long> ids = favoriteService.getFavoriteProductIds(memberId);
        return AjaxResult.success().put("data", ids);
    }

    /** POST /api/v1/favorites/{productId} — 收藏 */
    @PostMapping("/{productId}")
    public AjaxResult add(@PathVariable Long productId, HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        boolean now = favoriteService.toggle(memberId, productId);
        return AjaxResult.success().put("favorited", now);
    }

    /** DELETE /api/v1/favorites/{productId} — 取消收藏 */
    @DeleteMapping("/{productId}")
    public AjaxResult remove(@PathVariable Long productId, HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        favoriteService.toggle(memberId, productId);
        return AjaxResult.success();
    }
}
