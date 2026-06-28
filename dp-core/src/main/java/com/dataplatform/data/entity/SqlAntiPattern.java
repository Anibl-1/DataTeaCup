package com.dataplatform.data.entity;

import lombok.Data;

/**
 * SQL 反模式（低效 SQL 模式）
 */
@Data
public class SqlAntiPattern {
    /** 反模式类型 */
    private String patternType;
    /** 描述 */
    private String description;
    /** 严重级别：HIGH, MEDIUM, LOW */
    private String severity;
    /** 优化建议 */
    private String suggestion;

    public SqlAntiPattern() {}

    public SqlAntiPattern(String patternType, String description, String severity, String suggestion) {
        this.patternType = patternType;
        this.description = description;
        this.severity = severity;
        this.suggestion = suggestion;
    }
}
