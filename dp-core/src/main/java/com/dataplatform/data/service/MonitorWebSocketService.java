package com.dataplatform.data.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 监控数据 WebSocket 推送服务
 * 通过 STOMP 广播实时监控指标到前端订阅者
 */
@Slf4j
@Service
public class MonitorWebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SystemMonitorService monitorService;

    /**
     * 广播系统健康数据到 /topic/monitor/health
     */
    public void broadcastHealth() {
        try {
            Map<String, Object> health = monitorService.getSystemHealth();
            messagingTemplate.convertAndSend("/topic/monitor/health", health);
        } catch (Exception e) {
            log.debug("[WebSocket] 广播健康数据异常: {}", e.getMessage());
        }
    }

    /**
     * 广播JVM指标到 /topic/monitor/jvm
     */
    public void broadcastJvmMetrics() {
        try {
            Map<String, Object> jvm = monitorService.getJvmMetrics();
            messagingTemplate.convertAndSend("/topic/monitor/jvm", jvm);
        } catch (Exception e) {
            log.debug("[WebSocket] 广播JVM指标异常: {}", e.getMessage());
        }
    }

    /**
     * 广播告警通知到 /topic/monitor/alert
     */
    public void broadcastAlert(Map<String, Object> alertData) {
        try {
            messagingTemplate.convertAndSend("/topic/monitor/alert", alertData);
            log.debug("[WebSocket] 广播告警: {}", alertData.get("message"));
        } catch (Exception e) {
            log.debug("[WebSocket] 广播告警异常: {}", e.getMessage());
        }
    }

    /**
     * 广播所有实时监控数据（健康 + JVM）
     */
    public void broadcastAll() {
        broadcastHealth();
        broadcastJvmMetrics();
    }
}
