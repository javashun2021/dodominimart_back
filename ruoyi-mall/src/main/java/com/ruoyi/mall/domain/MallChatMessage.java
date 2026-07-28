package com.ruoyi.mall.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 站内聊天消息 mall_chat_message
 * content 按 contentType 存不同内容：text=正文 / sticker=表情code / image=图片URL。
 */
public class MallChatMessage implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long messageId;
    private Long conversationId;
    private Long senderId;
    private Long recipientId;
    /** 客户端生成ID（ack 对齐/去重） */
    private String clientMsgId;
    /** 类型：text / image / sticker */
    private String contentType;
    /** text=正文 / sticker=code / image=URL */
    private String content;
    /** 引用的市场帖ID（可空） */
    private Long refPostId;
    private Integer isRead;
    private Date readTime;
    private Date createTime;

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public String getClientMsgId() { return clientMsgId; }
    public void setClientMsgId(String clientMsgId) { this.clientMsgId = clientMsgId; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getRefPostId() { return refPostId; }
    public void setRefPostId(Long refPostId) { this.refPostId = refPostId; }

    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }

    public Date getReadTime() { return readTime; }
    public void setReadTime(Date readTime) { this.readTime = readTime; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
