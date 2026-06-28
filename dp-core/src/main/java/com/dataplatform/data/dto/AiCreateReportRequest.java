package com.dataplatform.data.dto;

import lombok.Data;

/**
 * AI创建报表请求DTO
 */
@Data
public class AiCreateReportRequest {
    private String reportName;
    private String description;
    private Long dataSourceId;
    private String sql;
    private Long parentMenuId;
    private String icon;
}
