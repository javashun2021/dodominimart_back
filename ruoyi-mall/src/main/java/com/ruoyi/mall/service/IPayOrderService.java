package com.ruoyi.mall.service;

import java.util.List;
import com.ruoyi.mall.domain.PayOrder;

/**
 * 聚合支付订单查询服务（后台）。
 */
public interface IPayOrderService
{
    List<PayOrder> selectOrderList(PayOrder query);

    PayOrder selectByPlatformNo(String platformNo);
}
