package com.ruoyi.mall.domain;

import java.util.Date;

public class MallVerifyCode
{
    private Long   id;
    private String email;
    private String code;
    private Date   expiresAt;
    private Integer used;
    private Integer attempts;
    private Date   createTime;

    public Long    getId()              { return id; }
    public void    setId(Long v)        { this.id = v; }

    public String  getEmail()           { return email; }
    public void    setEmail(String v)   { this.email = v; }

    public String  getCode()            { return code; }
    public void    setCode(String v)    { this.code = v; }

    public Date    getExpiresAt()       { return expiresAt; }
    public void    setExpiresAt(Date v) { this.expiresAt = v; }

    public Integer getUsed()            { return used; }
    public void    setUsed(Integer v)   { this.used = v; }

    public Integer getAttempts()        { return attempts; }
    public void    setAttempts(Integer v){ this.attempts = v; }

    public Date    getCreateTime()      { return createTime; }
    public void    setCreateTime(Date v){ this.createTime = v; }
}
