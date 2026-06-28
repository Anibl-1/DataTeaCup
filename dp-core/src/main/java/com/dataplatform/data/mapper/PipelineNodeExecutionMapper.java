package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.PipelineNodeExecution;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 流程节点执行记录Mapper
 */
@Mapper
public interface PipelineNodeExecutionMapper {

    @Select("SELECT * FROM pipeline_node_execution WHERE execution_id = #{executionId} ORDER BY create_time")
    List<PipelineNodeExecution> selectByExecutionId(@Param("executionId") Long executionId);

    @Select("SELECT * FROM pipeline_node_execution WHERE id = #{id}")
    PipelineNodeExecution selectById(@Param("id") Long id);

    @Insert("INSERT INTO pipeline_node_execution (execution_id, node_code, node_name, status, start_time, end_time, " +
            "duration, input_count, output_count, error_message, create_time) " +
            "VALUES (#{executionId}, #{nodeCode}, #{nodeName}, #{status}, #{startTime}, #{endTime}, " +
            "#{duration}, #{inputCount}, #{outputCount}, #{errorMessage}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PipelineNodeExecution record);

    @Update("UPDATE pipeline_node_execution SET status = #{status}, end_time = #{endTime}, " +
            "duration = #{duration}, output_count = #{outputCount}, error_message = #{errorMessage} " +
            "WHERE id = #{id}")
    int updateStatus(PipelineNodeExecution record);

    @Delete("DELETE FROM pipeline_node_execution WHERE execution_id = #{executionId}")
    int deleteByExecutionId(@Param("executionId") Long executionId);
}
