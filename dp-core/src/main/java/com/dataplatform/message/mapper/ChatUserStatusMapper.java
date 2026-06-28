package com.dataplatform.message.mapper;

import com.dataplatform.message.entity.ChatUserStatus;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 用户在线状态 Mapper 接口
 */
public interface ChatUserStatusMapper {

    ChatUserStatus selectByUserId(@Param("userId") Long userId);

    List<ChatUserStatus> selectByUserIds(@Param("userIds") List<Long> userIds);

    int insertOrUpdate(ChatUserStatus status);

    int updateStatus(@Param("userId") Long userId, @Param("status") String status);

    List<ChatUserStatus> selectAllOnline();
}
