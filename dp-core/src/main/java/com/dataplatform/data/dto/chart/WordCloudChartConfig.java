package com.dataplatform.data.dto.chart;

import lombok.Data;
import java.util.List;

/**
 * 词云图配置 DTO
 * 
 * 用于展示文本频率分析
 */
@Data
public class WordCloudChartConfig {
    
    /**
     * 词云形状: circle, cardioid, diamond, triangle-forward, triangle, pentagon, star
     */
    private String shape = "circle";
    
    /**
     * 颜色范围
     */
    private List<String> colorRange;
    
    /**
     * 最小字体大小
     */
    private Integer minFontSize = 12;
    
    /**
     * 最大字体大小
     */
    private Integer maxFontSize = 60;
    
    /**
     * 旋转角度范围 [min, max]
     */
    private List<Integer> rotationRange;
    
    /**
     * 词语字段
     */
    private String wordField;
    
    /**
     * 权重字段
     */
    private String weightField;
    
    /**
     * 网格大小（影响词语密度）
     */
    private Integer gridSize = 8;
    
    /**
     * 是否允许词语重叠
     */
    private Boolean drawOutOfBound = false;
    
    /**
     * 词云宽度
     */
    private String width = "100%";
    
    /**
     * 词云高度
     */
    private String height = "100%";
    
    /**
     * 背景颜色
     */
    private String backgroundColor = "transparent";
    
    /**
     * 字体
     */
    private String fontFamily = "sans-serif";
    
    /**
     * 字体粗细
     */
    private String fontWeight = "normal";
    
    /**
     * 验证配置有效性
     */
    public boolean isValid() {
        // 验证形状
        if (shape != null && !isValidShape(shape)) {
            return false;
        }
        // 验证字体大小
        if (minFontSize != null && maxFontSize != null && minFontSize > maxFontSize) {
            return false;
        }
        if (minFontSize != null && minFontSize < 1) {
            return false;
        }
        if (maxFontSize != null && maxFontSize > 200) {
            return false;
        }
        // 验证旋转角度
        if (rotationRange != null && rotationRange.size() == 2) {
            int min = rotationRange.get(0);
            int max = rotationRange.get(1);
            if (min < -90 || max > 90 || min > max) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 验证形状是否有效
     */
    private boolean isValidShape(String shape) {
        return shape.equals("circle") || shape.equals("cardioid") || 
               shape.equals("diamond") || shape.equals("triangle-forward") ||
               shape.equals("triangle") || shape.equals("pentagon") || 
               shape.equals("star");
    }
}