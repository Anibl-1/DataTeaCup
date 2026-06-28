package com.dataplatform.data.service.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 缓存键生成器
 * 
 * 负责生成包含 RLS（行级权限）信息的缓存键，确保不同用户/角色的查询结果正确隔离。
 * 
 * 缓存键组成：
 * 1. SQL 语句（规范化处理）
 * 2. 查询参数（排序后序列化）
 * 3. 数据源 ID
 * 4. 用户 ID（用于 RLS）
 * 5. 角色 ID 列表（用于 RLS，排序确保一致性）
 * 
 * 特性：
 * - 确定性：相同输入始终生成相同的键
 * - 唯一性：不同输入生成不同的键
 * - 安全性：使用 MD5 哈希避免键过长和特殊字符问题
 * 
 * 需求引用：
 * - 需求 7.7: 使用 SQL + 参数 + 数据源ID + 用户角色 生成缓存键，确保 RLS 正确应用
 * 
 * @see MultiLevelCacheManager
 */
@Slf4j
@Component
public class CacheKeyGenerator {
    
    /**
     * 查询缓存键前缀
     */
    private static final String QUERY_KEY_PREFIX = "query:";
    
    /**
     * JSON 序列化器
     */
    private final ObjectMapper objectMapper;
    
    @Autowired
    public CacheKeyGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * 生成缓存键（包含 RLS 信息）
     * 
     * 基于 SQL、参数、数据源ID、用户ID 和角色列表生成唯一缓存键。
     * 确保 RLS（行级权限）正确应用，不同用户/角色的查询结果正确隔离。
     * 
     * @param sql          SQL 语句
     * @param params       查询参数（可为 null）
     * @param dataSourceId 数据源 ID（可为 null）
     * @param userId       用户 ID（可为 null，用于 RLS）
     * @param roleIds      角色 ID 列表（可为 null，用于 RLS）
     * @return 缓存键（格式：query:{md5hash}）
     */
    public String generate(String sql, Map<String, Object> params, Long dataSourceId, Long userId, List<Long> roleIds) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // 1. 添加 SQL（规范化处理）
        String normalizedSql = normalizeSql(sql);
        keyBuilder.append("sql:").append(normalizedSql);
        
        // 2. 添加参数（排序后序列化，确保一致性）
        if (params != null && !params.isEmpty()) {
            String paramsJson = serializeParams(params);
            keyBuilder.append(":params:").append(paramsJson);
        }
        
        // 3. 添加数据源 ID
        if (dataSourceId != null) {
            keyBuilder.append(":ds:").append(dataSourceId);
        }
        
        // 4. 添加用户 ID（用于 RLS）
        if (userId != null) {
            keyBuilder.append(":user:").append(userId);
        }
        
        // 5. 添加角色 ID 列表（用于 RLS，排序确保一致性）
        if (roleIds != null && !roleIds.isEmpty()) {
            List<Long> sortedRoles = new ArrayList<>(roleIds);
            Collections.sort(sortedRoles);
            keyBuilder.append(":roles:").append(sortedRoles);
        }
        
        // 6. 生成 MD5 哈希作为最终键
        String rawKey = keyBuilder.toString();
        String hash = md5Hash(rawKey);
        
        log.debug("生成缓存键 - rawKey: {}, hash: {}", rawKey, hash);
        return QUERY_KEY_PREFIX + hash;
    }
    
    /**
     * 生成简单缓存键（不包含 RLS 信息）
     * 
     * 适用于不需要 RLS 隔离的场景，如公共数据缓存。
     * 
     * @param sql          SQL 语句
     * @param params       查询参数（可为 null）
     * @param dataSourceId 数据源 ID（可为 null）
     * @return 缓存键
     */
    public String generateSimple(String sql, Map<String, Object> params, Long dataSourceId) {
        return generate(sql, params, dataSourceId, null, null);
    }
    
    /**
     * 获取原始键内容（用于调试）
     * 
     * @param sql          SQL 语句
     * @param params       查询参数
     * @param dataSourceId 数据源 ID
     * @param userId       用户 ID
     * @param roleIds      角色 ID 列表
     * @return 原始键内容（未哈希）
     */
    public String getRawKey(String sql, Map<String, Object> params, Long dataSourceId, Long userId, List<Long> roleIds) {
        StringBuilder keyBuilder = new StringBuilder();
        
        String normalizedSql = normalizeSql(sql);
        keyBuilder.append("sql:").append(normalizedSql);
        
        if (params != null && !params.isEmpty()) {
            String paramsJson = serializeParams(params);
            keyBuilder.append(":params:").append(paramsJson);
        }
        
        if (dataSourceId != null) {
            keyBuilder.append(":ds:").append(dataSourceId);
        }
        
        if (userId != null) {
            keyBuilder.append(":user:").append(userId);
        }
        
        if (roleIds != null && !roleIds.isEmpty()) {
            List<Long> sortedRoles = new ArrayList<>(roleIds);
            Collections.sort(sortedRoles);
            keyBuilder.append(":roles:").append(sortedRoles);
        }
        
        return keyBuilder.toString();
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 规范化 SQL 语句
     * 
     * 处理：
     * - 去除首尾空白
     * - 将多个连续空白字符替换为单个空格
     * - 转换为小写（确保大小写不敏感）
     * 
     * @param sql 原始 SQL 语句
     * @return 规范化后的 SQL 语句
     */
    private String normalizeSql(String sql) {
        if (sql == null) {
            return "";
        }
        return sql.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }
    
    /**
     * 序列化参数为 JSON
     * 
     * 使用 TreeMap 确保参数按键排序，保证相同参数生成相同的 JSON 字符串。
     * 
     * @param params 查询参数
     * @return JSON 字符串
     */
    private String serializeParams(Map<String, Object> params) {
        try {
            // 按键排序确保一致性
            TreeMap<String, Object> sortedParams = new TreeMap<>(params);
            return objectMapper.writeValueAsString(sortedParams);
        } catch (JsonProcessingException e) {
            log.warn("参数序列化失败，使用 toString - error: {}", e.getMessage());
            // 降级处理：使用 TreeMap 的 toString
            return new TreeMap<>(params).toString();
        }
    }
    
    /**
     * 计算 MD5 哈希
     * 
     * @param input 输入字符串
     * @return 32 位十六进制 MD5 哈希值
     */
    private String md5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.warn("MD5 哈希计算失败，使用 hashCode - error: {}", e.getMessage());
            return String.valueOf(input.hashCode());
        }
    }
}
