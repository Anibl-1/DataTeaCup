package com.dataplatform.common.config;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Pattern;

/**
 * 日志脱敏转换器
 */
public class LogMaskingConverter extends ClassicConverter {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(1[3-9]\\d)\\d{4}(\\d{4})");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{3})\\d{11}(\\d{4})");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(\\w{1,3})\\w+(@\\w+\\.\\w+)");
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("(\\d{4})\\d{8,12}(\\d{4})");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(password|passwd|pwd|secret|token)[\"':\\s=]+(\\S+)", Pattern.CASE_INSENSITIVE);

    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        if (message == null) return "";
        return maskSensitiveData(message);
    }

    public static String maskSensitiveData(String text) {
        if (text == null || text.isEmpty()) return text;
        String result = text;
        result = PHONE_PATTERN.matcher(result).replaceAll("$1****$2");
        result = ID_CARD_PATTERN.matcher(result).replaceAll("$1***********$2");
        result = EMAIL_PATTERN.matcher(result).replaceAll("$1***$2");
        result = BANK_CARD_PATTERN.matcher(result).replaceAll("$1********$2");
        result = PASSWORD_PATTERN.matcher(result).replaceAll("$1:******");
        return result;
    }
}
