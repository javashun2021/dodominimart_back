package com.ruoyi.mall.service;

import java.util.List;
import com.ruoyi.mall.domain.MallGroupActivity;
import com.ruoyi.mall.domain.MallGroupOrder;

public interface IMallGroupService
{
    // ---- 后台管理 CRUD ----
    List<MallGroupActivity> selectActivityList(MallGroupActivity query);
    MallGroupActivity selectActivityById(Long activityId);
    int insertActivity(MallGroupActivity activity);
    int updateActivity(MallGroupActivity activity);
    int deleteActivityById(Long activityId);

    // ---- App 接口 ----

    /** 查询当前进行中的拼团活动（含价格阶梯） */
    List<MallGroupActivity> getActiveActivities();

    /** 发起拼团 */
    MallGroupOrder createGroup(Long activityId, Long memberId, int quantity, Long addressId);

    /** 根据邀请码获取团详情（含成员列表、活动信息） */
    MallGroupOrder getGroupDetail(String inviteCode);

    /** 加入拼团 */
    MallGroupOrder joinGroup(String inviteCode, Long memberId, int quantity, Long addressId);

    /** 查询我参与的拼团 */
    List<MallGroupOrder> getMyGroups(Long memberId);

    /** 定时任务：扫描过期未成团 → 标记失败 */
    void expireGroups();
}
