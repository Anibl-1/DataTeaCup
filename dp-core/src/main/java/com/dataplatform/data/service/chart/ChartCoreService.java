package com.dataplatform.data.service.chart;

import com.dataplatform.common.constants.Constants;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.entity.ChartDefinition;
import com.dataplatform.data.entity.DataSource;
import com.dataplatform.data.mapper.ChartDefinitionMapper;
import com.dataplatform.data.mapper.DataSourceMapper;
import com.dataplatform.data.service.DataSourceService;
import com.dataplatform.data.service.DbConnectionUtil;
import com.dataplatform.common.util.SqlSecurityUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 图表核心服务 - 统一的图表CRUD和SQL执行
 * 
 * 职责：
 * 1. 图表定义的增删改查
 * 2. SQL参数处理和执行
 * 3. 图表配置验证
 * 
 * 被 ChartDefinitionService 和 AiChartService 共同使用
 */
@Slf4j
@Service
public class ChartCoreService {

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
    private ChartConfigService chartConfigService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // SQL参数占位符正则
    private static final Pattern PARAM_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    private static final Pattern NAMED_PARAM_PATTERN = Pattern.compile(":([a-zA-Z_][a-zA-Z0-9_]*)");
    
    // ==================== 图表CRUD操作 ====================
    
    /**
     * 获取图表列表（分页）
     */
    public List<ChartDefinition> getChartList(Integer page, Integer pageSize, String keyword, String chartType, Integer status) {
        if (page == null || page < 1) page = Constants.DEFAULT_PAGE;
        if (pageSize == null || pageSize < 1) pageSize = Constants.DEFAULT_PAGE_SIZE;
        int offset = (page - 1) * pageSize;
        return chartDefinitionMapper.selectList(offset, pageSize, keyword, chartType, status);
    }
    
    /**
     * 获取图表总数
     */
    public long getChartCount(String keyword, String chartType, Integer status) {
        return chartDefinitionMapper.count(keyword, chartType, status);
    }
    
    /**
     * 根据ID获取图表
     */
    public ChartDefinition getChartById(Long id) {
        return chartDefinitionMapper.selectById(id);
    }
    
    /**
     * 根据编码获取图表
     */
    public ChartDefinition getChartByCode(String chartCode) {
        return chartDefinitionMapper.selectByCode(chartCode);
    }
    
    /**
     * 创建图表定义（统一入口）
     * AI生成和手动创建都使用此方法
     */
    @Transactional
    public ChartDefinition createChart(ChartDefinition chart) {
        // 1. 参数验证
        validateChartDefinition(chart, true);
        
        // 2. 检查编码唯一性
        if (chartDefinitionMapper.selectByCode(chart.getChartCode()) != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图表编码已存在: " + chart.getChartCode());
        }
        
        // 3. 验证数据源
        DataSource dataSource = dataSourceMapper.selectById(chart.getDataSourceId());
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        
        // 4. SQL安全验证
        SqlSecurityUtil.validateSql(chart.getSqlContent());
        
        // 5. 图表配置验证和修复
        if (StringUtils.hasText(chart.getChartConfig())) {
            String fixedConfig = chartConfigService.validateAndFixConfig(
                chart.getChartConfig(), chart.getChartType());
            chart.setChartConfig(fixedConfig);
        }
        
        // 6. 设置默认状态
        if (chart.getStatus() == null) {
            chart.setStatus(Constants.STATUS_ENABLED);
        }
        
        // 7. 保存
        chartDefinitionMapper.insert(chart);
        log.info("创建图表成功: id={}, code={}, name={}", chart.getId(), chart.getChartCode(), chart.getChartName());
        return chart;
    }
    
    /**
     * 更新图表定义（统一入口）
     */
    @Transactional
    public ChartDefinition updateChart(ChartDefinition chart) {
        if (chart.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图表ID不能为空");
        }
        
        ChartDefinition existing = chartDefinitionMapper.selectById(chart.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "图表不存在");
        }
        
        // 检查编码唯一性（如果修改了编码）
        if (StringUtils.hasText(chart.getChartCode()) && !chart.getChartCode().equals(existing.getChartCode())) {
            ChartDefinition codeExists = chartDefinitionMapper.selectByCode(chart.getChartCode());
            if (codeExists != null && !codeExists.getId().equals(chart.getId())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "图表编码已存在: " + chart.getChartCode());
            }
        }
        
        // SQL安全验证
        if (StringUtils.hasText(chart.getSqlContent())) {
            SqlSecurityUtil.validateSql(chart.getSqlContent());
        }
        
        // 图表配置验证和修复
        if (StringUtils.hasText(chart.getChartConfig())) {
            String chartType = StringUtils.hasText(chart.getChartType()) ? chart.getChartType() : existing.getChartType();
            String fixedConfig = chartConfigService.validateAndFixConfig(chart.getChartConfig(), chartType);
            chart.setChartConfig(fixedConfig);
        }
        
        chartDefinitionMapper.update(chart);
        log.info("更新图表成功: id={}", chart.getId());
        return chartDefinitionMapper.selectById(chart.getId());
    }
    
    /**
     * 删除图表
     */
    @Transactional
    public void deleteChart(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图表ID不能为空");
        }
        ChartDefinition chart = chartDefinitionMapper.selectById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "图表不存在");
        }
        chartDefinitionMapper.delete(id);
        log.info("删除图表成功: id={}, code={}", id, chart.getChartCode());
    }
    
    /**
     * 批量删除
     */
    @Transactional
    public void deleteCharts(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            deleteChart(id);
        }
    }
    
    /**
     * 批量更新状态
     */
    @Transactional
    public void updateChartsStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            ChartDefinition chart = new ChartDefinition();
            chart.setId(id);
            chart.setStatus(status);
            chartDefinitionMapper.update(chart);
        }
        log.info("批量更新图表状态: ids={}, status={}", ids, status);
    }
    
    // ==================== SQL执行 ====================
    
    /**
     * 执行图表SQL查询（核心方法）
     * 
     * @param chartId 图表ID
     * @param parameters 查询参数（JSON格式或Map）
     * @param limit 数据限制
     * @return 查询结果
     */
    public List<Map<String, Object>> executeChartQuery(Long chartId, Map<String, Object> parameters, Integer limit) {
        ChartDefinition chart = chartDefinitionMapper.selectById(chartId);
        if (chart == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "图表不存在");
        }
        if (chart.getStatus() != Constants.STATUS_ENABLED) {
            throw new BusinessException(ErrorCode.RESOURCE_DISABLED, "图表已禁用");
        }
        
        return executeSql(chart.getDataSourceId(), chart.getSqlContent(), parameters, limit);
    }
    
    /**
     * 直接执行SQL（用于预览和测试）
     * 
     * @param dataSourceId 数据源ID
     * @param sql SQL语句
     * @param parameters 参数
     * @param limit 限制
     * @return 查询结果
     */
    public List<Map<String, Object>> executeSql(Long dataSourceId, String sql, Map<String, Object> parameters, Integer limit) {
        DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        
        // 限制数据量
        if (limit == null || limit <= 0) limit = 10000;
        if (limit > 50000) limit = 50000;
        
        // 处理SQL参数
        String processedSql = processParameters(sql, parameters);
        
        // 确保有LIMIT
        processedSql = ensureSqlLimit(processedSql, limit);
        
        // SQL安全检查
        SqlSecurityUtil.validateSql(processedSql);
        
        // 执行查询
        String url = dataSourceService.buildJdbcUrl(dataSource);
        String driver = dbConnectionUtil.getDriverClassName(dataSource.getDbType());
        
        try {
            Class.forName(driver);
            List<Map<String, Object>> result = new ArrayList<>();
            
            try (Connection conn = connectionPoolManager.getConnection(dataSource);
                 Statement stmt = conn.createStatement()) {
                // 硬性限制最大返回行数，防止恶意SQL绕过LIMIT导致OOM
                stmt.setMaxRows(limit);
                stmt.setQueryTimeout(30);
                try (ResultSet rs = stmt.executeQuery(processedSql)) {
                
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnLabel = metaData.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        row.put(columnLabel, value);
                    }
                    result.add(row);
                }
                }
            }
            
            log.debug("SQL执行成功: rows={}", result.size());
            return result;
            
        } catch (ClassNotFoundException e) {
            throw new BusinessException(ErrorCode.ERROR, "数据库驱动加载失败: " + driver);
        } catch (SQLException e) {
            log.error("SQL执行失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, "SQL执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试SQL（不保存，仅执行）
     */
    public Map<String, Object> testSql(Long dataSourceId, String sql, Map<String, Object> parameters, Integer limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> data = executeSql(dataSourceId, sql, parameters, limit != null ? limit : 100);
            result.put("success", true);
            result.put("data", data);
            result.put("rowCount", data.size());
            
            // 返回列信息
            if (!data.isEmpty()) {
                result.put("columns", new ArrayList<>(data.get(0).keySet()));
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }
    
    // ==================== 参数处理 ====================
    
    /**
     * 处理SQL中的参数占位符
     * 支持 ${paramName} 和 :paramName 两种格式
     * 当参数值为空时，移除整个条件以避免SQL语法错误
     */
    public String processParameters(String sql, Map<String, Object> parameters) {
        if (sql == null || sql.isEmpty()) return sql;
        
        String result = sql;
        
        // 🔧 首先移除空参数对应的整个条件
        result = removeEmptyParamConditions(result, parameters);
        
        // 如果没有参数或参数为空，直接返回
        if (parameters == null || parameters.isEmpty()) return result;
        
        // 处理 ${paramName} 格式
        Matcher matcher = PARAM_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object value = parameters.get(paramName);
            if (value != null && !value.toString().isEmpty()) {
                String replacement = formatParamValue(value);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
        }
        matcher.appendTail(sb);
        result = sb.toString();
        
        // 处理 :paramName 格式
        matcher = NAMED_PARAM_PATTERN.matcher(result);
        sb = new StringBuffer();
        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object value = parameters.get(paramName);
            if (value != null) {
                String replacement = formatParamValue(value);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * 格式化参数值（处理不同类型）
     */
    private String formatParamValue(Object value) {
        if (value == null) return "NULL";
        
        if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return (Boolean) value ? "1" : "0";
        } else if (value instanceof java.util.Date) {
            return "'" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(value) + "'";
        } else if (value instanceof java.time.LocalDate) {
            return "'" + value.toString() + "'";
        } else if (value instanceof java.time.LocalDateTime) {
            return "'" + value.toString().replace("T", " ") + "'";
        } else {
            // 字符串类型，防SQL注入
            String strValue = value.toString().replace("'", "''");
            return "'" + strValue + "'";
        }
    }
    
    /**
     * 确保SQL有LIMIT限制
     */
    private String ensureSqlLimit(String sql, int limit) {
        if (sql == null) return sql;
        String upperSql = sql.toUpperCase().trim();
        
        // 已有LIMIT，不处理
        if (upperSql.contains("LIMIT")) return sql;
        
        // 移除末尾分号
        String trimmedSql = sql.trim();
        if (trimmedSql.endsWith(";")) {
            trimmedSql = trimmedSql.substring(0, trimmedSql.length() - 1);
        }
        
        return trimmedSql + " LIMIT " + limit;
    }
    
    /**
     * 移除SQL中空参数对应的条件
     * 当参数值为空或不存在时，移除包含该参数的整个条件
     */
    private String removeEmptyParamConditions(String sql, Map<String, Object> parameters) {
        if (sql == null || !sql.contains("${")) return sql;
        
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
    
    // ==================== 验证方法 ====================
    
    /**
     * 验证图表定义
     */
    private void validateChartDefinition(ChartDefinition chart, boolean isCreate) {
        if (!StringUtils.hasText(chart.getChartName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图表名称不能为空");
        }
        if (isCreate && !StringUtils.hasText(chart.getChartCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图表编码不能为空");
        }
        if (!StringUtils.hasText(chart.getChartType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图表类型不能为空");
        }
        if (chart.getDataSourceId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据源不能为空");
        }
        if (!StringUtils.hasText(chart.getSqlContent())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "SQL不能为空");
        }
        
        // 验证图表类型
        if (!chartConfigService.isValidChartType(chart.getChartType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的图表类型: " + chart.getChartType());
        }
    }
    
    /**
     * 解析JSON参数字符串
     */
    public Map<String, Object> parseParameters(String parametersJson) {
        if (!StringUtils.hasText(parametersJson)) return new HashMap<>();
        try {
            return objectMapper.readValue(parametersJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("参数解析失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
