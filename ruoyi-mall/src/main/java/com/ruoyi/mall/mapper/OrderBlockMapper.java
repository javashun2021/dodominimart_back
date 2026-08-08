package com.ruoyi.mall.mapper;

import org.apache.ibatis.annotations.Param;

public interface OrderBlockMapper
{
    /**
     * 命中拉黑数（按 会员id / ip / 支付宝pid 任一维度匹配，命中即拒单）。
     * 任一参数为空则该维度不参与匹配。
     */
    int countBlocked(@Param("userId") String userId,
                     @Param("clientIp") String clientIp,
                     @Param("buyerId") String buyerId);
}
