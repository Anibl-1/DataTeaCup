package com.dataplatform.data.service.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 基于Redis的分布式锁实现
 * 支持可重入锁和锁续期
 */
@Slf4j
@Service
public class RedisDistributedLockService implements DistributedLockService {
    
    private static final String LOCK_PREFIX = "distributed:lock:";
    private static final long DEFAULT_WAIT_TIME = 3000; // 默认等待3秒
    private static final long DEFAULT_LEASE_TIME = 30000; // 默认持有30秒
    private static final long RETRY_INTERVAL = 50; // 重试间隔50ms
    
    // 释放锁的Lua脚本（保证原子性）
    private static final String UNLOCK_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "    return redis.call('del', KEYS[1]) " +
        "else " +
        "    return 0 " +
        "end";
    
    // 续期锁的Lua脚本
    private static final String RENEW_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "    return redis.call('pexpire', KEYS[1], ARGV[2]) " +
        "else " +
        "    return 0 " +
        "end";
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        String key = LOCK_PREFIX + lockKey;
        String value = generateLockValue();
        
        // 检查是否已持有锁（可重入）
        if (LockHolder.isHeld(lockKey)) {
            String existingValue = LockHolder.get(lockKey);
            String currentValue = redisTemplate.opsForValue().get(key);
            if (existingValue != null && existingValue.equals(currentValue)) {
                LockHolder.incrementCount(lockKey);
                log.debug("锁重入成功: key={}, count={}", lockKey, LockHolder.getCount(lockKey));
                return true;
            }
        }
        
        long waitMillis = unit.toMillis(waitTime);
        long leaseMillis = unit.toMillis(leaseTime);
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < waitMillis) {
            Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, value, leaseMillis, TimeUnit.MILLISECONDS);
            
            if (Boolean.TRUE.equals(success)) {
                LockHolder.set(lockKey, value);
                log.debug("获取锁成功: key={}, value={}", lockKey, value);
                return true;
            }
            
            try {
                Thread.sleep(RETRY_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("获取锁被中断: key={}", lockKey);
                return false;
            }
        }
        
        log.debug("获取锁超时: key={}", lockKey);
        return false;
    }
    
    @Override
    public boolean tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public void unlock(String lockKey) {
        String key = LOCK_PREFIX + lockKey;
        String value = LockHolder.get(lockKey);
        
        if (value == null) {
            log.warn("尝试释放未持有的锁: key={}", lockKey);
            return;
        }
        
        // 检查重入计数
        int count = LockHolder.decrementCount(lockKey);
        if (count > 0) {
            log.debug("锁重入计数减少: key={}, count={}", lockKey, count);
            return;
        }
        
        // 使用Lua脚本原子性释放锁
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(key), value);
        
        LockHolder.remove(lockKey);
        
        if (result != null && result == 1) {
            log.debug("释放锁成功: key={}", lockKey);
        } else {
            log.warn("释放锁失败（锁可能已过期或被其他线程持有）: key={}", lockKey);
        }
    }
    
    @Override
    public boolean isLocked(String lockKey) {
        String key = LOCK_PREFIX + lockKey;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    @Override
    public boolean isHeldByCurrentThread(String lockKey) {
        if (!LockHolder.isHeld(lockKey)) {
            return false;
        }
        
        String key = LOCK_PREFIX + lockKey;
        String expectedValue = LockHolder.get(lockKey);
        String actualValue = redisTemplate.opsForValue().get(key);
        
        return expectedValue != null && expectedValue.equals(actualValue);
    }
    
    @Override
    public boolean renewLock(String lockKey, long leaseTime, TimeUnit unit) {
        String key = LOCK_PREFIX + lockKey;
        String value = LockHolder.get(lockKey);
        
        if (value == null) {
            log.warn("尝试续期未持有的锁: key={}", lockKey);
            return false;
        }
        
        long leaseMillis = unit.toMillis(leaseTime);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RENEW_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, 
            Collections.singletonList(key), value, String.valueOf(leaseMillis));
        
        boolean success = result != null && result == 1;
        if (success) {
            log.debug("锁续期成功: key={}, leaseTime={}ms", lockKey, leaseMillis);
        } else {
            log.warn("锁续期失败: key={}", lockKey);
        }
        
        return success;
    }
    
    @Override
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, 
                                  TimeUnit unit, Supplier<T> supplier) {
        boolean locked = false;
        try {
            locked = tryLock(lockKey, waitTime, leaseTime, unit);
            if (!locked) {
                throw new LockAcquisitionException("获取锁失败: " + lockKey);
            }
            return supplier.get();
        } finally {
            if (locked) {
                unlock(lockKey);
            }
        }
    }
    
    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, 
                               TimeUnit.MILLISECONDS, supplier);
    }
    
    @Override
    public void executeWithLock(String lockKey, Runnable runnable) {
        executeWithLock(lockKey, () -> {
            runnable.run();
            return null;
        });
    }
    
    /**
     * 生成唯一的锁值
     */
    private String generateLockValue() {
        return UUID.randomUUID().toString() + ":" + Thread.currentThread().getId();
    }
    
    /**
     * 锁获取异常
     */
    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }
    }
}
