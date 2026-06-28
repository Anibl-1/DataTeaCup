package com.dataplatform.data.service.subscription;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 推送日志
 */
@Data
public class PushLog {
    private String id;
    private String subscriptionId;
    private String status; // SUCCESS, FAILED
    private String channel;
    private String errorMessage;
    private LocalDateTime pushTime;
}
