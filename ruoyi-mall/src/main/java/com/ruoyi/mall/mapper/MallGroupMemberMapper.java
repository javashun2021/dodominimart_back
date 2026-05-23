package com.ruoyi.mall.mapper;

import java.util.List;
import com.ruoyi.mall.domain.MallGroupMember;

public interface MallGroupMemberMapper
{
    List<MallGroupMember> selectMembersByGroupOrderId(Long groupOrderId);

    MallGroupMember selectMember(Long groupOrderId, Long memberId);

    int insertMember(MallGroupMember member);

    /** 成团后回写 order_id */
    int updateMemberOrderId(Long id, Long orderId);
}
