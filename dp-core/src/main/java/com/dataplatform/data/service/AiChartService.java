package com.dataplatform.data.service;

import com.dataplatform.infra.ai.AiConfig;
import com.dataplatform.data.entity.ChartDefinition;
import com.dataplatform.data.service.chart.ChartCoreService;
import com.dataplatform.data.service.chart.ChartConfigService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AI图表服务 - 专门处理AI图表生成相关功能
 * 
 * 职责：
 * 1. AI生成图表配置（调用AI接口）
 * 2. 委托ChartCoreService进行图表CRUD
 * 3. 使用ChartConfigService处理配置
 * 
 * 与手动创建图表共用相同的核心逻辑
 */
@Slf4j
@Service
public class AiChartService {

    @Autowired
    private AiConfig aiConfig;
    
    @Autowired
    private AiService aiService;
    
    @Autowired
    private TableDataService tableDataService;
    
    @Autowired
    private ChartCoreService chartCoreService;
    
    @Autowired
    private ChartConfigService chartConfigService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 最大重试次数
    private static final int MAX_RETRY = 2;
    
    // 使用ChartConfigService的常量
    private Set<String> getValidChartTypes() {
        return ChartConfigService.VALID_CHART_TYPES;
    }
    
    private List<String> getDefaultColors() {
        return ChartConfigService.DEFAULT_COLORS;
    }
    
    /**
     * AI生成图表配置
     * 根据用户需求自动生成SQL、图表类型和ECharts配置
     * 支持自动重试和智能修复
     */
    public Map<String, Object> generateChart(String requirement, Long dataSourceId, Map<String, Object> context) {
        if (!aiConfig.isEnabled()) {
            return errorResult("AI功能未启用，请在系统设置中配置AI服务");
        }
        
        if (requirement == null || requirement.trim().length() < 5) {
            return errorResult("请输入更详细的图表需求描述（至少5个字符）");
        }
        
        if (dataSourceId == null) {
            return errorResult("请选择数据源");
        }
        
        String dbType = resolveChartDbType(dataSourceId, context);
        String systemPrompt = buildChartSystemPrompt(dbType);
        String userPrompt = buildChartUserPrompt(requirement, dataSourceId, context);
        
        Exception lastError = null;
        
        // 支持重试机制
        for (int attempt = 0; attempt <= MAX_RETRY; attempt++) {
            try {
                if (attempt > 0) {
                    log.info("AI生成图表第{}次重试", attempt);
                }
                
                String response = aiService.callAiProvider(systemPrompt, userPrompt);
                Map<String, Object> result = successResult(response);
                
                // 解析并验证JSON配置
                Map<String, Object> chartConfig = parseAndValidateChartConfig(response);
                if (chartConfig != null) {
                    // 智能修复配置
                    fixChartConfig(chartConfig);
                    result.put("chartConfig", chartConfig);
                    result.put("parseable", true);
                    result.put("retryCount", attempt);
                    return result;
                } else if (attempt < MAX_RETRY) {
                    log.warn("图表配置解析失败，准备重试");
                    continue;
                } else {
                    result.put("parseable", false);
                    result.put("warning", "AI返回的配置格式不完整，请手动调整或重新生成");
                    return result;
                }
            } catch (Exception e) {
                lastError = e;
                log.warn("AI生成图表失败(尝试{}): {}", attempt + 1, e.getMessage());
                if (attempt >= MAX_RETRY) {
                    break;
                }
                // 短暂延迟后重试
                try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        
        String errorMsg = lastError != null ? lastError.getMessage() : "未知错误";
        if (errorMsg.contains("timeout") || errorMsg.contains("timed out")) {
            return errorResult("AI服务响应超时，请稍后重试或简化需求描述");
        }
        return errorResult("AI生成图表失败: " + errorMsg);
    }
    
    /**
     * 智能修复图表配置
     */
    private void fixChartConfig(Map<String, Object> config) {
        // 确保必要字段存在
        if (config.get("chartName") == null || config.get("chartName").toString().isEmpty()) {
            config.put("chartName", "AI生成图表");
        }
        if (config.get("chartCode") == null || config.get("chartCode").toString().isEmpty()) {
            config.put("chartCode", "ai_chart_" + System.currentTimeMillis());
        }
        if (config.get("chartType") == null) {
            config.put("chartType", "bar");
        }
        
        // 修复SQL - 移除末尾分号
        String sql = (String) config.get("sql");
        if (sql != null && !sql.isEmpty()) {
            sql = sql.trim();
            if (sql.endsWith(";")) {
                sql = sql.substring(0, sql.length() - 1);
            }
            config.put("sql", sql);
        }
        
        // 确保dataMapping存在
        if (config.get("dataMapping") == null) {
            Map<String, String> defaultMapping = new HashMap<>();
            defaultMapping.put("xField", "name");
            defaultMapping.put("yField", "value");
            config.put("dataMapping", defaultMapping);
        }
        
        // 确保queryParams是数组
        if (config.get("queryParams") == null) {
            config.put("queryParams", new ArrayList<>());
        }
    }
    
    /**
     * 构建图表生成的系统提示词
     */
    private String buildChartSystemPrompt(String dbType) {
        String dialect = aiService.normalizeDbDialect(dbType);
        return """
            你是数据可视化专家。严格按要求生成图表配置JSON。
            
            # 重要：对话连续性
            - 如果用户请求是【修改请求】，必须基于"现有图表配置"进行修改，保留未提及的配置不变
            - 修改时只调整用户明确要求的部分，其他保持原样
            - 如果用户说"改成折线图"，只改chartType，SQL和其他配置保持不变
            - 如果用户说"添加筛选条件"，在原SQL基础上添加WHERE条件和queryParams

            # 图表类型及SQL模式（根据chartType选择正确的SQL模式）
            - 所有SQL必须使用__DIALECT__方言，不能混用其他数据库语法。
            - 时间格式化、日期截断、分页/限制、字符串函数必须使用__DIALECT__支持的写法。
            - 如果无法确定某个函数在__DIALECT__中是否支持，优先生成简单、通用且可执行的SQL。
            
            ## bar/line（柱状图/折线图）
            - SQL: SELECT 分类字段 AS name, SUM/COUNT/AVG(数值字段) AS value FROM 表 GROUP BY 分类字段
            - dataMapping: {"xField": "name", "yField": "value"}
            - 适合：按类别、时间、部门等维度统计数值
            
            ## pie（饼图）
            - SQL: SELECT 分类字段 AS name, SUM/COUNT(数值字段) AS value FROM 表 GROUP BY 分类字段
            - dataMapping: {"nameField": "name", "valueField": "value"}
            - 适合：占比分析、分布统计
            
            ## scatter（散点图）
            - SQL: SELECT 数值字段1 AS x, 数值字段2 AS y FROM 表
            - dataMapping: {"xField": "x", "yField": "y"}
            - 适合：两个数值变量的相关性分析
            
            ## gauge（仪表盘）
            - SQL: SELECT AVG/SUM(数值字段) AS value FROM 表（返回单个数值）
            - dataMapping: {"valueField": "value"}
            - 适合：KPI指标、完成率、百分比

            # 必须返回的JSON结构(只输出JSON，禁止解释文字):
            {
              "chartName": "中文图表名称",
              "chartCode": "chart_xxx",
              "chartType": "bar",
              "description": "描述",
              "sql": "SELECT category AS name, SUM(amount) AS value FROM sales GROUP BY category",
              "queryParams": [],
              "chartConfig": {
                "title": {"text": "标题", "left": "center"},
                "tooltip": {"trigger": "axis"},
                "grid": {"left": "3%", "right": "4%", "bottom": "10%", "containLabel": true},
                "xAxis": {"type": "category"},
                "yAxis": {"type": "value"},
                "series": [{"type": "bar", "name": "数据"}],
                "color": ["#5470c6", "#91cc75", "#fac858", "#ee6666", "#73c0de"]
              },
              "dataMapping": {"xField": "name", "yField": "value"}
            }

            # 关键规则
            1. 根据表字段类型智能选择：字符串/日期→分类维度，数值→聚合计算
            2. SQL用AS给字段起别名，dataMapping的字段必须与别名一致
            3. 聚合查询必须有GROUP BY，不要添加LIMIT
            4. 优先选择有业务含义的字段组合（如：部门+销售额、日期+订单数）
            5. 只输出JSON，禁止输出任何解释文字
            """.replace("__DIALECT__", dialect);
    }
    
    /**
     * 构建用户提示词
     */
    @SuppressWarnings("unchecked")
    private String buildChartUserPrompt(String requirement, Long dataSourceId, Map<String, Object> context) {
        StringBuilder userPrompt = new StringBuilder();
        
        // 🆕 判断是否为修改模式
        boolean isModifyMode = context != null && Boolean.TRUE.equals(context.get("isModify"));
        
        if (isModifyMode) {
            userPrompt.append("【修改请求】请基于现有图表配置进行修改，而不是重新生成。\n\n");
            
            // 添加现有配置
            Map<String, Object> previousConfig = (Map<String, Object>) context.get("previousConfig");
            if (previousConfig != null) {
                userPrompt.append("现有图表配置：\n");
                userPrompt.append("- 图表名称：").append(previousConfig.get("chartName")).append("\n");
                userPrompt.append("- 图表类型：").append(previousConfig.get("chartType")).append("\n");
                userPrompt.append("- 当前SQL：").append(previousConfig.get("sql")).append("\n");
                userPrompt.append("- 描述：").append(previousConfig.get("description")).append("\n\n");
            }
            
            // 添加对话历史
            List<Map<String, Object>> history = (List<Map<String, Object>>) context.get("conversationHistory");
            if (history != null && !history.isEmpty()) {
                userPrompt.append("之前的对话：\n");
                for (Map<String, Object> msg : history) {
                    String role = (String) msg.get("role");
                    String content = (String) msg.get("content");
                    if ("user".equals(role)) {
                        userPrompt.append("用户：").append(content).append("\n");
                    } else {
                        userPrompt.append("AI：已按要求生成/修改图表\n");
                    }
                }
                userPrompt.append("\n");
            }
            
            userPrompt.append("用户的修改要求：").append(requirement).append("\n\n");
            userPrompt.append("请根据修改要求，在现有配置基础上进行调整，输出完整的新配置JSON。\n\n");
        } else {
            userPrompt.append("需求：").append(requirement).append("\n\n");
        }
        
        String dbType = context != null ? Objects.toString(context.get("dbType"), null) : null;
        dbType = resolveChartDbType(dataSourceId, context);
        userPrompt.append("目标数据库类型：").append(aiService.normalizeDbDialect(dbType)).append("\n");
        userPrompt.append("请严格按上述数据库类型生成SQL。\n\n");
        
        // 获取用户选择的表
        List<String> selectedTables = getSelectedTables(context);
        
        // 添加表结构信息
        if (dataSourceId != null) {
            appendTableStructure(userPrompt, dataSourceId, selectedTables);
        }
        
        // 添加图表类型偏好
        if (context != null && context.containsKey("preferredChartType")) {
            userPrompt.append("图表类型：").append(context.get("preferredChartType")).append("\n");
        }
        
        return userPrompt.toString();
    }

    private String resolveChartDbType(Long dataSourceId, Map<String, Object> context) {
        String contextDbType = null;
        if (context != null && context.get("dbType") != null) {
            contextDbType = context.get("dbType").toString();
        }
        return aiService.resolveDbType(dataSourceId, contextDbType);
    }
    
    /**
     * 获取用户选择的表
     */
    @SuppressWarnings("unchecked")
    private List<String> getSelectedTables(Map<String, Object> context) {
        List<String> selectedTables = new ArrayList<>();
        if (context != null && context.containsKey("tables")) {
            List<String> tables = (List<String>) context.get("tables");
            if (tables != null && !tables.isEmpty()) {
                selectedTables.addAll(tables);
            }
        }
        return selectedTables;
    }
    
    /**
     * 添加表结构信息到提示词
     */
    private void appendTableStructure(StringBuilder userPrompt, Long dataSourceId, List<String> selectedTables) {
        try {
            List<Map<String, Object>> allTables = tableDataService.getTables(dataSourceId);
            
            // 如果没有选中表，取前3个
            if (selectedTables.isEmpty()) {
                for (int i = 0; i < Math.min(allTables.size(), 3); i++) {
                    selectedTables.add((String) allTables.get(i).get("tableName"));
                }
            }
            
            userPrompt.append("数据表结构：\n");
            for (String tableName : selectedTables) {
                userPrompt.append("表 ").append(tableName).append(":\n");
                try {
                    List<Map<String, Object>> columns = tableDataService.getTableStructure(dataSourceId, tableName);
                    for (Map<String, Object> col : columns) {
                        userPrompt.append("  - ").append(col.get("columnName"))
                            .append(" (").append(col.get("dataType")).append(")\n");
                    }
                } catch (Exception e) {
                    log.warn("获取表结构失败: {}", e.getMessage());
                }
            }
            userPrompt.append("\n");
        } catch (Exception e) {
            log.warn("获取数据表列表失败: {}", e.getMessage());
        }
    }
    
    /**
     * 解析并验证图表配置
     */
    private Map<String, Object> parseAndValidateChartConfig(String response) {
        try {
            int jsonStart = response.indexOf("{");
            int jsonEnd = response.lastIndexOf("}");
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonStr = response.substring(jsonStart, jsonEnd + 1);
                Map<String, Object> chartConfig = objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
                
                // 验证并补全必要字段
                validateAndEnrichConfig(chartConfig);
                
                return chartConfig;
            }
        } catch (Exception e) {
            log.warn("图表配置解析失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 验证并补全配置
     */
    private void validateAndEnrichConfig(Map<String, Object> config) {
        // 确保chartCode存在且唯一
        if (config.get("chartCode") == null || config.get("chartCode").toString().isEmpty()) {
            config.put("chartCode", "ai_chart_" + System.currentTimeMillis());
        }
        
        // 验证chartType
        String chartType = (String) config.get("chartType");
        if (chartType == null || !chartConfigService.isValidChartType(chartType)) {
            config.put("chartType", "bar");
        }
        
        // 确保有默认配色
        @SuppressWarnings("unchecked")
        Map<String, Object> chartConfig = (Map<String, Object>) config.get("chartConfig");
        if (chartConfig != null && !chartConfig.containsKey("color")) {
            chartConfig.put("color", getDefaultColors());
        }
    }

    /**
     * AI创建图表（带完整配置）
     * 支持样式配置、导出配置和PDF水印
     */
    public Map<String, Object> createChartWithConfig(String chartName, String chartCode, String chartType,
            String description, Long dataSourceId, String sql, 
            Map<String, Object> fieldMapping, Map<String, Object> styleConfig,
            List<Map<String, Object>> queryParams,
            Boolean allowExportExcel, Boolean allowExportPdf, String watermarkType, String pdfWatermark) {
        
        // 构建完整的图表配置JSON
        String chartConfigJson = null;
        try {
            Map<String, Object> fullConfig = new HashMap<>();
            
            // 添加字段映射
            if (fieldMapping != null) {
                fullConfig.put("fieldMapping", fieldMapping);
            }
            
            // 添加样式配置
            if (styleConfig != null) {
                fullConfig.put("styleConfig", styleConfig);
            }
            
            // 添加导出配置
            Map<String, Object> exportConfig = new HashMap<>();
            exportConfig.put("allowExportExcel", allowExportExcel != null ? allowExportExcel : true);
            exportConfig.put("allowExportPdf", allowExportPdf != null ? allowExportPdf : true);
            exportConfig.put("watermarkType", watermarkType);
            exportConfig.put("pdfWatermark", pdfWatermark);
            fullConfig.put("exportConfig", exportConfig);
            
            // 添加查询参数
            if (queryParams != null) {
                fullConfig.put("queryParams", queryParams);
            }
            
            chartConfigJson = objectMapper.writeValueAsString(fullConfig);
        } catch (Exception e) {
            log.warn("构建图表配置JSON失败: {}", e.getMessage());
        }
        
        return createChart(chartName, chartCode, chartType, description, dataSourceId, sql, chartConfigJson, queryParams, allowExportExcel, allowExportPdf, watermarkType, pdfWatermark);
    }
    
    /**
     * AI创建图表
     * 根据AI生成的配置直接创建图表定义
     */
    public Map<String, Object> createChart(String chartName, String chartCode, String chartType, 
            String description, Long dataSourceId, String sql, String chartConfigJson, 
            List<Map<String, Object>> queryParams,
            Boolean allowExportExcel, Boolean allowExportPdf, String watermarkType, String pdfWatermark) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证必填参数
            if (chartName == null || chartName.trim().isEmpty()) {
                return errorResult("图表名称不能为空");
            }
            if (dataSourceId == null) {
                return errorResult("数据源ID不能为空");
            }
            if (sql == null || sql.trim().isEmpty()) {
                return errorResult("SQL语句不能为空");
            }
            
            // 生成唯一编码
            String finalChartCode = (chartCode == null || chartCode.trim().isEmpty()) 
                ? "ai_chart_" + System.currentTimeMillis() 
                : chartCode;
            
            // 默认图表类型
            String finalChartType = (chartType == null || chartType.trim().isEmpty()) 
                ? "bar" 
                : chartType;
            
            // 🔧 将ECharts配置包装为ChartDesigner兼容的格式（包含查询参数）
            String wrappedChartConfig = wrapChartConfigForDesigner(chartConfigJson, sql, finalChartType, queryParams);
            
            // 创建图表实体
            ChartDefinition chart = new ChartDefinition();
            chart.setChartName(chartName.trim());
            chart.setChartCode(finalChartCode);
            chart.setChartType(finalChartType);
            chart.setDescription(description != null ? description : "由AI助手自动创建");
            chart.setDataSourceId(dataSourceId);
            chart.setSqlContent(sql.trim());
            chart.setChartConfig(wrappedChartConfig);
            chart.setStatus(1); // 默认启用
            chart.setAllowExportExcel(allowExportExcel != null && allowExportExcel ? 1 : 0);
            chart.setAllowExportPdf(allowExportPdf != null && allowExportPdf ? 1 : 0);
            chart.setWatermarkType(watermarkType != null ? watermarkType : "none");
            chart.setPdfWatermark(pdfWatermark);
            
            // 使用核心服务保存图表（统一入口）
            ChartDefinition saved = chartCoreService.createChart(chart);
            
            result.put("success", true);
            result.put("chartId", saved.getId());
            result.put("chartCode", saved.getChartCode());
            result.put("chartName", saved.getChartName());
            result.put("message", "图表创建成功：" + chartName);
        } catch (Exception e) {
            log.error("AI创建图表失败: {}", e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * AI一键生成并创建图表
     */
    public Map<String, Object> generateAndCreateChart(String requirement, Long dataSourceId, Map<String, Object> options) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 生成图表配置
            Map<String, Object> generateResult = generateChart(requirement, dataSourceId, options);
            if (!Boolean.TRUE.equals(generateResult.get("success"))) {
                return generateResult;
            }
            
            // 2. 解析配置
            @SuppressWarnings("unchecked")
            Map<String, Object> chartConfig = (Map<String, Object>) generateResult.get("chartConfig");
            if (chartConfig == null) {
                return errorResult("AI生成的图表配置无法解析，请重试");
            }
            
            // 3. 提取配置信息并创建
            String chartName = (String) chartConfig.get("chartName");
            String chartCode = (String) chartConfig.get("chartCode");
            String chartType = (String) chartConfig.get("chartType");
            String description = (String) chartConfig.get("description");
            String sql = (String) chartConfig.get("sql");
            
            // 4. 提取ECharts配置
            String echartsConfigJson = null;
            Object echartsConfig = chartConfig.get("chartConfig");
            if (echartsConfig != null) {
                echartsConfigJson = objectMapper.writeValueAsString(echartsConfig);
            }
            
            // 🆕 4.1 提取查询参数
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> queryParams = (List<Map<String, Object>>) chartConfig.get("queryParams");
            
            // 5. 创建图表（包含查询参数）
            Map<String, Object> createResult = createChart(chartName, chartCode, chartType, 
                    description, dataSourceId, sql, echartsConfigJson, queryParams, true, true, "none", null);
            
            if (Boolean.TRUE.equals(createResult.get("success"))) {
                result.put("success", true);
                result.put("chartId", createResult.get("chartId"));
                result.put("chartCode", createResult.get("chartCode"));
                result.put("chartName", createResult.get("chartName"));
                result.put("chartType", chartType);
                result.put("sql", sql);
                result.put("aiResponse", generateResult.get("content"));
                result.put("message", "图表生成并创建成功！");
            } else {
                return createResult;
            }
        } catch (Exception e) {
            log.error("AI一键生成图表失败: {}", e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * AI优化图表样式
     */
    public Map<String, Object> optimizeChartStyle(Long chartId, String stylePreference) {
        if (!aiConfig.isEnabled()) {
            return errorResult("AI功能未启用");
        }
        
        try {
            ChartDefinition chart = chartCoreService.getChartById(chartId);
            if (chart == null) {
                return errorResult("图表不存在");
            }
            
            String systemPrompt = buildStyleOptimizationPrompt();
            String userPrompt = buildStyleUserPrompt(chart, stylePreference);
            
            String response = aiService.callAiProvider(systemPrompt, userPrompt);
            Map<String, Object> result = successResult(response);
            
            // 解析优化后的配置
            try {
                int jsonStart = response.indexOf("{");
                int jsonEnd = response.lastIndexOf("}");
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    String jsonStr = response.substring(jsonStart, jsonEnd + 1);
                    Map<String, Object> optimizedConfig = objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
                    result.put("optimizedConfig", optimizedConfig);
                    result.put("parseable", true);
                }
            } catch (Exception e) {
                result.put("parseable", false);
            }
            
            return result;
        } catch (Exception e) {
            log.error("AI优化图表样式失败: {}", e.getMessage(), e);
            return errorResult("AI优化图表样式失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建样式优化系统提示词
     */
    private String buildStyleOptimizationPrompt() {
        return """
            你是专业的数据可视化设计师，擅长ECharts图表美化。
            请根据现有图表配置，生成更美观、更专业的样式配置。
            
            # 优化方向
            1. 配色方案：使用现代、专业的配色
            2. 动画效果：添加适当的动画提升体验
            3. 交互增强：优化tooltip、legend等交互元素
            4. 布局优化：调整间距、字体等使图表更美观
            
            # 配色主题
            - professional: 商务蓝色系
            - vibrant: 活力多彩
            - dark: 深色主题
            - pastel: 柔和色调
            
            只返回完整的ECharts option JSON配置，不要解释。
            """;
    }
    
    /**
     * 构建样式优化用户提示词
     */
    private String buildStyleUserPrompt(ChartDefinition chart, String stylePreference) {
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("请优化以下图表的样式：\n\n");
        userPrompt.append("图表名称：").append(chart.getChartName()).append("\n");
        userPrompt.append("图表类型：").append(chart.getChartType()).append("\n");
        
        if (chart.getChartConfig() != null) {
            userPrompt.append("当前配置：\n").append(chart.getChartConfig()).append("\n");
        }
        
        if (stylePreference != null && !stylePreference.isEmpty()) {
            userPrompt.append("样式偏好：").append(stylePreference).append("\n");
        }
        
        return userPrompt.toString();
    }
    
    /**
     * 构建成功结果
     */
    private Map<String, Object> successResult(String content) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("content", content);
        return result;
    }
    
    /**
     * 将ECharts配置包装为ChartDesigner兼容的格式
     * 添加metadata信息，使编辑时能正确加载
     */
    private String wrapChartConfigForDesigner(String echartsConfigJson, String sql, String chartType, List<Map<String, Object>> queryParams) {
        try {
            Map<String, Object> wrappedConfig = new HashMap<>();
            
            // 解析原始配置（可能包含fieldMapping, styleConfig等）
            Map<String, Object> originalConfig = null;
            if (echartsConfigJson != null && !echartsConfigJson.trim().isEmpty()) {
                originalConfig = objectMapper.readValue(echartsConfigJson, new TypeReference<Map<String, Object>>() {});
            }
            
            // 从SQL中提取表名
            String selectedTable = extractTableFromSql(sql);
            
            // 🔧 优先使用原始配置中的fieldMapping，否则从SQL提取
            @SuppressWarnings("unchecked")
            Map<String, Object> fieldMapping = originalConfig != null && originalConfig.get("fieldMapping") != null 
                ? (Map<String, Object>) originalConfig.get("fieldMapping")
                : extractFieldMappingFromSql(sql, chartType);
            
            // 🆕 转换AI生成的queryParams为ChartDesigner兼容的chartParameters格式
            List<Map<String, Object>> chartParameters = convertQueryParamsToChartParameters(queryParams, sql);
            
            // 构建metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("sourceType", "database");
            metadata.put("selectedTable", selectedTable);
            metadata.put("fieldMapping", fieldMapping);
            metadata.put("dataLimit", 100);
            metadata.put("aiGenerated", true);  // 标记为AI生成
            metadata.put("chartParameters", chartParameters);  // 🆕 添加查询参数
            
            // 组装完整配置
            wrappedConfig.put("metadata", metadata);
            
            // 🔧 生成基础的ECharts配置（不要把原始config当作echarts配置）
            Map<String, Object> echartsOptions = generateDefaultEchartsConfig(chartType, fieldMapping);
            wrappedConfig.put("echarts", echartsOptions);
            
            // 🆕 保留原始配置中的styleConfig（如果存在）
            if (originalConfig != null && originalConfig.get("styleConfig") != null) {
                wrappedConfig.put("styleConfig", originalConfig.get("styleConfig"));
            }
            
            // 🆕 保留原始配置中的exportConfig（如果存在）
            if (originalConfig != null && originalConfig.get("exportConfig") != null) {
                wrappedConfig.put("exportConfig", originalConfig.get("exportConfig"));
            }
            
            // 🆕 同时在顶层添加queryParameters，保持兼容性
            wrappedConfig.put("queryParameters", chartParameters);
            
            return objectMapper.writeValueAsString(wrappedConfig);
        } catch (Exception e) {
            log.warn("包装图表配置失败，使用原始配置: {}", e.getMessage());
            return echartsConfigJson;
        }
    }
    
    /**
     * 将AI生成的queryParams转换为ChartDesigner兼容的chartParameters格式
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> convertQueryParamsToChartParameters(List<Map<String, Object>> queryParams, String sql) {
        List<Map<String, Object>> chartParameters = new ArrayList<>();
        
        if (queryParams == null || queryParams.isEmpty()) {
            return chartParameters;
        }
        
        for (Map<String, Object> param : queryParams) {
            Map<String, Object> chartParam = new HashMap<>();
            
            String name = (String) param.getOrDefault("name", "param");
            String label = (String) param.getOrDefault("label", name);
            String type = (String) param.getOrDefault("type", "text");
            Object defaultValue = param.get("defaultValue");
            
            // 从SQL中推断运算符
            String operator = inferOperatorFromSql(sql, name);
            
            // 转换类型映射
            String chartParamType = convertParamType(type);
            
            chartParam.put("field", name);  // AI参数没有field，使用name
            chartParam.put("name", name);
            chartParam.put("label", label);
            chartParam.put("type", chartParamType);
            chartParam.put("operator", operator);
            chartParam.put("required", false);
            
            // 处理默认值
            if (defaultValue != null && !defaultValue.toString().isEmpty()) {
                chartParam.put("defaultValue", defaultValue);
            } else if ("date".equals(chartParamType)) {
                // 日期类型默认设置为"上日"
                chartParam.put("datePreset", "lastDay");
            }
            
            chartParameters.add(chartParam);
        }
        
        return chartParameters;
    }
    
    /**
     * 从SQL中推断参数的运算符
     */
    private String inferOperatorFromSql(String sql, String paramName) {
        if (sql == null || paramName == null) return "=";
        
        String placeholder = "${" + paramName + "}";
        int idx = sql.indexOf(placeholder);
        if (idx == -1) return "=";
        
        // 查找占位符前面的运算符
        String beforePlaceholder = sql.substring(0, idx).toUpperCase();
        
        if (beforePlaceholder.endsWith("LIKE ") || beforePlaceholder.endsWith("LIKE '") || beforePlaceholder.endsWith("LIKE '%")) {
            return "LIKE";
        } else if (beforePlaceholder.endsWith(">= ") || beforePlaceholder.endsWith(">='")) {
            return ">=";
        } else if (beforePlaceholder.endsWith("<= ") || beforePlaceholder.endsWith("<='")) {
            return "<=";
        } else if (beforePlaceholder.endsWith("> ") || beforePlaceholder.endsWith(">'")) {
            return ">";
        } else if (beforePlaceholder.endsWith("< ") || beforePlaceholder.endsWith("<'")) {
            return "<";
        } else if (beforePlaceholder.endsWith("!= ") || beforePlaceholder.endsWith("!='")) {
            return "!=";
        } else if (beforePlaceholder.endsWith("IN (")) {
            return "IN";
        } else if (beforePlaceholder.endsWith("BETWEEN ")) {
            return "BETWEEN";
        }
        
        return "=";
    }
    
    /**
     * 转换参数类型
     */
    private String convertParamType(String aiType) {
        if (aiType == null) return "text";
        
        switch (aiType.toLowerCase()) {
            case "number":
            case "int":
            case "integer":
            case "decimal":
            case "float":
            case "double":
                return "number";
            case "date":
            case "datetime":
            case "time":
                return "date";
            case "daterange":
                return "dateRange";
            case "select":
                return "select";
            case "multiselect":
                return "multiSelect";
            default:
                return "text";
        }
    }
    
    /**
     * 从SQL中提取表名
     */
    private String extractTableFromSql(String sql) {
        if (sql == null || sql.isEmpty()) return null;
        
        // 匹配 FROM table_name 或 FROM `table_name`
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "FROM\\s+`?(\\w+)`?", 
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(sql);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * 从SQL中提取字段映射（基于SELECT别名）
     */
    private Map<String, Object> extractFieldMappingFromSql(String sql, String chartType) {
        Map<String, Object> fieldMapping = new HashMap<>();
        
        if (sql == null || sql.isEmpty()) {
            return fieldMapping;
        }
        
        try {
            // 提取SELECT和FROM之间的字段列表
            java.util.regex.Pattern selectPattern = java.util.regex.Pattern.compile(
                "SELECT\\s+(.+?)\\s+FROM",
                java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL
            );
            java.util.regex.Matcher selectMatcher = selectPattern.matcher(sql);
            
            if (selectMatcher.find()) {
                String fieldsStr = selectMatcher.group(1);
                String[] fields = fieldsStr.split(",");
                
                List<String> aliases = new ArrayList<>();
                for (String field : fields) {
                    field = field.trim();
                    // 提取AS后面的别名，或者字段本身（支持中文）
                    java.util.regex.Pattern aliasPattern = java.util.regex.Pattern.compile(
                        "(?:AS\\s+)?`?([\\w\\u4e00-\\u9fa5]+)`?\\s*$",
                        java.util.regex.Pattern.CASE_INSENSITIVE
                    );
                    java.util.regex.Matcher aliasMatcher = aliasPattern.matcher(field);
                    if (aliasMatcher.find()) {
                        aliases.add(aliasMatcher.group(1));
                    }
                }
                
                // 根据图表类型设置字段映射
                if (aliases.size() >= 2) {
                    if ("pie".equals(chartType)) {
                        fieldMapping.put("xAxis", aliases.get(0));  // name字段
                        fieldMapping.put("yAxis", Arrays.asList(aliases.get(1)));  // value字段
                    } else {
                        fieldMapping.put("xAxis", aliases.get(0));  // X轴
                        fieldMapping.put("yAxis", aliases.subList(1, aliases.size()));  // Y轴（可能多个）
                    }
                } else if (aliases.size() == 1) {
                    fieldMapping.put("xAxis", aliases.get(0));
                    fieldMapping.put("yAxis", new ArrayList<>());
                }
            }
        } catch (Exception e) {
            log.warn("解析SQL字段映射失败: {}", e.getMessage());
        }
        
        return fieldMapping;
    }
    
    /**
     * 生成默认ECharts配置
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> generateDefaultEchartsConfig(String chartType, Map<String, Object> fieldMapping) {
        Map<String, Object> options = new HashMap<>();
        
        // 标题
        Map<String, Object> title = new HashMap<>();
        title.put("text", "");
        title.put("left", "center");
        options.put("title", title);
        
        // 提示框
        Map<String, Object> tooltip = new HashMap<>();
        tooltip.put("trigger", "pie".equals(chartType) ? "item" : "axis");
        options.put("tooltip", tooltip);
        
        // 图例
        Map<String, Object> legend = new HashMap<>();
        legend.put("top", 35);
        legend.put("left", "center");
        options.put("legend", legend);
        
        // 网格
        Map<String, Object> grid = new HashMap<>();
        grid.put("top", 70);
        grid.put("bottom", 30);
        grid.put("left", 50);
        grid.put("right", 30);
        grid.put("containLabel", true);
        options.put("grid", grid);
        
        // 配色
        options.put("color", getDefaultColors());
        
        // 根据图表类型配置
        if ("pie".equals(chartType)) {
            // 饼图
            List<Map<String, Object>> series = new ArrayList<>();
            Map<String, Object> serie = new HashMap<>();
            serie.put("type", "pie");
            serie.put("radius", "60%");
            serie.put("data", new ArrayList<>());
            series.add(serie);
            options.put("series", series);
        } else if ("line".equals(chartType)) {
            // 折线图
            Map<String, Object> xAxis = new HashMap<>();
            xAxis.put("type", "category");
            xAxis.put("data", new ArrayList<>());
            options.put("xAxis", xAxis);
            
            Map<String, Object> yAxis = new HashMap<>();
            yAxis.put("type", "value");
            options.put("yAxis", yAxis);
            
            List<Map<String, Object>> series = new ArrayList<>();
            Map<String, Object> serie = new HashMap<>();
            serie.put("type", "line");
            serie.put("data", new ArrayList<>());
            serie.put("smooth", true);
            series.add(serie);
            options.put("series", series);
        } else if ("area".equals(chartType)) {
            // 面积图
            Map<String, Object> xAxis = new HashMap<>();
            xAxis.put("type", "category");
            xAxis.put("data", new ArrayList<>());
            options.put("xAxis", xAxis);
            
            Map<String, Object> yAxis = new HashMap<>();
            yAxis.put("type", "value");
            options.put("yAxis", yAxis);
            
            List<Map<String, Object>> series = new ArrayList<>();
            Map<String, Object> serie = new HashMap<>();
            serie.put("type", "line");
            serie.put("data", new ArrayList<>());
            serie.put("smooth", true);
            Map<String, Object> areaStyle = new HashMap<>();
            areaStyle.put("opacity", 0.3);
            serie.put("areaStyle", areaStyle);
            series.add(serie);
            options.put("series", series);
        } else {
            // 默认柱状图
            Map<String, Object> xAxis = new HashMap<>();
            xAxis.put("type", "category");
            xAxis.put("data", new ArrayList<>());
            options.put("xAxis", xAxis);
            
            Map<String, Object> yAxis = new HashMap<>();
            yAxis.put("type", "value");
            options.put("yAxis", yAxis);
            
            List<Map<String, Object>> series = new ArrayList<>();
            Map<String, Object> serie = new HashMap<>();
            serie.put("type", "bar");
            serie.put("data", new ArrayList<>());
            series.add(serie);
            options.put("series", series);
        }
        
        return options;
    }
    
    /**
     * 构建错误结果
     */
    private Map<String, Object> errorResult(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", message);
        return result;
    }
}
