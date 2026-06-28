package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.PipelineExecution;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface PipelineExecutionMapper {
    
    @Select("SELECT * FROM pipeline_execution ORDER BY create_time DESC LIMIT 100")
    List<PipelineExecution> findRecent();
    
    @Select("SELECT * FROM pipeline_execution WHERE id = #{id}")
    PipelineExecution findById(@Param("id") Long id);
    
    @Select("SELECT * FROM pipeline_execution WHERE pipeline_id = #{pipelineId} ORDER BY create_time DESC")
    List<PipelineExecution> findByPipelineId(@Param("pipelineId") Long pipelineId);
    
    @Select("<script>" +
            "SELECT * FROM pipeline_execution WHERE 1=1 " +
            "<if test='pipelineId != null'>AND pipeline_id = #{pipelineId}</if>" +
            "<if test='status != null'>AND status = #{status}</if>" +
            "<if test='triggerType != null'>AND trigger_type = #{triggerType}</if>" +
            "<if test='startDate != null'>AND start_time &gt;= #{startDate}</if>" +
            "<if test='endDate != null'>AND start_time &lt;= #{endDate}</if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    List<PipelineExecution> search(@Param("pipelineId") Long pipelineId,
                                   @Param("status") Integer status,
                                   @Param("triggerType") Integer triggerType,
                                   @Param("startDate") String startDate,
                                   @Param("endDate") String endDate);
    
    @Insert("INSERT INTO pipeline_execution (pipeline_id, pipeline_name, execution_no, trigger_type, " +
            "status, start_time, execute_by) VALUES (#{pipelineId}, #{pipelineName}, #{executionNo}, " +
            "#{triggerType}, #{status}, #{startTime}, #{executeBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PipelineExecution execution);
    
    @Update("UPDATE pipeline_execution SET status = #{status}, end_time = #{endTime}, " +
            "duration = #{duration}, input_count = #{inputCount}, output_count = #{outputCount}, " +
            "error_count = #{errorCount}, error_message = #{errorMessage}, execute_log = #{executeLog} " +
            "WHERE id = #{id}")
    int update(PipelineExecution execution);
    
    @Update("UPDATE pipeline_execution SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    @Update("UPDATE pipeline_execution SET execute_log = #{log} WHERE id = #{id}")
    int updateLog(@Param("id") Long id, @Param("log") String log);
    
    @Update("UPDATE pipeline_execution SET execute_log = CONCAT(IFNULL(execute_log,''), #{log}) WHERE id = #{id}")
    int appendLog(@Param("id") Long id, @Param("log") String log);
    
    @Delete("DELETE FROM pipeline_execution WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
    
    @Select("SELECT COUNT(*) FROM pipeline_execution")
    int countAll();
    
    @Select("SELECT COUNT(*) FROM pipeline_execution WHERE status = #{status}")
    int countByStatus(@Param("status") Integer status);
    
    @Select("SELECT COUNT(*) FROM pipeline_execution WHERE DATE(start_time) = CURDATE()")
    int countToday();
    
    @Select("SELECT * FROM pipeline_execution WHERE status = 2")
    List<PipelineExecution> findRunning();

    @Select("SELECT DATE(start_time) AS day, " +
            "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS success_count, " +
            "SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS failed_count " +
            "FROM pipeline_execution " +
            "WHERE start_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE(start_time) ORDER BY day")
    List<java.util.Map<String, Object>> countDailyTrend(@Param("days") int days);
}
