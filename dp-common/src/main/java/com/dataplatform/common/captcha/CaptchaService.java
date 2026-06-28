package com.dataplatform.common.captcha;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码服务
 * 使用 Hutool LineCaptcha 生成图形验证码，内存存储（ConcurrentHashMap + TTL 自动清理）
 *
 * @author dataplatform
 */
@Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);

    /** 验证码是否启用（配置开关） */
    @Value("${app.captcha.enabled:false}")
    private boolean captchaEnabled;

    /** 验证码图片宽度 */
    @Value("${app.captcha.width:130}")
    private int captchaWidth;

    /** 验证码图片高度 */
    @Value("${app.captcha.height:40}")
    private int captchaHeight;

    /** 验证码字符长度 */
    @Value("${app.captcha.length:4}")
    private int captchaLength;

    /** 干扰线条数 */
    @Value("${app.captcha.lineCount:30}")
    private int lineCount;

    /** 验证码过期时间（毫秒），默认5分钟 */
    @Value("${app.captcha.expireMs:300000}")
    private long expireMs;

    /** 内存存储：key -> CaptchaEntry */
    private final ConcurrentHashMap<String, CaptchaEntry> store = new ConcurrentHashMap<>();

    /** 上次清理时间 */
    private volatile long lastCleanupTime = System.currentTimeMillis();
    private static final long CLEANUP_INTERVAL_MS = 60_000; // 1分钟清理一次

    /**
     * 是否启用验证码
     */
    public boolean isEnabled() {
        return captchaEnabled;
    }

    /**
     * 生成验证码
     *
     * @return CaptchaResult 包含 captchaKey 和 base64 图片
     */
    public CaptchaResult generate() {
        cleanupExpired();

        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(captchaWidth, captchaHeight, captchaLength, lineCount);
        String code = captcha.getCode().toLowerCase();
        String imageBase64 = captcha.getImageBase64Data();
        String key = UUID.randomUUID().toString().replace("-", "");

        store.put(key, new CaptchaEntry(code, System.currentTimeMillis() + expireMs));
        log.debug("Captcha generated: key={}, code={}", key, code);

        return new CaptchaResult(key, imageBase64);
    }

    /**
     * 验证验证码
     *
     * @param key  验证码key
     * @param code 用户输入的验证码
     * @return true=验证通过
     */
    public boolean verify(String key, String code) {
        if (!captchaEnabled) {
            return true; // 未启用时始终通过
        }
        if (key == null || code == null || key.isBlank() || code.isBlank()) {
            return false;
        }
        CaptchaEntry entry = store.remove(key); // 一次性使用
        if (entry == null) {
            return false;
        }
        if (System.currentTimeMillis() > entry.expireAt) {
            return false; // 已过期
        }
        return entry.code.equalsIgnoreCase(code.trim());
    }

    /**
     * 定期清理过期验证码
     */
    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime < CLEANUP_INTERVAL_MS) {
            return;
        }
        lastCleanupTime = now;
        int removed = 0;
        var it = store.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().expireAt < now) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Captcha cleanup: removed {} expired entries, remaining {}", removed, store.size());
        }
    }

    // ─── 内部数据结构 ───

    private static class CaptchaEntry {
        final String code;
        final long expireAt;

        CaptchaEntry(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }

    /**
     * 验证码生成结果
     */
    public record CaptchaResult(String captchaKey, String captchaImage) {}
}
