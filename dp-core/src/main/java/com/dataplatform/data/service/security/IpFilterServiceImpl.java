package com.dataplatform.data.service.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * IP过滤服务实现类
 * 实现IP黑白名单功能，支持IP访问控制和CIDR格式的IP段配置
 * 使用Redis缓存IP规则以提高性能
 * 
 * 需求 3.8: THE Security_Engine SHALL 支持IP黑白名单配置，限制特定IP的访问
 *
 * @author dataplatform
 */
@Slf4j
@Service
public class IpFilterServiceImpl implements IpFilterService {

    private static final String BLACKLIST_KEY = "security:ip:blacklist";
    private static final String WHITELIST_KEY = "security:ip:whitelist";
    private static final String WHITELIST_MODE_KEY = "security:ip:whitelist_mode";
    private static final String RULE_SEPARATOR = "::";

    /**
     * IPv4地址正则表达式
     */
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    /**
     * IPv4 CIDR格式正则表达式
     */
    private static final Pattern IPV4_CIDR_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)/(3[0-2]|[12]?[0-9])$"
    );

    /**
     * IPv6地址正则表达式（简化版）
     */
    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^::$|^::1$|^([0-9a-fA-F]{1,4}:)*:([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4}$"
    );

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /**
     * 本地黑名单缓存（当Redis不可用时使用）
     */
    private final Map<String, IpRule> localBlacklist = new ConcurrentHashMap<>();

    /**
     * 本地白名单缓存（当Redis不可用时使用）
     */
    private final Map<String, IpRule> localWhitelist = new ConcurrentHashMap<>();

    /**
     * 白名单模式标志
     */
    private volatile boolean whitelistModeEnabled = false;

    /**
     * 是否使用Redis
     */
    @Value("${security.ip-filter.use-redis:true}")
    private boolean useRedis;

    @PostConstruct
    public void init() {
        log.info("初始化IP过滤服务");
        try {
            refreshRules();
        } catch (Exception e) {
            log.warn("初始化IP规则失败（Redis可能未启动），将使用本地空规则: {}", e.getMessage());
        }
    }

    @Override
    public IpCheckResult checkAccess(String ip) {
        if (!StringUtils.hasText(ip)) {
            log.warn("IP检查失败: IP地址为空");
            return IpCheckResult.invalidIp("空");
        }

        // 规范化IP地址
        String normalizedIp = normalizeIp(ip);
        if (normalizedIp == null) {
            log.warn("IP检查失败: 无效的IP地址 {}", ip);
            return IpCheckResult.invalidIp(ip);
        }

        // 1. 检查黑名单（优先级最高）
        String blacklistMatch = findMatchingRule(normalizedIp, getBlacklistRules());
        if (blacklistMatch != null) {
            log.warn("IP被黑名单拦截: ip={}, matchedRule={}", normalizedIp, blacklistMatch);
            return IpCheckResult.deniedByBlacklist(blacklistMatch);
        }

        // 2. 检查白名单模式
        if (isWhitelistModeEnabled()) {
            String whitelistMatch = findMatchingRule(normalizedIp, getWhitelistRules());
            if (whitelistMatch != null) {
                log.debug("IP通过白名单验证: ip={}, matchedRule={}", normalizedIp, whitelistMatch);
                return IpCheckResult.allowedByWhitelist(whitelistMatch);
            } else {
                log.warn("IP不在白名单中被拦截: ip={}", normalizedIp);
                return IpCheckResult.deniedNotInWhitelist();
            }
        }

        // 3. 默认允许访问
        log.debug("IP允许访问: ip={}", normalizedIp);
        return IpCheckResult.allowed();
    }

    @Override
    public boolean isBlacklisted(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        String normalizedIp = normalizeIp(ip);
        if (normalizedIp == null) {
            return false;
        }
        return findMatchingRule(normalizedIp, getBlacklistRules()) != null;
    }

    @Override
    public boolean isWhitelisted(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        String normalizedIp = normalizeIp(ip);
        if (normalizedIp == null) {
            return false;
        }
        return findMatchingRule(normalizedIp, getWhitelistRules()) != null;
    }

    @Override
    public boolean addToBlacklist(String ip, String reason) {
        if (!isValidIpOrCidr(ip)) {
            log.warn("添加黑名单失败: 无效的IP格式 {}", ip);
            return false;
        }

        String normalizedIp = normalizeIpOrCidr(ip);
        IpRule rule = IpRule.blacklist(normalizedIp, reason);

        if (useRedis && redisTemplate != null) {
            try {
                String ruleValue = serializeRule(rule);
                redisTemplate.opsForSet().add(BLACKLIST_KEY, ruleValue);
                log.info("添加IP到黑名单(Redis): ip={}, reason={}", normalizedIp, reason);
                return true;
            } catch (Exception e) {
                log.error("添加黑名单到Redis失败: ip={}", normalizedIp, e);
            }
        }

        // 使用本地缓存
        localBlacklist.put(normalizedIp, rule);
        log.info("添加IP到黑名单(本地): ip={}, reason={}", normalizedIp, reason);
        return true;
    }

    @Override
    public int addToBlacklist(List<String> ips, String reason) {
        if (ips == null || ips.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String ip : ips) {
            if (addToBlacklist(ip, reason)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean removeFromBlacklist(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }

        String normalizedIp = normalizeIpOrCidr(ip);

        if (useRedis && redisTemplate != null) {
            try {
                Set<String> members = redisTemplate.opsForSet().members(BLACKLIST_KEY);
                if (members != null) {
                    for (String member : members) {
                        IpRule rule = deserializeRule(member);
                        if (rule != null && rule.ip().equals(normalizedIp)) {
                            redisTemplate.opsForSet().remove(BLACKLIST_KEY, member);
                            log.info("从黑名单移除IP(Redis): ip={}", normalizedIp);
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("从Redis移除黑名单失败: ip={}", normalizedIp, e);
            }
        }

        // 从本地缓存移除
        IpRule removed = localBlacklist.remove(normalizedIp);
        if (removed != null) {
            log.info("从黑名单移除IP(本地): ip={}", normalizedIp);
            return true;
        }
        return false;
    }

    @Override
    public int removeFromBlacklist(List<String> ips) {
        if (ips == null || ips.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String ip : ips) {
            if (removeFromBlacklist(ip)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean addToWhitelist(String ip, String reason) {
        if (!isValidIpOrCidr(ip)) {
            log.warn("添加白名单失败: 无效的IP格式 {}", ip);
            return false;
        }

        String normalizedIp = normalizeIpOrCidr(ip);
        IpRule rule = IpRule.whitelist(normalizedIp, reason);

        if (useRedis && redisTemplate != null) {
            try {
                String ruleValue = serializeRule(rule);
                redisTemplate.opsForSet().add(WHITELIST_KEY, ruleValue);
                log.info("添加IP到白名单(Redis): ip={}, reason={}", normalizedIp, reason);
                return true;
            } catch (Exception e) {
                log.error("添加白名单到Redis失败: ip={}", normalizedIp, e);
            }
        }

        // 使用本地缓存
        localWhitelist.put(normalizedIp, rule);
        log.info("添加IP到白名单(本地): ip={}, reason={}", normalizedIp, reason);
        return true;
    }

    @Override
    public int addToWhitelist(List<String> ips, String reason) {
        if (ips == null || ips.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String ip : ips) {
            if (addToWhitelist(ip, reason)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean removeFromWhitelist(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }

        String normalizedIp = normalizeIpOrCidr(ip);

        if (useRedis && redisTemplate != null) {
            try {
                Set<String> members = redisTemplate.opsForSet().members(WHITELIST_KEY);
                if (members != null) {
                    for (String member : members) {
                        IpRule rule = deserializeRule(member);
                        if (rule != null && rule.ip().equals(normalizedIp)) {
                            redisTemplate.opsForSet().remove(WHITELIST_KEY, member);
                            log.info("从白名单移除IP(Redis): ip={}", normalizedIp);
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("从Redis移除白名单失败: ip={}", normalizedIp, e);
            }
        }

        // 从本地缓存移除
        IpRule removed = localWhitelist.remove(normalizedIp);
        if (removed != null) {
            log.info("从白名单移除IP(本地): ip={}", normalizedIp);
            return true;
        }
        return false;
    }

    @Override
    public int removeFromWhitelist(List<String> ips) {
        if (ips == null || ips.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String ip : ips) {
            if (removeFromWhitelist(ip)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public Set<IpRule> getBlacklist() {
        return new HashSet<>(getBlacklistRules().values());
    }

    @Override
    public Set<IpRule> getWhitelist() {
        return new HashSet<>(getWhitelistRules().values());
    }

    @Override
    public void clearBlacklist() {
        if (useRedis && redisTemplate != null) {
            try {
                redisTemplate.delete(BLACKLIST_KEY);
                log.info("清空黑名单(Redis)");
            } catch (Exception e) {
                log.error("清空Redis黑名单失败", e);
            }
        }
        localBlacklist.clear();
        log.info("清空黑名单(本地)");
    }

    @Override
    public void clearWhitelist() {
        if (useRedis && redisTemplate != null) {
            try {
                redisTemplate.delete(WHITELIST_KEY);
                log.info("清空白名单(Redis)");
            } catch (Exception e) {
                log.error("清空Redis白名单失败", e);
            }
        }
        localWhitelist.clear();
        log.info("清空白名单(本地)");
    }

    @Override
    public void enableWhitelistMode() {
        whitelistModeEnabled = true;
        if (useRedis && redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(WHITELIST_MODE_KEY, "true");
            } catch (Exception e) {
                log.error("设置白名单模式到Redis失败", e);
            }
        }
        log.info("启用白名单模式");
    }

    @Override
    public void disableWhitelistMode() {
        whitelistModeEnabled = false;
        if (useRedis && redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(WHITELIST_MODE_KEY, "false");
            } catch (Exception e) {
                log.error("设置白名单模式到Redis失败", e);
            }
        }
        log.info("禁用白名单模式");
    }

    @Override
    public boolean isWhitelistModeEnabled() {
        if (useRedis && redisTemplate != null) {
            try {
                String value = redisTemplate.opsForValue().get(WHITELIST_MODE_KEY);
                if (value != null) {
                    return "true".equals(value);
                }
            } catch (Exception e) {
                log.error("从Redis获取白名单模式失败", e);
            }
        }
        return whitelistModeEnabled;
    }

    @Override
    public void refreshRules() {
        log.info("刷新IP规则缓存");
        if (useRedis && redisTemplate != null) {
            try {
                // 从Redis加载黑名单
                Set<String> blacklistMembers = redisTemplate.opsForSet().members(BLACKLIST_KEY);
                if (blacklistMembers != null) {
                    localBlacklist.clear();
                    for (String member : blacklistMembers) {
                        IpRule rule = deserializeRule(member);
                        if (rule != null) {
                            localBlacklist.put(rule.ip(), rule);
                        }
                    }
                }

                // 从Redis加载白名单
                Set<String> whitelistMembers = redisTemplate.opsForSet().members(WHITELIST_KEY);
                if (whitelistMembers != null) {
                    localWhitelist.clear();
                    for (String member : whitelistMembers) {
                        IpRule rule = deserializeRule(member);
                        if (rule != null) {
                            localWhitelist.put(rule.ip(), rule);
                        }
                    }
                }

                // 加载白名单模式
                String modeValue = redisTemplate.opsForValue().get(WHITELIST_MODE_KEY);
                whitelistModeEnabled = "true".equals(modeValue);

                log.info("从Redis加载IP规则完成: 黑名单={}, 白名单={}, 白名单模式={}",
                        localBlacklist.size(), localWhitelist.size(), whitelistModeEnabled);
            } catch (Exception e) {
                log.warn("从Redis加载IP规则失败（Redis可能未启动）: {}", e.getMessage());
            }
        }
    }

    @Override
    public boolean isValidIpOrCidr(String ipOrCidr) {
        if (!StringUtils.hasText(ipOrCidr)) {
            return false;
        }

        String trimmed = ipOrCidr.trim();

        // 检查IPv4
        if (IPV4_PATTERN.matcher(trimmed).matches()) {
            return true;
        }

        // 检查IPv4 CIDR
        if (IPV4_CIDR_PATTERN.matcher(trimmed).matches()) {
            return true;
        }

        // 检查IPv6（简化检查）
        if (IPV6_PATTERN.matcher(trimmed).matches()) {
            return true;
        }

        // 尝试使用InetAddress解析
        try {
            // 处理CIDR格式
            String ipPart = trimmed.contains("/") ? trimmed.split("/")[0] : trimmed;
            InetAddress.getByName(ipPart);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 规范化IP地址
     */
    private String normalizeIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return null;
        }

        String trimmed = ip.trim();

        // 处理IPv6映射的IPv4地址
        if (trimmed.startsWith("::ffff:")) {
            trimmed = trimmed.substring(7);
        }

        // 处理本地回环地址
        if ("0:0:0:0:0:0:0:1".equals(trimmed) || "::1".equals(trimmed)) {
            return "127.0.0.1";
        }

        try {
            InetAddress addr = InetAddress.getByName(trimmed);
            return addr.getHostAddress();
        } catch (Exception e) {
            log.debug("IP地址解析失败: {}", ip);
            return null;
        }
    }

    /**
     * 规范化IP或CIDR
     */
    private String normalizeIpOrCidr(String ipOrCidr) {
        if (!StringUtils.hasText(ipOrCidr)) {
            return ipOrCidr;
        }

        String trimmed = ipOrCidr.trim();

        // 如果是CIDR格式，保持原样
        if (trimmed.contains("/")) {
            return trimmed;
        }

        // 否则规范化IP
        String normalized = normalizeIp(trimmed);
        return normalized != null ? normalized : trimmed;
    }

    /**
     * 获取黑名单规则
     */
    private Map<String, IpRule> getBlacklistRules() {
        if (useRedis && redisTemplate != null) {
            try {
                Set<String> members = redisTemplate.opsForSet().members(BLACKLIST_KEY);
                if (members != null && !members.isEmpty()) {
                    Map<String, IpRule> rules = new HashMap<>();
                    for (String member : members) {
                        IpRule rule = deserializeRule(member);
                        if (rule != null) {
                            rules.put(rule.ip(), rule);
                        }
                    }
                    return rules;
                }
            } catch (Exception e) {
                log.error("从Redis获取黑名单失败", e);
            }
        }
        return new HashMap<>(localBlacklist);
    }

    /**
     * 获取白名单规则
     */
    private Map<String, IpRule> getWhitelistRules() {
        if (useRedis && redisTemplate != null) {
            try {
                Set<String> members = redisTemplate.opsForSet().members(WHITELIST_KEY);
                if (members != null && !members.isEmpty()) {
                    Map<String, IpRule> rules = new HashMap<>();
                    for (String member : members) {
                        IpRule rule = deserializeRule(member);
                        if (rule != null) {
                            rules.put(rule.ip(), rule);
                        }
                    }
                    return rules;
                }
            } catch (Exception e) {
                log.error("从Redis获取白名单失败", e);
            }
        }
        return new HashMap<>(localWhitelist);
    }

    /**
     * 查找匹配的规则
     */
    private String findMatchingRule(String ip, Map<String, IpRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return null;
        }

        // 1. 精确匹配
        if (rules.containsKey(ip)) {
            return ip;
        }

        // 2. CIDR匹配
        for (String ruleIp : rules.keySet()) {
            if (ruleIp.contains("/") && isIpInCidr(ip, ruleIp)) {
                return ruleIp;
            }
        }

        return null;
    }

    /**
     * 检查IP是否在CIDR范围内
     */
    private boolean isIpInCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }

            String networkAddress = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);

            InetAddress ipAddr = InetAddress.getByName(ip);
            InetAddress networkAddr = InetAddress.getByName(networkAddress);

            byte[] ipBytes = ipAddr.getAddress();
            byte[] networkBytes = networkAddr.getAddress();

            // 确保地址类型相同
            if (ipBytes.length != networkBytes.length) {
                return false;
            }

            // 计算掩码
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;

            // 比较完整字节
            for (int i = 0; i < fullBytes; i++) {
                if (ipBytes[i] != networkBytes[i]) {
                    return false;
                }
            }

            // 比较剩余位
            if (remainingBits > 0 && fullBytes < ipBytes.length) {
                int mask = (0xFF << (8 - remainingBits)) & 0xFF;
                if ((ipBytes[fullBytes] & mask) != (networkBytes[fullBytes] & mask)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.debug("CIDR匹配失败: ip={}, cidr={}", ip, cidr, e);
            return false;
        }
    }

    /**
     * 序列化规则
     */
    private String serializeRule(IpRule rule) {
        return rule.ip() + RULE_SEPARATOR + 
               rule.type().name() + RULE_SEPARATOR + 
               (rule.reason() != null ? rule.reason() : "") + RULE_SEPARATOR + 
               rule.createTime();
    }

    /**
     * 反序列化规则
     */
    private IpRule deserializeRule(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            String[] parts = value.split(RULE_SEPARATOR, 4);
            if (parts.length < 4) {
                return null;
            }

            String ip = parts[0];
            IpRuleType type = IpRuleType.valueOf(parts[1]);
            String reason = parts[2].isEmpty() ? null : parts[2];
            long createTime = Long.parseLong(parts[3]);

            return new IpRule(ip, type, reason, createTime);
        } catch (Exception e) {
            log.warn("反序列化IP规则失败: {}", value, e);
            return null;
        }
    }
}
