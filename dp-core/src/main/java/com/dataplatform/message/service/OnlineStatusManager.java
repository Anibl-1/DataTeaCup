package com.dataplatform.message.service;

import com.dataplatform.message.entity.ChatUserStatus;
import com.dataplatform.message.mapper.ChatUserStatusMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * 在线状态管理器
 * <p>
 * 负责用户上线/下线状态管理，支持 5 秒宽限期离线和批量状态查询。
 * DB 操作通过 ChatUserStatusMapper 完成，Redis 缓存可在 Controller 层按需添加。
 */
@Service
public class OnlineStatusManager {

    @Autowired
    private ChatUserStatusMapper statusMapper;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> pendingOffline = new ConcurrentHashMap<>();

    /**
     * 用户上线
     * <p>
     * 取消任何待执行的离线任务，更新数据库状态为 online。
     *
     * @param userId 用户 ID
     */
    public void userOnline(Long userId) {
        // Cancel any pending offline task for this user (reconnect within grace period)
        ScheduledFuture<?> pending = pendingOffline.remove(userId);
        if (pending != null) {
            pending.cancel(false);
        }

        // Update status to online in DB
        ChatUserStatus status = new ChatUserStatus();
        status.setUserId(userId);
        status.setStatus("online");
        status.setLastActiveTime(LocalDateTime.now());
        statusMapper.insertOrUpdate(status);
    }

    /**
     * 用户下线（5 秒宽限期）
     * <p>
     * 延迟 5 秒后将用户标记为离线，期间若用户重新连接则取消离线操作。
     *
     * @param userId 用户 ID
     */
    public void userOffline(Long userId) {
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            pendingOffline.remove(userId);
            statusMapper.updateStatus(userId, "offline");
        }, 5, TimeUnit.SECONDS);

        // Store the pending task; cancel previous if exists
        ScheduledFuture<?> prev = pendingOffline.put(userId, future);
        if (prev != null) {
            prev.cancel(false);
        }
    }

    /**
     * 批量查询用户在线状态
     *
     * @param userIds 用户 ID 列表
     * @return 用户状态列表
     */
    public List<ChatUserStatus> getOnlineStatus(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return statusMapper.selectByUserIds(userIds);
    }

    /**
     * 查询所有在线用户
     *
     * @return 在线用户状态列表
     */
    public List<ChatUserStatus> getAllOnlineUsers() {
        return statusMapper.selectAllOnline();
    }

    /**
     * 查询单个用户是否在线
     *
     * @param userId 用户 ID
     * @return true 表示在线
     */
    public boolean isOnline(Long userId) {
        ChatUserStatus status = statusMapper.selectByUserId(userId);
        return status != null && "online".equals(status.getStatus());
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
