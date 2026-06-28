package com.dataplatform.data.service.impl;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.entity.FieldStyleConfig;
import com.dataplatform.data.mapper.FieldStyleConfigMapper;
import com.dataplatform.data.service.FieldStyleConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 字段样式配置服务实现类
 * 支持字段样式配置的 CRUD 操作（需求 21.1.3）
 * 支持从数据库加载样式配置（需求 21.1.4）
 * 
 * @author dataplatform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FieldStyleConfigServiceImpl implements FieldStyleConfigService {
    
    private final FieldStyleConfigMapper fieldStyleConfigMapper;
    
    // ==================== CRUD 操作 ====================
    
    @Override
    @Transactional
    public FieldStyleConfig create(FieldStyleConfig config) {
        log.info("Creating field style config for report: {}, field: {}", config.getReportId(), config.getFieldName());
        
        // 检查是否已存在
        FieldStyleConfig existing = fieldStyleConfigMapper.findByReportIdAndFieldName(
                config.getReportId(), config.getFieldName());
        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, 
                    "字段样式配置已存在: reportId=" + config.getReportId() + ", fieldName=" + config.getFieldName());
        }
        
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        fieldStyleConfigMapper.insert(config);
        
        log.info("Created field style config with id: {}", config.getId());
        return config;
    }
    
    @Override
    @Transactional
    public FieldStyleConfig update(FieldStyleConfig config) {
        log.info("Updating field style config: {}", config.getId());
        
        FieldStyleConfig existing = fieldStyleConfigMapper.selectById(config.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "字段样式配置不存在: " + config.getId());
        }
        
        config.setUpdateTime(LocalDateTime.now());
        fieldStyleConfigMapper.updateById(config);
        
        log.info("Updated field style config: {}", config.getId());
        return fieldStyleConfigMapper.selectById(config.getId());
    }
    
    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Deleting field style config: {}", id);
        
        FieldStyleConfig existing = fieldStyleConfigMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "字段样式配置不存在: " + id);
        }
        
        fieldStyleConfigMapper.deleteById(id);
        log.info("Deleted field style config: {}", id);
    }
    
    @Override
    public FieldStyleConfig getById(Long id) {
        return fieldStyleConfigMapper.selectById(id);
    }
    
    // ==================== 查询方法 ====================
    
    @Override
    public List<FieldStyleConfig> getByReportId(Long reportId) {
        return fieldStyleConfigMapper.findByReportId(reportId);
    }
    
    @Override
    public FieldStyleConfig getByReportIdAndFieldName(Long reportId, String fieldName) {
        return fieldStyleConfigMapper.findByReportIdAndFieldName(reportId, fieldName);
    }
    
    @Override
    public List<FieldStyleConfig> getByCreatedBy(Long createdBy) {
        return fieldStyleConfigMapper.findByCreatedBy(createdBy);
    }
    
    // ==================== 批量操作 ====================
    
    @Override
    @Transactional
    public List<FieldStyleConfig> batchSave(Long reportId, List<FieldStyleConfig> configs) {
        log.info("Batch saving {} field style configs for report: {}", configs.size(), reportId);
        
        List<FieldStyleConfig> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (FieldStyleConfig config : configs) {
            config.setReportId(reportId);
            
            FieldStyleConfig existing = fieldStyleConfigMapper.findByReportIdAndFieldName(
                    reportId, config.getFieldName());
            
            if (existing != null) {
                // 更新已存在的配置
                config.setId(existing.getId());
                config.setCreateTime(existing.getCreateTime());
                config.setUpdateTime(now);
                fieldStyleConfigMapper.updateById(config);
            } else {
                // 创建新配置
                config.setCreateTime(now);
                config.setUpdateTime(now);
                fieldStyleConfigMapper.insert(config);
            }
            
            result.add(config);
        }
        
        log.info("Batch saved {} field style configs for report: {}", result.size(), reportId);
        return result;
    }
    
    @Override
    @Transactional
    public void deleteByReportId(Long reportId) {
        log.info("Deleting all field style configs for report: {}", reportId);
        int deleted = fieldStyleConfigMapper.deleteByReportId(reportId);
        log.info("Deleted {} field style configs for report: {}", deleted, reportId);
    }
    
    // ==================== 样式复制 ====================
    
    @Override
    @Transactional
    public List<FieldStyleConfig> copyStyles(Long sourceReportId, Long targetReportId) {
        log.info("Copying styles from report {} to report {}", sourceReportId, targetReportId);
        
        List<FieldStyleConfig> sourceConfigs = fieldStyleConfigMapper.findByReportId(sourceReportId);
        List<FieldStyleConfig> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (FieldStyleConfig source : sourceConfigs) {
            FieldStyleConfig target = FieldStyleConfig.builder()
                    .reportId(targetReportId)
                    .fieldName(source.getFieldName())
                    .styleConfig(source.getStyleConfig())
                    .conditionalRules(source.getConditionalRules())
                    .sortOrder(source.getSortOrder())
                    .createTime(now)
                    .updateTime(now)
                    .createdBy(source.getCreatedBy())
                    .build();
            
            fieldStyleConfigMapper.insert(target);
            result.add(target);
        }
        
        log.info("Copied {} styles from report {} to report {}", result.size(), sourceReportId, targetReportId);
        return result;
    }
}
