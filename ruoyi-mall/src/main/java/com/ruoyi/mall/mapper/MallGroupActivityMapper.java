package com.ruoyi.mall.mapper;

import java.util.List;
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

    int insertTiers(List<MallGroupTier> tiers);

    int deleteTiersByActivityId(Long activityId);
}
