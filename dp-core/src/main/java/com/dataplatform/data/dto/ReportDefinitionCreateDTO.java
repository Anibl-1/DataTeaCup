package com.dataplatform.data.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 报表定义创建DTO
 * 
 * @author dataplatform
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportDefinitionCreateDTO {
    /** 报表名称 */
    @NotBlank(message = "报表名称不能为空")
    private String reportName;
    
    /** 报表编码（唯一标识） */
    @NotBlank(message = "报表编码不能为空")
    private String reportCode;
    
    /** 数据源ID */
    @NotNull(message = "数据源ID不能为空")
    private Long dataSourceId;
    
    /** SQL查询语句 */
    @NotBlank(message = "SQL查询语句不能为空")
    private String sqlContent;
    
    /** 报表描述 */
    private String description;
    
    /** 查询参数配置（JSON） */
    private String params;
    
    /** 状态：1-启用，0-禁用 */
    private Integer status;
    
    /** 允许导出Excel */
    private Boolean allowExportExcel;
    
    /** 允许导出PDF */
    private Boolean allowExportPdf;
    
    /** 允许打印 */
    private Boolean allowPrint;
    
    /** PDF导出水印文字 */
    private String pdfWatermark;
    
    /** 字段列表 */
    private List<ReportFieldDTO> fields;
}

