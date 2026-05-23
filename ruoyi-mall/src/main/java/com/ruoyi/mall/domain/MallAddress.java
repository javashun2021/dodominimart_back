package com.ruoyi.mall.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 收货地址表 mall_address
 */
public class MallAddress implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long addressId;
    private Long memberId;
    private String label;
    private String fullAddress;
    private String phone;
    /** 是否默认（0否 1是） */
    private String isDefault;
    private Date createTime;
    private Date updateTime;

    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getFullAddress() { return fullAddress; }
    public void setFullAddress(String fullAddress) { this.fullAddress = fullAddress; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getIsDefault() { return isDefault; }
    public void setIsDefault(String isDefault) { this.isDefault = isDefault; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
