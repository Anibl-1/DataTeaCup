package com.dataplatform.data.service.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务实现类
 * 实现图形验证码和滑动验证码功能
 * 
 * 需求 4.1: THE Security_Engine SHALL 支持图形验证码，在登录失败3次后强制显示
 * 需求 4.2: THE Security_Engine SHALL 支持滑动验证码，作为图形验证码的替代方案
 *
 * @author dataplatform
 */
@Slf4j
@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final String CAPTCHA_KEY_PREFIX = "captcha:code:";
    private static final String SLIDER_KEY_PREFIX = "captcha:slider:";
    private static final String LOGIN_FAIL_KEY_PREFIX = "captcha:login_fail:";

    /** 验证码字符集 */
    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    
    /** 图形验证码宽度 */
    private static final int IMAGE_WIDTH = 120;
    /** 图形验证码高度 */
    private static final int IMAGE_HEIGHT = 40;
    /** 验证码长度 */
    private static final int CODE_LENGTH = 4;

    /** 滑动验证码背景宽度 */
    private static final int SLIDER_BG_WIDTH = 300;
    /** 滑动验证码背景高度 */
    private static final int SLIDER_BG_HEIGHT = 150;
    /** 滑块大小 */
    private static final int SLIDER_SIZE = 40;
    /** 滑动验证容差（像素） */
    private static final int SLIDER_TOLERANCE = 5;

    @Value("${security.captcha.expire-seconds:300}")
    private int expireSeconds;

    @Value("${security.captcha.required-fail-count:3}")
    private int requiredFailCount;

    @Value("${security.captcha.fail-count-expire-seconds:3600}")
    private int failCountExpireSeconds;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private final SecureRandom random = new SecureRandom();

    @Override
    public ImageCaptchaResult generateImageCaptcha() {
        // 生成验证码
        String code = generateCode();
        String captchaId = UUID.randomUUID().toString().replace("-", "");

        // 生成图片
        BufferedImage image = createImageCaptcha(code);
        String imageBase64 = imageToBase64(image);

        // 存储验证码
        storeCaptcha(captchaId, code);

        log.debug("生成图形验证码: captchaId={}", captchaId);
        return new ImageCaptchaResult(captchaId, imageBase64, expireSeconds);
    }

    @Override
    public SliderCaptchaResult generateSliderCaptcha() {
        String captchaId = UUID.randomUUID().toString().replace("-", "");

        // 生成随机滑块位置
        int sliderX = random.nextInt(SLIDER_BG_WIDTH - SLIDER_SIZE * 2) + SLIDER_SIZE;
        int sliderY = random.nextInt(SLIDER_BG_HEIGHT - SLIDER_SIZE * 2) + SLIDER_SIZE;

        // 生成背景图和滑块图
        BufferedImage[] images = createSliderCaptcha(sliderX, sliderY);
        String backgroundBase64 = imageToBase64(images[0]);
        String sliderBase64 = imageToBase64(images[1]);

        // 存储滑块X坐标
        storeSliderPosition(captchaId, sliderX);

        log.debug("生成滑动验证码: captchaId={}, sliderX={}", captchaId, sliderX);
        return new SliderCaptchaResult(captchaId, backgroundBase64, sliderBase64, sliderY, expireSeconds);
    }

    @Override
    public VerifyResult verifyImageCaptcha(String captchaId, String code) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(code)) {
            return VerifyResult.invalid();
        }

        String storedCode = getCaptcha(captchaId);
        if (storedCode == null) {
            return VerifyResult.notFound();
        }

        // 验证后删除验证码（一次性使用）
        deleteCaptcha(captchaId);

        if (storedCode.equalsIgnoreCase(code.trim())) {
            log.debug("图形验证码验证成功: captchaId={}", captchaId);
            return VerifyResult.success();
        } else {
            log.debug("图形验证码验证失败: captchaId={}", captchaId);
            return VerifyResult.invalid();
        }
    }

    @Override
    public VerifyResult verifySliderCaptcha(String captchaId, int sliderX) {
        if (!StringUtils.hasText(captchaId)) {
            return VerifyResult.invalid();
        }

        Integer storedX = getSliderPosition(captchaId);
        if (storedX == null) {
            return VerifyResult.notFound();
        }

        // 验证后删除
        deleteSliderPosition(captchaId);

        // 允许一定的误差
        if (Math.abs(storedX - sliderX) <= SLIDER_TOLERANCE) {
            log.debug("滑动验证码验证成功: captchaId={}", captchaId);
            return VerifyResult.success();
        } else {
            log.debug("滑动验证码验证失败: captchaId={}, expected={}, actual={}", captchaId, storedX, sliderX);
            return VerifyResult.invalid();
        }
    }

    @Override
    public boolean isRequired(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        return getLoginFailureCount(username) >= requiredFailCount;
    }

    @Override
    public void recordLoginFailure(String username) {
        if (!StringUtils.hasText(username) || redisTemplate == null) {
            return;
        }

        String key = LOGIN_FAIL_KEY_PREFIX + username;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, failCountExpireSeconds, TimeUnit.SECONDS);
            }
            log.debug("记录登录失败: username={}, count={}", username, count);
        } catch (Exception e) {
            log.error("记录登录失败异常: username={}", username, e);
        }
    }

    @Override
    public void clearLoginFailure(String username) {
        if (!StringUtils.hasText(username) || redisTemplate == null) {
            return;
        }

        String key = LOGIN_FAIL_KEY_PREFIX + username;
        try {
            redisTemplate.delete(key);
            log.debug("清除登录失败记录: username={}", username);
        } catch (Exception e) {
            log.error("清除登录失败记录异常: username={}", username, e);
        }
    }

    @Override
    public int getLoginFailureCount(String username) {
        if (!StringUtils.hasText(username) || redisTemplate == null) {
            return 0;
        }

        String key = LOGIN_FAIL_KEY_PREFIX + username;
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Integer.parseInt(value) : 0;
        } catch (Exception e) {
            log.error("获取登录失败次数异常: username={}", username, e);
            return 0;
        }
    }

    // ==================== 私有方法 ====================

    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CAPTCHA_CHARS.charAt(random.nextInt(CAPTCHA_CHARS.length())));
        }
        return code.toString();
    }

    private BufferedImage createImageCaptcha(String code) {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 设置抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 填充背景
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        // 绘制干扰线
        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            int x1 = random.nextInt(IMAGE_WIDTH);
            int y1 = random.nextInt(IMAGE_HEIGHT);
            int x2 = random.nextInt(IMAGE_WIDTH);
            int y2 = random.nextInt(IMAGE_HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        // 绘制干扰点
        for (int i = 0; i < 50; i++) {
            g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            g.fillOval(random.nextInt(IMAGE_WIDTH), random.nextInt(IMAGE_HEIGHT), 2, 2);
        }

        // 绘制验证码
        g.setFont(new Font("Arial", Font.BOLD, 28));
        int x = 10;
        for (char c : code.toCharArray()) {
            g.setColor(new Color(random.nextInt(100), random.nextInt(100), random.nextInt(100)));
            // 随机旋转
            double theta = (random.nextDouble() - 0.5) * 0.3;
            g.rotate(theta, x + 10, IMAGE_HEIGHT / 2);
            g.drawString(String.valueOf(c), x, 30);
            g.rotate(-theta, x + 10, IMAGE_HEIGHT / 2);
            x += 25;
        }

        g.dispose();
        return image;
    }

    private BufferedImage[] createSliderCaptcha(int sliderX, int sliderY) {
        // 创建背景图
        BufferedImage background = new BufferedImage(SLIDER_BG_WIDTH, SLIDER_BG_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D bgG = background.createGraphics();
        bgG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 绘制渐变背景
        GradientPaint gradient = new GradientPaint(0, 0, new Color(100, 150, 200), 
                SLIDER_BG_WIDTH, SLIDER_BG_HEIGHT, new Color(50, 100, 150));
        bgG.setPaint(gradient);
        bgG.fillRect(0, 0, SLIDER_BG_WIDTH, SLIDER_BG_HEIGHT);

        // 绘制一些随机图形作为背景纹理
        for (int i = 0; i < 20; i++) {
            bgG.setColor(new Color(random.nextInt(50) + 50, random.nextInt(50) + 100, random.nextInt(50) + 150, 100));
            int size = random.nextInt(30) + 10;
            bgG.fillOval(random.nextInt(SLIDER_BG_WIDTH), random.nextInt(SLIDER_BG_HEIGHT), size, size);
        }

        // 在滑块位置绘制缺口
        bgG.setColor(new Color(50, 50, 50, 150));
        bgG.fillRoundRect(sliderX, sliderY, SLIDER_SIZE, SLIDER_SIZE, 5, 5);
        bgG.dispose();

        // 创建滑块图
        BufferedImage slider = new BufferedImage(SLIDER_SIZE, SLIDER_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sliderG = slider.createGraphics();
        sliderG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 从背景图复制滑块区域
        sliderG.drawImage(background.getSubimage(sliderX, sliderY, SLIDER_SIZE, SLIDER_SIZE), 0, 0, null);

        // 添加边框
        sliderG.setColor(Color.WHITE);
        sliderG.setStroke(new BasicStroke(2));
        sliderG.drawRoundRect(1, 1, SLIDER_SIZE - 2, SLIDER_SIZE - 2, 5, 5);
        sliderG.dispose();

        return new BufferedImage[]{background, slider};
    }

    private String imageToBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("图片转Base64失败", e);
            return null;
        }
    }

    private void storeCaptcha(String captchaId, String code) {
        if (redisTemplate != null) {
            try {
                String key = CAPTCHA_KEY_PREFIX + captchaId;
                redisTemplate.opsForValue().set(key, code, expireSeconds, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("存储验证码失败: captchaId={}", captchaId, e);
            }
        }
    }

    private String getCaptcha(String captchaId) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            String key = CAPTCHA_KEY_PREFIX + captchaId;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("获取验证码失败: captchaId={}", captchaId, e);
            return null;
        }
    }

    private void deleteCaptcha(String captchaId) {
        if (redisTemplate != null) {
            try {
                String key = CAPTCHA_KEY_PREFIX + captchaId;
                redisTemplate.delete(key);
            } catch (Exception e) {
                log.error("删除验证码失败: captchaId={}", captchaId, e);
            }
        }
    }

    private void storeSliderPosition(String captchaId, int sliderX) {
        if (redisTemplate != null) {
            try {
                String key = SLIDER_KEY_PREFIX + captchaId;
                redisTemplate.opsForValue().set(key, String.valueOf(sliderX), expireSeconds, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("存储滑块位置失败: captchaId={}", captchaId, e);
            }
        }
    }

    private Integer getSliderPosition(String captchaId) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            String key = SLIDER_KEY_PREFIX + captchaId;
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Integer.parseInt(value) : null;
        } catch (Exception e) {
            log.error("获取滑块位置失败: captchaId={}", captchaId, e);
            return null;
        }
    }

    private void deleteSliderPosition(String captchaId) {
        if (redisTemplate != null) {
            try {
                String key = SLIDER_KEY_PREFIX + captchaId;
                redisTemplate.delete(key);
            } catch (Exception e) {
                log.error("删除滑块位置失败: captchaId={}", captchaId, e);
            }
        }
    }
}
