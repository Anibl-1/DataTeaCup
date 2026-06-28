package com.dataplatform.data.dto.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 迷你图表导出配置
 * Mini chart export configuration for converting to static images
 * 
 * **Validates: Requirements 23.2**
 * - 导出时保留迷你图表（转换为静态图片）
 * 
 * @author dataplatform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiniChartExportConfig {
    
    /**
     * 字段名
     */
    private String fieldName;
    
    /**
     * 迷你图表类型 (dataBar, sparkline, iconSet, progressBar)
     */
    private String chartType;
    
    /**
     * 图表宽度（像素）
     */
    @Builder.Default
    private int width = 100;
    
    /**
     * 图表高度（像素）
     */
    @Builder.Default
    private int height = 20;
    
    // ==================== 数据条配置 ====================
    
    /**
     * 数据条 - 最小值
     */
    private Double dataBarMin;
    
    /**
     * 数据条 - 最大值
     */
    private Double dataBarMax;
    
    /**
     * 数据条 - 正值颜色
     */
    @Builder.Default
    private String dataBarColor = "#1890ff";
    
    /**
     * 数据条 - 负值颜色
     */
    @Builder.Default
    private String dataBarNegativeColor = "#ff4d4f";
    
    /**
     * 数据条 - 是否显示数值
     */
    @Builder.Default
    private boolean dataBarShowValue = true;
    
    /**
     * 数据条 - 背景颜色
     */
    @Builder.Default
    private String dataBarBackgroundColor = "#f0f0f0";
    
    // ==================== 迷你折线图配置 ====================
    
    /**
     * 折线图 - 数据字段（用于获取历史数据）
     */
    private String sparklineDataField;
    
    /**
     * 折线图 - 线条颜色
     */
    @Builder.Default
    private String sparklineColor = "#1890ff";
    
    /**
     * 折线图 - 是否显示数据点
     */
    @Builder.Default
    private boolean sparklineShowPoints = false;
    
    /**
     * 折线图 - 是否平滑曲线
     */
    @Builder.Default
    private boolean sparklineSmooth = true;
    
    /**
     * 折线图 - 是否显示区域填充
     */
    @Builder.Default
    private boolean sparklineShowArea = false;
    
    // ==================== 图标集配置 ====================
    
    /**
     * 图标集 - 预设类型 (arrows, traffic, stars, flags, ratings)
     */
    private String iconSetType;
    
    /**
     * 图标集 - 自定义阈值配置
     */
    private List<IconThreshold> iconThresholds;
    
    /**
     * 图标集 - 是否显示数值
     */
    @Builder.Default
    private boolean iconSetShowValue = false;
    
    // ==================== 进度条配置 ====================
    
    /**
     * 进度条 - 最大值
     */
    @Builder.Default
    private double progressBarMax = 100;
    
    /**
     * 进度条 - 颜色
     */
    @Builder.Default
    private String progressBarColor = "#1890ff";
    
    /**
     * 进度条 - 轨道颜色
     */
    @Builder.Default
    private String progressBarTrackColor = "#f0f0f0";
    
    /**
     * 进度条 - 是否显示标签
     */
    @Builder.Default
    private boolean progressBarShowLabel = true;
    
    /**
     * 进度条 - 标签格式
     */
    @Builder.Default
    private String progressBarLabelFormat = "{percent}%";
    
    /**
     * 图标阈值配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IconThreshold {
        /**
         * 图标名称
         */
        private String icon;
        
        /**
         * 图标颜色
         */
        private String color;
        
        /**
         * 阈值
         */
        private double threshold;
        
        /**
         * 比较运算符 (gte, gt, lte, lt, eq)
         */
        @Builder.Default
        private String operator = "gte";
    }
}
