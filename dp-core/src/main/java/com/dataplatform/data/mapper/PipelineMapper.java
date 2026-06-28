package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.Pipeline;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface PipelineMapper {
    
    @Select("SELECT * FROM data_pipeline WHERE del_flag = 0 ORDER BY create_time DESC")
    List<Pipeline> findAll();
    
    @Select("SELECT * FROM data_pipeline WHERE id = #{id} AND del_flag = 0")
    Pipeline findById(@Param("id") Long id);
    
    @Select("SELECT * FROM data_pipeline WHERE pipeline_code = #{code} AND del_flag = 0")
    Pipeline findByCode(@Param("code") String code);
    
    @Select("<script>" +
            "SELECT * FROM data_pipeline WHERE del_flag = 0 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (pipeline_name LIKE CONCAT('%',#{keyword},'%') OR pipeline_code LIKE CONCAT('%',#{keyword},'%'))" +
            "</if>" +
            "<if test='pipelineType != null'>AND pipeline_type = #{pipelineType}</if>" +
            "<if test='pipelineStatus != null'>AND pipeline_status = #{pipelineStatus}</if>" +
            "<if test='scheduleType != null'>AND schedule_type = #{scheduleType}</if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    List<Pipeline> search(@Param("keyword") String keyword, 
                          @Param("pipelineType") Integer pipelineType,
                          @Param("pipelineStatus") Integer pipelineStatus,
                          @Param("scheduleType") Integer scheduleType);
    
    @Insert("INSERT INTO data_pipeline (pipeline_name, pipeline_code, pipeline_desc, pipeline_type, " +
            "flow_json, cron_expression, schedule_type, pipeline_status, version, timeout_seconds, " +
            "retry_count, alert_on_failure, create_by) VALUES (#{pipelineName}, #{pipelineCode}, " +
            "#{pipelineDesc}, #{pipelineType}, #{flowJson}, #{cronExpression}, #{scheduleType}, " +
            "#{pipelineStatus}, #{version}, #{timeoutSeconds}, #{retryCount}, #{alertOnFailure}, #{createBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Pipeline pipeline);
    
    @Update("UPDATE data_pipeline SET pipeline_name = #{pipelineName}, pipeline_code = #{pipelineCode}, " +
            "pipeline_desc = #{pipelineDesc}, pipeline_type = #{pipelineType}, flow_json = #{flowJson}, " +
            "cron_expression = #{cronExpression}, schedule_type = #{scheduleType}, " +
            "timeout_seconds = #{timeoutSeconds}, retry_count = #{retryCount}, " +
            "alert_on_failure = #{alertOnFailure}, version = version + 1 WHERE id = #{id}")
    int update(Pipeline pipeline);
    
    @Update("UPDATE data_pipeline SET pipeline_status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    @Update("UPDATE data_pipeline SET last_execute_time = NOW(), last_execute_status = #{status} WHERE id = #{id}")
    int updateLastExecute(@Param("id") Long id, @Param("status") Integer status);
    
    @Update("UPDATE data_pipeline SET del_flag = 1 WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
    
    @Select("SELECT * FROM data_pipeline WHERE schedule_type = 1 AND pipeline_status = 1 AND del_flag = 0")
    List<Pipeline> findScheduledPipelines();
    
    @Select("SELECT COUNT(*) FROM data_pipeline WHERE del_flag = 0")
    int countAll();
    
    @Select("SELECT COUNT(*) FROM data_pipeline WHERE pipeline_status = #{status} AND del_flag = 0")
    int countByStatus(@Param("status") Integer status);
}
