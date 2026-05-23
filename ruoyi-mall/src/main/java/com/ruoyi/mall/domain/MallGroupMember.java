package com.ruoyi.mall.domain;

import java.util.Date;

/**
 * 拼团成员表 mall_group_member
 */
public class MallGroupMember
{
    private Long id;
    private Long groupOrderId;
    private Long memberId;
    private Integer quantity;
    private Long orderId;
    private Long addressId;
    private Date joinedTime;

    /** 非DB：会员昵称和头像（详情页展示） */
    private String nickName;
    private String avatarUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGroupOrderId() { return groupOrderId; }
    public void setGroupOrderId(Long groupOrderId) { this.groupOrderId = groupOrderId; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }

    public Date getJoinedTime() { return joinedTime; }
    public void setJoinedTime(Date joinedTime) { this.joinedTime = joinedTime; }

    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
