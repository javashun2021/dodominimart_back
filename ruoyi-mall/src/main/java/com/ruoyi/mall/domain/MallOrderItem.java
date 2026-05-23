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
    /** 下单时单价（快照） */
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;

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

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
