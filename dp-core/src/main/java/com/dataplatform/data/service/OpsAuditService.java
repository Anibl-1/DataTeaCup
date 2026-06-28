package com.dataplatform.data.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 运维操作审计服务
 * 记录所有运维操作（缓存清理、配置修改、服务重启等）
 * 需求: 16.8
 */
@Slf4j
@Service
public class OpsAuditService {

    private final Deque<OpsAuditRecord> auditRecords = new ConcurrentLinkedDeque<>();
    private static final int MAX_RECORDS = 10000;

    /**
     * 记录运维操作
     */
    public void recordOperation(String operator, String operationType, String target,
                                 String detail, boolean success) {
        OpsAuditRecord record = new OpsAuditRecord();
        record.setId(UUID.randomUUID().toString().substring(0, 12));
        record.setOperator(operator);
        record.setOperationType(operationType);
        record.setTarget(target);
        record.setDetail(detail);
        record.setSuccess(success);
        record.setTimestamp(LocalDateTime.now());
        record.setIpAddress(""); // 可从RequestContext获取

        auditRecords.addLast(record);
        while (auditRecords.size() > MAX_RECORDS) {
            auditRecords.pollFirst();
        }

        log.info("[运维审计] operator={}, type={}, target={}, success={}",
                operator, operationType, target, success);
    }

    /**
     * 查询审计记录
     */
    public List<OpsAuditRecord> queryRecords(String operationType, int limit) {
        List<OpsAuditRecord> result = new ArrayList<>();
        for (OpsAuditRecord record : auditRecords) {
            if (operationType == null || operationType.isEmpty() ||
                    operationType.equals(record.getOperationType())) {
                result.add(record);
            }
        }
        if (result.size() > limit) {
            return result.subList(result.size() - limit, result.size());
        }
        return result;
    }

    /**
     * 获取审计统计
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalRecords", auditRecords.size());

        Map<String, Integer> byType = new LinkedHashMap<>();
        for (OpsAuditRecord record : auditRecords) {
            byType.merge(record.getOperationType(), 1, Integer::sum);
        }
        stats.put("byType", byType);

        long failCount = auditRecords.stream().filter(r -> !r.isSuccess()).count();
        stats.put("failCount", failCount);
        return stats;
    }

    @Data
    public static class OpsAuditRecord {
        private String id;
        private String operator;
        private String operationType; // cache_clear, config_update, service_restart, etc.
        private String target;
        private String detail;
        private boolean success;
        private String ipAddress;
        private LocalDateTime timestamp;
    }
}
