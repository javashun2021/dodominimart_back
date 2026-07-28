package com.ruoyi.mall.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.ruoyi.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;

public class MallMarketPost {
    private static final long serialVersionUID = 1L;

    private Long postId;
    private Long memberId;

    @Excel(name = "标题")
    private String title;

    private String description;
    private BigDecimal price;
    private String images;

    @Excel(name = "分类")
    private String category;

    /** 0=在售 1=已售 2=下架 */
    private String status;

    private String phone;
    private String whatsapp;
    private String messenger;
    private String telegram;
    private String viber;
    private String wechat;
    private String line;
    private String instagram;

    private Integer viewCount;
    private Integer commentCount;

    private String delFlag;

    private Integer moderated;
    private Integer reportCount;

    /** 0=待审核 1=已批准 2=已拒绝 */
    private String reviewStatus;
    private String reviewNote;

    /** fixed=固定价格 negotiable=面议 free=免费 */
    private String priceType;

    private String locationLabel;
    private Double latitude;
    private Double longitude;

    /** 外部同步来源标记（如 ext），普通用户发帖为空 */
    private String source;
    /** 外部来源唯一 ID，用于每日同步去重，普通用户发帖为空 */
    private String externalId;
    /** 同步帖原始联系方式（后台内部可见、对外隐藏）；仅 AdminPostResult 映射，App 查询不返回 */
    private String sourceContact;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    // transient fields joined from mall_member
    private String memberNickname;
    private String memberAvatar;

    // transient: 收藏列表标识帖子已被删除/下架；详情标识当前用户已收藏
    private Integer deleted;
    private Boolean favorited;

    // getters & setters
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }

    public Boolean getFavorited() { return favorited; }
    public void setFavorited(Boolean favorited) { this.favorited = favorited; }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }

    public String getMessenger() { return messenger; }
    public void setMessenger(String messenger) { this.messenger = messenger; }

    public String getTelegram() { return telegram; }
    public void setTelegram(String telegram) { this.telegram = telegram; }

    public String getViber() { return viber; }
    public void setViber(String viber) { this.viber = viber; }

    public String getWechat() { return wechat; }
    public void setWechat(String wechat) { this.wechat = wechat; }

    public String getLine() { return line; }
    public void setLine(String line) { this.line = line; }

    public String getInstagram() { return instagram; }
    public void setInstagram(String instagram) { this.instagram = instagram; }

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    public Integer getCommentCount() { return commentCount; }
    public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public Integer getModerated() { return moderated; }
    public void setModerated(Integer moderated) { this.moderated = moderated; }

    public Integer getReportCount() { return reportCount; }
    public void setReportCount(Integer reportCount) { this.reportCount = reportCount; }

    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }

    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }

    public String getPriceType() { return priceType; }
    public void setPriceType(String priceType) { this.priceType = priceType; }

    public String getLocationLabel() { return locationLabel; }
    public void setLocationLabel(String locationLabel) { this.locationLabel = locationLabel; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getSourceContact() { return sourceContact; }
    public void setSourceContact(String sourceContact) { this.sourceContact = sourceContact; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    public String getMemberNickname() { return memberNickname; }
    public void setMemberNickname(String memberNickname) { this.memberNickname = memberNickname; }

    public String getMemberAvatar() { return memberAvatar; }
    public void setMemberAvatar(String memberAvatar) { this.memberAvatar = memberAvatar; }

    /**
     * 是否可站内聊天：作者是真实会员（source 为空）=true；同步帖（source 非空、卖家为外部/机器人号）=false。
     * App 据此显示「站内聊天」还是「外链联系」。派生字段，随 getPost/listPosts 一并下发。
     */
    public boolean getChattable() { return source == null || source.trim().isEmpty(); }
}
