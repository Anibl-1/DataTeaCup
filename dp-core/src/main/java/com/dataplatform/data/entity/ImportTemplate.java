package com.dataplatform.data.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

/**
 * 导入模板实体
 * 支持自定义导入模板的存储和管理
 */
@Data
public class ImportTemplate {
    private Long id;
    private String templateName;
    private Long dataSourceId;
    private String tableName;
    private String columnHeaders; // JSON格式：列头信息
    private String filePath;      // 模板文件存储路径
    private String description;
    private Long createBy;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
    
    private Integer delFlag;
}
