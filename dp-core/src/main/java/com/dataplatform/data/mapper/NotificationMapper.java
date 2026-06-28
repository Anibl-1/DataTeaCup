package com.dataplatform.data.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataplatform.data.entity.Notification;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知Mapper
 */
@Mapper
public interface NotificationMapper {

    @Select("<script>" +
            "SELECT * FROM sys_notification " +
            "<where>" +
            "  <if test='targetUserId != null'>" +
            "    AND target_user_id = #{targetUserId}" +
            "  </if>" +
            "  <if test='isRead != null'>" +
            "    AND is_read = #{isRead}" +
            "  </if>" +
            "  <if test='notificationType != null and notificationType != \"\"'>" +
            "    AND notification_type = #{notificationType}" +
            "  </if>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (title LIKE CONCAT('%', #{keyword}, '%') OR content LIKE CONCAT('%', #{keyword}, '%'))" +
            "  </if>" +
            "</where>" +
            " ORDER BY create_time DESC" +
            "</script>")
    IPage<Notification> selectPage(Page<Notification> page,
                                   @Param("targetUserId") Long targetUserId,
                                   @Param("isRead") Boolean isRead,
                                   @Param("notificationType") String notificationType,
                                   @Param("keyword") String keyword);

    @Select("SELECT * FROM sys_notification WHERE target_user_id = #{userId} AND is_read = false ORDER BY create_time DESC")
    List<Notification> selectUnreadByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM sys_notification WHERE target_user_id = #{userId} AND is_read = false")
    int countUnread(@Param("userId") Long userId);

    @Select("SELECT * FROM sys_notification WHERE id = #{id}")
    Notification selectById(@Param("id") Long id);

    @Insert("INSERT INTO sys_notification (title, content, notification_type, priority, target_user_id, sender_id, sender_name, dept_id, is_read, related_type, related_id, attachments, remark, create_time) " +
            "VALUES (#{title}, #{content}, #{notificationType}, #{priority}, #{targetUserId}, #{senderId}, #{senderName}, #{deptId}, #{isRead}, #{relatedType}, #{relatedId}, #{attachments}, #{remark}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Notification notification);

    @Update("UPDATE sys_notification SET is_read = true, read_time = #{readTime} WHERE id = #{id}")
    int markAsRead(@Param("id") Long id, @Param("readTime") LocalDateTime readTime);

    @Update("UPDATE sys_notification SET is_read = true, read_time = #{readTime} WHERE target_user_id = #{userId} AND is_read = false")
    int markAllAsRead(@Param("userId") Long userId, @Param("readTime") LocalDateTime readTime);

    @Delete("DELETE FROM sys_notification WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Delete("<script>" +
            "DELETE FROM sys_notification WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "  #{id}" +
            "</foreach>" +
            "</script>")
    int batchDelete(@Param("ids") List<Long> ids);
}
