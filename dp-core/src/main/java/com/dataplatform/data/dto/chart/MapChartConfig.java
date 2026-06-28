package com.dataplatform.data.dto.chart;

import lombok.Data;
import java.util.List;

/**
 * 地图图表配置 DTO
 * 
 * 支持中国地图和世界地图的配置
 */
@Data
public class MapChartConfig {
    
    /**
     * 地图类型: china, world, province
     */
    private String mapType = "china";
    
    /**
     * 视觉映射配置
     */
    private VisualMapConfig visualMapConfig;
    
    /**
     * 是否显示标签
     */
    private Boolean showLabel = true;
    
    /**
     * 是否启用下钻
     */
    private Boolean enableDrillDown = false;
    
    /**
     * 区域名称字段
     */
    private String regionField;
    
    /**
     * 数值字段
     */
    private String valueField;
    
    /**
     * 省份名称（用于省级地图下钻）
     */
    private String provinceName;
    
    /**
     * 地图缩放级别
     */
    private Double zoom = 1.0;
    
    /**
     * 地图中心点
     */
    private List<Double> center;
    
    /**
     * 是否显示南海诸岛
     */
    private Boolean showSouthChinaSea = true;
    
    /**
     * 视觉映射配置
     */
    @Data
    public static class VisualMapConfig {
        /**
         * 最小值
         */
        private Double min = 0.0;
        
        /**
         * 最大值
         */
        private Double max = 100.0;
        
        /**
         * 颜色范围
         */
        private List<String> colors;
        
        /**
         * 文本标签 [高, 低]
         */
        private List<String> text;
        
        /**
         * 是否可计算（自动计算min/max）
         */
        private Boolean calculable = true;
        
        /**
         * 是否显示
         */
        private Boolean show = true;
        
        /**
         * 位置: left, right, bottom, top
         */
        private String orient = "vertical";
        
        /**
         * 左边距
         */
        private String left = "left";
        
        /**
         * 上边距
         */
        private String top = "bottom";
    }
    
    /**
     * 验证配置有效性
     */
    public boolean isValid() {
        if (mapType == null || mapType.isEmpty()) {
            return false;
        }
        if (!mapType.equals("china") && !mapType.equals("world") && !mapType.equals("province")) {
            return false;
        }
        return true;
    }
    
    /**
     * 创建默认中国地图配置
     */
    public static MapChartConfig createChinaMapDefault() {
        MapChartConfig config = new MapChartConfig();
        config.setMapType("china");
        config.setShowLabel(true);
        config.setEnableDrillDown(true);
        
        VisualMapConfig visualMap = new VisualMapConfig();
        visualMap.setMin(0.0);
        visualMap.setMax(1000.0);
        visualMap.setColors(List.of("#e0f3f8", "#abd9e9", "#74add1", "#4575b4", "#313695"));
        visualMap.setText(List.of("高", "低"));
        config.setVisualMapConfig(visualMap);
        
        return config;
    }
    
    /**
     * 创建默认世界地图配置
     */
    public static MapChartConfig createWorldMapDefault() {
        MapChartConfig config = new MapChartConfig();
        config.setMapType("world");
        config.setShowLabel(false);
        config.setEnableDrillDown(false);
        
        VisualMapConfig visualMap = new VisualMapConfig();
        visualMap.setMin(0.0);
        visualMap.setMax(1000.0);
        visualMap.setColors(List.of("#f7fbff", "#deebf7", "#c6dbef", "#9ecae1", "#6baed6", "#4292c6", "#2171b5", "#084594"));
        visualMap.setText(List.of("高", "低"));
        config.setVisualMapConfig(visualMap);
        
        return config;
    }
}
