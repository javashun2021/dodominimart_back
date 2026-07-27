package com.ruoyi.mall.service;

import java.util.List;
import com.ruoyi.mall.domain.MallPointsLog;

public interface IMallPointsService
{
    /** 积分功能是否开启（平台开关，关闭时 earn/deduct 均 no-op）。 */
    boolean isEnabled();

    /** 获得积分（source: 1=订单 2=评价 3=注册） */
    void earn(Long memberId, int delta, int source, String sourceId, String remark);

    /**
     * 消耗积分（source=4）
     * @return 实际扣除积分数（余额不足时返回 0，不抛异常）
     */
    int deduct(Long memberId, int points, String sourceId);

    int getBalance(Long memberId);

    List<MallPointsLog> getHistory(Long memberId);

    List<MallPointsLog> listAllLogs(Long memberId);
}
