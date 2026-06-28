package com.dataplatform.data.service.security;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 异常登录检测器接口
 * 检测异常登录行为并发送通知
 * 
 * 需求 4.7: WHEN 检测到异常登录行为时，THE Security_Engine SHALL 发送安全提醒通知
 *
 * @author dataplatform
 */
public interface LoginAnomalyDetector {

    /**
     * 分析登录行为是否异常
     *
     * @param context 登录上下文信息
     * @return 异常检测结果
     */
    AnomalyDetectionResult analyzeLogin(LoginContext context);

    /**
     * 记录登录历史
     *
     * @param context 登录上下文信息
     * @param success 是否登录成功
     */
    void recordLoginHistory(LoginContext context, boolean success);

    /**
     * 获取用户最近的登录历史
     *
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 登录历史列表
     */
    List<LoginHistory> getRecentLoginHistory(Long userId, int limit);

    /**
     * 发送异常登录通知
     *
     * @param userId 用户ID
     * @param result 异常检测结果
     */
    void sendAnomalyNotification(Long userId, AnomalyDetectionResult result);

    /**
     * 登录上下文信息
     */
    record LoginContext(
        Long userId,
        String username,
        String clientIp,
        String userAgent,
        String deviceFingerprint,
        String geoLocation,
        LocalDateTime loginTime
    ) {
        public static LoginContext of(Long userId, String username, String clientIp, String userAgent) {
            return new LoginContext(userId, username, clientIp, userAgent, null, null, LocalDateTime.now());
        }
    }

    /**
     * 登录历史记录
     */
    record LoginHistory(
        Long userId,
        String clientIp,
        String userAgent,
        String geoLocation,
        LocalDateTime loginTime,
        boolean success
    ) {}

    /**
     * 异常检测结果
     */
    record AnomalyDetectionResult(
        boolean isAnomaly,
        AnomalyType anomalyType,
        String description,
        RiskLevel riskLevel,
        String recommendation
    ) {
        public static AnomalyDetectionResult normal() {
            return new AnomalyDetectionResult(false, null, "正常登录", RiskLevel.NONE, null);
        }

        public static AnomalyDetectionResult anomaly(AnomalyType type, String description, RiskLevel level, String recommendation) {
            return new AnomalyDetectionResult(true, type, description, level, recommendation);
        }
    }

    /**
     * 异常类型
     */
    enum AnomalyType {
        /** 新设备登录 */
        NEW_DEVICE,
        /** 新IP地址登录 */
        NEW_IP,
        /** 异地登录 */
        NEW_LOCATION,
        /** 异常时间登录 */
        UNUSUAL_TIME,
        /** 短时间内多次登录 */
        FREQUENT_LOGIN,
        /** 多设备同时登录 */
        CONCURRENT_LOGIN,
        /** 可疑IP登录 */
        SUSPICIOUS_IP,
        /** 暴力破解尝试 */
        BRUTE_FORCE
    }

    /**
     * 风险等级
     */
    enum RiskLevel {
        NONE,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
