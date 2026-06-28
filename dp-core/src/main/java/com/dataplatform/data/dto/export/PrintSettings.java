package com.dataplatform.data.dto.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 打印设置配置
 * Print settings configuration for report export
 * 
 * **Validates: Requirements 23.3, 23.4, 23.5**
 * - 23.3: 支持配置打印页面设置（纸张大小、方向、边距）
 * - 23.4: 支持配置打印表头表尾（每页重复）
 * - 23.5: 支持配置分页规则（按行数、按分组）
 * 
 * @author dataplatform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintSettings {
    
    // ==================== 页面设置 (Requirements 23.3) ====================
    
    /**
     * 纸张大小
     * Paper size: A4, A3, Letter, Legal, etc.
     */
    @Builder.Default
    private PaperSize paperSize = PaperSize.A4;
    
    /**
     * 页面方向
     * Page orientation: portrait (纵向) or landscape (横向)
     */
    @Builder.Default
    private PageOrientation orientation = PageOrientation.PORTRAIT;
    
    /**
     * 上边距（英寸）
     */
    @Builder.Default
    private double marginTop = 0.75;
    
    /**
     * 下边距（英寸）
     */
    @Builder.Default
    private double marginBottom = 0.75;
    
    /**
     * 左边距（英寸）
     */
    @Builder.Default
    private double marginLeft = 0.7;
    
    /**
     * 右边距（英寸）
     */
    @Builder.Default
    private double marginRight = 0.7;
    
    /**
     * 页眉边距（英寸）
     */
    @Builder.Default
    private double headerMargin = 0.3;
    
    /**
     * 页脚边距（英寸）
     */
    @Builder.Default
    private double footerMargin = 0.3;
    
    // ==================== 表头表尾设置 (Requirements 23.4) ====================
    
    /**
     * 是否在每页重复表头
     */
    @Builder.Default
    private boolean repeatHeaderOnEachPage = true;
    
    /**
     * 重复表头的行数（从第1行开始）
     */
    @Builder.Default
    private int repeatHeaderRows = 1;
    
    /**
     * 是否在每页重复左侧列
     */
    @Builder.Default
    private boolean repeatLeftColumns = false;
    
    /**
     * 重复左侧列的列数（从第1列开始）
     */
    @Builder.Default
    private int repeatLeftColumnsCount = 0;
    
    /**
     * 页眉文本（左）
     */
    private String headerLeft;
    
    /**
     * 页眉文本（中）
     */
    private String headerCenter;
    
    /**
     * 页眉文本（右）
     */
    private String headerRight;
    
    /**
     * 页脚文本（左）
     */
    private String footerLeft;
    
    /**
     * 页脚文本（中）
     * 支持占位符: {page} - 当前页码, {pages} - 总页数, {date} - 日期, {time} - 时间
     */
    @Builder.Default
    private String footerCenter = "第 {page} 页，共 {pages} 页";
    
    /**
     * 页脚文本（右）
     */
    private String footerRight;
    
    // ==================== 分页规则设置 (Requirements 23.5) ====================
    
    /**
     * 分页规则类型
     */
    @Builder.Default
    private PaginationRule paginationRule = PaginationRule.AUTO;
    
    /**
     * 按行数分页时的每页行数
     */
    @Builder.Default
    private int rowsPerPage = 50;
    
    /**
     * 按分组分页时的分组字段
     */
    private String groupByField;
    
    /**
     * 是否在分组后分页
     */
    @Builder.Default
    private boolean pageBreakAfterGroup = false;
    
    /**
     * 是否适应页面宽度
     */
    @Builder.Default
    private boolean fitToPageWidth = true;
    
    /**
     * 是否适应页面高度
     */
    @Builder.Default
    private boolean fitToPageHeight = false;
    
    /**
     * 缩放比例（百分比，100表示100%）
     */
    @Builder.Default
    private int scale = 100;
    
    // ==================== 打印选项 ====================
    
    /**
     * 是否打印网格线
     */
    @Builder.Default
    private boolean printGridlines = true;
    
    /**
     * 是否打印行号列号
     */
    @Builder.Default
    private boolean printRowColHeadings = false;
    
    /**
     * 是否黑白打印
     */
    @Builder.Default
    private boolean blackAndWhite = false;
    
    /**
     * 是否草稿质量
     */
    @Builder.Default
    private boolean draftQuality = false;
    
    /**
     * 打印顺序
     */
    @Builder.Default
    private PrintOrder printOrder = PrintOrder.DOWN_THEN_OVER;
    
    /**
     * 纸张大小枚举
     */
    public enum PaperSize {
        A4(9),          // 210 x 297 mm
        A3(8),          // 297 x 420 mm
        A5(11),         // 148 x 210 mm
        LETTER(1),      // 8.5 x 11 inches
        LEGAL(5),       // 8.5 x 14 inches
        EXECUTIVE(7),   // 7.25 x 10.5 inches
        B4(12),         // 250 x 353 mm
        B5(13);         // 176 x 250 mm
        
        private final short poiValue;
        
        PaperSize(int poiValue) {
            this.poiValue = (short) poiValue;
        }
        
        public short getPoiValue() {
            return poiValue;
        }
    }
    
    /**
     * 页面方向枚举
     */
    public enum PageOrientation {
        PORTRAIT,   // 纵向
        LANDSCAPE   // 横向
    }
    
    /**
     * 分页规则枚举
     */
    public enum PaginationRule {
        AUTO,           // 自动分页
        BY_ROWS,        // 按行数分页
        BY_GROUP,       // 按分组分页
        MANUAL          // 手动分页
    }
    
    /**
     * 打印顺序枚举
     */
    public enum PrintOrder {
        DOWN_THEN_OVER,  // 先下后右
        OVER_THEN_DOWN   // 先右后下
    }
}
