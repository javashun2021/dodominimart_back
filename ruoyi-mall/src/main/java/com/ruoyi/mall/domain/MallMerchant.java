package com.ruoyi.mall.domain;

import java.math.BigDecimal;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.base.BaseEntity;

/**
 * 商家/门店（入驻）表 mall_merchant
 *
 * 附近商家一期：把社区店/附近商店入驻到 App。商品挂 mall_product.merchant_id。
 * status: 0待审核 / 1营业 / 2拒绝 / 3停业。地推员录入时归属 promoter_id(会员ID)。
 */
public class MallMerchant extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long merchantId;

    @Excel(name = "商家名称")
    private String name;

    /** 商家分类(字典 mall_merchant_type) */
    @Excel(name = "分类")
    private String category;

    private String description;

    /** 门店Logo/头图 */
    private String logoUrl;

    /** 门店照片，逗号分隔URL */
    private String images;

    @Excel(name = "地址")
    private String address;

    /** 纬度 */
    private BigDecimal lat;

    /** 经度 */
    private BigDecimal lng;

    @Excel(name = "电话")
    private String phone;

    /** 营业时间，如 08:00-22:00 */
    private String businessHours;

    /** 其它联系方式(JSON)：区别于 phone，存 messenger/telegram/whatsapp/viber/wechat 等 */
    private String contactInfo;

    /** 配送半径(km)，二期配送用 */
    private BigDecimal serviceRadiusKm;

    /** 录入的地推员会员ID(业绩归属) */
    private Long promoterId;

    /** 商家App账号会员ID(代录可空) */
    private Long ownerMemberId;

    /** 状态（0待审核 1营业 2拒绝 3停业） */
    @Excel(name = "状态", readConverterExp = "0=待审核,1=营业,2=拒绝,3=停业")
    private String status;

    /** 审核拒绝原因 */
    private String rejectReason;

    /** 审核时间 */
    private java.util.Date reviewTime;

    /** 审核人 */
    private String reviewer;

    private Integer sort;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    /** 非DB：距用户距离(km)，附近接口填充 */
    private BigDecimal distanceKm;

    /** 非DB：录入地推员昵称（后台列表展示） */
    private String promoterName;

    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public BigDecimal getLat() { return lat; }
    public void setLat(BigDecimal lat) { this.lat = lat; }

    public BigDecimal getLng() { return lng; }
    public void setLng(BigDecimal lng) { this.lng = lng; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getBusinessHours() { return businessHours; }
    public void setBusinessHours(String businessHours) { this.businessHours = businessHours; }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

    public BigDecimal getServiceRadiusKm() { return serviceRadiusKm; }
    public void setServiceRadiusKm(BigDecimal serviceRadiusKm) { this.serviceRadiusKm = serviceRadiusKm; }

    public Long getPromoterId() { return promoterId; }
    public void setPromoterId(Long promoterId) { this.promoterId = promoterId; }

    public Long getOwnerMemberId() { return ownerMemberId; }
    public void setOwnerMemberId(Long ownerMemberId) { this.ownerMemberId = ownerMemberId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public java.util.Date getReviewTime() { return reviewTime; }
    public void setReviewTime(java.util.Date reviewTime) { this.reviewTime = reviewTime; }

    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }

    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }

    public String getPromoterName() { return promoterName; }
    public void setPromoterName(String promoterName) { this.promoterName = promoterName; }

    /**
     * 非DB派生：是否可站内聊天。
     * 有真实店主会员(自助开店 owner_member_id 非空)=true；
     * 平台自录/测试/地推录入(无店主)=false，App 据此显示「联系店主」或回退打电话。
     */
    public boolean getChattable() { return ownerMemberId != null; }
}
