package com.ruoyi.mall.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 订单表 mall_order
 */
public class MallOrder implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String orderNo;
    private Long memberId;
    /** 下单时地址快照（JSON 字符串） */
    private String addressSnapshot;
    private BigDecimal totalAmount;
    /**
     * 状态：0待确认 1已确认 2配送中 3已完成 4已取消
     */
    private String status;
    private String remark;
    /** 支付方式：COD / GCASH */
    private String paymentMethod;
    /** 支付状态：UNPAID / PAID / REFUNDED */
    private String paymentStatus;
    /** GCash Reference ID */
    private String paymentNo;
    private BigDecimal paidAmount;
    private Date paymentTime;
    private String cancelReason;
    private Date createTime;
    private String updateBy;
    private Date updateTime;

    /** 非数据库字段，查询详情时填充 */
    private List<MallOrderItem> items;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getAddressSnapshot() { return addressSnapshot; }
    public void setAddressSnapshot(String addressSnapshot) { this.addressSnapshot = addressSnapshot; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentNo() { return paymentNo; }
    public void setPaymentNo(String paymentNo) { this.paymentNo = paymentNo; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public Date getPaymentTime() { return paymentTime; }
    public void setPaymentTime(Date paymentTime) { this.paymentTime = paymentTime; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    public List<MallOrderItem> getItems() { return items; }
    public void setItems(List<MallOrderItem> items) { this.items = items; }
}
