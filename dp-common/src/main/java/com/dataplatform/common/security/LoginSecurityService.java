package com.dataplatform.common.security;

/**
 * 登录安全服务接口
 * 定义在 dp-common 以避免跨模块循环依赖
 */
public interface LoginSecurityService {
    long checkLocked(String username);
    long recordFailure(String username);
    int getRemainingAttempts(String username);
    void clearFailures(String username);
}
