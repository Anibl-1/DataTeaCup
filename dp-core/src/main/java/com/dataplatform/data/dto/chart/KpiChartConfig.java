package com.dataplatform.data.dto.chart;

import lombok.Data;

/**
 * KPI 卡片配置 DTO
 * 
 * 用于展示单指标大数字、同比变化率和环比变化率
 */
@Data
public class KpiChartConfig {
    
    /**
     * 值格式: number, currency, percentage, compact
     */
    private String format = "number";
    
    /**
     * 货币符号（format为currency时使用）
     */
    private String currencySymbol = "¥";
    
    /**
     * 小数位数
     */
    private Integer decimals = 2;
    
    /**
     * 单位
     */
    private String unit;
    
    /**
     * 前缀
     */
    private String prefix;
    
    /**
     * 后缀
     */
    private String suffix;
    
    /**
     * 是否显示趋势
     */
    private Boolean showTrend = true;
    
    /**
     * 正向趋势是否为好（用于颜色显示）
     */
    private Boolean positiveIsGood = true;
    
    /**
     * 是否显示迷你图
     */
    private Boolean showSparkline = false;
    
    /**
     * 迷你图类型: line, bar
     */
    private String sparklineType = "line";
    
    /**
     * 当前值字段
     */
    private String valueField;
    
    /**
     * 上期值字段（用于计算同比）
     */
    private String previousValueField;
    
    /**
     * 上周期值字段（用于计算环比）
     */
    private String periodPreviousField;
    
    /**
     * 标题
     */
    private String title;
    
    /**
     * 副标题
     */
    private String subtitle;
    
    /**
     * 主题颜色
     */
    private String themeColor;
    
    /**
     * 字体大小
     */
    private Integer fontSize = 48;
    
    /**
     * 趋势字体大小
     */
    private Integer trendFontSize = 14;
    
    /**
     * 验证配置有效性
     */
    public boolean isValid() {
        if (format == null || format.isEmpty()) {
            return false;
        }
        if (!format.equals("number") && !format.equals("currency") && 
            !format.equals("percentage") && !format.equals("compact")) {
            return false;
        }
        if (decimals != null && (decimals < 0 || decimals > 10)) {
            return false;
        }
        return true;
    }
    
    /**
     * 创建默认配置
     */
    public static KpiChartConfig createDefault() {
        KpiChartConfig config = new KpiChartConfig();
        config.setFormat("number");
        config.setDecimals(2);
        config.setShowTrend(true);
        config.setPositiveIsGood(true);
        return config;
    }
    
    /**
     * 创建货币格式配置
     */
    public static KpiChartConfig createCurrencyConfig(String symbol) {
        KpiChartConfig config = new KpiChartConfig();
        config.setFormat("currency");
        config.setCurrencySymbol(symbol != null ? symbol : "¥");
        config.setDecimals(2);
        config.setShowTrend(true);
        config.setPositiveIsGood(true);
        return config;
    }
    
    /**
     * 创建百分比格式配置
     */
    public static KpiChartConfig createPercentageConfig() {
        KpiChartConfig config = new KpiChartConfig();
        config.setFormat("percentage");
        config.setDecimals(2);
        config.setShowTrend(true);
        config.setPositiveIsGood(true);
        return config;
    }
}
