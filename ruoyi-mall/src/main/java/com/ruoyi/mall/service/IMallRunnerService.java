package com.ruoyi.mall.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.mall.domain.MallOrder;
import com.ruoyi.mall.domain.MallRunnerApplication;
import com.ruoyi.mall.domain.MallRunnerRating;

public interface IMallRunnerService
{
    // ---- App：申请相关 ----

    /** 查询本人申请记录 */
    MallRunnerApplication getApplication(Long memberId);

    /** 提交申请（已通过者不可重复提交） */
    void applyRunner(MallRunnerApplication app);

    // ---- App：接单/配送 ----

    /** 可接单的订单列表（status=1 且 runner_member_id IS NULL），仅已审核通过的 runner 可调用 */
    List<MallOrder> getAvailableOrders(Long memberId);

    /** 接单（我来送） */
    MallOrder acceptOrder(Long orderId, Long memberId);

    /** 确认送达 */
    MallOrder completeOrder(Long orderId, Long memberId);

    /** 我的配送历史 */
    List<MallOrder> getMyDeliveries(Long memberId);

    // ---- App：评价 ----

    /** 顾客评价跑腿人 */
    void rateRunner(MallRunnerRating rating);

    /** 跑腿人统计（公开） */
    Map<String, Object> getRunnerStats(Long runnerMemberId);

    /** 本人详细统计（含本周/本月，需登录） */
    Map<String, Object> getMyStats(Long memberId);

    /** 设置在线接单状态 */
    void setOnlineStatus(Long memberId, boolean online);

    // ---- Admin：审核 ----

    List<MallRunnerApplication> listApplications(MallRunnerApplication query);

    /** 审核通过，并指派归属门店（storeId 可空，空则不绑定门店=可接全部门店订单） */
    void approveApplication(Long appId, String reviewer, Long storeId);

    void rejectApplication(Long appId, String reviewer, String rejectReason);

    // ---- Admin：结算 ----

    /** 未结算的 GCash 跑腿订单，按 runner 分组 */
    List<Map<String, Object>> getUnsettledOrders(Long runnerMemberId);

    /** 批量标记已结算 */
    void settleRunnerFee(List<Long> orderIds);
}
