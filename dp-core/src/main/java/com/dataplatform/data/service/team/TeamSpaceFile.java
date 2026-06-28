package com.dataplatform.data.service.team;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 团队空间文件
 */
@Data
public class TeamSpaceFile {
    private String id;
    private String spaceId;
    private String name;
    private long size;
    private String contentType;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private String storagePath;
}
