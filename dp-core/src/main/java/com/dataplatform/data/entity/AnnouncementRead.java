package com.dataplatform.data.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 公告已读记录实体
 */
@Data
public class AnnouncementRead {
    private Long id;
    /** 公告ID */
    private Long announcementId;
    /** 用户ID */
    private Long userId;
    /** 阅读时间 */
    private LocalDateTime readTime;
}
