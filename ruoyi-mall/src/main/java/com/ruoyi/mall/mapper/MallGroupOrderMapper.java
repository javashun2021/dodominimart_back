package com.ruoyi.mall.mapper;

import java.util.List;
import com.ruoyi.mall.domain.MallGroupOrder;

public interface MallGroupOrderMapper
{
    List<MallGroupOrder> selectGroupOrderList(MallGroupOrder query);

    MallGroupOrder selectGroupOrderById(Long groupOrderId);

    MallGroupOrder selectGroupOrderByInviteCode(String inviteCode);

    /** 查询会员参与的所有拼团 */
    List<MallGroupOrder> selectMyGroups(Long memberId);

    /** 查询已过期且仍为拼团中的记录（供定时任务调用） */
    List<MallGroupOrder> selectExpiredGroups();

    int insertGroupOrder(MallGroupOrder groupOrder);

    int updateGroupOrder(MallGroupOrder groupOrder);
}
