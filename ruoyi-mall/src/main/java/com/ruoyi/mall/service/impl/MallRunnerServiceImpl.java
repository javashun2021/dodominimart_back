package com.ruoyi.mall.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.mall.domain.MallMember;
import com.ruoyi.mall.domain.MallOrder;
import com.ruoyi.mall.domain.MallRunnerApplication;
import com.ruoyi.mall.domain.MallRunnerRating;
import com.ruoyi.mall.mapper.MallMemberMapper;
import com.ruoyi.mall.mapper.MallOrderMapper;
import com.ruoyi.mall.mapper.MallRunnerApplicationMapper;
import com.ruoyi.mall.mapper.MallRunnerRatingMapper;
import com.ruoyi.mall.service.FcmService;
import com.ruoyi.mall.service.IMallPointsService;
import com.ruoyi.mall.service.IMallRunnerService;
import com.ruoyi.mall.service.ISsePushService;

@Service
public class MallRunnerServiceImpl implements IMallRunnerService
{
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("20");

    @Autowired private MallRunnerApplicationMapper appMapper;
    @Autowired private MallRunnerRatingMapper       ratingMapper;
    @Autowired private MallOrderMapper              orderMapper;
    @Autowired private MallMemberMapper             memberMapper;
    @Autowired private FcmService                   fcmService;
    @Autowired private ISsePushService              sseService;
    @Autowired private IMallPointsService           pointsService;

    // ---- 申请相关 ----

    @Override
    public MallRunnerApplication getApplication(Long memberId)
    {
        return appMapper.selectByMemberId(memberId);
    }

    @Override
    public void applyRunner(MallRunnerApplication app)
    {
        MallRunnerApplication existing = appMapper.selectByMemberId(app.getMemberId());
        if (existing != null && "1".equals(existing.getStatus()))
        {
            throw new RuntimeException("Already an approved runner");
        }
        app.setApplyTime(new Date());
        appMapper.insertOrUpdate(app);
    }

    // ---- 接单/配送 ----

    @Override
    public List<MallOrder> getAvailableOrders(Long memberId)
    {
        requireApprovedRunner(memberId);
        MallRunnerApplication app = appMapper.selectByMemberId(memberId);
        if (!"1".equals(app.getIsOnline()))
        {
            return java.util.Collections.emptyList();
        }
        return orderMapper.selectAvailableForRunner();
    }

    @Override
    @Transactional
    public MallOrder acceptOrder(Long orderId, Long memberId)
    {
        requireApprovedRunner(memberId);

        MallOrder order = orderMapper.selectOrderById(orderId);
        if (order == null)
            throw new RuntimeException("Order not found");
        if (!"1".equals(order.getStatus()))
            throw new RuntimeException("Order is not available for delivery");
        if (order.getRunnerMemberId() != null)
            throw new RuntimeException("Order already taken by another runner");
        if (memberId.equals(order.getMemberId()))
            throw new RuntimeException("Cannot deliver your own order");

        order.setRunnerMemberId(memberId);
        order.setRunnerAcceptedTime(new Date());
        order.setDeliveryFee(DELIVERY_FEE);
        order.setStatus("2");
        int affected = orderMapper.updateRunnerInfo(order);
        if (affected == 0)
        {
            throw new RuntimeException("Order already taken by another runner");
        }
        MallOrder accepted = orderMapper.selectOrderById(orderId);

        // 推送：SSE（Web）+ FCM（Mobile）
        try
        {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("orderId", orderId);
            payload.put("status", "2");
            payload.put("title", "Runner On the Way");
            payload.put("body",  "Your order is being delivered");
            sseService.push(accepted.getMemberId(), "order_status", payload);

            MallMember customer = memberMapper.selectMemberById(accepted.getMemberId());
            if (customer != null && customer.getFcmToken() != null)
            {
                fcmService.sendToToken(customer.getFcmToken(), "Runner On the Way",
                        "Your order is being delivered",
                        java.util.Collections.singletonMap("orderId", String.valueOf(orderId)));
            }
        }
        catch (Exception e)
        {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Push failed after acceptOrder: {}", e.getMessage());
        }

        return accepted;
    }

    @Override
    @Transactional
    public MallOrder completeOrder(Long orderId, Long memberId)
    {
        MallOrder order = orderMapper.selectOrderById(orderId);
        if (order == null)
            throw new RuntimeException("Order not found");
        if (!memberId.equals(order.getRunnerMemberId()))
            throw new RuntimeException("You are not the runner for this order");
        if (!"2".equals(order.getStatus()))
            throw new RuntimeException("Order is not in delivering status");

        order.setStatus("3");
        order.setUpdateTime(new Date());
        orderMapper.updateOrder(order);
        MallOrder completed = orderMapper.selectOrderById(orderId);

        // 积分奖励：1分/₱1（按实付金额取整）
        try
        {
            int pts = completed.getTotalAmount().intValue();
            if (pts > 0)
            {
                pointsService.earn(completed.getMemberId(), pts, 1,
                        completed.getOrderNo(), "Order #" + completed.getOrderNo() + " completed");
            }
        }
        catch (Exception e)
        {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Points earn failed after completeOrder: {}", e.getMessage());
        }

        // 邀请返积分：被邀请人完成第一笔订单时，给邀请人 200 积分
        try
        {
            MallMember customer = memberMapper.selectMemberById(completed.getMemberId());
            if (customer != null && customer.getReferrerId() != null)
            {
                int completedCount = orderMapper.countCompletedByMemberId(completed.getMemberId());
                if (completedCount == 1)
                {
                    pointsService.earn(customer.getReferrerId(), 200, 5,
                            String.valueOf(completed.getMemberId()),
                            "Referral reward: new member completed first order");
                }
            }
        }
        catch (Exception e)
        {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Referral reward failed after completeOrder: {}", e.getMessage());
        }

        // 推送：SSE（Web）+ FCM（Mobile）
        try
        {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("orderId", orderId);
            payload.put("status", "3");
            payload.put("title", "Order Delivered!");
            payload.put("body",  "Tap to rate your runner");
            sseService.push(completed.getMemberId(), "order_status", payload);

            MallMember customer = memberMapper.selectMemberById(completed.getMemberId());
            if (customer != null && customer.getFcmToken() != null)
            {
                fcmService.sendToToken(customer.getFcmToken(), "Order Delivered!",
                        "Tap to rate your runner",
                        java.util.Collections.singletonMap("orderId", String.valueOf(orderId)));
            }
        }
        catch (Exception e)
        {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Push failed after completeOrder: {}", e.getMessage());
        }

        return completed;
    }

    @Override
    public List<MallOrder> getMyDeliveries(Long memberId)
    {
        return orderMapper.selectByRunnerMemberId(memberId);
    }

    // ---- 评价 ----

    @Override
    @Transactional
    public void rateRunner(MallRunnerRating rating)
    {
        MallOrder order = orderMapper.selectOrderById(rating.getOrderId());
        if (order == null)
            throw new RuntimeException("Order not found");
        if (!rating.getRaterMemberId().equals(order.getMemberId()))
            throw new RuntimeException("Order does not belong to you");
        if (!"3".equals(order.getStatus()))
            throw new RuntimeException("Order is not completed yet");
        if (order.getRunnerMemberId() == null)
            throw new RuntimeException("No runner for this order");
        if (ratingMapper.selectByOrderId(rating.getOrderId()) != null)
            throw new RuntimeException("Already rated");

        rating.setRunnerMemberId(order.getRunnerMemberId());
        rating.setCreateTime(new Date());
        ratingMapper.insert(rating);
    }

    @Override
    public Map<String, Object> getRunnerStats(Long runnerMemberId)
    {
        MallMember member = memberMapper.selectMemberById(runnerMemberId);
        int total          = orderMapper.countByRunnerMemberId(runnerMemberId);
        int ratingCount    = ratingMapper.countByRunnerMemberId(runnerMemberId);
        BigDecimal avgScore = ratingMapper.avgScoreByRunnerMemberId(runnerMemberId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("memberId",        runnerMemberId);
        stats.put("nickName",        member != null ? member.getNickName() : "");
        stats.put("avatarUrl",       member != null ? member.getAvatarUrl() : "");
        stats.put("totalDeliveries", total);
        stats.put("ratingCount",     ratingCount);
        stats.put("averageScore",    avgScore != null ? avgScore.setScale(1, java.math.RoundingMode.HALF_UP) : null);
        return stats;
    }

    @Override
    public Map<String, Object> getMyStats(Long memberId)
    {
        Map<String, Object> stats = getRunnerStats(memberId);
        stats.put("weekDeliveries",  orderMapper.countDeliveriesThisWeek(memberId));
        stats.put("monthDeliveries", orderMapper.countDeliveriesThisMonth(memberId));
        BigDecimal weekEarnings  = orderMapper.sumEarningsThisWeek(memberId);
        BigDecimal monthEarnings = orderMapper.sumEarningsThisMonth(memberId);
        stats.put("weekEarnings",  weekEarnings  != null ? weekEarnings  : BigDecimal.ZERO);
        stats.put("monthEarnings", monthEarnings != null ? monthEarnings : BigDecimal.ZERO);
        MallRunnerApplication app = appMapper.selectByMemberId(memberId);
        stats.put("isOnline", app != null && "1".equals(app.getIsOnline()));
        return stats;
    }

    @Override
    public void setOnlineStatus(Long memberId, boolean online)
    {
        requireApprovedRunner(memberId);
        appMapper.updateOnlineStatus(memberId, online ? "1" : "0");
    }

    // ---- Admin：审核 ----

    @Override
    public List<MallRunnerApplication> listApplications(MallRunnerApplication query)
    {
        return appMapper.selectList(query);
    }

    @Override
    public void approveApplication(Long appId, String reviewer)
    {
        MallRunnerApplication app = new MallRunnerApplication();
        app.setAppId(appId);
        app.setStatus("1");
        app.setRejectReason(null);
        app.setReviewTime(new Date());
        app.setReviewer(reviewer);
        appMapper.updateStatus(app);
    }

    @Override
    public void rejectApplication(Long appId, String reviewer, String rejectReason)
    {
        MallRunnerApplication app = new MallRunnerApplication();
        app.setAppId(appId);
        app.setStatus("2");
        app.setRejectReason(rejectReason);
        app.setReviewTime(new Date());
        app.setReviewer(reviewer);
        appMapper.updateStatus(app);
    }

    // ---- Admin：结算 ----

    @Override
    public List<Map<String, Object>> getUnsettledOrders(Long runnerMemberId)
    {
        List<MallOrder> orders = orderMapper.selectUnsettledRunnerOrders(runnerMemberId);
        // 批量加载所有 runner 信息，避免 N+1
        Map<Long, MallMember> memberMap = new HashMap<>();
        if (!orders.isEmpty())
        {
            List<Long> runnerIds = orders.stream()
                    .map(MallOrder::getRunnerMemberId)
                    .distinct()
                    .collect(Collectors.toList());
            memberMapper.selectMembersByIds(runnerIds)
                    .forEach(m -> memberMap.put(m.getMemberId(), m));
        }
        Map<Long, Map<String, Object>> grouped = new java.util.LinkedHashMap<>();
        for (MallOrder o : orders)
        {
            grouped.computeIfAbsent(o.getRunnerMemberId(), id -> {
                MallMember m = memberMap.get(id);
                Map<String, Object> row = new HashMap<>();
                row.put("runnerMemberId", id);
                row.put("nickName",  m != null ? m.getNickName() : "");
                row.put("phone",     m != null ? m.getPhone() : "");
                row.put("totalFee",  BigDecimal.ZERO);
                row.put("orders",    new ArrayList<>());
                return row;
            });
            Map<String, Object> row = grouped.get(o.getRunnerMemberId());
            row.put("totalFee", ((BigDecimal) row.get("totalFee")).add(o.getDeliveryFee()));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> orderList = (List<Map<String, Object>>) row.get("orders");
            Map<String, Object> item = new HashMap<>();
            item.put("orderId",     o.getOrderId());
            item.put("orderNo",     o.getOrderNo());
            item.put("deliveryFee", o.getDeliveryFee());
            orderList.add(item);
        }
        return new ArrayList<>(grouped.values());
    }

    @Override
    @Transactional
    public void settleRunnerFee(List<Long> orderIds)
    {
        if (orderIds == null || orderIds.isEmpty()) return;
        orderMapper.settleRunnerFee(orderIds);
    }

    // ---- private ----

    private void requireApprovedRunner(Long memberId)
    {
        MallRunnerApplication app = appMapper.selectByMemberId(memberId);
        if (app == null || !"1".equals(app.getStatus()))
        {
            throw new RuntimeException("Runner not approved");
        }
    }
}
