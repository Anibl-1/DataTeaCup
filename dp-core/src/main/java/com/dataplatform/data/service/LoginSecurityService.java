package com.dataplatform.data.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * 登录安全服务
 * 实现登录失败锁定策略：连续失败N次后锁定M分钟
 * 
 * @author dataplatform
 */
@Slf4j
@Service
public class LoginSecurityService implements com.dataplatform.common.security.LoginSecurityService {
    
    /** 最大失败次数 */
    @Value("${login.security.max-failures:5}")
    private int maxFailures;
    
    /** 锁定时间（分钟） */
    @Value("${login.security.lock-minutes:15}")
    private int lockMinutes;
    
    /** 失败计数缓存：username -> failCount */
    private Cache<String, Integer> failureCountCache;
    
    /** 锁定时间缓存：username -> lockExpireTime(ms) */
    private Cache<String, Long> lockCache;
    
    @PostConstruct
    public void init() {
        // 失败计数在锁定时间后过期
        failureCountCache = Caffeine.newBuilder()
                .expireAfterWrite(lockMinutes, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();
        
        lockCache = Caffeine.newBuilder()
                .expireAfterWrite(lockMinutes, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();
        
        log.info("登录安全服务初始化: 最大失败次数={}, 锁定时间={}分钟", maxFailures, lockMinutes);
    }
    
    /**
     * 检查用户是否被锁定
     * 
     * @param username 用户名
     * @return 如果被锁定返回剩余锁定时间（秒），未锁定返回0
     */
    public long checkLocked(String username) {
        Long lockExpire = lockCache.getIfPresent(username);
        if (lockExpire != null) {
            long remaining = (lockExpire - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                return remaining;
            }
            // 锁定已过期，清除
            lockCache.invalidate(username);
            failureCountCache.invalidate(username);
        }
        return 0;
    }
    
    /**
     * 记录登录失败
     * 
     * @param username 用户名
     * @return 如果触发锁定返回锁定时间（秒），否则返回0
     */
    public long recordFailure(String username) {
        Integer count = failureCountCache.getIfPresent(username);
        int newCount = (count == null ? 0 : count) + 1;
        failureCountCache.put(username, newCount);
        
        if (newCount >= maxFailures) {
            // 锁定用户
            long lockExpire = System.currentTimeMillis() + lockMinutes * 60 * 1000L;
            lockCache.put(username, lockExpire);
            log.warn("用户 {} 登录失败{}次，已锁定{}分钟", username, newCount, lockMinutes);
            return lockMinutes * 60L;
        }
        
        log.info("用户 {} 登录失败，已失败{}次，还剩{}次机会", username, newCount, maxFailures - newCount);
        return 0;
    }
    
    /**
     * 登录成功后清除失败记录
     * 
     * @param username 用户名
     */
    public void clearFailures(String username) {
        failureCountCache.invalidate(username);
        lockCache.invalidate(username);
    }
    
    /**
     * 获取剩余重试次数
     * 
     * @param username 用户名
     * @return 剩余重试次数
     */
    public int getRemainingAttempts(String username) {
        Integer count = failureCountCache.getIfPresent(username);
        return maxFailures - (count == null ? 0 : count);
    }
}
