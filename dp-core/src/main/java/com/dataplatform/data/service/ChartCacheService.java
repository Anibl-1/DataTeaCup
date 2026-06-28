package com.dataplatform.data.service;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 图表数据缓存服务
 * 基于统一缓存服务，支持Redis和本地缓存自动切换
 * 
 * @author dataplatform
 */
@Slf4j
@Service
public class ChartCacheService {
    
    private static final String CACHE_PREFIX = "chart:data:";
    
    @Autowired
    private CacheService cacheService;
    
    @Value("${cache.chart.ttl:300}")
    private long defaultTtlSeconds;
    
    /**
     * 生成缓存键
     */
    public String generateKey(Long chartId, String filters, Integer limit) {
        return generateKey(chartId, filters, limit, null);
    }
    
    /**
     * 生成缓存键（包含参数）
     */
    public String generateKey(Long chartId, String filters, Integer limit, String parameters) {
        int filtersHash = filters != null ? filters.hashCode() : 0;
        int paramsHash = parameters != null ? parameters.hashCode() : 0;
        return CACHE_PREFIX + String.format("%d:%d:%d:%d", chartId, filtersHash, limit, paramsHash);
    }
    
    /**
     * 获取缓存
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> get(String key) {
        return cacheService.get(key, new TypeReference<List<Map<String, Object>>>() {});
    }
    
    /**
     * 设置缓存
     */
    public void set(String key, List<Map<String, Object>> data) {
        set(key, data, defaultTtlSeconds);
    }
    
    /**
     * 设置缓存（自定义TTL，单位秒）
     */
    public void set(String key, List<Map<String, Object>> data, long ttlSeconds) {
        cacheService.set(key, data, ttlSeconds, TimeUnit.SECONDS);
    }
    
    /**
     * 删除缓存
     */
    public void delete(String key) {
        cacheService.delete(key);
    }
    
    /**
     * 清除指定图表的所有缓存
     */
    public void clearChart(Long chartId) {
        cacheService.deleteByPattern(CACHE_PREFIX + chartId + ":*");
    }
    
    /**
     * 清除所有图表缓存
     */
    public void clear() {
        cacheService.deleteByPattern(CACHE_PREFIX + "*");
    }
    
    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getStats() {
        return cacheService.getStats();
    }
    
    /**
     * 预热缓存
     */
    public void warmUp(Long chartId, java.util.function.Supplier<List<Map<String, Object>>> dataSupplier) {
        String key = generateKey(chartId, null, 10000);
        if (get(key) == null) {
            try {
                List<Map<String, Object>> data = dataSupplier.get();
                set(key, data);
                log.debug("图表缓存预热成功: chartId={}", chartId);
            } catch (Exception e) {
                log.warn("图表缓存预热失败: chartId={}, error={}", chartId, e.getMessage());
            }
        }
    }
}
