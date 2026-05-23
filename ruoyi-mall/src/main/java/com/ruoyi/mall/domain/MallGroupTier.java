package com.ruoyi.mall.domain;

import java.math.BigDecimal;

/**
 * 拼团价格阶梯表 mall_group_tier
 */
public class MallGroupTier
{
    private Long tierId;
    private Long activityId;
    private Integer minQuantity;
    private Integer maxQuantity;
    private BigDecimal price;

    public Long getTierId() { return tierId; }
    public void setTierId(Long tierId) { this.tierId = tierId; }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public Integer getMinQuantity() { return minQuantity; }
    public void setMinQuantity(Integer minQuantity) { this.minQuantity = minQuantity; }

    public Integer getMaxQuantity() { return maxQuantity; }
    public void setMaxQuantity(Integer maxQuantity) { this.maxQuantity = maxQuantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
