package com.dataplatform.data.service.masking;

import com.dataplatform.data.entity.MaskingAuditLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 脱敏审计服务接口
 * 记录所有脱敏操作的审计日志，包括操作时间、操作用户、脱敏字段和脱敏策略
 *
 * **Validates: Requirements 6.4**
 *
 * @author dataplatform
 */
public interface MaskingAuditService {

    /**
     * 记录脱敏操作审计日志
     *
     * @param userId        操作用户ID
     * @param operationType 操作类型：query / export
     * @param dataSourceId  数据源ID（可为null）
     * @param sqlHash       SQL哈希值（可为null）
     * @param maskedFields  脱敏字段与规则的映射 (fieldName -> MaskingRule)
     * @param rowCount      处理行数
     * @param executionTime 执行时间（毫秒）
     */
    void recordAudit(Long userId, String operationType, Long dataSourceId,
                     String sqlHash, Map<String, MaskingRule> maskedFields,
                     int rowCount, long executionTime);

    /**
     * 根据用户ID和时间范围查询审计日志
     *
     * @param userId    用户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 审计日志列表
     */
    List<MaskingAuditLog> queryByUser(Long userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据数据源ID和时间范围查询审计日志
     *
     * @param dataSourceId 数据源ID
     * @param startTime    开始时间
     * @param endTime      结束时间
     * @return 审计日志列表
     */
    List<MaskingAuditLog> queryByDataSource(Long dataSourceId, LocalDateTime startTime, LocalDateTime endTime);
}
