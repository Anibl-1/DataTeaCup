package com.dataplatform.data.service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 缓存精准失效实现
 * 
 * 实现数据变更时的缓存精准失效逻辑，支持：
 * - 按数据源 ID 失效
 * - 按表名失效
 * - 按缓存键模式失效
 * - 按用户/角色失效
 * 
 * 与 MultiLevelCacheManager 集成，同时失效 L1（Caffeine）和 L2（Redis）缓存。
 * 
 * 缓存键命名约定：
 * - 查询缓存：dp:cache:query:{hash}
 * - 数据源索引：dp:cache:idx:ds:{dataSourceId}:{cacheKey}
 * - 表索引：dp:cache:idx:table:{dataSourceId}:{tableName}:{cacheKey}
 * - 用户索引：dp:cache:idx:user:{userId}:{cacheKey}
 * - 角色索引：dp:cache:idx:role:{roleId}:{cacheKey}
 * 
 * 需求引用：
 * - 需求 8.3: 当数据源数据发生变更时，精准失效相关缓存
 * 
 * @see CacheInvalidator
 * @see MultiLevelCacheManager
 */
@Slf4j
@Service
@ConditionalOnBean(RedisTemplate.class)
public class CacheInvalidatorImpl implements CacheInvalidator {
    
    /**
     * 缓存键前缀
     */
    private static final String CACHE_KEY_PREFIX = "dp:cache:";
    
    /**
     * 数据源索引前缀
     */
    private static final String DS_INDEX_PREFIX = "dp:cache:idx:ds:";
    
    /**
     * 表索引前缀
     */
    private static final String TABLE_INDEX_PREFIX = "dp:cache:idx:table:";
    
    /**
     * 用户索引前缀
     */
    private static final String USER_INDEX_PREFIX = "dp:cache:idx:user:";
    
    /**
     * 角色索引前缀
     */
    private static final String ROLE_INDEX_PREFIX = "dp:cache:idx:role:";
    
    /**
     * L1 本地缓存（Caffeine）
     */
    private final Cache<String, Object> l1Cache;
    
    /**
     * L2 分布式缓存（Redis）
     */
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    public CacheInvalidatorImpl(
            @Qualifier("l1Cache") Cache<String, Object> l1Cache,
            RedisTemplate<String, Object> redisTemplate) {
        this.l1Cache = l1Cache;
        this.redisTemplate = redisTemplate;
        log.info("缓存失效器初始化完成");
    }
    
    @Override
    public int invalidateByDataSource(Long dataSourceId) {
        if (dataSourceId == null) {
            log.warn("数据源 ID 为空，跳过缓存失效");
            return 0;
        }
        
        log.info("按数据源失效缓存 - dataSourceId: {}", dataSourceId);
        
        // 1. 从索引获取相关缓存键
        String indexPattern = DS_INDEX_PREFIX + dataSourceId + ":*";
        Set<String> cacheKeys = getCacheKeysFromIndex(indexPattern);
        
        // 2. 如果没有索引，使用模式匹配（兼容旧数据）
        if (cacheKeys.isEmpty()) {
            String pattern = "*:ds:" + dataSourceId + ":*";
            return invalidateByPattern(pattern);
        }
        
        // 3. 失效缓存
        int count = invalidateCacheKeys(cacheKeys);
        
        // 4. 清理索引
        cleanupIndex(indexPattern);
        
        log.info("按数据源失效缓存完成 - dataSourceId: {}, count: {}", dataSourceId, count);
        return count;
    }
    
    @Override
    public int invalidateByTable(Long dataSourceId, String tableName) {
        if (dataSourceId == null || tableName == null || tableName.isEmpty()) {
            log.warn("数据源 ID 或表名为空，跳过缓存失效 - dataSourceId: {}, tableName: {}", 
                    dataSourceId, tableName);
            return 0;
        }
        
        log.info("按表名失效缓存 - dataSourceId: {}, tableName: {}", dataSourceId, tableName);
        
        // 1. 从索引获取相关缓存键
        String indexPattern = TABLE_INDEX_PREFIX + dataSourceId + ":" + tableName.toLowerCase() + ":*";
        Set<String> cacheKeys = getCacheKeysFromIndex(indexPattern);
        
        // 2. 如果没有索引，使用模式匹配（兼容旧数据）
        if (cacheKeys.isEmpty()) {
            // 尝试匹配包含表名的缓存键
            String pattern = "*" + tableName.toLowerCase() + "*";
            return invalidateByPatternWithDataSource(pattern, dataSourceId);
        }
        
        // 3. 失效缓存
        int count = invalidateCacheKeys(cacheKeys);
        
        // 4. 清理索引
        cleanupIndex(indexPattern);
        
        log.info("按表名失效缓存完成 - dataSourceId: {}, tableName: {}, count: {}", 
                dataSourceId, tableName, count);
        return count;
    }
    
    @Override
    public int invalidateByPattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            log.warn("缓存键模式为空，跳过缓存失效");
            return 0;
        }
        
        log.info("按模式失效缓存 - pattern: {}", pattern);
        
        AtomicInteger count = new AtomicInteger(0);
        
        // 1. 从 L1 缓存失效
        String regexPattern = convertPatternToRegex(pattern);
        Pattern compiledPattern = Pattern.compile(regexPattern);
        
        Set<String> l1KeysToInvalidate = l1Cache.asMap().keySet().stream()
                .filter(key -> compiledPattern.matcher(key).matches())
                .collect(Collectors.toSet());
        
        l1KeysToInvalidate.forEach(key -> {
            l1Cache.invalidate(key);
            count.incrementAndGet();
        });
        
        log.debug("从 L1 缓存按模式失效 - pattern: {}, count: {}", pattern, l1KeysToInvalidate.size());
        
        // 2. 从 L2 缓存失效
        try {
            String redisPattern = CACHE_KEY_PREFIX + pattern;
            Set<String> l2Keys = redisTemplate.keys(redisPattern);
            if (l2Keys != null && !l2Keys.isEmpty()) {
                redisTemplate.delete(l2Keys);
                // 注意：L1 和 L2 可能有重叠，这里不重复计数
                int l2OnlyCount = (int) l2Keys.stream()
                        .filter(key -> !l1KeysToInvalidate.contains(key))
                        .count();
                count.addAndGet(l2OnlyCount);
                log.debug("从 L2 缓存按模式失效 - pattern: {}, count: {}", pattern, l2Keys.size());
            }
        } catch (Exception e) {
            log.warn("L2 缓存按模式失效失败 - pattern: {}, error: {}", pattern, e.getMessage());
        }
        
        log.info("按模式失效缓存完成 - pattern: {}, totalCount: {}", pattern, count.get());
        return count.get();
    }
    
    @Override
    public boolean invalidate(String key) {
        if (key == null || key.isEmpty()) {
            log.warn("缓存键为空，跳过缓存失效");
            return false;
        }
        
        log.debug("失效单个缓存 - key: {}", key);
        
        String cacheKey = buildCacheKey(key);
        boolean existed = false;
        
        // 1. 从 L1 缓存失效
        if (l1Cache.getIfPresent(cacheKey) != null) {
            l1Cache.invalidate(cacheKey);
            existed = true;
            log.debug("从 L1 缓存失效 - key: {}", key);
        }
        
        // 2. 从 L2 缓存失效
        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (Boolean.TRUE.equals(deleted)) {
                existed = true;
                log.debug("从 L2 缓存失效 - key: {}", key);
            }
        } catch (Exception e) {
            log.warn("L2 缓存失效失败 - key: {}, error: {}", key, e.getMessage());
        }
        
        return existed;
    }
    
    @Override
    public int invalidateBatch(Iterable<String> keys) {
        if (keys == null) {
            return 0;
        }
        
        Set<String> cacheKeys = new HashSet<>();
        keys.forEach(key -> {
            if (key != null && !key.isEmpty()) {
                cacheKeys.add(buildCacheKey(key));
            }
        });
        
        if (cacheKeys.isEmpty()) {
            return 0;
        }
        
        log.info("批量失效缓存 - count: {}", cacheKeys.size());
        return invalidateCacheKeys(cacheKeys);
    }
    
    @Override
    public int invalidateByUser(Long userId) {
        if (userId == null) {
            log.warn("用户 ID 为空，跳过缓存失效");
            return 0;
        }
        
        log.info("按用户失效缓存 - userId: {}", userId);
        
        // 1. 从索引获取相关缓存键
        String indexPattern = USER_INDEX_PREFIX + userId + ":*";
        Set<String> cacheKeys = getCacheKeysFromIndex(indexPattern);
        
        // 2. 如果没有索引，使用模式匹配
        if (cacheKeys.isEmpty()) {
            String pattern = "*:user:" + userId + ":*";
            return invalidateByPattern(pattern);
        }
        
        // 3. 失效缓存
        int count = invalidateCacheKeys(cacheKeys);
        
        // 4. 清理索引
        cleanupIndex(indexPattern);
        
        log.info("按用户失效缓存完成 - userId: {}, count: {}", userId, count);
        return count;
    }
    
    @Override
    public int invalidateByRole(Long roleId) {
        if (roleId == null) {
            log.warn("角色 ID 为空，跳过缓存失效");
            return 0;
        }
        
        log.info("按角色失效缓存 - roleId: {}", roleId);
        
        // 1. 从索引获取相关缓存键
        String indexPattern = ROLE_INDEX_PREFIX + roleId + ":*";
        Set<String> cacheKeys = getCacheKeysFromIndex(indexPattern);
        
        // 2. 如果没有索引，使用模式匹配
        if (cacheKeys.isEmpty()) {
            String pattern = "*:roles:*" + roleId + "*";
            return invalidateByPattern(pattern);
        }
        
        // 3. 失效缓存
        int count = invalidateCacheKeys(cacheKeys);
        
        // 4. 清理索引
        cleanupIndex(indexPattern);
        
        log.info("按角色失效缓存完成 - roleId: {}, count: {}", roleId, count);
        return count;
    }
    
    @Override
    public int invalidateAll() {
        log.warn("清空所有缓存");
        
        AtomicInteger count = new AtomicInteger(0);
        
        // 1. 清空 L1 缓存
        long l1Size = l1Cache.estimatedSize();
        l1Cache.invalidateAll();
        count.addAndGet((int) l1Size);
        log.info("L1 缓存已清空 - count: {}", l1Size);
        
        // 2. 清空 L2 缓存
        try {
            Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                count.addAndGet(keys.size());
                log.info("L2 缓存已清空 - count: {}", keys.size());
            }
        } catch (Exception e) {
            log.warn("L2 缓存清空失败 - error: {}", e.getMessage());
        }
        
        log.warn("所有缓存已清空 - totalCount: {}", count.get());
        return count.get();
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 构建完整的缓存键
     */
    private String buildCacheKey(String key) {
        if (key.startsWith(CACHE_KEY_PREFIX)) {
            return key;
        }
        return CACHE_KEY_PREFIX + key;
    }
    
    /**
     * 从索引获取缓存键
     */
    private Set<String> getCacheKeysFromIndex(String indexPattern) {
        Set<String> cacheKeys = new HashSet<>();
        try {
            Set<String> indexKeys = redisTemplate.keys(indexPattern);
            if (indexKeys != null) {
                for (String indexKey : indexKeys) {
                    Object value = redisTemplate.opsForValue().get(indexKey);
                    if (value != null) {
                        cacheKeys.add(value.toString());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("从索引获取缓存键失败 - pattern: {}, error: {}", indexPattern, e.getMessage());
        }
        return cacheKeys;
    }
    
    /**
     * 失效缓存键集合
     */
    private int invalidateCacheKeys(Set<String> cacheKeys) {
        if (cacheKeys == null || cacheKeys.isEmpty()) {
            return 0;
        }
        
        AtomicInteger count = new AtomicInteger(0);
        
        // 1. 从 L1 缓存失效
        cacheKeys.forEach(key -> {
            String cacheKey = buildCacheKey(key);
            if (l1Cache.getIfPresent(cacheKey) != null) {
                l1Cache.invalidate(cacheKey);
                count.incrementAndGet();
            }
        });
        
        // 2. 从 L2 缓存失效
        try {
            Set<String> fullKeys = cacheKeys.stream()
                    .map(this::buildCacheKey)
                    .collect(Collectors.toSet());
            Long deleted = redisTemplate.delete(fullKeys);
            if (deleted != null && deleted > 0) {
                // 避免重复计数
                int l2OnlyCount = (int) (deleted - count.get());
                if (l2OnlyCount > 0) {
                    count.addAndGet(l2OnlyCount);
                }
            }
        } catch (Exception e) {
            log.warn("L2 缓存批量失效失败 - error: {}", e.getMessage());
        }
        
        return count.get();
    }
    
    /**
     * 清理索引
     */
    private void cleanupIndex(String indexPattern) {
        try {
            Set<String> indexKeys = redisTemplate.keys(indexPattern);
            if (indexKeys != null && !indexKeys.isEmpty()) {
                redisTemplate.delete(indexKeys);
                log.debug("清理索引 - pattern: {}, count: {}", indexPattern, indexKeys.size());
            }
        } catch (Exception e) {
            log.warn("清理索引失败 - pattern: {}, error: {}", indexPattern, e.getMessage());
        }
    }
    
    /**
     * 按模式失效缓存（限定数据源）
     */
    private int invalidateByPatternWithDataSource(String pattern, Long dataSourceId) {
        // 组合数据源 ID 和模式
        String combinedPattern = "*:ds:" + dataSourceId + ":*" + pattern + "*";
        return invalidateByPattern(combinedPattern);
    }
    
    /**
     * 将通配符模式转换为正则表达式
     * 
     * @param pattern 通配符模式（支持 * 和 ?）
     * @return 正则表达式
     */
    private String convertPatternToRegex(String pattern) {
        StringBuilder regex = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append(".");
                    break;
                case '.':
                case '(':
                case ')':
                case '[':
                case ']':
                case '{':
                case '}':
                case '\\':
                case '^':
                case '$':
                case '|':
                case '+':
                    regex.append("\\").append(c);
                    break;
                default:
                    regex.append(c);
            }
        }
        return regex.toString();
    }
}
