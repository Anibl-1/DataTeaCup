package com.dataplatform.data.service.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * Nonce验证器实现类
 * 使用Redis存储已使用的nonce，实现防重放攻击机制
 * 
 * 需求 3.5: THE Security_Engine SHALL 实现防重放攻击机制，使用nonce和时间戳验证请求唯一性
 *
 * 实现原理：
 * 1. 每个请求携带唯一的nonce和时间戳
 * 2. 服务端验证时间戳是否在有效期内
 * 3. 服务端检查nonce是否已被使用（通过Redis）
 * 4. 如果nonce未被使用，则标记为已使用并设置过期时间
 * 5. nonce的过期时间与签名过期时间一致，确保过期的请求无法重放
 *
 * @author dataplatform
 */
@Slf4j
@Service
public class NonceValidatorImpl implements NonceValidator {

    /** Redis键前缀 */
    private static final String NONCE_KEY_PREFIX = "security:nonce:";

    /** 已使用标记值 */
    private static final String USED_MARKER = "1";

    /** Redis模板 */
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /** nonce过期时间（秒），默认与签名过期时间一致（5分钟） */
    @Value("${security.nonce.expire-seconds:${security.signature.expire-seconds:300}}")
    private long nonceExpireSeconds;

    @Override
    public NonceValidationResult validateAndMark(String nonce, long timestamp) {
        // 1. 验证nonce是否为空
        if (!StringUtils.hasText(nonce)) {
            log.warn("防重放验证失败: nonce为空");
            return NonceValidationResult.emptyNonce();
        }

        // 2. 验证时间戳是否有效
        if (!isTimestampValid(timestamp)) {
            log.warn("防重放验证失败: 时间戳已过期 timestamp={}", timestamp);
            return NonceValidationResult.timestampExpired();
        }

        // 3. 检查Redis是否可用
        if (redisTemplate == null) {
            log.error("防重放验证失败: Redis不可用");
            return NonceValidationResult.storageError("Redis不可用");
        }

        // 4. 尝试标记nonce为已使用（原子操作）
        try {
            String key = buildNonceKey(nonce);
            
            // 计算过期时间
            long expireTime = calculateExpireTime(timestamp);
            
            // 使用setIfAbsent保证原子性，只有当key不存在时才设置成功
            Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, USED_MARKER, expireTime, TimeUnit.SECONDS);
            
            if (!Boolean.TRUE.equals(success)) {
                log.warn("防重放验证失败: nonce已被使用 nonce={}", truncateForLog(nonce));
                return NonceValidationResult.nonceAlreadyUsed();
            }

            log.debug("防重放验证通过: nonce={}", truncateForLog(nonce));
            return NonceValidationResult.success();
        } catch (Exception e) {
            log.error("防重放验证失败: Redis操作异常", e);
            return NonceValidationResult.storageError(e.getMessage());
        }
    }

    @Override
    public boolean isNonceValid(String nonce) {
        if (!StringUtils.hasText(nonce)) {
            return false;
        }
        return !isNonceUsed(nonce);
    }

    @Override
    public boolean isNonceUsed(String nonce) {
        if (!StringUtils.hasText(nonce)) {
            return false;
        }

        if (redisTemplate == null) {
            log.warn("Redis不可用，无法检查nonce状态");
            return false;
        }

        try {
            String key = buildNonceKey(nonce);
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("检查nonce状态失败", e);
            return false;
        }
    }

    @Override
    public boolean markNonceAsUsed(String nonce, long timestamp) {
        if (!StringUtils.hasText(nonce)) {
            return false;
        }

        if (redisTemplate == null) {
            log.warn("Redis不可用，无法标记nonce");
            return false;
        }

        try {
            String key = buildNonceKey(nonce);
            
            // 计算过期时间：从请求时间戳开始计算，确保nonce在签名有效期内不会被重用
            // 使用setIfAbsent保证原子性，只有当key不存在时才设置成功
            long expireTime = calculateExpireTime(timestamp);
            
            Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, USED_MARKER, expireTime, TimeUnit.SECONDS);
            
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.error("标记nonce失败", e);
            return false;
        }
    }

    @Override
    public boolean isTimestampValid(long timestamp) {
        if (timestamp <= 0) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long diff = Math.abs(currentTime - timestamp);
        
        // 时间戳必须在有效期内（转换为毫秒比较）
        return diff <= nonceExpireSeconds * 1000;
    }

    @Override
    public long getNonceExpireSeconds() {
        return nonceExpireSeconds;
    }

    @Override
    public void setNonceExpireSeconds(long expireSeconds) {
        if (expireSeconds > 0) {
            this.nonceExpireSeconds = expireSeconds;
        }
    }

    /**
     * 构建Redis键
     *
     * @param nonce 随机数
     * @return Redis键
     */
    private String buildNonceKey(String nonce) {
        return NONCE_KEY_PREFIX + nonce;
    }

    /**
     * 计算过期时间
     * 从请求时间戳开始计算，确保nonce在整个有效期内都不会被重用
     *
     * @param timestamp 请求时间戳
     * @return 过期时间（秒）
     */
    private long calculateExpireTime(long timestamp) {
        long currentTime = System.currentTimeMillis();
        long requestAge = (currentTime - timestamp) / 1000; // 请求已经过去的时间（秒）
        
        // 剩余有效期 = 总有效期 - 已过去的时间
        // 至少保留1秒，防止立即过期
        long remainingTime = nonceExpireSeconds - requestAge;
        
        // 为了安全起见，额外增加一些缓冲时间（防止时钟偏差）
        // 同时确保至少有1秒的过期时间
        return Math.max(remainingTime + 10, 1);
    }

    /**
     * 截断日志输出（防止日志注入）
     *
     * @param input 输入字符串
     * @return 截断后的字符串
     */
    private String truncateForLog(String input) {
        if (input == null) {
            return "null";
        }
        if (input.length() > 50) {
            return input.substring(0, 50) + "...";
        }
        return input.replaceAll("[\\r\\n]", " ");
    }

    /**
     * 设置Redis模板（用于测试）
     *
     * @param redisTemplate Redis模板
     */
    void setRedisTemplate(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
}
