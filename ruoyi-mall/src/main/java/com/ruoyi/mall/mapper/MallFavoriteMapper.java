package com.ruoyi.mall.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.mall.domain.MallProduct;

public interface MallFavoriteMapper
{
    /** 收藏商品完整信息列表 */
    List<MallProduct> selectFavoriteProducts(@Param("memberId") Long memberId);

    /** 仅返回收藏的 productId 列表（用于批量判断心形状态） */
    List<Long> selectFavoriteProductIds(@Param("memberId") Long memberId);

    /** 是否已收藏：返回 0 或 1 */
    int isFavorited(@Param("memberId") Long memberId, @Param("productId") Long productId);

    int insertFavorite(@Param("memberId") Long memberId, @Param("productId") Long productId);

    int deleteFavorite(@Param("memberId") Long memberId, @Param("productId") Long productId);
}
