package com.dataplatform.common.constants;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 图表类型枚举
 */
public enum ChartType {
    // 基础图表
    LINE("line", "折线图", ChartCategory.BASIC),
    BAR("bar", "柱状图", ChartCategory.BASIC),
    PIE("pie", "饼图", ChartCategory.BASIC),
    TABLE("table", "数据表格", ChartCategory.BASIC),
    SUMMARY_TABLE("summaryTable", "汇总表", ChartCategory.BASIC),
    PIVOT_TABLE("pivotTable", "透视表", ChartCategory.BASIC),
    SCATTER("scatter", "散点图", ChartCategory.BASIC),
    RADAR("radar", "雷达图", ChartCategory.BASIC),
    AREA("area", "面积图", ChartCategory.BASIC),
    // 高级图表
    KPI("kpi", "KPI 卡片", ChartCategory.ADVANCED),
    COMBO("combo", "组合图", ChartCategory.ADVANCED),
    WATERFALL("waterfall", "瀑布图", ChartCategory.ADVANCED),
    WORD_CLOUD("wordCloud", "词云图", ChartCategory.ADVANCED),
    // 特殊图表
    GAUGE("gauge", "仪表盘", ChartCategory.SPECIAL),
    FUNNEL("funnel", "漏斗图", ChartCategory.SPECIAL),
    HEATMAP("heatmap", "热力图", ChartCategory.SPECIAL),
    TREE("tree", "树图", ChartCategory.SPECIAL),
    SANKEY("sankey", "桑基图", ChartCategory.SPECIAL),
    PARALLEL("parallel", "平行坐标", ChartCategory.SPECIAL),
    // 地理图表
    MAP("map", "地图", ChartCategory.GEOGRAPHIC),
    CHINA_MAP("chinaMap", "中国地图", ChartCategory.GEOGRAPHIC),
    WORLD_MAP("worldMap", "世界地图", ChartCategory.GEOGRAPHIC),
    // 统计图表
    BOXPLOT("boxplot", "盒须图", ChartCategory.STATISTICAL),
    // 金融图表
    CANDLESTICK("candlestick", "K线图", ChartCategory.FINANCIAL),
    // 关系图表
    GRAPH("graph", "关系图", ChartCategory.RELATIONSHIP);

    private final String code;
    private final String label;
    private final ChartCategory category;

    ChartType(String code, String label, ChartCategory category) {
        this.code = code;
        this.label = label;
        this.category = category;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
    public ChartCategory getCategory() { return category; }

    public static ChartType fromCode(String code) {
        if (code == null) return null;
        for (ChartType type : values()) {
            if (type.code.equalsIgnoreCase(code)) return type;
        }
        return null;
    }

    public static boolean isValidCode(String code) { return fromCode(code) != null; }

    public static Set<String> getAllCodes() {
        return Arrays.stream(values()).map(ChartType::getCode).collect(Collectors.toSet());
    }

    public static Set<ChartType> getByCategory(ChartCategory category) {
        return Arrays.stream(values()).filter(type -> type.category == category).collect(Collectors.toSet());
    }

    public boolean isGeographic() { return this.category == ChartCategory.GEOGRAPHIC; }

    public boolean needsAxis() {
        return this.category == ChartCategory.BASIC && this != PIE && this != TABLE && this != SUMMARY_TABLE && this != PIVOT_TABLE;
    }

    public enum ChartCategory {
        BASIC("basic", "基础图表"),
        ADVANCED("advanced", "高级图表"),
        SPECIAL("special", "特殊图表"),
        GEOGRAPHIC("geographic", "地理图表"),
        STATISTICAL("statistical", "统计图表"),
        FINANCIAL("financial", "金融图表"),
        RELATIONSHIP("relationship", "关系图表");

        private final String code;
        private final String label;

        ChartCategory(String code, String label) { this.code = code; this.label = label; }
        public String getCode() { return code; }
        public String getLabel() { return label; }
    }
}
