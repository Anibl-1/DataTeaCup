package com.dataplatform.system.service.impl;

import com.dataplatform.system.dto.OperationLogQueryDTO;
import com.dataplatform.system.entity.OperationLog;
import com.dataplatform.system.mapper.OperationLogMapper;
import com.dataplatform.system.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class OperationLogServiceImpl implements OperationLogService {
    
    @Autowired
    private OperationLogMapper operationLogMapper;
    
    @Override
    @Async
    public void saveAsync(OperationLog operationLog) {
        try {
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            log.warn("操作日志保存失败: {}", e.getMessage());
        }
    }
    
    @Override
    public void save(OperationLog operationLog) {
        operationLogMapper.insert(operationLog);
    }
    
    @Override
    public OperationLog getById(Long id) {
        return operationLogMapper.selectById(id);
    }
    
    @Override
    public Map<String, Object> getByPage(Map<String, Object> params) {
        int page = params.get("page") != null ? (Integer) params.get("page") : 1;
        int pageSize = params.get("pageSize") != null ? (Integer) params.get("pageSize") : 10;
        params.put("offset", (page - 1) * pageSize);
        List<OperationLog> list = operationLogMapper.selectByPage(params);
        int total = operationLogMapper.countByParams(params);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }
    
    @Override
    public Map<String, Object> queryByCondition(OperationLogQueryDTO query) {
        int page = (query.getPage() != null && query.getPage() > 0) ? query.getPage() : 1;
        int pageSize = (query.getPageSize() != null && query.getPageSize() > 0) ? query.getPageSize() : 10;
        query.setPage(page);
        query.setPageSize(pageSize);
        List<OperationLog> list = operationLogMapper.selectByQuery(query);
        int total = operationLogMapper.countByQuery(query);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }
    
    @Override
    public Map<String, Object> getStatsOverview(Date startTime, Date endTime) {
        Map<String, Object> params = new HashMap<>();
        params.put("startTime", startTime);
        params.put("endTime", endTime);
        int total = operationLogMapper.countByParams(params);
        params.put("status", "success");
        int successCount = operationLogMapper.countByParams(params);
        params.put("status", "failed");
        int failedCount = operationLogMapper.countByParams(params);
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("successCount", successCount);
        result.put("failedCount", failedCount);
        result.put("successRate", total > 0 ? (double) successCount / total * 100 : 0);
        return result;
    }
    
    @Override
    public List<Map<String, Object>> getStatsByUser(Date startTime, Date endTime) {
        return operationLogMapper.countByUser(startTime, endTime);
    }
    
    @Override
    public List<Map<String, Object>> getStatsByModule(Date startTime, Date endTime) {
        return operationLogMapper.countByModule(startTime, endTime);
    }
    
    @Override
    public List<Map<String, Object>> getStatsByOperation(Date startTime, Date endTime) {
        return operationLogMapper.countByOperation(startTime, endTime);
    }
    
    @Override
    public List<Map<String, Object>> getTrend(Date startTime, Date endTime, String groupBy) {
        String format = "%Y-%m-%d";
        if ("hour".equals(groupBy)) { format = "%Y-%m-%d %H:00"; }
        else if ("month".equals(groupBy)) { format = "%Y-%m"; }
        return operationLogMapper.countTrend(startTime, endTime, format);
    }
    
    @Override
    public int cleanHistory(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -days);
        Date time = calendar.getTime();
        return operationLogMapper.deleteBeforeTime(time);
    }
}
