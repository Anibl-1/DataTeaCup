package com.dataplatform.common.service;

/**
 * 系统配置提供者接口
 * 用于跨模块获取系统配置值，避免 dp-data 直接依赖 dp-system
 */
public interface SystemConfigProvider {

    /**
     * 根据配置键获取配置值
     *
     * @param key 配置键
     * @return 配置值，不存在时返回 null
     */
    String getValueByKey(String key);

    /**
     * 根据配置键获取配置值，不存在时返回默认值
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    default String getValueByKey(String key, String defaultValue) {
        String value = getValueByKey(key);
        return value != null ? value : defaultValue;
    }
}
