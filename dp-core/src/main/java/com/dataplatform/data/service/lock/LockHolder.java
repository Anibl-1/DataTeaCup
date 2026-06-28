package com.dataplatform.data.service.lock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 锁持有者上下文
 * 用于存储当前线程持有的锁信息
 */
public class LockHolder {
    
    private static final ThreadLocal<Map<String, LockInfo>> LOCK_INFO = 
        ThreadLocal.withInitial(ConcurrentHashMap::new);
    
    /**
     * 设置锁信息
     */
    public static void set(String lockKey, String lockValue) {
        LOCK_INFO.get().put(lockKey, new LockInfo(lockValue, 1));
    }
    
    /**
     * 获取锁值
     */
    public static String get(String lockKey) {
        LockInfo info = LOCK_INFO.get().get(lockKey);
        return info != null ? info.getValue() : null;
    }
    
    /**
     * 移除锁信息
     */
    public static void remove(String lockKey) {
        LOCK_INFO.get().remove(lockKey);
    }
    
    /**
     * 增加重入计数
     */
    public static int incrementCount(String lockKey) {
        LockInfo info = LOCK_INFO.get().get(lockKey);
        if (info != null) {
            info.incrementCount();
            return info.getCount();
        }
        return 0;
    }
    
    /**
     * 减少重入计数
     */
    public static int decrementCount(String lockKey) {
        LockInfo info = LOCK_INFO.get().get(lockKey);
        if (info != null) {
            info.decrementCount();
            return info.getCount();
        }
        return 0;
    }
    
    /**
     * 获取重入计数
     */
    public static int getCount(String lockKey) {
        LockInfo info = LOCK_INFO.get().get(lockKey);
        return info != null ? info.getCount() : 0;
    }
    
    /**
     * 检查是否持有锁
     */
    public static boolean isHeld(String lockKey) {
        return LOCK_INFO.get().containsKey(lockKey);
    }
    
    /**
     * 清除当前线程所有锁信息
     */
    public static void clear() {
        LOCK_INFO.remove();
    }
    
    /**
     * 锁信息内部类
     */
    public static class LockInfo {
        private final String value;
        private int count;
        
        public LockInfo(String value, int count) {
            this.value = value;
            this.count = count;
        }
        
        public String getValue() {
            return value;
        }
        
        public int getCount() {
            return count;
        }
        
        public void incrementCount() {
            this.count++;
        }
        
        public void decrementCount() {
            this.count--;
        }
    }
}
