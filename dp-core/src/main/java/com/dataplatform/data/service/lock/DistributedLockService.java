package com.dataplatform.data.service.lock;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁服务接口
 * 提供分布式环境下的互斥锁功能
 */
public interface DistributedLockService {
    
    /**
     * 尝试获取锁
     * @param lockKey 锁键
     * @param waitTime 等待时间
     * @param leaseTime 持有时间
     * @param unit 时间单位
     * @return 是否获取成功
     */
    boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit);
    
    /**
     * 尝试获取锁（使用默认超时）
     * @param lockKey 锁键
     * @return 是否获取成功
     */
    boolean tryLock(String lockKey);
    
    /**
     * 释放锁
     * @param lockKey 锁键
     */
    void unlock(String lockKey);
    
    /**
     * 检查锁是否被持有
     * @param lockKey 锁键
     * @return 是否被持有
     */
    boolean isLocked(String lockKey);
    
    /**
     * 检查当前线程是否持有锁
     * @param lockKey 锁键
     * @return 是否持有
     */
    boolean isHeldByCurrentThread(String lockKey);
    
    /**
     * 续期锁
     * @param lockKey 锁键
     * @param leaseTime 新的持有时间
     * @param unit 时间单位
     * @return 是否续期成功
     */
    boolean renewLock(String lockKey, long leaseTime, TimeUnit unit);
    
    /**
     * 执行带锁的操作
     * @param lockKey 锁键
     * @param waitTime 等待时间
     * @param leaseTime 持有时间
     * @param unit 时间单位
     * @param supplier 要执行的操作
     * @return 操作结果
     */
    <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, 
                          TimeUnit unit, Supplier<T> supplier);
    
    /**
     * 执行带锁的操作（使用默认超时）
     * @param lockKey 锁键
     * @param supplier 要执行的操作
     * @return 操作结果
     */
    <T> T executeWithLock(String lockKey, Supplier<T> supplier);
    
    /**
     * 执行带锁的操作（无返回值）
     * @param lockKey 锁键
     * @param runnable 要执行的操作
     */
    void executeWithLock(String lockKey, Runnable runnable);
}
