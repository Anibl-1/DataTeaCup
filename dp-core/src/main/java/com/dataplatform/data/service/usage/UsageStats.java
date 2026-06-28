package com.dataplatform.data.service.usage;

import java.time.LocalDateTime;

/**
 * 使用统计实体类
 * 需求: 30.1, 30.2, 30.3, 30.4, 30.5
 */
public class UsageStats {
    private Long id;
    private String userId;
    private String featureCode;       // 功能代码
    private String action;            // 操作类型: view, query, export, create, edit, delete
    private String resourceType;      // 资源类型: dashboard, report, datasource
    private String resourceId;        // 资源ID
    private Long duration;            // 操作耗时(毫秒)
    private Integer resultCount;      // 查询结果行数
    private String errorCode;         // 错误码(如有)
    private String clientType;        // 客户端类型: web, mobile, api
    private String clientIp;
    private LocalDateTime createTime;

    public UsageStats() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFeatureCode() { return featureCode; }
    public void setFeatureCode(String featureCode) { this.featureCode = featureCode; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }

    public Integer getResultCount() { return resultCount; }
    public void setResultCount(Integer resultCount) { this.resultCount = resultCount; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getClientType() { return clientType; }
    public void setClientType(String clientType) { this.clientType = clientType; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
