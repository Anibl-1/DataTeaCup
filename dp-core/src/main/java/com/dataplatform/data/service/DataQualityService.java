package com.dataplatform.data.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataplatform.data.entity.DataQualityReport;
import com.dataplatform.data.entity.DataQualityRule;
import com.dataplatform.data.mapper.DataQualityReportMapper;
import com.dataplatform.data.mapper.DataQualityRuleMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 数据质量监控服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataQualityService {

    private final DataQualityRuleMapper ruleMapper;
    private final DataQualityReportMapper reportMapper;
    private final DataSourceService dataSourceService;
    private final AlertService alertService;
    private final ObjectMapper objectMapper;

    /**
     * 检查数据质量
     */
    public QualityReport checkQuality(Long dataSourceId, String tableName) {
        log.info("Checking data quality for table: {}", tableName);
        
        QualityReport report = new QualityReport();
        report.setDataSourceId(dataSourceId);
        report.setTableName(tableName);
        report.setDetails(new ArrayList<>());
        
        try {
            DataSource ds = dataSourceService.getDataSource(dataSourceId);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
            
            // 获取总行数
            Long totalRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName, Long.class);
            
            if (totalRows == null || totalRows == 0) {
                report.setScore(0);
                return report;
            }
            
            // 获取列信息
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ?",
                tableName);
            
            double totalScore = 0;
            int fieldCount = 0;
            
            for (Map<String, Object> col : columns) {
                String fieldName = (String) col.get("COLUMN_NAME");
                String dataType = (String) col.get("DATA_TYPE");
                
                FieldQualityDetail detail = analyzeField(jdbcTemplate, tableName, fieldName, dataType, totalRows);
                report.getDetails().add(detail);
                
                // 计算字段得分
                double fieldScore = calculateFieldScore(detail);
                totalScore += fieldScore;
                fieldCount++;
            }
            
            // 计算总体得分
            report.setScore(fieldCount > 0 ? totalScore / fieldCount : 100);
            
        } catch (Exception e) {
            log.error("Failed to check quality", e);
            report.setScore(0);
        }
        
        return report;
    }

    /**
     * 计算质量评分
     */
    public double calculateScore(QualityReport report) {
        if (report.getDetails() == null || report.getDetails().isEmpty()) {
            return 0;
        }
        
        double totalScore = 0;
        for (FieldQualityDetail detail : report.getDetails()) {
            totalScore += calculateFieldScore(detail);
        }
        
        return totalScore / report.getDetails().size();
    }

    /**
     * 保存质量规则
     */
    @Transactional
    public DataQualityRule saveQualityRule(DataQualityRule rule) {
        if (rule.getId() == null) {
            rule.setCreateTime(LocalDateTime.now());
            rule.setStatus(1);
            ruleMapper.insert(rule);
        } else {
            ruleMapper.updateById(rule);
        }
        return rule;
    }

    /**
     * 获取质量规则列表
     */
    public List<DataQualityRule> getQualityRules(Long dataSourceId) {
        LambdaQueryWrapper<DataQualityRule> wrapper = new LambdaQueryWrapper<>();
        if (dataSourceId != null) {
            wrapper.eq(DataQualityRule::getDataSourceId, dataSourceId);
        }
        wrapper.orderByDesc(DataQualityRule::getCreateTime);
        return ruleMapper.selectList(wrapper);
    }

    /**
     * 删除质量规则
     */
    @Transactional
    public void deleteRule(Long ruleId) {
        ruleMapper.deleteById(ruleId);
    }

    /**
     * 检查并告警
     */
    public void checkAndAlert(Long dataSourceId, String tableName) {
        // 获取规则
        LambdaQueryWrapper<DataQualityRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataQualityRule::getDataSourceId, dataSourceId)
               .eq(DataQualityRule::getTableName, tableName)
               .eq(DataQualityRule::getStatus, 1);
        
        DataQualityRule rule = ruleMapper.selectOne(wrapper);
        if (rule == null) {
            return;
        }
        
        // 检查质量
        QualityReport report = checkQuality(dataSourceId, tableName);
        
        // 保存报告
        saveReport(report, rule.getId());
        
        // 检查是否需要告警
        if (rule.getThreshold() != null && report.getScore() < rule.getThreshold().doubleValue()) {
            sendQualityAlert(rule, report);
        }
    }

    /**
     * 获取质量报告历史
     */
    public Page<DataQualityReport> getReportHistory(Long dataSourceId, String tableName, int page, int size) {
        LambdaQueryWrapper<DataQualityReport> wrapper = new LambdaQueryWrapper<>();
        if (dataSourceId != null) {
            wrapper.eq(DataQualityReport::getDataSourceId, dataSourceId);
        }
        if (tableName != null && !tableName.isEmpty()) {
            wrapper.eq(DataQualityReport::getTableName, tableName);
        }
        wrapper.orderByDesc(DataQualityReport::getCreateTime);
        
        return reportMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 获取最新报告
     */
    public DataQualityReport getLatestReport(Long dataSourceId, String tableName) {
        LambdaQueryWrapper<DataQualityReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataQualityReport::getDataSourceId, dataSourceId)
               .eq(DataQualityReport::getTableName, tableName)
               .orderByDesc(DataQualityReport::getCreateTime)
               .last("LIMIT 1");
        
        return reportMapper.selectOne(wrapper);
    }

    private FieldQualityDetail analyzeField(JdbcTemplate jdbcTemplate, String tableName, 
                                            String fieldName, String dataType, Long totalRows) {
        FieldQualityDetail detail = new FieldQualityDetail();
        detail.setFieldName(fieldName);
        
        try {
            // 空值率
            Long nullCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE " + fieldName + " IS NULL",
                Long.class);
            detail.setNullRate(nullCount != null ? (double) nullCount / totalRows : 0);
            
            // 重复率
            Long distinctCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT " + fieldName + ") FROM " + tableName,
                Long.class);
            detail.setDuplicateRate(distinctCount != null ? 
                1.0 - (double) distinctCount / totalRows : 0);
            
            // 类型一致性（简化：假设都一致）
            detail.setTypeConsistency(1.0);
            
            // 样本值
            List<Map<String, Object>> samples = jdbcTemplate.queryForList(
                "SELECT DISTINCT " + fieldName + " FROM " + tableName + " LIMIT 5");
            List<Object> sampleValues = new ArrayList<>();
            for (Map<String, Object> row : samples) {
                sampleValues.add(row.get(fieldName));
            }
            detail.setSampleValues(sampleValues);
            
        } catch (Exception e) {
            log.warn("Failed to analyze field: {}", fieldName, e);
            detail.setNullRate(0);
            detail.setDuplicateRate(0);
            detail.setTypeConsistency(1.0);
        }
        
        return detail;
    }

    private double calculateFieldScore(FieldQualityDetail detail) {
        // 空值率权重 40%，重复率权重 30%，类型一致性权重 30%
        double nullScore = (1 - detail.getNullRate()) * 40;
        double dupScore = (1 - detail.getDuplicateRate()) * 30;
        double typeScore = detail.getTypeConsistency() * 30;
        
        return nullScore + dupScore + typeScore;
    }

    private void saveReport(QualityReport report, Long ruleId) {
        try {
            DataQualityReport entity = new DataQualityReport();
            entity.setRuleId(ruleId);
            entity.setDataSourceId(report.getDataSourceId());
            entity.setTableName(report.getTableName());
            entity.setScore((int) Math.round(report.getScore()));
            entity.setDetailJson(objectMapper.writeValueAsString(report.getDetails()));
            entity.setCreateTime(LocalDateTime.now());
            
            reportMapper.insert(entity);
        } catch (Exception e) {
            log.error("Failed to save report", e);
        }
    }

    private void sendQualityAlert(DataQualityRule rule, QualityReport report) {
        try {
            String message = String.format(
                "数据质量告警：表 %s 质量评分 %.1f 低于阈值 %s",
                rule.getTableName(), report.getScore(), 
                rule.getThreshold() != null ? rule.getThreshold().toString() : "N/A");
            
            alertService.createAlert(
                "数据质量告警",
                message,
                "warning",
                "data_quality",
                rule.getTableName()
            );
        } catch (Exception e) {
            log.error("Failed to send quality alert", e);
        }
    }

    // 内部类
    @Data
    public static class QualityReport {
        private Long dataSourceId;
        private String tableName;
        private double score;
        private List<FieldQualityDetail> details;
    }

    @Data
    public static class FieldQualityDetail {
        private String fieldName;
        private double nullRate;
        private double duplicateRate;
        private double typeConsistency;
        private List<Object> sampleValues;
    }
}
