package com.dataplatform.data.service.security;

/**
 * 多因素认证服务接口
 * 实现TOTP动态口令功能
 * 
 * 需求 4.5: THE Security_Engine SHALL 支持多因素认证（MFA），支持TOTP动态口令
 *
 * @author dataplatform
 */
public interface MfaService {

    /**
     * MFA设置结果
     */
    record MfaSetupResult(
            String secretKey,
            String qrCodeUrl,
            String qrCodeBase64,
            String[] backupCodes
    ) {}

    /**
     * MFA验证结果
     */
    record MfaVerifyResult(
            boolean isSuccess,
            String errorCode,
            String message
    ) {
        public static MfaVerifyResult success() {
            return new MfaVerifyResult(true, null, "验证成功");
        }

        public static MfaVerifyResult failure(String errorCode, String message) {
            return new MfaVerifyResult(false, errorCode, message);
        }

        public static MfaVerifyResult invalidCode() {
            return new MfaVerifyResult(false, "INVALID_CODE", "验证码错误");
        }

        public static MfaVerifyResult expired() {
            return new MfaVerifyResult(false, "CODE_EXPIRED", "验证码已过期");
        }

        public static MfaVerifyResult notEnabled() {
            return new MfaVerifyResult(false, "MFA_NOT_ENABLED", "用户未启用MFA");
        }
    }

    /**
     * 生成MFA密钥和二维码
     *
     * @param userId   用户ID
     * @param username 用户名（用于显示）
     * @return MFA设置结果
     */
    MfaSetupResult generateSecret(Long userId, String username);

    /**
     * 启用MFA
     *
     * @param userId 用户ID
     * @param code   验证码（用于确认设置）
     * @return 验证结果
     */
    MfaVerifyResult enableMfa(Long userId, String code);

    /**
     * 禁用MFA
     *
     * @param userId   用户ID
     * @param password 用户密码（用于确认）
     * @return true 表示禁用成功
     */
    boolean disableMfa(Long userId, String password);

    /**
     * 验证TOTP码
     *
     * @param userId 用户ID
     * @param code   TOTP验证码
     * @return 验证结果
     */
    MfaVerifyResult verifyCode(Long userId, String code);

    /**
     * 验证备用码
     *
     * @param userId     用户ID
     * @param backupCode 备用码
     * @return 验证结果
     */
    MfaVerifyResult verifyBackupCode(Long userId, String backupCode);

    /**
     * 检查用户是否启用了MFA
     *
     * @param userId 用户ID
     * @return true 表示已启用MFA
     */
    boolean isMfaEnabled(Long userId);

    /**
     * 重新生成备用码
     *
     * @param userId 用户ID
     * @return 新的备用码数组
     */
    String[] regenerateBackupCodes(Long userId);

    /**
     * 获取剩余备用码数量
     *
     * @param userId 用户ID
     * @return 剩余备用码数量
     */
    int getRemainingBackupCodes(Long userId);
}
