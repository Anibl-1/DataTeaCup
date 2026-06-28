package com.dataplatform.data.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataplatform.data.entity.ParameterUsageHistory;
import com.dataplatform.data.mapper.ParameterUsageHistoryMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 参数默认值推荐服务
 * 基于用户历史使用记录智能推荐参数默认值
 * 
 * 推荐策略：
 * 1. 最常用值（frequency）：基于使用次数统计
 * 2. 最近使用值（recent）：基于最近使用时间
 * 3. 用户偏好（preference）：结合频率和时间的综合评分
 * 
 * @validates 需求 13.3 - 基于用户历史使用记录智能推荐参数默认值
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParameterRecommendService {
    
    private final ParameterUsageHistoryMapper usageHistoryMapper;
    private final ObjectMapper objectMapper;
    
    /** 默认推荐数量 */
    private static final int DEFAULT_RECOMMEND_LIMIT = 5;
    
    /** 时间衰减因子（天） */
    private static final double TIME_DECAY_DAYS = 30.0;
    
    /**
     * 推荐策略枚举
     */
    public enum RecommendStrategy {
        /** 最常用 */
        FREQUENCY,
        /** 最近使用 */
        RECENT,
        /** 综合偏好（默认） */
        PREFERENCE
    }
    
    /**
     * 推荐结果
     */
    @Data
    public static class RecommendResult {
        /** 推荐值 */
        private Object value;
        /** 推荐分数（0-1） */
        private double score;
        /** 使用次数 */
        private int usageCount;
        /** 最后使用时间 */
        private LocalDateTime lastUsedAt;
        /** 推荐原因 */
        private String reason;
    }
    
    /**
     * 推荐请求
     */
    @Data
    public static class RecommendRequest {
        /** 用户ID */
        private Long userId;
        /** 参数名称 */
        private String paramName;
        /** 报表ID（可选） */
        private Long reportId;
        /** 图表ID（可选） */
        private Long chartId;
        /** 推荐策略 */
        private RecommendStrategy strategy = RecommendStrategy.PREFERENCE;
        /** 推荐数量 */
        private Integer limit;
        /** 是否包含全局推荐 */
        private Boolean includeGlobal = true;
    }
    
    /**
     * 获取参数默认值推荐
     * 
     * @param request 推荐请求
     * @return 推荐结果列表
     */
    public List<RecommendResult> getRecommendations(RecommendRequest request) {
        if (request.getUserId() == null || !StringUtils.hasText(request.getParamName())) {
            return Collections.emptyList();
        }
        
        int limit = request.getLimit() != null ? request.getLimit() : DEFAULT_RECOMMEND_LIMIT;
        RecommendStrategy strategy = request.getStrategy() != null ? request.getStrategy() : RecommendStrategy.PREFERENCE;
        
        log.debug("获取参数推荐: userId={}, paramName={}, strategy={}", 
                request.getUserId(), request.getParamName(), strategy);
        
        List<ParameterUsageHistory> histories;
        
        // 根据上下文获取历史记录
        if (request.getReportId() != null) {
            histories = usageHistoryMapper.findByUserReportAndParam(
                    request.getUserId(), request.getReportId(), request.getParamName(), limit * 2);
        } else if (request.getChartId() != null) {
            histories = usageHistoryMapper.findByUserChartAndParam(
                    request.getUserId(), request.getChartId(), request.getParamName(), limit * 2);
        } else {
            // 根据策略选择查询方式
            if (strategy == RecommendStrategy.RECENT) {
                histories = usageHistoryMapper.findByUserAndParamOrderByRecent(
                        request.getUserId(), request.getParamName(), limit * 2);
            } else {
                histories = usageHistoryMapper.findByUserAndParamOrderByUsage(
                        request.getUserId(), request.getParamName(), limit * 2);
            }
        }
        
        // 如果用户历史不足且允许全局推荐，补充全局数据
        if (histories.size() < limit && Boolean.TRUE.equals(request.getIncludeGlobal())) {
            List<ParameterUsageHistory> globalHistories = usageHistoryMapper.findGlobalMostUsed(
                    request.getParamName(), limit);
            
            // 合并去重
            Set<String> existingValues = histories.stream()
                    .map(ParameterUsageHistory::getParamValue)
                    .collect(Collectors.toSet());
            
            for (ParameterUsageHistory global : globalHistories) {
                if (!existingValues.contains(global.getParamValue())) {
                    histories.add(global);
                }
            }
        }
        
        // 计算推荐分数
        List<RecommendResult> results = calculateScores(histories, strategy);
        
        // 排序并限制数量
        return results.stream()
                .sorted(Comparator.comparingDouble(RecommendResult::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取单个参数的推荐默认值
     * 
     * @param userId 用户ID
     * @param paramName 参数名称
     * @param reportId 报表ID（可选）
     * @param chartId 图表ID（可选）
     * @return 推荐的默认值，如果没有推荐则返回null
     */
    public Object getDefaultValue(Long userId, String paramName, Long reportId, Long chartId) {
        RecommendRequest request = new RecommendRequest();
        request.setUserId(userId);
        request.setParamName(paramName);
        request.setReportId(reportId);
        request.setChartId(chartId);
        request.setLimit(1);
        
        List<RecommendResult> recommendations = getRecommendations(request);
        
        if (recommendations.isEmpty()) {
            return null;
        }
        
        return recommendations.get(0).getValue();
    }
    
    /**
     * 批量获取参数默认值推荐
     * 
     * @param userId 用户ID
     * @param paramNames 参数名称列表
     * @param reportId 报表ID（可选）
     * @param chartId 图表ID（可选）
     * @return 参数名到推荐值的映射
     */
    public Map<String, Object> getDefaultValues(Long userId, List<String> paramNames, 
                                                 Long reportId, Long chartId) {
        Map<String, Object> result = new HashMap<>();
        
        for (String paramName : paramNames) {
            Object value = getDefaultValue(userId, paramName, reportId, chartId);
            if (value != null) {
                result.put(paramName, value);
            }
        }
        
        return result;
    }
    
    /**
     * 记录参数使用
     * 
     * @param userId 用户ID
     * @param paramName 参数名称
     * @param paramValue 参数值
     * @param reportId 报表ID（可选）
     * @param chartId 图表ID（可选）
     */
    @Transactional
    public void recordUsage(Long userId, String paramName, Object paramValue, 
                           Long reportId, Long chartId) {
        if (userId == null || !StringUtils.hasText(paramName) || paramValue == null) {
            return;
        }
        
        String valueJson = serializeValue(paramValue);
        String valueType = detectValueType(paramValue);
        
        // 查找已存在的记录
        ParameterUsageHistory existing = usageHistoryMapper.findExisting(
                userId, paramName, valueJson, reportId, chartId);
        
        if (existing != null) {
            // 更新使用次数和时间
            existing.setUsageCount(existing.getUsageCount() + 1);
            existing.setLastUsedAt(LocalDateTime.now());
            usageHistoryMapper.updateById(existing);
            log.debug("更新参数使用记录: userId={}, paramName={}, usageCount={}", 
                    userId, paramName, existing.getUsageCount());
        } else {
            // 创建新记录
            ParameterUsageHistory history = new ParameterUsageHistory();
            history.setUserId(userId);
            history.setParamName(paramName);
            history.setParamValue(valueJson);
            history.setValueType(valueType);
            history.setReportId(reportId);
            history.setChartId(chartId);
            history.setUsageCount(1);
            history.setLastUsedAt(LocalDateTime.now());
            history.setCreateTime(LocalDateTime.now());
            history.setUpdateTime(LocalDateTime.now());
            usageHistoryMapper.insert(history);
            log.debug("创建参数使用记录: userId={}, paramName={}, value={}", 
                    userId, paramName, valueJson);
        }
    }
    
    /**
     * 批量记录参数使用
     * 
     * @param userId 用户ID
     * @param paramValues 参数值映射
     * @param reportId 报表ID（可选）
     * @param chartId 图表ID（可选）
     */
    @Transactional
    public void recordUsageBatch(Long userId, Map<String, Object> paramValues, 
                                 Long reportId, Long chartId) {
        if (paramValues == null || paramValues.isEmpty()) {
            return;
        }
        
        for (Map.Entry<String, Object> entry : paramValues.entrySet()) {
            recordUsage(userId, entry.getKey(), entry.getValue(), reportId, chartId);
        }
    }
    
    /**
     * 清除用户的参数使用历史
     * 
     * @param userId 用户ID
     * @param paramName 参数名称（可选，为空则清除所有）
     */
    @Transactional
    public void clearHistory(Long userId, String paramName) {
        LambdaQueryWrapper<ParameterUsageHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ParameterUsageHistory::getUserId, userId);
        
        if (StringUtils.hasText(paramName)) {
            wrapper.eq(ParameterUsageHistory::getParamName, paramName);
        }
        
        int deleted = usageHistoryMapper.delete(wrapper);
        log.info("清除参数使用历史: userId={}, paramName={}, deleted={}", userId, paramName, deleted);
    }
    
    /**
     * 计算推荐分数
     */
    private List<RecommendResult> calculateScores(List<ParameterUsageHistory> histories, 
                                                   RecommendStrategy strategy) {
        if (histories.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 计算最大使用次数（用于归一化）
        int maxUsageCount = histories.stream()
                .mapToInt(ParameterUsageHistory::getUsageCount)
                .max()
                .orElse(1);
        
        LocalDateTime now = LocalDateTime.now();
        
        return histories.stream()
                .map(history -> {
                    RecommendResult result = new RecommendResult();
                    result.setValue(deserializeValue(history.getParamValue(), history.getValueType()));
                    result.setUsageCount(history.getUsageCount());
                    result.setLastUsedAt(history.getLastUsedAt());
                    
                    double score;
                    String reason;
                    
                    switch (strategy) {
                        case FREQUENCY:
                            score = (double) history.getUsageCount() / maxUsageCount;
                            reason = String.format("使用 %d 次", history.getUsageCount());
                            break;
                            
                        case RECENT:
                            score = calculateRecencyScore(history.getLastUsedAt(), now);
                            reason = String.format("最近使用于 %s", formatRelativeTime(history.getLastUsedAt()));
                            break;
                            
                        case PREFERENCE:
                        default:
                            // 综合评分：60% 频率 + 40% 时间
                            double frequencyScore = (double) history.getUsageCount() / maxUsageCount;
                            double recencyScore = calculateRecencyScore(history.getLastUsedAt(), now);
                            score = 0.6 * frequencyScore + 0.4 * recencyScore;
                            reason = String.format("使用 %d 次，最近使用于 %s", 
                                    history.getUsageCount(), formatRelativeTime(history.getLastUsedAt()));
                            break;
                    }
                    
                    result.setScore(score);
                    result.setReason(reason);
                    return result;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 计算时间衰减分数
     */
    private double calculateRecencyScore(LocalDateTime lastUsedAt, LocalDateTime now) {
        if (lastUsedAt == null) {
            return 0.0;
        }
        
        long daysBetween = java.time.Duration.between(lastUsedAt, now).toDays();
        // 指数衰减
        return Math.exp(-daysBetween / TIME_DECAY_DAYS);
    }
    
    /**
     * 格式化相对时间
     */
    private String formatRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "未知";
        }
        
        LocalDateTime now = LocalDateTime.now();
        long days = java.time.Duration.between(dateTime, now).toDays();
        
        if (days == 0) {
            return "今天";
        } else if (days == 1) {
            return "昨天";
        } else if (days < 7) {
            return days + " 天前";
        } else if (days < 30) {
            return (days / 7) + " 周前";
        } else {
            return (days / 30) + " 个月前";
        }
    }
    
    /**
     * 序列化参数值
     */
    private String serializeValue(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof String) {
            return (String) value;
        }
        
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("序列化参数值失败: {}", e.getMessage());
            return String.valueOf(value);
        }
    }
    
    /**
     * 反序列化参数值
     */
    private Object deserializeValue(String valueJson, String valueType) {
        if (valueJson == null) {
            return null;
        }
        
        if (valueType == null || "string".equals(valueType)) {
            // 尝试解析JSON，如果失败则返回原字符串
            if (valueJson.startsWith("\"") && valueJson.endsWith("\"")) {
                try {
                    return objectMapper.readValue(valueJson, String.class);
                } catch (JsonProcessingException e) {
                    return valueJson;
                }
            }
            return valueJson;
        }
        
        try {
            switch (valueType) {
                case "number":
                    if (valueJson.contains(".")) {
                        return Double.parseDouble(valueJson);
                    }
                    return Long.parseLong(valueJson);
                case "date":
                    return valueJson;
                case "array":
                    return objectMapper.readValue(valueJson, List.class);
                case "object":
                    return objectMapper.readValue(valueJson, Map.class);
                default:
                    return valueJson;
            }
        } catch (Exception e) {
            log.warn("反序列化参数值失败: type={}, value={}, error={}", valueType, valueJson, e.getMessage());
            return valueJson;
        }
    }
    
    /**
     * 检测值类型
     */
    private String detectValueType(Object value) {
        if (value == null) {
            return "string";
        }
        
        if (value instanceof Number) {
            return "number";
        }
        
        if (value instanceof Boolean) {
            return "boolean";
        }
        
        if (value instanceof List || value.getClass().isArray()) {
            return "array";
        }
        
        if (value instanceof Map) {
            return "object";
        }
        
        // 检查是否为日期格式
        String strValue = String.valueOf(value);
        if (strValue.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
            return "date";
        }
        
        return "string";
    }
}
