package com.dataplatform.message.mapper;

import com.dataplatform.message.entity.ChatMessage;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 聊天消息 Mapper 接口
 */
public interface ChatMessageMapper {

    int insert(ChatMessage message);

    ChatMessage selectById(Long id);

    /**
     * 游标分页查询消息历史，按 sendTime 升序，每页 limit 条
     * cursor 为上一页最后一条消息的 id，首次查询传 null
     */
    List<ChatMessage> selectByConversationId(
            @Param("conversationId") Long conversationId,
            @Param("cursor") Long cursor,
            @Param("limit") int limit);

    /**
     * 查询会话最后一条消息的 content
     */
    String selectLastContent(@Param("conversationId") Long conversationId);
}
