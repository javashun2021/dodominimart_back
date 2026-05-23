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

    /** 查询可被跑腿接单的订单（status=1 且 runner_member_id IS NULL） */
    List<MallOrder> selectAvailableForRunner();

    /** 查询某 runner 的配送记录 */
    List<MallOrder> selectByRunnerMemberId(Long runnerMemberId);

    /** 接单：写入 runner 信息并更新 status */
    int updateRunnerInfo(MallOrder order);

    /** 查询未结算的 GCash 跑腿订单（payment_method=GCASH, runner_member_id IS NOT NULL, runner_fee_settled=0, status=3） */
    List<MallOrder> selectUnsettledRunnerOrders(@org.apache.ibatis.annotations.Param("runnerMemberId") Long runnerMemberId);

    /** 批量标记跑腿费已结算 */
    int settleRunnerFee(List<Long> orderIds);
}
