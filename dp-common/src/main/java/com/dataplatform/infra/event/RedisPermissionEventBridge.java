package com.dataplatform.infra.event;

import com.dataplatform.common.event.PermissionCacheInvalidateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

/**
 * Redis Pub/Sub 权限缓存失效桥接器
 *
 * <p>解决微服务模式下 Spring ApplicationEvent 只能在单 JVM 内传播的问题。</p>
 *
 * <h3>工作流程：</h3>
 * <ol>
 *   <li>system-service 中 RoleService/UserService 发布 {@link PermissionCacheInvalidateEvent}</li>
 *   <li>本类监听该事件，转发到 Redis channel {@code dp:event:permission-invalidate}</li>
 *   <li>所有微服务订阅该 channel，收到消息后在本地重新发布 Spring Event</li>
 *   <li>StpInterfaceImpl 的 @TransactionalEventListener 照常处理本地事件</li>
 * </ol>
 *
 * <p>使用 {@code sourceInstanceId} 去重，避免发布者自己重复消费。</p>
 *
 * @author dataplatform
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true")
public class RedisPermissionEventBridge implements MessageListener {

    private static final String CHANNEL = "dp:event:permission-invalidate";
    private static final String SEPARATOR = "|";

    /** 本实例唯一标识，用于去重（避免自己发布的消息自己又消费） */
    private final String instanceId = java.util.UUID.randomUUID().toString().substring(0, 8);

    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer listenerContainer;
    private final ApplicationEventPublisher eventPublisher;

    public RedisPermissionEventBridge(StringRedisTemplate redisTemplate,
                                       RedisMessageListenerContainer listenerContainer,
                                       ApplicationEventPublisher eventPublisher) {
        this.redisTemplate = redisTemplate;
        this.listenerContainer = listenerContainer;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void init() {
        listenerContainer.addMessageListener(this, new ChannelTopic(CHANNEL));
        log.info("Redis 权限事件桥接器已启动 [instanceId={}, channel={}]", instanceId, CHANNEL);
    }

    // ========================= 发布端：Spring Event → Redis =========================

    /**
     * 捕获本地 PermissionCacheInvalidateEvent，转发到 Redis channel
     */
    @EventListener
    public void onLocalEvent(PermissionCacheInvalidateEvent event) {
        // 跳过来自 Redis 的事件，避免无限循环: Redis→本地Event→Redis→...
        if (event.isFromRedis()) {
            return;
        }
        try {
            // 消息格式: instanceId|userId  (userId 为空则用 "ALL")
            String payload = instanceId + SEPARATOR
                    + (event.isInvalidateAll() ? "ALL" : String.valueOf(event.getUserId()));
            redisTemplate.convertAndSend(CHANNEL, payload);
            log.debug("权限失效事件已发送到 Redis [payload={}]", payload);
        } catch (Exception e) {
            log.warn("发送权限失效事件到 Redis 失败: {}", e.getMessage());
            // 本地事件仍然会被 StpInterfaceImpl 的 @TransactionalEventListener 处理，不影响本 JVM
        }
    }

    // ========================= 订阅端：Redis → Spring Event =========================

    /**
     * 收到 Redis channel 消息，在本地重新发布 Spring Event（跳过自己发布的消息）
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            // 去掉 Redis 序列化可能带的引号
            payload = payload.replace("\"", "");

            String[] parts = payload.split("\\" + SEPARATOR, 2);
            if (parts.length < 2) {
                log.warn("收到格式错误的权限失效消息: {}", payload);
                return;
            }

            String sourceId = parts[0];
            String userIdStr = parts[1];

            // 跳过自己发布的消息（本 JVM 的 @TransactionalEventListener 已经处理过了）
            if (instanceId.equals(sourceId)) {
                log.debug("跳过自身发布的权限失效消息 [instanceId={}]", instanceId);
                return;
            }

            log.info("收到跨服务权限失效事件 [source={}, target={}]", sourceId, userIdStr);

            // 在本地重新发布 Spring Event（标记 fromRedis=true），触发 StpInterfaceImpl 清缓存
            if ("ALL".equals(userIdStr)) {
                eventPublisher.publishEvent(new PermissionCacheInvalidateEvent(this, null, true));
            } else {
                Long userId = Long.parseLong(userIdStr);
                eventPublisher.publishEvent(new PermissionCacheInvalidateEvent(this, userId, true));
            }
        } catch (Exception e) {
            log.error("处理 Redis 权限失效消息异常", e);
        }
    }
}
