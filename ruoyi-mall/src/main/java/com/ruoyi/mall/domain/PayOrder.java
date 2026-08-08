package com.ruoyi.mall.domain;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 聚合支付下游商户订单 pay_order。
 */
public class PayOrder
{
    private Long id;
    /** 平台订单号 payOrderNo（OL...） */
    private String platformNo;
    private String merchantId;
    /** 商户号/站点码 */
    private String merchantCode;
    /** 通道编码 */
    private String channelId;
    /** 商户订单号 orderNo */
    private String outTradeNo;
    private BigDecimal amount;
    private String currency;
    private String userId;
    private String clientIp;
    private String extra;
    private String notifyUrl;
    private String requestId;
    private String payUrl;
    private String upstreamNo;
    /** CREATED / PAID / CLOSED */
    private String status;
    /** 0未通知 1成功 2失败 */
    private Integer notifyStatus;
    private Integer notifyCount;
    private Date lastNotifyTime;
    private String upstreamRaw;
    private Date createTime;
    private Date updateTime;
    private Date payTime;

    public boolean isPaid() { return "PAID".equalsIgnoreCase(status); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlatformNo() { return platformNo; }
    public void setPlatformNo(String platformNo) { this.platformNo = platformNo; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public String getMerchantCode() { return merchantCode; }
    public void setMerchantCode(String merchantCode) { this.merchantCode = merchantCode; }

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }

    public String getOutTradeNo() { return outTradeNo; }
    public void setOutTradeNo(String outTradeNo) { this.outTradeNo = outTradeNo; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public String getExtra() { return extra; }
    public void setExtra(String extra) { this.extra = extra; }

    public String getNotifyUrl() { return notifyUrl; }
    public void setNotifyUrl(String notifyUrl) { this.notifyUrl = notifyUrl; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getPayUrl() { return payUrl; }
    public void setPayUrl(String payUrl) { this.payUrl = payUrl; }

    public String getUpstreamNo() { return upstreamNo; }
    public void setUpstreamNo(String upstreamNo) { this.upstreamNo = upstreamNo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getNotifyStatus() { return notifyStatus; }
    public void setNotifyStatus(Integer notifyStatus) { this.notifyStatus = notifyStatus; }

    public Integer getNotifyCount() { return notifyCount; }
    public void setNotifyCount(Integer notifyCount) { this.notifyCount = notifyCount; }

    public Date getLastNotifyTime() { return lastNotifyTime; }
    public void setLastNotifyTime(Date lastNotifyTime) { this.lastNotifyTime = lastNotifyTime; }

    public String getUpstreamRaw() { return upstreamRaw; }
    public void setUpstreamRaw(String upstreamRaw) { this.upstreamRaw = upstreamRaw; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    public Date getPayTime() { return payTime; }
    public void setPayTime(Date payTime) { this.payTime = payTime; }
}
