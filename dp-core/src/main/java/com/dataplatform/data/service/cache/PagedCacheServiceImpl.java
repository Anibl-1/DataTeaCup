package com.dataplatform.data.service.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 分页缓存服务实现
 * 
 * 支持按页缓存查询结果，用于处理大数据集的分页存储和检索。
 * 
 * 实现特性：
 * - 使用 MultiLevelCacheManager 进行缓存存储（L1 + L2）
 * - 自动检测数据大小，超过 1MB 时使用 GZIP 压缩
 * - 分离存储页数据和元数据（总记录数）
 * - 支持 TTL 随机偏移防止缓存雪崩
 * 
 * 缓存键结构：
 * - 页数据键：{baseKey}:page:{pageNumber}
 * - 元数据键：{baseKey}:meta
 * 
 * 需求引用：
 * - 需求 9.1: THE Cache_Manager SHALL 支持按页缓存查询结果，每页默认 1000 条数据
 * - 需求 9.3: THE Cache_Manager SHALL 对超过 1MB 的缓存数据使用 GZIP 压缩存储
 * - 需求 9.5: WHEN 缓存数据超过配置的大小限制时，THE Cache_Manager SHALL 自动压缩或分片存储
 * 
 * @see PagedCacheService 接口定义
 * @see MultiLevelCacheManager 多级缓存管理器
 */
@Slf4j
@Service
public class PagedCacheServiceImpl implements PagedCacheService {
    
    /**
     * 页数据键后缀
     */
    private static final String PAGE_KEY_SUFFIX = ":page:";
    
    /**
     * 元数据键后缀
     */
    private static final String META_KEY_SUFFIX = ":meta";
    
    /**
     * 压缩数据标记前缀
     */
    private static final String COMPRESSED_PREFIX = "GZIP:";
    
    /**
     * 默认 TTL（秒）- 30 分钟
     */
    private static final long DEFAULT_TTL_SECONDS = 30 * 60;
    
    /**
     * 多级缓存管理器
     */
    private final MultiLevelCacheManager cacheManager;
    
    /**
     * JSON 序列化器
     */
    private final ObjectMapper objectMapper;
    
    /**
     * TTL 随机偏移器
     */
    private final TtlRandomizer ttlRandomizer;
    
    /**
     * 数据压缩服务
     */
    private final DataCompressionService compressionService;
    
    @Autowired
    public PagedCacheServiceImpl(
            MultiLevelCacheManager cacheManager,
            ObjectMapper objectMapper,
            TtlRandomizer ttlRandomizer,
            DataCompressionService compressionService) {
        this.cacheManager = cacheManager;
        this.objectMapper = objectMapper;
        this.ttlRandomizer = ttlRandomizer;
        this.compressionService = compressionService;
        log.info("分页缓存服务初始化完成");
    }

    
    @Override
    public PagedResult<Map<String, Object>> getPage(String baseKey, int page, int pageSize) {
        if (baseKey == null || baseKey.isEmpty()) {
            log.warn("无效的基础缓存键");
            return null;
        }
        
        if (page < 1) {
            log.warn("无效的页码: {}", page);
            return null;
        }
        
        if (pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        
        String pageKey = buildPageKey(baseKey, page);
        
        try {
            // 获取页数据
            Object cachedValue = cacheManager.get(pageKey, Object.class);
            if (cachedValue == null) {
                log.debug("分页缓存未命中 - baseKey: {}, page: {}", baseKey, page);
                return null;
            }
            
            // 解析数据（可能是压缩的）
            List<Map<String, Object>> data = parsePageData(cachedValue);
            if (data == null) {
                log.warn("分页数据解析失败 - baseKey: {}, page: {}", baseKey, page);
                return null;
            }
            
            // 获取总记录数
            long totalCount = getTotalCount(baseKey);
            if (totalCount < 0) {
                // 如果元数据丢失，使用当前页数据估算
                totalCount = (long) (page - 1) * pageSize + data.size();
                log.warn("元数据缺失，使用估算值 - baseKey: {}, totalCount: {}", baseKey, totalCount);
            }
            
            log.debug("分页缓存命中 - baseKey: {}, page: {}, dataSize: {}", baseKey, page, data.size());
            return PagedResult.of(data, page, pageSize, totalCount);
            
        } catch (Exception e) {
            log.error("获取分页缓存失败 - baseKey: {}, page: {}, error: {}", baseKey, page, e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public void cachePage(String baseKey, int page, List<Map<String, Object>> data, long totalCount) {
        cachePage(baseKey, page, data, totalCount, DEFAULT_TTL_SECONDS);
    }
    
    @Override
    public void cachePage(String baseKey, int page, List<Map<String, Object>> data, long totalCount, long ttlSeconds) {
        if (baseKey == null || baseKey.isEmpty()) {
            log.warn("无效的基础缓存键");
            return;
        }
        
        if (page < 1) {
            log.warn("无效的页码: {}", page);
            return;
        }
        
        if (data == null) {
            data = Collections.emptyList();
        }
        
        String pageKey = buildPageKey(baseKey, page);
        String metaKey = buildMetaKey(baseKey);
        
        try {
            // 应用 TTL 随机偏移防止缓存雪崩
            long randomizedTtl = ttlRandomizer.randomize(ttlSeconds, TimeUnit.SECONDS);
            
            // 判断是否需要压缩
            Object valueToCache;
            if (shouldCompress(data)) {
                byte[] compressed = compress(data);
                // 使用 Base64 编码存储压缩数据
                valueToCache = COMPRESSED_PREFIX + java.util.Base64.getEncoder().encodeToString(compressed);
                log.debug("数据已压缩 - baseKey: {}, page: {}, 原始大小: {}, 压缩后: {}", 
                        baseKey, page, estimateSize(data), compressed.length);
            } else {
                valueToCache = data;
            }
            
            // 缓存页数据
            cacheManager.put(pageKey, valueToCache, randomizedTtl, TimeUnit.SECONDS);
            
            // 缓存元数据（总记录数）
            PageMeta meta = new PageMeta(totalCount, System.currentTimeMillis());
            cacheManager.put(metaKey, meta, randomizedTtl, TimeUnit.SECONDS);
            
            log.debug("分页数据已缓存 - baseKey: {}, page: {}, dataSize: {}, totalCount: {}, ttl: {}s", 
                    baseKey, page, data.size(), totalCount, randomizedTtl);
            
        } catch (Exception e) {
            log.error("缓存分页数据失败 - baseKey: {}, page: {}, error: {}", baseKey, page, e.getMessage(), e);
        }
    }
    
    @Override
    public long getTotalCount(String baseKey) {
        if (baseKey == null || baseKey.isEmpty()) {
            return -1;
        }
        
        String metaKey = buildMetaKey(baseKey);
        
        try {
            PageMeta meta = cacheManager.get(metaKey, PageMeta.class);
            if (meta != null) {
                return meta.getTotalCount();
            }
        } catch (Exception e) {
            log.warn("获取总记录数失败 - baseKey: {}, error: {}", baseKey, e.getMessage());
        }
        
        return -1;
    }
    
    @Override
    public void invalidate(String baseKey) {
        if (baseKey == null || baseKey.isEmpty()) {
            return;
        }
        
        // 使用模式匹配删除所有相关缓存
        String pattern = baseKey + ":*";
        cacheManager.evictByPattern(pattern);
        
        log.debug("分页缓存已失效 - baseKey: {}", baseKey);
    }
    
    @Override
    public void invalidatePage(String baseKey, int page) {
        if (baseKey == null || baseKey.isEmpty() || page < 1) {
            return;
        }
        
        String pageKey = buildPageKey(baseKey, page);
        cacheManager.evict(pageKey);
        
        log.debug("分页缓存已失效 - baseKey: {}, page: {}", baseKey, page);
    }
    
    @Override
    public boolean isPageCached(String baseKey, int page) {
        if (baseKey == null || baseKey.isEmpty() || page < 1) {
            return false;
        }
        
        String pageKey = buildPageKey(baseKey, page);
        return cacheManager.exists(pageKey);
    }

    
    @Override
    public byte[] compress(List<Map<String, Object>> data) {
        return compressionService.compressData(data);
    }
    
    @Override
    public List<Map<String, Object>> decompress(byte[] compressed) {
        return compressionService.decompressData(compressed);
    }
    
    @Override
    public long estimateSize(List<Map<String, Object>> data) {
        return compressionService.estimateSize(data);
    }
    
    @Override
    public boolean shouldCompress(List<Map<String, Object>> data) {
        return compressionService.shouldCompress(data);
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 构建页数据缓存键
     */
    private String buildPageKey(String baseKey, int page) {
        return baseKey + PAGE_KEY_SUFFIX + page;
    }
    
    /**
     * 构建元数据缓存键
     */
    private String buildMetaKey(String baseKey) {
        return baseKey + META_KEY_SUFFIX;
    }
    
    /**
     * 解析页数据（处理压缩和非压缩两种情况）
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parsePageData(Object cachedValue) {
        if (cachedValue == null) {
            return null;
        }
        
        // 检查是否是压缩数据
        if (cachedValue instanceof String) {
            String strValue = (String) cachedValue;
            if (strValue.startsWith(COMPRESSED_PREFIX)) {
                // 解压数据
                String base64Data = strValue.substring(COMPRESSED_PREFIX.length());
                byte[] compressed = java.util.Base64.getDecoder().decode(base64Data);
                return decompress(compressed);
            }
        }
        
        // 非压缩数据，直接转换
        if (cachedValue instanceof List) {
            return (List<Map<String, Object>>) cachedValue;
        }
        
        // 尝试使用 ObjectMapper 转换
        try {
            return objectMapper.convertValue(cachedValue, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("页数据转换失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 分页元数据
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PageMeta {
        /**
         * 总记录数
         */
        private long totalCount;
        
        /**
         * 缓存时间戳
         */
        private long cachedAt;
    }
}
