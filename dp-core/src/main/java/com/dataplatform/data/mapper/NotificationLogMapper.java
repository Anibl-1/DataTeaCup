package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.NotificationLog;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface NotificationLogMapper {

    @Insert("INSERT INTO sys_notification_log (notification_type, channel, recipient, subject, content, status, error_message, retry_count, send_time, create_time) " +
            "VALUES (#{notificationType}, #{channel}, #{recipient}, #{subject}, #{content}, #{status}, #{errorMessage}, #{retryCount}, #{sendTime}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(NotificationLog log);

    @Update("UPDATE sys_notification_log SET status = #{status}, error_message = #{errorMessage}, retry_count = #{retryCount}, send_time = #{sendTime} WHERE id = #{id}")
    void update(NotificationLog log);

    @Select("SELECT * FROM sys_notification_log ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<NotificationLog> findByPage(@Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM sys_notification_log")
    long countAll();

    @Select("SELECT channel, status, COUNT(*) as cnt FROM sys_notification_log WHERE send_time >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) GROUP BY channel, status")
    List<Map<String, Object>> statsByChannelAndStatus(@Param("hours") int hours);

    @Select("SELECT channel, COUNT(*) as total, SUM(CASE WHEN status='success' THEN 1 ELSE 0 END) as success_count, SUM(CASE WHEN status='failed' THEN 1 ELSE 0 END) as failed_count FROM sys_notification_log WHERE send_time >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) GROUP BY channel")
    List<Map<String, Object>> statsByChannel(@Param("hours") int hours);

    @Delete("DELETE FROM sys_notification_log WHERE create_time < DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int cleanOldLogs(@Param("days") int days);
}
