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

    /** 品牌名（叠在商品图片上展示，可空） */
    @Excel(name = "品牌")
    private String brandName;

    private String description;

    @Excel(name = "售价(PHP)")
    private BigDecimal price;

    @Excel(name = "库存")
    private Integer stock;

    private String imageUrl;

    /** 附加图（封面之外的图），逗号分隔的 URL；App 详情页图廊 = [封面] + images */
    private String images;

    /** 单层属性名，如「口味」（空=无属性） */
    @Excel(name = "属性名")
    private String specName;

    /** 属性选项，逗号分隔，如「苹果味,水蜜桃味,原味」 */
    private String specOptions;

    /** 库内条码/SKU（Code128，标签打印 + POS 扫码用） */
    @Excel(name = "条码")
    private String barcode;

    /** 状态（0上架 1下架） */
    @Excel(name = "状态", readConverterExp = "0=上架,1=下架")
    private String status;

    private Integer sort;

    /** 首页精选（0否 1是），App 首页 Featured 板块只展示 is_featured=1 的商品 */
    @Excel(name = "首页精选", readConverterExp = "0=否,1=是")
    private Integer isFeatured;

    /** 归属商家ID（NULL=平台自营；非空=某入驻商家的商品） */
    private Long merchantId;

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
    /** 非DB：排序方式（price_asc/price_desc/newest） */
    private String sortBy;
    /** 非DB：只看限时特惠 */
    private boolean onlyFlashSale;
    /** 非DB：只看拼团 */
    private boolean onlyGroupBuy;
    /** 非DB：只看有货 */
    private boolean inStockOnly;
    /** 非DB：只看平台自营（merchant_id IS NULL），App 自营首页/列表用 */
    private boolean selfOperatedOnly;
    /** 非DB：所属店铺名（跨店聚合接口 /products/nearby 填充；App 卡片显示「店名·距离」） */
    private String storeName;
    /** 非DB：所属店铺距离(km)（跨店聚合接口填充；自营为 0/null 排最前） */
    private BigDecimal storeDistanceKm;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }

    public String getSpecName() { return specName; }
    public void setSpecName(String specName) { this.specName = specName; }

    public String getSpecOptions() { return specOptions; }
    public void setSpecOptions(String specOptions) { this.specOptions = specOptions; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }

    public Integer getIsFeatured() { return isFeatured; }
    public void setIsFeatured(Integer isFeatured) { this.isFeatured = isFeatured; }

    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }

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

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public boolean isOnlyFlashSale() { return onlyFlashSale; }
    public void setOnlyFlashSale(boolean onlyFlashSale) { this.onlyFlashSale = onlyFlashSale; }

    public boolean isOnlyGroupBuy() { return onlyGroupBuy; }
    public void setOnlyGroupBuy(boolean onlyGroupBuy) { this.onlyGroupBuy = onlyGroupBuy; }

    public boolean isInStockOnly() { return inStockOnly; }
    public void setInStockOnly(boolean inStockOnly) { this.inStockOnly = inStockOnly; }

    public boolean isSelfOperatedOnly() { return selfOperatedOnly; }
    public void setSelfOperatedOnly(boolean selfOperatedOnly) { this.selfOperatedOnly = selfOperatedOnly; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public BigDecimal getStoreDistanceKm() { return storeDistanceKm; }
    public void setStoreDistanceKm(BigDecimal storeDistanceKm) { this.storeDistanceKm = storeDistanceKm; }
}
