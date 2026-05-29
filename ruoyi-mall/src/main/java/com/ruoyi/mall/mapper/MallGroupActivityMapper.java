package com.ruoyi.mall.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.mall.domain.MallGroupActivity;
import com.ruoyi.mall.domain.MallGroupTier;

public interface MallGroupActivityMapper
{
    List<MallGroupActivity> selectActivityList(MallGroupActivity activity);

    MallGroupActivity selectActivityById(Long activityId);

    /** 查询商品当前进行中的活动（商品接口用） */
    MallGroupActivity selectActiveByProductId(Long productId);

    int insertActivity(MallGroupActivity activity);

    int updateActivity(MallGroupActivity activity);

    int deleteActivityById(Long activityId);

    // 阶梯管理
    List<MallGroupTier> selectTiersByActivityId(Long activityId);

    /** Batch fetch tiers for multiple activities to avoid N+1 */
    List<MallGroupTier> selectTiersByActivityIds(@Param("list") List<Long> activityIds);

    int insertTiers(List<MallGroupTier> tiers);

    int deleteTiersByActivityId(Long activityId);
}
