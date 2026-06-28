package com.dataplatform.data.dto.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表格导出样式配置
 * Table export style configuration
 * 
 * **Validates: Requirements 23.1**
 * 
 * @author dataplatform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableExportStyle {
    
    // ==================== 表头样式 ====================
    
    /**
     * 表头背景颜色
     */
    @Builder.Default
    private String headerBackgroundColor = "#f5f7fa";
    
    /**
     * 表头字体颜色
     */
    @Builder.Default
    private String headerFontColor = "#303133";
    
    /**
     * 表头字体粗细
     */
    @Builder.Default
    private String headerFontWeight = "bold";
    
    /**
     * 表头字号
     */
    @Builder.Default
    private int headerFontSize = 12;
    
    /**
     * 表头水平对齐
     */
    @Builder.Default
    private String headerHorizontalAlign = "center";
    
    // ==================== 数据行样式 ====================
    
    /**
     * 数据行背景颜色
     */
    @Builder.Default
    private String bodyBackgroundColor = "#ffffff";
    
    /**
     * 数据行字体颜色
     */
    @Builder.Default
    private String bodyFontColor = "#606266";
    
    /**
     * 数据行字号
     */
    @Builder.Default
    private int bodyFontSize = 12;
    
    // ==================== 斑马纹样式 ====================
    
    /**
     * 是否启用斑马纹
     */
    @Builder.Default
    private boolean enableZebraStripes = false;
    
    /**
     * 斑马纹颜色（偶数行背景色）
     */
    @Builder.Default
    private String zebraStripeColor = "#fafafa";
    
    // ==================== 边框样式 ====================
    
    /**
     * 边框样式 (none, horizontal, vertical, all, outer)
     */
    @Builder.Default
    private String borderStyle = "all";
    
    /**
     * 边框颜色
     */
    @Builder.Default
    private String borderColor = "#e8e8e8";
    
    /**
     * 边框宽度
     */
    @Builder.Default
    private int borderWidth = 1;
    
    // ==================== 汇总行样式 ====================
    
    /**
     * 汇总行背景颜色
     */
    private String summaryBackgroundColor;
    
    /**
     * 汇总行字体颜色
     */
    private String summaryFontColor;
    
    /**
     * 汇总行字体粗细
     */
    @Builder.Default
    private String summaryFontWeight = "bold";
}
