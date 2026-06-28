package com.dataplatform.data.dto;

import lombok.Data;

/**
 * AI创建ETL任务请求DTO
 */
@Data
public class AiCreateEtlRequest {
    private String jobName;
    private String jobDesc;
    private Long sourceDataSourceId;
    private String sourceTable;
    private Long targetDataSourceId;
    private String targetTable;
    private String writeMode = "insert";
}
