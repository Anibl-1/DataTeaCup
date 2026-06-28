package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.PipelineNode;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface PipelineNodeMapper {
    
    @Select("SELECT id, pipeline_id, node_code, node_name, node_type, node_config, " +
            "position_x, position_y, sort_order, pre_task_codes AS preTaskCodesStr, fail_strategy, " +
            "timeout_flag, timeout_seconds, timeout_strategy, retry_times, retry_interval, " +
            "priority, condition_type, condition_result, run_flag, is_enabled, description, create_time " +
            "FROM pipeline_node WHERE pipeline_id = #{pipelineId} ORDER BY sort_order")
    List<PipelineNode> findByPipelineId(@Param("pipelineId") Long pipelineId);
    
    @Select("SELECT * FROM pipeline_node WHERE id = #{id}")
    PipelineNode findById(@Param("id") Long id);
    
    @Select("SELECT * FROM pipeline_node WHERE pipeline_id = #{pipelineId} AND node_code = #{nodeCode}")
    PipelineNode findByCode(@Param("pipelineId") Long pipelineId, @Param("nodeCode") String nodeCode);
    
    @Insert("INSERT INTO pipeline_node (pipeline_id, node_code, node_name, node_type, node_config, " +
            "position_x, position_y, sort_order, pre_task_codes, fail_strategy, timeout_flag, " +
            "timeout_seconds, timeout_strategy, retry_times, retry_interval, priority, " +
            "condition_type, condition_result, run_flag, is_enabled, description) " +
            "VALUES (#{pipelineId}, #{nodeCode}, #{nodeName}, #{nodeType}, #{nodeConfig}, " +
            "#{positionX}, #{positionY}, #{sortOrder}, #{preTaskCodesStr}, #{failStrategy}, #{timeoutFlag}, " +
            "#{timeoutSeconds}, #{timeoutStrategy}, #{retryTimes}, #{retryInterval}, #{priority}, " +
            "#{conditionType}, #{conditionResult}, #{runFlag}, #{isEnabled}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PipelineNode node);
    
    @Update("UPDATE pipeline_node SET node_name = #{nodeName}, node_type = #{nodeType}, " +
            "node_config = #{nodeConfig}, position_x = #{positionX}, position_y = #{positionY}, " +
            "sort_order = #{sortOrder}, pre_task_codes = #{preTaskCodesStr}, fail_strategy = #{failStrategy}, " +
            "timeout_flag = #{timeoutFlag}, timeout_seconds = #{timeoutSeconds}, timeout_strategy = #{timeoutStrategy}, " +
            "retry_times = #{retryTimes}, retry_interval = #{retryInterval}, priority = #{priority}, " +
            "condition_type = #{conditionType}, condition_result = #{conditionResult}, run_flag = #{runFlag}, " +
            "is_enabled = #{isEnabled}, description = #{description} WHERE id = #{id}")
    int update(PipelineNode node);
    
    @Delete("DELETE FROM pipeline_node WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
    
    @Delete("DELETE FROM pipeline_node WHERE pipeline_id = #{pipelineId}")
    int deleteByPipelineId(@Param("pipelineId") Long pipelineId);
    
    @Insert("<script>" +
            "INSERT INTO pipeline_node (pipeline_id, node_code, node_name, node_type, node_config, " +
            "position_x, position_y, sort_order, pre_task_codes, fail_strategy, timeout_flag, " +
            "timeout_seconds, timeout_strategy, retry_times, retry_interval, priority, " +
            "condition_type, condition_result, run_flag, is_enabled, description) VALUES " +
            "<foreach collection='nodes' item='node' separator=','>" +
            "(#{node.pipelineId}, #{node.nodeCode}, #{node.nodeName}, #{node.nodeType}, #{node.nodeConfig}, " +
            "#{node.positionX}, #{node.positionY}, #{node.sortOrder}, #{node.preTaskCodesStr}, #{node.failStrategy}, " +
            "#{node.timeoutFlag}, #{node.timeoutSeconds}, #{node.timeoutStrategy}, #{node.retryTimes}, " +
            "#{node.retryInterval}, #{node.priority}, #{node.conditionType}, #{node.conditionResult}, " +
            "#{node.runFlag}, #{node.isEnabled}, #{node.description})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("nodes") List<PipelineNode> nodes);
}
