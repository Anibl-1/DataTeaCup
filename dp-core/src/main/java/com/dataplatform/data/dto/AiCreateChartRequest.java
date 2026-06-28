package com.dataplatform.data.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * AI创建图表请求DTO
 */
@Data
public class AiCreateChartRequest {
    private String chartName;
    private String chartCode;
    private String chartType;
    private String description;
    private Long dataSourceId;
    private String sql;
    private String chartConfig;
    private List<Map<String, Object>> queryParams;
    private Long parentMenuId;
    private String icon;
}
