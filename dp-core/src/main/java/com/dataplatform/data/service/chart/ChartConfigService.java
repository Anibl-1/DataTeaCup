package com.dataplatform.data.service.chart;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 图表配置服务 - 处理ECharts配置的验证、修复和生成
 * 
 * 职责：
 * 1. 图表配置验证
 * 2. 配置智能修复
 * 3. 默认配置生成
 * 4. 数据映射处理
 */
@Slf4j
@Service
public class ChartConfigService {

    private static final Logger log = LoggerFactory.getLogger(ChartConfigService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 支持的图表类型
    public static final Set<String> VALID_CHART_TYPES = Set.of(
        "bar", "line", "pie", "scatter", "gauge", "radar", "funnel", "heatmap", "area",
        "map", "chinamap", "worldmap", "kpi", "combo", "waterfall", "wordcloud",
        "sankey", "tree", "boxplot", "candlestick", "graph", "parallel",
        "summarytable", "pivottable", "table"
    );
    
    // 默认配色方案
    public static final List<String> DEFAULT_COLORS = List.of(
        "#5470c6", "#91cc75", "#fac858", "#ee6666", "#73c0de",
        "#3ba272", "#fc8452", "#9a60b4", "#ea7ccc", "#67C23A"
    );
    
    // 主题配色
    public static final Map<String, List<String>> THEME_COLORS = Map.of(
        "default", DEFAULT_COLORS,
        "blue", List.of("#1890ff", "#36cfc9", "#40a9ff", "#73d13d", "#9254de"),
        "warm", List.of("#ff7a45", "#ffa940", "#ffc53d", "#ff4d4f", "#f759ab"),
        "cool", List.of("#36cfc9", "#13c2c2", "#1890ff", "#2f54eb", "#722ed1"),
        "business", List.of("#3366cc", "#dc3912", "#ff9900", "#109618", "#990099")
    );
    
    /**
     * 验证图表类型
     */
    public boolean isValidChartType(String chartType) {
        return chartType != null && VALID_CHART_TYPES.contains(chartType.toLowerCase());
    }
    
    /**
     * 验证并修复图表配置
     * 
     * @param configJson 配置JSON字符串
     * @param chartType 图表类型
     * @return 修复后的配置JSON
     */
    public String validateAndFixConfig(String configJson, String chartType) {
        if (!StringUtils.hasText(configJson)) {
            return generateDefaultConfig(chartType, null, null);
        }
        
        try {
            Map<String, Object> config = objectMapper.readValue(configJson, 
                new TypeReference<Map<String, Object>>() {});
            
            // 修复配置
            fixConfig(config, chartType);
            
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            log.warn("配置解析失败，使用默认配置: {}", e.getMessage());
            return generateDefaultConfig(chartType, null, null);
        }
    }
    
    /**
     * 修复图表配置
     */
    @SuppressWarnings("unchecked")
    public void fixConfig(Map<String, Object> config, String chartType) {
        if (config == null) return;
        chartType = chartType != null ? chartType.toLowerCase() : "bar";
        
        // 1. 确保有标题配置
        if (!config.containsKey("title")) {
            config.put("title", Map.of("text", "", "left", "center"));
        }
        
        // 2. 确保有tooltip
        if (!config.containsKey("tooltip")) {
            String trigger = "pie".equals(chartType) ? "item" : "axis";
            config.put("tooltip", Map.of("trigger", trigger));
        }
        
        // 3. 确保有颜色
        if (!config.containsKey("color")) {
            config.put("color", DEFAULT_COLORS);
        }
        
        // 4. 根据图表类型修复特定配置
        switch (chartType) {
            case "pie":
                fixPieConfig(config);
                break;
            case "gauge":
                fixGaugeConfig(config);
                break;
            case "radar":
                fixRadarConfig(config);
                break;
            case "map":
            case "chinamap":
            case "worldmap":
                fixMapConfig(config, chartType);
                break;
            case "kpi":
                fixKpiConfig(config);
                break;
            case "combo":
                fixComboConfig(config);
                break;
            case "waterfall":
                fixWaterfallConfig(config);
                break;
            case "wordcloud":
                fixWordCloudConfig(config);
                break;
            case "sankey":
            case "tree":
            case "graph":
                fixNoAxisConfig(config);
                break;
            case "bar":
            case "line":
            case "area":
            case "scatter":
            default:
                fixCartesianConfig(config, chartType);
                break;
        }
        
        // 5. 确保series存在
        if (!config.containsKey("series") || config.get("series") == null) {
            config.put("series", List.of(createDefaultSeries(chartType)));
        }
    }
    
    /**
     * 修复笛卡尔坐标系图表（柱状图、折线图等）
     */
    private void fixCartesianConfig(Map<String, Object> config, String chartType) {
        // 确保有grid
        if (!config.containsKey("grid")) {
            config.put("grid", Map.of(
                "left", "3%",
                "right", "4%",
                "bottom", "10%",
                "containLabel", true
            ));
        }
        
        // 确保有xAxis
        if (!config.containsKey("xAxis")) {
            config.put("xAxis", Map.of("type", "category"));
        }
        
        // 确保有yAxis
        if (!config.containsKey("yAxis")) {
            config.put("yAxis", Map.of("type", "value"));
        }
    }
    
    /**
     * 修复饼图配置
     */
    private void fixPieConfig(Map<String, Object> config) {
        // 饼图不需要坐标轴
        config.remove("xAxis");
        config.remove("yAxis");
        config.remove("grid");
        
        // 确保legend配置
        if (!config.containsKey("legend")) {
            config.put("legend", Map.of("bottom", "5%", "left", "center"));
        }
    }
    
    /**
     * 修复仪表盘配置
     */
    private void fixGaugeConfig(Map<String, Object> config) {
        config.remove("xAxis");
        config.remove("yAxis");
        config.remove("grid");
    }
    
    /**
     * 修复雷达图配置
     */
    private void fixRadarConfig(Map<String, Object> config) {
        config.remove("xAxis");
        config.remove("yAxis");
        config.remove("grid");
        
        // 确保有radar配置
        if (!config.containsKey("radar")) {
            config.put("radar", Map.of("indicator", List.of()));
        }
    }
    
    /**
     * 修复地图图表配置
     */
    private void fixMapConfig(Map<String, Object> config, String chartType) {
        config.remove("xAxis");
        config.remove("yAxis");
        config.remove("grid");
        
        if (!config.containsKey("visualMap")) {
            config.put("visualMap", Map.of(
                "min", 0, "max", 1000, "calculable", true,
                "inRange", Map.of("color", List.of("#e0f3f8", "#abd9e9", "#74add1", "#4575b4", "#313695")),
                "text", List.of("高", "低"), "left", "left", "top", "bottom"
            ));
        }
    }
    
    /**
     * 修复 KPI 卡片配置（非 ECharts，自定义渲染）
     */
    private void fixKpiConfig(Map<String, Object> config) {
        config.remove("xAxis");
        config.remove("yAxis");
        config.remove("grid");
        config.remove("series");
        
        if (!config.containsKey("format")) {
            config.put("format", "number");
        }
        if (!config.containsKey("decimals")) {
            config.put("decimals", 2);
        }
        if (!config.containsKey("showTrend")) {
            config.put("showTrend", true);
        }
    }
    
    /**
     * 修复组合图配置（双 Y 轴）
     */
    private void fixComboConfig(Map<String, Object> config) {
        if (!config.containsKey("grid")) {
            config.put("grid", Map.of("left", "3%", "right", "8%", "bottom", "10%", "containLabel", true));
        }
        if (!config.containsKey("xAxis")) {
            config.put("xAxis", Map.of("type", "category"));
        }
        // 组合图需要双 Y 轴
        if (!config.containsKey("yAxis")) {
            config.put("yAxis", List.of(
                Map.of("type", "value", "name", ""),
                Map.of("type", "value", "name", "")
            ));
        }
    }
    
    /**
     * 修复瀑布图配置
     */
    private void fixWaterfallConfig(Map<String, Object> config) {
        if (!config.containsKey("grid")) {
            config.put("grid", Map.of("left", "3%", "right", "4%", "bottom", "10%", "containLabel", true));
        }
        if (!config.containsKey("xAxis")) {
            config.put("xAxis", Map.of("type", "category"));
        }
        if (!config.containsKey("yAxis")) {
            config.put("yAxis", Map.of("type", "value"));
        }
    }
    
    /**
     * 修复词云图配置
     */
    private void fixWordCloudConfig(Map<String, Object> config) {
        config.remove("xAxis");
        config.remove("yAxis");
        config.remove("grid");
    }
    
    /**
     * 修复无坐标轴图表（桑基图、树图、关系图等）
     */
    private void fixNoAxisConfig(Map<String, Object> config) {
        config.remove("xAxis");
        config.remove("yAxis");
        config.remove("grid");
    }
    
    /**
     * 创建默认series
     */
    private Map<String, Object> createDefaultSeries(String chartType) {
        Map<String, Object> series = new HashMap<>();
        series.put("name", "数据");
        
        switch (chartType) {
            case "pie":
                series.put("type", "pie");
                series.put("radius", List.of("40%", "70%"));
                series.put("avoidLabelOverlap", false);
                series.put("itemStyle", Map.of(
                    "borderRadius", 10,
                    "borderColor", "#fff",
                    "borderWidth", 2
                ));
                series.put("label", Map.of("show", true, "formatter", "{b}: {d}%"));
                break;
            case "bar":
                series.put("type", "bar");
                series.put("itemStyle", Map.of("borderRadius", List.of(4, 4, 0, 0)));
                break;
            case "line":
            case "area":
                series.put("type", "line");
                series.put("smooth", true);
                if ("area".equals(chartType)) {
                    series.put("areaStyle", Map.of());
                }
                break;
            case "gauge":
                series.put("type", "gauge");
                series.put("progress", Map.of("show", true, "width", 18));
                series.put("axisLine", Map.of("lineStyle", Map.of("width", 18)));
                series.put("detail", Map.of("valueAnimation", true, "fontSize", 36));
                break;
            case "map":
            case "chinamap":
            case "worldmap":
                series.put("type", "map");
                series.put("map", "worldmap".equals(chartType) ? "world" : "china");
                series.put("roam", true);
                series.put("label", Map.of("show", true));
                series.put("emphasis", Map.of("label", Map.of("show", true)));
                break;
            case "waterfall":
                series.put("type", "bar");
                series.put("stack", "waterfall");
                series.put("itemStyle", Map.of("borderRadius", List.of(2, 2, 0, 0)));
                break;
            case "wordcloud":
                series.put("type", "wordCloud");
                series.put("shape", "circle");
                series.put("sizeRange", List.of(12, 60));
                series.put("rotationRange", List.of(-45, 45));
                series.put("gridSize", 8);
                break;
            case "combo":
                series.put("type", "bar");
                series.put("yAxisIndex", 0);
                break;
            case "sankey":
                series.put("type", "sankey");
                series.put("layout", "none");
                series.put("emphasis", Map.of("focus", "adjacency"));
                break;
            case "tree":
                series.put("type", "treemap");
                series.put("roam", false);
                break;
            case "graph":
                series.put("type", "graph");
                series.put("layout", "force");
                series.put("roam", true);
                break;
            default:
                series.put("type", chartType);
                break;
        }
        
        return series;
    }
    
    /**
     * 生成默认配置
     * 
     * @param chartType 图表类型
     * @param title 标题（可选）
     * @param theme 主题（可选）
     * @return 配置JSON字符串
     */
    public String generateDefaultConfig(String chartType, String title, String theme) {
        chartType = chartType != null ? chartType.toLowerCase() : "bar";
        
        Map<String, Object> config = new LinkedHashMap<>();
        
        // 标题
        config.put("title", Map.of(
            "text", title != null ? title : "",
            "left", "center"
        ));
        
        // 配色
        List<String> colors = THEME_COLORS.getOrDefault(theme, DEFAULT_COLORS);
        config.put("color", colors);
        
        // tooltip
        String trigger = Set.of("pie", "gauge", "radar", "funnel").contains(chartType) ? "item" : "axis";
        config.put("tooltip", Map.of("trigger", trigger));
        
        // 根据图表类型添加特定配置
        switch (chartType) {
            case "pie":
                config.put("legend", Map.of("bottom", "5%", "left", "center"));
                config.put("series", List.of(Map.of(
                    "type", "pie",
                    "radius", List.of("40%", "70%"),
                    "avoidLabelOverlap", false,
                    "itemStyle", Map.of("borderRadius", 10, "borderColor", "#fff", "borderWidth", 2),
                    "label", Map.of("show", true, "formatter", "{b}: {d}%"),
                    "data", List.of()
                )));
                break;
                
            case "gauge":
                config.put("series", List.of(Map.of(
                    "type", "gauge",
                    "progress", Map.of("show", true, "width", 18),
                    "axisLine", Map.of("lineStyle", Map.of("width", 18)),
                    "axisTick", Map.of("show", false),
                    "splitLine", Map.of("length", 15, "lineStyle", Map.of("width", 2, "color", "#999")),
                    "axisLabel", Map.of("distance", 25, "color", "#999", "fontSize", 14),
                    "detail", Map.of("valueAnimation", true, "fontSize", 36, "offsetCenter", List.of(0, "70%")),
                    "data", List.of(Map.of("value", 0, "name", ""))
                )));
                break;
                
            case "radar":
                config.put("legend", Map.of("bottom", "5%"));
                config.put("radar", Map.of("indicator", List.of()));
                config.put("series", List.of(Map.of(
                    "type", "radar",
                    "data", List.of()
                )));
                break;
                
            case "map":
            case "chinamap":
            case "worldmap": {
                String mapName = "worldmap".equals(chartType) ? "world" : "china";
                config.put("visualMap", Map.of(
                    "min", 0, "max", 1000, "calculable", true,
                    "inRange", Map.of("color", List.of("#e0f3f8", "#abd9e9", "#74add1", "#4575b4", "#313695")),
                    "text", List.of("高", "低"), "left", "left", "top", "bottom"
                ));
                config.put("series", List.of(Map.of(
                    "type", "map", "map", mapName, "roam", true,
                    "label", Map.of("show", true),
                    "emphasis", Map.of("label", Map.of("show", true)),
                    "data", List.of()
                )));
                break;
            }
                
            case "kpi":
                // KPI 卡片使用自定义渲染，不使用 ECharts series
                config.put("format", "number");
                config.put("decimals", 2);
                config.put("showTrend", true);
                config.put("positiveIsGood", true);
                config.remove("tooltip");
                config.remove("color");
                break;
                
            case "combo":
                config.put("legend", Map.of("bottom", "5%"));
                config.put("grid", Map.of("left", "3%", "right", "8%", "bottom", "15%", "containLabel", true));
                config.put("xAxis", Map.of("type", "category"));
                config.put("yAxis", List.of(
                    Map.of("type", "value", "name", ""),
                    Map.of("type", "value", "name", "")
                ));
                config.put("series", List.of(
                    Map.of("type", "bar", "name", "柱状", "yAxisIndex", 0),
                    Map.of("type", "line", "name", "折线", "yAxisIndex", 1, "smooth", true)
                ));
                break;
                
            case "waterfall":
                config.put("grid", Map.of("left", "3%", "right", "4%", "bottom", "10%", "containLabel", true));
                config.put("xAxis", Map.of("type", "category"));
                config.put("yAxis", Map.of("type", "value"));
                config.put("series", List.of(
                    Map.of("type", "bar", "name", "辅助", "stack", "waterfall",
                        "itemStyle", Map.of("borderColor", "transparent", "color", "transparent"),
                        "emphasis", Map.of("itemStyle", Map.of("borderColor", "transparent", "color", "transparent"))),
                    Map.of("type", "bar", "name", "数据", "stack", "waterfall",
                        "label", Map.of("show", true, "position", "top"))
                ));
                break;
                
            case "wordcloud":
                config.put("series", List.of(Map.of(
                    "type", "wordCloud", "shape", "circle",
                    "sizeRange", List.of(12, 60),
                    "rotationRange", List.of(-45, 45),
                    "gridSize", 8,
                    "textStyle", Map.of("fontFamily", "sans-serif"),
                    "data", List.of()
                )));
                break;
                
            case "sankey":
                config.put("series", List.of(Map.of(
                    "type", "sankey", "layout", "none",
                    "emphasis", Map.of("focus", "adjacency"),
                    "data", List.of(), "links", List.of()
                )));
                break;
                
            case "tree":
                config.put("series", List.of(Map.of(
                    "type", "treemap", "roam", false,
                    "data", List.of()
                )));
                break;
                
            case "line":
            case "area":
                config.put("grid", Map.of("left", "3%", "right", "4%", "bottom", "10%", "containLabel", true));
                config.put("xAxis", Map.of("type", "category", "boundaryGap", false));
                config.put("yAxis", Map.of("type", "value"));
                Map<String, Object> lineSeries = new LinkedHashMap<>();
                lineSeries.put("type", "line");
                lineSeries.put("smooth", true);
                if ("area".equals(chartType)) {
                    lineSeries.put("areaStyle", Map.of());
                }
                config.put("series", List.of(lineSeries));
                break;
                
            case "scatter":
                config.put("grid", Map.of("left", "3%", "right", "4%", "bottom", "10%", "containLabel", true));
                config.put("xAxis", Map.of("type", "value"));
                config.put("yAxis", Map.of("type", "value"));
                config.put("series", List.of(Map.of("type", "scatter", "symbolSize", 10)));
                break;
                
            case "bar":
            default:
                config.put("grid", Map.of("left", "3%", "right", "4%", "bottom", "10%", "containLabel", true));
                config.put("xAxis", Map.of("type", "category"));
                config.put("yAxis", Map.of("type", "value"));
                config.put("series", List.of(Map.of(
                    "type", "bar",
                    "itemStyle", Map.of("borderRadius", List.of(4, 4, 0, 0))
                )));
                break;
        }
        
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            return "{}";
        }
    }
    
    /**
     * 解析并验证数据映射配置
     */
    public Map<String, String> parseDataMapping(String dataMappingJson) {
        Map<String, String> mapping = new HashMap<>();
        if (!StringUtils.hasText(dataMappingJson)) {
            // 默认映射
            mapping.put("xField", "name");
            mapping.put("yField", "value");
            return mapping;
        }
        
        try {
            Map<String, Object> parsed = objectMapper.readValue(dataMappingJson, 
                new TypeReference<Map<String, Object>>() {});
            for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                if (entry.getValue() != null) {
                    mapping.put(entry.getKey(), entry.getValue().toString());
                }
            }
        } catch (Exception e) {
            log.warn("数据映射解析失败: {}", e.getMessage());
            mapping.put("xField", "name");
            mapping.put("yField", "value");
        }
        
        return mapping;
    }
    
    /**
     * 根据数据自动推断数据映射
     */
    public Map<String, String> inferDataMapping(List<Map<String, Object>> data, String chartType) {
        Map<String, String> mapping = new HashMap<>();
        
        if (data == null || data.isEmpty()) {
            mapping.put("xField", "name");
            mapping.put("yField", "value");
            return mapping;
        }
        
        Map<String, Object> firstRow = data.get(0);
        List<String> stringFields = new ArrayList<>();
        List<String> numericFields = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : firstRow.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Number) {
                numericFields.add(field);
            } else {
                stringFields.add(field);
            }
        }
        
        // 饼图使用nameField和valueField
        if ("pie".equals(chartType)) {
            mapping.put("nameField", stringFields.isEmpty() ? firstRow.keySet().iterator().next() : stringFields.get(0));
            mapping.put("valueField", numericFields.isEmpty() ? (stringFields.size() > 1 ? stringFields.get(1) : stringFields.get(0)) : numericFields.get(0));
        } else if ("map".equals(chartType) || "chinamap".equals(chartType) || "worldmap".equals(chartType)) {
            mapping.put("regionField", stringFields.isEmpty() ? firstRow.keySet().iterator().next() : stringFields.get(0));
            mapping.put("valueField", numericFields.isEmpty() ? (stringFields.size() > 1 ? stringFields.get(1) : stringFields.get(0)) : numericFields.get(0));
        } else if ("kpi".equals(chartType)) {
            mapping.put("valueField", numericFields.isEmpty() ? firstRow.keySet().iterator().next() : numericFields.get(0));
            if (numericFields.size() > 1) mapping.put("previousValueField", numericFields.get(1));
            if (numericFields.size() > 2) mapping.put("periodPreviousField", numericFields.get(2));
        } else if ("wordcloud".equals(chartType)) {
            mapping.put("wordField", stringFields.isEmpty() ? firstRow.keySet().iterator().next() : stringFields.get(0));
            mapping.put("weightField", numericFields.isEmpty() ? (stringFields.size() > 1 ? stringFields.get(1) : stringFields.get(0)) : numericFields.get(0));
        } else {
            // 其他图表使用xField和yField
            mapping.put("xField", stringFields.isEmpty() ? firstRow.keySet().iterator().next() : stringFields.get(0));
            mapping.put("yField", numericFields.isEmpty() ? (stringFields.size() > 1 ? stringFields.get(1) : stringFields.get(0)) : numericFields.get(0));
        }
        
        return mapping;
    }
    
    /**
     * 将数据填充到ECharts配置中
     */
    @SuppressWarnings("unchecked")
    public String fillDataToConfig(String configJson, List<Map<String, Object>> data, Map<String, String> dataMapping, String chartType) {
        if (!StringUtils.hasText(configJson) || data == null || data.isEmpty()) {
            return configJson;
        }
        
        try {
            Map<String, Object> config = objectMapper.readValue(configJson, 
                new TypeReference<Map<String, Object>>() {});
            
            chartType = chartType != null ? chartType.toLowerCase() : "bar";
            
            String xField = dataMapping.getOrDefault("xField", dataMapping.getOrDefault("nameField", "name"));
            String yField = dataMapping.getOrDefault("yField", dataMapping.getOrDefault("valueField", "value"));
            
            // 填充数据
            switch (chartType) {
                case "pie":
                    fillPieData(config, data, xField, yField);
                    break;
                case "gauge":
                    fillGaugeData(config, data, yField);
                    break;
                case "map":
                case "chinamap":
                case "worldmap":
                    fillMapData(config, data, dataMapping);
                    break;
                case "wordcloud":
                    fillWordCloudData(config, data, dataMapping);
                    break;
                case "kpi":
                    // KPI uses raw data directly, no ECharts series fill needed
                    break;
                default:
                    fillCartesianData(config, data, xField, yField);
                    break;
            }
            
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            log.error("数据填充失败: {}", e.getMessage());
            return configJson;
        }
    }
    
    @SuppressWarnings("unchecked")
    private void fillPieData(Map<String, Object> config, List<Map<String, Object>> data, String nameField, String valueField) {
        List<Map<String, Object>> seriesData = new ArrayList<>();
        for (Map<String, Object> row : data) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", String.valueOf(row.getOrDefault(nameField, "")));
            item.put("value", row.getOrDefault(valueField, 0));
            seriesData.add(item);
        }
        
        List<Map<String, Object>> series = (List<Map<String, Object>>) config.get("series");
        if (series != null && !series.isEmpty()) {
            series.get(0).put("data", seriesData);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void fillGaugeData(Map<String, Object> config, List<Map<String, Object>> data, String valueField) {
        if (!data.isEmpty()) {
            Object value = data.get(0).getOrDefault(valueField, 0);
            List<Map<String, Object>> series = (List<Map<String, Object>>) config.get("series");
            if (series != null && !series.isEmpty()) {
                series.get(0).put("data", List.of(Map.of("value", value)));
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void fillCartesianData(Map<String, Object> config, List<Map<String, Object>> data, String xField, String yField) {
        List<String> xData = new ArrayList<>();
        List<Object> yData = new ArrayList<>();
        
        for (Map<String, Object> row : data) {
            xData.add(String.valueOf(row.getOrDefault(xField, "")));
            yData.add(row.getOrDefault(yField, 0));
        }
        
        // 设置xAxis数据
        Object xAxis = config.get("xAxis");
        if (xAxis instanceof Map) {
            ((Map<String, Object>) xAxis).put("data", xData);
        }
        
        // 设置series数据
        List<Map<String, Object>> series = (List<Map<String, Object>>) config.get("series");
        if (series != null && !series.isEmpty()) {
            series.get(0).put("data", yData);
        }
    }
    
    /**
     * 填充地图数据
     */
    @SuppressWarnings("unchecked")
    private void fillMapData(Map<String, Object> config, List<Map<String, Object>> data, Map<String, String> dataMapping) {
        String regionField = dataMapping.getOrDefault("regionField", "name");
        String valueField = dataMapping.getOrDefault("valueField", "value");
        
        List<Map<String, Object>> mapData = new ArrayList<>();
        for (Map<String, Object> row : data) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", String.valueOf(row.getOrDefault(regionField, "")));
            item.put("value", row.getOrDefault(valueField, 0));
            mapData.add(item);
        }
        
        List<Map<String, Object>> series = (List<Map<String, Object>>) config.get("series");
        if (series != null && !series.isEmpty()) {
            series.get(0).put("data", mapData);
        }
    }
    
    /**
     * 填充词云数据
     */
    @SuppressWarnings("unchecked")
    private void fillWordCloudData(Map<String, Object> config, List<Map<String, Object>> data, Map<String, String> dataMapping) {
        String wordField = dataMapping.getOrDefault("wordField", "name");
        String weightField = dataMapping.getOrDefault("weightField", "value");
        
        List<Map<String, Object>> cloudData = new ArrayList<>();
        for (Map<String, Object> row : data) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", String.valueOf(row.getOrDefault(wordField, "")));
            item.put("value", row.getOrDefault(weightField, 0));
            cloudData.add(item);
        }
        
        List<Map<String, Object>> series = (List<Map<String, Object>>) config.get("series");
        if (series != null && !series.isEmpty()) {
            series.get(0).put("data", cloudData);
        }
    }
    
    /**
     * 合并配置（用于更新时部分更新）
     */
    public String mergeConfig(String baseConfigJson, String updateConfigJson) {
        if (!StringUtils.hasText(baseConfigJson)) return updateConfigJson;
        if (!StringUtils.hasText(updateConfigJson)) return baseConfigJson;
        
        try {
            Map<String, Object> baseConfig = objectMapper.readValue(baseConfigJson, 
                new TypeReference<Map<String, Object>>() {});
            Map<String, Object> updateConfig = objectMapper.readValue(updateConfigJson, 
                new TypeReference<Map<String, Object>>() {});
            
            deepMerge(baseConfig, updateConfig);
            
            return objectMapper.writeValueAsString(baseConfig);
        } catch (Exception e) {
            log.warn("配置合并失败: {}", e.getMessage());
            return updateConfigJson;
        }
    }
    
    @SuppressWarnings("unchecked")
    private void deepMerge(Map<String, Object> base, Map<String, Object> update) {
        for (Map.Entry<String, Object> entry : update.entrySet()) {
            String key = entry.getKey();
            Object updateValue = entry.getValue();
            Object baseValue = base.get(key);
            
            if (updateValue instanceof Map && baseValue instanceof Map) {
                deepMerge((Map<String, Object>) baseValue, (Map<String, Object>) updateValue);
            } else {
                base.put(key, updateValue);
            }
        }
    }
}
