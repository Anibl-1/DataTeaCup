package com.dataplatform.data.service.usage;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 使用统计服务接口
 * 需求: 30.1, 30.2, 30.3, 30.4, 30.5
 */
public interface UsageStatsService {

    /** 记录使用事件 */
    void recordUsage(UsageStats stats);

    /** 获取功能使用频率统计 */
    Map<String, Long> getFeatureUsageCount(LocalDate startDate, LocalDate endDate);

    /** 获取日活跃用户数(DAU) */
    long getDailyActiveUsers(LocalDate date);

    /** 获取周活跃用户数(WAU) */
    long getWeeklyActiveUsers(LocalDate weekStart);

    /** 获取月活跃用户数(MAU) */
    long getMonthlyActiveUsers(int year, int month);

    /** 获取资源访问排行 */
    List<ResourceAccessRank> getResourceAccessRank(String resourceType, int topN, LocalDate startDate, LocalDate endDate);

    /** 获取查询性能分布 */
    Map<String, Long> getQueryPerformanceDistribution(LocalDate startDate, LocalDate endDate);

    /** 获取错误率统计 */
    Map<String, Long> getErrorRateStats(LocalDate startDate, LocalDate endDate);

    /** 获取指定时间范围内的统计记录 */
    List<UsageStats> getUsageRecords(LocalDate startDate, LocalDate endDate, int limit);

    /** 资源访问排行数据 */
    class ResourceAccessRank {
        private String resourceId;
        private String resourceType;
        private long accessCount;
        private long uniqueUsers;

        public String getResourceId() { return resourceId; }
        public void setResourceId(String resourceId) { this.resourceId = resourceId; }
        public String getResourceType() { return resourceType; }
        public void setResourceType(String resourceType) { this.resourceType = resourceType; }
        public long getAccessCount() { return accessCount; }
        public void setAccessCount(long accessCount) { this.accessCount = accessCount; }
        public long getUniqueUsers() { return uniqueUsers; }
        public void setUniqueUsers(long uniqueUsers) { this.uniqueUsers = uniqueUsers; }
    }
}
