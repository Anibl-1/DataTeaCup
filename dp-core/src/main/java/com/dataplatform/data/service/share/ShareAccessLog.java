package com.dataplatform.data.service.share;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 分享访问日志
 */
@Data
public class ShareAccessLog {
    private String id;
    private String shareId;
    private String accessIp;
    private String userAgent;
    private boolean success;
    private String failReason;
    private LocalDateTime accessTime;
}
