package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.MessageChannel;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 消息通道配置Mapper
 */
@Mapper
public interface MessageChannelMapper {

    @Select("SELECT * FROM sys_message_channel ORDER BY channel_type, is_default DESC, create_time DESC")
    List<MessageChannel> findAll();

    @Select("SELECT * FROM sys_message_channel WHERE id = #{id}")
    MessageChannel findById(@Param("id") Long id);

    @Select("SELECT * FROM sys_message_channel WHERE channel_type = #{channelType} AND status = 1 ORDER BY is_default DESC, create_time DESC")
    List<MessageChannel> findByType(@Param("channelType") String channelType);

    @Select("SELECT * FROM sys_message_channel WHERE channel_type = #{channelType} AND is_default = 1 AND status = 1 LIMIT 1")
    MessageChannel findDefaultByType(@Param("channelType") String channelType);

    @Select("SELECT * FROM sys_message_channel WHERE status = 1 ORDER BY channel_type, is_default DESC")
    List<MessageChannel> findEnabled();

    @Insert("INSERT INTO sys_message_channel (channel_name, channel_type, config, is_default, status, description, create_by, create_time, update_time) " +
            "VALUES (#{channelName}, #{channelType}, #{config}, #{isDefault}, #{status}, #{description}, #{createBy}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MessageChannel channel);

    @Update("UPDATE sys_message_channel SET channel_name=#{channelName}, config=#{config}, is_default=#{isDefault}, " +
            "status=#{status}, description=#{description}, update_time=NOW() WHERE id=#{id}")
    int update(MessageChannel channel);

    @Delete("DELETE FROM sys_message_channel WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Update("UPDATE sys_message_channel SET is_default = 0 WHERE channel_type = #{channelType} AND id != #{excludeId}")
    int clearDefaultByType(@Param("channelType") String channelType, @Param("excludeId") Long excludeId);

    @Select("SELECT COUNT(*) FROM sys_message_channel WHERE channel_type = #{channelType}")
    int countByType(@Param("channelType") String channelType);
}
