package com.dataplatform.data.dto;

import lombok.Data;

/**
 * AI SQL相关请求DTO（生成、优化、解释）
 */
@Data
public class AiSqlRequest {
    private String query;
    private String sql;
    private String tableSchema;
    private String dbType;
    private Long dataSourceId;
}
