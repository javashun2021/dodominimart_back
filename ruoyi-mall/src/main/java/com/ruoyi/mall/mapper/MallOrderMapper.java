package com.ruoyi.mall.mapper;

import java.util.List;
import com.ruoyi.mall.domain.MallOrder;

public interface MallOrderMapper
{
    List<MallOrder> selectOrderList(MallOrder order);

    MallOrder selectOrderById(Long orderId);

    MallOrder selectOrderByOrderNo(String orderNo);

    /** 带行锁查询，用于支付回调幂等防重 */
    MallOrder selectOrderByOrderNoForUpdate(String orderNo);

    int insertOrder(MallOrder order);

    int updateOrder(MallOrder order);

    /** 聚合支付：回填商户单号 + 补贴 */
    int updateAggregateInfo(@org.apache.ibatis.annotations.Param("orderId") Long orderId,
                            @org.apache.ibatis.annotations.Param("merchantOutTradeNo") String merchantOutTradeNo,
                            @org.apache.ibatis.annotations.Param("subsidy") java.math.BigDecimal subsidy);

    /** POS：写入到店实付方式/收现/找零/收银员 */
    int updatePosTender(MallOrder order);

    /** 查询可被跑腿接单的订单（status=1 且 runner_member_id IS NULL）；storeId 非空则仅本店 */
    List<MallOrder> selectAvailableForRunner(@org.apache.ibatis.annotations.Param("storeId") Long storeId);

    /** 查询某 runner 的配送记录 */
    List<MallOrder> selectByRunnerMemberId(Long runnerMemberId);

    /** 统计某 runner 完成的配送数量 */
    int countByRunnerMemberId(Long runnerMemberId);

    /** 接单：写入 runner 信息并更新 status */
    int updateRunnerInfo(MallOrder order);

    /** 查询未结算的 GCash 跑腿订单（payment_method=GCASH, runner_member_id IS NOT NULL, runner_fee_settled=0, status=3） */
    List<MallOrder> selectUnsettledRunnerOrders(@org.apache.ibatis.annotations.Param("runnerMemberId") Long runnerMemberId);

    /** 批量标记跑腿费已结算 */
    int settleRunnerFee(List<Long> orderIds);

    int countDeliveriesThisWeek(Long runnerMemberId);
    int countDeliveriesThisMonth(Long runnerMemberId);
    java.math.BigDecimal sumEarningsThisWeek(Long runnerMemberId);
    java.math.BigDecimal sumEarningsThisMonth(Long runnerMemberId);

    /** 统计某会员已完成（status=3）的订单数 */
    int countCompletedByMemberId(Long memberId);

    /** 统计某会员历史订单总数（含已取消，防止取消后重新触发首单奖励） */
    int countAllByMemberId(Long memberId);

    // ── 外部导入单 ──

    /** 幂等去重：按外部订单号(存入 merchant_out_trade_no)查已导入的订单 */
    MallOrder selectOrderByMerchantOutTradeNo(String merchantOutTradeNo);

    /** 导入单模拟接单：写入 runner/接单时间/到达时间/状态 */
    int assignImportRunner(@org.apache.ibatis.annotations.Param("orderId") Long orderId,
                           @org.apache.ibatis.annotations.Param("runnerMemberId") Long runnerMemberId,
                           @org.apache.ibatis.annotations.Param("acceptedTime") java.util.Date acceptedTime,
                           @org.apache.ibatis.annotations.Param("arrivalTime") java.util.Date arrivalTime,
                           @org.apache.ibatis.annotations.Param("status") String status,
                           @org.apache.ibatis.annotations.Param("updateTime") java.util.Date updateTime);

    /** 扫描到点(arrival_time<=now)仍配送中的导入单，返回 orderId 列表 */
    List<Long> selectArrivedImports(@org.apache.ibatis.annotations.Param("limit") int limit);

    /** 到点完成：置 status=3、update_time=arrival_time */
    int completeArrived(@org.apache.ibatis.annotations.Param("orderId") Long orderId);
}
