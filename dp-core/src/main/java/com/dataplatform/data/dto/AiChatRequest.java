package com.dataplatform.data.dto;

import lombok.Data;

/**
 * AI对话请求DTO
 */
@Data
public class AiChatRequest {
    private String message;
    private String context;
    private String sessionId;
}
