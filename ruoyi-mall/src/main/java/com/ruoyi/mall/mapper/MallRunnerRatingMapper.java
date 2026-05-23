package com.ruoyi.mall.mapper;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.mall.domain.MallRunnerRating;

public interface MallRunnerRatingMapper
{
    MallRunnerRating selectByOrderId(Long orderId);

    List<MallRunnerRating> selectByRunnerMemberId(Long runnerMemberId);

    int insert(MallRunnerRating rating);

    int countByRunnerMemberId(Long runnerMemberId);

    BigDecimal avgScoreByRunnerMemberId(Long runnerMemberId);
}
