package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.DataxJobLog;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface DataxJobLogMapper {
    
    @Select("<script>" +
            "SELECT * FROM datax_job_log WHERE 1=1 " +
            "<if test='jobId != null'>AND job_id = #{jobId}</if>" +
            "<if test='status != null'>AND status = #{status}</if>" +
            "<if test='jobName != null and jobName != \"\"'>AND job_name LIKE CONCAT('%', #{jobName}, '%')</if>" +
            "ORDER BY start_time DESC " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<DataxJobLog> selectList(@Param("offset") int offset,
                                  @Param("pageSize") int pageSize,
                                  @Param("jobId") Long jobId,
                                  @Param("status") Integer status,
                                  @Param("jobName") String jobName);
    
    @Select("<script>" +
            "SELECT COUNT(*) FROM datax_job_log WHERE 1=1 " +
            "<if test='jobId != null'>AND job_id = #{jobId}</if>" +
            "<if test='status != null'>AND status = #{status}</if>" +
            "<if test='jobName != null and jobName != \"\"'>AND job_name LIKE CONCAT('%', #{jobName}, '%')</if>" +
            "</script>")
    long count(@Param("jobId") Long jobId, @Param("status") Integer status, @Param("jobName") String jobName);
    
    @Select("<script>" +
            "SELECT COUNT(*) FROM datax_job_log WHERE 1=1 " +
            "<if test='jobId != null'>AND job_id = #{jobId}</if>" +
            "<if test='status != null'>AND status = #{status}</if>" +
            "</script>")
    long countByStatus(@Param("jobId") Long jobId, @Param("status") Integer status);
    
    @Select("SELECT * FROM datax_job_log WHERE id = #{id}")
    DataxJobLog selectById(@Param("id") Long id);
    
    @Insert("INSERT INTO datax_job_log (job_id, job_name, start_time, status, trigger_type) " +
            "VALUES (#{jobId}, #{jobName}, #{startTime}, #{status}, #{triggerType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DataxJobLog log);
    
    @Update("<script>" +
            "UPDATE datax_job_log SET end_time = #{endTime}, status = #{status}, " +
            "read_count = #{readCount}, write_count = #{writeCount}, " +
            "error_message = #{errorMessage}, duration = #{duration} " +
            "<if test='executeLog != null'>, execute_log = #{executeLog}</if>" +
            "<if test='executeParams != null'>, execute_params = #{executeParams}</if>" +
            "WHERE id = #{id}" +
            "</script>")
    int update(DataxJobLog log);

    // ==================== 统计相关查询 ====================

    /**
     * 获取执行趋势（最近N天）
     */
    @Select("SELECT DATE(start_time) as date, " +
            "COUNT(*) as total, " +
            "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as success, " +
            "SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) as failed " +
            "FROM datax_job_log " +
            "WHERE start_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE(start_time) " +
            "ORDER BY date")
    List<Map<String, Object>> selectExecutionTrend(@Param("days") int days);

    /**
     * 获取今日统计
     */
    @Select("SELECT " +
            "COUNT(*) as todayTotal, " +
            "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as todaySuccess, " +
            "SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) as todayFailed " +
            "FROM datax_job_log " +
            "WHERE DATE(start_time) = CURDATE()")
    Map<String, Object> selectTodayStatistics();
}
