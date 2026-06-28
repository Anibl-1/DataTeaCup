package com.dataplatform.data.mapper;

import com.dataplatform.data.service.team.*;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TeamSpaceMapper {

    // ==================== 空间 ====================

    @Insert("INSERT INTO sys_team_space (id, name, description, owner_id, visibility, created_at) " +
            "VALUES (#{id}, #{name}, #{description}, #{ownerId}, #{visibility}, #{createdAt})")
    int insertSpace(TeamSpace space);

    @Select("SELECT * FROM sys_team_space WHERE id = #{id}")
    TeamSpace selectSpaceById(@Param("id") String id);

    @Update("UPDATE sys_team_space SET name = #{name}, description = #{description}, visibility = #{visibility} WHERE id = #{id}")
    int updateSpace(TeamSpace space);

    @Delete("DELETE FROM sys_team_space WHERE id = #{id}")
    int deleteSpace(@Param("id") String id);

    @Select("SELECT s.* FROM sys_team_space s " +
            "INNER JOIN sys_team_space_member m ON s.id = m.space_id " +
            "WHERE m.user_id = #{userId} ORDER BY s.created_at DESC")
    List<TeamSpace> selectSpacesByUser(@Param("userId") String userId);

    // ==================== 成员 ====================

    @Insert("INSERT INTO sys_team_space_member (space_id, user_id, role, joined_at) " +
            "VALUES (#{spaceId}, #{userId}, #{role}, #{joinedAt})")
    int insertMember(TeamSpaceMember member);

    @Select("SELECT * FROM sys_team_space_member WHERE space_id = #{spaceId}")
    List<TeamSpaceMember> selectMembersBySpace(@Param("spaceId") String spaceId);

    @Update("UPDATE sys_team_space_member SET role = #{role} WHERE space_id = #{spaceId} AND user_id = #{userId}")
    int updateMemberRole(@Param("spaceId") String spaceId, @Param("userId") String userId, @Param("role") String role);

    @Delete("DELETE FROM sys_team_space_member WHERE space_id = #{spaceId} AND user_id = #{userId}")
    int deleteMember(@Param("spaceId") String spaceId, @Param("userId") String userId);

    @Delete("DELETE FROM sys_team_space_member WHERE space_id = #{spaceId}")
    int deleteMembersBySpace(@Param("spaceId") String spaceId);

    // ==================== 文件 ====================

    @Insert("INSERT INTO sys_team_space_file (id, space_id, name, size, content_type, uploaded_by, storage_path, uploaded_at) " +
            "VALUES (#{id}, #{spaceId}, #{name}, #{size}, #{contentType}, #{uploadedBy}, #{storagePath}, #{uploadedAt})")
    int insertFile(TeamSpaceFile file);

    @Select("SELECT * FROM sys_team_space_file WHERE space_id = #{spaceId} ORDER BY uploaded_at DESC")
    List<TeamSpaceFile> selectFilesBySpace(@Param("spaceId") String spaceId);

    @Select("SELECT * FROM sys_team_space_file WHERE id = #{id} AND space_id = #{spaceId}")
    TeamSpaceFile selectFileById(@Param("spaceId") String spaceId, @Param("id") String id);

    @Delete("DELETE FROM sys_team_space_file WHERE id = #{id} AND space_id = #{spaceId}")
    int deleteFile(@Param("spaceId") String spaceId, @Param("id") String id);

    @Delete("DELETE FROM sys_team_space_file WHERE space_id = #{spaceId}")
    int deleteFilesBySpace(@Param("spaceId") String spaceId);

    // ==================== 消息 ====================

    @Insert("INSERT INTO sys_team_space_message (id, space_id, sender_id, sender_name, content, created_at) " +
            "VALUES (#{id}, #{spaceId}, #{senderId}, #{senderName}, #{content}, #{createdAt})")
    int insertMessage(TeamSpaceMessage message);

    @Select("SELECT * FROM sys_team_space_message WHERE space_id = #{spaceId} ORDER BY created_at DESC LIMIT #{limit}")
    List<TeamSpaceMessage> selectMessagesBySpace(@Param("spaceId") String spaceId, @Param("limit") int limit);

    @Delete("DELETE FROM sys_team_space_message WHERE space_id = #{spaceId}")
    int deleteMessagesBySpace(@Param("spaceId") String spaceId);

    // ==================== 活动 ====================

    @Insert("INSERT INTO sys_team_space_activity (space_id, user_id, action, detail, created_at) " +
            "VALUES (#{spaceId}, #{userId}, #{action}, #{detail}, #{createdAt})")
    int insertActivity(TeamSpaceService.SpaceActivity activity);

    @Select("SELECT * FROM sys_team_space_activity WHERE space_id = #{spaceId} ORDER BY created_at DESC LIMIT #{limit}")
    List<TeamSpaceService.SpaceActivity> selectActivitiesBySpace(@Param("spaceId") String spaceId, @Param("limit") int limit);

    @Delete("DELETE FROM sys_team_space_activity WHERE space_id = #{spaceId}")
    int deleteActivitiesBySpace(@Param("spaceId") String spaceId);

    // ==================== 资源 ====================

    @Insert("INSERT IGNORE INTO sys_team_space_resource (space_id, resource_id) VALUES (#{spaceId}, #{resourceId})")
    int insertResource(@Param("spaceId") String spaceId, @Param("resourceId") String resourceId);

    @Select("SELECT resource_id FROM sys_team_space_resource WHERE space_id = #{spaceId}")
    List<String> selectResourcesBySpace(@Param("spaceId") String spaceId);

    @Delete("DELETE FROM sys_team_space_resource WHERE space_id = #{spaceId} AND resource_id = #{resourceId}")
    int deleteResource(@Param("spaceId") String spaceId, @Param("resourceId") String resourceId);

    @Delete("DELETE FROM sys_team_space_resource WHERE space_id = #{spaceId}")
    int deleteResourcesBySpace(@Param("spaceId") String spaceId);
}
