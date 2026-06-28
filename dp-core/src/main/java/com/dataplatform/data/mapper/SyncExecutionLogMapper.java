package com.dataplatform.data.mapper;

import com.dataplatform.data.service.sync.SyncExecutionLog;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 同步执行日志Mapper接口
 */
public interface SyncExecutionLogMapper {

    int insert(SyncExecutionLog log);

    int update(SyncExecutionLog log);

    List<SyncExecutionLog> selectByTaskId(@Param("taskId") String taskId, @Param("limit") int limit);
}
