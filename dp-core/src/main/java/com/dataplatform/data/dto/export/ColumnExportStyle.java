package com.dataplatform.data.dto.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 列导出样式配置
 * Column export style configuration
 * 
 * **Validates: Requirements 23.1**
 * 
 * @author dataplatform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnExportStyle {
    
    /**
     * 字段名
     */
    private String fieldName;
    
    // ==================== 字体样式 ====================
    
    /**
     * 字体族
     */
    private String fontFamily;
    
    /**
     * 字号
     */
    private Integer fontSize;
    
    /**
     * 字体粗细 (normal, bold, 100-900)
     */
    private String fontWeight;
    
    /**
     * 字体样式 (normal, italic, oblique)
     */
    private String fontStyle;
    
    /**
     * 文字装饰 (none, underline, line-through)
     */
    private String textDecoration;
    
    /**
     * 字体颜色
     */
    private String fontColor;
    
    // ==================== 对齐样式 ====================
    
    /**
     * 水平对齐 (left, center, right, justify)
     */
    private String horizontalAlign;
    
    /**
     * 垂直对齐 (top, middle, bottom)
     */
    private String verticalAlign;
    
    /**
     * 是否自动换行
     */
    private Boolean wrapText;
    
    /**
     * 文字旋转角度 (-90 to 90)
     */
    private Integer rotation;
    
    // ==================== 边框样式 ====================
    
    /**
     * 边框样式 (none, solid, dashed, dotted, double)
     */
    private String borderStyle;
    
    /**
     * 边框宽度
     */
    private Integer borderWidth;
    
    /**
     * 边框颜色
     */
    private String borderColor;
    
    // ==================== 背景样式 ====================
    
    /**
     * 背景类型 (solid, gradient, pattern)
     */
    private String backgroundType;
    
    /**
     * 背景颜色
     */
    private String backgroundColor;
    
    // ==================== 数据格式 ====================
    
    /**
     * 数据格式类型 (number, date, text, custom)
     */
    private String formatType;
    
    /**
     * 数值格式 - 小数位数
     */
    private Integer decimalPlaces;
    
    /**
     * 数值格式 - 是否使用千分位
     */
    private Boolean useThousandsSeparator;
    
    /**
     * 数值格式 - 负数格式 (minus, parentheses, red, redParentheses)
     */
    private String negativeFormat;
    
    /**
     * 数值格式 - 前缀
     */
    private String prefix;
    
    /**
     * 数值格式 - 后缀
     */
    private String suffix;
    
    /**
     * 日期格式模式
     */
    private String datePattern;
    
    /**
     * 空值显示文本
     */
    private String emptyText;
}
