package com.dataplatform.data.service.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 登录安全服务实现类
 * 实现登录失败锁定机制
 * 
 * 需求 4.4: THE Security_Engine SHALL 实现登录失败锁定机制，连续失败5次后锁定账户30分钟
 *
 * @author dataplatform
 */
@Slf4j
@Service
public class LoginSecurityServiceImpl implements LoginSecurityService {

    private static final String FAIL_COUNT_KEY_PREFIX = "login:fail_count:";
    private static final String LOCK_KEY_PREFIX = "login:lock:";

    @Value("${security.login.max-fail-attempts:5}")
    private int maxFailAttempts;

    @Value("${security.login.lock-duration-seconds:1800}")
    private int lockDurationSeconds;

    @Value("${security.login.fail-count-expire-seconds:3600}")
    private int failCountExpireSeconds;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Override
    public LockStatus checkLockStatus(String username) {
        if (!StringUtils.hasText(username)) {
            return LockStatus.unlocked(0, maxFailAttempts);
        }

        // 检查是否被锁定
        long lockRemaining = getLockRemainingSeconds(username);
        if (lockRemaining > 0) {
            int failCount = getFailCount(username);
            LocalDateTime lockUntil = LocalDateTime.now().plusSeconds(lockRemaining);
            return LockStatus.locked(failCount, lockUntil, lockRemaining);
        }

        // 未锁定，返回失败次数
        int failCount = getFailCount(username);
        return LockStatus.unlocked(failCount, maxFailAttempts);
    }

    @Override
    public LoginAttemptResult checkLoginAttempt(String username) {
        if (!StringUtils.hasText(username)) {
            return LoginAttemptResult.allowed();
        }

        // 检查是否被锁定
        long lockRemaining = getLockRemainingSeconds(username);
        if (lockRemaining > 0) {
            log.warn("登录尝试被拒绝: 账户已锁定 username={}, remainingSeconds={}", username, lockRemaining);
            return LoginAttemptResult.locked(lockRemaining);
        }

        return LoginAttemptResult.allowed();
    }

    @Override
    public LoginAttemptResult recordLoginFailure(String username) {
        if (!StringUtils.hasText(username)) {
            return LoginAttemptResult.failed(maxFailAttempts);
        }

        // 检查是否已锁定
        long lockRemaining = getLockRemainingSeconds(username);
        if (lockRemaining > 0) {
            return LoginAttemptResult.locked(lockRemaining);
        }

        // 增加失败计数
        int failCount = incrementFailCount(username);
        int remaining = maxFailAttempts - failCount;

        log.warn("登录失败: username={}, failCount={}, remaining={}", username, failCount, remaining);

        // 检查是否需要锁定
        if (remaining <= 0) {
            lockAccount(username);
            log.warn("账户已锁定: username={}, duration={}s", username, lockDurationSeconds);
            return LoginAttemptResult.locked(lockDurationSeconds);
        }

        return LoginAttemptResult.failed(remaining);
    }

    @Override
    public void recordLoginSuccess(String username) {
        if (!StringUtils.hasText(username)) {
            return;
        }

        // 清除失败计数和锁定状态
        clearFailCount(username);
        clearLock(username);
        log.debug("登录成功，清除失败记录: username={}", username);
    }

    @Override
    public boolean unlockAccount(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }

        clearFailCount(username);
        clearLock(username);
        log.info("手动解锁账户: username={}", username);
        return true;
    }

    @Override
    public int getFailCount(String username) {
        if (!StringUtils.hasText(username) || redisTemplate == null) {
            return 0;
        }

        String key = FAIL_COUNT_KEY_PREFIX + username;
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Integer.parseInt(value) : 0;
        } catch (Exception e) {
            log.error("获取登录失败次数异常: username={}", username, e);
            return 0;
        }
    }

    @Override
    public int getMaxFailAttempts() {
        return maxFailAttempts;
    }

    @Override
    public int getLockDurationSeconds() {
        return lockDurationSeconds;
    }

    // ==================== 私有方法 ====================

    private int incrementFailCount(String username) {
        if (redisTemplate == null) {
            return 1;
        }

        String key = FAIL_COUNT_KEY_PREFIX + username;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                // 首次失败，设置过期时间
                redisTemplate.expire(key, failCountExpireSeconds, TimeUnit.SECONDS);
            }
            return count != null ? count.intValue() : 1;
        } catch (Exception e) {
            log.error("增加登录失败次数异常: username={}", username, e);
            return 1;
        }
    }

    private void clearFailCount(String username) {
        if (redisTemplate == null) {
            return;
        }

        String key = FAIL_COUNT_KEY_PREFIX + username;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("清除登录失败次数异常: username={}", username, e);
        }
    }

    private void lockAccount(String username) {
        if (redisTemplate == null) {
            return;
        }

        String key = LOCK_KEY_PREFIX + username;
        try {
            redisTemplate.opsForValue().set(key, "1", lockDurationSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("锁定账户异常: username={}", username, e);
        }
    }

    private void clearLock(String username) {
        if (redisTemplate == null) {
            return;
        }

        String key = LOCK_KEY_PREFIX + username;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("清除账户锁定异常: username={}", username, e);
        }
    }

    private long getLockRemainingSeconds(String username) {
        if (redisTemplate == null) {
            return 0;
        }

        String key = LOCK_KEY_PREFIX + username;
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl != null && ttl > 0 ? ttl : 0;
        } catch (Exception e) {
            log.error("获取锁定剩余时间异常: username={}", username, e);
            return 0;
        }
    }
}
