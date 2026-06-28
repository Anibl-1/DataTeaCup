package com.dataplatform.data.service.cache;

import java.util.concurrent.TimeUnit;

/**
 * TTL 随机偏移器接口
 * 
 * 为缓存 TTL 添加随机偏移量（±10%），防止缓存雪崩。
 * 
 * 缓存雪崩是指大量缓存在同一时间过期，导致大量请求同时穿透到数据库，
 * 造成数据库压力骤增甚至崩溃。通过为 TTL 添加随机偏移，可以使缓存
 * 过期时间分散，避免同时过期的情况。
 * 
 * 需求引用：
 * - 需求 8.5: THE Cache_Manager SHALL 为缓存 TTL 添加随机偏移量（±10%），防止缓存雪崩
 * 
 * @see TtlRandomizerImpl 默认实现
 */
public interface TtlRandomizer {
    
    /**
     * 默认偏移百分比（10%）
     */
    double DEFAULT_OFFSET_PERCENTAGE = 0.10;
    
    /**
     * 为 TTL 添加随机偏移
     * 
     * 返回的 TTL 值在 [originalTtl * 0.9, originalTtl * 1.1] 范围内随机分布。
     * 
     * @param originalTtl 原始 TTL 值
     * @param unit        时间单位
     * @return 添加随机偏移后的 TTL 值（同一时间单位）
     */
    long randomize(long originalTtl, TimeUnit unit);
    
    /**
     * 为 TTL 添加随机偏移（毫秒）
     * 
     * 返回的 TTL 值在 [originalTtlMillis * 0.9, originalTtlMillis * 1.1] 范围内随机分布。
     * 
     * @param originalTtlMillis 原始 TTL 值（毫秒）
     * @return 添加随机偏移后的 TTL 值（毫秒）
     */
    long randomizeMillis(long originalTtlMillis);
    
    /**
     * 为 TTL 添加自定义范围的随机偏移
     * 
     * @param originalTtl      原始 TTL 值
     * @param unit             时间单位
     * @param offsetPercentage 偏移百分比（如 0.10 表示 ±10%）
     * @return 添加随机偏移后的 TTL 值（同一时间单位）
     */
    long randomize(long originalTtl, TimeUnit unit, double offsetPercentage);
    
    /**
     * 获取 TTL 的最小值（原始值的 90%）
     * 
     * @param originalTtl 原始 TTL 值
     * @param unit        时间单位
     * @return 最小 TTL 值
     */
    long getMinTtl(long originalTtl, TimeUnit unit);
    
    /**
     * 获取 TTL 的最大值（原始值的 110%）
     * 
     * @param originalTtl 原始 TTL 值
     * @param unit        时间单位
     * @return 最大 TTL 值
     */
    long getMaxTtl(long originalTtl, TimeUnit unit);
    
    /**
     * 检查给定的 TTL 是否在有效范围内
     * 
     * @param ttl         要检查的 TTL 值
     * @param originalTtl 原始 TTL 值
     * @param unit        时间单位
     * @return 如果 TTL 在 [originalTtl * 0.9, originalTtl * 1.1] 范围内返回 true
     */
    boolean isInValidRange(long ttl, long originalTtl, TimeUnit unit);
}
