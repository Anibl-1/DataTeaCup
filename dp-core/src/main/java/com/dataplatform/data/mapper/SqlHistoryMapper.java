package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.SqlHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * SQL执行历史Mapper
 */
@Mapper
public interface SqlHistoryMapper {

    @Insert("INSERT INTO sql_history (session_id, db_type, db_name, sql_content, sql_type, " +
            "status, affected_rows, execute_time, error_message, execute_at, create_time) " +
            "VALUES (#{sessionId}, #{dbType}, #{dbName}, #{sqlContent}, #{sqlType}, " +
            "#{status}, #{affectedRows}, #{executeTime}, #{errorMessage}, #{executeAt}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SqlHistory history);

    @Select("SELECT * FROM sql_history WHERE session_id = #{sessionId} " +
            "ORDER BY execute_at DESC LIMIT #{limit}")
    List<SqlHistory> selectBySessionId(@Param("sessionId") String sessionId, 
                                       @Param("limit") int limit);

    @Select("<script>" +
            "SELECT * FROM sql_history " +
            "<where>" +
            "  <if test='sessionId != null'>AND session_id = #{sessionId}</if>" +
            "  <if test='dbType != null'>AND db_type = #{dbType}</if>" +
            "  <if test='keyword != null'>AND sql_content LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "  <if test='status != null'>AND status = #{status}</if>" +
            "</where>" +
            "ORDER BY execute_at DESC LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<SqlHistory> selectList(@Param("sessionId") String sessionId,
                                @Param("dbType") String dbType,
                                @Param("keyword") String keyword,
                                @Param("status") String status,
                                @Param("offset") int offset,
                                @Param("pageSize") int pageSize);

    @Select("<script>" +
            "SELECT COUNT(*) FROM sql_history " +
            "<where>" +
            "  <if test='sessionId != null'>AND session_id = #{sessionId}</if>" +
            "  <if test='dbType != null'>AND db_type = #{dbType}</if>" +
            "  <if test='keyword != null'>AND sql_content LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "  <if test='status != null'>AND status = #{status}</if>" +
            "</where>" +
            "</script>")
    long count(@Param("sessionId") String sessionId,
               @Param("dbType") String dbType,
               @Param("keyword") String keyword,
               @Param("status") String status);

    @Delete("DELETE FROM sql_history WHERE session_id = #{sessionId}")
    int deleteBySessionId(@Param("sessionId") String sessionId);

    @Delete("DELETE FROM sql_history WHERE create_time < DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int deleteOlderThan(@Param("days") int days);
}
