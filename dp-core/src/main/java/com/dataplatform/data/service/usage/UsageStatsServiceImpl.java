package com.dataplatform.data.service.usage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * 使用统计服务实现
 * 使用内存存储，生产环境应替换为数据库
 * 需求: 30.1, 30.2, 30.3, 30.4, 30.5
 */
@Service
public class UsageStatsServiceImpl implements UsageStatsService {

    private static final Logger log = LoggerFactory.getLogger(UsageStatsServiceImpl.class);

    private final ConcurrentLinkedQueue<UsageStats> records = new ConcurrentLinkedQueue<>();

    @PostConstruct
    public void seedDemoUsageData() {
        if (!records.isEmpty()) {
            return;
        }
        String[] users = {"admin", "analyst", "operator", "viewer"};
        String[] features = {"dashboard", "report", "chart", "sql-executor", "data-source"};
        String[] resourceTypes = {"dashboard", "report", "chart", "datasource"};
        String[] actions = {"view", "query", "export", "create"};

        LocalDateTime now = LocalDateTime.now();
        for (int day = 0; day < 7; day++) {
            for (int i = 0; i < 8; i++) {
                UsageStats stats = new UsageStats();
                stats.setUserId(users[(day + i) % users.length]);
                stats.setFeatureCode(features[(day + i) % features.length]);
                stats.setAction(actions[i % actions.length]);
                stats.setResourceType(resourceTypes[(day + i) % resourceTypes.length]);
                stats.setResourceId(stats.getResourceType() + "-" + ((i % 4) + 1));
                stats.setDuration(80L + (long) (day * 120 + i * 95));
                stats.setResultCount(20 + day * 5 + i);
                stats.setClientType(i % 3 == 0 ? "api" : "web");
                if (i == 7 && day % 3 == 0) {
                    stats.setErrorCode("E_QUERY_TIMEOUT");
                }
                stats.setCreateTime(now.minusDays(day).minusMinutes(i * 17L));
                records.add(stats);
            }
        }
        log.info("Seeded {} in-memory usage statistics records for ops inspection", records.size());
    }

    @Override
    public void recordUsage(UsageStats stats) {
        if (stats.getCreateTime() == null) {
            stats.setCreateTime(LocalDateTime.now());
        }
        records.add(stats);
        log.debug("记录使用统计: feature={}, action={}, user={}",
                stats.getFeatureCode(), stats.getAction(), stats.getUserId());
    }

    @Override
    public Map<String, Long> getFeatureUsageCount(LocalDate startDate, LocalDate endDate) {
        return filterByDateRange(startDate, endDate).stream()
                .filter(s -> s.getFeatureCode() != null)
                .collect(Collectors.groupingBy(UsageStats::getFeatureCode, Collectors.counting()));
    }

    @Override
    public long getDailyActiveUsers(LocalDate date) {
        return filterByDateRange(date, date).stream()
                .map(UsageStats::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    @Override
    public long getWeeklyActiveUsers(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        return filterByDateRange(weekStart, weekEnd).stream()
                .map(UsageStats::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    @Override
    public long getMonthlyActiveUsers(int year, int month) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        return filterByDateRange(monthStart, monthEnd).stream()
                .map(UsageStats::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    @Override
    public List<ResourceAccessRank> getResourceAccessRank(String resourceType, int topN,
                                                           LocalDate startDate, LocalDate endDate) {
        String normalizedResourceType = resourceType == null ? "" : resourceType.trim();
        boolean includeAllTypes = normalizedResourceType.isEmpty() || "all".equalsIgnoreCase(normalizedResourceType);

        Map<String, List<UsageStats>> grouped = filterByDateRange(startDate, endDate).stream()
                .filter(s -> s.getResourceId() != null)
                .filter(s -> includeAllTypes || normalizedResourceType.equals(s.getResourceType()))
                .collect(Collectors.groupingBy(s -> String.valueOf(s.getResourceType()) + "\u0000" + s.getResourceId()));

        return grouped.entrySet().stream()
                .map(entry -> {
                    UsageStats first = entry.getValue().get(0);
                    ResourceAccessRank rank = new ResourceAccessRank();
                    rank.setResourceId(first.getResourceId());
                    rank.setResourceType(first.getResourceType());
                    rank.setAccessCount(entry.getValue().size());
                    rank.setUniqueUsers(entry.getValue().stream()
                            .map(UsageStats::getUserId).filter(Objects::nonNull).distinct().count());
                    return rank;
                })
                .sorted((a, b) -> Long.compare(b.getAccessCount(), a.getAccessCount()))
                .limit(topN)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Long> getQueryPerformanceDistribution(LocalDate startDate, LocalDate endDate) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("<100ms", 0L);
        distribution.put("100-500ms", 0L);
        distribution.put("500ms-1s", 0L);
        distribution.put("1s-5s", 0L);
        distribution.put(">5s", 0L);

        filterByDateRange(startDate, endDate).stream()
                .filter(s -> "query".equals(s.getAction()) && s.getDuration() != null)
                .forEach(s -> {
                    long d = s.getDuration();
                    if (d < 100) distribution.merge("<100ms", 1L, Long::sum);
                    else if (d < 500) distribution.merge("100-500ms", 1L, Long::sum);
                    else if (d < 1000) distribution.merge("500ms-1s", 1L, Long::sum);
                    else if (d < 5000) distribution.merge("1s-5s", 1L, Long::sum);
                    else distribution.merge(">5s", 1L, Long::sum);
                });

        return distribution;
    }

    @Override
    public Map<String, Long> getErrorRateStats(LocalDate startDate, LocalDate endDate) {
        List<UsageStats> filtered = filterByDateRange(startDate, endDate);
        long total = filtered.size();
        long errors = filtered.stream().filter(s -> s.getErrorCode() != null).count();

        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("errors", errors);

        // 按错误码分组
        filtered.stream()
                .filter(s -> s.getErrorCode() != null)
                .collect(Collectors.groupingBy(UsageStats::getErrorCode, Collectors.counting()))
                .forEach(stats::put);

        return stats;
    }

    @Override
    public List<UsageStats> getUsageRecords(LocalDate startDate, LocalDate endDate, int limit) {
        return filterByDateRange(startDate, endDate).stream()
                .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<UsageStats> filterByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        return records.stream()
                .filter(s -> s.getCreateTime() != null
                        && !s.getCreateTime().isBefore(start)
                        && s.getCreateTime().isBefore(end))
                .collect(Collectors.toList());
    }
}
