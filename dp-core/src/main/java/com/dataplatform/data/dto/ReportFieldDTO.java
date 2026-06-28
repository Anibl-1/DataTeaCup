package com.dataplatform.data.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 报表字段DTO
 * 
 * @author dataplatform
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportFieldDTO {
    /** 字段ID（更新时使用） */
    private Long id;
    
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
}

