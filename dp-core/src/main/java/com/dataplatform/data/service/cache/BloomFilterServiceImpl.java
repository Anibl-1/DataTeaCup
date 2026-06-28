package com.dataplatform.data.service.cache;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 布隆过滤器服务实现
 * 
 * 使用 Guava 的 BloomFilter 实现，用于防止缓存穿透攻击。
 * 
 * 配置：
 * - 默认预期元素数量：1,000,000（100万）
 * - 默认误判率：0.01（1%）
 * 
 * 线程安全：
 * - 使用 ReadWriteLock 保证线程安全
 * - 读操作（mightContain）使用读锁，支持并发读
 * - 写操作（add、rebuild）使用写锁，保证原子性
 * 
 * 需求引用：
 * - 需求 8.4: 使用布隆过滤器防止缓存穿透攻击
 * - 需求 8.6: 如果缓存键不存在于布隆过滤器中，直接返回空结果而不查询数据库
 * 
 * @see BloomFilterService
 */
@Slf4j
@Service
public class BloomFilterServiceImpl implements BloomFilterService {
    
    /**
     * 默认预期元素数量：100万
     */
    private static final long DEFAULT_EXPECTED_INSERTIONS = 1_000_000L;
    
    /**
     * 默认误判率：1%
     */
    private static final double DEFAULT_FALSE_POSITIVE_RATE = 0.01;
    
    /**
     * Guava 布隆过滤器实例
     */
    private volatile BloomFilter<CharSequence> bloomFilter;
    
    /**
     * 读写锁，保证线程安全
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * 当前配置的预期元素数量
     */
    private volatile long expectedInsertions = DEFAULT_EXPECTED_INSERTIONS;
    
    /**
     * 当前配置的误判率
     */
    private volatile double falsePositiveRate = DEFAULT_FALSE_POSITIVE_RATE;
    
    /**
     * 已添加的元素计数（近似值）
     */
    private final AtomicLong elementCount = new AtomicLong(0);
    
    /**
     * 初始化布隆过滤器
     */
    @PostConstruct
    public void init() {
        createBloomFilter(expectedInsertions, falsePositiveRate);
        log.info("布隆过滤器初始化完成 - expectedInsertions: {}, falsePositiveRate: {}", 
                expectedInsertions, falsePositiveRate);
    }
    
    @Override
    public void add(String key) {
        if (key == null || key.isEmpty()) {
            return;
        }
        
        lock.writeLock().lock();
        try {
            if (bloomFilter != null) {
                boolean added = bloomFilter.put(key);
                if (added) {
                    elementCount.incrementAndGet();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public boolean mightContain(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        
        lock.readLock().lock();
        try {
            if (bloomFilter == null) {
                return false;
            }
            return bloomFilter.mightContain(key);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void addAll(Iterable<String> keys) {
        if (keys == null) {
            return;
        }
        
        lock.writeLock().lock();
        try {
            if (bloomFilter != null) {
                for (String key : keys) {
                    if (key != null && !key.isEmpty()) {
                        boolean added = bloomFilter.put(key);
                        if (added) {
                            elementCount.incrementAndGet();
                        }
                    }
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void rebuild() {
        rebuild(expectedInsertions, falsePositiveRate);
    }
    
    @Override
    public void rebuild(long expectedInsertions, double falsePositiveRate) {
        validateParameters(expectedInsertions, falsePositiveRate);
        
        lock.writeLock().lock();
        try {
            this.expectedInsertions = expectedInsertions;
            this.falsePositiveRate = falsePositiveRate;
            createBloomFilter(expectedInsertions, falsePositiveRate);
            elementCount.set(0);
            log.info("布隆过滤器重建完成 - expectedInsertions: {}, falsePositiveRate: {}", 
                    expectedInsertions, falsePositiveRate);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public long getExpectedInsertions() {
        return expectedInsertions;
    }
    
    @Override
    public double getFalsePositiveRate() {
        return falsePositiveRate;
    }
    
    @Override
    public long getApproximateElementCount() {
        return elementCount.get();
    }
    
    @Override
    public boolean isInitialized() {
        lock.readLock().lock();
        try {
            return bloomFilter != null;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 创建布隆过滤器实例
     * 
     * @param expectedInsertions 预期元素数量
     * @param falsePositiveRate  误判率
     */
    private void createBloomFilter(long expectedInsertions, double falsePositiveRate) {
        this.bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                expectedInsertions,
                falsePositiveRate
        );
    }
    
    /**
     * 验证参数有效性
     * 
     * @param expectedInsertions 预期元素数量
     * @param falsePositiveRate  误判率
     */
    private void validateParameters(long expectedInsertions, double falsePositiveRate) {
        if (expectedInsertions <= 0) {
            throw new IllegalArgumentException("预期元素数量必须大于0，当前值: " + expectedInsertions);
        }
        if (falsePositiveRate <= 0 || falsePositiveRate >= 1) {
            throw new IllegalArgumentException("误判率必须在 (0, 1) 范围内，当前值: " + falsePositiveRate);
        }
    }
}
