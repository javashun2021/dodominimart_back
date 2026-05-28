package com.ruoyi.mall.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.mall.domain.MallOrderItem;

public interface MallOrderItemMapper
{
    List<MallOrderItem> selectItemsByOrderId(Long orderId);

    List<MallOrderItem> selectItemsByOrderIds(@Param("orderIds") List<Long> orderIds);

    int insertOrderItem(MallOrderItem item);

    int insertOrderItemBatch(List<MallOrderItem> items);
}
