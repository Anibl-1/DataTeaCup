package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.MonitorMetric;
import org.apache.ibatis.annotations.*;
import java.util.Date;
import java.util.List;

/**
 * 监控指标 Mapper
 */
@Mapper
public interface MonitorMetricMapper {

    @Insert("INSERT INTO monitor_metric (cpu_usage, memory_usage, disk_usage, heap_used, heap_max, " +
            "thread_count, gc_count, active_connections, running_tasks, collect_time) " +
            "VALUES (#{cpuUsage}, #{memoryUsage}, #{diskUsage}, #{heapUsed}, #{heapMax}, " +
            "#{threadCount}, #{gcCount}, #{activeConnections}, #{runningTasks}, #{collectTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(MonitorMetric metric);

    @Select("SELECT * FROM monitor_metric WHERE collect_time >= #{since} ORDER BY collect_time ASC")
    @Results({
            @Result(property = "cpuUsage", column = "cpu_usage"),
            @Result(property = "memoryUsage", column = "memory_usage"),
            @Result(property = "diskUsage", column = "disk_usage"),
            @Result(property = "heapUsed", column = "heap_used"),
            @Result(property = "heapMax", column = "heap_max"),
            @Result(property = "threadCount", column = "thread_count"),
            @Result(property = "gcCount", column = "gc_count"),
            @Result(property = "activeConnections", column = "active_connections"),
            @Result(property = "runningTasks", column = "running_tasks"),
            @Result(property = "collectTime", column = "collect_time")
    })
    List<MonitorMetric> findSince(@Param("since") Date since);

    @Delete("DELETE FROM monitor_metric WHERE collect_time < #{before}")
    int deleteOlderThan(@Param("before") Date before);
}
