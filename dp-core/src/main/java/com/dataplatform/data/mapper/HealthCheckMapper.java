package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.HealthCheck;
import org.apache.ibatis.annotations.*;
import java.util.Date;
import java.util.List;

/**
 * 健康检查 Mapper
 */
@Mapper
public interface HealthCheckMapper {

    @Insert("INSERT INTO sys_health_check (check_time, check_type, check_name, status, response_time, " +
            "error_message, details) VALUES (#{checkTime}, #{checkType}, #{checkName}, #{status}, " +
            "#{responseTime}, #{errorMessage}, #{details})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(HealthCheck check);

    @Select("SELECT * FROM sys_health_check WHERE check_type=#{checkType} ORDER BY check_time DESC LIMIT 1")
    HealthCheck findLatestByType(@Param("checkType") String checkType);

    @Select("SELECT h1.* FROM sys_health_check h1 INNER JOIN " +
            "(SELECT check_type, MAX(check_time) AS max_time FROM sys_health_check GROUP BY check_type) h2 " +
            "ON h1.check_type = h2.check_type AND h1.check_time = h2.max_time ORDER BY h1.check_type")
    List<HealthCheck> findLatestAll();

    @Select("SELECT * FROM sys_health_check WHERE check_time >= #{since} ORDER BY check_time DESC LIMIT #{limit}")
    List<HealthCheck> findByTimeRange(@Param("since") Date since, @Param("limit") int limit);

    @Delete("DELETE FROM sys_health_check WHERE create_time < #{before}")
    int deleteOlderThan(@Param("before") Date before);
}
