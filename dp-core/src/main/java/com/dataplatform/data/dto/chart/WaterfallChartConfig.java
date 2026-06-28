package com.dataplatform.data.dto.chart;

import lombok.Data;

/**
 * 瀑布图配置 DTO
 * 
 * 用于展示财务增减分析
 */
@Data
public class WaterfallChartConfig {
    
    /**
     * 是否显示总计
     */
    private Boolean showTotal = true;
    
    /**
     * 总计标签
     */
    private String totalLabel = "总计";
    
    /**
     * 起始值
     */
    private Double startValue = 0.0;
    
    /**
     * 起始标签
     */
    private String startLabel = "起始";
    
    /**
     * 正值颜色
     */
    private String positiveColor = "#91cc75";
    
    /**
     * 负值颜色
     */
    private String negativeColor = "#ee6666";
    
    /**
     * 总计颜色
     */
    private String totalColor = "#5470c6";
    
    /**
     * 名称字段
     */
    private String nameField;
    
    /**
     * 数值字段
     */
    private String valueField;
    
    /**
     * 是否为总计字段（用于标记哪些项是总计）
     */
    private String isTotalField;
    
    /**
     * 是否显示标签
     */
    private Boolean showLabel = true;
    
    /**
     * 标签位置: top, inside, bottom
     */
    private String labelPosition = "top";
    
    /**
     * 是否显示连接线
     */
    private Boolean showConnector = true;
    
    /**
     * 连接线颜色
     */
    private String connectorColor = "#aaa";
    
    /**
     * 柱子宽度
     */
    private String barWidth = "40%";
    
    /**
     * 验证配置有效性
     */
    public boolean isValid() {
        // 颜色格式验证
        if (positiveColor != null && !isValidColor(positiveColor)) {
            return false;
        }
        if (negativeColor != null && !isValidColor(negativeColor)) {
            return false;
        }
        if (totalColor != null && !isValidColor(totalColor)) {
            return false;
        }
        return true;
    }
    
    /**
     * 验证颜色格式
     */
    private boolean isValidColor(String color) {
        if (color == null || color.isEmpty()) {
            return false;
        }
        // 支持 #RGB, #RRGGBB, rgb(), rgba() 格式
        return color.matches("^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6})$") ||
               color.matches("^rgb\\(\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*\\)$") ||
               color.matches("^rgba\\(\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*[0-9.]+\\s*\\)$");
    }
    
    /**
     * 创建默认配置
     */
    public static WaterfallChartConfig createDefault() {
        WaterfallChartConfig config = new WaterfallChartConfig();
        config.setShowTotal(true);
        config.setTotalLabel("总计");
        config.setStartValue(0.0);
        config.setPositiveColor("#91cc75");
        config.setNegativeColor("#ee6666");
        config.setTotalColor("#5470c6");
        config.setShowLabel(true);
        config.setShowConnector(true);
        return config;
    }
    
    /**
     * 创建财务分析配置
     */
    public static WaterfallChartConfig createFinancialConfig() {
        WaterfallChartConfig config = createDefault();
        config.setStartLabel("期初余额");
        config.setTotalLabel("期末余额");
        config.setPositiveColor("#52c41a");
        config.setNegativeColor("#f5222d");
        config.setTotalColor("#1890ff");
        return config;
    }
}
