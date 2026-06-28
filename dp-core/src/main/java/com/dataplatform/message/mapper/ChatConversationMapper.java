package com.dataplatform.message.mapper;

import com.dataplatform.message.entity.ChatConversation;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 会话 Mapper 接口
 */
public interface ChatConversationMapper {

    ChatConversation selectById(Long id);

    int insert(ChatConversation conversation);

    int update(ChatConversation conversation);

    int deleteById(Long id);

    /**
     * 查询用户的会话列表，按 lastMessageTime 降序
     */
    List<ChatConversation> selectByUserId(@Param("userId") Long userId);

    /**
     * 更新会话最后消息时间
     */
    int updateLastMessageTime(@Param("id") Long id);
}
