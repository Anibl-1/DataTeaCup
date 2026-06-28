package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.NotificationTemplate;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 通知模板Mapper
 */
@Mapper
public interface NotificationTemplateMapper {

    @Select("SELECT * FROM sys_notification_template ORDER BY create_time DESC")
    List<NotificationTemplate> findAll();

    @Select("SELECT * FROM sys_notification_template WHERE id = #{id}")
    NotificationTemplate findById(@Param("id") Long id);

    @Select("SELECT * FROM sys_notification_template WHERE template_code = #{code} AND is_enabled = 1")
    NotificationTemplate findByCode(@Param("code") String code);

    @Select("<script>" +
            "SELECT * FROM sys_notification_template WHERE 1=1" +
            "<if test='channel != null'> AND (channel = #{channel} OR channel = 'all')</if>" +
            "<if test='notificationType != null'> AND notification_type = #{notificationType}</if>" +
            " ORDER BY create_time DESC" +
            "</script>")
    List<NotificationTemplate> search(@Param("channel") String channel,
                                       @Param("notificationType") String notificationType);

    @Insert("INSERT INTO sys_notification_template (template_code, template_name, channel, notification_type, " +
            "subject, content, variables, is_enabled, create_time, update_time) VALUES " +
            "(#{templateCode}, #{templateName}, #{channel}, #{notificationType}, " +
            "#{subject}, #{content}, #{variables}, #{isEnabled}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NotificationTemplate template);

    @Update("UPDATE sys_notification_template SET template_name=#{templateName}, channel=#{channel}, " +
            "notification_type=#{notificationType}, subject=#{subject}, content=#{content}, " +
            "variables=#{variables}, is_enabled=#{isEnabled}, update_time=NOW() WHERE id=#{id}")
    int update(NotificationTemplate template);

    @Delete("DELETE FROM sys_notification_template WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
