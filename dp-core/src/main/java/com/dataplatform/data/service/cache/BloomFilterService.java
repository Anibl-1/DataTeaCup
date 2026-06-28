package com.dataplatform.data.service.cache;

/**
 * 布隆过滤器服务接口
 * 
 * 用于防止缓存穿透攻击。布隆过滤器是一种空间效率极高的概率型数据结构，
 * 用于判断一个元素是否在集合中。
 * 
 * 特性：
 * - 如果返回 false，则元素一定不存在
 * - 如果返回 true，则元素可能存在（存在误判率）
 * - 不支持删除操作
 * 
 * 使用场景：
 * - 在缓存查询前先检查布隆过滤器
 * - 如果键不存在于布隆过滤器中，直接返回空结果，避免穿透到数据库
 * 
 * 需求引用：
 * - 需求 8.4: 使用布隆过滤器防止缓存穿透攻击
 * - 需求 8.6: 如果缓存键不存在于布隆过滤器中，直接返回空结果而不查询数据库
 * 
 * @see BloomFilterServiceImpl 默认实现
 */
public interface BloomFilterService {
    
    /**
     * 添加键到布隆过滤器
     * 
     * @param key 要添加的键，null 或空字符串将被忽略
     */
    void add(String key);
    
    /**
     * 检查键是否可能存在
     * 
     * 注意：
     * - 返回 false 表示键一定不存在
     * - 返回 true 表示键可能存在（存在误判率）
     * 
     * @param key 要检查的键
     * @return 如果键可能存在返回 true，如果键一定不存在返回 false
     */
    boolean mightContain(String key);
    
    /**
     * 批量添加键到布隆过滤器
     * 
     * @param keys 要添加的键集合
     */
    void addAll(Iterable<String> keys);
    
    /**
     * 重建布隆过滤器
     * 
     * 当数据变化较大时，可以重建布隆过滤器以清除旧数据。
     * 重建后需要重新添加所有有效的键。
     */
    void rebuild();
    
    /**
     * 重建布隆过滤器并使用新的配置
     * 
     * @param expectedInsertions 预期插入的元素数量
     * @param falsePositiveRate  期望的误判率（0 到 1 之间）
     */
    void rebuild(long expectedInsertions, double falsePositiveRate);
    
    /**
     * 获取当前布隆过滤器的预期元素数量
     * 
     * @return 预期元素数量
     */
    long getExpectedInsertions();
    
    /**
     * 获取当前布隆过滤器的误判率
     * 
     * @return 误判率（0 到 1 之间）
     */
    double getFalsePositiveRate();
    
    /**
     * 获取已添加的元素数量（近似值）
     * 
     * @return 已添加的元素数量
     */
    long getApproximateElementCount();
    
    /**
     * 检查布隆过滤器是否已初始化
     * 
     * @return 如果已初始化返回 true
     */
    boolean isInitialized();
}
