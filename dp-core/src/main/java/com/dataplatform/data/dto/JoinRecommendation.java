package com.dataplatform.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表关联推荐结果
 * 基于外键关系或字段名匹配推荐表之间的关联
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinRecommendation {
    
    /**
     * 左表名
     */
    private String leftTable;
    
    /**
     * 左表关联字段
     */
    private String leftField;
    
    /**
     * 右表名
     */
    private String rightTable;
    
    /**
     * 右表关联字段
     */
    private String rightField;
    
    /**
     * 推荐的 JOIN 类型
     */
    private String joinType;
    
    /**
     * 推荐来源类型
     */
    private RecommendationType recommendationType;
    
    /**
     * 置信度分数 (0.0 - 1.0)
     * 外键关系置信度最高，字段名完全匹配次之，模式匹配最低
     */
    private double confidence;
    
    /**
     * 推荐原因描述
     */
    private String reason;
    
    /**
     * 推荐来源类型枚举
     */
    public enum RecommendationType {
        /**
         * 基于数据库外键关系
         */
        FOREIGN_KEY,
        
        /**
         * 基于字段名完全匹配 (如 user_id = id)
         */
        FIELD_NAME_EXACT,
        
        /**
         * 基于字段名模式匹配 (如 orders.user_id 匹配 users.id)
         */
        FIELD_NAME_PATTERN,
        
        /**
         * 基于主键匹配
         */
        PRIMARY_KEY_MATCH
    }
}
