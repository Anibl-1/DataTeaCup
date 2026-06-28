package com.dataplatform.data.service.cache;

import java.util.List;

/**
 * 缓存预热器接口
 * 
 * 用于在系统启动时预热热点数据到缓存，提升首次访问性能。
 * 
 * 功能特性：
 * - 系统启动时自动预热热点数据
 * - 支持手动触发预热指定的键
 * - 从 HotspotDetector 获取热点键列表
 * - 支持异步预热以避免阻塞启动
 * 
 * 需求引用：
 * - 需求 8.2: 系统启动时预热热点数据到缓存
 * 
 * @see HotspotDetector 热点数据检测器
 * @see MultiLevelCacheManager 多级缓存管理器
 */
public interface CacheWarmer {
    
    /**
     * 预热热点数据
     * 
     * 从 HotspotDetector 获取热点键列表，并将对应数据加载到缓存。
     * 此方法通常在系统启动时自动调用。
     */
    void warmUp();
    
    /**
     * 预热指定数据
     * 
     * 手动触发预热指定的缓存键列表。
     * 
     * @param keys 要预热的缓存键列表
     */
    void warmUp(List<String> keys);
    
    /**
     * 异步预热热点数据
     * 
     * 在后台线程中执行预热操作，不阻塞主线程。
     * 适用于系统启动时避免阻塞启动流程。
     */
    void warmUpAsync();
    
    /**
     * 异步预热指定数据
     * 
     * 在后台线程中预热指定的缓存键列表。
     * 
     * @param keys 要预热的缓存键列表
     */
    void warmUpAsync(List<String> keys);
    
    /**
     * 检查预热是否完成
     * 
     * @return 如果预热已完成返回 true
     */
    boolean isWarmUpComplete();
    
    /**
     * 获取预热进度
     * 
     * @return 预热进度百分比（0-100）
     */
    int getWarmUpProgress();
    
    /**
     * 获取已预热的键数量
     * 
     * @return 已成功预热的键数量
     */
    int getWarmedKeyCount();
    
    /**
     * 获取预热失败的键数量
     * 
     * @return 预热失败的键数量
     */
    int getFailedKeyCount();
    
    /**
     * 应用启动完成后自动预热回调
     * 
     * 使用 ApplicationReadyEvent 确保在所有 Bean 初始化完成后执行
     */
    void onApplicationReady();
    
    /**
     * 注册预热数据加载器
     * 
     * 注册一个数据加载器，用于根据缓存键加载实际数据。
     * 预热器会调用此加载器获取数据并写入缓存。
     * 
     * @param loader 数据加载器
     */
    void registerDataLoader(CacheDataLoader loader);
    
    /**
     * 缓存数据加载器接口
     * 
     * 用于根据缓存键加载实际数据
     */
    @FunctionalInterface
    interface CacheDataLoader {
        /**
         * 根据缓存键加载数据
         * 
         * @param key 缓存键
         * @return 加载的数据，如果无法加载返回 null
         */
        Object load(String key);
    }
}
