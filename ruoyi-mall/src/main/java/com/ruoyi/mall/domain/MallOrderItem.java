package com.ruoyi.mall.domain;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细表 mall_order_item
 */
public class MallOrderItem implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long itemId;
    private Long orderId;
    private Long productId;
    /** 商品名称（快照） */
    private String productName;
    /** 商品图片（快照） */
    private String productImage;
    /** 下单时实付单价（快照：限时价/拼团价/原价） */
    private BigDecimal price;
    /** 下单时商品原价（快照，用于展示划线价/优惠额；可为空） */
    private BigDecimal originalPrice;
    private Integer quantity;
    private BigDecimal subtotal;
    /** 下单所选属性值快照，如「苹果味」（可空） */
    private String spec;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
}
