package com.dataplatform.common.service;

/**
 * 操作日志提供者接口
 * 用于 dp-common 的 OperationLogAspect 记录操作日志，避免直接依赖 dp-system
 */
public interface OperationLogProvider {

    /**
     * 异步保存操作日志
     */
    void saveLogAsync(String module, String operationType, String description,
                      String method, String requestUrl, String requestMethod,
                      String requestParams, String responseResult,
                      String operatorName, String operatorIp, long duration, boolean success);

    /**
     * 异步保存操作日志（含变更前后数据快照）
     */
    default void saveLogAsync(String module, String operationType, String description,
                              String method, String requestUrl, String requestMethod,
                              String requestParams, String responseResult,
                              String operatorName, String operatorIp, long duration, boolean success,
                              String beforeData, String afterData) {
        // 默认实现：忽略 beforeData/afterData，调用原方法保持向后兼容
        saveLogAsync(module, operationType, description, method, requestUrl, requestMethod,
                     requestParams, responseResult, operatorName, operatorIp, duration, success);
    }
}
