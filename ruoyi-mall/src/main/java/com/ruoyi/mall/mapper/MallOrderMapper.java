package com.ruoyi.mall.mapper;

import java.util.List;
import com.ruoyi.mall.domain.MallOrder;

public interface MallOrderMapper
{
    List<MallOrder> selectOrderList(MallOrder order);

    MallOrder selectOrderById(Long orderId);

    MallOrder selectOrderByOrderNo(String orderNo);

    int insertOrder(MallOrder order);

    int updateOrder(MallOrder order);
}
