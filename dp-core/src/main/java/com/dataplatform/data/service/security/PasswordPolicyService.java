package com.dataplatform.data.service.security;

import java.util.List;

/**
 * 密码策略服务接口
 * 实现密码复杂度检查和历史密码检查
 * 
 * 需求 4.3: THE Security_Engine SHALL 实现密码策略配置，包括最小长度、复杂度要求、历史密码检查
 *
 * @author dataplatform
 */
public interface PasswordPolicyService {

    /**
     * 密码验证结果
     */
    record ValidationResult(
            boolean valid,
            List<String> errors
    ) {
        public static ValidationResult success() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors);
        }

        public static ValidationResult failure(String error) {
            return new ValidationResult(false, List.of(error));
        }
    }

    /**
     * 密码策略配置
     */
    record PasswordPolicy(
            int minLength,
            int maxLength,
            boolean requireUppercase,
            boolean requireLowercase,
            boolean requireDigit,
            boolean requireSpecial,
            int historyCount,
            int maxRepeatingChars,
            List<String> forbiddenPatterns
    ) {}

    /**
     * 验证密码是否符合策略
     *
     * @param password 密码
     * @return 验证结果
     */
    ValidationResult validatePassword(String password);

    /**
     * 验证密码是否符合策略（包含历史密码检查）
     *
     * @param userId   用户ID
     * @param password 新密码
     * @return 验证结果
     */
    ValidationResult validatePassword(Long userId, String password);

    /**
     * 检查密码是否在历史密码中
     *
     * @param userId   用户ID
     * @param password 密码
     * @return true 表示密码在历史记录中
     */
    boolean isPasswordInHistory(Long userId, String password);

    /**
     * 保存密码到历史记录
     *
     * @param userId       用户ID
     * @param passwordHash 密码哈希
     */
    void savePasswordHistory(Long userId, String passwordHash);

    /**
     * 获取当前密码策略
     *
     * @return 密码策略配置
     */
    PasswordPolicy getPolicy();

    /**
     * 生成符合策略的随机密码
     *
     * @return 随机密码
     */
    String generateRandomPassword();

    /**
     * 计算密码强度（0-100）
     *
     * @param password 密码
     * @return 强度分数
     */
    int calculateStrength(String password);
}
