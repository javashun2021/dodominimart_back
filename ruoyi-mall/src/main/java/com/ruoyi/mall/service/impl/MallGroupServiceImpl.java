package com.ruoyi.mall.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.mall.domain.MallAddress;
import com.ruoyi.mall.domain.MallGroupActivity;
import com.ruoyi.mall.domain.MallGroupMember;
import com.ruoyi.mall.domain.MallGroupOrder;
import com.ruoyi.mall.domain.MallGroupTier;
import com.ruoyi.mall.domain.MallOrder;
import com.ruoyi.mall.domain.MallOrderItem;
import com.ruoyi.mall.domain.MallProduct;
import com.ruoyi.mall.mapper.MallAddressMapper;
import com.ruoyi.mall.mapper.MallGroupActivityMapper;
import com.ruoyi.mall.mapper.MallGroupMemberMapper;
import com.ruoyi.mall.mapper.MallGroupOrderMapper;
import com.ruoyi.mall.mapper.MallOrderItemMapper;
import com.ruoyi.mall.mapper.MallOrderMapper;
import com.ruoyi.mall.mapper.MallProductMapper;
import com.ruoyi.mall.mapper.MallMemberMapper;
import com.ruoyi.mall.service.FcmService;
import com.ruoyi.mall.service.IGCashService;
import com.ruoyi.mall.service.IMallGroupService;

@Service
public class MallGroupServiceImpl implements IMallGroupService
{
    private static final String INVITE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    @Autowired private MallGroupActivityMapper activityMapper;
    @Autowired private MallGroupOrderMapper    groupOrderMapper;
    @Autowired private MallGroupMemberMapper   memberMapper;
    @Autowired private MallOrderMapper         orderMapper;
    @Autowired private MallOrderItemMapper     orderItemMapper;
    @Autowired private MallProductMapper          productMapper;
    @Autowired private MallAddressMapper          addressMapper;
    @Autowired private IGCashService              gcashService;
    @Autowired private com.ruoyi.mall.mapper.MallPaymentRecordMapper paymentRecordMapper;
    @Autowired private MallMemberMapper            mallMemberMapper;
    @Autowired private FcmService                  fcmService;

    @Override
    public List<MallGroupOrder> selectGroupOrderList(MallGroupOrder query)
    {
        return groupOrderMapper.selectGroupOrderList(query);
    }

    @Override
    public List<MallGroupMember> selectGroupMembers(Long groupOrderId)
    {
        return memberMapper.selectMembersByGroupOrderId(groupOrderId);
    }

    @Override
    public List<MallGroupActivity> selectActivityList(MallGroupActivity query)
    {
        List<MallGroupActivity> list = activityMapper.selectActivityList(query);
        fillTiers(list);
        return list;
    }

    @Override
    public MallGroupActivity selectActivityById(Long activityId)
    {
        MallGroupActivity activity = activityMapper.selectActivityById(activityId);
        if (activity != null)
        {
            activity.setTiers(activityMapper.selectTiersByActivityId(activityId));
        }
        return activity;
    }

    @Override
    @Transactional
    public int insertActivity(MallGroupActivity activity)
    {
        activity.setCreateTime(new Date());
        if (activity.getStatus() == null) activity.setStatus("0");
        int rows = activityMapper.insertActivity(activity);
        if (activity.getTiers() != null && !activity.getTiers().isEmpty())
        {
            for (MallGroupTier t : activity.getTiers())
            {
                t.setActivityId(activity.getActivityId());
            }
            activityMapper.insertTiers(activity.getTiers());
        }
        return rows;
    }

    @Override
    @Transactional
    public int updateActivity(MallGroupActivity activity)
    {
        int rows = activityMapper.updateActivity(activity);
        if (activity.getTiers() != null)
        {
            activityMapper.deleteTiersByActivityId(activity.getActivityId());
            if (!activity.getTiers().isEmpty())
            {
                for (MallGroupTier t : activity.getTiers())
                {
                    t.setActivityId(activity.getActivityId());
                }
                activityMapper.insertTiers(activity.getTiers());
            }
        }
        return rows;
    }

    @Override
    public int deleteActivityById(Long activityId)
    {
        activityMapper.deleteTiersByActivityId(activityId);
        return activityMapper.deleteActivityById(activityId);
    }

    @Override
    public List<MallGroupActivity> getActiveActivities()
    {
        MallGroupActivity query = new MallGroupActivity();
        query.setStatus("0");
        List<MallGroupActivity> list = activityMapper.selectActivityList(query);
        // 展示窗口 = [start - 1h, end]：开始前 1 小时起就展示（让顾客提前知道有活动、做倒计时预热），
        // 已过期（now > end）的不再展示。startTime/endTime 必填，缺失则不展示。
        // 同时标记 upcoming=true（now < start，尚未开始）——App 据此显示「即将开始」并禁用开团按钮，
        // 避免顾客点进去撞上 createGroup 的「not in valid time range」拦截。
        Date now = new Date();
        long preheatMs = 60L * 60 * 1000; // 开团前 1 小时预热
        list = list.stream()
                .filter(a -> a.getStartTime() != null && a.getEndTime() != null
                        && now.getTime() >= a.getStartTime().getTime() - preheatMs
                        && !now.after(a.getEndTime()))
                .collect(Collectors.toList());
        for (MallGroupActivity a : list)
        {
            a.setUpcoming(now.before(a.getStartTime()));
        }
        fillTiers(list);
        return list;
    }

    @Override
    @Transactional
    public MallGroupOrder createGroup(Long activityId, Long memberId, int quantity, Long addressId)
    {
        MallGroupActivity activity = activityMapper.selectActivityById(activityId);
        if (activity == null || !"0".equals(activity.getStatus()))
        {
            throw new RuntimeException("Group activity is not available");
        }
        Date now = new Date();
        if (now.before(activity.getStartTime()) || now.after(activity.getEndTime()))
        {
            throw new RuntimeException("Activity is not in valid time range");
        }

        String inviteCode = generateInviteCode();

        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.HOUR_OF_DAY, activity.getDurationHours());

        BigDecimal price = calcTierPrice(activityId, 1);

        MallGroupOrder groupOrder = new MallGroupOrder();
        groupOrder.setActivityId(activityId);
        groupOrder.setProductId(activity.getProductId());
        groupOrder.setInitiatorMemberId(memberId);
        groupOrder.setInviteCode(inviteCode);
        groupOrder.setCurrentSize(1);
        groupOrder.setCurrentPrice(price);
        groupOrder.setStatus("0");
        groupOrder.setExpireTime(cal.getTime());
        groupOrder.setCreateTime(now);
        groupOrderMapper.insertGroupOrder(groupOrder);

        MallGroupMember member = new MallGroupMember();
        member.setGroupOrderId(groupOrder.getGroupOrderId());
        member.setMemberId(memberId);
        member.setQuantity(quantity);
        member.setAddressId(addressId);
        member.setJoinedTime(now);
        memberMapper.insertMember(member);

        // 填充 activity（含商品图片、价格阶梯）和 members，与 getGroupDetail 返回结构一致
        activity.setTiers(activityMapper.selectTiersByActivityId(activityId));
        groupOrder.setActivity(activity);
        groupOrder.setMembers(memberMapper.selectMembersByGroupOrderId(groupOrder.getGroupOrderId()));
        return groupOrder;
    }

    @Override
    public MallGroupOrder getGroupDetail(String inviteCode)
    {
        MallGroupOrder groupOrder = groupOrderMapper.selectGroupOrderByInviteCode(inviteCode);
        if (groupOrder == null) return null;
        groupOrder.setMembers(memberMapper.selectMembersByGroupOrderId(groupOrder.getGroupOrderId()));
        MallGroupActivity activity = activityMapper.selectActivityById(groupOrder.getActivityId());
        if (activity != null)
        {
            activity.setTiers(activityMapper.selectTiersByActivityId(activity.getActivityId()));
            groupOrder.setActivity(activity);
        }
        return groupOrder;
    }

    @Override
    @Transactional
    public MallGroupOrder joinGroup(String inviteCode, Long memberId, int quantity, Long addressId)
    {
        MallGroupOrder groupOrder = groupOrderMapper.selectGroupOrderByInviteCode(inviteCode);
        if (groupOrder == null) throw new RuntimeException("Group not found");
        if (!"0".equals(groupOrder.getStatus())) throw new RuntimeException("Group is no longer active");
        if (new Date().after(groupOrder.getExpireTime())) throw new RuntimeException("Group has expired");
        if (memberMapper.selectMember(groupOrder.getGroupOrderId(), memberId) != null)
        {
            throw new RuntimeException("Already joined this group");
        }

        MallGroupMember member = new MallGroupMember();
        member.setGroupOrderId(groupOrder.getGroupOrderId());
        member.setMemberId(memberId);
        member.setQuantity(quantity);
        member.setAddressId(addressId);
        member.setJoinedTime(new Date());
        memberMapper.insertMember(member);

        int newSize = groupOrder.getCurrentSize() + 1;
        groupOrder.setCurrentSize(newSize);
        groupOrder.setCurrentPrice(calcTierPrice(groupOrder.getActivityId(), newSize));
        groupOrderMapper.updateGroupOrder(groupOrder);

        return getGroupDetail(inviteCode);
    }

    @Override
    public List<MallGroupOrder> getMyGroups(Long memberId)
    {
        List<MallGroupOrder> list = groupOrderMapper.selectMyGroups(memberId);
        if (list.isEmpty()) return list;
        // Batch-fetch unique activities to avoid N+1
        List<Long> activityIds = list.stream()
                .map(MallGroupOrder::getActivityId).distinct().collect(Collectors.toList());
        List<MallGroupActivity> activities = activityIds.stream()
                .map(activityMapper::selectActivityById)
                .filter(a -> a != null)
                .collect(Collectors.toList());
        fillTiers(activities);
        Map<Long, MallGroupActivity> actMap = activities.stream()
                .collect(Collectors.toMap(MallGroupActivity::getActivityId, a -> a));
        for (MallGroupOrder go : list)
        {
            go.setMembers(memberMapper.selectMembersByGroupOrderId(go.getGroupOrderId()));
            go.setActivity(actMap.get(go.getActivityId()));
        }
        return list;
    }

    @Override
    @Transactional
    public void expireGroups()
    {
        List<MallGroupOrder> expired = groupOrderMapper.selectExpiredGroups();
        for (MallGroupOrder go : expired)
        {
            MallGroupActivity activity = activityMapper.selectActivityById(go.getActivityId());
            if (activity != null && go.getCurrentSize() >= activity.getMinGroupSize())
            {
                // Reached min size at expiry — auto-complete and generate orders
                completeGroup(go, activity);
            }
            else
            {
                // Did not reach min size — fail and refund
                go.setStatus("2");
                groupOrderMapper.updateGroupOrder(go);
                List<MallGroupMember> members = memberMapper.selectMembersByGroupOrderId(go.getGroupOrderId());
                for (MallGroupMember m : members)
                {
                    if (m.getOrderId() != null)
                    {
                        try
                        {
                            com.ruoyi.mall.domain.MallPaymentRecord rec =
                                    paymentRecordMapper.selectByOrderId(m.getOrderId());
                            if (rec != null && "SUCCESS".equals(rec.getStatus()) && rec.getPaymentNo() != null)
                            {
                                gcashService.refund(rec.getPaymentNo(), rec.getAmount());
                            }
                        }
                        catch (Exception ignored) { }
                    }
                    // 推送拼团失败通知（用 inviteCode 跳转拼团详情页查看结果）
                    try
                    {
                        com.ruoyi.mall.domain.MallMember customer = mallMemberMapper.selectMemberById(m.getMemberId());
                        if (customer != null && customer.getFcmToken() != null)
                        {
                            fcmService.sendToToken(customer.getFcmToken(), "Group Buy Failed",
                                    "Your group did not reach the minimum size. Any payment will be refunded.",
                                    java.util.Collections.singletonMap("inviteCode", go.getInviteCode()));
                        }
                    }
                    catch (Exception ignored) { }
                }
            }
        }
    }

    @Override
    @Transactional
    public MallGroupOrder closeGroup(String inviteCode, Long memberId)
    {
        MallGroupOrder groupOrder = groupOrderMapper.selectGroupOrderByInviteCode(inviteCode);
        if (groupOrder == null)
            throw new RuntimeException("Group not found");
        if (!"0".equals(groupOrder.getStatus()))
            throw new RuntimeException("Group is no longer active");
        if (!memberId.equals(groupOrder.getInitiatorMemberId()))
            throw new RuntimeException("Only the initiator can close the group");

        MallGroupActivity activity = activityMapper.selectActivityById(groupOrder.getActivityId());
        if (groupOrder.getCurrentSize() < activity.getMinGroupSize())
            throw new RuntimeException("Minimum group size not reached yet");

        completeGroup(groupOrder, activity);
        return getGroupDetail(inviteCode);
    }

    // ---- private helpers ----

    @Transactional
    private void completeGroup(MallGroupOrder groupOrder, MallGroupActivity activity)
    {
        groupOrder.setStatus("1");
        groupOrder.setSuccessTime(new Date());
        groupOrderMapper.updateGroupOrder(groupOrder);

        MallProduct product = productMapper.selectProductById(groupOrder.getProductId());
        List<MallGroupMember> members = memberMapper.selectMembersByGroupOrderId(groupOrder.getGroupOrderId());

        for (MallGroupMember m : members)
        {
            if (m.getAddressId() == null) continue;
            MallAddress address = addressMapper.selectAddressById(m.getAddressId());
            if (address == null) continue;

            BigDecimal unitPrice = groupOrder.getCurrentPrice();
            BigDecimal subtotal  = unitPrice.multiply(BigDecimal.valueOf(m.getQuantity()));

            MallOrderItem item = new MallOrderItem();
            item.setProductId(product.getProductId());
            item.setProductName(product.getName());
            item.setProductImage(product.getImageUrl());
            item.setQuantity(m.getQuantity());
            item.setPrice(unitPrice);
            item.setOriginalPrice(product.getPrice()); // 原价快照（拼团价低于原价，用于展示优惠）
            item.setSubtotal(subtotal);

            MallOrder order = new MallOrder();
            order.setOrderNo(generateOrderNo());
            order.setMemberId(m.getMemberId());
            order.setAddressSnapshot(buildAddressSnapshot(address));
            order.setTotalAmount(subtotal);
            order.setStatus("1");
            order.setPaymentMethod("COD");
            order.setRemark("Group buy successful");
            order.setOrderSource("GROUP");
            order.setPointsUsed(0);
            order.setCouponDiscount(BigDecimal.ZERO);
            order.setCreateTime(new Date());
            orderMapper.insertOrder(order);

            item.setOrderId(order.getOrderId());
            orderItemMapper.insertOrderItemBatch(Collections.singletonList(item));

            memberMapper.updateMemberOrderId(m.getId(), order.getOrderId());

            // 推送拼团成功通知（用 inviteCode 跳转拼团详情页）
            try
            {
                com.ruoyi.mall.domain.MallMember customer = mallMemberMapper.selectMemberById(m.getMemberId());
                if (customer != null && customer.getFcmToken() != null)
                {
                    fcmService.sendToToken(customer.getFcmToken(), "Group Buy Success!",
                            "Your group order for " + product.getName() + " has been confirmed",
                            java.util.Collections.singletonMap("inviteCode", groupOrder.getInviteCode()));
                }
            }
            catch (Exception ignored) { }
        }
    }

    private void fillTiers(List<MallGroupActivity> activities)
    {
        if (activities == null || activities.isEmpty()) return;
        List<Long> ids = activities.stream()
                .map(MallGroupActivity::getActivityId).collect(Collectors.toList());
        Map<Long, List<MallGroupTier>> tiersMap = activityMapper.selectTiersByActivityIds(ids)
                .stream().collect(Collectors.groupingBy(MallGroupTier::getActivityId));
        for (MallGroupActivity a : activities)
        {
            a.setTiers(tiersMap.getOrDefault(a.getActivityId(), Collections.emptyList()));
        }
    }

    private BigDecimal calcTierPrice(Long activityId, int currentSize)
    {
        List<MallGroupTier> tiers = activityMapper.selectTiersByActivityId(activityId);
        if (tiers.isEmpty()) return BigDecimal.ZERO;
        for (MallGroupTier t : tiers)
        {
            if (currentSize >= t.getMinQuantity()
                    && (t.getMaxQuantity() == null || currentSize <= t.getMaxQuantity()))
            {
                return t.getPrice();
            }
        }
        // below first tier threshold → use first tier price (highest)
        // above all tiers → use last tier price (lowest/best discount)
        return currentSize < tiers.get(0).getMinQuantity()
                ? tiers.get(0).getPrice()
                : tiers.get(tiers.size() - 1).getPrice();
    }

    private String generateInviteCode()
    {
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(7);
        for (int i = 0; i < 7; i++) sb.append(INVITE_CHARS.charAt(rnd.nextInt(INVITE_CHARS.length())));
        return sb.toString();
    }

    private String generateOrderNo()
    {
        return "G" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + String.format("%04d", new Random().nextInt(10000));
    }

    private String buildAddressSnapshot(MallAddress a)
    {
        return String.format("{\"addressId\":%d,\"label\":\"%s\",\"fullAddress\":\"%s\"}",
                a.getAddressId(), a.getLabel(), a.getFullAddress());
    }
}
