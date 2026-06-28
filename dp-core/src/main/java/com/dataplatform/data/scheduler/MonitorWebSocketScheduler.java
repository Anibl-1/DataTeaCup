package com.dataplatform.data.scheduler;

import com.dataplatform.data.service.MonitorWebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * WebSocket 监控推送定时任务
 * 每10秒广播系统健康和JVM指标到WebSocket订阅者
 */
@Slf4j
@Component
public class MonitorWebSocketScheduler {

    @Autowired
    private MonitorWebSocketService webSocketService;

    @Value("${monitor.websocket.enabled:true}")
    private boolean enabled;

    /**
     * 每10秒推送实时监控数据
     */
    @Scheduled(fixedRate = 10000, initialDelay = 5000)
    public void pushMonitorData() {
        if (!enabled) {
            return;
        }
        try {
            webSocketService.broadcastAll();
        } catch (Exception e) {
            log.debug("[MonitorWS] 推送异常: {}", e.getMessage());
        }
    }
}
