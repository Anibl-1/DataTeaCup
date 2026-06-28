package com.dataplatform.data.service.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 签名验证器接口
 * 实现API请求签名验证，防止请求被篡改
 * 
 * 需求 3.4: THE Security_Engine SHALL 实现API请求签名验证，防止请求被篡改
 *
 * @author dataplatform
 */
public interface SignatureVerifier {

    /**
     * 验证请求签名
     * 验证HTTP请求的签名是否有效
     *
     * @param request HTTP请求
     * @return 验证结果
     */
    SignatureVerificationResult verifyRequest(HttpServletRequest request);

    /**
     * 验证签名
     * 根据提供的参数验证签名是否有效
     *
     * @param timestamp 时间戳（毫秒）
     * @param nonce     随机数
     * @param params    请求参数（已排序的查询字符串）
     * @param signature 待验证的签名
     * @param secretKey 签名密钥
     * @return 验证结果
     */
    SignatureVerificationResult verifySignature(long timestamp, String nonce, 
                                                 String params, String signature, 
                                                 String secretKey);

    /**
     * 生成签名
     * 根据提供的参数生成HMAC-SHA256签名
     *
     * @param timestamp 时间戳（毫秒）
     * @param nonce     随机数
     * @param params    请求参数（已排序的查询字符串）
     * @param secretKey 签名密钥
     * @return 生成的签名（十六进制字符串）
     */
    String generateSignature(long timestamp, String nonce, String params, String secretKey);

    /**
     * 生成随机数
     * 生成用于签名的随机数（nonce）
     *
     * @return 随机数字符串
     */
    String generateNonce();

    /**
     * 检查时间戳是否过期
     * 验证请求时间戳是否在有效期内
     *
     * @param timestamp 时间戳（毫秒）
     * @return true 表示时间戳有效，false 表示已过期
     */
    boolean isTimestampValid(long timestamp);

    /**
     * 获取签名过期时间（秒）
     *
     * @return 签名过期时间
     */
    long getSignatureExpireSeconds();

    /**
     * 签名验证结果
     */
    record SignatureVerificationResult(
            boolean valid,
            SignatureErrorCode errorCode,
            String message
    ) {
        /**
         * 创建成功结果
         */
        public static SignatureVerificationResult success() {
            return new SignatureVerificationResult(true, null, "签名验证通过");
        }

        /**
         * 创建失败结果
         */
        public static SignatureVerificationResult failure(SignatureErrorCode errorCode, String message) {
            return new SignatureVerificationResult(false, errorCode, message);
        }

        /**
         * 创建缺少参数的失败结果
         */
        public static SignatureVerificationResult missingParameter(String paramName) {
            return failure(SignatureErrorCode.MISSING_PARAMETER, "缺少签名参数: " + paramName);
        }

        /**
         * 创建时间戳过期的失败结果
         */
        public static SignatureVerificationResult timestampExpired() {
            return failure(SignatureErrorCode.TIMESTAMP_EXPIRED, "请求时间戳已过期");
        }

        /**
         * 创建签名不匹配的失败结果
         */
        public static SignatureVerificationResult signatureMismatch() {
            return failure(SignatureErrorCode.SIGNATURE_MISMATCH, "签名验证失败");
        }

        /**
         * 创建无效时间戳的失败结果
         */
        public static SignatureVerificationResult invalidTimestamp() {
            return failure(SignatureErrorCode.INVALID_TIMESTAMP, "无效的时间戳格式");
        }
    }

    /**
     * 签名错误码
     */
    enum SignatureErrorCode {
        /** 缺少签名参数 */
        MISSING_PARAMETER,
        /** 时间戳已过期 */
        TIMESTAMP_EXPIRED,
        /** 签名不匹配 */
        SIGNATURE_MISMATCH,
        /** 无效的时间戳格式 */
        INVALID_TIMESTAMP,
        /** 签名计算错误 */
        SIGNATURE_COMPUTATION_ERROR
    }
}
