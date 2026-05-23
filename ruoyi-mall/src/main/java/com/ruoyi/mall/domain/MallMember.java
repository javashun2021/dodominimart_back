package com.ruoyi.mall.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * App 会员表 mall_member
 */
public class MallMember implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long memberId;
    /** Google 账号唯一标识(sub) */
    private String googleId;
    /** Apple 账号唯一标识(sub) */
    private String appleId;
    private String email;
    private String nickName;
    private String avatarUrl;
    private String phone;
    /** 状态（0正常 1禁用） */
    private String status;
    private Date createTime;
    private Date updateTime;

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public String getAppleId() { return appleId; }
    public void setAppleId(String appleId) { this.appleId = appleId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
