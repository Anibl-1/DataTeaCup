package com.dataplatform.data.entity;

import lombok.Data;

import java.util.List;

/**
 * 索引优化建议
 */
@Data
public class IndexRecommendation {
    /** 表名 */
    private String tableName;
    /** 建议索引的列 */
    private List<String> columns;
    /** 索引类型：BTREE, HASH 等 */
    private String indexType;
    /** 建议原因 */
    private String reason;
    /** CREATE INDEX 语句 */
    private String createStatement;
    /** 预估性能提升百分比 */
    private double estimatedImprovement;
}
