package com.ruoyi.mall.domain;

import java.util.Date;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 拼团活动表 mall_group_activity
 */
public class MallGroupActivity
{
    private Long activityId;
    private Long productId;
    private String title;
    private Integer minGroupSize;
    private Integer durationHours;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private Date startTime;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private Date endTime;
    /** 0进行中 1已结束 */
    private String status;
    private Date createTime;

    /** 非DB：价格阶梯（查询时填充） */
    private List<MallGroupTier> tiers;
    /** 非DB：商品名称 */
    private String productName;
    /** 非DB：商品图片 */
    private String productImage;
    /** 非DB：商品原价 */
    private java.math.BigDecimal originalPrice;
    /** 非DB：最优（最低）拼团价，商品列表用 */
    private java.math.BigDecimal bestPrice;
    /** 非DB：是否「即将开始」(now < startTime)，App 据此显示倒计时并禁用开团按钮 */
    private boolean upcoming;

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getMinGroupSize() { return minGroupSize; }
    public void setMinGroupSize(Integer minGroupSize) { this.minGroupSize = minGroupSize; }

    public Integer getDurationHours() { return durationHours; }
    public void setDurationHours(Integer durationHours) { this.durationHours = durationHours; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public List<MallGroupTier> getTiers() { return tiers; }
    public void setTiers(List<MallGroupTier> tiers) { this.tiers = tiers; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }

    public java.math.BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(java.math.BigDecimal originalPrice) { this.originalPrice = originalPrice; }

    public java.math.BigDecimal getBestPrice() { return bestPrice; }
    public void setBestPrice(java.math.BigDecimal bestPrice) { this.bestPrice = bestPrice; }

    public boolean getUpcoming() { return upcoming; }
    public void setUpcoming(boolean upcoming) { this.upcoming = upcoming; }
}
