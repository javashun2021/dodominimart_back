package com.ruoyi.mall.domain;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 限时优惠活动表 mall_flash_sale
 */
public class MallFlashSale
{
    private Long saleId;
    private Long productId;
    private String title;
    private BigDecimal flashPrice;
    private Integer stockLimit;
    private Integer soldCount;
    private Integer perLimit;
    private Date startTime;
    private Date endTime;
    /** 状态：0未开始 1进行中 2已结束 */
    private String status;
    private Date createTime;

    /** 非DB，关联商品名称（后台列表展示用） */
    private String productName;

    public Long getSaleId() { return saleId; }
    public void setSaleId(Long saleId) { this.saleId = saleId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public BigDecimal getFlashPrice() { return flashPrice; }
    public void setFlashPrice(BigDecimal flashPrice) { this.flashPrice = flashPrice; }

    public Integer getStockLimit() { return stockLimit; }
    public void setStockLimit(Integer stockLimit) { this.stockLimit = stockLimit; }

    public Integer getSoldCount() { return soldCount; }
    public void setSoldCount(Integer soldCount) { this.soldCount = soldCount; }

    public Integer getPerLimit() { return perLimit; }
    public void setPerLimit(Integer perLimit) { this.perLimit = perLimit; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
}