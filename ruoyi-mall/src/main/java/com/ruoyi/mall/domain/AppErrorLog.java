package com.ruoyi.mall.domain;

import com.ruoyi.common.base.BaseEntity;

/**
 * App 客户端错误上报记录
 * 由 /api/v1/app/error-log 接口写入，后台「App错误日志」菜单查看
 */
public class AppErrorLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long   id;
    /** 平台 android/ios/web */
    private String platform;
    /** App 版本 如 1.2.0+8 */
    private String appVersion;
    /** 类型 flutter/async/manual */
    private String type;
    /** 错误摘要 */
    private String message;
    /** 完整堆栈 */
    private String stack;
    /** 上下文 */
    private String context;
    /** 上报会员ID（匿名为空）*/
    private Long   memberId;
    /** 客户端IP */
    private String ip;
    /** App 上报时间（原始字符串）*/
    private String clientTime;

    public Long   getId()                 { return id; }
    public void   setId(Long v)           { id = v; }
    public String getPlatform()           { return platform; }
    public void   setPlatform(String v)   { platform = v; }
    public String getAppVersion()         { return appVersion; }
    public void   setAppVersion(String v) { appVersion = v; }
    public String getType()               { return type; }
    public void   setType(String v)       { type = v; }
    public String getMessage()            { return message; }
    public void   setMessage(String v)    { message = v; }
    public String getStack()              { return stack; }
    public void   setStack(String v)      { stack = v; }
    public String getContext()            { return context; }
    public void   setContext(String v)    { context = v; }
    public Long   getMemberId()           { return memberId; }
    public void   setMemberId(Long v)     { memberId = v; }
    public String getIp()                 { return ip; }
    public void   setIp(String v)         { ip = v; }
    public String getClientTime()         { return clientTime; }
    public void   setClientTime(String v) { clientTime = v; }
}
