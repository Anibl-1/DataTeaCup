package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.AiChatHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * AI对话历史Mapper
 */
@Mapper
public interface AiChatHistoryMapper {
    
    @Insert("INSERT INTO ai_chat_history (session_id, user_id, role, content, data_source_id, message_type, metadata, create_time) " +
            "VALUES (#{sessionId}, #{userId}, #{role}, #{content}, #{dataSourceId}, #{messageType}, #{metadata}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiChatHistory history);
    
    @Select("SELECT session_id FROM ai_chat_history WHERE user_id = #{userId} " +
            "GROUP BY session_id ORDER BY MAX(create_time) DESC LIMIT #{limit}")
    List<String> getUserSessions(@Param("userId") Long userId, @Param("limit") Integer limit);
    
    @Select("SELECT id, session_id as sessionId, user_id as userId, role, content, data_source_id as dataSourceId, " +
            "message_type as messageType, metadata, create_time as createTime FROM ai_chat_history " +
            "WHERE session_id = #{sessionId} ORDER BY create_time ASC")
    List<AiChatHistory> getSessionMessages(@Param("sessionId") String sessionId);
    
    @Select("SELECT id, session_id as sessionId, user_id as userId, role, content, data_source_id as dataSourceId, " +
            "message_type as messageType, metadata, create_time as createTime FROM ai_chat_history " +
            "WHERE session_id = #{sessionId} ORDER BY create_time DESC LIMIT #{limit}")
    List<AiChatHistory> getRecentMessages(@Param("sessionId") String sessionId, @Param("limit") Integer limit);

    @Select("<script>" +
            "SELECT t.id, t.session_id as sessionId, t.user_id as userId, t.role, t.content, " +
            "t.data_source_id as dataSourceId, t.message_type as messageType, t.metadata, t.create_time as createTime " +
            "FROM ai_chat_history t INNER JOIN (" +
            "  SELECT MIN(id) as min_id FROM ai_chat_history WHERE session_id IN " +
            "  <foreach collection='sessionIds' item='sid' open='(' separator=',' close=')'>#{sid}</foreach>" +
            "  GROUP BY session_id" +
            ") m ON t.id = m.min_id" +
            "</script>")
    List<AiChatHistory> getFirstMessagesBySessionIds(@Param("sessionIds") List<String> sessionIds);
}
