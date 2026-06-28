package com.dataplatform.data.service.sentinel;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;

import java.util.function.Supplier;

/**
 * Sentinel服务接口
 * 提供限流、熔断的编程式API
 */
public interface SentinelService {

    /**
     * 执行带限流保护的操作
     * @param resource 资源名称
     * @param supplier 业务逻辑
     * @param fallback 降级逻辑
     * @return 执行结果
     */
    <T> T executeWithFlowControl(String resource, Supplier<T> supplier, Supplier<T> fallback);

    /**
     * 执行带熔断保护的操作
     * @param resource 资源名称
     * @param supplier 业务逻辑
     * @param fallback 降级逻辑
     * @return 执行结果
     */
    <T> T executeWithCircuitBreaker(String resource, Supplier<T> supplier, Supplier<T> fallback);

    /**
     * 执行带热点参数限流的操作
     * @param resource 资源名称
     * @param param 热点参数
     * @param supplier 业务逻辑
     * @param fallback 降级逻辑
     * @return 执行结果
     */
    <T> T executeWithParamFlowControl(String resource, Object param, Supplier<T> supplier, Supplier<T> fallback);

    /**
     * 检查资源是否可用（不阻塞）
     * @param resource 资源名称
     * @return 是否可用
     */
    boolean isResourceAvailable(String resource);

    /**
     * 获取资源当前QPS
     * @param resource 资源名称
     * @return 当前QPS
     */
    double getCurrentQps(String resource);

    /**
     * 获取资源当前并发数
     * @param resource 资源名称
     * @return 当前并发数
     */
    int getCurrentConcurrency(String resource);

    /**
     * 检查熔断器是否打开
     * @param resource 资源名称
     * @return 是否熔断
     */
    boolean isCircuitBreakerOpen(String resource);
}
