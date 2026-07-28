package com.ruoyi.mall.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.mall.domain.MallChatMessage;

public interface MallChatMessageMapper
{
    int insert(MallChatMessage message);

    /** 幂等去重：同一会话同一发送者同一 clientMsgId 已存在则返回 */
    MallChatMessage selectByClientMsgId(@Param("conversationId") Long conversationId,
                                        @Param("senderId") Long senderId,
                                        @Param("clientMsgId") String clientMsgId);

    MallChatMessage selectById(Long messageId);

    /** 历史消息（message_id 逆序翻页）；beforeId 为空则取最新一页 */
    List<MallChatMessage> selectHistory(@Param("conversationId") Long conversationId,
                                        @Param("beforeId") Long beforeId,
                                        @Param("limit") int limit);

    /** 接收方读全部：is_read=1, read_time=now */
    int markRead(@Param("conversationId") Long conversationId,
                 @Param("recipientId") Long recipientId);
}
