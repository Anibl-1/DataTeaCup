package com.dataplatform.data.service.alert;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警升级服务
 * 未处理的告警自动升级通知更高级别的人员
 * 需求: 15.6
 */
@Slf4j
@Service
public class AlertEscalationService {

    /** 升级策略 key=level */
    private final Map<String, EscalationPolicy> policies = new ConcurrentHashMap<>();

    /** 待升级告警追踪 key=alertId */
    private final Map<String, PendingAlert> pendingAlerts = new ConcurrentHashMap<>();

    public AlertEscalationService() {
        // 默认升级策略
        policies.put("warning", new EscalationPolicy("warning", 30, "critical", List.of("team-lead")));
        policies.put("critical", new EscalationPolicy("critical", 15, "emergency", List.of("manager", "oncall")));
        policies.put("emergency", new EscalationPolicy("emergency", 5, "emergency", List.of("director", "cto")));
    }

    /**
     * 注册待升级告警
     */
    public void trackAlert(String alertId, String level) {
        PendingAlert pending = new PendingAlert();
        pending.setAlertId(alertId);
        pending.setOriginalLevel(level);
        pending.setCurrentLevel(level);
        pending.setCreatedAt(LocalDateTime.now());
        pending.setEscalationCount(0);
        pendingAlerts.put(alertId, pending);
    }

    /**
     * 标记告警已处理（停止升级）
     */
    public void resolveAlert(String alertId) {
        pendingAlerts.remove(alertId);
    }

    /**
     * 检查并执行升级（定期调用）
     *
     * @return 需要升级的告警列表
     */
    public List<EscalationAction> checkEscalations() {
        List<EscalationAction> actions = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (PendingAlert pending : pendingAlerts.values()) {
            EscalationPolicy policy = policies.get(pending.getCurrentLevel());
            if (policy == null) continue;

            LocalDateTime escalateAt = pending.getCreatedAt().plusMinutes(
                    (long) policy.getEscalateAfterMinutes() * (pending.getEscalationCount() + 1));

            if (now.isAfter(escalateAt)) {
                EscalationAction action = new EscalationAction();
                action.setAlertId(pending.getAlertId());
                action.setFromLevel(pending.getCurrentLevel());
                action.setToLevel(policy.getEscalateToLevel());
                action.setNotifyTargets(policy.getNotifyTargets());
                actions.add(action);

                pending.setCurrentLevel(policy.getEscalateToLevel());
                pending.setEscalationCount(pending.getEscalationCount() + 1);
                log.warn("告警升级: alertId={}, {}→{}, 通知: {}",
                        pending.getAlertId(), action.getFromLevel(), action.getToLevel(), policy.getNotifyTargets());
            }
        }
        return actions;
    }

    /**
     * 更新升级策略
     */
    public void updatePolicy(String level, int escalateAfterMinutes, String escalateToLevel, List<String> notifyTargets) {
        policies.put(level, new EscalationPolicy(level, escalateAfterMinutes, escalateToLevel, notifyTargets));
    }

    /**
     * 获取所有策略
     */
    public List<EscalationPolicy> listPolicies() {
        return new ArrayList<>(policies.values());
    }

    /**
     * 获取待升级告警数
     */
    public int getPendingCount() {
        return pendingAlerts.size();
    }

    @Data
    public static class EscalationPolicy {
        private String level;
        private int escalateAfterMinutes;
        private String escalateToLevel;
        private List<String> notifyTargets;

        public EscalationPolicy(String level, int minutes, String toLevel, List<String> targets) {
            this.level = level;
            this.escalateAfterMinutes = minutes;
            this.escalateToLevel = toLevel;
            this.notifyTargets = targets;
        }
    }

    @Data
    public static class PendingAlert {
        private String alertId;
        private String originalLevel;
        private String currentLevel;
        private LocalDateTime createdAt;
        private int escalationCount;
    }

    @Data
    public static class EscalationAction {
        private String alertId;
        private String fromLevel;
        private String toLevel;
        private List<String> notifyTargets;
    }
}
