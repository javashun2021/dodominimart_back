package com.ruoyi.mall.mapper;

import java.util.List;
import com.ruoyi.mall.domain.MallOrderItem;

public interface MallOrderItemMapper
{
    List<MallOrderItem> selectItemsByOrderId(Long orderId);

    int insertOrderItem(MallOrderItem item);

    int insertOrderItemBatch(List<MallOrderItem> items);
}
