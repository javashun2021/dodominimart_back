package com.ruoyi.mall.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * POS 暂存挂单 mall_pos_held_cart
 * 存的是"购物车"(商品ID+数量)，不是订单：不占库存、不动积分；恢复时按当前价/库存重算。
 */
public class MallPosHeldCart implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long       id;
    /** 暂存的收银员 memberId */
    private Long       cashierId;
    /** 关联会员(可空=散客) */
    private Long       memberId;
    /** 暂存标签：顾客名/手机号/自动序号 */
    private String     label;
    /** 购物车明细 JSON：[{productId,quantity}] */
    private String     cartJson;
    /** 件数(展示用) */
    private Integer    itemCount;
    /** 总额快照(仅列表展示，结算按当前价重算) */
    private BigDecimal totalAmount;
    private String     remark;
    /** 0暂存中 1已恢复结算 2作废 */
    private String     status;
    private Date       createTime;
    private Date       updateTime;

    public Long getId()             { return id; }
    public void setId(Long v)       { this.id = v; }

    public Long getCashierId()       { return cashierId; }
    public void setCashierId(Long v) { this.cashierId = v; }

    public Long getMemberId()        { return memberId; }
    public void setMemberId(Long v)  { this.memberId = v; }

    public String getLabel()         { return label; }
    public void   setLabel(String v) { this.label = v; }

    public String getCartJson()         { return cartJson; }
    public void   setCartJson(String v) { this.cartJson = v; }

    public Integer getItemCount()        { return itemCount; }
    public void    setItemCount(Integer v) { this.itemCount = v; }

    public BigDecimal getTotalAmount()             { return totalAmount; }
    public void       setTotalAmount(BigDecimal v) { this.totalAmount = v; }

    public String getRemark()         { return remark; }
    public void   setRemark(String v) { this.remark = v; }

    public String getStatus()         { return status; }
    public void   setStatus(String v) { this.status = v; }

    public Date getCreateTime()       { return createTime; }
    public void setCreateTime(Date v) { this.createTime = v; }

    public Date getUpdateTime()       { return updateTime; }
    public void setUpdateTime(Date v) { this.updateTime = v; }
}
