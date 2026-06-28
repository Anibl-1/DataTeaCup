package com.dataplatform.data.service;

import com.dataplatform.data.entity.AlertRecord;
import com.dataplatform.data.entity.AlertRule;
import com.dataplatform.data.mapper.AlertRecordMapper;
import com.dataplatform.data.mapper.AlertRuleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警服务
 * 管理告警规则CRUD、规则引擎匹配、告警记录管理
 */
@Slf4j
@Service
public class AlertService {

    @Autowired
    private AlertRuleMapper alertRuleMapper;

    @Autowired
    private AlertRecordMapper alertRecordMapper;

    @Autowired(required = false)
    private AlertNotificationService alertNotificationService;

    @Autowired(required = false)
    private NotificationChannelRouter channelRouter;

    @Autowired(required = false)
    private MonitorWebSocketService webSocketService;

    /** 告警冷却Map: ruleId -> lastAlertTimeMs */
    private final Map<Long, Long> cooldownMap = new ConcurrentHashMap<>();

    /** 冷却时间（毫秒），默认5分钟 */
    private static final long COOLDOWN_MS = 5 * 60 * 1000L;

    // ==================== 规则 CRUD ====================

    public List<AlertRule> listRules() {
        return alertRuleMapper.findAll();
    }

    public AlertRule getRuleById(Long id) {
        return alertRuleMapper.findById(id);
    }

    public void createRule(AlertRule rule) {
        if (rule.getIsEnabled() == null) {
            rule.setIsEnabled(1);
        }
        alertRuleMapper.insert(rule);
        log.info("创建告警规则: {} ({})", rule.getRuleName(), rule.getId());
    }

    public void updateRule(AlertRule rule) {
        alertRuleMapper.update(rule);
        log.info("更新告警规则: {} ({})", rule.getRuleName(), rule.getId());
    }

    public void deleteRule(Long id) {
        alertRuleMapper.deleteById(id);
        cooldownMap.remove(id);
        log.info("删除告警规则: {}", id);
    }

    public void toggleRule(Long id) {
        AlertRule rule = alertRuleMapper.findById(id);
        if (rule != null) {
            int newStatus = rule.getIsEnabled() == 1 ? 0 : 1;
            alertRuleMapper.toggleEnabled(id, newStatus);
            log.info("切换告警规则 {} 状态: {}", id, newStatus == 1 ? "启用" : "禁用");
        }
    }

    // ==================== 规则引擎 ====================

    /**
     * 检查指标值是否触发告警规则
     * @param metricType 指标类型
     * @param metricValue 当前指标值
     */
    public void checkMetric(String metricType, double metricValue) {
        List<AlertRule> rules = alertRuleMapper.findByMetricType(metricType);
        for (AlertRule rule : rules) {
            if (isTriggered(rule, metricValue)) {
                fireAlert(rule, metricValue);
            }
        }
    }

    /**
     * 批量检查所有指标
     */
    public void checkAllMetrics(Map<String, Double> metrics) {
        for (Map.Entry<String, Double> entry : metrics.entrySet()) {
            checkMetric(entry.getKey(), entry.getValue());
        }
    }

    private boolean isTriggered(AlertRule rule, double value) {
        if (rule.getThresholdValue() == null) return false;
        double threshold = rule.getThresholdValue().doubleValue();
        String type = rule.getThresholdType();
        if (type == null) type = "gt";

        return switch (type) {
            case "gt" -> value > threshold;
            case "gte" -> value >= threshold;
            case "lt" -> value < threshold;
            case "lte" -> value <= threshold;
            case "eq" -> Math.abs(value - threshold) < 0.001;
            default -> value > threshold;
        };
    }

    private void fireAlert(AlertRule rule, double metricValue) {
        // 冷却检查
        Long lastAlert = cooldownMap.get(rule.getId());
        long now = System.currentTimeMillis();
        if (lastAlert != null && (now - lastAlert) < COOLDOWN_MS) {
            return;
        }

        // 创建告警记录
        AlertRecord record = new AlertRecord();
        record.setRuleId(rule.getId());
        record.setRuleName(rule.getRuleName());
        record.setMetricType(rule.getMetricType());
        record.setMetricName(rule.getMetricName());
        record.setMetricValue(BigDecimal.valueOf(metricValue));
        record.setThresholdValue(rule.getThresholdValue());
        record.setAlertLevel(rule.getAlertLevel());

        String message = rule.getAlertMessage();
        if (message != null) {
            message = message.replace("{value}", String.format("%.1f", metricValue))
                    .replace("{threshold}", rule.getThresholdValue().toPlainString());
        } else {
            message = String.format("%s 告警: 当前值 %.1f 超过阈值 %s",
                    rule.getMetricName(), metricValue, rule.getThresholdValue().toPlainString());
        }
        record.setAlertMessage(message);
        record.setAlertTime(new Date());
        record.setIsResolved(0);

        // 通过统一路由器发送多渠道通知
        boolean notified = false;
        String channels = rule.getNotificationChannels();
        if (channels != null && !channels.isEmpty() && channelRouter != null) {
            try {
                Map<String, String> ctx = new HashMap<>();
                ctx.put("alertLevel", rule.getAlertLevel());
                ctx.put("metricName", rule.getMetricName());
                ctx.put("currentValue", String.format("%.1f", metricValue));
                ctx.put("threshold", rule.getThresholdValue().toPlainString());
                ctx.put("timestamp", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

                String notifyUsers = rule.getNotificationUsers();
                channelRouter.route(channels, "alert", rule.getRuleName(), message,
                        notifyUsers != null ? notifyUsers : "", ctx);
                notified = true;
            } catch (Exception e) {
                log.error("发送告警通知失败: {}", e.getMessage());
            }
        } else if (alertNotificationService != null) {
            // 降级：无路由器时使用旧逻辑
            try {
                alertNotificationService.sendResourceAlert(
                        rule.getMetricName(), metricValue, rule.getThresholdValue().doubleValue());
                notified = true;
            } catch (Exception e) {
                log.error("发送告警通知失败(降级): {}", e.getMessage());
            }
        }
        record.setIsNotified(notified ? 1 : 0);
        if (notified) {
            record.setNotificationTime(new Date());
        }

        // WebSocket 实时推送告警
        if (webSocketService != null) {
            try {
                Map<String, Object> alertData = new HashMap<>();
                alertData.put("ruleName", rule.getRuleName());
                alertData.put("level", rule.getAlertLevel());
                alertData.put("message", message);
                alertData.put("metricValue", metricValue);
                alertData.put("timestamp", System.currentTimeMillis());
                webSocketService.broadcastAlert(alertData);
            } catch (Exception e) {
                log.debug("WebSocket告警推送失败: {}", e.getMessage());
            }
        }

        alertRecordMapper.insert(record);
        cooldownMap.put(rule.getId(), now);
        log.warn("触发告警: {} - {}", rule.getRuleName(), message);
    }

    // ==================== 告警记录 ====================

    public Map<String, Object> listRecords(int page, int size) {
        int offset = (page - 1) * size;
        List<AlertRecord> records = alertRecordMapper.findByPage(offset, size);
        long total = alertRecordMapper.countAll();
        Map<String, Object> result = new HashMap<>();
        result.put("list", records);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    public void resolveRecord(Long id, Long resolveBy, String resolveNote) {
        alertRecordMapper.resolve(id, new Date(), resolveBy, resolveNote);
        log.info("解决告警记录: {}", id);
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUnresolved", alertRecordMapper.countUnresolved());
        stats.put("totalAll", alertRecordMapper.countAll());
        stats.put("byLevel", alertRecordMapper.countByLevel());
        stats.put("enabledRules", alertRuleMapper.findEnabled().size());
        stats.put("totalRules", alertRuleMapper.findAll().size());
        return stats;
    }

    /**
     * 创建告警记录
     * 
     * @param title 告警标题
     * @param message 告警消息
     * @param level 告警级别
     * @param type 告警类型
     * @param source 告警来源
     */
    public void createAlert(String title, String message, String level, String type, String source) {
        log.info("Creating alert: title={}, level={}, type={}", title, level, type);
        
        AlertRecord record = new AlertRecord();
        record.setRuleName(title);
        record.setAlertMessage(message);
        record.setAlertLevel(level != null ? level : "warning");
        record.setMetricType(type);
        record.setMetricName(source);
        record.setAlertTime(new Date());
        record.setIsResolved(0);
        record.setIsNotified(0);
        
        alertRecordMapper.insert(record);
    }
}
