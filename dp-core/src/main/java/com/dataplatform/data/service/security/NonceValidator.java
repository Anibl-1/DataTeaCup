package com.dataplatform.data.service.security;

/**
 * Nonce验证器接口
 * 实现防重放攻击机制，使用nonce和时间戳验证请求唯一性
 * 
 * 需求 3.5: THE Security_Engine SHALL 实现防重放攻击机制，使用nonce和时间戳验证请求唯一性
 *
 * @author dataplatform
 */
public interface NonceValidator {

    /**
     * 验证nonce是否有效（未被使用过）
     * 检查nonce是否已被使用，如果未使用则标记为已使用
     *
     * @param nonce     随机数
     * @param timestamp 时间戳（毫秒）
     * @return 验证结果
     */
    NonceValidationResult validateAndMark(String nonce, long timestamp);

    /**
     * 仅验证nonce是否有效（不标记）
     * 检查nonce是否已被使用，不会标记为已使用
     *
     * @param nonce 随机数
     * @return true 表示nonce有效（未被使用），false 表示已被使用
     */
    boolean isNonceValid(String nonce);

    /**
     * 检查nonce是否已被使用
     *
     * @param nonce 随机数
     * @return true 表示已被使用，false 表示未被使用
     */
    boolean isNonceUsed(String nonce);

    /**
     * 标记nonce为已使用
     *
     * @param nonce     随机数
     * @param timestamp 时间戳（毫秒），用于计算过期时间
     * @return true 表示标记成功，false 表示标记失败（可能已被使用）
     */
    boolean markNonceAsUsed(String nonce, long timestamp);

    /**
     * 验证时间戳是否在有效范围内
     *
     * @param timestamp 时间戳（毫秒）
     * @return true 表示时间戳有效，false 表示已过期或无效
     */
    boolean isTimestampValid(long timestamp);

    /**
     * 获取nonce过期时间（秒）
     *
     * @return nonce过期时间
     */
    long getNonceExpireSeconds();

    /**
     * 设置nonce过期时间（秒）
     *
     * @param expireSeconds 过期时间
     */
    void setNonceExpireSeconds(long expireSeconds);

    /**
     * Nonce验证结果
     */
    record NonceValidationResult(
            boolean valid,
            NonceErrorCode errorCode,
            String message
    ) {
        /**
         * 创建成功结果
         */
        public static NonceValidationResult success() {
            return new NonceValidationResult(true, null, "Nonce验证通过");
        }

        /**
         * 创建失败结果
         */
        public static NonceValidationResult failure(NonceErrorCode errorCode, String message) {
            return new NonceValidationResult(false, errorCode, message);
        }

        /**
         * 创建nonce为空的失败结果
         */
        public static NonceValidationResult emptyNonce() {
            return failure(NonceErrorCode.EMPTY_NONCE, "Nonce不能为空");
        }

        /**
         * 创建nonce已使用的失败结果
         */
        public static NonceValidationResult nonceAlreadyUsed() {
            return failure(NonceErrorCode.NONCE_ALREADY_USED, "Nonce已被使用，请求可能是重放攻击");
        }

        /**
         * 创建时间戳过期的失败结果
         */
        public static NonceValidationResult timestampExpired() {
            return failure(NonceErrorCode.TIMESTAMP_EXPIRED, "请求时间戳已过期");
        }

        /**
         * 创建时间戳无效的失败结果
         */
        public static NonceValidationResult invalidTimestamp() {
            return failure(NonceErrorCode.INVALID_TIMESTAMP, "无效的时间戳");
        }

        /**
         * 创建存储错误的失败结果
         */
        public static NonceValidationResult storageError(String message) {
            return failure(NonceErrorCode.STORAGE_ERROR, "存储错误: " + message);
        }
    }

    /**
     * Nonce错误码
     */
    enum NonceErrorCode {
        /** Nonce为空 */
        EMPTY_NONCE,
        /** Nonce已被使用 */
        NONCE_ALREADY_USED,
        /** 时间戳已过期 */
        TIMESTAMP_EXPIRED,
        /** 无效的时间戳 */
        INVALID_TIMESTAMP,
        /** 存储错误 */
        STORAGE_ERROR
    }
}
