package com.dataplatform.data.service.security;

/**
 * 安全防护服务接口
 * 提供SQL注入检测、XSS攻击检测、CSRF防护和输入验证功能
 * 
 * 验证需求: 3.1, 3.2, 3.3
 *
 * @author dataplatform
 */
public interface SecurityProtectionService {

    /**
     * 检测SQL注入攻击
     * 对用户输入进行SQL注入检测，识别危险的SQL注入模式
     * 
     * 需求 3.1: THE Security_Engine SHALL 实现SQL注入防护，对所有用户输入进行参数化处理和危险字符过滤
     *
     * @param input 用户输入
     * @return true 表示检测到SQL注入攻击，false 表示安全
     */
    boolean detectSqlInjection(String input);

    /**
     * 过滤XSS攻击内容
     * 对输入内容进行XSS过滤，移除或转义危险的脚本内容
     * 
     * 需求 3.2: THE Security_Engine SHALL 实现XSS防护，对所有输出内容进行HTML实体编码
     *
     * @param input 用户输入
     * @return 过滤后的安全内容
     */
    String filterXss(String input);

    /**
     * 检测XSS攻击
     * 检测输入内容是否包含XSS攻击代码
     *
     * @param input 用户输入
     * @return true 表示检测到XSS攻击，false 表示安全
     */
    boolean detectXss(String input);

    /**
     * HTML实体编码
     * 将特殊字符转换为HTML实体，防止XSS攻击
     * 
     * 需求 3.2: 对所有输出内容进行HTML实体编码
     *
     * @param input 用户输入
     * @return HTML实体编码后的内容
     */
    String encodeHtml(String input);

    /**
     * 生成CSRF Token
     * 生成用于CSRF防护的随机Token
     * 
     * 需求 3.3: THE Security_Engine SHALL 实现CSRF防护，使用Token验证所有状态变更请求
     *
     * @param sessionId 会话ID
     * @return CSRF Token
     */
    String generateCsrfToken(String sessionId);

    /**
     * 验证CSRF Token
     * 验证请求中的CSRF Token是否有效
     *
     * @param sessionId 会话ID
     * @param token     请求中的Token
     * @return true 表示Token有效，false 表示无效
     */
    boolean validateCsrfToken(String sessionId, String token);

    /**
     * 验证输入内容
     * 综合验证输入内容的安全性，包括SQL注入和XSS检测
     *
     * @param input 用户输入
     * @return 验证结果
     */
    InputValidationResult validateInput(String input);

    /**
     * 清理和过滤输入内容
     * 对输入内容进行清理，移除危险字符和脚本
     *
     * @param input 用户输入
     * @return 清理后的安全内容
     */
    String sanitizeInput(String input);

    /**
     * 请求签名验证
     * 验证API请求的签名是否有效，防止请求被篡改
     * 
     * 需求 3.4: THE Security_Engine SHALL 实现请求签名验证，防止API请求被篡改
     *
     * @param request HTTP请求
     * @return true 表示签名有效，false 表示签名无效
     */
    boolean verifySignature(jakarta.servlet.http.HttpServletRequest request);

    /**
     * 防重放验证
     * 验证请求的唯一性，防止重放攻击
     * 
     * 需求 3.5: THE Security_Engine SHALL 实现防重放攻击机制，使用时间戳和随机数验证请求唯一性
     *
     * @param nonce     随机数
     * @param timestamp 时间戳
     * @return true 表示请求有效，false 表示可能是重放攻击
     */
    boolean verifyNonce(String nonce, long timestamp);

    /**
     * 二次验证
     * 执行敏感操作的二次验证，支持密码确认、短信验证码、邮箱验证码
     * 
     * 需求 3.6: THE Security_Engine SHALL 实现敏感操作二次验证，支持密码确认、短信验证码、邮箱验证码
     *
     * @param userId     用户ID
     * @param verifyType 验证类型（password/sms/email）
     * @param verifyCode 验证码或密码
     * @return true 表示验证成功，false 表示验证失败
     */
    boolean verifySecondFactor(Long userId, String verifyType, String verifyCode);

    /**
     * 输入验证结果
     */
    record InputValidationResult(
            boolean safe,
            boolean hasSqlInjection,
            boolean hasXss,
            String sanitizedInput,
            String message
    ) {
        public static InputValidationResult safe(String sanitizedInput) {
            return new InputValidationResult(true, false, false, sanitizedInput, "输入验证通过");
        }

        public static InputValidationResult sqlInjection(String input) {
            return new InputValidationResult(false, true, false, input, "检测到SQL注入攻击");
        }

        public static InputValidationResult xssAttack(String input) {
            return new InputValidationResult(false, false, true, input, "检测到XSS攻击");
        }

        public static InputValidationResult bothAttacks(String input) {
            return new InputValidationResult(false, true, true, input, "检测到SQL注入和XSS攻击");
        }
    }
}
