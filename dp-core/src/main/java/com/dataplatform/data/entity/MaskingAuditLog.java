package com.dataplatform.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 脱敏审计日志实体
 * 记录所有脱敏操作的审计信息，包括操作时间、操作用户、脱敏字段和脱敏策略
 *
 * **Validates: Requirements 6.4**
 *
 * @author dataplatform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("masking_audit_log")
public class MaskingAuditLog {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 操作类型：query（查询脱敏）/ export（导出脱敏）
     */
    private String operationType;

    /**
     * 数据源ID
     */
    private Long dataSourceId;

    /**
     * SQL哈希值（SHA-256），用于关联查询
     */
    private String sqlHash;

    /**
     * 脱敏字段列表JSON
     * 格式: [{"fieldName":"phone","strategyType":"MASK","sensitiveType":"PHONE"}, ...]
     */
    private String maskedFields;

    /**
     * 处理行数
     */
    private Integer rowCount;

    /**
     * 脱敏执行时间（毫秒）
     */
    private Long executionTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
