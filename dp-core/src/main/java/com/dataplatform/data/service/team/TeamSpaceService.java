package com.dataplatform.data.service.team;

import com.dataplatform.data.mapper.TeamSpaceMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 团队空间服务 - 数据库持久化
 * 需求: 22.1, 22.2, 22.3, 22.4, 22.5, 22.7
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamSpaceService {

    private final TeamSpaceMapper teamSpaceMapper;

    public TeamSpace createSpace(TeamSpace space) {
        if (space.getId() == null) {
            space.setId(UUID.randomUUID().toString().substring(0, 12));
        }
        space.setCreatedAt(LocalDateTime.now());
        teamSpaceMapper.insertSpace(space);

        // 创建者自动成为owner
        TeamSpaceMember owner = new TeamSpaceMember();
        owner.setSpaceId(space.getId());
        owner.setUserId(space.getOwnerId());
        owner.setRole("owner");
        owner.setJoinedAt(LocalDateTime.now());
        teamSpaceMapper.insertMember(owner);

        recordActivity(space.getId(), space.getOwnerId(), "create_space", space.getName());
        return space;
    }

    public TeamSpace getSpace(String spaceId) {
        return teamSpaceMapper.selectSpaceById(spaceId);
    }

    public List<TeamSpace> listSpacesByUser(String userId) {
        return teamSpaceMapper.selectSpacesByUser(userId);
    }

    public void addMember(String spaceId, String userId, String role) {
        TeamSpaceMember member = new TeamSpaceMember();
        member.setSpaceId(spaceId);
        member.setUserId(userId);
        member.setRole(role);
        member.setJoinedAt(LocalDateTime.now());
        teamSpaceMapper.insertMember(member);
        recordActivity(spaceId, userId, "join_space", "角色: " + role);
    }

    public void removeMember(String spaceId, String userId) {
        teamSpaceMapper.deleteMember(spaceId, userId);
        recordActivity(spaceId, userId, "leave_space", null);
    }

    public List<TeamSpaceMember> getMembers(String spaceId) {
        return teamSpaceMapper.selectMembersBySpace(spaceId);
    }

    public void addResource(String spaceId, String resourceId, String userId) {
        teamSpaceMapper.insertResource(spaceId, resourceId);
        recordActivity(spaceId, userId, "add_resource", resourceId);
    }

    public void shareResourceToSpace(String fromSpaceId, String toSpaceId, String resourceId, String userId) {
        teamSpaceMapper.insertResource(toSpaceId, resourceId);
        recordActivity(toSpaceId, userId, "share_from_space", "来自空间: " + fromSpaceId);
    }

    public Set<String> getResources(String spaceId) {
        List<String> list = teamSpaceMapper.selectResourcesBySpace(spaceId);
        return new LinkedHashSet<>(list);
    }

    public void removeResource(String spaceId, String resourceId, String userId) {
        teamSpaceMapper.deleteResource(spaceId, resourceId);
        recordActivity(spaceId, userId, "remove_resource", resourceId);
    }

    public TeamSpace updateSpace(String spaceId, TeamSpace update) {
        TeamSpace space = teamSpaceMapper.selectSpaceById(spaceId);
        if (space == null) return null;
        if (update.getName() != null) space.setName(update.getName());
        if (update.getDescription() != null) space.setDescription(update.getDescription());
        if (update.getVisibility() != null) space.setVisibility(update.getVisibility());
        teamSpaceMapper.updateSpace(space);
        return space;
    }

    public void deleteSpace(String spaceId) {
        teamSpaceMapper.deleteMembersBySpace(spaceId);
        teamSpaceMapper.deleteActivitiesBySpace(spaceId);
        teamSpaceMapper.deleteResourcesBySpace(spaceId);
        teamSpaceMapper.deleteFilesBySpace(spaceId);
        teamSpaceMapper.deleteMessagesBySpace(spaceId);
        teamSpaceMapper.deleteSpace(spaceId);
    }

    public void updateMemberRole(String spaceId, String userId, String role) {
        teamSpaceMapper.updateMemberRole(spaceId, userId, role);
    }

    // ==================== 文件管理 ====================

    public TeamSpaceFile addFile(String spaceId, TeamSpaceFile file) {
        if (file.getId() == null) {
            file.setId(UUID.randomUUID().toString().substring(0, 12));
        }
        file.setSpaceId(spaceId);
        file.setUploadedAt(LocalDateTime.now());
        teamSpaceMapper.insertFile(file);
        recordActivity(spaceId, file.getUploadedBy(), "upload_file", file.getName());
        return file;
    }

    public List<TeamSpaceFile> getFiles(String spaceId) {
        return teamSpaceMapper.selectFilesBySpace(spaceId);
    }

    public TeamSpaceFile getFile(String spaceId, String fileId) {
        return teamSpaceMapper.selectFileById(spaceId, fileId);
    }

    public void deleteFile(String spaceId, String fileId, String userId) {
        teamSpaceMapper.deleteFile(spaceId, fileId);
        recordActivity(spaceId, userId, "delete_file", fileId);
    }

    // ==================== 聊天消息 ====================

    public TeamSpaceMessage addMessage(String spaceId, String senderId, String senderName, String content) {
        TeamSpaceMessage msg = new TeamSpaceMessage();
        msg.setId(UUID.randomUUID().toString().substring(0, 12));
        msg.setSpaceId(spaceId);
        msg.setSenderId(senderId);
        msg.setSenderName(senderName);
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        teamSpaceMapper.insertMessage(msg);
        return msg;
    }

    public List<TeamSpaceMessage> getMessages(String spaceId, int limit) {
        List<TeamSpaceMessage> messages = teamSpaceMapper.selectMessagesBySpace(spaceId, limit);
        // 数据库按 DESC 返回，翻转为时间正序
        Collections.reverse(messages);
        return messages;
    }

    public List<SpaceActivity> getActivities(String spaceId, int limit) {
        List<SpaceActivity> activities = teamSpaceMapper.selectActivitiesBySpace(spaceId, limit);
        Collections.reverse(activities);
        return activities;
    }

    private void recordActivity(String spaceId, String userId, String action, String detail) {
        SpaceActivity activity = new SpaceActivity();
        activity.setSpaceId(spaceId);
        activity.setUserId(userId);
        activity.setAction(action);
        activity.setDetail(detail);
        activity.setCreatedAt(LocalDateTime.now());
        try {
            teamSpaceMapper.insertActivity(activity);
        } catch (Exception e) {
            log.warn("记录团队空间活动失败: {}", e.getMessage());
        }
    }

    @Data
    public static class SpaceActivity {
        private Long id;
        private String spaceId;
        private String userId;
        private String action;
        private String detail;
        private LocalDateTime createdAt;
    }
}
