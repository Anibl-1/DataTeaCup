package com.dataplatform.serviceapi.data.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * SQL 执行结果 DTO
 */
@Data
public class SqlExecuteResult implements Serializable {
    private List<String> columns;
    private List<Map<String, Object>> rows;
    private long totalRows;
    private long executionTimeMs;
}
