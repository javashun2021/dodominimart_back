package com.ruoyi.mall.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.mall.domain.MallChatConversation;

public interface MallChatConversationMapper
{
    MallChatConversation selectById(Long conversationId);

    /** 按成员对查会话（a 恒较小、b 恒较大） */
    MallChatConversation selectByPair(@Param("a") Long a, @Param("b") Long b);

    int insert(MallChatConversation conversation);

    /** 发消息后：更新预览+时间，给接收方未读+1，并把双方软隐藏复位（会话复现） */
    int touchOnSend(@Param("conversationId") Long conversationId,
                    @Param("lastText") String lastText,
                    @Param("lastTime") java.util.Date lastTime,
                    @Param("recipientId") Long recipientId);

    /** 某成员已读：把其未读清零 */
    int clearUnread(@Param("conversationId") Long conversationId,
                    @Param("memberId") Long memberId);

    /** 我的会话列表（对方昵称/头像、我的未读、最后预览），排除我已软隐藏的 */
    List<Map<String, Object>> selectConversationsForMember(@Param("memberId") Long memberId);

    /** 我的全部未读总数 */
    int selectUnreadTotal(@Param("memberId") Long memberId);
}
