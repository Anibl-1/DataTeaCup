package com.dataplatform.data.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * TTL 随机偏移器实现
 * 
 * 为缓存 TTL 添加 ±10% 的随机偏移量，防止缓存雪崩。
 * 
 * 实现原理：
 * - 使用 ThreadLocalRandom 生成随机数，保证线程安全和高性能
 * - 偏移范围为 [TTL * (1 - offset), TTL * (1 + offset)]
 * - 默认偏移百分比为 10%，即范围为 [TTL * 0.9, TTL * 1.1]
 * 
 * 示例：
 * - 原始 TTL = 300 秒（5分钟）
 * - 随机化后 TTL 范围 = [270, 330] 秒
 * 
 * 需求引用：
 * - 需求 8.5: THE Cache_Manager SHALL 为缓存 TTL 添加随机偏移量（±10%），防止缓存雪崩
 * 
 * @see TtlRandomizer 接口定义
 */
@Slf4j
@Component
public class TtlRandomizerImpl implements TtlRandomizer {
    
    /**
     * 最小 TTL 值（毫秒），防止 TTL 过小
     */
    private static final long MIN_TTL_MILLIS = 100;
    
    @Override
    public long randomize(long originalTtl, TimeUnit unit) {
        return randomize(originalTtl, unit, DEFAULT_OFFSET_PERCENTAGE);
    }
    
    @Override
    public long randomizeMillis(long originalTtlMillis) {
        return randomize(originalTtlMillis, TimeUnit.MILLISECONDS, DEFAULT_OFFSET_PERCENTAGE);
    }
    
    @Override
    public long randomize(long originalTtl, TimeUnit unit, double offsetPercentage) {
        if (originalTtl <= 0) {
            log.warn("无效的 TTL 值: {}，返回原值", originalTtl);
            return originalTtl;
        }
        
        if (offsetPercentage < 0 || offsetPercentage > 1) {
            log.warn("无效的偏移百分比: {}，使用默认值 {}", offsetPercentage, DEFAULT_OFFSET_PERCENTAGE);
            offsetPercentage = DEFAULT_OFFSET_PERCENTAGE;
        }
        
        // 转换为毫秒进行计算，保证精度
        long originalMillis = unit.toMillis(originalTtl);
        
        // 计算偏移范围
        long offsetMillis = (long) (originalMillis * offsetPercentage);
        long minMillis = Math.max(MIN_TTL_MILLIS, originalMillis - offsetMillis);
        long maxMillis = originalMillis + offsetMillis;
        
        // 生成随机 TTL（毫秒）
        long randomizedMillis = ThreadLocalRandom.current().nextLong(minMillis, maxMillis + 1);
        
        // 转换回原始时间单位
        long randomizedTtl = convertFromMillis(randomizedMillis, unit);
        
        // 确保结果至少为 1
        randomizedTtl = Math.max(1, randomizedTtl);
        
        log.debug("TTL 随机化: 原始={} {}, 随机化后={} {}, 范围=[{}, {}] {}",
                originalTtl, unit, randomizedTtl, unit,
                convertFromMillis(minMillis, unit), convertFromMillis(maxMillis, unit), unit);
        
        return randomizedTtl;
    }
    
    @Override
    public long getMinTtl(long originalTtl, TimeUnit unit) {
        return getMinTtl(originalTtl, unit, DEFAULT_OFFSET_PERCENTAGE);
    }
    
    @Override
    public long getMaxTtl(long originalTtl, TimeUnit unit) {
        return getMaxTtl(originalTtl, unit, DEFAULT_OFFSET_PERCENTAGE);
    }
    
    @Override
    public boolean isInValidRange(long ttl, long originalTtl, TimeUnit unit) {
        return isInValidRange(ttl, originalTtl, unit, DEFAULT_OFFSET_PERCENTAGE);
    }
    
    /**
     * 获取 TTL 的最小值
     * 
     * @param originalTtl      原始 TTL 值
     * @param unit             时间单位
     * @param offsetPercentage 偏移百分比
     * @return 最小 TTL 值
     */
    public long getMinTtl(long originalTtl, TimeUnit unit, double offsetPercentage) {
        if (originalTtl <= 0) {
            return originalTtl;
        }
        
        long originalMillis = unit.toMillis(originalTtl);
        long offsetMillis = (long) (originalMillis * offsetPercentage);
        long minMillis = Math.max(MIN_TTL_MILLIS, originalMillis - offsetMillis);
        
        return Math.max(1, convertFromMillis(minMillis, unit));
    }
    
    /**
     * 获取 TTL 的最大值
     * 
     * @param originalTtl      原始 TTL 值
     * @param unit             时间单位
     * @param offsetPercentage 偏移百分比
     * @return 最大 TTL 值
     */
    public long getMaxTtl(long originalTtl, TimeUnit unit, double offsetPercentage) {
        if (originalTtl <= 0) {
            return originalTtl;
        }
        
        long originalMillis = unit.toMillis(originalTtl);
        long offsetMillis = (long) (originalMillis * offsetPercentage);
        long maxMillis = originalMillis + offsetMillis;
        
        return convertFromMillis(maxMillis, unit);
    }
    
    /**
     * 检查给定的 TTL 是否在有效范围内
     * 
     * @param ttl              要检查的 TTL 值
     * @param originalTtl      原始 TTL 值
     * @param unit             时间单位
     * @param offsetPercentage 偏移百分比
     * @return 如果 TTL 在有效范围内返回 true
     */
    public boolean isInValidRange(long ttl, long originalTtl, TimeUnit unit, double offsetPercentage) {
        if (originalTtl <= 0) {
            return ttl == originalTtl;
        }
        
        long minTtl = getMinTtl(originalTtl, unit, offsetPercentage);
        long maxTtl = getMaxTtl(originalTtl, unit, offsetPercentage);
        
        return ttl >= minTtl && ttl <= maxTtl;
    }
    
    /**
     * 将毫秒转换为指定时间单位
     * 
     * @param millis 毫秒值
     * @param unit   目标时间单位
     * @return 转换后的值
     */
    private long convertFromMillis(long millis, TimeUnit unit) {
        switch (unit) {
            case NANOSECONDS:
                return TimeUnit.MILLISECONDS.toNanos(millis);
            case MICROSECONDS:
                return TimeUnit.MILLISECONDS.toMicros(millis);
            case MILLISECONDS:
                return millis;
            case SECONDS:
                return TimeUnit.MILLISECONDS.toSeconds(millis);
            case MINUTES:
                return TimeUnit.MILLISECONDS.toMinutes(millis);
            case HOURS:
                return TimeUnit.MILLISECONDS.toHours(millis);
            case DAYS:
                return TimeUnit.MILLISECONDS.toDays(millis);
            default:
                return millis;
        }
    }
}
