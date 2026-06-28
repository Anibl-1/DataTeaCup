package com.dataplatform.data.entity;

import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * 报表定义实体类
 * 
 * @author dataplatform
 */
@Data
public class ReportDefinition {
    /** 报表ID */
    private Long id;
    
    /** 报表名称 */
    private String reportName;
    
    /** 报表编码（唯一标识） */
    private String reportCode;
    
    /** 数据源ID */
    private Long dataSourceId;
    
    /** SQL查询语句 */
    private String sqlContent;
    
    /** 查询参数配置（JSON） */
    private String params;
    
    /** 报表描述 */
    private String description;
    
    /** 状态：1-启用，0-禁用 */
    private Integer status;
    
    /** 移动端启用：1-启用，0-禁用 */
    private Integer mobileEnabled;
    
    /** 报表类型：sql-SQL模式，visual-可视化模式 */
    private String reportType;
    
    /** 配置JSON */
    private String configJson;
    
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
    
    /** 创建人ID */
    private Long createBy;
    
    /** 创建时间 */
    private Date createTime;
    
    /** 更新时间 */
    private Date updateTime;
    
    /** 字段列表（关联查询） */
    private List<ReportField> fields;
    
    /** 数据源信息（关联查询） */
    private DataSource dataSource;
}

