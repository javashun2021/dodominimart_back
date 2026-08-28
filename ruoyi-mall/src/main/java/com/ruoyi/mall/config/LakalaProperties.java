package com.ruoyi.mall.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 拉卡拉收银台（线上 H5）支付配置。
 * 对应 application.yml 里的 mall.lakala.* 。
 */
@Component
@ConfigurationProperties(prefix = "mall.lakala")
public class LakalaProperties
{
    /** 总开关：true 才启用拉卡拉线上支付 */
    private boolean enabled;
    /** 接入方唯一编号 appId（拉卡拉分配） */
    private String appId;
    /** 商户加签证书序列号 serial_no */
    private String serialNo;
    /** 银联商户号 merchant_no */
    private String merchantNo;
    /** 终端号 term_no */
    private String termNo;
    /** 开放平台服务地址（测试 https://test.wsmsd.cn/sit） */
    private String serverUrl;
    /** SM4 报文加密密钥（仅全报文加密接口需要，收银台标准下单可留空） */
    private String sm4Key;
    /** 商户私钥文件路径（用于请求加签） */
    private String priKeyPath;
    /** 拉卡拉公钥证书路径（用于响应验签） */
    private String lklCerPath;
    /** 拉卡拉通知验签证书路径（当前同 lklCerPath） */
    private String lklNotifyCerPath;
    /** 支付成功异步通知地址（拉卡拉回调本服务） */
    private String notifyUrl;
    /** 付款完成后浏览器回跳地址（H5 落地页） */
    private String callbackUrl;
    /** 订单有效期（分钟），默认 30 */
    private int orderEfficientMinutes = 30;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getSerialNo() { return serialNo; }
    public void setSerialNo(String serialNo) { this.serialNo = serialNo; }

    public String getMerchantNo() { return merchantNo; }
    public void setMerchantNo(String merchantNo) { this.merchantNo = merchantNo; }

    public String getTermNo() { return termNo; }
    public void setTermNo(String termNo) { this.termNo = termNo; }

    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }

    public String getSm4Key() { return sm4Key; }
    public void setSm4Key(String sm4Key) { this.sm4Key = sm4Key; }

    public String getPriKeyPath() { return priKeyPath; }
    public void setPriKeyPath(String priKeyPath) { this.priKeyPath = priKeyPath; }

    public String getLklCerPath() { return lklCerPath; }
    public void setLklCerPath(String lklCerPath) { this.lklCerPath = lklCerPath; }

    public String getLklNotifyCerPath() { return lklNotifyCerPath; }
    public void setLklNotifyCerPath(String lklNotifyCerPath) { this.lklNotifyCerPath = lklNotifyCerPath; }

    public String getNotifyUrl() { return notifyUrl; }
    public void setNotifyUrl(String notifyUrl) { this.notifyUrl = notifyUrl; }

    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }

    public int getOrderEfficientMinutes() { return orderEfficientMinutes; }
    public void setOrderEfficientMinutes(int orderEfficientMinutes) { this.orderEfficientMinutes = orderEfficientMinutes; }
}
