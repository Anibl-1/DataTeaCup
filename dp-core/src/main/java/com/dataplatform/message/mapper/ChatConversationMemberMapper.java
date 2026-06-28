package com.dataplatform.message.mapper;

import com.dataplatform.message.entity.ChatConversationMember;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 会话成员 Mapper 接口
 */
public interface ChatConversationMemberMapper {

    int insert(ChatConversationMember member);

    int batchInsert(@Param("members") List<ChatConversationMember> members);

    int deleteByConversationId(@Param("conversationId") Long conversationId);

    List<ChatConversationMember> selectByConversationId(@Param("conversationId") Long conversationId);

    List<ChatConversationMember> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询两个用户之间的私聊会话 ID
     */
    Long selectPrivateConversationId(@Param("userIdA") Long userIdA, @Param("userIdB") Long userIdB);

    /**
     * 增加未读消息数
     */
    int incrementUnreadCount(@Param("conversationId") Long conversationId, @Param("excludeUserId") Long excludeUserId);

    /**
     * 重置未读消息数
     */
    int resetUnreadCount(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    /**
     * 查询会话成员（关联用户表获取昵称）
     */
    List<ChatConversationMember> selectMembersWithNickname(@Param("conversationId") Long conversationId);
}
