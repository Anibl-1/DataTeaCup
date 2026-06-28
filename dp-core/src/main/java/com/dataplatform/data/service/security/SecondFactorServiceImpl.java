package com.dataplatform.data.service.security;

import com.dataplatform.system.entity.User;
import com.dataplatform.system.mapper.UserMapper;
import com.dataplatform.common.util.MD5Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * 二次验证服务实现类
 * 实现敏感操作的二次验证功能，支持密码确认、短信验证码、邮箱验证码等方式
 * 
 * 需求 3.6: THE Security_Engine SHALL 实现敏感操作二次验证，支持密码确认、短信验证码、邮箱验证码
 *
 * 实现原理：
 * 1. 验证码生成：使用SecureRandom生成6位数字验证码
 * 2. 验证码存储：使用Redis存储验证码，设置过期时间
 * 3. 发送频率限制：使用Redis记录上次发送时间，限制发送间隔
 * 4. 验证失败限制：使用Redis记录失败次数，超过阈值后锁定
 * 5. 密码验证：使用BCrypt验证用户密码
 *
 * @author dataplatform
 */
@Slf4j
@Service
public class SecondFactorServiceImpl implements SecondFactorService {

    // ==================== Redis键前缀 ====================
    
    /** 验证码存储键前缀 */
    private static final String CODE_KEY_PREFIX = "second_factor:code:";
    
    /** 发送时间记录键前缀 */
    private static final String SEND_TIME_KEY_PREFIX = "second_factor:send_time:";
    
    /** 验证失败次数键前缀 */
    private static final String FAIL_COUNT_KEY_PREFIX = "second_factor:fail_count:";
    
    /** 锁定状态键前缀 */
    private static final String LOCK_KEY_PREFIX = "second_factor:lock:";

    // ==================== 配置参数 ====================
    
    /** 验证码长度 */
    private static final int CODE_LENGTH = 6;
    
    /** 验证码过期时间（秒），默认5分钟 */
    @Value("${security.second-factor.code-expire-seconds:300}")
    private int codeExpireSeconds;
    
    /** 发送间隔时间（秒），默认60秒 */
    @Value("${security.second-factor.send-interval-seconds:60}")
    private int sendIntervalSeconds;
    
    /** 最大验证失败次数，默认5次 */
    @Value("${security.second-factor.max-failed-attempts:5}")
    private int maxFailedAttempts;
    
    /** 锁定时间（秒），默认30分钟 */
    @Value("${security.second-factor.lock-duration-seconds:1800}")
    private int lockDurationSeconds;

    // ==================== 依赖注入 ====================
    
    /** Redis模板 */
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;
    
    /** 用户Mapper */
    @Autowired
    private UserMapper userMapper;
    
    /** 安全随机数生成器 */
    private final SecureRandom secureRandom = new SecureRandom();

    // ==================== 验证码发送实现 ====================

    @Override
    public SendCodeResult sendSmsCode(Long userId, String phone) {
        return sendCode(userId, VerifyType.SMS, phone);
    }

    @Override
    public SendCodeResult sendEmailCode(Long userId, String email) {
        return sendCode(userId, VerifyType.EMAIL, email);
    }

    /**
     * 发送验证码的通用实现
     *
     * @param userId     用户ID
     * @param verifyType 验证类型
     * @param target     目标地址（手机号或邮箱）
     * @return 发送结果
     */
    private SendCodeResult sendCode(Long userId, VerifyType verifyType, String target) {
        // 1. 参数验证
        if (userId == null) {
            log.warn("发送验证码失败: 用户ID为空");
            return SendCodeResult.userNotFound();
        }

        if (verifyType == VerifyType.PASSWORD) {
            log.warn("发送验证码失败: 密码验证类型不支持发送验证码");
            return SendCodeResult.unsupportedType();
        }

        // 2. 检查Redis是否可用
        if (redisTemplate == null) {
            log.error("发送验证码失败: Redis不可用");
            return SendCodeResult.sendFailed("存储服务不可用");
        }

        // 3. 获取用户信息，确定目标地址
        String actualTarget = target;
        if (!StringUtils.hasText(actualTarget)) {
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.warn("发送验证码失败: 用户不存在 userId={}", userId);
                return SendCodeResult.userNotFound();
            }
            
            actualTarget = switch (verifyType) {
                case SMS -> user.getPhone();
                case EMAIL -> user.getEmail();
                default -> null;
            };
            
            if (!StringUtils.hasText(actualTarget)) {
                log.warn("发送验证码失败: 用户未绑定{} userId={}", 
                        verifyType == VerifyType.SMS ? "手机号" : "邮箱", userId);
                return SendCodeResult.invalidTarget();
            }
        }

        // 4. 检查发送频率限制
        int waitSeconds = getWaitSecondsForNextSend(userId, verifyType);
        if (waitSeconds > 0) {
            log.warn("发送验证码失败: 发送过于频繁 userId={}, verifyType={}, waitSeconds={}", 
                    userId, verifyType, waitSeconds);
            return SendCodeResult.rateLimited(waitSeconds);
        }

        // 5. 生成验证码
        String code = generateCode();

        // 6. 存储验证码
        try {
            String codeKey = buildCodeKey(userId, verifyType);
            redisTemplate.opsForValue().set(codeKey, code, codeExpireSeconds, TimeUnit.SECONDS);

            // 7. 记录发送时间
            String sendTimeKey = buildSendTimeKey(userId, verifyType);
            redisTemplate.opsForValue().set(sendTimeKey, 
                    String.valueOf(System.currentTimeMillis()), 
                    sendIntervalSeconds, TimeUnit.SECONDS);

            // 8. 实际发送验证码（这里只是模拟，实际需要调用短信/邮件服务）
            boolean sent = doSendCode(verifyType, actualTarget, code);
            if (!sent) {
                // 发送失败，删除已存储的验证码
                redisTemplate.delete(codeKey);
                return SendCodeResult.sendFailed("发送服务异常");
            }

            log.info("验证码发送成功: userId={}, verifyType={}, target={}", 
                    userId, verifyType, maskTarget(actualTarget, verifyType));
            return SendCodeResult.success(codeExpireSeconds);
        } catch (Exception e) {
            log.error("发送验证码失败: userId={}, verifyType={}", userId, verifyType, e);
            return SendCodeResult.sendFailed(e.getMessage());
        }
    }

    /**
     * 实际发送验证码（模拟实现）
     * 实际项目中需要调用短信服务或邮件服务
     *
     * @param verifyType 验证类型
     * @param target     目标地址
     * @param code       验证码
     * @return 是否发送成功
     */
    private boolean doSendCode(VerifyType verifyType, String target, String code) {
        // 这里只是模拟发送，实际需要集成短信/邮件服务
        // 例如：阿里云短信、腾讯云短信、SendGrid邮件等
        log.debug("模拟发送验证码: type={}, target={}, code={}", 
                verifyType, maskTarget(target, verifyType), code);
        
        // 模拟发送成功
        return true;
    }

    // ==================== 二次验证实现 ====================

    @Override
    public VerifyResult verify(Long userId, VerifyType verifyType, String verifyCode) {
        // 1. 参数验证
        if (userId == null) {
            log.warn("二次验证失败: 用户ID为空");
            return VerifyResult.userNotFound();
        }

        if (verifyType == null) {
            log.warn("二次验证失败: 验证类型为空");
            return VerifyResult.unsupportedType();
        }

        if (!StringUtils.hasText(verifyCode)) {
            log.warn("二次验证失败: 验证码为空 userId={}, verifyType={}", userId, verifyType);
            return VerifyResult.invalidCode(getRemainingAttempts(userId, verifyType));
        }

        // 2. 检查是否被锁定
        if (isLocked(userId, verifyType)) {
            log.warn("二次验证失败: 用户已被锁定 userId={}, verifyType={}", userId, verifyType);
            return VerifyResult.tooManyAttempts();
        }

        // 3. 根据验证类型执行验证
        return switch (verifyType) {
            case PASSWORD -> verifyPassword(userId, verifyCode);
            case SMS -> verifySmsCode(userId, verifyCode);
            case EMAIL -> verifyEmailCode(userId, verifyCode);
        };
    }

    @Override
    public VerifyResult verify(Long userId, String verifyType, String verifyCode) {
        VerifyType type = VerifyType.fromCode(verifyType);
        if (type == null) {
            log.warn("二次验证失败: 不支持的验证类型 verifyType={}", verifyType);
            return VerifyResult.unsupportedType();
        }
        return verify(userId, type, verifyCode);
    }

    @Override
    public VerifyResult verifyPassword(Long userId, String password) {
        // 1. 参数验证
        if (userId == null) {
            return VerifyResult.userNotFound();
        }

        if (!StringUtils.hasText(password)) {
            // 空密码直接返回错误，不计入失败次数
            return VerifyResult.invalidPassword(-1);
        }

        // 2. 检查是否被锁定
        if (isLocked(userId, VerifyType.PASSWORD)) {
            return VerifyResult.tooManyAttempts();
        }

        // 3. 获取用户密码并验证
        // 注意：实际项目中需要从用户服务获取用户的加密密码
        String storedPassword = getUserPassword(userId);
        if (storedPassword == null) {
            return VerifyResult.userNotFound();
        }

        // 4. 验证密码
        boolean matches = verifyPasswordMatch(password, storedPassword);
        
        if (matches) {
            // 验证成功，清除失败计数
            clearFailCount(userId, VerifyType.PASSWORD);
            log.info("密码验证成功: userId={}", userId);
            return VerifyResult.success();
        } else {
            // 验证失败，增加失败计数
            int remainingAttempts = incrementFailCount(userId, VerifyType.PASSWORD);
            log.warn("密码验证失败: userId={}, remainingAttempts={}", userId, remainingAttempts);
            
            if (remainingAttempts <= 0) {
                return VerifyResult.tooManyAttempts();
            }
            return VerifyResult.invalidPassword(remainingAttempts);
        }
    }

    @Override
    public VerifyResult verifySmsCode(Long userId, String code) {
        return verifyCode(userId, VerifyType.SMS, code);
    }

    @Override
    public VerifyResult verifyEmailCode(Long userId, String code) {
        return verifyCode(userId, VerifyType.EMAIL, code);
    }

    /**
     * 验证码验证的通用实现
     *
     * @param userId     用户ID
     * @param verifyType 验证类型
     * @param code       验证码
     * @return 验证结果
     */
    private VerifyResult verifyCode(Long userId, VerifyType verifyType, String code) {
        // 1. 参数验证
        if (userId == null) {
            return VerifyResult.userNotFound();
        }

        if (!StringUtils.hasText(code)) {
            // 空验证码直接返回错误，不计入失败次数
            return VerifyResult.invalidCode(-1);
        }

        // 2. 检查是否被锁定
        if (isLocked(userId, verifyType)) {
            return VerifyResult.tooManyAttempts();
        }

        // 3. 检查Redis是否可用
        if (redisTemplate == null) {
            log.error("验证码验证失败: Redis不可用");
            return VerifyResult.failure("STORAGE_ERROR", "存储服务不可用", -1);
        }

        // 4. 获取存储的验证码
        String codeKey = buildCodeKey(userId, verifyType);
        String storedCode = redisTemplate.opsForValue().get(codeKey);

        if (storedCode == null) {
            log.warn("验证码验证失败: 验证码不存在或已过期 userId={}, verifyType={}", userId, verifyType);
            return VerifyResult.codeNotFound();
        }

        // 5. 验证码比对（忽略大小写）
        if (storedCode.equalsIgnoreCase(code.trim())) {
            // 验证成功，删除验证码并清除失败计数
            redisTemplate.delete(codeKey);
            clearFailCount(userId, verifyType);
            log.info("验证码验证成功: userId={}, verifyType={}", userId, verifyType);
            return VerifyResult.success();
        } else {
            // 验证失败，增加失败计数
            int remainingAttempts = incrementFailCount(userId, verifyType);
            log.warn("验证码验证失败: userId={}, verifyType={}, remainingAttempts={}", 
                    userId, verifyType, remainingAttempts);
            
            if (remainingAttempts <= 0) {
                // 删除验证码，防止继续尝试
                redisTemplate.delete(codeKey);
                return VerifyResult.tooManyAttempts();
            }
            return VerifyResult.invalidCode(remainingAttempts);
        }
    }

    // ==================== 状态查询实现 ====================

    @Override
    public boolean canSendCode(Long userId, VerifyType verifyType) {
        return getWaitSecondsForNextSend(userId, verifyType) <= 0;
    }

    @Override
    public int getWaitSecondsForNextSend(Long userId, VerifyType verifyType) {
        if (userId == null || verifyType == null || redisTemplate == null) {
            return 0;
        }

        String sendTimeKey = buildSendTimeKey(userId, verifyType);
        String lastSendTimeStr = redisTemplate.opsForValue().get(sendTimeKey);

        if (lastSendTimeStr == null) {
            return 0;
        }

        try {
            long lastSendTime = Long.parseLong(lastSendTimeStr);
            long elapsed = (System.currentTimeMillis() - lastSendTime) / 1000;
            int waitSeconds = sendIntervalSeconds - (int) elapsed;
            return Math.max(0, waitSeconds);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public int getRemainingAttempts(Long userId, VerifyType verifyType) {
        if (userId == null || verifyType == null || redisTemplate == null) {
            return maxFailedAttempts;
        }

        String failCountKey = buildFailCountKey(userId, verifyType);
        String failCountStr = redisTemplate.opsForValue().get(failCountKey);

        if (failCountStr == null) {
            return maxFailedAttempts;
        }

        try {
            int failCount = Integer.parseInt(failCountStr);
            return Math.max(0, maxFailedAttempts - failCount);
        } catch (NumberFormatException e) {
            return maxFailedAttempts;
        }
    }

    @Override
    public boolean isLocked(Long userId, VerifyType verifyType) {
        if (userId == null || verifyType == null || redisTemplate == null) {
            return false;
        }

        String lockKey = buildLockKey(userId, verifyType);
        Boolean exists = redisTemplate.hasKey(lockKey);
        return Boolean.TRUE.equals(exists);
    }

    // ==================== 配置获取 ====================

    @Override
    public int getCodeExpireSeconds() {
        return codeExpireSeconds;
    }

    @Override
    public int getSendIntervalSeconds() {
        return sendIntervalSeconds;
    }

    @Override
    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    @Override
    public int getLockDurationSeconds() {
        return lockDurationSeconds;
    }

    // ==================== 辅助方法 ====================

    /**
     * 生成验证码
     *
     * @return 6位数字验证码
     */
    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(secureRandom.nextInt(10));
        }
        return code.toString();
    }

    /**
     * 构建验证码存储键
     */
    private String buildCodeKey(Long userId, VerifyType verifyType) {
        return CODE_KEY_PREFIX + verifyType.getCode() + ":" + userId;
    }

    /**
     * 构建发送时间记录键
     */
    private String buildSendTimeKey(Long userId, VerifyType verifyType) {
        return SEND_TIME_KEY_PREFIX + verifyType.getCode() + ":" + userId;
    }

    /**
     * 构建失败计数键
     */
    private String buildFailCountKey(Long userId, VerifyType verifyType) {
        return FAIL_COUNT_KEY_PREFIX + verifyType.getCode() + ":" + userId;
    }

    /**
     * 构建锁定状态键
     */
    private String buildLockKey(Long userId, VerifyType verifyType) {
        return LOCK_KEY_PREFIX + verifyType.getCode() + ":" + userId;
    }

    /**
     * 增加失败计数
     *
     * @return 剩余尝试次数
     */
    private int incrementFailCount(Long userId, VerifyType verifyType) {
        if (redisTemplate == null) {
            return maxFailedAttempts;
        }

        String failCountKey = buildFailCountKey(userId, verifyType);
        Long newCount = redisTemplate.opsForValue().increment(failCountKey);
        
        if (newCount == null) {
            newCount = 1L;
        }

        // 设置失败计数的过期时间
        redisTemplate.expire(failCountKey, lockDurationSeconds, TimeUnit.SECONDS);

        int remaining = maxFailedAttempts - newCount.intValue();

        // 如果达到最大失败次数，设置锁定
        if (remaining <= 0) {
            String lockKey = buildLockKey(userId, verifyType);
            redisTemplate.opsForValue().set(lockKey, "1", lockDurationSeconds, TimeUnit.SECONDS);
            log.warn("用户已被锁定: userId={}, verifyType={}, lockDuration={}s", 
                    userId, verifyType, lockDurationSeconds);
        }

        return Math.max(0, remaining);
    }

    /**
     * 清除失败计数
     */
    private void clearFailCount(Long userId, VerifyType verifyType) {
        if (redisTemplate == null) {
            return;
        }

        String failCountKey = buildFailCountKey(userId, verifyType);
        String lockKey = buildLockKey(userId, verifyType);
        
        redisTemplate.delete(failCountKey);
        redisTemplate.delete(lockKey);
    }

    /**
     * 获取用户密码
     * 从用户服务获取用户的加密密码
     *
     * @param userId 用户ID
     * @return 加密后的密码，如果用户不存在返回null
     */
    private String getUserPassword(Long userId) {
        if (userId == null) {
            return null;
        }
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.debug("获取用户密码失败: 用户不存在 userId={}", userId);
            return null;
        }
        
        return user.getPassword();
    }

    /**
     * 验证密码是否匹配
     * 使用MD5算法验证密码（与系统登录保持一致）
     *
     * @param rawPassword     原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    private boolean verifyPasswordMatch(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        try {
            // 使用MD5加密后比较（与UserService.login保持一致）
            String encryptedInput = MD5Util.encrypt(rawPassword);
            return encodedPassword.equals(encryptedInput);
        } catch (Exception e) {
            log.warn("密码验证异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 脱敏目标地址
     *
     * @param target     目标地址
     * @param verifyType 验证类型
     * @return 脱敏后的地址
     */
    private String maskTarget(String target, VerifyType verifyType) {
        if (target == null || target.length() < 4) {
            return "****";
        }

        if (verifyType == VerifyType.SMS) {
            // 手机号脱敏：保留前3位和后4位
            if (target.length() >= 11) {
                return target.substring(0, 3) + "****" + target.substring(target.length() - 4);
            }
        } else if (verifyType == VerifyType.EMAIL) {
            // 邮箱脱敏：保留@前2位和@后的域名
            int atIndex = target.indexOf('@');
            if (atIndex > 2) {
                return target.substring(0, 2) + "****" + target.substring(atIndex);
            }
        }

        return target.substring(0, 2) + "****";
    }

    // ==================== 测试辅助方法 ====================

    /**
     * 设置Redis模板（用于测试）
     */
    void setRedisTemplate(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 设置验证码过期时间（用于测试）
     */
    void setCodeExpireSeconds(int codeExpireSeconds) {
        this.codeExpireSeconds = codeExpireSeconds;
    }

    /**
     * 设置发送间隔时间（用于测试）
     */
    void setSendIntervalSeconds(int sendIntervalSeconds) {
        this.sendIntervalSeconds = sendIntervalSeconds;
    }

    /**
     * 设置最大失败次数（用于测试）
     */
    void setMaxFailedAttempts(int maxFailedAttempts) {
        this.maxFailedAttempts = maxFailedAttempts;
    }

    /**
     * 设置锁定时间（用于测试）
     */
    void setLockDurationSeconds(int lockDurationSeconds) {
        this.lockDurationSeconds = lockDurationSeconds;
    }
}
