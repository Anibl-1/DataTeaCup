package com.dataplatform.data.service;

import com.dataplatform.data.dto.JoinRecommendation;

import java.util.List;

/**
 * 表关联推荐服务接口
 * 基于外键关系和字段名匹配自动推荐表之间的关联关系
 * 
 * 验证需求: 12.2 - THE Query_Builder SHALL 自动推荐表之间的关联关系（基于外键或字段名匹配）
 */
public interface JoinRecommendService {
    
    /**
     * 推荐两个表之间的关联关系
     * 
     * @param dataSourceId 数据源ID
     * @param leftTable 左表名
     * @param rightTable 右表名
     * @return 推荐的关联列表，按置信度降序排列
     */
    List<JoinRecommendation> recommendJoins(Long dataSourceId, String leftTable, String rightTable);
    
    /**
     * 推荐多个表之间的所有可能关联关系
     * 
     * @param dataSourceId 数据源ID
     * @param tableNames 表名列表
     * @return 推荐的关联列表，按置信度降序排列
     */
    List<JoinRecommendation> recommendJoinsForTables(Long dataSourceId, List<String> tableNames);
    
    /**
     * 检测基于外键的关联关系
     * 
     * @param dataSourceId 数据源ID
     * @param leftTable 左表名
     * @param rightTable 右表名
     * @return 基于外键的关联推荐列表
     */
    List<JoinRecommendation> detectForeignKeyJoins(Long dataSourceId, String leftTable, String rightTable);
    
    /**
     * 检测基于字段名匹配的关联关系
     * 
     * @param dataSourceId 数据源ID
     * @param leftTable 左表名
     * @param rightTable 右表名
     * @return 基于字段名匹配的关联推荐列表
     */
    List<JoinRecommendation> detectFieldNameJoins(Long dataSourceId, String leftTable, String rightTable);
}
