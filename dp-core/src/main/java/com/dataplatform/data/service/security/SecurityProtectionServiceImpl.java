package com.dataplatform.data.service.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 安全防护服务实现类
 * 实现SQL注入检测、XSS攻击检测、CSRF防护和输入验证功能
 * 
 * 验证需求: 3.1, 3.2, 3.3
 *
 * @author dataplatform
 */
@Slf4j
@Service
public class SecurityProtectionServiceImpl implements SecurityProtectionService {

    private static final String CSRF_TOKEN_PREFIX = "csrf:token:";
    private static final int CSRF_TOKEN_LENGTH = 32;
    private static final long CSRF_TOKEN_EXPIRE_MINUTES = 30;

    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SignatureVerifier signatureVerifier;

    @Autowired
    private NonceValidator nonceValidator;

    @Autowired
    private SecondFactorService secondFactorService;

    // ==================== SQL注入检测相关 ====================

    /**
     * SQL注入攻击模式
     * 需求 3.1: 实现SQL注入防护
     */
    private static final List<Pattern> SQL_INJECTION_PATTERNS = Arrays.asList(
            // 基本SQL关键字注入
            Pattern.compile("(?i)\\b(SELECT|INSERT|UPDATE|DELETE|DROP|TRUNCATE|ALTER|CREATE|EXEC|EXECUTE)\\b.*\\b(FROM|INTO|TABLE|DATABASE)\\b"),
            // UNION注入
            Pattern.compile("(?i)\\bUNION\\b.*\\bSELECT\\b"),
            // 注释注入
            Pattern.compile("(?i)(--|#|/\\*|\\*/|;)"),
            // 单引号注入
            Pattern.compile("'\\s*(OR|AND)\\s*'", Pattern.CASE_INSENSITIVE),
            // 数字型注入
            Pattern.compile("\\d+\\s*(OR|AND)\\s*\\d+\\s*=\\s*\\d+", Pattern.CASE_INSENSITIVE),
            // 布尔盲注
            Pattern.compile("(?i)'\\s*(OR|AND)\\s*['\"]?\\d+['\"]?\\s*=\\s*['\"]?\\d+['\"]?"),
            // 时间盲注
            Pattern.compile("(?i)\\b(SLEEP|BENCHMARK|WAITFOR|DELAY)\\b\\s*\\("),
            // 堆叠查询
            Pattern.compile(";\\s*(?i)(SELECT|INSERT|UPDATE|DELETE|DROP|TRUNCATE)\\b"),
            // 系统函数调用
            Pattern.compile("(?i)\\b(LOAD_FILE|INTO\\s+OUTFILE|INTO\\s+DUMPFILE)\\b"),
            // 信息获取
            Pattern.compile("(?i)\\b(INFORMATION_SCHEMA|MYSQL|SYS|PERFORMANCE_SCHEMA)\\b"),
            // 十六进制编码绕过
            Pattern.compile("(?i)0x[0-9a-fA-F]+"),
            // CHAR函数绕过
            Pattern.compile("(?i)\\bCHAR\\s*\\(\\s*\\d+"),
            // 条件注入
            Pattern.compile("(?i)'\\s*;\\s*--"),
            // 空字节注入
            Pattern.compile("\\x00")
    );

    /**
     * SQL危险关键字
     */
    private static final Set<String> SQL_DANGEROUS_KEYWORDS = new HashSet<>(Arrays.asList(
            "DROP", "DELETE", "TRUNCATE", "ALTER", "CREATE", "INSERT", "UPDATE",
            "GRANT", "REVOKE", "EXEC", "EXECUTE", "CALL", "MERGE", "REPLACE",
            "SHUTDOWN", "KILL", "LOAD", "OUTFILE", "DUMPFILE"
    ));

    // ==================== XSS检测相关 ====================

    /**
     * XSS攻击模式
     * 需求 3.2: 实现XSS防护
     */
    private static final List<Pattern> XSS_PATTERNS = Arrays.asList(
            // script标签
            Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("<script[^>]*>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
            // 事件处理器
            Pattern.compile("\\bon\\w+\\s*=", Pattern.CASE_INSENSITIVE),
            // javascript协议
            Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE),
            // vbscript协议
            Pattern.compile("vbscript\\s*:", Pattern.CASE_INSENSITIVE),
            // data协议
            Pattern.compile("data\\s*:", Pattern.CASE_INSENSITIVE),
            // expression
            Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE),
            // iframe
            Pattern.compile("<iframe[^>]*>", Pattern.CASE_INSENSITIVE),
            // object
            Pattern.compile("<object[^>]*>", Pattern.CASE_INSENSITIVE),
            // embed
            Pattern.compile("<embed[^>]*>", Pattern.CASE_INSENSITIVE),
            // form
            Pattern.compile("<form[^>]*>", Pattern.CASE_INSENSITIVE),
            // input
            Pattern.compile("<input[^>]*>", Pattern.CASE_INSENSITIVE),
            // svg事件
            Pattern.compile("<svg[^>]*on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
            // img事件
            Pattern.compile("<img[^>]*on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
            // body事件
            Pattern.compile("<body[^>]*on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
            // style注入
            Pattern.compile("style\\s*=\\s*['\"]?[^'\"]*expression", Pattern.CASE_INSENSITIVE),
            // 编码绕过
            Pattern.compile("&#x?\\d+;?"),
            // eval函数
            Pattern.compile("\\beval\\s*\\(", Pattern.CASE_INSENSITIVE),
            // document对象
            Pattern.compile("\\bdocument\\s*\\.", Pattern.CASE_INSENSITIVE),
            // window对象
            Pattern.compile("\\bwindow\\s*\\.", Pattern.CASE_INSENSITIVE),
            // alert函数
            Pattern.compile("\\balert\\s*\\(", Pattern.CASE_INSENSITIVE)
    );

    /**
     * HTML特殊字符映射
     */
    private static final Map<Character, String> HTML_ENTITIES = Map.of(
            '&', "&amp;",
            '<', "&lt;",
            '>', "&gt;",
            '"', "&quot;",
            '\'', "&#x27;",
            '/', "&#x2F;"
    );

    // ==================== SQL注入检测实现 ====================

    @Override
    public boolean detectSqlInjection(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }

        String normalizedInput = normalizeInput(input);

        // 检查SQL注入模式
        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(normalizedInput).find()) {
                log.warn("检测到SQL注入攻击模式: pattern={}, input={}", pattern.pattern(), truncateForLog(input));
                return true;
            }
        }

        // 检查危险关键字组合
        String upperInput = normalizedInput.toUpperCase();
        int keywordCount = 0;
        for (String keyword : SQL_DANGEROUS_KEYWORDS) {
            if (upperInput.contains(keyword)) {
                keywordCount++;
                if (keywordCount >= 2) {
                    log.warn("检测到多个SQL危险关键字: input={}", truncateForLog(input));
                    return true;
                }
            }
        }

        return false;
    }

    // ==================== XSS检测和过滤实现 ====================

    @Override
    public String filterXss(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }

        String result = input;

        // 移除XSS攻击模式
        for (Pattern pattern : XSS_PATTERNS) {
            result = pattern.matcher(result).replaceAll("");
        }

        // 移除空字节
        result = result.replace("\0", "");

        return result;
    }

    @Override
    public boolean detectXss(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }

        String normalizedInput = normalizeInput(input);

        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(normalizedInput).find()) {
                log.warn("检测到XSS攻击模式: pattern={}, input={}", pattern.pattern(), truncateForLog(input));
                return true;
            }
        }

        return false;
    }

    @Override
    public String encodeHtml(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }

        StringBuilder encoded = new StringBuilder(input.length() * 2);
        for (char c : input.toCharArray()) {
            String entity = HTML_ENTITIES.get(c);
            if (entity != null) {
                encoded.append(entity);
            } else {
                encoded.append(c);
            }
        }

        return encoded.toString();
    }

    // ==================== CSRF防护实现 ====================

    @Override
    public String generateCsrfToken(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("会话ID不能为空");
        }

        String token = generateSecureToken();

        // 存储到Redis（如果可用）
        if (redisTemplate != null) {
            String key = CSRF_TOKEN_PREFIX + sessionId;
            redisTemplate.opsForValue().set(key, token, CSRF_TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);
            log.debug("生成CSRF Token: sessionId={}", sessionId);
        } else {
            log.warn("Redis不可用，CSRF Token仅在内存中生成");
        }

        return token;
    }

    @Override
    public boolean validateCsrfToken(String sessionId, String token) {
        if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(token)) {
            log.warn("CSRF验证失败: 会话ID或Token为空");
            return false;
        }

        if (redisTemplate == null) {
            log.warn("Redis不可用，无法验证CSRF Token");
            return false;
        }

        String key = CSRF_TOKEN_PREFIX + sessionId;
        String storedToken = redisTemplate.opsForValue().get(key);

        if (storedToken == null) {
            log.warn("CSRF验证失败: Token不存在或已过期, sessionId={}", sessionId);
            return false;
        }

        boolean valid = storedToken.equals(token);
        if (!valid) {
            log.warn("CSRF验证失败: Token不匹配, sessionId={}", sessionId);
        }

        return valid;
    }

    // ==================== 输入验证实现 ====================

    @Override
    public InputValidationResult validateInput(String input) {
        if (!StringUtils.hasText(input)) {
            return InputValidationResult.safe(input);
        }

        boolean hasSqlInjection = detectSqlInjection(input);
        boolean hasXss = detectXss(input);

        if (hasSqlInjection && hasXss) {
            return InputValidationResult.bothAttacks(input);
        } else if (hasSqlInjection) {
            return InputValidationResult.sqlInjection(input);
        } else if (hasXss) {
            return InputValidationResult.xssAttack(input);
        }

        String sanitized = sanitizeInput(input);
        return InputValidationResult.safe(sanitized);
    }

    @Override
    public String sanitizeInput(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }

        String result = input;

        // 1. 移除空字节
        result = result.replace("\0", "");

        // 2. 过滤XSS
        result = filterXss(result);

        // 3. 移除SQL注释
        result = result.replaceAll("--.*$", "");
        result = result.replaceAll("/\\*.*?\\*/", "");
        result = result.replaceAll("#.*$", "");

        // 4. 规范化空白字符
        result = result.replaceAll("\\s+", " ").trim();

        return result;
    }

    // ==================== 请求签名验证实现 ====================

    @Override
    public boolean verifySignature(HttpServletRequest request) {
        if (request == null) {
            log.warn("请求签名验证失败: 请求为空");
            return false;
        }
        SignatureVerifier.SignatureVerificationResult result = signatureVerifier.verifyRequest(request);
        if (!result.valid()) {
            log.warn("请求签名验证失败: errorCode={}, message={}", result.errorCode(), result.message());
        }
        return result.valid();
    }

    // ==================== 防重放验证实现 ====================

    @Override
    public boolean verifyNonce(String nonce, long timestamp) {
        if (!StringUtils.hasText(nonce)) {
            log.warn("防重放验证失败: nonce为空");
            return false;
        }
        NonceValidator.NonceValidationResult result = nonceValidator.validateAndMark(nonce, timestamp);
        if (!result.valid()) {
            log.warn("防重放验证失败: errorCode={}, message={}", result.errorCode(), result.message());
        }
        return result.valid();
    }

    // ==================== 二次验证实现 ====================

    @Override
    public boolean verifySecondFactor(Long userId, String verifyType, String verifyCode) {
        if (userId == null) {
            log.warn("二次验证失败: 用户ID为空");
            return false;
        }
        if (!StringUtils.hasText(verifyType)) {
            log.warn("二次验证失败: 验证类型为空");
            return false;
        }
        if (!StringUtils.hasText(verifyCode)) {
            log.warn("二次验证失败: 验证码为空");
            return false;
        }

        SecondFactorService.VerifyResult result = secondFactorService.verify(userId, verifyType, verifyCode);
        
        if (result.successful()) {
            log.info("二次验证成功: userId={}, verifyType={}", userId, verifyType);
            return true;
        } else {
            log.warn("二次验证失败: userId={}, verifyType={}, errorCode={}, message={}", 
                    userId, verifyType, result.errorCode(), result.message());
            return false;
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 规范化输入（用于检测）
     */
    private String normalizeInput(String input) {
        if (input == null) {
            return "";
        }

        // 解码URL编码
        String decoded = input;
        try {
            decoded = java.net.URLDecoder.decode(input, "UTF-8");
        } catch (Exception e) {
            log.trace("URL解码失败，使用原始输入: {}", e.getMessage());
        }

        // 移除多余空白
        decoded = decoded.replaceAll("\\s+", " ");

        return decoded;
    }

    /**
     * 生成安全的随机Token
     */
    private String generateSecureToken() {
        byte[] bytes = new byte[CSRF_TOKEN_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 截断日志输出（防止日志注入）
     */
    private String truncateForLog(String input) {
        if (input == null) {
            return "null";
        }
        if (input.length() > 100) {
            return input.substring(0, 100) + "...";
        }
        return input.replaceAll("[\\r\\n]", " ");
    }
}
