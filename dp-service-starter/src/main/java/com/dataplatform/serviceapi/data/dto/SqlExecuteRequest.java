package com.dataplatform.serviceapi.data.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * SQL 执行请求 DTO
 */
@Data
public class SqlExecuteRequest implements Serializable {
    private Long dataSourceId;
    private String sql;
    private Integer limit;
    private Map<String, Object> parameters;
}
