package com.ruoyi.mall.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.ruoyi.mall.service.IMallRunnerService;

@Service
public class MallRunnerServiceImpl implements IMallRunnerService
{
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("20");

    @Autowired private MallRunnerApplicationMapper appMapper;
    @Autowired private MallRunnerRatingMapper       ratingMapper;
    @Autowired private MallOrderMapper              orderMapper;
    @Autowired private MallMemberMapper             memberMapper;

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
        orderMapper.updateRunnerInfo(order);
        return orderMapper.selectOrderById(orderId);
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
        return orderMapper.selectOrderById(orderId);
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
        int total          = orderMapper.selectByRunnerMemberId(runnerMemberId).size();
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
        Map<Long, Map<String, Object>> grouped = new java.util.LinkedHashMap<>();
        for (MallOrder o : orders)
        {
            grouped.computeIfAbsent(o.getRunnerMemberId(), id -> {
                MallMember m = memberMapper.selectMemberById(id);
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
