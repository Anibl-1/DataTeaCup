package com.dataplatform.data.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 报表定义更新DTO
 * 
 * @author dataplatform
 */
@Data
public class ReportDefinitionUpdateDTO {
    /** 报表ID */
    @NotNull(message = "报表ID不能为空")
    private Long id;
    
    /** 报表名称 */
    private String reportName;
    
    /** 报表编码（唯一标识） */
    private String reportCode;
    
    /** 数据源ID */
    private Long dataSourceId;
    
    /** SQL查询语句 */
    private String sqlContent;
    
    /** 报表描述 */
    private String description;
    
    /** 查询参数配置（JSON） */
    private String params;
    
    /** 状态：1-启用，0-禁用 */
    private Integer status;
    
    /** 报表类型：sql-SQL模式，visual-可视化模式 */
    private String reportType;
    
    /** 允许导出Excel：1-允许，0-禁止 */
    private Integer allowExportExcel;
    
    /** 允许导出PDF：1-允许，0-禁止 */
    private Integer allowExportPdf;
    
    /** 允许打印：1-允许，0-禁止 */
    private Integer allowPrint;
    
    /** PDF导出水印文字 */
    private String pdfWatermark;
    
    /** 水印类型：none-无水印，user_ip-用户名_IP，custom-自定义文本 */
    private String watermarkType;
    
    /** 字段列表 */
    private List<ReportFieldDTO> fields;
}

