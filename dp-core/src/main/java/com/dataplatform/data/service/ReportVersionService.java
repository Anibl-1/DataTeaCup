package com.dataplatform.data.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataplatform.data.entity.ReportDefinition;
import com.dataplatform.data.entity.ReportVersion;
import com.dataplatform.data.mapper.ReportDefinitionMapper;
import com.dataplatform.data.mapper.ReportVersionMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 报表版本管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportVersionService {

    private final ReportVersionMapper versionMapper;
    private final ReportDefinitionMapper reportMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建版本
     */
    @Transactional
    public ReportVersion createVersion(Long reportId, String summary, Long userId) {
        log.info("Creating version for report: {}", reportId);
        
        // 获取报表
        ReportDefinition report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new RuntimeException("报表不存在");
        }
        
        // 获取最新版本号
        int nextVersionNo = getNextVersionNo(reportId);
        
        // 创建版本
        ReportVersion version = new ReportVersion();
        version.setReportId(reportId);
        version.setVersionNo(nextVersionNo);
        version.setConfigSnapshot(report.getConfigJson());
        version.setSqlSnapshot(report.getSqlContent());
        version.setSummary(summary);
        version.setCreateBy(userId);
        version.setCreateTime(LocalDateTime.now());
        
        versionMapper.insert(version);
        return version;
    }

    /**
     * 获取版本历史
     */
    public List<ReportVersion> getVersionHistory(Long reportId) {
        LambdaQueryWrapper<ReportVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportVersion::getReportId, reportId)
               .orderByDesc(ReportVersion::getVersionNo);
        return versionMapper.selectList(wrapper);
    }

    /**
     * 获取版本详情
     */
    public ReportVersion getVersion(Long versionId) {
        return versionMapper.selectById(versionId);
    }

    /**
     * 比较两个版本
     */
    public VersionCompareResult compareVersions(Long versionId1, Long versionId2) {
        ReportVersion v1 = versionMapper.selectById(versionId1);
        ReportVersion v2 = versionMapper.selectById(versionId2);
        
        if (v1 == null || v2 == null) {
            throw new RuntimeException("版本不存在");
        }
        
        VersionCompareResult result = new VersionCompareResult();
        result.setVersion1(v1);
        result.setVersion2(v2);
        result.setDiffs(new ArrayList<>());
        
        // 比较 SQL
        if (!Objects.equals(v1.getSqlSnapshot(), v2.getSqlSnapshot())) {
            VersionDiff diff = new VersionDiff();
            diff.setField("sql");
            diff.setOldValue(v1.getSqlSnapshot());
            diff.setNewValue(v2.getSqlSnapshot());
            diff.setChangeType("modified");
            result.getDiffs().add(diff);
        }
        
        // 比较配置
        try {
            Map<String, Object> config1 = parseConfig(v1.getConfigSnapshot());
            Map<String, Object> config2 = parseConfig(v2.getConfigSnapshot());
            
            compareConfigs(config1, config2, "", result.getDiffs());
        } catch (Exception e) {
            log.warn("Failed to compare configs", e);
        }
        
        return result;
    }

    /**
     * 回滚到指定版本
     */
    @Transactional
    public void rollbackToVersion(Long reportId, Long versionId) {
        log.info("Rolling back report {} to version {}", reportId, versionId);
        
        ReportVersion version = versionMapper.selectById(versionId);
        if (version == null || !version.getReportId().equals(reportId)) {
            throw new RuntimeException("版本不存在或不属于该报表");
        }
        
        ReportDefinition report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new RuntimeException("报表不存在");
        }
        
        // 更新报表
        report.setConfigJson(version.getConfigSnapshot());
        report.setSqlContent(version.getSqlSnapshot());
        report.setUpdateTime(new java.util.Date());
        
        reportMapper.update(report);
    }

    /**
     * 删除版本
     */
    @Transactional
    public void deleteVersion(Long versionId) {
        versionMapper.deleteById(versionId);
    }

    private int getNextVersionNo(Long reportId) {
        LambdaQueryWrapper<ReportVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportVersion::getReportId, reportId)
               .orderByDesc(ReportVersion::getVersionNo)
               .last("LIMIT 1");
        
        ReportVersion latest = versionMapper.selectOne(wrapper);
        return latest != null ? latest.getVersionNo() + 1 : 1;
    }

    private Map<String, Object> parseConfig(String configJson) {
        if (configJson == null || configJson.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(configJson, new TypeReference<>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private void compareConfigs(Map<String, Object> config1, Map<String, Object> config2, 
                               String prefix, List<VersionDiff> diffs) {
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(config1.keySet());
        allKeys.addAll(config2.keySet());
        
        for (String key : allKeys) {
            String fieldName = prefix.isEmpty() ? key : prefix + "." + key;
            Object val1 = config1.get(key);
            Object val2 = config2.get(key);
            
            if (val1 == null && val2 != null) {
                VersionDiff diff = new VersionDiff();
                diff.setField(fieldName);
                diff.setOldValue(null);
                diff.setNewValue(val2);
                diff.setChangeType("added");
                diffs.add(diff);
            } else if (val1 != null && val2 == null) {
                VersionDiff diff = new VersionDiff();
                diff.setField(fieldName);
                diff.setOldValue(val1);
                diff.setNewValue(null);
                diff.setChangeType("removed");
                diffs.add(diff);
            } else if (!Objects.equals(val1, val2)) {
                VersionDiff diff = new VersionDiff();
                diff.setField(fieldName);
                diff.setOldValue(val1);
                diff.setNewValue(val2);
                diff.setChangeType("modified");
                diffs.add(diff);
            }
        }
    }

    // 内部类
    @Data
    public static class VersionCompareResult {
        private ReportVersion version1;
        private ReportVersion version2;
        private List<VersionDiff> diffs;
    }

    @Data
    public static class VersionDiff {
        private String field;
        private Object oldValue;
        private Object newValue;
        private String changeType; // added, removed, modified
    }
}
