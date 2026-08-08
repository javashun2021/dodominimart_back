package com.ruoyi.mall.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.mall.domain.PayOrder;

public interface PayOrderMapper
{
    int insertPayOrder(PayOrder order);

    PayOrder selectByPlatformNo(String platformNo);

    PayOrder selectByMerchantAndOutTradeNo(@Param("merchantCode") String merchantCode,
                                           @Param("outTradeNo") String outTradeNo);

    /** 标记支付成功（仅当当前非 PAID 时更新，返回受影响行数用于幂等判断） */
    int markPaid(@Param("platformNo") String platformNo,
                 @Param("upstreamNo") String upstreamNo,
                 @Param("upstreamRaw") String upstreamRaw);

    /** 更新回调结果 */
    int updateNotifyResult(@Param("platformNo") String platformNo,
                           @Param("notifyStatus") Integer notifyStatus);

    /** 待回调（已支付但未回调成功）的订单，用于定时补发 */
    List<PayOrder> selectPendingNotify(@Param("limit") int limit);
}
