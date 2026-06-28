package com.dataplatform.system.service;

import com.dataplatform.system.dto.OperationLogQueryDTO;
import com.dataplatform.system.entity.OperationLog;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务接口
 */
public interface OperationLogService {
    void saveAsync(OperationLog log);
    void save(OperationLog log);
    OperationLog getById(Long id);
    Map<String, Object> getByPage(Map<String, Object> params);
    /**
     * 按查询条件分页查询操作日志（类型安全版本）
     * 支持按操作类型、操作人、时间范围、模块名称组合筛选
     */
    Map<String, Object> queryByCondition(OperationLogQueryDTO query);
    Map<String, Object> getStatsOverview(Date startTime, Date endTime);
    List<Map<String, Object>> getStatsByUser(Date startTime, Date endTime);
    List<Map<String, Object>> getStatsByModule(Date startTime, Date endTime);
    List<Map<String, Object>> getStatsByOperation(Date startTime, Date endTime);
    List<Map<String, Object>> getTrend(Date startTime, Date endTime, String groupBy);
    int cleanHistory(int days);
}
