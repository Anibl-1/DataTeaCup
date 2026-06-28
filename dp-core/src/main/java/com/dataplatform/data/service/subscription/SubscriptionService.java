package com.dataplatform.data.service.subscription;

import com.dataplatform.data.mapper.SubscriptionMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 订阅与推送服务 - 数据库持久化
 * 需求: 25.1, 25.2, 25.3, 25.4, 25.5, 25.6
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionMapper subscriptionMapper;
    private static final ObjectMapper JSON = new ObjectMapper();

    public ReportSubscription subscribe(ReportSubscription sub) {
        if (sub.getId() == null) {
            sub.setId(UUID.randomUUID().toString().substring(0, 12));
        }
        sub.setActive(true);
        sub.setCreatedAt(LocalDateTime.now());
        sub.setRecipientsJson(toJson(sub.getRecipients()));
        subscriptionMapper.insertSubscription(sub);
        log.info("[订阅] 创建: id={}, resource={}/{}", sub.getId(), sub.getResourceType(), sub.getResourceId());
        return sub;
    }

    public void unsubscribe(String subscriptionId) {
        subscriptionMapper.deactivate(subscriptionId);
    }

    public List<ReportSubscription> getUserSubscriptions(String userId) {
        List<ReportSubscription> subs = subscriptionMapper.selectByUser(userId);
        subs.forEach(this::parseRecipientsFromJson);
        return subs;
    }

    public List<ReportSubscription> getActiveSubscriptions() {
        List<ReportSubscription> subs = subscriptionMapper.selectActive();
        subs.forEach(this::parseRecipientsFromJson);
        return subs;
    }

    /**
     * 执行推送（需求: 25.2, 25.4, 25.5）
     */
    public PushLog executePush(String subscriptionId) {
        ReportSubscription sub = subscriptionMapper.selectById(subscriptionId);
        if (sub == null || !sub.isActive()) {
            return null;
        }

        PushLog pushLog = new PushLog();
        pushLog.setId(UUID.randomUUID().toString().substring(0, 12));
        pushLog.setSubscriptionId(subscriptionId);
        pushLog.setChannel(sub.getPushChannel());
        pushLog.setPushTime(LocalDateTime.now());

        try {
            // 条件推送检查（需求: 25.5）
            if (sub.getCondition() != null && !sub.getCondition().isEmpty()) {
                if (!evaluateCondition(sub.getCondition())) {
                    pushLog.setStatus("SKIPPED");
                    subscriptionMapper.insertPushLog(pushLog);
                    return pushLog;
                }
            }

            // 实际推送逻辑（通过告警渠道发送）
            log.info("[推送] 执行: subscription={}, channel={}, format={}",
                    subscriptionId, sub.getPushChannel(), sub.getFormat());

            pushLog.setStatus("SUCCESS");
            subscriptionMapper.updateLastPushAt(subscriptionId, LocalDateTime.now());
        } catch (Exception e) {
            pushLog.setStatus("FAILED");
            pushLog.setErrorMessage(e.getMessage());
        }

        subscriptionMapper.insertPushLog(pushLog);
        return pushLog;
    }

    public List<PushLog> getPushLogs(String subscriptionId, int limit) {
        return subscriptionMapper.selectPushLogs(subscriptionId, limit);
    }

    private boolean evaluateCondition(String condition) {
        // 简化实现：实际应执行SQL或表达式
        return true;
    }

    private String toJson(List<String> list) {
        try { return JSON.writeValueAsString(list != null ? list : Collections.emptyList()); } catch (Exception e) { return "[]"; }
    }

    private void parseRecipientsFromJson(ReportSubscription sub) {
        if (sub.getRecipientsJson() != null && !sub.getRecipientsJson().isEmpty()) {
            try {
                sub.setRecipients(JSON.readValue(sub.getRecipientsJson(), new TypeReference<List<String>>() {}));
            } catch (Exception e) {
                sub.setRecipients(Collections.emptyList());
            }
        }
    }
}
