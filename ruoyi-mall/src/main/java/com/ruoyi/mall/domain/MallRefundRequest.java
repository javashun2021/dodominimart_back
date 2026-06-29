package com.ruoyi.mall.domain;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 退款申请（售后）mall_refund_request
 * 顾客对「已完成」订单发起，后台审核通过则走退款。
 */
public class MallRefundRequest
{
    private Long requestId;
    private Long orderId;
    private Long memberId;
    private String reason;
    /** 凭证图 URL，逗号分隔（可选） */
    private String images;
    /** PENDING / APPROVED / REJECTED */
    private String status;
    private String adminRemark;
    private String handleBy;
    private Date handleTime;
    private Date createTime;

    // —— 以下为列表/详情展示用的连表字段（非本表列）——
    private String orderNo;
    private BigDecimal amount;
    private String memberNick;

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminRemark() { return adminRemark; }
    public void setAdminRemark(String adminRemark) { this.adminRemark = adminRemark; }

    public String getHandleBy() { return handleBy; }
    public void setHandleBy(String handleBy) { this.handleBy = handleBy; }

    public Date getHandleTime() { return handleTime; }
    public void setHandleTime(Date handleTime) { this.handleTime = handleTime; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getMemberNick() { return memberNick; }
    public void setMemberNick(String memberNick) { this.memberNick = memberNick; }
}
