package com.dataplatform.data.service.health;

import java.util.List;
import java.util.Map;

/**
 * 健康检查服务接口
 */
public interface HealthCheckService {

    /**
     * 执行全面健康检查
     * @return 健康状态
     */
    HealthStatus checkHealth();

    /**
     * 检查特定组件
     * @param componentName 组件名称
     * @return 组件健康状态
     */
    ComponentHealth checkComponent(String componentName);

    /**
     * 获取健康历史记录
     * @param componentName 组件名称
     * @param hours 小时数
     * @return 健康记录列表
     */
    List<HealthRecord> getHealthHistory(String componentName, int hours);

    /**
     * 注册健康检查器
     * @param name 检查器名称
     * @param checker 检查器实例
     */
    void registerChecker(String name, HealthChecker checker);

    /**
     * 获取所有已注册的检查器
     * @return 检查器映射
     */
    Map<String, HealthChecker> getCheckers();

    /**
     * 判断系统是否健康
     * @return 是否健康
     */
    boolean isHealthy();
}
