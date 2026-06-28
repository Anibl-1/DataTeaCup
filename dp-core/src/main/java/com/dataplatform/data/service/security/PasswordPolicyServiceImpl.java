package com.dataplatform.data.service.security;

import com.dataplatform.common.util.MD5Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 密码策略服务实现类
 * 实现密码复杂度检查和历史密码检查
 * 
 * 需求 4.3: THE Security_Engine SHALL 实现密码策略配置，包括最小长度、复杂度要求、历史密码检查
 *
 * @author dataplatform
 */
@Slf4j
@Service
public class PasswordPolicyServiceImpl implements PasswordPolicyService {

    private static final String PASSWORD_HISTORY_KEY_PREFIX = "password:history:";
    
    /** 大写字母 */
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    /** 小写字母 */
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    /** 数字 */
    private static final String DIGITS = "0123456789";
    /** 特殊字符 */
    private static final String SPECIAL = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    @Value("${security.password.min-length:8}")
    private int minLength;

    @Value("${security.password.max-length:32}")
    private int maxLength;

    @Value("${security.password.require-uppercase:true}")
    private boolean requireUppercase;

    @Value("${security.password.require-lowercase:true}")
    private boolean requireLowercase;

    @Value("${security.password.require-digit:true}")
    private boolean requireDigit;

    @Value("${security.password.require-special:true}")
    private boolean requireSpecial;

    @Value("${security.password.history-count:5}")
    private int historyCount;

    @Value("${security.password.max-repeating-chars:3}")
    private int maxRepeatingChars;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private final SecureRandom random = new SecureRandom();

    /** 常见弱密码模式 */
    private static final List<Pattern> WEAK_PATTERNS = Arrays.asList(
            Pattern.compile("^(password|passwd|pwd)\\d*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\d{6,}$"),  // 纯数字
            Pattern.compile("^(qwerty|asdfgh|zxcvbn)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(123456|654321|111111|000000)"),
            Pattern.compile("^(admin|root|user)\\d*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(abc|xyz)\\d*$", Pattern.CASE_INSENSITIVE)
    );

    @Override
    public ValidationResult validatePassword(String password) {
        List<String> errors = new ArrayList<>();

        if (!StringUtils.hasText(password)) {
            errors.add("密码不能为空");
            return ValidationResult.failure(errors);
        }

        // 长度检查
        if (password.length() < minLength) {
            errors.add("密码长度不能少于" + minLength + "位");
        }
        if (password.length() > maxLength) {
            errors.add("密码长度不能超过" + maxLength + "位");
        }

        // 复杂度检查
        if (requireUppercase && !containsUppercase(password)) {
            errors.add("密码必须包含大写字母");
        }
        if (requireLowercase && !containsLowercase(password)) {
            errors.add("密码必须包含小写字母");
        }
        if (requireDigit && !containsDigit(password)) {
            errors.add("密码必须包含数字");
        }
        if (requireSpecial && !containsSpecial(password)) {
            errors.add("密码必须包含特殊字符");
        }

        // 重复字符检查
        if (hasRepeatingChars(password, maxRepeatingChars)) {
            errors.add("密码不能包含超过" + maxRepeatingChars + "个连续相同字符");
        }

        // 弱密码检查
        if (isWeakPassword(password)) {
            errors.add("密码过于简单，请使用更复杂的密码");
        }

        if (errors.isEmpty()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(errors);
    }

    @Override
    public ValidationResult validatePassword(Long userId, String password) {
        // 先进行基本验证
        ValidationResult basicResult = validatePassword(password);
        if (!basicResult.valid()) {
            return basicResult;
        }

        // 历史密码检查
        if (userId != null && isPasswordInHistory(userId, password)) {
            return ValidationResult.failure("不能使用最近" + historyCount + "次使用过的密码");
        }

        return ValidationResult.success();
    }

    @Override
    public boolean isPasswordInHistory(Long userId, String password) {
        if (userId == null || !StringUtils.hasText(password) || redisTemplate == null) {
            return false;
        }

        String key = PASSWORD_HISTORY_KEY_PREFIX + userId;
        try {
            List<String> history = redisTemplate.opsForList().range(key, 0, -1);
            if (history == null || history.isEmpty()) {
                return false;
            }

            String passwordHash = MD5Util.encrypt(password);
            return history.contains(passwordHash);
        } catch (Exception e) {
            log.error("检查历史密码失败: userId={}", userId, e);
            return false;
        }
    }

    @Override
    public void savePasswordHistory(Long userId, String passwordHash) {
        if (userId == null || !StringUtils.hasText(passwordHash) || redisTemplate == null) {
            return;
        }

        String key = PASSWORD_HISTORY_KEY_PREFIX + userId;
        try {
            // 添加到列表头部
            redisTemplate.opsForList().leftPush(key, passwordHash);
            // 保留最近N条记录
            redisTemplate.opsForList().trim(key, 0, historyCount - 1);
            log.debug("保存密码历史: userId={}", userId);
        } catch (Exception e) {
            log.error("保存密码历史失败: userId={}", userId, e);
        }
    }

    @Override
    public PasswordPolicy getPolicy() {
        return new PasswordPolicy(
                minLength,
                maxLength,
                requireUppercase,
                requireLowercase,
                requireDigit,
                requireSpecial,
                historyCount,
                maxRepeatingChars,
                WEAK_PATTERNS.stream().map(Pattern::pattern).toList()
        );
    }

    @Override
    public String generateRandomPassword() {
        StringBuilder password = new StringBuilder();
        StringBuilder allChars = new StringBuilder();

        // 确保包含必需的字符类型
        if (requireUppercase) {
            password.append(UPPERCASE.charAt(random.nextInt(UPPERCASE.length())));
            allChars.append(UPPERCASE);
        }
        if (requireLowercase) {
            password.append(LOWERCASE.charAt(random.nextInt(LOWERCASE.length())));
            allChars.append(LOWERCASE);
        }
        if (requireDigit) {
            password.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
            allChars.append(DIGITS);
        }
        if (requireSpecial) {
            password.append(SPECIAL.charAt(random.nextInt(SPECIAL.length())));
            allChars.append(SPECIAL);
        }

        // 填充剩余长度
        int remaining = minLength - password.length();
        for (int i = 0; i < remaining; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // 打乱顺序
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        return new String(chars);
    }

    @Override
    public int calculateStrength(String password) {
        if (!StringUtils.hasText(password)) {
            return 0;
        }

        int score = 0;

        // 长度得分（最高30分）
        score += Math.min(password.length() * 3, 30);

        // 字符类型得分（每种15分，最高60分）
        if (containsUppercase(password)) score += 15;
        if (containsLowercase(password)) score += 15;
        if (containsDigit(password)) score += 15;
        if (containsSpecial(password)) score += 15;

        // 扣分项
        if (hasRepeatingChars(password, 2)) score -= 10;
        if (isWeakPassword(password)) score -= 20;
        if (password.length() < 8) score -= 20;

        return Math.max(0, Math.min(100, score));
    }

    // ==================== 私有方法 ====================

    private boolean containsUppercase(String password) {
        return password.chars().anyMatch(Character::isUpperCase);
    }

    private boolean containsLowercase(String password) {
        return password.chars().anyMatch(Character::isLowerCase);
    }

    private boolean containsDigit(String password) {
        return password.chars().anyMatch(Character::isDigit);
    }

    private boolean containsSpecial(String password) {
        return password.chars().anyMatch(c -> SPECIAL.indexOf(c) >= 0);
    }

    private boolean hasRepeatingChars(String password, int maxRepeat) {
        if (password.length() < maxRepeat) {
            return false;
        }
        int count = 1;
        for (int i = 1; i < password.length(); i++) {
            if (password.charAt(i) == password.charAt(i - 1)) {
                count++;
                if (count > maxRepeat) {
                    return true;
                }
            } else {
                count = 1;
            }
        }
        return false;
    }

    private boolean isWeakPassword(String password) {
        for (Pattern pattern : WEAK_PATTERNS) {
            if (pattern.matcher(password).matches()) {
                return true;
            }
        }
        return false;
    }
}
