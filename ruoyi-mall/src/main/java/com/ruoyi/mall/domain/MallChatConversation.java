package com.ruoyi.mall.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 站内聊天会话 mall_chat_conversation
 * 一对成员一条；member_a_id 恒为较小 memberId、member_b_id 较大，保证一对一线程唯一。
 */
public class MallChatConversation implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long conversationId;
    private Long memberAId;
    private Long memberBId;
    /** 发起来源的市场帖ID（可空） */
    private Long originPostId;
    private String lastMessageText;
    private Date lastMessageTime;
    private Integer aUnread;
    private Integer bUnread;
    private String aDeleted;
    private String bDeleted;
    private Date createTime;
    private Date updateTime;

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public Long getMemberAId() { return memberAId; }
    public void setMemberAId(Long memberAId) { this.memberAId = memberAId; }

    public Long getMemberBId() { return memberBId; }
    public void setMemberBId(Long memberBId) { this.memberBId = memberBId; }

    public Long getOriginPostId() { return originPostId; }
    public void setOriginPostId(Long originPostId) { this.originPostId = originPostId; }

    public String getLastMessageText() { return lastMessageText; }
    public void setLastMessageText(String lastMessageText) { this.lastMessageText = lastMessageText; }

    public Date getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(Date lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public Integer getAUnread() { return aUnread; }
    public void setAUnread(Integer aUnread) { this.aUnread = aUnread; }

    public Integer getBUnread() { return bUnread; }
    public void setBUnread(Integer bUnread) { this.bUnread = bUnread; }

    public String getADeleted() { return aDeleted; }
    public void setADeleted(String aDeleted) { this.aDeleted = aDeleted; }

    public String getBDeleted() { return bDeleted; }
    public void setBDeleted(String bDeleted) { this.bDeleted = bDeleted; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
