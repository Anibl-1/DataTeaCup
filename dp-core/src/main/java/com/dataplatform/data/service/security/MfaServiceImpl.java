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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 多因素认证服务实现类
 * 实现TOTP动态口令功能
 * 
 * 需求 4.5: THE Security_Engine SHALL 支持多因素认证（MFA），支持TOTP动态口令
 *
 * @author dataplatform
 */
@Slf4j
@Service
public class MfaServiceImpl implements MfaService {

    private static final String MFA_SECRET_KEY_PREFIX = "mfa:secret:";
    private static final String MFA_BACKUP_KEY_PREFIX = "mfa:backup:";
    private static final String MFA_TEMP_SECRET_PREFIX = "mfa:temp:";
    
    /** TOTP时间步长（秒） */
    private static final int TIME_STEP = 30;
    /** TOTP码长度 */
    private static final int CODE_DIGITS = 6;
    /** 允许的时间偏移窗口 */
    private static final int TIME_WINDOW = 1;
    /** 备用码数量 */
    private static final int BACKUP_CODE_COUNT = 10;
    /** Base32字符集 */
    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    @Value("${security.mfa.issuer:DataTeaCup}")
    private String issuer;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserMapper userMapper;

    private final SecureRandom random = new SecureRandom();

    @Override
    public MfaSetupResult generateSecret(Long userId, String username) {
        if (userId == null || !StringUtils.hasText(username)) {
            return null;
        }

        // 生成密钥
        String secretKey = generateSecretKey();

        // 临时存储密钥（等待用户确认）
        storeTempSecret(userId, secretKey);

        // 生成二维码URL
        String qrCodeUrl = generateQrCodeUrl(username, secretKey);

        // 生成二维码图片
        String qrCodeBase64 = generateQrCodeImage(qrCodeUrl);

        // 生成备用码
        String[] backupCodes = generateBackupCodes();

        log.info("生成MFA密钥: userId={}", userId);
        return new MfaSetupResult(secretKey, qrCodeUrl, qrCodeBase64, backupCodes);
    }

    @Override
    public MfaVerifyResult enableMfa(Long userId, String code) {
        if (userId == null || !StringUtils.hasText(code)) {
            return MfaVerifyResult.invalidCode();
        }

        // 获取临时密钥
        String tempSecret = getTempSecret(userId);
        if (tempSecret == null) {
            return MfaVerifyResult.failure("SECRET_NOT_FOUND", "请先生成MFA密钥");
        }

        // 验证码
        if (!verifyTotpCode(tempSecret, code)) {
            return MfaVerifyResult.invalidCode();
        }

        // 保存密钥
        saveSecret(userId, tempSecret);

        // 生成并保存备用码
        String[] backupCodes = generateBackupCodes();
        saveBackupCodes(userId, backupCodes);

        // 删除临时密钥
        deleteTempSecret(userId);

        log.info("启用MFA成功: userId={}", userId);
        return MfaVerifyResult.success();
    }

    @Override
    public boolean disableMfa(Long userId, String password) {
        if (userId == null || !StringUtils.hasText(password)) {
            return false;
        }

        // 验证密码
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }

        String encryptedPassword = MD5Util.encrypt(password);
        if (!encryptedPassword.equals(user.getPassword())) {
            log.warn("禁用MFA失败: 密码错误 userId={}", userId);
            return false;
        }

        // 删除MFA数据
        deleteSecret(userId);
        deleteBackupCodes(userId);

        log.info("禁用MFA成功: userId={}", userId);
        return true;
    }

    @Override
    public MfaVerifyResult verifyCode(Long userId, String code) {
        if (userId == null || !StringUtils.hasText(code)) {
            return MfaVerifyResult.invalidCode();
        }

        // 获取密钥
        String secret = getSecret(userId);
        if (secret == null) {
            return MfaVerifyResult.notEnabled();
        }

        // 验证TOTP码
        if (verifyTotpCode(secret, code)) {
            log.debug("MFA验证成功: userId={}", userId);
            return MfaVerifyResult.success();
        }

        log.warn("MFA验证失败: userId={}", userId);
        return MfaVerifyResult.invalidCode();
    }

    @Override
    public MfaVerifyResult verifyBackupCode(Long userId, String backupCode) {
        if (userId == null || !StringUtils.hasText(backupCode)) {
            return MfaVerifyResult.invalidCode();
        }

        // 检查是否启用MFA
        if (!isMfaEnabled(userId)) {
            return MfaVerifyResult.notEnabled();
        }

        // 验证并消费备用码
        if (consumeBackupCode(userId, backupCode.trim().toUpperCase())) {
            log.info("备用码验证成功: userId={}", userId);
            return MfaVerifyResult.success();
        }

        log.warn("备用码验证失败: userId={}", userId);
        return MfaVerifyResult.invalidCode();
    }

    @Override
    public boolean isMfaEnabled(Long userId) {
        if (userId == null) {
            return false;
        }
        return getSecret(userId) != null;
    }

    @Override
    public String[] regenerateBackupCodes(Long userId) {
        if (userId == null || !isMfaEnabled(userId)) {
            return new String[0];
        }

        String[] backupCodes = generateBackupCodes();
        saveBackupCodes(userId, backupCodes);

        log.info("重新生成备用码: userId={}", userId);
        return backupCodes;
    }

    @Override
    public int getRemainingBackupCodes(Long userId) {
        if (userId == null || redisTemplate == null) {
            return 0;
        }

        String key = MFA_BACKUP_KEY_PREFIX + userId;
        try {
            Long size = redisTemplate.opsForSet().size(key);
            return size != null ? size.intValue() : 0;
        } catch (Exception e) {
            log.error("获取备用码数量失败: userId={}", userId, e);
            return 0;
        }
    }

    // ==================== 私有方法 ====================

    private String generateSecretKey() {
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return base32Encode(bytes);
    }

    private String base32Encode(byte[] data) {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;

        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                result.append(BASE32_CHARS.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
                bitsLeft -= 5;
            }
        }

        if (bitsLeft > 0) {
            result.append(BASE32_CHARS.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }

        return result.toString();
    }

    private byte[] base32Decode(String encoded) {
        encoded = encoded.toUpperCase().replaceAll("[^A-Z2-7]", "");
        byte[] result = new byte[encoded.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;

        for (char c : encoded.toCharArray()) {
            int value = BASE32_CHARS.indexOf(c);
            if (value < 0) continue;

            buffer = (buffer << 5) | value;
            bitsLeft += 5;

            if (bitsLeft >= 8) {
                result[index++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }

        return Arrays.copyOf(result, index);
    }

    private String generateQrCodeUrl(String username, String secretKey) {
        try {
            String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
            return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                    encodedIssuer, encodedUsername, secretKey, encodedIssuer, CODE_DIGITS, TIME_STEP);
        } catch (Exception e) {
            log.error("生成二维码URL失败", e);
            return null;
        }
    }

    private String generateQrCodeImage(String content) {
        if (content == null) {
            return null;
        }

        try {
            // 简单的二维码生成（实际项目中应使用ZXing等库）
            int size = 200;
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, size, size);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, 10));
            
            // 绘制提示文字（实际应生成真正的二维码）
            g.drawString("请使用Google Authenticator", 20, 80);
            g.drawString("或其他TOTP应用扫描", 40, 100);
            g.drawString("Secret: " + content.substring(content.indexOf("secret=") + 7, 
                    content.indexOf("secret=") + 23) + "...", 10, 130);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("生成二维码图片失败", e);
            return null;
        }
    }

    private String[] generateBackupCodes() {
        String[] codes = new String[BACKUP_CODE_COUNT];
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            codes[i] = generateBackupCode();
        }
        return codes;
    }

    private String generateBackupCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(BASE32_CHARS.charAt(random.nextInt(BASE32_CHARS.length())));
        }
        return code.toString();
    }

    private boolean verifyTotpCode(String secret, String code) {
        if (!StringUtils.hasText(secret) || !StringUtils.hasText(code)) {
            return false;
        }

        try {
            long currentTime = System.currentTimeMillis() / 1000 / TIME_STEP;
            
            // 检查当前时间窗口和前后窗口
            for (int i = -TIME_WINDOW; i <= TIME_WINDOW; i++) {
                String expectedCode = generateTotpCode(secret, currentTime + i);
                if (expectedCode.equals(code.trim())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("验证TOTP码失败", e);
            return false;
        }
    }

    private String generateTotpCode(String secret, long counter) throws Exception {
        byte[] key = base32Decode(secret);
        byte[] data = new byte[8];
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (counter & 0xFF);
            counter >>= 8;
        }

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(data);

        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24) |
                     ((hash[offset + 1] & 0xFF) << 16) |
                     ((hash[offset + 2] & 0xFF) << 8) |
                     (hash[offset + 3] & 0xFF);

        int otp = binary % (int) Math.pow(10, CODE_DIGITS);
        return String.format("%0" + CODE_DIGITS + "d", otp);
    }

    // ==================== Redis操作 ====================

    private void storeTempSecret(Long userId, String secret) {
        if (redisTemplate == null) return;
        String key = MFA_TEMP_SECRET_PREFIX + userId;
        redisTemplate.opsForValue().set(key, secret, 10, TimeUnit.MINUTES);
    }

    private String getTempSecret(Long userId) {
        if (redisTemplate == null) return null;
        String key = MFA_TEMP_SECRET_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }

    private void deleteTempSecret(Long userId) {
        if (redisTemplate == null) return;
        String key = MFA_TEMP_SECRET_PREFIX + userId;
        redisTemplate.delete(key);
    }

    private void saveSecret(Long userId, String secret) {
        if (redisTemplate == null) return;
        String key = MFA_SECRET_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, secret);
    }

    private String getSecret(Long userId) {
        if (redisTemplate == null) return null;
        String key = MFA_SECRET_KEY_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }

    private void deleteSecret(Long userId) {
        if (redisTemplate == null) return;
        String key = MFA_SECRET_KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }

    private void saveBackupCodes(Long userId, String[] codes) {
        if (redisTemplate == null) return;
        String key = MFA_BACKUP_KEY_PREFIX + userId;
        redisTemplate.delete(key);
        redisTemplate.opsForSet().add(key, codes);
    }

    private boolean consumeBackupCode(Long userId, String code) {
        if (redisTemplate == null) return false;
        String key = MFA_BACKUP_KEY_PREFIX + userId;
        Long removed = redisTemplate.opsForSet().remove(key, code);
        return removed != null && removed > 0;
    }

    private void deleteBackupCodes(Long userId) {
        if (redisTemplate == null) return;
        String key = MFA_BACKUP_KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }
}
