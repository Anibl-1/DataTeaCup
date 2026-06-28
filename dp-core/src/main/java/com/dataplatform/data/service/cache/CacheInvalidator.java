package com.dataplatform.data.service.cache;

/**
 * 缓存精准失效接口
 * 
 * 当数据源中的数据发生变更时，需要精准地使相关缓存失效，避免返回过期数据。
 * 
 * 支持的失效方式：
 * 1. 按数据源 ID 失效：当数据源配置变更或数据源数据批量更新时使用
 * 2. 按表名失效：当特定表的数据发生变更时使用
 * 3. 按缓存键模式失效：支持通配符模式匹配
 * 4. 按指定缓存键失效：精确失效单个缓存
 * 
 * 需求引用：
 * - 需求 8.3: 当数据源数据发生变更时，精准失效相关缓存
 * 
 * @see MultiLevelCacheManager
 */
public interface CacheInvalidator {
    
    /**
     * 按数据源 ID 失效缓存
     * 
     * 失效所有与指定数据源相关的缓存。
     * 适用场景：
     * - 数据源配置变更
     * - 数据源数据批量更新
     * - 数据源连接信息变更
     * 
     * @param dataSourceId 数据源 ID
     * @return 失效的缓存键数量
     */
    int invalidateByDataSource(Long dataSourceId);
    
    /**
     * 按表名失效缓存
     * 
     * 失效所有与指定数据源和表名相关的缓存。
     * 适用场景：
     * - 表数据发生 INSERT/UPDATE/DELETE 操作
     * - 表结构变更
     * 
     * @param dataSourceId 数据源 ID
     * @param tableName    表名
     * @return 失效的缓存键数量
     */
    int invalidateByTable(Long dataSourceId, String tableName);
    
    /**
     * 按缓存键模式失效
     * 
     * 支持通配符模式匹配，失效所有匹配的缓存。
     * 模式语法：
     * - * 匹配任意字符序列
     * - ? 匹配单个字符
     * 
     * 示例：
     * - "query:*" 失效所有查询缓存
     * - "ds:1:*" 失效数据源1的所有缓存
     * - "ds:1:table:users:*" 失效数据源1的users表相关缓存
     * 
     * @param pattern 缓存键模式
     * @return 失效的缓存键数量
     */
    int invalidateByPattern(String pattern);
    
    /**
     * 失效指定的缓存键
     * 
     * 精确失效单个缓存键。
     * 
     * @param key 缓存键
     * @return 如果缓存存在并被成功失效返回 true
     */
    boolean invalidate(String key);
    
    /**
     * 批量失效缓存键
     * 
     * 批量失效多个缓存键，比逐个调用 invalidate 更高效。
     * 
     * @param keys 缓存键集合
     * @return 成功失效的缓存键数量
     */
    int invalidateBatch(Iterable<String> keys);
    
    /**
     * 按用户 ID 失效缓存
     * 
     * 失效所有与指定用户相关的缓存。
     * 适用场景：
     * - 用户权限变更
     * - 用户角色变更
     * 
     * @param userId 用户 ID
     * @return 失效的缓存键数量
     */
    int invalidateByUser(Long userId);
    
    /**
     * 按角色 ID 失效缓存
     * 
     * 失效所有与指定角色相关的缓存。
     * 适用场景：
     * - 角色权限变更
     * - 角色数据权限变更
     * 
     * @param roleId 角色 ID
     * @return 失效的缓存键数量
     */
    int invalidateByRole(Long roleId);
    
    /**
     * 清空所有缓存
     * 
     * 谨慎使用，会清空所有缓存数据。
     * 
     * @return 失效的缓存键数量
     */
    int invalidateAll();
}
