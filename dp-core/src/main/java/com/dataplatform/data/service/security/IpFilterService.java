package com.dataplatform.data.service.security;

import java.util.List;
import java.util.Set;

/**
 * IP过滤服务接口
 * 实现IP黑白名单功能，支持IP访问控制
 * 
 * 需求 3.8: THE Security_Engine SHALL 支持IP黑白名单配置，限制特定IP的访问
 *
 * @author dataplatform
 */
public interface IpFilterService {

    /**
     * 检查IP是否允许访问
     * 根据黑白名单规则判断IP是否可以访问系统
     * 
     * 规则优先级：
     * 1. 如果IP在黑名单中，拒绝访问
     * 2. 如果启用了白名单模式且IP不在白名单中，拒绝访问
     * 3. 其他情况允许访问
     *
     * @param ip 客户端IP地址
     * @return 访问检查结果
     */
    IpCheckResult checkAccess(String ip);

    /**
     * 检查IP是否在黑名单中
     *
     * @param ip 客户端IP地址
     * @return true 表示IP在黑名单中，false 表示不在
     */
    boolean isBlacklisted(String ip);

    /**
     * 检查IP是否在白名单中
     *
     * @param ip 客户端IP地址
     * @return true 表示IP在白名单中，false 表示不在
     */
    boolean isWhitelisted(String ip);

    /**
     * 添加IP到黑名单
     *
     * @param ip     IP地址或CIDR格式的IP段（如：192.168.1.0/24）
     * @param reason 加入黑名单的原因
     * @return true 表示添加成功，false 表示添加失败
     */
    boolean addToBlacklist(String ip, String reason);

    /**
     * 批量添加IP到黑名单
     *
     * @param ips    IP地址列表
     * @param reason 加入黑名单的原因
     * @return 成功添加的数量
     */
    int addToBlacklist(List<String> ips, String reason);

    /**
     * 从黑名单移除IP
     *
     * @param ip IP地址或CIDR格式的IP段
     * @return true 表示移除成功，false 表示移除失败
     */
    boolean removeFromBlacklist(String ip);

    /**
     * 批量从黑名单移除IP
     *
     * @param ips IP地址列表
     * @return 成功移除的数量
     */
    int removeFromBlacklist(List<String> ips);

    /**
     * 添加IP到白名单
     *
     * @param ip     IP地址或CIDR格式的IP段（如：192.168.1.0/24）
     * @param reason 加入白名单的原因
     * @return true 表示添加成功，false 表示添加失败
     */
    boolean addToWhitelist(String ip, String reason);

    /**
     * 批量添加IP到白名单
     *
     * @param ips    IP地址列表
     * @param reason 加入白名单的原因
     * @return 成功添加的数量
     */
    int addToWhitelist(List<String> ips, String reason);

    /**
     * 从白名单移除IP
     *
     * @param ip IP地址或CIDR格式的IP段
     * @return true 表示移除成功，false 表示移除失败
     */
    boolean removeFromWhitelist(String ip);

    /**
     * 批量从白名单移除IP
     *
     * @param ips IP地址列表
     * @return 成功移除的数量
     */
    int removeFromWhitelist(List<String> ips);

    /**
     * 获取所有黑名单IP
     *
     * @return 黑名单IP集合
     */
    Set<IpRule> getBlacklist();

    /**
     * 获取所有白名单IP
     *
     * @return 白名单IP集合
     */
    Set<IpRule> getWhitelist();

    /**
     * 清空黑名单
     */
    void clearBlacklist();

    /**
     * 清空白名单
     */
    void clearWhitelist();

    /**
     * 启用白名单模式
     * 启用后，只有白名单中的IP才能访问
     */
    void enableWhitelistMode();

    /**
     * 禁用白名单模式
     * 禁用后，只检查黑名单
     */
    void disableWhitelistMode();

    /**
     * 检查是否启用了白名单模式
     *
     * @return true 表示启用了白名单模式
     */
    boolean isWhitelistModeEnabled();

    /**
     * 刷新IP规则缓存
     * 从持久化存储重新加载IP规则
     */
    void refreshRules();

    /**
     * 验证IP地址或CIDR格式是否有效
     *
     * @param ipOrCidr IP地址或CIDR格式字符串
     * @return true 表示格式有效
     */
    boolean isValidIpOrCidr(String ipOrCidr);

    /**
     * IP检查结果
     */
    record IpCheckResult(
            boolean isAllowed,
            IpCheckStatus status,
            String message,
            String matchedRule
    ) {
        /**
         * 创建允许访问的结果
         */
        public static IpCheckResult allowed() {
            return new IpCheckResult(true, IpCheckStatus.ALLOWED, "IP允许访问", null);
        }

        /**
         * 创建允许访问的结果（白名单匹配）
         */
        public static IpCheckResult allowedByWhitelist(String matchedRule) {
            return new IpCheckResult(true, IpCheckStatus.WHITELISTED, "IP在白名单中", matchedRule);
        }

        /**
         * 创建拒绝访问的结果（黑名单匹配）
         */
        public static IpCheckResult deniedByBlacklist(String matchedRule) {
            return new IpCheckResult(false, IpCheckStatus.BLACKLISTED, "IP在黑名单中，禁止访问", matchedRule);
        }

        /**
         * 创建拒绝访问的结果（不在白名单中）
         */
        public static IpCheckResult deniedNotInWhitelist() {
            return new IpCheckResult(false, IpCheckStatus.NOT_IN_WHITELIST, "IP不在白名单中，禁止访问", null);
        }

        /**
         * 创建无效IP的结果
         */
        public static IpCheckResult invalidIp(String ip) {
            return new IpCheckResult(false, IpCheckStatus.INVALID_IP, "无效的IP地址: " + ip, null);
        }
    }

    /**
     * IP检查状态
     */
    enum IpCheckStatus {
        /** 允许访问 */
        ALLOWED,
        /** 在白名单中 */
        WHITELISTED,
        /** 在黑名单中 */
        BLACKLISTED,
        /** 不在白名单中（白名单模式下） */
        NOT_IN_WHITELIST,
        /** 无效的IP地址 */
        INVALID_IP
    }

    /**
     * IP规则
     */
    record IpRule(
            String ip,
            IpRuleType type,
            String reason,
            long createTime
    ) {
        /**
         * 创建黑名单规则
         */
        public static IpRule blacklist(String ip, String reason) {
            return new IpRule(ip, IpRuleType.BLACKLIST, reason, System.currentTimeMillis());
        }

        /**
         * 创建白名单规则
         */
        public static IpRule whitelist(String ip, String reason) {
            return new IpRule(ip, IpRuleType.WHITELIST, reason, System.currentTimeMillis());
        }
    }

    /**
     * IP规则类型
     */
    enum IpRuleType {
        /** 黑名单 */
        BLACKLIST,
        /** 白名单 */
        WHITELIST
    }
}
