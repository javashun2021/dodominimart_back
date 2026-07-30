package com.ruoyi.mall.domain;

import java.util.Date;

/**
 * 门店级商品库存覆盖 mall_product_stock
 *
 * 无行 = 该门店对该商品用商户总库存(mall_product.stock)；
 * 有行 = 该门店优先用此独立库存。取数/扣减 COALESCE(覆盖, 总库存)。
 */
public class MallProductStock
{
    private Long productId;
    private Long storeId;
    private Integer stock;
    private Date createTime;
    private Date updateTime;

    /** 非DB：商品名（后台按店配库存页展示） */
    private String productName;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
}
