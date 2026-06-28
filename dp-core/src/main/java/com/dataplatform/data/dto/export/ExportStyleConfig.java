package com.dataplatform.data.dto.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 导出样式配置
 * Export style configuration for preserving conditional formatting and styles during export
 * 
 * **Validates: Requirements 23.1, 23.2**
 * - 23.1: 导出时保留所有条件格式化样式
 * - 23.2: 导出时保留迷你图表（转换为静态图片）
 * 
 * @author dataplatform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportStyleConfig {
    
    /**
     * 是否保留条件格式化样式
     * Whether to preserve conditional formatting styles
     */
    @Builder.Default
    private boolean preserveConditionalFormatting = true;
    
    /**
     * 是否将迷你图表转换为静态图片
     * Whether to convert mini charts to static images
     */
    @Builder.Default
    private boolean convertMiniChartsToImages = true;
    
    /**
     * 列样式配置映射 (字段名 -> 样式配置JSON)
     * Column style configuration map (field name -> style config JSON)
     */
    private Map<String, ColumnExportStyle> columnStyles;
    
    /**
     * 条件格式化规则列表
     * Conditional formatting rules list
     */
    private List<ConditionalFormatRule> conditionalRules;
    
    /**
     * 迷你图表配置映射 (字段名 -> 迷你图表配置)
     * Mini chart configuration map (field name -> mini chart config)
     */
    private Map<String, MiniChartExportConfig> miniChartConfigs;
    
    /**
     * 表格样式配置
     * Table style configuration
     */
    private TableExportStyle tableStyle;
    
    /**
     * 是否启用斑马纹
     * Whether to enable zebra stripes
     */
    @Builder.Default
    private boolean enableZebraStripes = false;
    
    /**
     * 斑马纹颜色
     * Zebra stripe color
     */
    private String zebraStripeColor;
}
