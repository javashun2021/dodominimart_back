package com.ruoyi.mall.service;

import java.util.List;
import com.ruoyi.mall.domain.MallProduct;

public interface IMallFavoriteService
{
    List<MallProduct> getFavoriteProducts(Long memberId);
    List<Long>        getFavoriteProductIds(Long memberId);
    boolean           isFavorited(Long memberId, Long productId);
    /** 切换收藏状态，返回操作后的状态：true=已收藏 false=已取消 */
    boolean           toggle(Long memberId, Long productId);
}
