package com.ruoyi.mall.domain;

import java.io.Serializable;
import java.util.Date;

public class MallPointsLog implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long    logId;
    private Long    memberId;
    private int     delta;
    private int     balanceAfter;
    /** 1=订单完成 2=评价奖励 3=注册赠送 4=积分抵扣 */
    private int     source;
    private String  sourceId;
    private String  remark;
    private Date    createTime;

    public Long   getLogId()       { return logId;       }
    public void   setLogId(Long v)  { this.logId = v;     }

    public Long   getMemberId()         { return memberId;        }
    public void   setMemberId(Long v)   { this.memberId = v;      }

    public int    getDelta()        { return delta;       }
    public void   setDelta(int v)   { this.delta = v;     }

    public int    getBalanceAfter()       { return balanceAfter;       }
    public void   setBalanceAfter(int v)  { this.balanceAfter = v;     }

    public int    getSource()       { return source;      }
    public void   setSource(int v)  { this.source = v;    }

    public String getSourceId()         { return sourceId;        }
    public void   setSourceId(String v) { this.sourceId = v;      }

    public String getRemark()           { return remark;          }
    public void   setRemark(String v)   { this.remark = v;        }

    public Date   getCreateTime()         { return createTime;        }
    public void   setCreateTime(Date v)   { this.createTime = v;      }
}
