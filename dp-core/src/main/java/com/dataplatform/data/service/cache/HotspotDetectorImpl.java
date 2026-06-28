package com.dataplatform.data.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 热点数据检测器实现
 * 
 * 使用滑动窗口算法统计访问频率，识别热点数据。
 * 
 * 实现特性：
 * - 滑动窗口：默认1分钟窗口，分为6个10秒的桶
 * - 热点阈值：默认每分钟10次访问
 * - 内存效率：限制最大统计键数量，使用 LRU 淘汰
 * - 线程安全：使用 ConcurrentHashMap 和原子操作
 * 
 * 算法说明：
 * 1. 将时间窗口分为多个桶（bucket），每个桶记录该时间段的访问次数
 * 2. 记录访问时，根据当前时间确定桶索引，增加对应桶的计数
 * 3. 查询时，清理过期桶，汇总所有有效桶的访问次数
 * 4. 如果总访问次数超过阈值，则判定为热点
 * 
 * 需求引用：
 * - 需求 8.1: 自动识别访问频率超过阈值（每分钟10次）的热点数据
 * 
 * @see HotspotDetector 接口定义
 */
@Slf4j
@Service
public class HotspotDetectorImpl implements HotspotDetector {
    
    /**
     * 默认热点阈值：每分钟10次访问
     */
    private static final int DEFAULT_THRESHOLD = 10;
    
    /**
     * 默认时间窗口：1分钟（60000毫秒）
     */
    private static final long DEFAULT_WINDOW_SIZE_MILLIS = 60_000L;
    
    /**
     * 桶数量：将时间窗口分为6个桶，每个桶10秒
     */
    private static final int BUCKET_COUNT = 6;
    
    /**
     * 最大统计键数量：限制内存使用
     */
    private static final int MAX_TRACKED_KEYS = 10000;
    
    /**
     * 访问记录存储
     * key: 缓存键
     * value: 访问记录（包含各桶的访问计数）
     */
    private final ConcurrentHashMap<String, AccessRecord> accessRecords = new ConcurrentHashMap<>();
    
    /**
     * 热点阈值（可配置）
     */
    private volatile int threshold = DEFAULT_THRESHOLD;
    
    /**
     * 时间窗口大小（毫秒）
     */
    private final long windowSizeMillis = DEFAULT_WINDOW_SIZE_MILLIS;
    
    /**
     * 每个桶的时间跨度（毫秒）
     */
    private final long bucketSizeMillis = windowSizeMillis / BUCKET_COUNT;
    
    @Override
    public void recordAccess(String key) {
        if (key == null || key.isEmpty()) {
            return;
        }
        
        // 检查是否需要清理过期记录（内存效率）
        if (accessRecords.size() >= MAX_TRACKED_KEYS) {
            cleanupOldRecords();
        }
        
        long currentTime = System.currentTimeMillis();
        int bucketIndex = getBucketIndex(currentTime);
        
        // 获取或创建访问记录
        AccessRecord record = accessRecords.computeIfAbsent(key, k -> new AccessRecord());
        
        // 清理过期桶并增加当前桶计数
        record.cleanExpiredBuckets(currentTime, bucketSizeMillis, BUCKET_COUNT);
        record.incrementBucket(bucketIndex, currentTime);
        
        if (log.isTraceEnabled()) {
            log.trace("记录访问 - key: {}, bucket: {}, count: {}", 
                    key, bucketIndex, record.getTotalCount(currentTime, bucketSizeMillis, BUCKET_COUNT));
        }
    }
    
    @Override
    public boolean isHotspot(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        
        AccessRecord record = accessRecords.get(key);
        if (record == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        int totalCount = record.getTotalCount(currentTime, bucketSizeMillis, BUCKET_COUNT);
        
        boolean isHot = totalCount >= threshold;
        
        if (log.isDebugEnabled() && isHot) {
            log.debug("检测到热点 - key: {}, count: {}, threshold: {}", key, totalCount, threshold);
        }
        
        return isHot;
    }
    
    @Override
    public List<String> getHotspots() {
        long currentTime = System.currentTimeMillis();
        
        List<String> hotspots = accessRecords.entrySet().stream()
                .filter(entry -> {
                    int count = entry.getValue().getTotalCount(currentTime, bucketSizeMillis, BUCKET_COUNT);
                    return count >= threshold;
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        if (log.isDebugEnabled() && !hotspots.isEmpty()) {
            log.debug("获取热点列表 - count: {}, threshold: {}", hotspots.size(), threshold);
        }
        
        return hotspots;
    }
    
    @Override
    public int getAccessCount(String key) {
        if (key == null || key.isEmpty()) {
            return 0;
        }
        
        AccessRecord record = accessRecords.get(key);
        if (record == null) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        return record.getTotalCount(currentTime, bucketSizeMillis, BUCKET_COUNT);
    }
    
    @Override
    public int getThreshold() {
        return threshold;
    }
    
    @Override
    public void setThreshold(int threshold) {
        if (threshold < 1) {
            throw new IllegalArgumentException("阈值必须大于0");
        }
        this.threshold = threshold;
        log.info("热点阈值已更新 - threshold: {}", threshold);
    }
    
    @Override
    public long getWindowSizeMillis() {
        return windowSizeMillis;
    }
    
    @Override
    public void clear() {
        accessRecords.clear();
        log.info("热点检测器已清空所有访问记录");
    }
    
    @Override
    public int getTrackedKeyCount() {
        return accessRecords.size();
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 根据时间戳计算桶索引
     */
    private int getBucketIndex(long timestamp) {
        return (int) ((timestamp / bucketSizeMillis) % BUCKET_COUNT);
    }
    
    /**
     * 清理过期的访问记录
     * 使用 LRU 策略，移除最久未访问的记录
     */
    private void cleanupOldRecords() {
        long currentTime = System.currentTimeMillis();
        
        // 移除所有过期的记录（窗口外的记录）
        accessRecords.entrySet().removeIf(entry -> {
            int count = entry.getValue().getTotalCount(currentTime, bucketSizeMillis, BUCKET_COUNT);
            return count == 0;
        });
        
        // 如果仍然超过限制，移除访问次数最少的记录
        if (accessRecords.size() >= MAX_TRACKED_KEYS) {
            int toRemove = accessRecords.size() - MAX_TRACKED_KEYS + MAX_TRACKED_KEYS / 10;
            
            accessRecords.entrySet().stream()
                    .sorted(Comparator.comparingInt(e -> 
                            e.getValue().getTotalCount(currentTime, bucketSizeMillis, BUCKET_COUNT)))
                    .limit(toRemove)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList())
                    .forEach(accessRecords::remove);
            
            log.debug("清理过期访问记录 - 移除 {} 条", toRemove);
        }
    }
    
    /**
     * 访问记录内部类
     * 使用桶数组记录各时间段的访问次数
     */
    private static class AccessRecord {
        /**
         * 各桶的访问计数
         */
        private final AtomicInteger[] buckets = new AtomicInteger[BUCKET_COUNT];
        
        /**
         * 各桶的最后更新时间戳
         */
        private final long[] bucketTimestamps = new long[BUCKET_COUNT];
        
        /**
         * 同步锁
         */
        private final Object lock = new Object();
        
        AccessRecord() {
            for (int i = 0; i < BUCKET_COUNT; i++) {
                buckets[i] = new AtomicInteger(0);
                bucketTimestamps[i] = 0;
            }
        }
        
        /**
         * 增加指定桶的计数
         */
        void incrementBucket(int bucketIndex, long currentTime) {
            synchronized (lock) {
                long bucketTime = bucketTimestamps[bucketIndex];
                long timeDiff = currentTime - bucketTime;
                
                // 如果桶已过期（超过一个完整窗口），重置计数
                if (timeDiff >= BUCKET_COUNT * (currentTime / BUCKET_COUNT - bucketTime / BUCKET_COUNT)) {
                    // 简化判断：如果时间戳差异过大，重置
                    if (bucketTime == 0 || timeDiff > 60_000L) {
                        buckets[bucketIndex].set(1);
                    } else {
                        buckets[bucketIndex].incrementAndGet();
                    }
                } else {
                    buckets[bucketIndex].incrementAndGet();
                }
                bucketTimestamps[bucketIndex] = currentTime;
            }
        }
        
        /**
         * 清理过期的桶
         */
        void cleanExpiredBuckets(long currentTime, long bucketSizeMillis, int bucketCount) {
            synchronized (lock) {
                long windowStart = currentTime - (bucketSizeMillis * bucketCount);
                
                for (int i = 0; i < bucketCount; i++) {
                    if (bucketTimestamps[i] < windowStart) {
                        buckets[i].set(0);
                        bucketTimestamps[i] = 0;
                    }
                }
            }
        }
        
        /**
         * 获取总访问次数（所有有效桶的总和）
         */
        int getTotalCount(long currentTime, long bucketSizeMillis, int bucketCount) {
            synchronized (lock) {
                long windowStart = currentTime - (bucketSizeMillis * bucketCount);
                int total = 0;
                
                for (int i = 0; i < bucketCount; i++) {
                    if (bucketTimestamps[i] >= windowStart) {
                        total += buckets[i].get();
                    }
                }
                
                return total;
            }
        }
    }
}
