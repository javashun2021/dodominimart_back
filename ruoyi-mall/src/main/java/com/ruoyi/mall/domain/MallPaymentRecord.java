package com.ruoyi.mall.domain;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付流水表 mall_payment_record
 */
public class MallPaymentRecord
{
    private Long recordId;
    private Long orderId;
    private Long memberId;
    private BigDecimal amount;
    private String paymentNo;
    private String gcashRaw;
    /** PENDING / SUCCESS / FAILED / REFUNDED */
    private String status;
    private Date createTime;
    private Date updateTime;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentNo() { return paymentNo; }
    public void setPaymentNo(String paymentNo) { this.paymentNo = paymentNo; }

    public String getGcashRaw() { return gcashRaw; }
    public void setGcashRaw(String gcashRaw) { this.gcashRaw = gcashRaw; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
