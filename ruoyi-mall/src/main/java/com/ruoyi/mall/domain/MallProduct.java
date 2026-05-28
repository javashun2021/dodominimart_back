package com.ruoyi.mall.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.base.BaseEntity;
import com.ruoyi.mall.domain.MallGroupActivity;

/**
 * 商品表 mall_product
 */
public class MallProduct extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long productId;

    @Excel(name = "分类ID")
    private Integer categoryId;

    @Excel(name = "商品名称")
    private String name;

    private String description;

    @Excel(name = "售价(PHP)")
    private BigDecimal price;

    @Excel(name = "库存")
    private Integer stock;

    private String imageUrl;

    /** 状态（0上架 1下架） */
    @Excel(name = "状态", readConverterExp = "0=上架,1=下架")
    private String status;

    private Integer sort;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    /** 非DB：限时优惠活动价，无活动时为 null */
    private BigDecimal flashPrice;
    /** 非DB：限时优惠结束时间 */
    private Date flashSaleEndTime;
    /** 非DB：限时优惠剩余库存 */
    private Integer flashStockLeft;
    /** 非DB：限时优惠活动ID（下单时占库存用） */
    private Long flashSaleId;
    /** 非DB：当前进行中的拼团活动 */
    private MallGroupActivity groupActivity;
    /** 非DB：评价平均分（仅商品详情接口填充） */
    private java.math.BigDecimal avgScore;
    /** 非DB：评价总数 */
    private Integer reviewCount;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public BigDecimal getFlashPrice() { return flashPrice; }
    public void setFlashPrice(BigDecimal flashPrice) { this.flashPrice = flashPrice; }

    public Date getFlashSaleEndTime() { return flashSaleEndTime; }
    public void setFlashSaleEndTime(Date flashSaleEndTime) { this.flashSaleEndTime = flashSaleEndTime; }

    public Integer getFlashStockLeft() { return flashStockLeft; }
    public void setFlashStockLeft(Integer flashStockLeft) { this.flashStockLeft = flashStockLeft; }

    public Long getFlashSaleId() { return flashSaleId; }
    public void setFlashSaleId(Long flashSaleId) { this.flashSaleId = flashSaleId; }

    public MallGroupActivity getGroupActivity() { return groupActivity; }
    public void setGroupActivity(MallGroupActivity groupActivity) { this.groupActivity = groupActivity; }

    public java.math.BigDecimal getAvgScore() { return avgScore; }
    public void setAvgScore(java.math.BigDecimal avgScore) { this.avgScore = avgScore; }

    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
}
