package com.ruoyi.mall.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
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

    @Override
    public List<MallGroupActivity> selectActivityList(MallGroupActivity query)
    {
        List<MallGroupActivity> list = activityMapper.selectActivityList(query);
        for (MallGroupActivity a : list)
        {
            a.setTiers(activityMapper.selectTiersByActivityId(a.getActivityId()));
        }
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
        for (MallGroupActivity a : list)
        {
            a.setTiers(activityMapper.selectTiersByActivityId(a.getActivityId()));
        }
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

        if (activity.getMinGroupSize() <= 1)
        {
            completeGroup(groupOrder, activity);
        }

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

        MallGroupActivity activity = activityMapper.selectActivityById(groupOrder.getActivityId());
        if (newSize >= activity.getMinGroupSize())
        {
            completeGroup(groupOrder, activity);
        }

        return getGroupDetail(inviteCode);
    }

    @Override
    public List<MallGroupOrder> getMyGroups(Long memberId)
    {
        List<MallGroupOrder> list = groupOrderMapper.selectMyGroups(memberId);
        for (MallGroupOrder go : list)
        {
            go.setMembers(memberMapper.selectMembersByGroupOrderId(go.getGroupOrderId()));
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
            }
        }
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
            item.setSubtotal(subtotal);

            MallOrder order = new MallOrder();
            order.setOrderNo(generateOrderNo());
            order.setMemberId(m.getMemberId());
            order.setAddressSnapshot(buildAddressSnapshot(address));
            order.setTotalAmount(subtotal);
            order.setStatus("1");
            order.setRemark("拼团成功");
            order.setCreateTime(new Date());
            orderMapper.insertOrder(order);

            item.setOrderId(order.getOrderId());
            orderItemMapper.insertOrderItemBatch(Collections.singletonList(item));

            memberMapper.updateMemberOrderId(m.getId(), order.getOrderId());
        }
    }

    private BigDecimal calcTierPrice(Long activityId, int currentSize)
    {
        List<MallGroupTier> tiers = activityMapper.selectTiersByActivityId(activityId);
        for (MallGroupTier t : tiers)
        {
            if (currentSize >= t.getMinQuantity()
                    && (t.getMaxQuantity() == null || currentSize <= t.getMaxQuantity()))
            {
                return t.getPrice();
            }
        }
        return tiers.isEmpty() ? BigDecimal.ZERO : tiers.get(tiers.size() - 1).getPrice();
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
