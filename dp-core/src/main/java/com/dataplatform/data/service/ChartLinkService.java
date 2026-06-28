package com.dataplatform.data.service;

import com.dataplatform.data.dto.ChartLinkConfig;
import com.dataplatform.data.dto.DrillDownConfig;
import com.dataplatform.data.entity.ChartDefinition;
import com.dataplatform.data.entity.DashboardConfig;
import com.dataplatform.data.mapper.DashboardConfigMapper;
import com.dataplatform.data.service.chart.ChartCoreService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 图表联动与交叉筛选服务
 * 
 * 功能：
 * 1. 图表联动配置管理（保存、获取、删除）
 * 2. 交叉筛选支持（根据源图表选择获取联动图表数据）
 * 3. 下钻支持（按时间层级下钻）
 * 
 * @author dataplatform
 */
@Slf4j
@Service
public class ChartLinkService {

    @Autowired
    private DashboardConfigMapper dashboardConfigMapper;

    @Autowired
    private ChartCoreService chartCoreService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== 联动配置管理 ====================

    /**
     * 保存图表联动配置
     *
     * @param dashboardId 仪表盘ID
     * @param configs     联动配置列表
     */
    @Transactional
    public void saveLinkConfig(Long dashboardId, List<ChartLinkConfig> configs) {
        log.info("Saving link config for dashboard: {}, configs count: {}", dashboardId, configs != null ? configs.size() : 0);
        
        DashboardConfig dashboard = dashboardConfigMapper.selectById(dashboardId);
        if (dashboard == null) {
            throw new RuntimeException("Dashboard not found: " + dashboardId);
        }

        // 为没有ID的配置生成ID
        if (configs != null) {
            for (ChartLinkConfig config : configs) {
                if (config.getId() == null || config.getId().isEmpty()) {
                    config.setId(UUID.randomUUID().toString().substring(0, 8));
                }
            }
        }

        try {
            String linkConfigJson = objectMapper.writeValueAsString(configs != null ? configs : new ArrayList<>());
            dashboard.setLinkConfigJson(linkConfigJson);
            dashboard.setUpdateTime(LocalDateTime.now());
            dashboardConfigMapper.updateById(dashboard);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize link config", e);
            throw new RuntimeException("Failed to serialize link config", e);
        }
    }

    /**
     * 获取图表联动配置
     *
     * @param dashboardId 仪表盘ID
     * @return 联动配置列表
     */
    public List<ChartLinkConfig> getLinkConfig(Long dashboardId) {
        log.debug("Getting link config for dashboard: {}", dashboardId);
        
        DashboardConfig dashboard = dashboardConfigMapper.selectById(dashboardId);
        if (dashboard == null) {
            throw new RuntimeException("Dashboard not found: " + dashboardId);
        }

        String linkConfigJson = dashboard.getLinkConfigJson();
        if (linkConfigJson == null || linkConfigJson.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(linkConfigJson, new TypeReference<List<ChartLinkConfig>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse link config JSON", e);
            return new ArrayList<>();
        }
    }

    /**
     * 删除指定的联动配置
     *
     * @param dashboardId 仪表盘ID
     * @param linkId      联动配置ID
     */
    @Transactional
    public void deleteLinkConfig(Long dashboardId, String linkId) {
        log.info("Deleting link config: {} from dashboard: {}", linkId, dashboardId);
        
        List<ChartLinkConfig> configs = getLinkConfig(dashboardId);
        boolean removed = configs.removeIf(config -> linkId.equals(config.getId()));
        
        if (!removed) {
            log.warn("Link config not found: {}", linkId);
            return;
        }
        
        saveLinkConfig(dashboardId, configs);
    }

    // ==================== 交叉筛选支持 ====================

    /**
     * 获取联动图表数据
     * 根据源图表的选中维度值，查询目标图表的过滤数据
     *
     * @param sourceChartId  源图表ID
     * @param dimensionValue 选中的维度值
     * @param targetChartIds 目标图表ID列表
     * @return 目标图表ID到数据的映射
     */
    public Map<Long, List<Map<String, Object>>> getLinkedChartData(
            Long sourceChartId, 
            String dimensionValue, 
            List<Long> targetChartIds) {
        
        log.info("Getting linked chart data: sourceChartId={}, dimensionValue={}, targetChartIds={}", 
                sourceChartId, dimensionValue, targetChartIds);
        
        Map<Long, List<Map<String, Object>>> result = new HashMap<>();
        
        if (targetChartIds == null || targetChartIds.isEmpty()) {
            return result;
        }

        // 获取源图表信息以确定筛选字段
        ChartDefinition sourceChart = chartCoreService.getChartById(sourceChartId);
        if (sourceChart == null) {
            log.warn("Source chart not found: {}", sourceChartId);
            return result;
        }

        for (Long targetChartId : targetChartIds) {
            try {
                ChartDefinition targetChart = chartCoreService.getChartById(targetChartId);
                if (targetChart == null) {
                    log.warn("Target chart not found: {}", targetChartId);
                    continue;
                }

                // 构建带筛选条件的SQL
                String originalSql = targetChart.getSqlContent();
                if (originalSql == null || originalSql.isEmpty()) {
                    log.warn("Target chart has no SQL: {}", targetChartId);
                    continue;
                }

                // 尝试从图表配置中获取筛选字段
                String filterField = extractFilterField(targetChart);
                if (filterField == null) {
                    // 如果没有配置筛选字段，跳过该图表
                    log.debug("No filter field configured for chart: {}", targetChartId);
                    result.put(targetChartId, chartCoreService.executeChartQuery(targetChartId, null, null));
                    continue;
                }

                String filteredSql = buildFilteredSql(originalSql, filterField, dimensionValue);
                List<Map<String, Object>> data = chartCoreService.executeSql(
                        targetChart.getDataSourceId(), 
                        filteredSql, 
                        null, 
                        null);
                
                result.put(targetChartId, data);
            } catch (Exception e) {
                log.error("Failed to get linked data for chart: {}", targetChartId, e);
                // 保持原始数据不变
                try {
                    result.put(targetChartId, chartCoreService.executeChartQuery(targetChartId, null, null));
                } catch (Exception ex) {
                    log.error("Failed to get original data for chart: {}", targetChartId, ex);
                }
            }
        }

        return result;
    }

    /**
     * 构建带筛选条件的SQL
     *
     * @param originalSql 原始SQL
     * @param filterField 筛选字段
     * @param filterValue 筛选值
     * @return 带筛选条件的SQL
     */
    public String buildFilteredSql(String originalSql, String filterField, Object filterValue) {
        if (originalSql == null || originalSql.isEmpty()) {
            return originalSql;
        }
        
        if (filterField == null || filterField.isEmpty() || filterValue == null) {
            return originalSql;
        }

        String sql = originalSql.trim();
        
        // 格式化筛选值
        String formattedValue = formatFilterValue(filterValue);
        String filterCondition = filterField + " = " + formattedValue;

        // 检查SQL是否已有WHERE子句
        Pattern wherePattern = Pattern.compile("\\bWHERE\\b", Pattern.CASE_INSENSITIVE);
        Matcher whereMatcher = wherePattern.matcher(sql);
        
        if (whereMatcher.find()) {
            // 已有WHERE子句，添加AND条件
            // 找到WHERE后的位置，在第一个条件前添加新条件
            int whereEnd = whereMatcher.end();
            sql = sql.substring(0, whereEnd) + " " + filterCondition + " AND" + sql.substring(whereEnd);
        } else {
            // 没有WHERE子句，需要添加
            // 查找GROUP BY, ORDER BY, LIMIT等子句的位置
            Pattern clausePattern = Pattern.compile("\\b(GROUP\\s+BY|ORDER\\s+BY|LIMIT|HAVING)\\b", Pattern.CASE_INSENSITIVE);
            Matcher clauseMatcher = clausePattern.matcher(sql);
            
            if (clauseMatcher.find()) {
                // 在这些子句之前插入WHERE
                int insertPos = clauseMatcher.start();
                sql = sql.substring(0, insertPos) + " WHERE " + filterCondition + " " + sql.substring(insertPos);
            } else {
                // 没有这些子句，直接在末尾添加WHERE
                sql = sql + " WHERE " + filterCondition;
            }
        }

        return sql;
    }

    // ==================== 下钻支持 ====================

    /**
     * 获取图表的下钻配置
     *
     * @param chartId 图表ID
     * @return 下钻配置
     */
    public DrillDownConfig getDrillDownConfig(Long chartId) {
        log.debug("Getting drill down config for chart: {}", chartId);
        
        ChartDefinition chart = chartCoreService.getChartById(chartId);
        if (chart == null) {
            throw new RuntimeException("Chart not found: " + chartId);
        }

        // 从图表配置中提取下钻配置
        String chartConfigJson = chart.getChartConfig();
        if (chartConfigJson == null || chartConfigJson.isEmpty()) {
            return createDefaultDrillDownConfig(chartId);
        }

        try {
            Map<String, Object> chartConfig = objectMapper.readValue(chartConfigJson, new TypeReference<Map<String, Object>>() {});
            Object drillDownObj = chartConfig.get("drillDown");
            
            if (drillDownObj != null) {
                String drillDownJson = objectMapper.writeValueAsString(drillDownObj);
                DrillDownConfig config = objectMapper.readValue(drillDownJson, DrillDownConfig.class);
                config.setChartId(chartId);
                return config;
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse drill down config", e);
        }

        return createDefaultDrillDownConfig(chartId);
    }

    /**
     * 获取下钻数据
     *
     * @param chartId        图表ID
     * @param dimensionValue 当前维度值（用于筛选）
     * @param level          目标下钻层级
     * @return 下钻后的数据
     */
    public List<Map<String, Object>> getDrillDownData(Long chartId, String dimensionValue, int level) {
        log.info("Getting drill down data: chartId={}, dimensionValue={}, level={}", chartId, dimensionValue, level);
        
        ChartDefinition chart = chartCoreService.getChartById(chartId);
        if (chart == null) {
            throw new RuntimeException("Chart not found: " + chartId);
        }

        DrillDownConfig drillConfig = getDrillDownConfig(chartId);
        if (drillConfig.getLevels() == null || drillConfig.getLevels().isEmpty()) {
            log.warn("No drill down levels configured for chart: {}", chartId);
            return chartCoreService.executeChartQuery(chartId, null, null);
        }

        // 验证层级有效性
        if (level < 0 || level >= drillConfig.getLevels().size()) {
            log.warn("Invalid drill down level: {} for chart: {}", level, chartId);
            return chartCoreService.executeChartQuery(chartId, null, null);
        }

        DrillDownConfig.DrillLevel targetLevel = drillConfig.getLevels().get(level);
        String originalSql = chart.getSqlContent();
        
        // 构建下钻SQL
        String drillDownSql = buildDrillDownSql(originalSql, targetLevel, dimensionValue, level > 0 ? drillConfig.getLevels().get(level - 1) : null);
        
        return chartCoreService.executeSql(chart.getDataSourceId(), drillDownSql, null, null);
    }

    // ==================== 辅助方法 ====================

    /**
     * 从图表配置中提取筛选字段
     */
    private String extractFilterField(ChartDefinition chart) {
        String chartConfigJson = chart.getChartConfig();
        if (chartConfigJson == null || chartConfigJson.isEmpty()) {
            return null;
        }

        try {
            Map<String, Object> chartConfig = objectMapper.readValue(chartConfigJson, new TypeReference<Map<String, Object>>() {});
            
            // 尝试从fieldMapping中获取维度字段
            Object fieldMapping = chartConfig.get("fieldMapping");
            if (fieldMapping instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mapping = (Map<String, Object>) fieldMapping;
                Object xField = mapping.get("xField");
                if (xField != null) {
                    return xField.toString();
                }
                Object categoryField = mapping.get("categoryField");
                if (categoryField != null) {
                    return categoryField.toString();
                }
            }
            
            // 尝试从linkConfig中获取
            Object linkConfig = chartConfig.get("linkConfig");
            if (linkConfig instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> link = (Map<String, Object>) linkConfig;
                Object filterField = link.get("filterField");
                if (filterField != null) {
                    return filterField.toString();
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse chart config", e);
        }

        return null;
    }

    /**
     * 格式化筛选值
     */
    private String formatFilterValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        
        if (value instanceof Number) {
            return value.toString();
        }
        
        // 字符串值需要加引号并转义
        String strValue = value.toString();
        strValue = strValue.replace("'", "''"); // 转义单引号
        return "'" + strValue + "'";
    }

    /**
     * 创建默认的下钻配置（年→季→月→日）
     */
    private DrillDownConfig createDefaultDrillDownConfig(Long chartId) {
        DrillDownConfig config = new DrillDownConfig();
        config.setChartId(chartId);
        config.setCurrentLevel(0);
        
        List<DrillDownConfig.DrillLevel> levels = new ArrayList<>();
        
        DrillDownConfig.DrillLevel yearLevel = new DrillDownConfig.DrillLevel();
        yearLevel.setField("year");
        yearLevel.setGranularity("year");
        levels.add(yearLevel);
        
        DrillDownConfig.DrillLevel quarterLevel = new DrillDownConfig.DrillLevel();
        quarterLevel.setField("quarter");
        quarterLevel.setGranularity("quarter");
        levels.add(quarterLevel);
        
        DrillDownConfig.DrillLevel monthLevel = new DrillDownConfig.DrillLevel();
        monthLevel.setField("month");
        monthLevel.setGranularity("month");
        levels.add(monthLevel);
        
        DrillDownConfig.DrillLevel dayLevel = new DrillDownConfig.DrillLevel();
        dayLevel.setField("day");
        dayLevel.setGranularity("day");
        levels.add(dayLevel);
        
        config.setLevels(levels);
        return config;
    }

    /**
     * 构建下钻SQL
     */
    private String buildDrillDownSql(String originalSql, DrillDownConfig.DrillLevel targetLevel, 
                                      String dimensionValue, DrillDownConfig.DrillLevel parentLevel) {
        if (originalSql == null || originalSql.isEmpty()) {
            return originalSql;
        }

        String sql = originalSql.trim();
        
        // 如果有父级维度值，添加筛选条件
        if (dimensionValue != null && !dimensionValue.isEmpty() && parentLevel != null) {
            sql = buildFilteredSql(sql, parentLevel.getField(), dimensionValue);
        }

        // 根据目标粒度修改GROUP BY
        // 这里简化处理，实际可能需要更复杂的SQL改写逻辑
        String granularity = targetLevel.getGranularity();
        String timeField = targetLevel.getField();
        
        // 尝试替换时间粒度函数
        sql = adjustTimeGranularity(sql, timeField, granularity);

        return sql;
    }

    /**
     * 调整SQL中的时间粒度
     */
    private String adjustTimeGranularity(String sql, String timeField, String granularity) {
        // 根据不同的粒度生成对应的时间提取表达式
        String timeExpression;
        switch (granularity.toLowerCase()) {
            case "year":
                timeExpression = "YEAR(" + timeField + ")";
                break;
            case "quarter":
                timeExpression = "CONCAT(YEAR(" + timeField + "), '-Q', QUARTER(" + timeField + "))";
                break;
            case "month":
                timeExpression = "DATE_FORMAT(" + timeField + ", '%Y-%m')";
                break;
            case "day":
                timeExpression = "DATE(" + timeField + ")";
                break;
            default:
                return sql;
        }

        // 简单的替换逻辑，实际场景可能需要更复杂的SQL解析
        // 这里假设SQL中有类似 YEAR(date_field) 的表达式需要替换
        Pattern yearPattern = Pattern.compile("YEAR\\s*\\(\\s*" + Pattern.quote(timeField) + "\\s*\\)", Pattern.CASE_INSENSITIVE);
        sql = yearPattern.matcher(sql).replaceAll(Matcher.quoteReplacement(timeExpression));
        
        Pattern dateFormatPattern = Pattern.compile("DATE_FORMAT\\s*\\(\\s*" + Pattern.quote(timeField) + "\\s*,\\s*'[^']*'\\s*\\)", Pattern.CASE_INSENSITIVE);
        sql = dateFormatPattern.matcher(sql).replaceAll(Matcher.quoteReplacement(timeExpression));

        return sql;
    }
}
