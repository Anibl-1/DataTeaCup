package com.dataplatform.data.service.masking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 敏感字段检测器实现类
 * 
 * 实现以下检测方式：
 * 1. 正则表达式匹配：手机号、身份证、银行卡、邮箱、IP地址
 * 2. 关键词匹配：姓名、地址
 * 3. 字段名推断：基于字段名称推断敏感类型
 * 4. 采样分析：分析样本数据特征推断类型
 * 
 * @author dataplatform
 */
@Slf4j
@Service
public class SensitiveFieldDetectorImpl implements SensitiveFieldDetector {
    
    /**
     * 预编译的正则表达式模式缓存
     */
    private final Map<SensitiveFieldType, Pattern> patternCache;
    
    /**
     * 字段名到敏感类型的映射规则
     * 支持的字段名模式：
     * - phone, mobile, tel -> PHONE
     * - idcard, id_card, identity -> ID_CARD
     * - bankcard, bank_card, card_no -> BANK_CARD
     * - email, mail -> EMAIL
     * - ip, ip_address -> IP_ADDRESS
     * - name, username, realname -> NAME
     * - address, addr -> ADDRESS
     */
    private final Map<Pattern, SensitiveFieldType> fieldNamePatterns;
    
    /**
     * 地址关键词列表
     */
    private static final List<String> ADDRESS_KEYWORDS = Arrays.asList(
            "省", "市", "区", "县", "街道", "路", "镇", "乡", "村", "号", "栋", "单元", "室",
            "大道", "广场", "小区", "花园", "大厦", "中心", "工业园"
    );
    
    /**
     * 中文姓名常见姓氏（用于辅助判断）
     */
    private static final Set<String> COMMON_SURNAMES = new HashSet<>(Arrays.asList(
            "王", "李", "张", "刘", "陈", "杨", "黄", "赵", "周", "吴",
            "徐", "孙", "马", "朱", "胡", "郭", "何", "高", "林", "罗",
            "郑", "梁", "谢", "宋", "唐", "许", "韩", "冯", "邓", "曹"
    ));
    
    /**
     * 采样分析的最小匹配率阈值
     */
    private static final double MIN_MATCH_RATE = 0.5;
    
    /**
     * 高置信度阈值
     */
    private static final double HIGH_CONFIDENCE = 0.8;
    
    public SensitiveFieldDetectorImpl() {
        this.patternCache = initPatternCache();
        this.fieldNamePatterns = initFieldNamePatterns();
    }

    /**
     * 初始化正则表达式模式缓存
     */
    private Map<SensitiveFieldType, Pattern> initPatternCache() {
        Map<SensitiveFieldType, Pattern> cache = new EnumMap<>(SensitiveFieldType.class);
        for (SensitiveFieldType type : SensitiveFieldType.values()) {
            if (type.hasPattern()) {
                try {
                    cache.put(type, Pattern.compile(type.getPattern()));
                } catch (Exception e) {
                    log.warn("Failed to compile pattern for type {}: {}", type, e.getMessage());
                }
            }
        }
        return cache;
    }
    
    /**
     * 初始化字段名匹配模式
     */
    private Map<Pattern, SensitiveFieldType> initFieldNamePatterns() {
        Map<Pattern, SensitiveFieldType> patterns = new LinkedHashMap<>();
        
        // 手机号字段名模式
        patterns.put(Pattern.compile("(?i).*(phone|mobile|tel|telephone|cellphone|手机).*"), 
                SensitiveFieldType.PHONE);
        
        // 身份证字段名模式
        patterns.put(Pattern.compile("(?i).*(idcard|id_card|identity|idno|id_no|身份证|证件号).*"), 
                SensitiveFieldType.ID_CARD);
        
        // 银行卡字段名模式
        patterns.put(Pattern.compile("(?i).*(bankcard|bank_card|cardno|card_no|银行卡|卡号).*"), 
                SensitiveFieldType.BANK_CARD);
        
        // 邮箱字段名模式
        patterns.put(Pattern.compile("(?i).*(email|mail|邮箱|电子邮件).*"), 
                SensitiveFieldType.EMAIL);
        
        // IP地址字段名模式
        patterns.put(Pattern.compile("(?i).*(ip|ip_address|ipaddr|ip_addr|客户端ip).*"), 
                SensitiveFieldType.IP_ADDRESS);
        
        // 姓名字段名模式
        patterns.put(Pattern.compile("(?i).*(name|username|realname|real_name|user_name|姓名|用户名|真实姓名).*"), 
                SensitiveFieldType.NAME);
        
        // 地址字段名模式
        patterns.put(Pattern.compile("(?i).*(address|addr|地址|住址|通讯地址).*"), 
                SensitiveFieldType.ADDRESS);
        
        return patterns;
    }
    
    @Override
    public Map<String, SensitiveFieldType> detectSensitiveFields(String tableName, List<String> columns) {
        if (columns == null || columns.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<String, SensitiveFieldType> result = new HashMap<>();
        for (String column : columns) {
            SensitiveFieldType type = inferTypeByFieldName(column);
            if (type != null) {
                result.put(column, type);
            }
        }
        
        log.debug("Detected {} sensitive fields in table {}", result.size(), tableName);
        return result;
    }
    
    @Override
    public SensitiveFieldType detectFieldType(String fieldName, List<Object> sampleValues) {
        // 1. 首先尝试通过字段名推断
        SensitiveFieldType inferredType = inferTypeByFieldName(fieldName);
        
        // 2. 如果有样本值，通过值内容进行验证或检测
        if (sampleValues != null && !sampleValues.isEmpty()) {
            SensitiveFieldType detectedType = detectTypeBySampling(sampleValues);
            
            // 如果字段名推断和采样分析结果一致，返回该类型
            if (inferredType != null && inferredType == detectedType) {
                return inferredType;
            }
            
            // 如果采样分析有结果，优先使用采样分析结果
            if (detectedType != null) {
                return detectedType;
            }
        }
        
        // 3. 返回字段名推断结果
        return inferredType;
    }
    
    @Override
    public SensitiveFieldType inferTypeByFieldName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        
        String normalizedName = fieldName.toLowerCase().trim();
        
        for (Map.Entry<Pattern, SensitiveFieldType> entry : fieldNamePatterns.entrySet()) {
            if (entry.getKey().matcher(normalizedName).matches()) {
                log.debug("Field '{}' inferred as {} by field name pattern", fieldName, entry.getValue());
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    @Override
    public SensitiveFieldType detectTypeByValue(Object value) {
        if (value == null) {
            return null;
        }
        
        String strValue = value.toString().trim();
        if (strValue.isEmpty()) {
            return null;
        }
        
        // 按优先级顺序检测各种敏感类型
        // 1. 手机号（最常见，优先检测）
        if (isPhone(strValue)) {
            return SensitiveFieldType.PHONE;
        }
        
        // 2. 邮箱
        if (isEmail(strValue)) {
            return SensitiveFieldType.EMAIL;
        }
        
        // 3. IP地址
        if (isIpAddress(strValue)) {
            return SensitiveFieldType.IP_ADDRESS;
        }
        
        // 4. 身份证号
        if (isIdCard(strValue)) {
            return SensitiveFieldType.ID_CARD;
        }
        
        // 5. 银行卡号
        if (isBankCard(strValue)) {
            return SensitiveFieldType.BANK_CARD;
        }
        
        // 6. 地址（关键词匹配）
        if (isAddress(strValue)) {
            return SensitiveFieldType.ADDRESS;
        }
        
        // 7. 中文姓名
        if (isChineseName(strValue)) {
            return SensitiveFieldType.NAME;
        }
        
        return null;
    }

    @Override
    public List<SensitiveFieldInfo> detectSensitiveFields(List<Map<String, Object>> data, List<String> fieldNames) {
        if (fieldNames == null || fieldNames.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<SensitiveFieldInfo> results = new ArrayList<>();
        
        for (String fieldName : fieldNames) {
            // 收集该字段的样本值
            List<Object> sampleValues = collectSampleValues(data, fieldName);
            
            // 检测敏感类型
            SensitiveFieldInfo info = detectFieldWithDetails(fieldName, sampleValues);
            if (info != null) {
                results.add(info);
            }
        }
        
        return results;
    }
    
    /**
     * 收集字段的样本值
     */
    private List<Object> collectSampleValues(List<Map<String, Object>> data, String fieldName) {
        if (data == null || data.isEmpty()) {
            return Collections.emptyList();
        }
        
        return data.stream()
                .limit(100) // 最多采样100条
                .map(row -> row.get(fieldName))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * 检测字段敏感类型并返回详细信息
     */
    private SensitiveFieldInfo detectFieldWithDetails(String fieldName, List<Object> sampleValues) {
        // 1. 通过字段名推断
        SensitiveFieldType inferredType = inferTypeByFieldName(fieldName);
        
        // 2. 通过采样分析检测
        SamplingResult samplingResult = analyzeSamples(sampleValues);
        
        // 3. 综合判断
        SensitiveFieldType finalType = null;
        SensitiveFieldInfo.DetectionMethod method = null;
        double confidence = 0.0;
        
        if (inferredType != null && samplingResult.type != null) {
            if (inferredType == samplingResult.type) {
                // 字段名和采样分析结果一致，高置信度
                finalType = inferredType;
                method = SensitiveFieldInfo.DetectionMethod.COMBINED;
                confidence = Math.min(1.0, samplingResult.matchRate + 0.2);
            } else {
                // 结果不一致，优先使用采样分析结果（如果匹配率足够高）
                if (samplingResult.matchRate >= HIGH_CONFIDENCE) {
                    finalType = samplingResult.type;
                    method = SensitiveFieldInfo.DetectionMethod.SAMPLE_ANALYSIS;
                    confidence = samplingResult.matchRate;
                } else {
                    finalType = inferredType;
                    method = SensitiveFieldInfo.DetectionMethod.FIELD_NAME_INFERENCE;
                    confidence = 0.6;
                }
            }
        } else if (samplingResult.type != null) {
            finalType = samplingResult.type;
            method = samplingResult.isKeywordMatch 
                    ? SensitiveFieldInfo.DetectionMethod.KEYWORD_MATCH 
                    : SensitiveFieldInfo.DetectionMethod.REGEX_MATCH;
            confidence = samplingResult.matchRate;
        } else if (inferredType != null) {
            finalType = inferredType;
            method = SensitiveFieldInfo.DetectionMethod.FIELD_NAME_INFERENCE;
            confidence = 0.6;
        }
        
        if (finalType == null) {
            return null;
        }
        
        return SensitiveFieldInfo.builder()
                .fieldName(fieldName)
                .sensitiveType(finalType)
                .confidence(confidence)
                .detectionMethod(method)
                .matchedSampleCount(samplingResult.matchedCount)
                .totalSampleCount(sampleValues.size())
                .build();
    }
    
    /**
     * 通过采样分析检测敏感类型
     */
    private SensitiveFieldType detectTypeBySampling(List<Object> sampleValues) {
        SamplingResult result = analyzeSamples(sampleValues);
        return result.type;
    }
    
    /**
     * 分析样本数据
     */
    private SamplingResult analyzeSamples(List<Object> sampleValues) {
        if (sampleValues == null || sampleValues.isEmpty()) {
            return new SamplingResult(null, 0, 0, false);
        }
        
        // 统计各类型的匹配数量
        Map<SensitiveFieldType, Integer> typeCounts = new EnumMap<>(SensitiveFieldType.class);
        int validSampleCount = 0;
        boolean hasKeywordMatch = false;
        
        for (Object value : sampleValues) {
            if (value == null) {
                continue;
            }
            
            String strValue = value.toString().trim();
            if (strValue.isEmpty()) {
                continue;
            }
            
            validSampleCount++;
            SensitiveFieldType type = detectTypeByValue(strValue);
            if (type != null) {
                typeCounts.merge(type, 1, Integer::sum);
                if (type == SensitiveFieldType.ADDRESS) {
                    hasKeywordMatch = true;
                }
            }
        }
        
        if (validSampleCount == 0) {
            return new SamplingResult(null, 0, 0, false);
        }
        
        // 找出匹配数量最多的类型
        SensitiveFieldType bestType = null;
        int maxCount = 0;
        
        for (Map.Entry<SensitiveFieldType, Integer> entry : typeCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                bestType = entry.getKey();
            }
        }
        
        // 计算匹配率
        double matchRate = (double) maxCount / validSampleCount;
        
        // 只有匹配率超过阈值才返回结果
        if (matchRate < MIN_MATCH_RATE) {
            return new SamplingResult(null, 0, validSampleCount, false);
        }
        
        return new SamplingResult(bestType, maxCount, validSampleCount, 
                hasKeywordMatch && bestType == SensitiveFieldType.ADDRESS);
    }

    /**
     * 采样分析结果
     */
    private static class SamplingResult {
        final SensitiveFieldType type;
        final int matchedCount;
        final int totalCount;
        final double matchRate;
        final boolean isKeywordMatch;
        
        SamplingResult(SensitiveFieldType type, int matchedCount, int totalCount, boolean isKeywordMatch) {
            this.type = type;
            this.matchedCount = matchedCount;
            this.totalCount = totalCount;
            this.matchRate = totalCount > 0 ? (double) matchedCount / totalCount : 0;
            this.isKeywordMatch = isKeywordMatch;
        }
    }
    
    // ==================== 各类型检测方法 ====================
    
    /**
     * 检测是否为手机号
     * 规则：11位数字，1开头，第二位为3-9
     */
    private boolean isPhone(String value) {
        if (value == null || value.length() != 11) {
            return false;
        }
        Pattern pattern = patternCache.get(SensitiveFieldType.PHONE);
        return pattern != null && pattern.matcher(value).matches();
    }
    
    /**
     * 检测是否为身份证号
     * 规则：15位数字 或 17位数字+1位校验码（数字或X）
     */
    private boolean isIdCard(String value) {
        if (value == null) {
            return false;
        }
        int len = value.length();
        if (len != 15 && len != 18) {
            return false;
        }
        Pattern pattern = patternCache.get(SensitiveFieldType.ID_CARD);
        if (pattern == null || !pattern.matcher(value).matches()) {
            return false;
        }
        
        // 18位身份证进行校验码验证
        if (len == 18) {
            return validateIdCardChecksum(value);
        }
        
        return true;
    }
    
    /**
     * 验证18位身份证校验码
     */
    private boolean validateIdCardChecksum(String idCard) {
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        char[] checkCodes = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
        
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            char c = idCard.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
            sum += (c - '0') * weights[i];
        }
        
        char expectedCheck = checkCodes[sum % 11];
        char actualCheck = Character.toUpperCase(idCard.charAt(17));
        
        return expectedCheck == actualCheck;
    }
    
    /**
     * 检测是否为银行卡号
     * 规则：16-19位数字
     */
    private boolean isBankCard(String value) {
        if (value == null) {
            return false;
        }
        int len = value.length();
        if (len < 16 || len > 19) {
            return false;
        }
        Pattern pattern = patternCache.get(SensitiveFieldType.BANK_CARD);
        if (pattern == null || !pattern.matcher(value).matches()) {
            return false;
        }
        
        // 使用Luhn算法验证银行卡号
        return validateLuhn(value);
    }
    
    /**
     * Luhn算法验证银行卡号
     */
    private boolean validateLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            char c = cardNumber.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
            int digit = c - '0';
            
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        return sum % 10 == 0;
    }
    
    /**
     * 检测是否为邮箱地址
     */
    private boolean isEmail(String value) {
        if (value == null || !value.contains("@")) {
            return false;
        }
        Pattern pattern = patternCache.get(SensitiveFieldType.EMAIL);
        return pattern != null && pattern.matcher(value).matches();
    }
    
    /**
     * 检测是否为IP地址（IPv4）
     */
    private boolean isIpAddress(String value) {
        if (value == null) {
            return false;
        }
        
        // 检测IPv4
        Pattern ipv4Pattern = patternCache.get(SensitiveFieldType.IP_ADDRESS);
        if (ipv4Pattern != null && ipv4Pattern.matcher(value).matches()) {
            return true;
        }
        
        // 检测IPv6（简化版本）
        return isIpv6Address(value);
    }
    
    /**
     * 检测是否为IPv6地址
     */
    private boolean isIpv6Address(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        
        // 简化的IPv6检测：包含冒号且由十六进制字符组成
        if (!value.contains(":")) {
            return false;
        }
        
        String[] parts = value.split(":");
        if (parts.length < 3 || parts.length > 8) {
            return false;
        }
        
        for (String part : parts) {
            if (part.isEmpty()) {
                continue; // 允许::简写
            }
            if (part.length() > 4) {
                return false;
            }
            for (char c : part.toCharArray()) {
                if (!isHexChar(c)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    private boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
    
    /**
     * 检测是否为地址（关键词匹配）
     */
    private boolean isAddress(String value) {
        if (value == null || value.length() < 5) {
            return false;
        }
        
        // 检查是否包含地址关键词
        int keywordCount = 0;
        for (String keyword : ADDRESS_KEYWORDS) {
            if (value.contains(keyword)) {
                keywordCount++;
                if (keywordCount >= 2) {
                    return true;
                }
            }
        }
        
        // 单个关键词但长度较长也可能是地址
        return keywordCount >= 1 && value.length() >= 10;
    }
    
    /**
     * 检测是否为中文姓名
     * 规则：2-4个汉字，可能以常见姓氏开头
     */
    private boolean isChineseName(String value) {
        if (value == null) {
            return false;
        }
        
        int len = value.length();
        if (len < 2 || len > 4) {
            return false;
        }
        
        // 检查是否全为汉字
        for (char c : value.toCharArray()) {
            if (!isChineseChar(c)) {
                return false;
            }
        }
        
        // 如果以常见姓氏开头，增加置信度
        String firstChar = value.substring(0, 1);
        if (COMMON_SURNAMES.contains(firstChar)) {
            return true;
        }
        
        // 2-4个汉字也可能是姓名
        return true;
    }
    
    /**
     * 判断是否为汉字
     */
    private boolean isChineseChar(char c) {
        return c >= '\u4e00' && c <= '\u9fa5';
    }
}
