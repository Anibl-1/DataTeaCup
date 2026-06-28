package com.dataplatform.data.service.cache;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 缓存预热器实现
 * 
 * 在系统启动时自动预热热点数据到缓存，提升首次访问性能。
 * 
 * 实现特性：
 * - 使用 @EventListener(ApplicationReadyEvent) 在应用启动完成后自动预热
 * - 支持异步预热，避免阻塞启动流程
 * - 从 HotspotDetector 获取热点键列表
 * - 通过注册的 CacheDataLoader 加载数据
 * - 支持预热进度跟踪
 * 
 * 需求引用：
 * - 需求 8.2: 系统启动时预热热点数据到缓存
 * 
 * @see CacheWarmer 接口定义
 * @see HotspotDetector 热点数据检测器
 * @see MultiLevelCacheManager 多级缓存管理器
 */
@Slf4j
@Service
public class CacheWarmerImpl implements CacheWarmer {
    
    /**
     * 热点数据检测器
     */
    private final HotspotDetector hotspotDetector;
    
    /**
     * 多级缓存管理器
     */
    private final MultiLevelCacheManager cacheManager;
    
    /**
     * 数据加载器列表
     */
    private final List<CacheDataLoader> dataLoaders = new CopyOnWriteArrayList<>();
    
    /**
     * 预热是否完成
     */
    private final AtomicBoolean warmUpComplete = new AtomicBoolean(false);
    
    /**
     * 已预热的键数量
     */
    private final AtomicInteger warmedKeyCount = new AtomicInteger(0);
    
    /**
     * 预热失败的键数量
     */
    private final AtomicInteger failedKeyCount = new AtomicInteger(0);
    
    /**
     * 总共需要预热的键数量
     */
    private final AtomicInteger totalKeyCount = new AtomicInteger(0);
    
    /**
     * 是否启用自动预热
     */
    @Value("${cache.warmer.enabled:true}")
    private boolean warmUpEnabled;
    
    /**
     * 是否使用异步预热
     */
    @Value("${cache.warmer.async:true}")
    private boolean asyncWarmUp;
    
    @Autowired
    public CacheWarmerImpl(HotspotDetector hotspotDetector, MultiLevelCacheManager cacheManager) {
        this.hotspotDetector = hotspotDetector;
        this.cacheManager = cacheManager;
        log.info("缓存预热器初始化完成");
    }

    /**
     * 应用启动完成后自动预热
     * 
     * 使用 ApplicationReadyEvent 确保在所有 Bean 初始化完成后执行
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!warmUpEnabled) {
            log.info("缓存预热已禁用，跳过自动预热");
            return;
        }
        
        log.info("应用启动完成，开始缓存预热...");
        
        if (asyncWarmUp) {
            warmUpAsync();
        } else {
            warmUp();
        }
    }
    
    @Override
    public void warmUp() {
        log.info("开始同步预热热点数据...");
        
        // 重置状态
        resetState();
        
        // 获取热点键列表
        List<String> hotspots = hotspotDetector.getHotspots();
        
        if (hotspots.isEmpty()) {
            log.info("没有检测到热点数据，跳过预热");
            warmUpComplete.set(true);
            return;
        }
        
        log.info("检测到 {} 个热点键，开始预热", hotspots.size());
        totalKeyCount.set(hotspots.size());
        
        // 执行预热
        doWarmUp(hotspots);
        
        warmUpComplete.set(true);
        log.info("缓存预热完成 - 成功: {}, 失败: {}", warmedKeyCount.get(), failedKeyCount.get());
    }
    
    @Override
    public void warmUp(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            log.debug("预热键列表为空，跳过预热");
            return;
        }
        
        log.info("开始预热指定的 {} 个键", keys.size());
        
        // 重置状态
        resetState();
        totalKeyCount.set(keys.size());
        
        // 执行预热
        doWarmUp(keys);
        
        warmUpComplete.set(true);
        log.info("指定键预热完成 - 成功: {}, 失败: {}", warmedKeyCount.get(), failedKeyCount.get());
    }
    
    @Override
    @Async
    public void warmUpAsync() {
        log.info("开始异步预热热点数据...");
        
        CompletableFuture.runAsync(() -> {
            try {
                warmUp();
            } catch (Exception e) {
                log.error("异步预热失败", e);
                warmUpComplete.set(true);
            }
        });
    }
    
    @Override
    @Async
    public void warmUpAsync(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            log.debug("预热键列表为空，跳过异步预热");
            return;
        }
        
        log.info("开始异步预热指定的 {} 个键", keys.size());
        
        CompletableFuture.runAsync(() -> {
            try {
                warmUp(keys);
            } catch (Exception e) {
                log.error("异步预热指定键失败", e);
                warmUpComplete.set(true);
            }
        });
    }
    
    @Override
    public boolean isWarmUpComplete() {
        return warmUpComplete.get();
    }
    
    @Override
    public int getWarmUpProgress() {
        int total = totalKeyCount.get();
        if (total == 0) {
            return warmUpComplete.get() ? 100 : 0;
        }
        
        int processed = warmedKeyCount.get() + failedKeyCount.get();
        return Math.min(100, (processed * 100) / total);
    }
    
    @Override
    public int getWarmedKeyCount() {
        return warmedKeyCount.get();
    }
    
    @Override
    public int getFailedKeyCount() {
        return failedKeyCount.get();
    }
    
    @Override
    public void registerDataLoader(CacheDataLoader loader) {
        if (loader != null) {
            dataLoaders.add(loader);
            log.debug("注册数据加载器，当前加载器数量: {}", dataLoaders.size());
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 重置预热状态
     */
    private void resetState() {
        warmUpComplete.set(false);
        warmedKeyCount.set(0);
        failedKeyCount.set(0);
        totalKeyCount.set(0);
    }
    
    /**
     * 执行预热操作
     * 
     * @param keys 要预热的键列表
     */
    private void doWarmUp(List<String> keys) {
        for (String key : keys) {
            try {
                boolean loaded = loadAndCacheData(key);
                if (loaded) {
                    warmedKeyCount.incrementAndGet();
                    log.debug("预热成功 - key: {}", key);
                } else {
                    failedKeyCount.incrementAndGet();
                    log.debug("预热跳过（无数据加载器或数据为空） - key: {}", key);
                }
            } catch (Exception e) {
                failedKeyCount.incrementAndGet();
                log.warn("预热失败 - key: {}, error: {}", key, e.getMessage());
            }
        }
    }
    
    /**
     * 加载数据并写入缓存
     * 
     * @param key 缓存键
     * @return 如果成功加载并缓存返回 true
     */
    private boolean loadAndCacheData(String key) {
        // 首先检查缓存中是否已存在
        Object existingValue = cacheManager.get(key, Object.class);
        if (existingValue != null) {
            log.debug("缓存已存在，跳过预热 - key: {}", key);
            return true;
        }
        
        // 尝试使用注册的数据加载器加载数据
        for (CacheDataLoader loader : dataLoaders) {
            try {
                Object data = loader.load(key);
                if (data != null) {
                    cacheManager.put(key, data);
                    return true;
                }
            } catch (Exception e) {
                log.debug("数据加载器加载失败 - key: {}, error: {}", key, e.getMessage());
            }
        }
        
        // 如果没有数据加载器或所有加载器都无法加载数据
        if (dataLoaders.isEmpty()) {
            log.debug("没有注册数据加载器，无法预热 - key: {}", key);
        }
        
        return false;
    }
}
