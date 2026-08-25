package com.ruoyi.mall.domain;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

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
    /** 性别（0未知 1男 2女） */
    private String gender;
    /** 生日 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date birthday;
    /** 状态（0正常 1禁用） */
    private String status;
    /** 角色：customer(默认) / cashier / admin —— POS 收银台门禁用 */
    private String role;
    /** 密码哈希，BCrypt，邮箱注册用户有值，Google/Apple 用户为 null */
    private String passwordHash;
    private Date createTime;
    private Date updateTime;
    /** FCM 设备推送令牌 */
    private String fcmToken;
    /** 积分余额 */
    private int points;
    /** 个人邀请码（注册时自动生成） */
    private String inviteCode;
    /** 邀请人会员ID */
    private Long referrerId;
    /** 外部会员标识（字符串；外部系统导入订单时的 userId，与内部 member_id 解耦） */
    private String externalId;
    /** 非DB：已完成订单数（邀请记录接口填充） */
    private int completedOrderCount;

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

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Date getBirthday() { return birthday; }
    public void setBirthday(Date birthday) { this.birthday = birthday; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    /** 是否已设置登录密码（瞬时字段，不入库），用于前端判断显示“设置”还是“修改”密码 */
    private Boolean hasPassword;
    public Boolean getHasPassword() { return hasPassword; }
    public void setHasPassword(Boolean hasPassword) { this.hasPassword = hasPassword; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    public Long getReferrerId() { return referrerId; }
    public void setReferrerId(Long referrerId) { this.referrerId = referrerId; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public int getCompletedOrderCount() { return completedOrderCount; }
    public void setCompletedOrderCount(int completedOrderCount) { this.completedOrderCount = completedOrderCount; }
}
