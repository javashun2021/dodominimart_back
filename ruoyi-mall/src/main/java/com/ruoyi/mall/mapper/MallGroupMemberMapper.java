package com.ruoyi.mall.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.mall.domain.MallGroupMember;

public interface MallGroupMemberMapper
{
    List<MallGroupMember> selectMembersByGroupOrderId(Long groupOrderId);

    MallGroupMember selectMember(@Param("groupOrderId") Long groupOrderId, @Param("memberId") Long memberId);

    int insertMember(MallGroupMember member);

    /** 成团后回写 order_id */
    int updateMemberOrderId(@Param("id") Long id, @Param("orderId") Long orderId);
}
