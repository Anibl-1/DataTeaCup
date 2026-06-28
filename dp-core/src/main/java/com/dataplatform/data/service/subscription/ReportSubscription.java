package com.dataplatform.data.service.subscription;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 报表订阅
 * 需求: 25.1, 25.2
 */
@Data
public class ReportSubscription {
    private String id;
    private String userId;
    private String resourceType; // report, dashboard
    private String resourceId;
    private String resourceName;
    private String cronExpression; // 推送频率
    private String pushChannel; // email, dingtalk, wecom
    private List<String> recipients = new ArrayList<>();
    private String recipientsJson; // DB持久化用JSON字符串
    private String format; // pdf, excel, image
    private String condition; // 条件推送表达式（可选）
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastPushAt;
}
