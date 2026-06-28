package com.dataplatform.data.dto.export;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 导出请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 任务类型: excel, csv, json
     */
    private String taskType;
    
    /**
     * SQL查询语句
     */
    private String sql;
    
    /**
     * 数据源ID
     */
    private Long dataSourceId;
    
    /**
     * 关联报表ID（可选）
     */
    private Long reportId;
    
    /**
     * 关联报表编码（可选）
     */
    private String reportCode;
    
    /**
     * 筛选条件JSON
     */
    private String filters;
    
    /**
     * 自定义参数JSON
     */
    private String params;
    
    /**
     * 脱敏规则ID列表
     */
    private List<Long> maskingRuleIds;
    
    /**
     * 脱敏规则JSON（直接传递规则配置）
     */
    private String maskingRulesJson;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户角色ID列表
     */
    private List<Long> roleIds;
    
    /**
     * 字段标签映射
     */
    private Map<String, String> fieldLabels;
    
    /**
     * 导出配置
     */
    private ExportConfig config;
    
    /**
     * 是否启用断点续传
     */
    private Boolean enableCheckpoint;
    
    /**
     * 预估行数（可选，用于优化导出策略）
     */
    private Long estimatedRows;
}
