package com.dataplatform.data.entity;

import lombok.Data;
import java.util.Date;

/**
 * 图表定义实体类
 * 
 * @author dataplatform
 */
@Data
public class ChartDefinition {
    /** 图表ID */
    private Long id;
    
    /** 图表名称 */
    private String chartName;
    
    /** 图表编码（唯一标识） */
    private String chartCode;
    
    /** 图表类型：line,bar,pie,scatter,radar,map等 */
    private String chartType;
    
    /** 数据源ID */
    private Long dataSourceId;
    
    /** SQL查询语句 */
    private String sqlContent;
    
    /** 图表配置（JSON格式，ECharts配置） */
    private String chartConfig;
    
    /** 图表描述 */
    private String description;
    
    /** 状态：1-启用，0-禁用 */
    private Integer status;
    
    /** 移动端启用：1-启用，0-禁用 */
    private Integer mobileEnabled;
    
    /** 允许导出Excel */
    private Integer allowExportExcel;
    
    /** 允许导出PDF */
    private Integer allowExportPdf;
    
    /** 水印类型: none-无水印, user_ip-用户名_IP, custom-自定义文本 */
    private String watermarkType;
    
    /** PDF导出水印文字（自定义文本） */
    private String pdfWatermark;
    
    /** 创建人ID */
    private Long createBy;
    
    /** 创建时间 */
    private Date createTime;
    
    /** 更新时间 */
    private Date updateTime;
    
    /** 数据源信息（关联查询） */
    private DataSource dataSource;
}

