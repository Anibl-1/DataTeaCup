package com.dataplatform.data.service.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 异常登录检测器实现类
 * 
 * 需求 4.7: WHEN 检测到异常登录行为时，THE Security_Engine SHALL 发送安全提醒通知
 *
 * @author dataplatform
 */
@Slf4j
@Service
public class LoginAnomalyDetectorImpl implements LoginAnomalyDetector {

    private static final String LOGIN_HISTORY_KEY_PREFIX = "login:history:";
    private static final String KNOWN_IPS_KEY_PREFIX = "login:known_ips:";
    private static final String KNOWN_DEVICES_KEY_PREFIX = "login:known_devices:";
    private static final String RECENT_LOGINS_KEY_PREFIX = "login:recent:";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Value("${security.login.anomaly.history-days:30}")
    private int historyDays;

    @Value("${security.login.anomaly.max-history-records:100}")
    private int maxHistoryRecords;

    @Value("${security.login.anomaly.unusual-hour-start:0}")
    private int unusualHourStart;

    @Value("${security.login.anomaly.unusual-hour-end:6}")
    private int unusualHourEnd;

    @Value("${security.login.anomaly.frequent-login-threshold:5}")
    private int frequentLoginThreshold;

    @Value("${security.login.anomaly.frequent-login-window-minutes:10}")
    private int frequentLoginWindowMinutes;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Override
    public AnomalyDetectionResult analyzeLogin(LoginContext context) {
        if (context == null || context.userId() == null) {
            return AnomalyDetectionResult.normal();
        }

        List<AnomalyDetectionResult> anomalies = new ArrayList<>();

        // 检测新IP地址
        if (isNewIp(context.userId(), context.clientIp())) {
            anomalies.add(AnomalyDetectionResult.anomaly(
                AnomalyType.NEW_IP,
                "检测到新IP地址登录: " + context.clientIp(),
                RiskLevel.MEDIUM,
                "如果这不是您本人操作，请立即修改密码"
            ));
        }

        // 检测新设备
        if (isNewDevice(context.userId(), context.userAgent())) {
            anomalies.add(AnomalyDetectionResult.anomaly(
                AnomalyType.NEW_DEVICE,
                "检测到新设备登录",
                RiskLevel.LOW,
                "如果这不是您本人操作，请检查账户安全"
            ));
        }

        // 检测异常时间登录
        if (isUnusualTime(context.loginTime())) {
            anomalies.add(AnomalyDetectionResult.anomaly(
                AnomalyType.UNUSUAL_TIME,
                "检测到异常时间登录: " + context.loginTime().format(TIME_FORMATTER),
                RiskLevel.LOW,
                "请确认是否为本人操作"
            ));
        }

        // 检测频繁登录
        if (isFrequentLogin(context.userId())) {
            anomalies.add(AnomalyDetectionResult.anomaly(
                AnomalyType.FREQUENT_LOGIN,
                "检测到短时间内多次登录尝试",
                RiskLevel.HIGH,
                "您的账户可能正在被尝试暴力破解，请确保密码安全"
            ));
        }

        // 返回最高风险等级的异常
        if (!anomalies.isEmpty()) {
            return anomalies.stream()
                .max(Comparator.comparing(r -> r.riskLevel().ordinal()))
                .orElse(AnomalyDetectionResult.normal());
        }

        return AnomalyDetectionResult.normal();
    }


    @Override
    public void recordLoginHistory(LoginContext context, boolean success) {
        if (context == null || context.userId() == null) {
            return;
        }

        try {
            // 记录登录历史
            String historyKey = LOGIN_HISTORY_KEY_PREFIX + context.userId();
            String historyEntry = buildHistoryEntry(context, success);
            
            if (redisTemplate != null) {
                redisTemplate.opsForList().leftPush(historyKey, historyEntry);
                redisTemplate.opsForList().trim(historyKey, 0, maxHistoryRecords - 1);
                redisTemplate.expire(historyKey, historyDays, TimeUnit.DAYS);
            }

            // 如果登录成功，记录已知IP和设备
            if (success) {
                recordKnownIp(context.userId(), context.clientIp());
                recordKnownDevice(context.userId(), context.userAgent());
            }

            // 记录最近登录（用于频繁登录检测）
            recordRecentLogin(context.userId());

            log.debug("记录登录历史: userId={}, ip={}, success={}", 
                context.userId(), context.clientIp(), success);
        } catch (Exception e) {
            log.error("记录登录历史异常: userId={}", context.userId(), e);
        }
    }

    @Override
    public List<LoginHistory> getRecentLoginHistory(Long userId, int limit) {
        if (userId == null || redisTemplate == null) {
            return Collections.emptyList();
        }

        try {
            String historyKey = LOGIN_HISTORY_KEY_PREFIX + userId;
            List<String> entries = redisTemplate.opsForList().range(historyKey, 0, limit - 1);
            
            if (entries == null || entries.isEmpty()) {
                return Collections.emptyList();
            }

            return entries.stream()
                .map(this::parseHistoryEntry)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取登录历史异常: userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void sendAnomalyNotification(Long userId, AnomalyDetectionResult result) {
        if (userId == null || result == null || !result.isAnomaly()) {
            return;
        }

        log.warn("检测到异常登录: userId={}, type={}, level={}, description={}", 
            userId, result.anomalyType(), result.riskLevel(), result.description());

        // TODO: 集成通知服务发送邮件/短信/站内信
        // notificationService.sendSecurityAlert(userId, buildAlertMessage(result));
    }

    // ==================== 私有方法 ====================

    private boolean isNewIp(Long userId, String clientIp) {
        if (!StringUtils.hasText(clientIp) || redisTemplate == null) {
            return false;
        }

        String key = KNOWN_IPS_KEY_PREFIX + userId;
        try {
            Boolean isMember = redisTemplate.opsForSet().isMember(key, clientIp);
            return !Boolean.TRUE.equals(isMember);
        } catch (Exception e) {
            log.error("检查已知IP异常: userId={}", userId, e);
            return false;
        }
    }

    private boolean isNewDevice(Long userId, String userAgent) {
        if (!StringUtils.hasText(userAgent) || redisTemplate == null) {
            return false;
        }

        String deviceHash = hashUserAgent(userAgent);
        String key = KNOWN_DEVICES_KEY_PREFIX + userId;
        try {
            Boolean isMember = redisTemplate.opsForSet().isMember(key, deviceHash);
            return !Boolean.TRUE.equals(isMember);
        } catch (Exception e) {
            log.error("检查已知设备异常: userId={}", userId, e);
            return false;
        }
    }

    private boolean isUnusualTime(LocalDateTime loginTime) {
        if (loginTime == null) {
            return false;
        }

        LocalTime time = loginTime.toLocalTime();
        int hour = time.getHour();
        return hour >= unusualHourStart && hour < unusualHourEnd;
    }

    private boolean isFrequentLogin(Long userId) {
        if (userId == null || redisTemplate == null) {
            return false;
        }

        String key = RECENT_LOGINS_KEY_PREFIX + userId;
        try {
            Long count = redisTemplate.opsForList().size(key);
            return count != null && count >= frequentLoginThreshold;
        } catch (Exception e) {
            log.error("检查频繁登录异常: userId={}", userId, e);
            return false;
        }
    }

    private void recordKnownIp(Long userId, String clientIp) {
        if (!StringUtils.hasText(clientIp) || redisTemplate == null) {
            return;
        }

        String key = KNOWN_IPS_KEY_PREFIX + userId;
        try {
            redisTemplate.opsForSet().add(key, clientIp);
            redisTemplate.expire(key, historyDays, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("记录已知IP异常: userId={}", userId, e);
        }
    }

    private void recordKnownDevice(Long userId, String userAgent) {
        if (!StringUtils.hasText(userAgent) || redisTemplate == null) {
            return;
        }

        String deviceHash = hashUserAgent(userAgent);
        String key = KNOWN_DEVICES_KEY_PREFIX + userId;
        try {
            redisTemplate.opsForSet().add(key, deviceHash);
            redisTemplate.expire(key, historyDays, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("记录已知设备异常: userId={}", userId, e);
        }
    }

    private void recordRecentLogin(Long userId) {
        if (userId == null || redisTemplate == null) {
            return;
        }

        String key = RECENT_LOGINS_KEY_PREFIX + userId;
        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            redisTemplate.opsForList().leftPush(key, timestamp);
            redisTemplate.opsForList().trim(key, 0, frequentLoginThreshold);
            redisTemplate.expire(key, frequentLoginWindowMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("记录最近登录异常: userId={}", userId, e);
        }
    }

    private String buildHistoryEntry(LoginContext context, boolean success) {
        return String.join("|",
            context.clientIp() != null ? context.clientIp() : "",
            context.userAgent() != null ? context.userAgent() : "",
            context.geoLocation() != null ? context.geoLocation() : "",
            context.loginTime().toString(),
            String.valueOf(success)
        );
    }

    private LoginHistory parseHistoryEntry(String entry) {
        if (!StringUtils.hasText(entry)) {
            return null;
        }

        try {
            String[] parts = entry.split("\\|", -1);
            if (parts.length < 5) {
                return null;
            }

            return new LoginHistory(
                null,
                parts[0],
                parts[1],
                parts[2],
                LocalDateTime.parse(parts[3]),
                Boolean.parseBoolean(parts[4])
            );
        } catch (Exception e) {
            log.warn("解析登录历史记录失败: {}", entry, e);
            return null;
        }
    }

    private String hashUserAgent(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "";
        }
        // 简单哈希，实际可使用更复杂的设备指纹算法
        return String.valueOf(userAgent.hashCode());
    }
}
