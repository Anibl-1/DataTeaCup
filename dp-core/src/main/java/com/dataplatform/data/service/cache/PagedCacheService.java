package com.dataplatform.data.service.cache;

import java.util.List;
import java.util.Map;

/**
 * 分页缓存服务接口
 * 
 * 支持按页缓存查询结果，用于处理大数据集的分页存储和检索。
 * 
 * 功能特性：
 * - 按页缓存查询结果，每页默认 1000 条数据
 * - 支持获取指定页的数据
 * - 支持缓存总记录数
 * - 支持缓存失效
 * - 支持大数据压缩存储（GZIP）
 * 
 * 缓存键结构：
 * - 页数据键：{baseKey}:page:{pageNumber}
 * - 元数据键：{baseKey}:meta（存储总记录数等信息）
 * 
 * 需求引用：
 * - 需求 9.1: THE Cache_Manager SHALL 支持按页缓存查询结果，每页默认 1000 条数据
 * - 需求 9.3: THE Cache_Manager SHALL 对超过 1MB 的缓存数据使用 GZIP 压缩存储
 * - 需求 9.5: WHEN 缓存数据超过配置的大小限制时，THE Cache_Manager SHALL 自动压缩或分片存储
 * 
 * @see PagedResult 分页结果封装
 * @see PagedCacheServiceImpl 默认实现
 */
public interface PagedCacheService {
    
    /**
     * 默认每页大小
     */
    int DEFAULT_PAGE_SIZE = 1000;
    
    /**
     * 压缩阈值（1MB）
     */
    int COMPRESSION_THRESHOLD_BYTES = 1024 * 1024;
    
    /**
     * 获取分页数据
     * 
     * 从缓存中获取指定页的数据。如果缓存未命中，返回 null。
     * 
     * @param baseKey  基础缓存键（不包含页码后缀）
     * @param page     页码（从 1 开始）
     * @param pageSize 每页大小
     * @return 分页结果，缓存未命中时返回 null
     */
    PagedResult<Map<String, Object>> getPage(String baseKey, int page, int pageSize);
    
    /**
     * 缓存分页数据
     * 
     * 将指定页的数据缓存起来。如果数据超过压缩阈值，会自动进行 GZIP 压缩。
     * 
     * @param baseKey    基础缓存键
     * @param page       页码（从 1 开始）
     * @param data       当前页数据
     * @param totalCount 总记录数
     */
    void cachePage(String baseKey, int page, List<Map<String, Object>> data, long totalCount);
    
    /**
     * 缓存分页数据（带 TTL）
     * 
     * @param baseKey    基础缓存键
     * @param page       页码（从 1 开始）
     * @param data       当前页数据
     * @param totalCount 总记录数
     * @param ttlSeconds TTL（秒）
     */
    void cachePage(String baseKey, int page, List<Map<String, Object>> data, long totalCount, long ttlSeconds);
    
    /**
     * 获取缓存的总记录数
     * 
     * @param baseKey 基础缓存键
     * @return 总记录数，缓存未命中时返回 -1
     */
    long getTotalCount(String baseKey);
    
    /**
     * 失效指定基础键的所有分页缓存
     * 
     * 删除该基础键下的所有页数据和元数据。
     * 
     * @param baseKey 基础缓存键
     */
    void invalidate(String baseKey);
    
    /**
     * 失效指定页的缓存
     * 
     * @param baseKey 基础缓存键
     * @param page    页码
     */
    void invalidatePage(String baseKey, int page);
    
    /**
     * 检查指定页是否已缓存
     * 
     * @param baseKey 基础缓存键
     * @param page    页码
     * @return 如果已缓存返回 true
     */
    boolean isPageCached(String baseKey, int page);
    
    /**
     * 压缩数据
     * 
     * 使用 GZIP 算法压缩数据列表。
     * 
     * @param data 要压缩的数据
     * @return 压缩后的字节数组
     */
    byte[] compress(List<Map<String, Object>> data);
    
    /**
     * 解压数据
     * 
     * 解压 GZIP 压缩的数据。
     * 
     * @param compressed 压缩的字节数组
     * @return 解压后的数据列表
     */
    List<Map<String, Object>> decompress(byte[] compressed);
    
    /**
     * 估算数据大小（字节）
     * 
     * @param data 数据列表
     * @return 估算的字节大小
     */
    long estimateSize(List<Map<String, Object>> data);
    
    /**
     * 判断数据是否需要压缩
     * 
     * @param data 数据列表
     * @return 如果数据大小超过压缩阈值返回 true
     */
    boolean shouldCompress(List<Map<String, Object>> data);
}
