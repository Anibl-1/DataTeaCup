package com.dataplatform.data.service.alert;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警静默服务
 * 在维护期间暂停告警发送
 * 需求: 15.5
 */
@Slf4j
@Service
public class AlertSilenceService {

    /** 静默规则 key=silenceId */
    private final Map<String, SilenceRule> silenceRules = new ConcurrentHashMap<>();

    /**
     * 添加静默规则
     */
    public String addSilenceRule(String name, String matchPattern, LocalDateTime startTime,
                                  LocalDateTime endTime, String createdBy) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        SilenceRule rule = new SilenceRule();
        rule.setId(id);
        rule.setName(name);
        rule.setMatchPattern(matchPattern);
        rule.setStartTime(startTime);
        rule.setEndTime(endTime);
        rule.setCreatedBy(createdBy);
        rule.setCreatedAt(LocalDateTime.now());
        silenceRules.put(id, rule);
        log.info("添加告警静默规则: id={}, name={}, pattern={}, until={}", id, name, matchPattern, endTime);
        return id;
    }

    /**
     * 移除静默规则
     */
    public boolean removeSilenceRule(String silenceId) {
        SilenceRule removed = silenceRules.remove(silenceId);
        if (removed != null) {
            log.info("移除告警静默规则: {}", silenceId);
            return true;
        }
        return false;
    }

    /**
     * 检查告警是否被静默
     *
     * @param alertFingerprint 告警指纹
     * @return true=被静默
     */
    public boolean isSilenced(String alertFingerprint) {
        LocalDateTime now = LocalDateTime.now();
        for (SilenceRule rule : silenceRules.values()) {
            if (now.isBefore(rule.getStartTime()) || now.isAfter(rule.getEndTime())) {
                continue;
            }
            if (matches(alertFingerprint, rule.getMatchPattern())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取所有静默规则
     */
    public List<SilenceRule> listSilenceRules() {
        // 清理过期规则
        LocalDateTime now = LocalDateTime.now();
        silenceRules.entrySet().removeIf(e -> now.isAfter(e.getValue().getEndTime()));
        return new ArrayList<>(silenceRules.values());
    }

    private boolean matches(String fingerprint, String pattern) {
        if (pattern == null || pattern.equals("*")) return true;
        // 简单匹配：支持前缀匹配（pattern以*结尾）
        if (pattern.endsWith("*")) {
            return fingerprint.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return fingerprint.contains(pattern);
    }

    @Data
    public static class SilenceRule {
        private String id;
        private String name;
        private String matchPattern;  // 匹配模式，支持*通配
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String createdBy;
        private LocalDateTime createdAt;
    }
}
