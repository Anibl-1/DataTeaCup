package com.dataplatform.data.service.security;

import java.time.LocalDateTime;

/**
 * 登录安全服务接口
 * 实现登录失败锁定机制
 * 
 * 需求 4.4: THE Security_Engine SHALL 实现登录失败锁定机制，连续失败5次后锁定账户30分钟
 *
 * @author dataplatform
 */
public interface LoginSecurityService {

    /**
     * 账户锁定状态
     */
    record LockStatus(
            boolean locked,
            int failCount,
            int remainingAttempts,
            LocalDateTime lockUntil,
            long lockRemainingSeconds
    ) {
        public static LockStatus unlocked(int failCount, int maxAttempts) {
            return new LockStatus(false, failCount, maxAttempts - failCount, null, 0);
        }

        public static LockStatus locked(int failCount, LocalDateTime lockUntil, long remainingSeconds) {
            return new LockStatus(true, failCount, 0, lockUntil, remainingSeconds);
        }
    }

    /**
     * 登录尝试结果
     */
    record LoginAttemptResult(
            boolean isAllowed,
            String errorCode,
            String message,
            int remainingAttempts,
            long lockRemainingSeconds
    ) {
        public static LoginAttemptResult allowed() {
            return new LoginAttemptResult(true, null, null, -1, 0);
        }

        public static LoginAttemptResult locked(long remainingSeconds) {
            return new LoginAttemptResult(false, "ACCOUNT_LOCKED", 
                    "账户已锁定，请" + (remainingSeconds / 60 + 1) + "分钟后重试", 0, remainingSeconds);
        }

        public static LoginAttemptResult failed(int remainingAttempts) {
            return new LoginAttemptResult(false, "LOGIN_FAILED", 
                    "登录失败，还剩" + remainingAttempts + "次尝试机会", remainingAttempts, 0);
        }
    }

    /**
     * 检查账户是否被锁定
     *
     * @param username 用户名
     * @return 锁定状态
     */
    LockStatus checkLockStatus(String username);

    /**
     * 检查是否允许登录尝试
     *
     * @param username 用户名
     * @return 登录尝试结果
     */
    LoginAttemptResult checkLoginAttempt(String username);

    /**
     * 记录登录失败
     *
     * @param username 用户名
     * @return 登录尝试结果（包含剩余次数或锁定信息）
     */
    LoginAttemptResult recordLoginFailure(String username);

    /**
     * 记录登录成功（清除失败计数）
     *
     * @param username 用户名
     */
    void recordLoginSuccess(String username);

    /**
     * 手动解锁账户
     *
     * @param username 用户名
     * @return true 表示解锁成功
     */
    boolean unlockAccount(String username);

    /**
     * 获取登录失败次数
     *
     * @param username 用户名
     * @return 失败次数
     */
    int getFailCount(String username);

    /**
     * 获取最大允许失败次数
     *
     * @return 最大失败次数
     */
    int getMaxFailAttempts();

    /**
     * 获取锁定时长（秒）
     *
     * @return 锁定时长
     */
    int getLockDurationSeconds();
}
