package com.dataplatform.data.service.share;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 分享链接
 * 需求: 21.1, 21.2, 21.3, 21.4, 21.5
 */
@Data
public class ShareLink {
    private String id;
    private String token; // 分享令牌
    private String resourceType; // dashboard, report, chart
    private String resourceId;
    private String resourceName;
    private String createdBy;
    private String accessType; // public, password, internal
    private String password; // 密码保护
    private LocalDateTime expireAt; // 过期时间
    private int maxAccessCount; // 最大访问次数，0=无限
    private int accessCount; // 已访问次数
    private boolean watermarkEnabled; // 是否启用水印
    private boolean embeddable; // 是否允许嵌入
    private boolean active; // 是否有效
    private LocalDateTime createdAt;
}
