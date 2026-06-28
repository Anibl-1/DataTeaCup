package com.dataplatform.data.dto.export;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * 导出配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportConfig {
    
    /**
     * 导出格式: excel, csv, json
     */
    private String format;
    
    /**
     * Sheet名称（Excel导出时使用）
     */
    private String sheetName;
    
    /**
     * CSV分隔符
     */
    private String separator;
    
    /**
     * 是否包含表头
     */
    private Boolean includeHeader;
    
    /**
     * 字段标签映射（字段名 -> 显示名）
     */
    private Map<String, String> fieldLabels;
    
    /**
     * 每批次处理行数
     */
    private Integer batchSize;
    
    /**
     * 是否启用压缩
     */
    private Boolean enableCompression;
    
    /**
     * 压缩阈值（字节）
     */
    private Long compressionThreshold;
    
    /**
     * 样式配置
     */
    private ExportStyleConfig styleConfig;
    
    /**
     * 打印设置
     */
    private PrintSettings printSettings;
    
    /**
     * 水印配置
     */
    private WatermarkConfig watermarkConfig;
    
    /**
     * 获取默认配置
     */
    public static ExportConfig defaultConfig() {
        return ExportConfig.builder()
                .format("excel")
                .sheetName("数据")
                .separator(",")
                .includeHeader(true)
                .batchSize(1000)
                .enableCompression(true)
                .compressionThreshold(10 * 1024 * 1024L) // 10MB
                .build();
    }
}
