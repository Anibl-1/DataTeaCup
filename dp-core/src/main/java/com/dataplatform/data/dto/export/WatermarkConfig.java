package com.dataplatform.data.dto.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 水印配置
 * Watermark configuration for report export
 * 
 * **Validates: Requirements 23.6**
 * - 支持水印配置（文字水印、图片水印）
 * 
 * @author dataplatform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatermarkConfig {
    
    /**
     * 水印类型
     */
    @Builder.Default
    private WatermarkType type = WatermarkType.TEXT;
    
    /**
     * 是否启用水印
     */
    @Builder.Default
    private boolean enabled = false;
    
    // ==================== 文字水印配置 ====================
    
    /**
     * 水印文字内容
     */
    private String text;
    
    /**
     * 字体名称
     */
    @Builder.Default
    private String fontName = "Microsoft YaHei";
    
    /**
     * 字体大小
     */
    @Builder.Default
    private int fontSize = 48;
    
    /**
     * 字体颜色（十六进制）
     */
    @Builder.Default
    private String fontColor = "#CCCCCC";
    
    /**
     * 透明度（0-100，100为完全不透明）
     */
    @Builder.Default
    private int opacity = 30;
    
    /**
     * 旋转角度（度）
     */
    @Builder.Default
    private int rotation = -45;
    
    /**
     * 是否加粗
     */
    @Builder.Default
    private boolean bold = false;
    
    /**
     * 是否斜体
     */
    @Builder.Default
    private boolean italic = false;
    
    // ==================== 图片水印配置 ====================
    
    /**
     * 图片URL或Base64编码
     */
    private String imageUrl;
    
    /**
     * 图片Base64数据
     */
    private String imageBase64;
    
    /**
     * 图片宽度（像素）
     */
    @Builder.Default
    private int imageWidth = 200;
    
    /**
     * 图片高度（像素）
     */
    @Builder.Default
    private int imageHeight = 100;
    
    // ==================== 通用配置 ====================
    
    /**
     * 水印位置
     */
    @Builder.Default
    private WatermarkPosition position = WatermarkPosition.CENTER;
    
    /**
     * 是否平铺
     */
    @Builder.Default
    private boolean tiled = true;
    
    /**
     * 平铺时的水平间距（像素）
     */
    @Builder.Default
    private int horizontalSpacing = 200;
    
    /**
     * 平铺时的垂直间距（像素）
     */
    @Builder.Default
    private int verticalSpacing = 150;
    
    /**
     * 水印类型枚举
     */
    public enum WatermarkType {
        TEXT,   // 文字水印
        IMAGE   // 图片水印
    }
    
    /**
     * 水印位置枚举
     */
    public enum WatermarkPosition {
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
        CENTER_LEFT,
        CENTER,
        CENTER_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER,
        BOTTOM_RIGHT
    }
}
