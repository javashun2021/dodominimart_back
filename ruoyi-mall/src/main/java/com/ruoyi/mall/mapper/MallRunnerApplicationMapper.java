package com.ruoyi.mall.mapper;

import java.util.List;
import com.ruoyi.mall.domain.MallRunnerApplication;

public interface MallRunnerApplicationMapper
{
    MallRunnerApplication selectByMemberId(Long memberId);

    MallRunnerApplication selectById(Long appId);

    List<MallRunnerApplication> selectList(MallRunnerApplication query);

    int insertOrUpdate(MallRunnerApplication app);

    int updateStatus(MallRunnerApplication app);

    int updateOnlineStatus(@org.apache.ibatis.annotations.Param("memberId") Long memberId,
                           @org.apache.ibatis.annotations.Param("isOnline") String isOnline);
}
