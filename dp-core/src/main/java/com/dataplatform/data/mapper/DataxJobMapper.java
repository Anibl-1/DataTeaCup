package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.DataxJob;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface DataxJobMapper {
    
    @Select("<script>" +
            "SELECT j.*, " +
            "s1.name as sourceDataSourceName, " +
            "s2.name as targetDataSourceName " +
            "FROM datax_job j " +
            "LEFT JOIN data_source s1 ON j.source_data_source_id = s1.id " +
            "LEFT JOIN data_source s2 ON j.target_data_source_id = s2.id " +
            "WHERE j.del_flag = 0 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (j.job_name LIKE CONCAT('%', #{keyword}, '%') OR j.job_desc LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "<if test='jobStatus != null'>" +
            "AND j.job_status = #{jobStatus}" +
            "</if>" +
            "ORDER BY j.create_time DESC " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<DataxJob> selectList(@Param("offset") int offset, 
                               @Param("pageSize") int pageSize,
                               @Param("keyword") String keyword,
                               @Param("jobStatus") Integer jobStatus);
    
    @Select("<script>" +
            "SELECT COUNT(*) FROM datax_job WHERE del_flag = 0 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (job_name LIKE CONCAT('%', #{keyword}, '%') OR job_desc LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "<if test='jobStatus != null'>" +
            "AND job_status = #{jobStatus}" +
            "</if>" +
            "</script>")
    long count(@Param("keyword") String keyword, @Param("jobStatus") Integer jobStatus);
    
    @Select("SELECT j.*, s1.name as sourceDataSourceName, s2.name as targetDataSourceName " +
            "FROM datax_job j " +
            "LEFT JOIN data_source s1 ON j.source_data_source_id = s1.id " +
            "LEFT JOIN data_source s2 ON j.target_data_source_id = s2.id " +
            "WHERE j.id = #{id} AND j.del_flag = 0")
    DataxJob selectById(@Param("id") Long id);
    
    @Insert("INSERT INTO datax_job (job_name, job_desc, job_type, source_data_source_id, source_table, " +
            "source_query_sql, target_data_source_id, target_table, write_mode, column_mapping, " +
            "cron_expression, job_status, increment_type, increment_column, increment_value, " +
            "last_increment_value, channel_count, engine_type, datax_home, batch_size, " +
            "parameter_definition, default_parameters, " +
            "create_time, update_time, create_by, del_flag) " +
            "VALUES (#{jobName}, #{jobDesc}, #{jobType}, #{sourceDataSourceId}, #{sourceTable}, " +
            "#{sourceQuerySql}, #{targetDataSourceId}, #{targetTable}, #{writeMode}, #{columnMapping}, " +
            "#{cronExpression}, #{jobStatus}, #{incrementType}, #{incrementColumn}, #{incrementValue}, " +
            "#{lastIncrementValue}, #{channelCount}, #{engineType}, #{dataxHome}, #{batchSize}, " +
            "#{parameterDefinition}, #{defaultParameters}, " +
            "#{createTime}, #{updateTime}, #{createBy}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DataxJob job);
    
    @Update("<script>" +
            "UPDATE datax_job SET update_time = NOW() " +
            "<if test='jobName != null'>, job_name = #{jobName}</if>" +
            "<if test='jobDesc != null'>, job_desc = #{jobDesc}</if>" +
            "<if test='jobType != null'>, job_type = #{jobType}</if>" +
            "<if test='sourceDataSourceId != null'>, source_data_source_id = #{sourceDataSourceId}</if>" +
            "<if test='sourceTable != null'>, source_table = #{sourceTable}</if>" +
            "<if test='sourceQuerySql != null'>, source_query_sql = #{sourceQuerySql}</if>" +
            "<if test='targetDataSourceId != null'>, target_data_source_id = #{targetDataSourceId}</if>" +
            "<if test='targetTable != null'>, target_table = #{targetTable}</if>" +
            "<if test='writeMode != null'>, write_mode = #{writeMode}</if>" +
            "<if test='columnMapping != null'>, column_mapping = #{columnMapping}</if>" +
            "<if test='cronExpression != null'>, cron_expression = #{cronExpression}</if>" +
            "<if test='jobStatus != null'>, job_status = #{jobStatus}</if>" +
            "<if test='incrementType != null'>, increment_type = #{incrementType}</if>" +
            "<if test='incrementColumn != null'>, increment_column = #{incrementColumn}</if>" +
            "<if test='incrementValue != null'>, increment_value = #{incrementValue}</if>" +
            "<if test='lastIncrementValue != null'>, last_increment_value = #{lastIncrementValue}</if>" +
            "<if test='channelCount != null'>, channel_count = #{channelCount}</if>" +
            "<if test='engineType != null'>, engine_type = #{engineType}</if>" +
            "<if test='dataxHome != null'>, datax_home = #{dataxHome}</if>" +
            "<if test='batchSize != null'>, batch_size = #{batchSize}</if>" +
            "<if test='parameterDefinition != null'>, parameter_definition = #{parameterDefinition}</if>" +
            "<if test='defaultParameters != null'>, default_parameters = #{defaultParameters}</if>" +
            "<if test='lastExecuteTime != null'>, last_execute_time = #{lastExecuteTime}</if>" +
            "WHERE id = #{id}" +
            "</script>")
    int update(DataxJob job);
    
    @Update("UPDATE datax_job SET del_flag = 1, update_time = NOW() WHERE id = #{id}")
    int delete(@Param("id") Long id);

    // ==================== 模板相关查询 ====================

    /**
     * 查询所有模板（job_type = 2）
     */
    @Select("SELECT j.*, s1.name as sourceDataSourceName, s2.name as targetDataSourceName " +
            "FROM datax_job j " +
            "LEFT JOIN data_source s1 ON j.source_data_source_id = s1.id " +
            "LEFT JOIN data_source s2 ON j.target_data_source_id = s2.id " +
            "WHERE j.del_flag = 0 AND j.job_type = 2 " +
            "ORDER BY j.create_time DESC")
    List<DataxJob> selectTemplates();

    /**
     * 统计模板数量
     */
    @Select("SELECT COUNT(*) FROM datax_job WHERE del_flag = 0 AND job_type = 2")
    long countTemplates();
}
