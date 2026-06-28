package com.dataplatform.data.service.quality;

import lombok.Data;

/**
 * 数据质量规则
 */
@Data
public class QualityRule {
    private String id;
    private String name;
    private String type; // completeness, uniqueness, timeliness, accuracy, custom
    private String targetTable;
    private String targetColumn;
    private String expression; // SQL表达式或规则表达式
    private double threshold; // 阈值（0-1），低于此值视为不合格
    private String severity; // critical, warning, info
}
