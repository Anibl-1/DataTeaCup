package com.dataplatform.data.service.masking;

import com.dataplatform.data.entity.MaskingAuditLog;
import com.dataplatform.data.mapper.MaskingAuditLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 脱敏审计服务实现
 * 异步记录脱敏操作审计日志，避免影响查询性能
 *
 * **Validates: Requirements 6.4**
 *
 * @author dataplatform
 */
@Slf4j
@Service
public class MaskingAuditServiceImpl implements MaskingAuditService {

    private final MaskingAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public MaskingAuditServiceImpl(MaskingAuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    @Async
    @Override
    public void recordAudit(Long userId, String operationType, Long dataSourceId,
                            String sqlHash, Map<String, MaskingRule> maskedFields,
                            int rowCount, long executionTime) {
        try {
            String maskedFieldsJson = serializeMaskedFields(maskedFields);

            MaskingAuditLog auditLog = MaskingAuditLog.builder()
                    .userId(userId)
                    .operationType(operationType)
                    .dataSourceId(dataSourceId)
                    .sqlHash(sqlHash)
                    .maskedFields(maskedFieldsJson)
                    .rowCount(rowCount)
                    .executionTime(executionTime)
                    .createTime(LocalDateTime.now())
                    .build();

            auditLogMapper.insert(auditLog);
            log.debug("Recorded masking audit log: user={}, type={}, fields={}, rows={}",
                    userId, operationType, maskedFields.size(), rowCount);
        } catch (Exception e) {
            // Audit logging should never break the main flow
            log.error("Failed to record masking audit log: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<MaskingAuditLog> queryByUser(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogMapper.findByUserAndTimeRange(userId, startTime, endTime);
    }

    @Override
    public List<MaskingAuditLog> queryByDataSource(Long dataSourceId, LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogMapper.findByDataSourceAndTimeRange(dataSourceId, startTime, endTime);
    }

    /**
     * Serialize masked fields map to JSON for storage.
     * Each entry records the field name, strategy type, and sensitive type.
     */
    private String serializeMaskedFields(Map<String, MaskingRule> maskedFields) {
        if (maskedFields == null || maskedFields.isEmpty()) {
            return "[]";
        }

        List<Map<String, String>> fieldEntries = maskedFields.entrySet().stream()
                .map(entry -> {
                    MaskingRule rule = entry.getValue();
                    return Map.of(
                            "fieldName", entry.getKey(),
                            "strategyType", rule.getStrategyType() != null ? rule.getStrategyType() : "",
                            "sensitiveType", rule.getSensitiveType() != null ? rule.getSensitiveType().name() : ""
                    );
                })
                .collect(Collectors.toList());

        try {
            return objectMapper.writeValueAsString(fieldEntries);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize masked fields: {}", e.getMessage());
            return "[]";
        }
    }
}
