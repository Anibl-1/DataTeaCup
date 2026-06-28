package com.dataplatform.data.service.cache;

import java.util.List;

/**
 * 热点数据检测器接口
 * 
 * 用于识别访问频率超过阈值的热点数据，支持：
 * - 记录键的访问
 * - 判断键是否为热点
 * - 获取当前所有热点键列表
 * 
 * 热点判定规则：
 * - 默认阈值：1分钟内访问超过10次
 * - 使用滑动窗口算法统计访问频率
 * 
 * 需求引用：
 * - 需求 8.1: 自动识别访问频率超过阈值（每分钟10次）的热点数据
 * 
 * @see HotspotDetectorImpl 默认实现
 * @see CacheWarmer 缓存预热器（使用热点数据）
 */
public interface HotspotDetector {
    
    /**
     * 记录键的访问
     * 
     * 每次访问缓存键时调用此方法，用于统计访问频率
     * 
     * @param key 被访问的缓存键
     */
    void recordAccess(String key);
    
    /**
     * 判断键是否为热点
     * 
     * 根据访问频率判断键是否达到热点阈值
     * 默认阈值：1分钟内访问超过10次
     * 
     * @param key 要检查的缓存键
     * @return 如果是热点返回 true，否则返回 false
     */
    boolean isHotspot(String key);
    
    /**
     * 获取当前所有热点键列表
     * 
     * 返回当前被识别为热点的所有缓存键
     * 
     * @return 热点键列表，如果没有热点则返回空列表
     */
    List<String> getHotspots();
    
    /**
     * 获取键的当前访问频率
     * 
     * 返回指定键在当前时间窗口内的访问次数
     * 
     * @param key 要查询的缓存键
     * @return 访问次数，如果键不存在则返回 0
     */
    int getAccessCount(String key);
    
    /**
     * 获取热点阈值
     * 
     * @return 当前配置的热点阈值（每分钟访问次数）
     */
    int getThreshold();
    
    /**
     * 设置热点阈值
     * 
     * @param threshold 新的热点阈值（每分钟访问次数）
     */
    void setThreshold(int threshold);
    
    /**
     * 获取时间窗口大小
     * 
     * @return 时间窗口大小（毫秒）
     */
    long getWindowSizeMillis();
    
    /**
     * 清除所有访问记录
     * 
     * 重置所有统计数据，通常用于测试或系统重置
     */
    void clear();
    
    /**
     * 获取统计的键数量
     * 
     * @return 当前正在统计的键数量
     */
    int getTrackedKeyCount();
}
