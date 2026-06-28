package com.dataplatform.data.service;

import com.dataplatform.data.entity.DashboardConfig;
import com.dataplatform.data.dto.WidgetConfig;
import com.dataplatform.data.dto.FilterConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 自然语言生成仪表盘服务
 * 根据用户自然语言描述，自动生成仪表盘配置
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NLDashboardService {

    private final AiService aiService;
    private final DashboardDesignerService dashboardDesignerService;
    private final DataSourceService dataSourceService;
    private final ObjectMapper objectMapper;

    /**
     * 根据自然语言描述生成仪表盘
     */
    @Transactional
    public DashboardConfig generateDashboard(String description, Long dataSourceId, Long userId) {
        log.info("Generating dashboard from description: {}", description);
        
        try {
            // 1. 获取数据源表结构信息
            String schemaInfo = getSchemaInfo(dataSourceId);
            
            // 2. 调用 AI 生成仪表盘配置
            String prompt = buildDashboardPrompt(description, schemaInfo);
            Map<String, Object> aiResult = aiService.chatWithContext(prompt, schemaInfo);
            
            if (!(Boolean) aiResult.getOrDefault("success", false)) {
                throw new RuntimeException("AI 生成失败: " + aiResult.get("message"));
            }
            
            String aiResponse = (String) aiResult.get("content");
            
            // 3. 解析 AI 响应为仪表盘配置
            DashboardGenerationResult result = parseAiResponse(aiResponse);
            
            // 4. 创建仪表盘
            DashboardConfig dashboard = new DashboardConfig();
            dashboard.setName(result.name);
            dashboard.setDescription(result.description);
            dashboard.setCreateBy(userId);
            dashboard.setLayoutJson(objectMapper.writeValueAsString(result.widgets));
            dashboard.setGlobalFiltersJson(objectMapper.writeValueAsString(result.filters));
            dashboard.setCreateTime(LocalDateTime.now());
            dashboard.setUpdateTime(LocalDateTime.now());
            
            return dashboardDesignerService.createDashboard(dashboard);
            
        } catch (Exception e) {
            log.error("Failed to generate dashboard", e);
            throw new RuntimeException("生成仪表盘失败: " + e.getMessage());
        }
    }

    /**
     * 推荐图表配置
     */
    public List<ChartRecommendation> recommendCharts(Long dataSourceId, String description) {
        log.info("Recommending charts for dataSource: {}, description: {}", dataSourceId, description);
        
        try {
            String schemaInfo = getSchemaInfo(dataSourceId);
            String prompt = buildChartRecommendationPrompt(description, schemaInfo);
            
            Map<String, Object> aiResult = aiService.chatWithContext(prompt, schemaInfo);
            
            if (!(Boolean) aiResult.getOrDefault("success", false)) {
                return Collections.emptyList();
            }
            
            String aiResponse = (String) aiResult.get("content");
            return parseChartRecommendations(aiResponse);
            
        } catch (Exception e) {
            log.error("Failed to recommend charts", e);
            return Collections.emptyList();
        }
    }

    /**
     * 流式生成仪表盘（返回生成进度）
     */
    public Map<String, Object> generateDashboardWithProgress(String description, Long dataSourceId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        List<String> steps = new ArrayList<>();
        
        try {
            steps.add("正在分析需求...");
            result.put("steps", new ArrayList<>(steps));
            
            // 获取表结构
            steps.add("正在获取数据源信息...");
            String schemaInfo = getSchemaInfo(dataSourceId);
            
            // 推荐图表
            steps.add("正在推荐图表类型...");
            List<ChartRecommendation> recommendations = recommendCharts(dataSourceId, description);
            result.put("recommendations", recommendations);
            
            // 生成仪表盘
            steps.add("正在生成仪表盘配置...");
            DashboardConfig dashboard = generateDashboard(description, dataSourceId, userId);
            
            steps.add("仪表盘生成完成！");
            result.put("success", true);
            result.put("dashboard", dashboard);
            result.put("steps", steps);
            
        } catch (Exception e) {
            steps.add("生成失败: " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("steps", steps);
        }
        
        return result;
    }

    private String getSchemaInfo(Long dataSourceId) {
        try {
            Map<String, Object> context = aiService.getSystemContext(dataSourceId);
            return (String) context.getOrDefault("tableSchema", "");
        } catch (Exception e) {
            log.warn("Failed to get schema info for dataSource: {}", dataSourceId);
            return "";
        }
    }

    private String buildDashboardPrompt(String description, String schemaInfo) {
        return """
            你是一个数据可视化专家。请根据用户的需求描述，生成一个仪表盘配置。
            
            用户需求：%s
            
            数据表结构：
            %s
            
            请生成一个 JSON 格式的仪表盘配置，包含以下字段：
            {
              "name": "仪表盘名称",
              "description": "仪表盘描述",
              "widgets": [
                {
                  "i": "widget-1",
                  "x": 0, "y": 0, "w": 6, "h": 4,
                  "type": "chart",
                  "config": {
                    "chartType": "bar|line|pie|area|scatter|kpi|map|waterfall|wordcloud|combo",
                    "title": "图表标题",
                    "sql": "SELECT ... FROM ...",
                    "xField": "x轴字段",
                    "yField": "y轴字段"
                  }
                }
              ],
              "filters": [
                {
                  "id": "filter-1",
                  "type": "date|select|input",
                  "field": "字段名",
                  "label": "筛选器标签"
                }
              ]
            }
            
            注意：
            1. 网格布局为 12 列，合理安排 widget 位置
            2. 根据数据特点选择合适的图表类型
            3. 添加必要的全局筛选器
            4. SQL 必须基于提供的表结构
            
            只返回 JSON，不要其他内容。
            """.formatted(description, schemaInfo);
    }

    private String buildChartRecommendationPrompt(String description, String schemaInfo) {
        return """
            你是一个数据可视化专家。请根据用户需求和数据表结构，推荐合适的图表配置。
            
            用户需求：%s
            
            数据表结构：
            %s
            
            请返回 JSON 数组格式的图表推荐：
            [
              {
                "chartType": "bar|line|pie|area|scatter|kpi|map|waterfall|wordcloud|combo",
                "title": "推荐图表标题",
                "reason": "推荐理由",
                "sql": "SELECT ... FROM ...",
                "xField": "x轴字段",
                "yField": "y轴字段",
                "priority": 1
              }
            ]
            
            按优先级排序，最多推荐 5 个图表。只返回 JSON，不要其他内容。
            """.formatted(description, schemaInfo);
    }

    private DashboardGenerationResult parseAiResponse(String response) {
        DashboardGenerationResult result = new DashboardGenerationResult();
        
        try {
            // 提取 JSON 内容
            String json = extractJson(response);
            Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<>() {});
            
            result.name = (String) parsed.getOrDefault("name", "AI 生成仪表盘");
            result.description = (String) parsed.getOrDefault("description", "");
            
            // 解析 widgets
            List<Map<String, Object>> widgetMaps = (List<Map<String, Object>>) parsed.getOrDefault("widgets", Collections.emptyList());
            result.widgets = new ArrayList<>();
            
            int index = 0;
            for (Map<String, Object> wm : widgetMaps) {
                WidgetConfig widget = new WidgetConfig();
                widget.setI((String) wm.getOrDefault("i", "widget-" + index++));
                widget.setX(((Number) wm.getOrDefault("x", 0)).intValue());
                widget.setY(((Number) wm.getOrDefault("y", 0)).intValue());
                widget.setW(((Number) wm.getOrDefault("w", 6)).intValue());
                widget.setH(((Number) wm.getOrDefault("h", 4)).intValue());
                widget.setType((String) wm.getOrDefault("type", "chart"));
                widget.setConfig((Map<String, Object>) wm.getOrDefault("config", new HashMap<>()));
                result.widgets.add(widget);
            }
            
            // 解析 filters
            List<Map<String, Object>> filterMaps = (List<Map<String, Object>>) parsed.getOrDefault("filters", Collections.emptyList());
            result.filters = new ArrayList<>();
            
            for (Map<String, Object> fm : filterMaps) {
                FilterConfig filter = new FilterConfig();
                filter.setId((String) fm.getOrDefault("id", UUID.randomUUID().toString()));
                filter.setType((String) fm.getOrDefault("type", "select"));
                filter.setField((String) fm.getOrDefault("field", ""));
                filter.setName((String) fm.getOrDefault("label", ""));
                result.filters.add(filter);
            }
            
        } catch (Exception e) {
            log.error("Failed to parse AI response", e);
            result.name = "AI 生成仪表盘";
            result.description = "解析失败，请手动配置";
            result.widgets = Collections.emptyList();
            result.filters = Collections.emptyList();
        }
        
        return result;
    }

    private List<ChartRecommendation> parseChartRecommendations(String response) {
        List<ChartRecommendation> recommendations = new ArrayList<>();
        
        try {
            String json = extractJson(response);
            List<Map<String, Object>> parsed = objectMapper.readValue(json, new TypeReference<>() {});
            
            for (Map<String, Object> item : parsed) {
                ChartRecommendation rec = new ChartRecommendation();
                rec.chartType = (String) item.getOrDefault("chartType", "bar");
                rec.title = (String) item.getOrDefault("title", "");
                rec.reason = (String) item.getOrDefault("reason", "");
                rec.sql = (String) item.getOrDefault("sql", "");
                rec.xField = (String) item.getOrDefault("xField", "");
                rec.yField = (String) item.getOrDefault("yField", "");
                rec.priority = ((Number) item.getOrDefault("priority", 1)).intValue();
                recommendations.add(rec);
            }
            
            recommendations.sort(Comparator.comparingInt(r -> r.priority));
            
        } catch (Exception e) {
            log.error("Failed to parse chart recommendations", e);
        }
        
        return recommendations;
    }

    private String extractJson(String text) {
        // 尝试提取 JSON 块
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        
        if (start == -1) {
            start = text.indexOf('[');
            end = text.lastIndexOf(']');
        }
        
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        
        return text;
    }

    // 内部类
    private static class DashboardGenerationResult {
        String name;
        String description;
        List<WidgetConfig> widgets;
        List<FilterConfig> filters;
    }

    public static class ChartRecommendation {
        public String chartType;
        public String title;
        public String reason;
        public String sql;
        public String xField;
        public String yField;
        public int priority;
    }
}
