package com.dataplatform.data.service.version;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 资源版本
 * 需求: 24.1
 */
@Data
public class ResourceVersion {
    private String id;
    private String resourceType; // dashboard, report, chart
    private String resourceId;
    private int versionNumber;
    private String content; // JSON序列化的资源内容
    private String changeDescription;
    private String createdBy;
    private LocalDateTime createdAt;
}
