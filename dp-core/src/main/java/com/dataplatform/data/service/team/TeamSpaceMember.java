package com.dataplatform.data.service.team;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 团队空间成员
 */
@Data
public class TeamSpaceMember {
    private String spaceId;
    private String userId;
    private String role; // owner, admin, editor, viewer
    private LocalDateTime joinedAt;
}
