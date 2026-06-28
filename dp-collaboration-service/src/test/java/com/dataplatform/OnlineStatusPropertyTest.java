package com.dataplatform;

import com.dataplatform.message.entity.ChatUserStatus;
import com.dataplatform.message.mapper.ChatUserStatusMapper;
import com.dataplatform.message.service.OnlineStatusManager;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 属性测试：在线状态更新
 *
 * Feature: mars-integration-optimization, Property 19: WebSocket 连接触发在线状态更新
 * **Validates: Requirements 13.1**
 */
class OnlineStatusPropertyTest {

    // ========== Helper methods ==========

    private OnlineStatusManager createManagerWithMock(ChatUserStatusMapper mapper) throws Exception {
        OnlineStatusManager manager = new OnlineStatusManager();

        Field mapperField = OnlineStatusManager.class.getDeclaredField("statusMapper");
        mapperField.setAccessible(true);
        mapperField.set(manager, mapper);

        return manager;
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<Long, ScheduledFuture<?>> getPendingOffline(OnlineStatusManager manager) throws Exception {
        Field field = OnlineStatusManager.class.getDeclaredField("pendingOffline");
        field.setAccessible(true);
        return (ConcurrentHashMap<Long, ScheduledFuture<?>>) field.get(manager);
    }

    // ========== Property 19: WebSocket 连接触发在线状态更新 ==========

    /**
     * For any user establishing a WebSocket connection (calling userOnline),
     * the mapper's insertOrUpdate should be called with status="online"
     * and a non-null lastActiveTime.
     *
     * Feature: mars-integration-optimization, Property 19: WebSocket 连接触发在线状态更新
     * **Validates: Requirements 13.1**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_19_WebSocket连接触发在线状态更新")
    void userOnline_setsStatusToOnlineWithLastActiveTime(
            @ForAll @LongRange(min = 1, max = 10000) Long userId
    ) throws Exception {
        ChatUserStatusMapper mapper = Mockito.mock(ChatUserStatusMapper.class);
        when(mapper.insertOrUpdate(any(ChatUserStatus.class))).thenReturn(1);

        OnlineStatusManager manager = createManagerWithMock(mapper);

        LocalDateTime before = LocalDateTime.now();
        manager.userOnline(userId);
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<ChatUserStatus> captor = ArgumentCaptor.forClass(ChatUserStatus.class);
        verify(mapper, times(1)).insertOrUpdate(captor.capture());

        ChatUserStatus captured = captor.getValue();
        assertThat(captured.getUserId()).isEqualTo(userId);
        assertThat(captured.getStatus()).isEqualTo("online");
        assertThat(captured.getLastActiveTime()).isNotNull();
        assertThat(captured.getLastActiveTime()).isAfterOrEqualTo(before);
        assertThat(captured.getLastActiveTime()).isBeforeOrEqualTo(after);
    }

    /**
     * For any user who goes offline and then quickly comes back online,
     * the pending offline task should be cancelled when userOnline is called.
     *
     * Feature: mars-integration-optimization, Property 19: WebSocket 连接触发在线状态更新
     * **Validates: Requirements 13.1**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_19_WebSocket连接触发在线状态更新")
    void userOnline_cancelsPendingOfflineTask(
            @ForAll @LongRange(min = 1, max = 10000) Long userId
    ) throws Exception {
        ChatUserStatusMapper mapper = Mockito.mock(ChatUserStatusMapper.class);
        when(mapper.insertOrUpdate(any(ChatUserStatus.class))).thenReturn(1);

        OnlineStatusManager manager = createManagerWithMock(mapper);

        // Simulate going offline first (schedules a 5-second delayed offline task)
        manager.userOffline(userId);

        // Verify there is a pending offline task
        ConcurrentHashMap<Long, ScheduledFuture<?>> pendingOffline = getPendingOffline(manager);
        assertThat(pendingOffline).containsKey(userId);
        ScheduledFuture<?> pendingTask = pendingOffline.get(userId);
        assertThat(pendingTask.isCancelled()).isFalse();

        // Now come back online — should cancel the pending offline task
        manager.userOnline(userId);

        // The pending offline map should no longer contain this user
        assertThat(pendingOffline).doesNotContainKey(userId);
        // The previously scheduled task should be cancelled
        assertThat(pendingTask.isCancelled()).isTrue();

        // insertOrUpdate should have been called for the online status
        verify(mapper, times(1)).insertOrUpdate(any(ChatUserStatus.class));

        // Cleanup
        manager.shutdown();
    }

    /**
     * getOnlineStatus with an empty list should return an empty list.
     *
     * Feature: mars-integration-optimization, Property 19: WebSocket 连接触发在线状态更新
     * **Validates: Requirements 13.1**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_19_WebSocket连接触发在线状态更新")
    void getOnlineStatus_emptyList_returnsEmptyList(
            @ForAll @LongRange(min = 1, max = 10000) Long ignoredUserId
    ) throws Exception {
        ChatUserStatusMapper mapper = Mockito.mock(ChatUserStatusMapper.class);
        OnlineStatusManager manager = createManagerWithMock(mapper);

        List<ChatUserStatus> result = manager.getOnlineStatus(Collections.emptyList());

        assertThat(result).isEmpty();
        // Mapper should NOT be called for empty input
        verify(mapper, never()).selectByUserIds(any());
    }

    /**
     * getOnlineStatus delegates to the mapper correctly for non-empty lists.
     *
     * Feature: mars-integration-optimization, Property 19: WebSocket 连接触发在线状态更新
     * **Validates: Requirements 13.1**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_19_WebSocket连接触发在线状态更新")
    void getOnlineStatus_delegatesToMapper(
            @ForAll @LongRange(min = 1, max = 10000) Long userId
    ) throws Exception {
        ChatUserStatusMapper mapper = Mockito.mock(ChatUserStatusMapper.class);

        ChatUserStatus expectedStatus = new ChatUserStatus();
        expectedStatus.setUserId(userId);
        expectedStatus.setStatus("online");
        expectedStatus.setLastActiveTime(LocalDateTime.now());

        List<Long> userIds = List.of(userId);
        when(mapper.selectByUserIds(userIds)).thenReturn(List.of(expectedStatus));

        OnlineStatusManager manager = createManagerWithMock(mapper);

        List<ChatUserStatus> result = manager.getOnlineStatus(userIds);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(userId);
        assertThat(result.get(0).getStatus()).isEqualTo("online");
        verify(mapper, times(1)).selectByUserIds(userIds);
    }
}
