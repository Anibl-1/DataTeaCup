package com.dataplatform.data.service.quality;

import java.util.List;
import java.util.Map;

/**
 * 数据质量服务接口
 * 需求: 19.1, 19.2, 19.3, 19.4
 */
public interface DataQualityService {

    /**
     * 添加质量规则
     */
    QualityRule addRule(QualityRule rule);

    /**
     * 获取所有规则
     */
    List<QualityRule> listRules();

    /**
     * 对数据集执行质量检查
     */
    List<QualityCheckResult> checkQuality(String tableName, List<Map<String, Object>> data);

    /**
     * 执行单条规则检查
     */
    QualityCheckResult checkRule(QualityRule rule, List<Map<String, Object>> data);

    /**
     * 获取质量报告
     */
    Map<String, Object> getQualityReport(String tableName);

    /**
     * 获取质量趋势
     */
    List<Map<String, Object>> getQualityTrend(String tableName, int days);
}
