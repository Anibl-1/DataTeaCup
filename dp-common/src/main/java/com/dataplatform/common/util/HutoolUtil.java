package com.dataplatform.common.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.http.HttpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Hutool工具类封装
 * 
 * @author dataplatform
 */
@Slf4j
public class HutoolUtil {

    private HutoolUtil() {}

    // ==================== ID生成 ====================
    public static String uuid() { return IdUtil.simpleUUID(); }
    public static String uuidWithDash() { return IdUtil.randomUUID(); }
    public static long snowflakeId() { return IdUtil.getSnowflakeNextId(); }
    public static String snowflakeIdStr() { return IdUtil.getSnowflakeNextIdStr(); }

    // ==================== 字符串处理 ====================
    public static boolean isBlank(String str) { return StrUtil.isBlank(str); }
    public static boolean isNotBlank(String str) { return StrUtil.isNotBlank(str); }
    public static String format(String template, Object... params) { return StrUtil.format(template, params); }
    public static String toUnderlineCase(String str) { return StrUtil.toUnderlineCase(str); }
    public static String toCamelCase(String str) { return StrUtil.toCamelCase(str); }
    public static String abbreviate(String str, int maxLength) { return StrUtil.maxLength(str, maxLength); }

    // ==================== 日期处理 ====================
    public static String formatDate(Date date, String pattern) { return DateUtil.format(date, pattern); }
    public static String formatNow(String pattern) { return DateUtil.format(new Date(), pattern); }
    public static Date parseDate(String dateStr) { return DateUtil.parse(dateStr); }
    public static long currentTimestamp() { return DateUtil.current(); }
    public static long betweenDays(Date beginDate, Date endDate) { return DateUtil.betweenDay(beginDate, endDate, true); }

    // ==================== 加密解密 ====================
    public static String md5(String str) { return SecureUtil.md5(str); }
    public static String sha256(String str) { return SecureUtil.sha256(str); }
    public static String aesEncrypt(String content, String key) { AES aes = SecureUtil.aes(key.getBytes()); return aes.encryptHex(content); }
    public static String aesDecrypt(String encryptedHex, String key) { AES aes = SecureUtil.aes(key.getBytes()); return aes.decryptStr(encryptedHex); }

    // ==================== 集合处理 ====================
    public static boolean isEmpty(java.util.Collection<?> collection) { return CollUtil.isEmpty(collection); }
    public static boolean isNotEmpty(java.util.Collection<?> collection) { return CollUtil.isNotEmpty(collection); }
    public static <T> List<T> page(int pageNo, int pageSize, List<T> list) { return CollUtil.page(pageNo, pageSize, list); }

    // ==================== Bean处理 ====================
    public static Map<String, Object> beanToMap(Object bean) { return BeanUtil.beanToMap(bean); }
    public static <T> T mapToBean(Map<?, ?> map, Class<T> clazz) { return BeanUtil.mapToBean(map, clazz, true, null); }
    public static void copyProperties(Object source, Object target) { BeanUtil.copyProperties(source, target); }

    // ==================== 随机数 ====================
    public static String randomString(int length) { return RandomUtil.randomString(length); }
    public static int randomInt(int min, int max) { return RandomUtil.randomInt(min, max); }

    // ==================== 文件处理 ====================
    public static String readFileToString(String filePath) { return FileUtil.readUtf8String(filePath); }
    public static void writeStringToFile(String filePath, String content) { FileUtil.writeUtf8String(content, filePath); }
    public static String getFileExtension(String fileName) { return FileUtil.extName(fileName); }
    public static String getReadableFileSize(long size) { return FileUtil.readableFileSize(size); }

    // ==================== HTTP请求 ====================
    public static String httpGet(String url) { return HttpUtil.get(url); }
    public static String httpPostJson(String url, String jsonBody) { return HttpUtil.post(url, jsonBody); }
    public static String httpPostForm(String url, Map<String, Object> params) { return HttpUtil.post(url, params); }
}
