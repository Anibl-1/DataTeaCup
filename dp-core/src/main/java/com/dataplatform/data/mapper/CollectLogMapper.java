package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.CollectLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CollectLogMapper {
    
    @Insert("INSERT INTO collect_log (task_id, task_name, source_table, target_table, status, row_count, start_time, end_time, duration, error_message, execute_sql, create_time) " +
            "VALUES (#{taskId}, #{taskName}, #{sourceTable}, #{targetTable}, #{status}, #{rowCount}, #{startTime}, #{endTime}, #{duration}, #{errorMessage}, #{executeSql}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CollectLog log);
    
    @Update("UPDATE collect_log SET status = #{status}, row_count = #{rowCount}, end_time = #{endTime}, duration = #{duration}, error_message = #{errorMessage} WHERE id = #{id}")
    int update(CollectLog log);
    
    @Select("<script>" +
            "SELECT * FROM collect_log " +
            "<where>" +
            "  <if test='taskId != null'>AND task_id = #{taskId}</if>" +
            "  <if test='status != null and status != \"\"'>AND status = #{status}</if>" +
            "  <if test='startDate != null'>AND start_time &gt;= #{startDate}</if>" +
            "  <if test='endDate != null'>AND start_time &lt;= #{endDate}</if>" +
            "</where>" +
            "ORDER BY id DESC LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<CollectLog> selectList(@Param("taskId") Long taskId, 
                                 @Param("status") String status,
                                 @Param("startDate") String startDate,
                                 @Param("endDate") String endDate,
                                 @Param("offset") int offset, 
                                 @Param("pageSize") int pageSize);
    
    @Select("<script>" +
            "SELECT COUNT(*) FROM collect_log " +
            "<where>" +
            "  <if test='taskId != null'>AND task_id = #{taskId}</if>" +
            "  <if test='status != null and status != \"\"'>AND status = #{status}</if>" +
            "  <if test='startDate != null'>AND start_time &gt;= #{startDate}</if>" +
            "  <if test='endDate != null'>AND start_time &lt;= #{endDate}</if>" +
            "</where>" +
            "</script>")
    long count(@Param("taskId") Long taskId, 
               @Param("status") String status,
               @Param("startDate") String startDate,
               @Param("endDate") String endDate);
    
    @Select("SELECT * FROM collect_log WHERE id = #{id}")
    CollectLog selectById(Long id);
    
    @Select("SELECT * FROM collect_log WHERE task_id = #{taskId} ORDER BY id DESC LIMIT #{limit}")
    List<CollectLog> selectByTaskId(@Param("taskId") Long taskId, @Param("limit") int limit);
}
