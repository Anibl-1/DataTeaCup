package com.dataplatform.data.service.health;

/**
 * 健康检查器接口
 */
public interface HealthChecker {

    /**
     * 执行健康检查
     * @return 组件健康状态
     */
    ComponentHealth check();

    /**
     * 获取检查器名称
     * @return 名称
     */
    String getName();

    /**
     * 获取检查间隔（秒）
     * @return 间隔秒数
     */
    default int getCheckInterval() {
        return 30;
    }

    /**
     * 是否为关键组件
     * @return 是否关键
     */
    default boolean isCritical() {
        return true;
    }
}
