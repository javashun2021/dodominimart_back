package com.ruoyi.mall.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.mall.domain.MallPointsLog;

public interface MallPointsMapper
{
    /** 原子增减积分（delta 可为负），返回变动后余额 */
    int addPoints(@Param("memberId") Long memberId, @Param("delta") int delta);

    /** 查余额 */
    int selectBalance(@Param("memberId") Long memberId);

    /** 插入流水 */
    int insertLog(MallPointsLog log);

    /** 最近50条流水 */
    List<MallPointsLog> selectLogsByMemberId(@Param("memberId") Long memberId);

    List<MallPointsLog> selectAllLogs(@Param("memberId") Long memberId);
}
