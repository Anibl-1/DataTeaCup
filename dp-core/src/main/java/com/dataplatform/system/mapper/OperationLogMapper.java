package com.dataplatform.system.mapper;

import com.dataplatform.system.dto.OperationLogQueryDTO;
import com.dataplatform.system.entity.OperationLog;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 操作日志Mapper
 */
public interface OperationLogMapper {
    int insert(OperationLog log);
    OperationLog selectById(Long id);
    List<OperationLog> selectByPage(@Param("params") Map<String, Object> params);
    int countByParams(@Param("params") Map<String, Object> params);
    /** 按查询DTO条件分页查询操作日志 */
    List<OperationLog> selectByQuery(@Param("query") OperationLogQueryDTO query);
    /** 按查询DTO条件统计操作日志数量 */
    int countByQuery(@Param("query") OperationLogQueryDTO query);
    List<Map<String, Object>> countByUser(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
    List<Map<String, Object>> countByModule(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
    List<Map<String, Object>> countByOperation(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
    List<Map<String, Object>> countTrend(@Param("startTime") Date startTime, @Param("endTime") Date endTime, @Param("groupBy") String groupBy);
    int deleteBeforeTime(@Param("time") Date time);
}
