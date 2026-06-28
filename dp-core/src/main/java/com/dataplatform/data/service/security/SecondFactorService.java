package com.dataplatform.data.service.security;

/**
 * 二次验证服务接口
 * 提供敏感操作的二次验证功能，支持密码确认、短信验证码、邮箱验证码等方式
 * 
 * 需求 3.6: THE Security_Engine SHALL 实现敏感操作二次验证，支持密码确认、短信验证码、邮箱验证码
 *
 * @author dataplatform
 */
public interface SecondFactorService {

    /**
     * 验证类型枚举
     */
    enum VerifyType {
        /** 密码确认 */
        PASSWORD("password", "密码确认"),
        /** 短信验证码 */
        SMS("sms", "短信验证码"),
        /** 邮箱验证码 */
        EMAIL("email", "邮箱验证码");

        private final String code;
        private final String description;

        VerifyType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        /**
         * 根据code获取验证类型
         *
         * @param code 验证类型代码
         * @return 验证类型，如果不存在返回null
         */
        public static VerifyType fromCode(String code) {
            if (code == null) {
                return null;
            }
            for (VerifyType type : values()) {
                if (type.code.equalsIgnoreCase(code)) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * 验证结果
     */
    record VerifyResult(
            boolean successful,
            String errorCode,
            String message,
            int remainingAttempts
    ) {
        /**
         * 创建成功结果
         */
        public static VerifyResult success() {
            return new VerifyResult(true, null, "验证成功", -1);
        }

        /**
         * 创建失败结果
         */
        public static VerifyResult failure(String errorCode, String message, int remainingAttempts) {
            return new VerifyResult(false, errorCode, message, remainingAttempts);
        }

        /**
         * 验证码错误
         */
        public static VerifyResult invalidCode(int remainingAttempts) {
            return new VerifyResult(false, "INVALID_CODE", "验证码错误", remainingAttempts);
        }

        /**
         * 验证码已过期
         */
        public static VerifyResult codeExpired() {
            return new VerifyResult(false, "CODE_EXPIRED", "验证码已过期", -1);
        }

        /**
         * 验证码不存在
         */
        public static VerifyResult codeNotFound() {
            return new VerifyResult(false, "CODE_NOT_FOUND", "请先获取验证码", -1);
        }

        /**
         * 密码错误
         */
        public static VerifyResult invalidPassword(int remainingAttempts) {
            return new VerifyResult(false, "INVALID_PASSWORD", "密码错误", remainingAttempts);
        }

        /**
         * 验证次数超限
         */
        public static VerifyResult tooManyAttempts() {
            return new VerifyResult(false, "TOO_MANY_ATTEMPTS", "验证失败次数过多，请稍后重试", 0);
        }

        /**
         * 发送频率限制
         */
        public static VerifyResult rateLimited(int waitSeconds) {
            return new VerifyResult(false, "RATE_LIMITED", 
                    "发送过于频繁，请" + waitSeconds + "秒后重试", -1);
        }

        /**
         * 用户不存在
         */
        public static VerifyResult userNotFound() {
            return new VerifyResult(false, "USER_NOT_FOUND", "用户不存在", -1);
        }

        /**
         * 发送失败
         */
        public static VerifyResult sendFailed(String reason) {
            return new VerifyResult(false, "SEND_FAILED", "发送失败: " + reason, -1);
        }

        /**
         * 不支持的验证类型
         */
        public static VerifyResult unsupportedType() {
            return new VerifyResult(false, "UNSUPPORTED_TYPE", "不支持的验证类型", -1);
        }
    }

    /**
     * 发送验证码结果
     */
    record SendCodeResult(
            boolean successful,
            String errorCode,
            String message,
            int expireSeconds
    ) {
        /**
         * 创建成功结果
         */
        public static SendCodeResult success(int expireSeconds) {
            return new SendCodeResult(true, null, "验证码已发送", expireSeconds);
        }

        /**
         * 创建失败结果
         */
        public static SendCodeResult failure(String errorCode, String message) {
            return new SendCodeResult(false, errorCode, message, 0);
        }

        /**
         * 发送频率限制
         */
        public static SendCodeResult rateLimited(int waitSeconds) {
            return new SendCodeResult(false, "RATE_LIMITED", 
                    "发送过于频繁，请" + waitSeconds + "秒后重试", 0);
        }

        /**
         * 用户不存在
         */
        public static SendCodeResult userNotFound() {
            return new SendCodeResult(false, "USER_NOT_FOUND", "用户不存在", 0);
        }

        /**
         * 发送失败
         */
        public static SendCodeResult sendFailed(String reason) {
            return new SendCodeResult(false, "SEND_FAILED", "发送失败: " + reason, 0);
        }

        /**
         * 不支持的验证类型
         */
        public static SendCodeResult unsupportedType() {
            return new SendCodeResult(false, "UNSUPPORTED_TYPE", "不支持的验证类型", 0);
        }

        /**
         * 目标地址无效
         */
        public static SendCodeResult invalidTarget() {
            return new SendCodeResult(false, "INVALID_TARGET", "目标地址无效", 0);
        }
    }

    // ==================== 验证码发送 ====================

    /**
     * 发送短信验证码
     *
     * @param userId 用户ID
     * @param phone  手机号（可选，如果为null则使用用户绑定的手机号）
     * @return 发送结果
     */
    SendCodeResult sendSmsCode(Long userId, String phone);

    /**
     * 发送邮箱验证码
     *
     * @param userId 用户ID
     * @param email  邮箱地址（可选，如果为null则使用用户绑定的邮箱）
     * @return 发送结果
     */
    SendCodeResult sendEmailCode(Long userId, String email);

    // ==================== 二次验证 ====================

    /**
     * 执行二次验证
     *
     * @param userId     用户ID
     * @param verifyType 验证类型
     * @param verifyCode 验证码或密码
     * @return 验证结果
     */
    VerifyResult verify(Long userId, VerifyType verifyType, String verifyCode);

    /**
     * 执行二次验证（使用字符串类型）
     *
     * @param userId     用户ID
     * @param verifyType 验证类型代码
     * @param verifyCode 验证码或密码
     * @return 验证结果
     */
    VerifyResult verify(Long userId, String verifyType, String verifyCode);

    /**
     * 验证密码
     *
     * @param userId   用户ID
     * @param password 密码
     * @return 验证结果
     */
    VerifyResult verifyPassword(Long userId, String password);

    /**
     * 验证短信验证码
     *
     * @param userId 用户ID
     * @param code   验证码
     * @return 验证结果
     */
    VerifyResult verifySmsCode(Long userId, String code);

    /**
     * 验证邮箱验证码
     *
     * @param userId 用户ID
     * @param code   验证码
     * @return 验证结果
     */
    VerifyResult verifyEmailCode(Long userId, String code);

    // ==================== 状态查询 ====================

    /**
     * 检查是否可以发送验证码（频率限制检查）
     *
     * @param userId     用户ID
     * @param verifyType 验证类型
     * @return 如果可以发送返回true，否则返回false
     */
    boolean canSendCode(Long userId, VerifyType verifyType);

    /**
     * 获取下次可发送验证码的等待时间（秒）
     *
     * @param userId     用户ID
     * @param verifyType 验证类型
     * @return 等待时间（秒），如果可以立即发送返回0
     */
    int getWaitSecondsForNextSend(Long userId, VerifyType verifyType);

    /**
     * 获取剩余验证尝试次数
     *
     * @param userId     用户ID
     * @param verifyType 验证类型
     * @return 剩余尝试次数，-1表示无限制
     */
    int getRemainingAttempts(Long userId, VerifyType verifyType);

    /**
     * 检查用户是否被锁定（验证失败次数过多）
     *
     * @param userId     用户ID
     * @param verifyType 验证类型
     * @return 如果被锁定返回true
     */
    boolean isLocked(Long userId, VerifyType verifyType);

    // ==================== 配置 ====================

    /**
     * 获取验证码过期时间（秒）
     *
     * @return 过期时间
     */
    int getCodeExpireSeconds();

    /**
     * 获取发送间隔时间（秒）
     *
     * @return 发送间隔
     */
    int getSendIntervalSeconds();

    /**
     * 获取最大验证失败次数
     *
     * @return 最大失败次数
     */
    int getMaxFailedAttempts();

    /**
     * 获取锁定时间（秒）
     *
     * @return 锁定时间
     */
    int getLockDurationSeconds();
}
