package com.dataplatform.data.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 动态参数数据源服务
 * 支持从数据库动态获取参数选项
 * 
 * 功能：
 * - 执行SQL查询获取参数选项
 * - 支持SQL参数替换（${parentValue}）
 * - 缓存常用选项
 * - 安全的SQL参数处理
 * 
 * @validates 需求 13.2 - 从数据库动态获取参数选项值
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicParameterService {

    private final DataSourceService dataSourceService;
    
    /** 参数选项缓存：cacheKey -> 选项列表，TTL 5分钟 */
    private Cache<String, List<ParameterOption>> optionsCache;
    
    /** SQL参数占位符模式：${paramName} */
    private static final Pattern PARAM_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    
    /** 最大结果数限制 */
    private static final int MAX_RESULTS = 1000;
    
    /** 默认结果数限制 */
    private static final int DEFAULT_LIMIT = 200;
    
    @PostConstruct
    public void initCache() {
        optionsCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats()
                .build();
        log.info("动态参数选项缓存初始化完成, TTL=5分钟, maxSize=500");
    }
    
    /**
     * 获取参数选项
     * 
     * @param dataSourceId 数据源ID
     * @param sql SQL查询语句，支持 ${paramName} 占位符
     * @param dependencies 依赖参数值映射
     * @param labelField 标签字段名（默认为第一列或"label"）
     * @param valueField 值字段名（默认为第二列或"value"）
     * @param useCache 是否使用缓存
     * @return 参数选项列表
     */
    public List<ParameterOption> getParameterOptions(
            Long dataSourceId,
            String sql,
            Map<String, Object> dependencies,
            String labelField,
            String valueField,
            boolean useCache) {
        
        // 参数验证
        if (dataSourceId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据源ID不能为空");
        }
        if (!StringUtils.hasText(sql)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "SQL语句不能为空");
        }
        
        // 替换SQL参数
        String processedSql = substituteParameters(sql, dependencies != null ? dependencies : Collections.emptyMap());
        
        // 生成缓存键
        String cacheKey = generateCacheKey(dataSourceId, processedSql, labelField, valueField);
        
        // 尝试从缓存获取
        if (useCache) {
            List<ParameterOption> cached = optionsCache.getIfPresent(cacheKey);
            if (cached != null) {
                log.debug("从缓存获取参数选项: cacheKey={}, size={}", cacheKey, cached.size());
                return cached;
            }
        }
        
        // 执行查询
        List<ParameterOption> options = executeQuery(dataSourceId, processedSql, labelField, valueField);
        
        // 写入缓存
        if (useCache && !options.isEmpty()) {
            optionsCache.put(cacheKey, options);
            log.debug("参数选项已缓存: cacheKey={}, size={}", cacheKey, options.size());
        }
        
        return options;
    }
    
    /**
     * 获取参数选项（使用缓存）
     */
    public List<ParameterOption> getParameterOptions(
            Long dataSourceId,
            String sql,
            Map<String, Object> dependencies,
            String labelField,
            String valueField) {
        return getParameterOptions(dataSourceId, sql, dependencies, labelField, valueField, true);
    }
    
    /**
     * 获取参数选项（使用默认字段名）
     */
    public List<ParameterOption> getParameterOptions(
            Long dataSourceId,
            String sql,
            Map<String, Object> dependencies) {
        return getParameterOptions(dataSourceId, sql, dependencies, null, null, true);
    }
    
    /**
     * 替换SQL中的参数占位符
     * 
     * @param sql 原始SQL
     * @param dependencies 参数值映射
     * @return 替换后的SQL
     */
    public String substituteParameters(String sql, Map<String, Object> dependencies) {
        if (sql == null || dependencies == null || dependencies.isEmpty()) {
            return sql;
        }
        
        StringBuffer result = new StringBuffer();
        Matcher matcher = PARAM_PATTERN.matcher(sql);
        
        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object value = dependencies.get(paramName);
            
            // 安全处理参数值
            String replacement = escapeValue(value);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 安全转义参数值，防止SQL注入
     */
    private String escapeValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        
        if (value instanceof Number) {
            return value.toString();
        }
        
        if (value instanceof Boolean) {
            return (Boolean) value ? "1" : "0";
        }
        
        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            if (collection.isEmpty()) {
                return "NULL";
            }
            StringJoiner joiner = new StringJoiner(", ");
            for (Object item : collection) {
                joiner.add(escapeValue(item));
            }
            return joiner.toString();
        }
        
        // 字符串类型：转义单引号
        String strValue = value.toString();
        strValue = strValue.replace("'", "''");
        // 移除可能的SQL注入字符
        strValue = strValue.replaceAll("[;\\-\\-]", "");
        return "'" + strValue + "'";
    }
    
    /**
     * 执行SQL查询获取参数选项
     */
    private List<ParameterOption> executeQuery(
            Long dataSourceId,
            String sql,
            String labelField,
            String valueField) {
        
        List<ParameterOption> options = new ArrayList<>();
        
        try {
            DataSource ds = dataSourceService.getDataSource(dataSourceId);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
            
            // 添加结果数限制
            String limitedSql = addLimitIfNeeded(sql);
            
            log.debug("执行参数选项查询: dataSourceId={}, sql={}", dataSourceId, limitedSql);
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(limitedSql);
            
            if (results.isEmpty()) {
                return options;
            }
            
            // 确定标签和值字段
            Map<String, Object> firstRow = results.get(0);
            String actualLabelField = determineLabelField(firstRow, labelField);
            String actualValueField = determineValueField(firstRow, valueField, actualLabelField);
            
            // 转换结果为选项列表
            for (Map<String, Object> row : results) {
                ParameterOption option = new ParameterOption();
                
                Object labelValue = row.get(actualLabelField);
                Object valueValue = row.get(actualValueField);
                
                option.setLabel(labelValue != null ? labelValue.toString() : "");
                option.setValue(valueValue != null ? valueValue.toString() : "");
                
                // 检查是否有禁用标记
                if (row.containsKey("disabled")) {
                    Object disabled = row.get("disabled");
                    option.setDisabled(isTrue(disabled));
                }
                
                options.add(option);
            }
            
            log.debug("参数选项查询完成: dataSourceId={}, count={}", dataSourceId, options.size());
            
        } catch (Exception e) {
            log.error("参数选项查询失败: dataSourceId={}, sql={}, error={}", 
                    dataSourceId, sql, e.getMessage());
            throw new BusinessException(ErrorCode.ERROR, "获取参数选项失败: " + e.getMessage());
        }
        
        return options;
    }
    
    /**
     * 确定标签字段名
     */
    private String determineLabelField(Map<String, Object> row, String specifiedField) {
        if (StringUtils.hasText(specifiedField) && row.containsKey(specifiedField)) {
            return specifiedField;
        }
        
        // 优先查找常用标签字段名
        String[] commonLabelFields = {"label", "name", "text", "title", "display_name", "displayName"};
        for (String field : commonLabelFields) {
            if (row.containsKey(field)) {
                return field;
            }
        }
        
        // 使用第一个字段
        return row.keySet().iterator().next();
    }
    
    /**
     * 确定值字段名
     */
    private String determineValueField(Map<String, Object> row, String specifiedField, String labelField) {
        if (StringUtils.hasText(specifiedField) && row.containsKey(specifiedField)) {
            return specifiedField;
        }
        
        // 优先查找常用值字段名
        String[] commonValueFields = {"value", "id", "code", "key"};
        for (String field : commonValueFields) {
            if (row.containsKey(field) && !field.equals(labelField)) {
                return field;
            }
        }
        
        // 使用第二个字段（如果有），否则使用标签字段
        Iterator<String> iterator = row.keySet().iterator();
        String first = iterator.next();
        if (iterator.hasNext()) {
            String second = iterator.next();
            if (!second.equals(labelField)) {
                return second;
            }
        }
        
        return labelField;
    }
    
    /**
     * 添加LIMIT限制（如果SQL中没有）
     */
    private String addLimitIfNeeded(String sql) {
        String upperSql = sql.toUpperCase().trim();
        if (!upperSql.contains("LIMIT")) {
            return sql + " LIMIT " + DEFAULT_LIMIT;
        }
        return sql;
    }
    
    /**
     * 判断值是否为true
     */
    private boolean isTrue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        String str = value.toString().toLowerCase();
        return "true".equals(str) || "1".equals(str) || "yes".equals(str);
    }
    
    /**
     * 生成缓存键
     */
    private String generateCacheKey(Long dataSourceId, String sql, String labelField, String valueField) {
        StringBuilder key = new StringBuilder();
        key.append("param_options:");
        key.append(dataSourceId).append(":");
        key.append(sql.hashCode());
        if (StringUtils.hasText(labelField)) {
            key.append(":l=").append(labelField);
        }
        if (StringUtils.hasText(valueField)) {
            key.append(":v=").append(valueField);
        }
        return key.toString();
    }
    
    /**
     * 清除指定数据源的缓存
     */
    public void clearCache(Long dataSourceId) {
        String prefix = "param_options:" + dataSourceId + ":";
        optionsCache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
        log.info("已清除数据源 {} 的参数选项缓存", dataSourceId);
    }
    
    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        optionsCache.invalidateAll();
        log.info("已清除所有参数选项缓存");
    }
    
    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        var cacheStats = optionsCache.stats();
        stats.put("hitCount", cacheStats.hitCount());
        stats.put("missCount", cacheStats.missCount());
        stats.put("hitRate", cacheStats.hitRate());
        stats.put("evictionCount", cacheStats.evictionCount());
        stats.put("size", optionsCache.estimatedSize());
        return stats;
    }
    
    /**
     * 参数选项
     */
    public static class ParameterOption {
        private String label;
        private String value;
        private Boolean disabled;
        private List<ParameterOption> children;
        
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public Boolean getDisabled() { return disabled; }
        public void setDisabled(Boolean disabled) { this.disabled = disabled; }
        public List<ParameterOption> getChildren() { return children; }
        public void setChildren(List<ParameterOption> children) { this.children = children; }
    }
}
