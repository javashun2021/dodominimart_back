package com.ruoyi.mall.domain;

import java.util.Date;
import com.ruoyi.common.base.BaseEntity;

/**
 * Android 封闭测试「内测申请」记录
 * 由 /api/v1/beta/apply 写入，/api/v1/beta/approve 审批通过。
 * createTime / remark 继承自 BaseEntity。
 */
public class MallBetaTester extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    public static final String STATUS_PENDING  = "pending";
    public static final String STATUS_APPROVED = "approved";

    private Long   id;
    /** 测试者 Gmail（小写存储） */
    private String email;
    /** 状态 pending / approved */
    private String status;
    /** 申请来源 web 等 */
    private String source;
    /** 已登录会员ID（可空） */
    private Long   memberId;
    /** 申请IP */
    private String ip;
    /** 审批通过时间 */
    private Date   approveTime;

    public Long   getId()                  { return id; }
    public void   setId(Long v)            { id = v; }
    public String getEmail()               { return email; }
    public void   setEmail(String v)       { email = v; }
    public String getStatus()              { return status; }
    public void   setStatus(String v)      { status = v; }
    public String getSource()              { return source; }
    public void   setSource(String v)      { source = v; }
    public Long   getMemberId()            { return memberId; }
    public void   setMemberId(Long v)      { memberId = v; }
    public String getIp()                  { return ip; }
    public void   setIp(String v)          { ip = v; }
    public Date   getApproveTime()         { return approveTime; }
    public void   setApproveTime(Date v)   { approveTime = v; }
}
