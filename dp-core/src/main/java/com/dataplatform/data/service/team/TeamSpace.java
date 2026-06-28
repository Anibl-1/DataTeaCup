package com.dataplatform.data.service.team;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 团队空间
 * 需求: 22.1, 22.2
 */
@Data
public class TeamSpace {
    private String id;
    private String name;
    private String description;
    private String ownerId;
    private String visibility; // public, private
    private LocalDateTime createdAt;
}
