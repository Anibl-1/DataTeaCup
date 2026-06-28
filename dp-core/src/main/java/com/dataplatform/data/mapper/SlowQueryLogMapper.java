package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.SlowQueryLog;
import org.apache.ibatis.annotations.*;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 慢查询日志 Mapper
 */
@Mapper
public interface SlowQueryLogMapper {

    @Insert("INSERT INTO sys_slow_query_log (data_source_id, data_source_name, sql_text, sql_hash, " +
            "execution_time, rows_examined, rows_returned, query_time, user_name, client_ip, database_name) " +
            "VALUES (#{dataSourceId}, #{dataSourceName}, #{sqlText}, #{sqlHash}, #{executionTime}, " +
            "#{rowsExamined}, #{rowsReturned}, #{queryTime}, #{userName}, #{clientIp}, #{databaseName})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(SlowQueryLog log);

    @Select("SELECT * FROM sys_slow_query_log ORDER BY query_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<SlowQueryLog> findByPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM sys_slow_query_log")
    long countAll();

    @Select("SELECT data_source_name, COUNT(*) AS query_count, " +
            "ROUND(AVG(execution_time)) AS avg_time, MAX(execution_time) AS max_time, " +
            "SUM(rows_examined) AS total_rows_examined " +
            "FROM sys_slow_query_log WHERE query_time >= #{since} " +
            "GROUP BY data_source_name ORDER BY query_count DESC")
    List<Map<String, Object>> findStats(@Param("since") Date since);

    @Select("SELECT sql_hash, sql_text, COUNT(*) AS occurrence, " +
            "ROUND(AVG(execution_time)) AS avg_time, MAX(execution_time) AS max_time " +
            "FROM sys_slow_query_log WHERE query_time >= #{since} " +
            "GROUP BY sql_hash, sql_text ORDER BY occurrence DESC LIMIT 20")
    List<Map<String, Object>> findTopSlowQueries(@Param("since") Date since);

    @Delete("DELETE FROM sys_slow_query_log WHERE create_time < #{before}")
    int deleteOlderThan(@Param("before") Date before);

    @Select("<script>" +
            "SELECT * FROM sys_slow_query_log WHERE 1=1 " +
            "<if test='dataSourceId != null'>AND data_source_id = #{dataSourceId}</if>" +
            "<if test='minTime != null'>AND execution_time &gt;= #{minTime}</if>" +
            "<if test='startDate != null'>AND query_time &gt;= #{startDate}</if>" +
            "<if test='endDate != null'>AND query_time &lt;= #{endDate}</if>" +
            "ORDER BY query_time DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<SlowQueryLog> findByPageFiltered(@Param("offset") int offset, @Param("limit") int limit,
                                          @Param("dataSourceId") Long dataSourceId,
                                          @Param("minTime") Long minTime,
                                          @Param("startDate") String startDate,
                                          @Param("endDate") String endDate);

    @Select("<script>" +
            "SELECT COUNT(*) FROM sys_slow_query_log WHERE 1=1 " +
            "<if test='dataSourceId != null'>AND data_source_id = #{dataSourceId}</if>" +
            "<if test='minTime != null'>AND execution_time &gt;= #{minTime}</if>" +
            "<if test='startDate != null'>AND query_time &gt;= #{startDate}</if>" +
            "<if test='endDate != null'>AND query_time &lt;= #{endDate}</if>" +
            "</script>")
    long countFiltered(@Param("dataSourceId") Long dataSourceId,
                       @Param("minTime") Long minTime,
                       @Param("startDate") String startDate,
                       @Param("endDate") String endDate);

    @Select("SELECT DISTINCT data_source_id, data_source_name FROM sys_slow_query_log " +
            "WHERE data_source_name IS NOT NULL ORDER BY data_source_name")
    List<Map<String, Object>> findDistinctDataSources();
}
