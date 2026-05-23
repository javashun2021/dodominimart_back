package com.ruoyi.mall.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 拼团单表 mall_group_order
 */
public class MallGroupOrder
{
    private Long groupOrderId;
    private Long activityId;
    private Long productId;
    private Long initiatorMemberId;
    private String inviteCode;
    private Integer currentSize;
    private BigDecimal currentPrice;
    /** 0拼团中 1成功 2失败 */
    private String status;
    private Date expireTime;
    private Date successTime;
    private Date createTime;

    /** 非DB：参与成员列表 */
    private List<MallGroupMember> members;
    /** 非DB：活动信息 */
    private MallGroupActivity activity;
    /** 非DB：后台列表展示 */
    private String activityTitle;
    private String productName;
    private Integer minGroupSize;

    public Long getGroupOrderId() { return groupOrderId; }
    public void setGroupOrderId(Long groupOrderId) { this.groupOrderId = groupOrderId; }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getInitiatorMemberId() { return initiatorMemberId; }
    public void setInitiatorMemberId(Long initiatorMemberId) { this.initiatorMemberId = initiatorMemberId; }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    public Integer getCurrentSize() { return currentSize; }
    public void setCurrentSize(Integer currentSize) { this.currentSize = currentSize; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getExpireTime() { return expireTime; }
    public void setExpireTime(Date expireTime) { this.expireTime = expireTime; }

    public Date getSuccessTime() { return successTime; }
    public void setSuccessTime(Date successTime) { this.successTime = successTime; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public List<MallGroupMember> getMembers() { return members; }
    public void setMembers(List<MallGroupMember> members) { this.members = members; }

    public MallGroupActivity getActivity() { return activity; }
    public void setActivity(MallGroupActivity activity) { this.activity = activity; }

    public String getActivityTitle() { return activityTitle; }
    public void setActivityTitle(String activityTitle) { this.activityTitle = activityTitle; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getMinGroupSize() { return minGroupSize; }
    public void setMinGroupSize(Integer minGroupSize) { this.minGroupSize = minGroupSize; }
}
