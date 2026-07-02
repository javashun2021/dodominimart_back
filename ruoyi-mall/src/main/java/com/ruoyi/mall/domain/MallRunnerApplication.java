package com.ruoyi.mall.domain;

import java.util.Date;

/**
 * 跑腿资格申请表 mall_runner_application
 */
public class MallRunnerApplication
{
    private Long   appId;
    private Long   memberId;
    private String realName;
    private String idNumber;
    private String phone;
    private String idPhotoUrl;
    /** 0待审核 1通过 2拒绝 */
    private String status;
    private String rejectReason;
    private Date   applyTime;
    private Date   reviewTime;
    private String reviewer;

    /** 是否在线接单（0否 1是） */
    private String isOnline;

    /** 归属门店ID（模型C：审核通过时指派，决定该骑手抢单池） */
    private Long storeId;

    /** 非DB：申请人昵称（列表展示用） */
    private String nickName;
    /** 非DB：申请人头像 */
    private String avatarUrl;
    /** 非DB：归属门店名称（列表展示用） */
    private String storeName;

    public Long   getAppId()        { return appId; }
    public void   setAppId(Long v)  { this.appId = v; }

    public Long   getMemberId()        { return memberId; }
    public void   setMemberId(Long v)  { this.memberId = v; }

    public String getRealName()        { return realName; }
    public void   setRealName(String v){ this.realName = v; }

    public String getIdNumber()        { return idNumber; }
    public void   setIdNumber(String v){ this.idNumber = v; }

    public String getPhone()        { return phone; }
    public void   setPhone(String v){ this.phone = v; }

    public String getIdPhotoUrl()        { return idPhotoUrl; }
    public void   setIdPhotoUrl(String v){ this.idPhotoUrl = v; }

    public String getStatus()        { return status; }
    public void   setStatus(String v){ this.status = v; }

    public String getRejectReason()        { return rejectReason; }
    public void   setRejectReason(String v){ this.rejectReason = v; }

    public Date   getApplyTime()       { return applyTime; }
    public void   setApplyTime(Date v) { this.applyTime = v; }

    public Date   getReviewTime()       { return reviewTime; }
    public void   setReviewTime(Date v) { this.reviewTime = v; }

    public String getReviewer()        { return reviewer; }
    public void   setReviewer(String v){ this.reviewer = v; }

    public String getIsOnline()         { return isOnline; }
    public void   setIsOnline(String v) { this.isOnline = v; }

    public Long   getStoreId()        { return storeId; }
    public void   setStoreId(Long v)  { this.storeId = v; }

    public String getNickName()        { return nickName; }
    public void   setNickName(String v){ this.nickName = v; }

    public String getAvatarUrl()        { return avatarUrl; }
    public void   setAvatarUrl(String v){ this.avatarUrl = v; }

    public String getStoreName()        { return storeName; }
    public void   setStoreName(String v){ this.storeName = v; }
}
