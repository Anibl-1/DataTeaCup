package com.dataplatform.data.service;

import com.dataplatform.common.constants.Constants;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.entity.ChartDefinition;
import com.dataplatform.data.entity.DataSource;
import com.dataplatform.data.mapper.ChartDefinitionMapper;
import com.dataplatform.data.mapper.DataSourceMapper;
import com.dataplatform.data.service.chart.ChartCoreService;
import com.dataplatform.data.service.chart.ChartConfigService;
import com.dataplatform.data.service.DbConnectionUtil;
import com.dataplatform.common.util.SqlSecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图表定义服务 - 对外接口层
 * 
 * 委托ChartCoreService处理核心逻辑，保持向后兼容
 * AI创建和手动创建图表共用相同的核心服务
 */
@Slf4j
@Service
public class ChartDefinitionService {
    @Autowired
    private ChartDefinitionMapper chartDefinitionMapper;
    
    @Autowired
    private DataSourceMapper dataSourceMapper;
    
    @Autowired
    private DataSourceService dataSourceService;
    
    @Autowired
    private DbConnectionUtil dbConnectionUtil;
    
    @Autowired
    private com.dataplatform.data.service.DataSourceConnectionPoolManager connectionPoolManager;
    
    @Autowired
    @Lazy  // 避免循环依赖
    private ChartCoreService chartCoreService;
    
    @Autowired
    private ChartConfigService chartConfigService;
    
    public List<ChartDefinition> getChartDefinitionList(Integer page, Integer pageSize, String keyword, String chartType, Integer status) {
        if (page == null || page < 1) {
            page = Constants.DEFAULT_PAGE;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = Constants.DEFAULT_PAGE_SIZE;
        }
        int offset = (page - 1) * pageSize;
        return chartDefinitionMapper.selectList(offset, pageSize, keyword, chartType, status);
    }
    
    public long getChartDefinitionCount(String keyword, String chartType, Integer status) {
        return chartDefinitionMapper.count(keyword, chartType, status);
    }
    
    public ChartDefinition getChartDefinitionById(Long id) {
        return chartDefinitionMapper.selectById(id);
    }
    
    public ChartDefinition getChartDefinitionByCode(String chartCode) {
        return chartDefinitionMapper.selectByCode(chartCode);
    }
    
    /**
     * 创建图表定义 - 委托给核心服务
     * AI创建和手动创建都使用此方法
     */
    @Transactional
    public ChartDefinition createChartDefinition(ChartDefinition chart) {
        // 委托给核心服务，统一处理逻辑
        return chartCoreService.createChart(chart);
    }
    
    /**
     * 更新图表定义 - 委托给核心服务
     */
    @Transactional
    public ChartDefinition updateChartDefinition(ChartDefinition chart) {
        // 委托给核心服务，统一处理逻辑
        return chartCoreService.updateChart(chart);
    }
    
    /**
     * 删除图表定义 - 委托给核心服务
     */
    @Transactional
    public void deleteChartDefinition(Long id) {
        chartCoreService.deleteChart(id);
    }
    
    public List<Map<String, Object>> executeChartQuery(Long chartId, String filters, Integer limit) {
        return executeChartQuery(chartId, filters, limit, null);
    }
    
    public List<Map<String, Object>> executeChartQuery(Long chartId, String filters, Integer limit, String parameters) {
        ChartDefinition chart = chartDefinitionMapper.selectById(chartId);
        if (chart == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Chart definition not found");
        }
        if (chart.getStatus() != Constants.STATUS_ENABLED) {
            throw new BusinessException(ErrorCode.RESOURCE_DISABLED, "Chart is disabled");
        }
        
        DataSource dataSource = dataSourceMapper.selectById(chart.getDataSourceId());
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "Data source not found");
        }
        
        if (limit == null || limit <= 0) {
            limit = 10000;
        }
        if (limit > 50000) {
            limit = 50000;
        }
        
        String url = dataSourceService.buildJdbcUrl(dataSource);
        String driver = dbConnectionUtil.getDriverClassName(dataSource.getDbType());
        
        try {
            Class.forName(driver);
            List<Map<String, Object>> result = new ArrayList<>();
            
            try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
                List<com.dataplatform.common.dto.FilterCondition> filterList = com.dataplatform.common.util.FilterUtil.parseFilters(filters);
                Map<String, Object> paramMap = parseParameters(parameters);
                
                String baseSql = chart.getSqlContent();
                String finalSql = baseSql;
                List<Object> sqlParams = new ArrayList<>();
                
                // 🔧 调试日志
                log.info("【图表查询】原始SQL: {}", baseSql);
                log.info("【图表查询】参数Map: {}", paramMap);
                
                finalSql = processParameterPlaceholders(finalSql, paramMap, sqlParams);
                
                log.info("【图表查询】处理后SQL: {}", finalSql);
                log.info("【图表查询】SQL参数: {}", sqlParams);
                
                // 如果有参数但SQL中没有对应的占位符，自动添加WHERE条件
                if (paramMap != null && !paramMap.isEmpty()) {
                    List<String> autoConditions = new ArrayList<>();
                    List<Object> autoParams = new ArrayList<>();
                    
                    for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                        String paramName = entry.getKey();
                        Object paramObj = entry.getValue();
                        
                        // 检查这个参数是否已经被处理（在sqlParams中）
                        String placeholder1 = ":" + paramName;
                        String placeholder2 = "${" + paramName + "}";
                        
                        if (!baseSql.contains(placeholder1) && !baseSql.contains(placeholder2)) {
                            // 参数没有在SQL中使用，自动添加为WHERE条件
                            Object paramValue;
                            String operator = "=";
                            String fieldName = paramName; // 🔧 默认使用参数名作为字段名
                            
                            if (paramObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> paramMap2 = (Map<String, Object>) paramObj;
                                paramValue = paramMap2.get("value");
                                operator = (String) paramMap2.getOrDefault("operator", "=");
                                // 🔧 优先使用field属性作为数据库字段名
                                if (paramMap2.containsKey("field") && paramMap2.get("field") != null) {
                                    fieldName = paramMap2.get("field").toString();
                                }
                            } else {
                                paramValue = paramObj;
                            }
                            
                            if (paramValue == null || paramValue.toString().trim().isEmpty()) {
                                continue;
                            }
                            
                            log.info("【自动条件】参数: {}, 字段: {}, 运算符: {}, 值: {}", paramName, fieldName, operator, paramValue);
                            
                            // 根据运算符生成条件（使用fieldName而非paramName）
                            String condition = buildCondition(fieldName, operator);
                            if (condition != null) {
                                autoConditions.add(condition);
                                
                                // 处理 LIKE 运算符
                                if ("LIKE".equalsIgnoreCase(operator)) {
                                    autoParams.add("%" + paramValue + "%");
                                } else if ("IN".equalsIgnoreCase(operator) || "NOT IN".equalsIgnoreCase(operator)) {
                                    // IN 运算符需要特殊处理
                                    if (paramValue instanceof List) {
                                        @SuppressWarnings("unchecked")
                                        List<Object> values = (List<Object>) paramValue;
                                        autoParams.addAll(values);
                                    } else {
                                        autoParams.add(paramValue);
                                    }
                                } else {
                                    autoParams.add(paramValue);
                                }
                            }
                        }
                    }
                    
                    if (!autoConditions.isEmpty()) {
                        String whereClause = String.join(" AND ", autoConditions);
                        finalSql = "SELECT * FROM (" + finalSql + ") AS param_filtered WHERE " + whereClause;
                        sqlParams.addAll(autoParams);
                    }
                }
                
                if (filterList != null && !filterList.isEmpty()) {
                    com.dataplatform.common.util.FilterUtil.FilterWhereClause whereClause = 
                        com.dataplatform.common.util.FilterUtil.buildDynamicWhereClause(filterList);
                    if (!whereClause.getWhereClause().isEmpty()) {
                        finalSql = "SELECT * FROM (" + finalSql + ") AS chart_data " + whereClause.getWhereClause();
                        sqlParams.addAll(whereClause.getParameters());
                    }
                }
                
                // 🔧 在执行前移除未替换的参数占位符
                finalSql = removeUnreplacedParameters(finalSql);
                
                finalSql = addLimitClause(finalSql, dataSource.getDbType(), 1, limit);
                
                // 🔧 最终执行日志
                log.info("【图表查询】最终SQL: {}", finalSql);
                log.info("【图表查询】最终参数: {} (共{}个)", sqlParams, sqlParams.size());
                
                try (PreparedStatement ps = conn.prepareStatement(finalSql)) {
                    for (int i = 0; i < sqlParams.size(); i++) {
                        ps.setObject(i + 1, sqlParams.get(i));
                    }
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        
                        while (rs.next()) {
                            Map<String, Object> row = new HashMap<>();
                            for (int i = 1; i <= columnCount; i++) {
                                row.put(metaData.getColumnName(i), rs.getObject(i));
                            }
                            result.add(row);
                        }
                    }
                }
            }
            
            return result;
        } catch (ClassNotFoundException e) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                "Database driver load failed: " + e.getMessage());
        } catch (SQLException e) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                "SQL execution failed: " + e.getMessage());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ERROR, "Execute SQL failed: " + e.getMessage());
        }
    }
    
    private Map<String, Object> parseParameters(String parameters) {
        if (parameters == null || parameters.trim().isEmpty()) {
            return null;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> rawMap = mapper.readValue(parameters, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            
            // 处理参数格式，支持简单值和带运算符的对象
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                String key = entry.getKey();
                Object val = entry.getValue();
                
                if (val instanceof Map) {
                    // 带运算符的格式: { value: xxx, operator: '=' }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> paramObj = (Map<String, Object>) val;
                    result.put(key, paramObj);
                } else {
                    // 简单值格式，包装成对象
                    Map<String, Object> paramObj = new HashMap<>();
                    paramObj.put("value", val);
                    paramObj.put("operator", "=");
                    result.put(key, paramObj);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("解析参数失败: {}", e.getMessage());
            return null;
        }
    }
    
    private String processParameterPlaceholders(String sql, Map<String, Object> paramMap, List<Object> sqlParams) {
        if (sql == null || sql.isEmpty()) {
            return sql;
        }
        
        String result = sql;
        
        // 辅助方法：从参数对象中提取值
        java.util.function.Function<Object, Object> extractValue = (paramObj) -> {
            if (paramObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) paramObj;
                return map.get("value");
            }
            return paramObj;
        };
        
        // 辅助方法：检查参数是否有值
        java.util.function.Function<Object, Boolean> hasParamValueFn = (paramObj) -> {
            Object value = extractValue.apply(paramObj);
            return value != null && !value.toString().trim().isEmpty();
        };
        
        // 🔧 优先处理AI生成的可选参数模式，避免参数数量不匹配
        // 模式: (${param} IS NULL OR ${param} = '' OR field = '${param}')
        // 或:   (${param} IS NULL OR ${param} = '' OR field = ${param})
        // 或:   (${param} IS NULL OR ${param} = '')
        java.util.regex.Pattern aiOptPattern = java.util.regex.Pattern.compile(
            "\\(\\s*\\$\\{([^}]+)\\}\\s+IS\\s+NULL\\s+OR\\s+\\$\\{[^}]+\\}\\s*=\\s*''(?:\\s+OR\\s+([`\\w\\u4e00-\\u9fa5.]+)\\s*=\\s*'?\\$\\{[^}]+\\}'?)?\\s*\\)",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher aiMatcher = aiOptPattern.matcher(result);
        StringBuffer aiSb = new StringBuffer();
        while (aiMatcher.find()) {
            String paramName = aiMatcher.group(1);
            String fieldName = aiMatcher.group(2); // 可能为null（两条件模式）
            
            boolean hasValue = paramMap != null && paramMap.containsKey(paramName) 
                && hasParamValueFn.apply(paramMap.get(paramName));
            
            if (hasValue && fieldName != null) {
                // 参数有值且有字段名，简化为: field = ?
                Object value = extractValue.apply(paramMap.get(paramName));
                aiMatcher.appendReplacement(aiSb, java.util.regex.Matcher.quoteReplacement(fieldName + " = ?"));
                sqlParams.add(value);
                log.info("【AI参数模式】{} 有值={}, 简化为 {} = ?", paramName, value, fieldName);
            } else if (hasValue) {
                // 参数有值但无字段名，替换为 1=1
                aiMatcher.appendReplacement(aiSb, "1=1");
                log.info("【AI参数模式】{} 有值但无字段, 替换为1=1", paramName);
            } else {
                // 参数无值，移除整个条件
                aiMatcher.appendReplacement(aiSb, "1=1");
                log.info("【AI参数模式】{} 无值, 替换为1=1", paramName);
            }
        }
        aiMatcher.appendTail(aiSb);
        result = aiSb.toString();
        
        // 清理 WHERE 1=1 AND 1=1 等残留
        result = result.replaceAll("\\b1=1\\s+AND\\s+1=1\\b", "1=1");
        result = result.replaceAll("WHERE\\s+1=1\\s*(?=ORDER|GROUP|LIMIT|$)", "");
        
        // 使用已声明的辅助方法
        java.util.function.Function<Object, Boolean> hasParamValue = hasParamValueFn;
        
        // Process marked conditions
        java.util.regex.Pattern markedConditionPattern = java.util.regex.Pattern.compile(
            "/\\*(optional|required)\\*/([^\\n]+?)(?=\\s+AND\\s+|\\s+OR\\s+|\\s*$|\\s*\\n)",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        
        java.util.regex.Matcher matcher = markedConditionPattern.matcher(result);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String marker = matcher.group(1).toLowerCase();
            String condition = matcher.group(2).trim();
            
            java.util.regex.Pattern paramPattern = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}");
            java.util.regex.Matcher paramMatcher = paramPattern.matcher(condition);
            
            boolean hasValue = true;
            List<String> paramNames = new ArrayList<>();
            
            while (paramMatcher.find()) {
                String paramName = paramMatcher.group(1);
                paramNames.add(paramName);
                
                if (paramMap == null || !paramMap.containsKey(paramName) || 
                    !hasParamValue.apply(paramMap.get(paramName))) {
                    hasValue = false;
                }
            }
            
            if ("optional".equals(marker) && !hasValue) {
                matcher.appendReplacement(sb, "1=1");
            } else if (!hasValue) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, 
                    "Required parameter missing: " + String.join(", ", paramNames));
            } else if (paramMap != null) {
                String processedCondition = condition;
                for (String paramName : paramNames) {
                    Object value = extractValue.apply(paramMap.get(paramName));
                    processedCondition = processedCondition.replace("${" + paramName + "}", "?");
                    sqlParams.add(value);
                }
                matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(processedCondition));
            }
        }
        matcher.appendTail(sb);
        result = sb.toString();
        
        // Process old format :paramName placeholders
        if (paramMap != null && !paramMap.isEmpty()) {
            for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                String paramName = entry.getKey();
                Object paramValue = extractValue.apply(entry.getValue());
                String placeholder = ":" + paramName;
                
                if (result.contains(placeholder)) {
                    result = result.replace(placeholder, "?");
                    sqlParams.add(paramValue);
                }
            }
        }
        
        // 🔧 先处理有值的参数，将 ${paramName} 替换为 ? 并添加到 sqlParams
        java.util.regex.Pattern paramPattern = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}");
        java.util.regex.Matcher paramMatcher = paramPattern.matcher(result);
        StringBuffer sb2 = new StringBuffer();
        
        while (paramMatcher.find()) {
            String paramName = paramMatcher.group(1);
            if (paramMap != null && paramMap.containsKey(paramName) && hasParamValue.apply(paramMap.get(paramName))) {
                // 参数有值，替换为 ?
                Object value = extractValue.apply(paramMap.get(paramName));
                paramMatcher.appendReplacement(sb2, "?");
                sqlParams.add(value);
            } else {
                // 参数无值，保留占位符（稍后移除条件）
                paramMatcher.appendReplacement(sb2, java.util.regex.Matcher.quoteReplacement(paramMatcher.group(0)));
            }
        }
        paramMatcher.appendTail(sb2);
        result = sb2.toString();
        
        // 🔧 移除包含未提供参数的整个条件（避免SQL语法错误）
        // 🆕 AI生成的可选参数模式 (${param} IS NULL OR ${param} = '' OR field = ${param}) - 支持中文
        result = result.replaceAll("(?i)\\s*\\(\\s*\\$\\{[^}]+\\}\\s+IS\\s+NULL\\s+OR\\s+\\$\\{[^}]+\\}\\s*=\\s*''\\s+OR\\s+[`\\w\\u4e00-\\u9fa5.]+\\s*=\\s*\\$\\{[^}]+\\}\\s*\\)", "");
        result = result.replaceAll("(?i)\\s+AND\\s*\\(\\s*\\$\\{[^}]+\\}\\s+IS\\s+NULL\\s+OR\\s+\\$\\{[^}]+\\}\\s*=\\s*''\\s+OR\\s+[`\\w\\u4e00-\\u9fa5.]+\\s*=\\s*\\$\\{[^}]+\\}\\s*\\)", "");
        // 简化版: AND (${param} IS NULL OR field = ${param})
        result = result.replaceAll("(?i)\\s+AND\\s*\\(\\s*\\$\\{[^}]+\\}\\s+IS\\s+NULL\\s+OR\\s+[`\\w\\u4e00-\\u9fa5.]+\\s*=\\s*'?\\$\\{[^}]+\\}'?\\s*\\)", "");
        // 匹配 AND field = ${param} 或 AND field IN (${param}) 等模式（支持中文字段名）
        result = result.replaceAll("(?i)\\s+AND\\s+[`\\w\\u4e00-\\u9fa5.]+`?\\s*[=<>!]+\\s*'?\\$\\{[^}]+\\}'?", "");
        result = result.replaceAll("(?i)\\s+AND\\s+[`\\w\\u4e00-\\u9fa5.]+`?\\s+LIKE\\s+'%?\\$\\{[^}]+\\}%?'", "");
        result = result.replaceAll("(?i)\\s+AND\\s+[`\\w\\u4e00-\\u9fa5.]+`?\\s+IN\\s*\\([^)]*\\$\\{[^}]+\\}[^)]*\\)", "");
        result = result.replaceAll("(?i)\\s+OR\\s+[`\\w\\u4e00-\\u9fa5.]+`?\\s*[=<>!]+\\s*'?\\$\\{[^}]+\\}'?", "");
        // 清理空的WHERE
        result = result.replaceAll("(?i)WHERE\\s+ORDER", " ORDER");
        result = result.replaceAll("(?i)WHERE\\s+LIMIT", " LIMIT");
        result = result.replaceAll("(?i)WHERE\\s+GROUP", " GROUP");
        
        // Clean up redundant conditions
        result = result.replaceAll("\\bAND\\s+1=1\\b", "");
        result = result.replaceAll("\\b1=1\\s+AND\\b", "");
        result = result.replaceAll("WHERE\\s+1=1\\s*$", "");
        result = result.replaceAll("WHERE\\s+$", "");
        // 🔧 清理可能残留的空条件
        result = result.replaceAll("WHERE\\s+AND\\s+", "WHERE ");
        result = result.replaceAll("WHERE\\s+OR\\s+", "WHERE ");
        
        return result;
    }
    
    public List<Map<String, Object>> executeChartQuery(Long chartId) {
        return executeChartQuery(chartId, null, 10000);
    }
    
    public List<Map<String, Object>> testSql(Long dataSourceId, String sql, Integer limit) {
        if (dataSourceId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Data source ID is required");
        }
        if (!StringUtils.hasText(sql)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "SQL is required");
        }
        
        if (limit == null || limit <= 0) {
            limit = 100;
        }
        if (limit > 10000) {
            limit = 10000;
        }
        
        // 🔧 安全处理：移除未替换的参数占位符条件，防止SQL语法错误
        String processedSql = removeUnreplacedParameters(sql);
        
        DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "Data source not found");
        }
        
        SqlSecurityUtil.validateSql(processedSql);
        
        String url = dataSourceService.buildJdbcUrl(dataSource);
        String driver = dbConnectionUtil.getDriverClassName(dataSource.getDbType());
        
        try {
            Class.forName(driver);
            List<Map<String, Object>> result = new ArrayList<>();
            
            try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
                String limitSql = addLimitClause(processedSql, dataSource.getDbType(), 1, limit);
                
                try (PreparedStatement ps = conn.prepareStatement(limitSql)) {
                    try (ResultSet rs = ps.executeQuery()) {
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        
                        while (rs.next()) {
                            Map<String, Object> row = new HashMap<>();
                            for (int i = 1; i <= columnCount; i++) {
                                row.put(metaData.getColumnName(i), rs.getObject(i));
                            }
                            result.add(row);
                        }
                    }
                }
            }
            
            return result;
        } catch (ClassNotFoundException e) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                "Database driver load failed: " + e.getMessage());
        } catch (SQLException e) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                "SQL execution failed: " + e.getMessage());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ERROR, "Execute SQL failed: " + e.getMessage());
        }
    }
    
    public List<Map<String, Object>> testSql(Long dataSourceId, String sql) {
        return testSql(dataSourceId, sql, 100);
    }
    
    /**
     * 移除SQL中未替换的参数占位符条件，防止SQL语法错误
     * 支持多种AI生成的可选参数模式
     */
    private String removeUnreplacedParameters(String sql) {
        if (sql == null || !sql.contains("${")) {
            return sql;
        }
        
        String result = sql;
        
        // 🔧 通用方案：移除所有包含 ${...} 的括号条件
        while (result.matches("(?s).*\\([^()]*\\$\\{[^}]+\\}[^()]*\\).*")) {
            result = result.replaceAll("\\s*\\([^()]*\\$\\{[^}]+\\}[^()]*\\)", "");
        }
        
        // 移除残留的 AND/OR 条件
        result = result.replaceAll("(?i)\\s+AND\\s+[^AND|OR]*\\$\\{[^}]+\\}[^AND|OR]*(?=\\s+AND|\\s+OR|\\s+ORDER|\\s+GROUP|\\s+LIMIT|\\s*$)", "");
        result = result.replaceAll("(?i)\\s+OR\\s+[^AND|OR]*\\$\\{[^}]+\\}[^AND|OR]*(?=\\s+AND|\\s+OR|\\s+ORDER|\\s+GROUP|\\s+LIMIT|\\s*$)", "");
        
        // 清理残留语法问题
        result = result.replaceAll("(?i)WHERE\\s+AND\\s+", "WHERE ");
        result = result.replaceAll("(?i)WHERE\\s+OR\\s+", "WHERE ");
        result = result.replaceAll("(?i)WHERE\\s+ORDER", " ORDER");
        result = result.replaceAll("(?i)WHERE\\s+LIMIT", " LIMIT");
        result = result.replaceAll("(?i)WHERE\\s+GROUP", " GROUP");
        result = result.replaceAll("(?i)WHERE\\s*$", "");
        
        // 最终检查：如果还有${...}，直接替换为NULL
        if (result.contains("${")) {
            log.warn("SQL中仍有未替换的参数占位符，替换为NULL: {}", result);
            result = result.replaceAll("\\$\\{[^}]+\\}", "NULL");
        }
        
        return result;
    }
    
    /**
     * 复制图表定义
     */
    @Transactional
    public ChartDefinition copyChartDefinition(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Chart ID is required");
        }
        
        ChartDefinition source = chartDefinitionMapper.selectById(id);
        if (source == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Chart definition not found");
        }
        
        // 创建副本
        ChartDefinition copy = new ChartDefinition();
        copy.setChartName(source.getChartName() + "_copy");
        copy.setChartCode(source.getChartCode() + "_copy_" + System.currentTimeMillis());
        copy.setChartType(source.getChartType());
        copy.setDataSourceId(source.getDataSourceId());
        copy.setSqlContent(source.getSqlContent());
        copy.setChartConfig(source.getChartConfig());
        copy.setDescription(source.getDescription());
        copy.setStatus(Constants.STATUS_ENABLED);
        
        chartDefinitionMapper.insert(copy);
        return copy;
    }
    
    /**
     * 更新图表状态
     */
    @Transactional
    public ChartDefinition updateChartStatus(Long id, Integer status) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Chart ID is required");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Invalid status value");
        }
        
        ChartDefinition chart = chartDefinitionMapper.selectById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Chart definition not found");
        }
        
        chart.setStatus(status);
        chartDefinitionMapper.update(chart);
        return chartDefinitionMapper.selectById(id);
    }
    
    /**
     * 批量更新图表状态
     */
    @Transactional
    public int batchUpdateChartStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Chart IDs are required");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Invalid status value");
        }
        
        int count = 0;
        for (Long id : ids) {
            ChartDefinition chart = chartDefinitionMapper.selectById(id);
            if (chart != null) {
                chart.setStatus(status);
                chartDefinitionMapper.update(chart);
                count++;
            }
        }
        return count;
    }
    
    /**
     * 根据运算符构建SQL条件
     */
    private String buildCondition(String fieldName, String operator) {
        if (operator == null) operator = "=";
        
        switch (operator.toUpperCase()) {
            case "=":
                return fieldName + " = ?";
            case "!=":
            case "<>":
                return fieldName + " != ?";
            case ">":
                return fieldName + " > ?";
            case ">=":
                return fieldName + " >= ?";
            case "<":
                return fieldName + " < ?";
            case "<=":
                return fieldName + " <= ?";
            case "LIKE":
                return fieldName + " LIKE ?";
            case "IN":
                return fieldName + " IN (?)";
            case "NOT IN":
                return fieldName + " NOT IN (?)";
            default:
                return fieldName + " = ?";
        }
    }
    
    private String addLimitClause(String sql, String dbType, int page, int pageSize) {
        String sqlUpper = sql.trim().toUpperCase();
        if (sqlUpper.contains("LIMIT")) {
            return sql;
        }
        
        String dbTypeLower = dbType.toLowerCase();
        int offset = (page - 1) * pageSize;
        
        if ("mysql".equals(dbTypeLower)) {
            return sql + " LIMIT " + offset + ", " + pageSize;
        } else if ("postgresql".equals(dbTypeLower)) {
            return sql + " LIMIT " + pageSize + " OFFSET " + offset;
        } else if ("oracle".equals(dbTypeLower)) {
            return "SELECT * FROM (SELECT ROWNUM rn, t.* FROM (" + sql + ") t WHERE ROWNUM <= " + (offset + pageSize) + ") WHERE rn > " + offset;
        } else if ("sqlserver".equals(dbTypeLower)) {
            return "SELECT * FROM (SELECT ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn, * FROM (" + sql + ") t) t WHERE rn > " + offset + " AND rn <= " + (offset + pageSize);
        } else {
            return sql + " LIMIT " + offset + ", " + pageSize;
        }
    }
}
