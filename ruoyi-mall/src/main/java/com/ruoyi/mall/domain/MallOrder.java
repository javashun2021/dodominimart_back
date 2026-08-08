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
    /** 来源：NORMAL / FLASH_SALE / GROUP */
    private String orderSource;
    private Date createTime;
    private String updateBy;
    private Date updateTime;

    /** 接单跑腿人ID */
    private Long runnerMemberId;
    /** 接单时间 */
    private Date runnerAcceptedTime;
    /** 跑腿费（PHP） */
    private java.math.BigDecimal deliveryFee;
    /** 跑腿费是否已结算 0否 1是（仅GCash订单需关注） */
    private String runnerFeeSettled;

    /** 本单使用积分数 */
    private int pointsUsed;

    /** 本单使用的会员券实例 ID */
    private Long memberCouponId;

    /** 优惠券减免金额 */
    private java.math.BigDecimal couponDiscount;

    /** POS 到店实付方式：CASH/GCASH（阶段1 仅 CASH） */
    private String tenderType;
    /** POS 收现金额 */
    private java.math.BigDecimal cashReceived;
    /** POS 找零 */
    private java.math.BigDecimal changeDue;
    /** POS 开单收银员 memberId */
    private Long cashierId;

    /** 归属门店ID（模型C：下单时固化，决定骑手抢单池/发货网点） */
    private Long storeId;
    /** 非DB：门店名称（列表/详情展示用） */
    private String storeName;

    /** 非数据库字段，查询详情时填充 */
    private List<MallOrderItem> items;
    /** 非DB：接单 runner 的手机号（详情接口填充） */
    private String runnerPhone;
    /** 非DB：本单投递联系电话（详情接口填充，App 展示用） */
    private String customerPhone;
    /** 非DB：顾客姓名/昵称（runner 列表填充，App 展示用） */
    private String customerName;

    /** 聚合支付：下游商户订单号 */
    private String merchantOutTradeNo;
    /** 聚合支付：平台补贴（名义额 - 实付MOSS额） */
    private BigDecimal subsidy;

    public String getMerchantOutTradeNo() { return merchantOutTradeNo; }
    public void setMerchantOutTradeNo(String merchantOutTradeNo) { this.merchantOutTradeNo = merchantOutTradeNo; }

    public BigDecimal getSubsidy() { return subsidy; }
    public void setSubsidy(BigDecimal subsidy) { this.subsidy = subsidy; }

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

    public String getOrderSource() { return orderSource; }
    public void setOrderSource(String orderSource) { this.orderSource = orderSource; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    // —— 退款申请（售后）展示用，非本表列，仅在订单详情接口里临时填充 ——
    /** 最新退款申请状态：PENDING / APPROVED / REJECTED；无申请则 null */
    private String refundStatus;
    /** 审核备注（驳回原因等） */
    private String refundRemark;
    /** 是否可发起退款申请（已完成 + 已付 + 3天内 + 无进行中申请） */
    private Boolean canRefund;

    public String getRefundStatus() { return refundStatus; }
    public void setRefundStatus(String refundStatus) { this.refundStatus = refundStatus; }

    public String getRefundRemark() { return refundRemark; }
    public void setRefundRemark(String refundRemark) { this.refundRemark = refundRemark; }

    public Boolean getCanRefund() { return canRefund; }
    public void setCanRefund(Boolean canRefund) { this.canRefund = canRefund; }

    public Long   getRunnerMemberId()        { return runnerMemberId; }
    public void   setRunnerMemberId(Long v)  { this.runnerMemberId = v; }

    public Date   getRunnerAcceptedTime()       { return runnerAcceptedTime; }
    public void   setRunnerAcceptedTime(Date v) { this.runnerAcceptedTime = v; }

    public java.math.BigDecimal getDeliveryFee()                         { return deliveryFee; }
    public void                 setDeliveryFee(java.math.BigDecimal v)   { this.deliveryFee = v; }

    public String getRunnerFeeSettled()        { return runnerFeeSettled; }
    public void   setRunnerFeeSettled(String v){ this.runnerFeeSettled = v; }

    public int  getPointsUsed()      { return pointsUsed; }
    public void setPointsUsed(int v) { this.pointsUsed = v; }

    public List<MallOrderItem> getItems() { return items; }
    public void setItems(List<MallOrderItem> items) { this.items = items; }

    public String getRunnerPhone()         { return runnerPhone; }
    public void   setRunnerPhone(String v) { this.runnerPhone = v; }

    public String getCustomerPhone()         { return customerPhone; }
    public void   setCustomerPhone(String v) { this.customerPhone = v; }

    public String getCustomerName()          { return customerName; }
    public void   setCustomerName(String v)  { this.customerName = v; }

    public Long   getMemberCouponId()              { return memberCouponId; }
    public void   setMemberCouponId(Long v)       { this.memberCouponId = v; }

    public java.math.BigDecimal getCouponDiscount()                              { return couponDiscount; }
    public void                 setCouponDiscount(java.math.BigDecimal v)       { this.couponDiscount = v; }

    public String getTenderType()         { return tenderType; }
    public void   setTenderType(String v) { this.tenderType = v; }

    public java.math.BigDecimal getCashReceived()                        { return cashReceived; }
    public void                 setCashReceived(java.math.BigDecimal v)  { this.cashReceived = v; }

    public java.math.BigDecimal getChangeDue()                           { return changeDue; }
    public void                 setChangeDue(java.math.BigDecimal v)      { this.changeDue = v; }

    public Long getCashierId()         { return cashierId; }
    public void setCashierId(Long v)   { this.cashierId = v; }

    public Long   getStoreId()         { return storeId; }
    public void   setStoreId(Long v)   { this.storeId = v; }

    public String getStoreName()         { return storeName; }
    public void   setStoreName(String v) { this.storeName = v; }
}
