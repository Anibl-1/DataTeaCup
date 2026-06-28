package com.dataplatform.common.event;

import org.springframework.context.ApplicationEvent;

/**
 * 权限缓存失效事件
 * 
 * <p>当角色权限、角色菜单或用户角色发生变更时，发布此事件。
 * {@code StpInterfaceImpl} 监听此事件并清除对应的 Caffeine L1 缓存，
 * 确保权限变更立即生效，无需等待缓存自然过期。</p>
 * 
 * <p>使用事件机制彻底消除 dp-system ↔ dp-data 之间的循环依赖。</p>
 * 
 * @author dataplatform
 */
public class PermissionCacheInvalidateEvent extends ApplicationEvent {

    /**
     * null 表示清除所有用户缓存；非 null 表示只清除指定用户
     */
    private final Long userId;

    /**
     * 标记事件是否来自 Redis Pub/Sub（跨服务传播），用于防止重复广播
     */
    private final boolean fromRedis;

    /**
     * 清除所有用户权限缓存（角色权限/菜单变更时使用）
     */
    public PermissionCacheInvalidateEvent(Object source) {
        super(source);
        this.userId = null;
        this.fromRedis = false;
    }

    /**
     * 清除指定用户权限缓存（用户角色变更时使用）
     */
    public PermissionCacheInvalidateEvent(Object source, Long userId) {
        super(source);
        this.userId = userId;
        this.fromRedis = false;
    }

    /**
     * 带 Redis 来源标记的构造（由 RedisPermissionEventBridge 使用）
     */
    public PermissionCacheInvalidateEvent(Object source, Long userId, boolean fromRedis) {
        super(source);
        this.userId = userId;
        this.fromRedis = fromRedis;
    }

    /**
     * @return 目标用户ID，null 表示清除所有
     */
    public Long getUserId() {
        return userId;
    }

    public boolean isInvalidateAll() {
        return userId == null;
    }

    /**
     * @return 是否来自 Redis 跨服务传播
     */
    public boolean isFromRedis() {
        return fromRedis;
    }
}
