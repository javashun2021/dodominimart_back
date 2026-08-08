package com.ruoyi.mall.domain;

import java.util.Date;

/**
 * 商户表 imspay_merchant（下游接入平台的商户）。
 * code = 站点码(Request-Site-Code)，app_secret = MD5 签名 KEY。
 */
public class ImspayMerchant
{
    private String id;
    private String code;
    private String name;
    /** 签名密钥（下游 MD5 sign 用） */
    private String appSecret;
    private String privateKey;
    private String secret;
    /** 状态：启用 Enable / 停用 Disable */
    private String status;
    private Date createTime;
    private Date updateTime;
    private String summary;
    private String orderCountLimit;
    private String userDailyCountLimit;
    private String userDailySumLimit;
    private String orderSumLimit;
    private String depositBalance;
    private String dailyStartTime;
    private String dailyEndTime;
    private String telegramChatIds;

    public boolean isEnabled()
    {
        return "Enable".equalsIgnoreCase(status);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }

    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getOrderCountLimit() { return orderCountLimit; }
    public void setOrderCountLimit(String orderCountLimit) { this.orderCountLimit = orderCountLimit; }

    public String getUserDailyCountLimit() { return userDailyCountLimit; }
    public void setUserDailyCountLimit(String userDailyCountLimit) { this.userDailyCountLimit = userDailyCountLimit; }

    public String getUserDailySumLimit() { return userDailySumLimit; }
    public void setUserDailySumLimit(String userDailySumLimit) { this.userDailySumLimit = userDailySumLimit; }

    public String getOrderSumLimit() { return orderSumLimit; }
    public void setOrderSumLimit(String orderSumLimit) { this.orderSumLimit = orderSumLimit; }

    public String getDepositBalance() { return depositBalance; }
    public void setDepositBalance(String depositBalance) { this.depositBalance = depositBalance; }

    public String getDailyStartTime() { return dailyStartTime; }
    public void setDailyStartTime(String dailyStartTime) { this.dailyStartTime = dailyStartTime; }

    public String getDailyEndTime() { return dailyEndTime; }
    public void setDailyEndTime(String dailyEndTime) { this.dailyEndTime = dailyEndTime; }

    public String getTelegramChatIds() { return telegramChatIds; }
    public void setTelegramChatIds(String telegramChatIds) { this.telegramChatIds = telegramChatIds; }
}
