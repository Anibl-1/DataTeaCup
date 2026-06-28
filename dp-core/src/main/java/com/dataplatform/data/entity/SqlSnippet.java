package com.dataplatform.data.entity;

import lombok.Data;
import java.util.Date;

/**
 * SQL收藏片段实体
 */
@Data
public class SqlSnippet {
    private Long id;
    private String name;
    private String sqlContent;
    private String description;
    private String dbType;
    private Date createTime;
    private Date updateTime;
}
