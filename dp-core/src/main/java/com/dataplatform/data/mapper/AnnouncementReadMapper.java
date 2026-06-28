package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.AnnouncementRead;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告已读记录Mapper
 */
@Mapper
public interface AnnouncementReadMapper {

    @Select("SELECT COUNT(*) FROM sys_announcement_read WHERE announcement_id = #{announcementId} AND user_id = #{userId}")
    int countByAnnouncementAndUser(@Param("announcementId") Long announcementId, @Param("userId") Long userId);

    @Insert("INSERT INTO sys_announcement_read (announcement_id, user_id, read_time) VALUES (#{announcementId}, #{userId}, #{readTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AnnouncementRead record);

    @Select("SELECT COUNT(*) FROM sys_announcement_read WHERE announcement_id = #{announcementId}")
    int countByAnnouncement(@Param("announcementId") Long announcementId);

    @Select("SELECT user_id FROM sys_announcement_read WHERE announcement_id = #{announcementId}")
    List<Long> selectUserIdsByAnnouncement(@Param("announcementId") Long announcementId);
}
