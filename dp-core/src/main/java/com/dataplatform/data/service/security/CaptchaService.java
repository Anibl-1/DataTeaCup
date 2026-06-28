package com.dataplatform.data.service.security;

/**
 * 验证码服务接口
 * 提供图形验证码和滑动验证码功能
 * 
 * 需求 4.1: THE Security_Engine SHALL 支持图形验证码，在登录失败3次后强制显示
 * 需求 4.2: THE Security_Engine SHALL 支持滑动验证码，作为图形验证码的替代方案
 *
 * @author dataplatform
 */
public interface CaptchaService {

    /**
     * 验证码类型
     */
    enum CaptchaType {
        /** 图形验证码 */
        IMAGE("image"),
        /** 滑动验证码 */
        SLIDER("slider");

        private final String code;

        CaptchaType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static CaptchaType fromCode(String code) {
            for (CaptchaType type : values()) {
                if (type.code.equalsIgnoreCase(code)) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * 图形验证码结果
     */
    record ImageCaptchaResult(
            String captchaId,
            String imageBase64,
            int expireSeconds
    ) {}

    /**
     * 滑动验证码结果
     */
    record SliderCaptchaResult(
            String captchaId,
            String backgroundBase64,
            String sliderBase64,
            int sliderY,
            int expireSeconds
    ) {}

    /**
     * 验证结果
     */
    record VerifyResult(
            boolean isSuccess,
            String errorCode,
            String message
    ) {
        public static VerifyResult success() {
            return new VerifyResult(true, null, "验证成功");
        }

        public static VerifyResult failure(String errorCode, String message) {
            return new VerifyResult(false, errorCode, message);
        }

        public static VerifyResult expired() {
            return new VerifyResult(false, "CAPTCHA_EXPIRED", "验证码已过期");
        }

        public static VerifyResult invalid() {
            return new VerifyResult(false, "CAPTCHA_INVALID", "验证码错误");
        }

        public static VerifyResult notFound() {
            return new VerifyResult(false, "CAPTCHA_NOT_FOUND", "验证码不存在");
        }
    }

    /**
     * 生成图形验证码
     *
     * @return 图形验证码结果，包含验证码ID和Base64编码的图片
     */
    ImageCaptchaResult generateImageCaptcha();

    /**
     * 生成滑动验证码
     *
     * @return 滑动验证码结果，包含背景图、滑块图和滑块Y坐标
     */
    SliderCaptchaResult generateSliderCaptcha();

    /**
     * 验证图形验证码
     *
     * @param captchaId 验证码ID
     * @param code      用户输入的验证码
     * @return 验证结果
     */
    VerifyResult verifyImageCaptcha(String captchaId, String code);

    /**
     * 验证滑动验证码
     *
     * @param captchaId 验证码ID
     * @param sliderX   用户滑动的X坐标
     * @return 验证结果
     */
    VerifyResult verifySliderCaptcha(String captchaId, int sliderX);

    /**
     * 检查是否需要显示验证码
     * 根据登录失败次数判断
     *
     * @param username 用户名
     * @return true 表示需要显示验证码
     */
    boolean isRequired(String username);

    /**
     * 记录登录失败
     *
     * @param username 用户名
     */
    void recordLoginFailure(String username);

    /**
     * 清除登录失败记录
     *
     * @param username 用户名
     */
    void clearLoginFailure(String username);

    /**
     * 获取登录失败次数
     *
     * @param username 用户名
     * @return 失败次数
     */
    int getLoginFailureCount(String username);
}
