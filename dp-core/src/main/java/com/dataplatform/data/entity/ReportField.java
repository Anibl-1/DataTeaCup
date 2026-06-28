package com.dataplatform.data.entity;

import lombok.Data;
import java.util.Date;

/**
 * 报表字段实体类
 * 
 * @author dataplatform
 */
@Data
public class ReportField {
    /** 字段ID */
    private Long id;
    
    /** 报表ID */
    private Long reportId;
    
    /** 字段名称（数据库字段名） */
    private String fieldName;
    
    /** 字段显示名称（表头） */
    private String fieldLabel;
    
    /** 字段类型 */
    private String fieldType;
    
    /** 排序顺序 */
    private Integer sortOrder;
    
    /** 是否可见：1-可见，0-隐藏 */
    private Integer isVisible;
    
    /** 列宽度 */
    private Integer width;
    
    /** 对齐方式：left,center,right */
    private String align;

    /** 关联数据字典类型（用于值映射翻译） */
    private String dictType;
    
    /** 创建时间 */
    private Date createTime;
    
    /** 更新时间 */
    private Date updateTime;
}

