package com.dataplatform.data.service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存统计服务实现
 * 
 * 提供 L1（Caffeine）和 L2（Redis）缓存的统计功能：
 * - 实时统计：从 Caffeine 和 Redis 获取当前统计信息
 * - 历史统计：定期持久化统计数据到数据库
 * - 健康评估：基于命中率评估缓存健康状态
 * 
 * 需求引用：
 * - 需求 10.1: 分别统计 L1 和 L2 缓存的命中率
 * - 需求 10.2: 实时监控缓存内存占用大小
 * 
 * @see CacheStatsService 缓存统计服务接口
 * @see MultiLevelCacheManager 多级缓存管理器
 */
@Slf4j
@Service
public class CacheStatsServiceImpl implements CacheStatsService {
    
    /**
     * 缓存键前缀
     */
    private static final String CACHE_KEY_PREFIX = "dp:cache:";
    
    /**
     * L1 缓存最大条目数
     */
    private static final long L1_MAX_SIZE = 10000;
    
    /**
     * 健康命中率阈值（70%）
     */
    private static final double HEALTHY_HIT_RATE_THRESHOLD = 0.70;
    
    /**
     * 警告命中率阈值（50%）
     */
    private static final double WARNING_HIT_RATE_THRESHOLD = 0.50;
    
    /**
     * L1 本地缓存
     */
    private final Cache<String, Object> l1Cache;
    
    /**
     * Redis 模板（可选）
     */
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * JDBC 模板
     */
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * L2 命中计数器
     */
    private final AtomicLong l2HitCounter = new AtomicLong(0);
    
    /**
     * L2 未命中计数器
     */
    private final AtomicLong l2MissCounter = new AtomicLong(0);
    
    /**
     * 上次持久化时的 L1 命中数
     */
    private long lastL1HitCount = 0;
    
    /**
     * 上次持久化时的 L1 未命中数
     */
    private long lastL1MissCount = 0;
    
    /**
     * 上次持久化时的 L2 命中数
     */
    private long lastL2HitCount = 0;
    
    /**
     * 上次持久化时的 L2 未命中数
     */
    private long lastL2MissCount = 0;
    
    @Autowired
    public CacheStatsServiceImpl(
            @Qualifier("l1Cache") Cache<String, Object> l1Cache,
            @Autowired(required = false) RedisTemplate<String, Object> redisTemplate,
            JdbcTemplate jdbcTemplate) {
        this.l1Cache = l1Cache;
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
        log.info("缓存统计服务初始化完成 - Redis可用: {}", redisTemplate != null);
    }
    
    @Override
    public CacheStats getRealTimeStats() {
        L1CacheStats l1Stats = getL1Stats();
        L2CacheStats l2Stats = getL2Stats();
        
        return CacheStats.builder()
                .l1HitCount(l1Stats.getHitCount())
                .l1MissCount(l1Stats.getMissCount())
                .l1HitRate(l1Stats.getHitRate())
                .l1Size(l1Stats.getEntryCount())
                .l2HitCount(l2Stats.getHitCount())
                .l2MissCount(l2Stats.getMissCount())
                .l2HitRate(l2Stats.getHitRate())
                .l2MemoryUsage(l2Stats.getMemoryUsage())
                .build();
    }
    
    @Override
    public L1CacheStats getL1Stats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats = l1Cache.stats();
        long entryCount = l1Cache.estimatedSize();
        
        // 估算内存使用量（粗略估算：每个条目约 1KB）
        long estimatedMemory = entryCount * 1024;
        
        return L1CacheStats.builder()
                .hitCount(caffeineStats.hitCount())
                .missCount(caffeineStats.missCount())
                .hitRate(caffeineStats.hitRate())
                .entryCount(entryCount)
                .maxSize(L1_MAX_SIZE)
                .evictionCount(caffeineStats.evictionCount())
                .loadSuccessCount(caffeineStats.loadSuccessCount())
                .loadFailureCount(caffeineStats.loadFailureCount())
                .averageLoadPenalty(caffeineStats.averageLoadPenalty())
                .estimatedMemoryUsage(estimatedMemory)
                .build();
    }

    
    @Override
    public L2CacheStats getL2Stats() {
        if (redisTemplate == null) {
            return L2CacheStats.builder()
                    .hitCount(0)
                    .missCount(0)
                    .hitRate(0.0)
                    .keyCount(0)
                    .memoryUsage(0)
                    .memoryUsageHuman("N/A")
                    .maxMemory(0)
                    .connectedClients(0)
                    .available(false)
                    .expiredKeys(0)
                    .evictedKeys(0)
                    .build();
        }
        
        try {
            // 获取 Redis 服务器信息
            Properties memoryInfo = getRedisInfo("memory");
            Properties statsInfo = getRedisInfo("stats");
            Properties clientsInfo = getRedisInfo("clients");
            
            long memoryUsage = parseLong(memoryInfo, "used_memory", 0);
            String memoryUsageHuman = memoryInfo != null ? 
                    memoryInfo.getProperty("used_memory_human", "0B") : "0B";
            long maxMemory = parseLong(memoryInfo, "maxmemory", 0);
            int connectedClients = parseInt(clientsInfo, "connected_clients", 0);
            long expiredKeys = parseLong(statsInfo, "expired_keys", 0);
            long evictedKeys = parseLong(statsInfo, "evicted_keys", 0);
            
            // 获取缓存键数量
            long keyCount = getL2KeyCount();
            
            // 获取命中统计
            long hitCount = l2HitCounter.get();
            long missCount = l2MissCounter.get();
            double hitRate = calculateHitRate(hitCount, missCount);
            
            return L2CacheStats.builder()
                    .hitCount(hitCount)
                    .missCount(missCount)
                    .hitRate(hitRate)
                    .keyCount(keyCount)
                    .memoryUsage(memoryUsage)
                    .memoryUsageHuman(memoryUsageHuman)
                    .maxMemory(maxMemory)
                    .connectedClients(connectedClients)
                    .available(true)
                    .expiredKeys(expiredKeys)
                    .evictedKeys(evictedKeys)
                    .build();
        } catch (Exception e) {
            log.warn("获取 L2 缓存统计失败: {}", e.getMessage());
            return L2CacheStats.builder()
                    .hitCount(l2HitCounter.get())
                    .missCount(l2MissCounter.get())
                    .hitRate(calculateHitRate(l2HitCounter.get(), l2MissCounter.get()))
                    .available(false)
                    .build();
        }
    }
    
    @Override
    public void recordHit(CacheLevel cacheLevel) {
        if (cacheLevel == CacheLevel.L2) {
            l2HitCounter.incrementAndGet();
        }
        // L1 命中由 Caffeine 自动统计
    }
    
    @Override
    public void recordMiss(CacheLevel cacheLevel) {
        if (cacheLevel == CacheLevel.L2) {
            l2MissCounter.incrementAndGet();
        }
        // L1 未命中由 Caffeine 自动统计
    }
    
    @Override
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void persistStats() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // 获取当前统计
            L1CacheStats l1Stats = getL1Stats();
            L2CacheStats l2Stats = getL2Stats();
            
            // 计算增量（自上次持久化以来的变化）
            long l1HitDelta = l1Stats.getHitCount() - lastL1HitCount;
            long l1MissDelta = l1Stats.getMissCount() - lastL1MissCount;
            long l2HitDelta = l2Stats.getHitCount() - lastL2HitCount;
            long l2MissDelta = l2Stats.getMissCount() - lastL2MissCount;
            
            // 保存 L1 统计
            if (l1HitDelta > 0 || l1MissDelta > 0) {
                double l1HitRate = calculateHitRate(l1HitDelta, l1MissDelta);
                saveStatsRecord(now, CacheLevel.L1, l1HitDelta, l1MissDelta, l1HitRate,
                        l1Stats.getEstimatedMemoryUsage(), (int) l1Stats.getEntryCount());
            }
            
            // 保存 L2 统计
            if (l2Stats.isAvailable() && (l2HitDelta > 0 || l2MissDelta > 0)) {
                double l2HitRate = calculateHitRate(l2HitDelta, l2MissDelta);
                saveStatsRecord(now, CacheLevel.L2, l2HitDelta, l2MissDelta, l2HitRate,
                        l2Stats.getMemoryUsage(), (int) l2Stats.getKeyCount());
            }
            
            // 更新上次统计值
            lastL1HitCount = l1Stats.getHitCount();
            lastL1MissCount = l1Stats.getMissCount();
            lastL2HitCount = l2Stats.getHitCount();
            lastL2MissCount = l2Stats.getMissCount();
            
            log.debug("缓存统计已持久化 - L1: hit={}, miss={}, L2: hit={}, miss={}",
                    l1HitDelta, l1MissDelta, l2HitDelta, l2MissDelta);
        } catch (Exception e) {
            log.error("持久化缓存统计失败: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public List<CacheStatsRecord> getHistoryStats(LocalDateTime startTime, LocalDateTime endTime, CacheLevel cacheLevel) {
        String sql;
        Object[] params;
        
        if (cacheLevel != null) {
            sql = "SELECT id, stat_time, cache_level, hit_count, miss_count, hit_rate, memory_usage, entry_count " +
                  "FROM cache_stats WHERE stat_time BETWEEN ? AND ? AND cache_level = ? ORDER BY stat_time DESC";
            params = new Object[]{startTime, endTime, cacheLevel.getCode()};
        } else {
            sql = "SELECT id, stat_time, cache_level, hit_count, miss_count, hit_rate, memory_usage, entry_count " +
                  "FROM cache_stats WHERE stat_time BETWEEN ? AND ? ORDER BY stat_time DESC";
            params = new Object[]{startTime, endTime};
        }
        
        try {
            return jdbcTemplate.query(sql, params, (rs, rowNum) -> CacheStatsRecord.builder()
                    .id(rs.getLong("id"))
                    .statTime(rs.getTimestamp("stat_time").toLocalDateTime())
                    .cacheLevel(rs.getString("cache_level"))
                    .hitCount(rs.getLong("hit_count"))
                    .missCount(rs.getLong("miss_count"))
                    .hitRate(rs.getDouble("hit_rate"))
                    .memoryUsage(rs.getLong("memory_usage"))
                    .entryCount(rs.getInt("entry_count"))
                    .build());
        } catch (Exception e) {
            log.warn("查询历史统计失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    @Override
    public double getAverageHitRate(LocalDateTime startTime, LocalDateTime endTime, CacheLevel cacheLevel) {
        String sql = "SELECT AVG(hit_rate) FROM cache_stats WHERE stat_time BETWEEN ? AND ? AND cache_level = ?";
        
        try {
            Double avgRate = jdbcTemplate.queryForObject(sql, Double.class, 
                    startTime, endTime, cacheLevel.getCode());
            return avgRate != null ? avgRate : 0.0;
        } catch (Exception e) {
            log.warn("查询平均命中率失败: {}", e.getMessage());
            return 0.0;
        }
    }
    
    @Override
    public void resetStats() {
        l2HitCounter.set(0);
        l2MissCounter.set(0);
        lastL1HitCount = 0;
        lastL1MissCount = 0;
        lastL2HitCount = 0;
        lastL2MissCount = 0;
        log.info("缓存统计计数器已重置");
    }
    
    @Override
    public CacheHealthStatus getHealthStatus() {
        L1CacheStats l1Stats = getL1Stats();
        L2CacheStats l2Stats = getL2Stats();
        
        CacheHealthStatus.HealthLevel l1Level = evaluateHealthLevel(l1Stats.getHitRate());
        CacheHealthStatus.HealthLevel l2Level = l2Stats.isAvailable() ? 
                evaluateHealthLevel(l2Stats.getHitRate()) : 
                CacheHealthStatus.HealthLevel.UNAVAILABLE;
        
        // 计算总体命中率
        long totalHits = l1Stats.getHitCount() + l2Stats.getHitCount();
        long totalMisses = l2Stats.getMissCount(); // L1 miss 会查 L2，所以只算 L2 miss
        double overallHitRate = calculateHitRate(totalHits, totalMisses);
        
        CacheHealthStatus.HealthLevel overallLevel = evaluateHealthLevel(overallHitRate);
        
        CacheHealthStatus status = CacheHealthStatus.builder()
                .overallStatus(overallLevel)
                .l1Status(l1Level)
                .l2Status(l2Level)
                .l1HitRate(l1Stats.getHitRate())
                .l2HitRate(l2Stats.getHitRate())
                .overallHitRate(overallHitRate)
                .l1MemoryUsageRate(l1Stats.getUsageRate())
                .l2MemoryUsageRate(l2Stats.getMemoryUsageRate())
                .build();
        
        // 添加健康问题和建议
        addHealthIssuesAndSuggestions(status, l1Stats, l2Stats);
        
        return status;
    }

    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 获取 Redis 信息
     */
    private Properties getRedisInfo(String section) {
        if (redisTemplate == null) {
            return null;
        }
        
        try {
            return redisTemplate.execute((RedisCallback<Properties>) connection -> 
                    connection.serverCommands().info(section));
        } catch (Exception e) {
            log.debug("获取 Redis {} 信息失败: {}", section, e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取 L2 缓存键数量
     */
    private long getL2KeyCount() {
        if (redisTemplate == null) {
            return 0;
        }
        
        try {
            Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.debug("获取 L2 缓存键数量失败: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * 计算命中率
     */
    private double calculateHitRate(long hitCount, long missCount) {
        long total = hitCount + missCount;
        if (total == 0) {
            return 0.0;
        }
        return (double) hitCount / total;
    }
    
    /**
     * 评估健康级别
     */
    private CacheHealthStatus.HealthLevel evaluateHealthLevel(double hitRate) {
        if (hitRate >= HEALTHY_HIT_RATE_THRESHOLD) {
            return CacheHealthStatus.HealthLevel.HEALTHY;
        } else if (hitRate >= WARNING_HIT_RATE_THRESHOLD) {
            return CacheHealthStatus.HealthLevel.WARNING;
        } else {
            return CacheHealthStatus.HealthLevel.CRITICAL;
        }
    }
    
    /**
     * 添加健康问题和建议
     */
    private void addHealthIssuesAndSuggestions(CacheHealthStatus status, 
                                                L1CacheStats l1Stats, 
                                                L2CacheStats l2Stats) {
        // L1 缓存问题检查
        if (l1Stats.getHitRate() < WARNING_HIT_RATE_THRESHOLD) {
            status.addIssue("L1 缓存命中率过低 (" + String.format("%.1f%%", l1Stats.getHitRate() * 100) + ")");
            status.addSuggestion("考虑增加 L1 缓存容量或调整 TTL 配置");
        }
        
        if (l1Stats.getUsageRate() > 0.9) {
            status.addIssue("L1 缓存使用率过高 (" + String.format("%.1f%%", l1Stats.getUsageRate() * 100) + ")");
            status.addSuggestion("考虑增加 L1 缓存最大条目数");
        }
        
        // L2 缓存问题检查
        if (!l2Stats.isAvailable()) {
            status.addIssue("L2 缓存（Redis）不可用");
            status.addSuggestion("检查 Redis 服务状态和连接配置");
        } else {
            if (l2Stats.getHitRate() < WARNING_HIT_RATE_THRESHOLD) {
                status.addIssue("L2 缓存命中率过低 (" + String.format("%.1f%%", l2Stats.getHitRate() * 100) + ")");
                status.addSuggestion("考虑调整 L2 缓存 TTL 或检查缓存失效策略");
            }
            
            if (l2Stats.getMemoryUsageRate() > 0.8) {
                status.addIssue("L2 缓存内存使用率过高 (" + String.format("%.1f%%", l2Stats.getMemoryUsageRate() * 100) + ")");
                status.addSuggestion("考虑增加 Redis 内存限制或清理过期数据");
            }
        }
        
        // 总体命中率检查
        if (status.getOverallHitRate() < HEALTHY_HIT_RATE_THRESHOLD) {
            status.addIssue("总体缓存命中率低于 70%");
            status.addSuggestion("建议分析热点数据并优化缓存预热策略");
        }
    }
    
    /**
     * 保存统计记录到数据库
     */
    private void saveStatsRecord(LocalDateTime statTime, CacheLevel cacheLevel,
                                  long hitCount, long missCount, double hitRate,
                                  long memoryUsage, int entryCount) {
        String sql = "INSERT INTO cache_stats (stat_time, cache_level, hit_count, miss_count, hit_rate, memory_usage, entry_count) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try {
            jdbcTemplate.update(sql, statTime, cacheLevel.getCode(), hitCount, missCount, 
                    hitRate, memoryUsage, entryCount);
        } catch (Exception e) {
            log.warn("保存缓存统计记录失败: {}", e.getMessage());
        }
    }
    
    /**
     * 从 Properties 解析 long 值
     */
    private long parseLong(Properties props, String key, long defaultValue) {
        if (props == null) {
            return defaultValue;
        }
        String value = props.getProperty(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 从 Properties 解析 int 值
     */
    private int parseInt(Properties props, String key, int defaultValue) {
        if (props == null) {
            return defaultValue;
        }
        String value = props.getProperty(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
