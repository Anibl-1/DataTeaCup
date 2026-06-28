package com.dataplatform.data.dto;

import lombok.Data;

/**
 * AI配置请求DTO
 */
@Data
public class AiConfigRequest {
    private String provider;
    private String apiKey;
    private String baseUrl;
    private String model;
    private Integer maxHistory;
    private Double temperature;
    private Integer maxTokens;
    private Boolean setActive;
}
