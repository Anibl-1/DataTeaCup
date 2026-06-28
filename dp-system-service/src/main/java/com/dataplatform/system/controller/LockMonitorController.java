package com.dataplatform.system.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequireRole;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 閿佺洃鎺PI
 * 鎻愪緵鍒嗗竷寮忛攣鐘舵€佹煡璇㈠拰绠＄悊鍔熻兘
 */
@Slf4j
@RestController
@RequestMapping("/monitor/locks")
@RequireRole("admin")
public class LockMonitorController {

    private static final String LOCK_PREFIX = "distributed:lock:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 鏌ヨ鎵€鏈夋椿璺冪殑鍒嗗竷寮忛攣
     */
    @GetMapping
    public Result<List<LockInfo>> listActiveLocks() {
        Set<String> keys = redisTemplate.keys(LOCK_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        List<LockInfo> locks = keys.stream()
            .map(this::buildLockInfo)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        return Result.success(locks);
    }

    /**
     * 鏌ヨ鎸囧畾閿佺殑鐘舵€?
     */
    @GetMapping("/{lockKey}")
    public Result<LockInfo> getLockStatus(@PathVariable String lockKey) {
        String key = LOCK_PREFIX + lockKey;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return Result.success(null);
        }

        LockInfo info = new LockInfo();
        info.setLockKey(lockKey);
        info.setValue(value);
        Long ttl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
        info.setTtlMs(ttl != null ? ttl : -1);
        return Result.success(info);
    }

    /**
     * 寮哄埗閲婃斁閿侊紙绠＄悊鍛樻搷浣滐級
     */
    @DeleteMapping("/{lockKey}")
    public Result<Boolean> forceUnlock(@PathVariable String lockKey) {
        String key = LOCK_PREFIX + lockKey;
        Boolean deleted = redisTemplate.delete(key);
        log.warn("寮哄埗閲婃斁閿? key={}, result={}", lockKey, deleted);
        return Result.success(Boolean.TRUE.equals(deleted));
    }

    private LockInfo buildLockInfo(String fullKey) {
        String value = redisTemplate.opsForValue().get(fullKey);
        if (value == null) {
            return null;
        }
        LockInfo info = new LockInfo();
        info.setLockKey(fullKey.substring(LOCK_PREFIX.length()));
        info.setValue(value);
        Long ttl = redisTemplate.getExpire(fullKey, TimeUnit.MILLISECONDS);
        info.setTtlMs(ttl != null ? ttl : -1);
        return info;
    }

    @Data
    public static class LockInfo {
        private String lockKey;
        private String value;
        private long ttlMs;
    }
}
