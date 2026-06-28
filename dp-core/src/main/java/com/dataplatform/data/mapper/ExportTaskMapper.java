package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.ExportTask;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 导出任务Mapper
 */
@Mapper
public interface ExportTaskMapper {
    
    @Insert("INSERT INTO export_task (task_name, task_type, ref_id, ref_code, filters, params, status, progress, " +
            "create_by, create_time, expire_time) VALUES (#{taskName}, #{taskType}, #{refId}, #{refCode}, " +
            "#{filters}, #{params}, #{status}, #{progress}, #{createBy}, #{createTime}, #{expireTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExportTask task);
    
    @Update("UPDATE export_task SET status = #{status}, progress = #{progress}, " +
            "start_time = #{startTime}, finish_time = #{finishTime}, " +
            "file_path = #{filePath}, file_name = #{fileName}, file_size = #{fileSize}, " +
            "total_rows = #{totalRows}, data_type = #{dataType}, error_msg = #{errorMsg}, " +
            "checkpoint_offset = #{checkpointOffset}, processed_rows = #{processedRows}, " +
            "temp_file_path = #{tempFilePath}, export_sql = #{exportSql}, data_source_id = #{dataSourceId} WHERE id = #{id}")
    int update(ExportTask task);
    
    @Update("UPDATE export_task SET status = #{status}, progress = #{progress} WHERE id = #{id}")
    int updateProgress(@Param("id") Long id, @Param("status") Integer status, @Param("progress") Integer progress);
    
    /**
     * 更新断点位置（用于断点续传）
     * 需求: 17.3 - 支持导出任务的断点续传功能
     */
    @Update("UPDATE export_task SET checkpoint_offset = #{checkpointOffset}, processed_rows = #{processedRows}, " +
            "progress = #{progress}, temp_file_path = #{tempFilePath} WHERE id = #{id}")
    int updateCheckpoint(@Param("id") Long id, @Param("checkpointOffset") Long checkpointOffset, 
                        @Param("processedRows") Long processedRows, @Param("progress") Integer progress,
                        @Param("tempFilePath") String tempFilePath);
    
    /**
     * 保存导出任务的SQL和数据源信息（用于断点续传）
     */
    @Update("UPDATE export_task SET export_sql = #{exportSql}, data_source_id = #{dataSourceId} WHERE id = #{id}")
    int updateExportInfo(@Param("id") Long id, @Param("exportSql") String exportSql, @Param("dataSourceId") Long dataSourceId);
    
    @Select("SELECT * FROM export_task WHERE id = #{id}")
    ExportTask selectById(Long id);
    
    @Select("SELECT * FROM export_task WHERE create_by = #{userId} ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<ExportTask> selectByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);
    
    @Select("SELECT COUNT(*) FROM export_task WHERE create_by = #{userId}")
    long countByUserId(Long userId);
    
    @Select("<script>" +
            "SELECT * FROM export_task WHERE create_by = #{userId}" +
            "<if test='taskName != null and taskName != \"\"'> AND task_name LIKE CONCAT('%', #{taskName}, '%')</if>" +
            "<if test='startDate != null and startDate != \"\"'> AND create_time >= #{startDate}</if>" +
            "<if test='endDate != null and endDate != \"\"'> AND create_time &lt;= CONCAT(#{endDate}, ' 23:59:59')</if>" +
            " ORDER BY create_time DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    List<ExportTask> selectByUserIdWithConditions(@Param("userId") Long userId, @Param("offset") int offset, 
                                                   @Param("limit") int limit, @Param("taskName") String taskName,
                                                   @Param("startDate") String startDate, @Param("endDate") String endDate);
    
    @Select("<script>" +
            "SELECT COUNT(*) FROM export_task WHERE create_by = #{userId}" +
            "<if test='taskName != null and taskName != \"\"'> AND task_name LIKE CONCAT('%', #{taskName}, '%')</if>" +
            "<if test='startDate != null and startDate != \"\"'> AND create_time >= #{startDate}</if>" +
            "<if test='endDate != null and endDate != \"\"'> AND create_time &lt;= CONCAT(#{endDate}, ' 23:59:59')</if>" +
            "</script>")
    long countByUserIdWithConditions(@Param("userId") Long userId, @Param("taskName") String taskName,
                                      @Param("startDate") String startDate, @Param("endDate") String endDate);
    
    @Select("SELECT * FROM export_task WHERE create_by = #{userId} AND status IN (0, 1) ORDER BY create_time DESC")
    List<ExportTask> selectPendingByUserId(Long userId);
    
    @Delete("DELETE FROM export_task WHERE id = #{id} AND create_by = #{userId}")
    int deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);
    
    @Delete("DELETE FROM export_task WHERE expire_time < NOW()")
    int deleteExpired();
    
    @Update("UPDATE export_task SET status = 5, error_msg = '文件已过期清理' " +
            "WHERE status = 2 AND finish_time < #{cutoffTime} AND file_path IS NOT NULL")
    int markExpiredTasks(@Param("cutoffTime") java.time.LocalDateTime cutoffTime);
    
    @Select("SELECT * FROM export_task WHERE status IN (0, 1) AND create_time < #{threshold}")
    List<ExportTask> selectStuckTasks(@Param("threshold") java.time.LocalDateTime threshold);
}
