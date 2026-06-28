package com.dataplatform.system.service.impl;

import com.dataplatform.common.service.OperationLogProvider;
import com.dataplatform.system.entity.OperationLog;
import com.dataplatform.system.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 操作日志提供者实现
 * 桥接 dp-common 的 OperationLogAspect 和 dp-system 的 OperationLogService
 */
@Service
public class OperationLogProviderImpl implements OperationLogProvider {

    @Autowired
    private OperationLogService operationLogService;

    @Override
    public void saveLogAsync(String module, String operationType, String description,
                             String method, String requestUrl, String requestMethod,
                             String requestParams, String responseResult,
                             String operatorName, String operatorIp, long duration, boolean success) {
        saveLogAsync(module, operationType, description, method, requestUrl, requestMethod,
                     requestParams, responseResult, operatorName, operatorIp, duration, success,
                     null, null);
    }

    @Override
    public void saveLogAsync(String module, String operationType, String description,
                             String method, String requestUrl, String requestMethod,
                             String requestParams, String responseResult,
                             String operatorName, String operatorIp, long duration, boolean success,
                             String beforeData, String afterData) {
        OperationLog log = new OperationLog();
        log.setModuleName(module);
        log.setOperationType(operationType);
        log.setOperationDesc(description);
        log.setRequestUrl(requestUrl);
        log.setRequestMethod(requestMethod);
        log.setRequestParams(requestParams);
        log.setResponseResult(responseResult);
        log.setUsername(operatorName);
        log.setIpAddress(operatorIp);
        log.setDurationMs(duration);
        log.setStatus(success ? "success" : "failed");
        log.setBeforeData(beforeData);
        log.setAfterData(afterData);
        operationLogService.saveAsync(log);
    }
}
