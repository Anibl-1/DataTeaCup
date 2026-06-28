package com.dataplatform.data.handler;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.dataplatform.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Sentinel限流降级处理器
 * 处理各种类型的流量控制异常
 */
@Slf4j
@Component
public class SentinelBlockHandler {

    // 错误码定义
    public static final int RATE_LIMITED = 429;
    public static final int SERVICE_DEGRADED = 503;
    public static final int SYSTEM_OVERLOAD = 503;
    public static final int AUTHORITY_BLOCKED = 403;

    /**
     * 通用限流处理
     */
    public static Result<?> handleBlock(BlockException ex) {
        log.warn("请求被限流: resource={}, rule={}", 
            ex.getRule() != null ? ex.getRule().getResource() : "unknown", ex.getRule());
        
        if (ex instanceof FlowException) {
            return handleFlowException((FlowException) ex);
        } else if (ex instanceof DegradeException) {
            return handleDegradeException((DegradeException) ex);
        } else if (ex instanceof ParamFlowException) {
            return handleParamFlowException((ParamFlowException) ex);
        } else if (ex instanceof SystemBlockException) {
            return handleSystemBlockException((SystemBlockException) ex);
        } else if (ex instanceof AuthorityException) {
            return handleAuthorityException((AuthorityException) ex);
        }
        
        return Result.error(RATE_LIMITED, "请求被拒绝，请稍后重试");
    }

    /**
     * 处理流量限流异常
     */
    public static Result<?> handleFlowException(FlowException ex) {
        String resource = ex.getRule() != null ? ex.getRule().getResource() : "unknown";
        log.warn("流量限流触发: resource={}", resource);
        return Result.error(RATE_LIMITED, "请求过于频繁，请5秒后重试");
    }

    /**
     * 处理熔断降级异常
     */
    public static Result<?> handleDegradeException(DegradeException ex) {
        String resource = ex.getRule() != null ? ex.getRule().getResource() : "unknown";
        int timeWindow = ex.getRule() != null ? ex.getRule().getTimeWindow() : 30;
        log.warn("熔断降级触发: resource={}, timeWindow={}s", resource, timeWindow);
        return Result.error(SERVICE_DEGRADED, 
            String.format("服务暂时不可用，预计%d秒后恢复", timeWindow));
    }

    /**
     * 处理热点参数限流异常
     */
    public static Result<?> handleParamFlowException(ParamFlowException ex) {
        String resource = ex.getRule() != null ? ex.getRule().getResource() : "unknown";
        log.warn("热点参数限流触发: resource={}", resource);
        return Result.error(RATE_LIMITED, "该参数请求过于频繁，请3秒后重试");
    }

    /**
     * 处理系统保护异常
     */
    public static Result<?> handleSystemBlockException(SystemBlockException ex) {
        log.warn("系统保护触发: rule={}", ex.getRule());
        return Result.error(SYSTEM_OVERLOAD, "系统负载过高，请10秒后重试");
    }

    /**
     * 处理授权异常
     */
    public static Result<?> handleAuthorityException(AuthorityException ex) {
        String resource = ex.getRule() != null ? ex.getRule().getResource() : "unknown";
        log.warn("授权限制触发: resource={}", resource);
        return Result.error(AUTHORITY_BLOCKED, "无权访问该资源");
    }

    /**
     * API请求限流处理
     */
    public static Result<?> handleApiBlock(BlockException ex) {
        log.warn("API请求被限流");
        return handleBlock(ex);
    }

    /**
     * 数据查询限流处理
     */
    public static Result<?> handleQueryBlock(BlockException ex) {
        log.warn("数据查询被限流");
        return Result.error(RATE_LIMITED, "查询请求过于频繁，请稍后重试");
    }

    /**
     * 报表导出限流处理
     */
    public static Result<?> handleExportBlock(BlockException ex) {
        log.warn("报表导出被限流");
        return Result.error(RATE_LIMITED, "导出请求过于频繁，请10秒后重试");
    }

    /**
     * 登录限流处理
     */
    public static Result<?> handleLoginBlock(BlockException ex) {
        log.warn("登录请求被限流");
        return Result.error(RATE_LIMITED, "登录请求过于频繁，请30秒后重试");
    }

    /**
     * 外部服务熔断降级处理
     */
    public static Result<?> handleExternalServiceFallback(BlockException ex) {
        log.warn("外部服务熔断降级");
        return Result.error(SERVICE_DEGRADED, "外部服务暂时不可用");
    }

    /**
     * 数据库操作熔断降级处理
     */
    public static Result<?> handleDatabaseFallback(BlockException ex) {
        log.warn("数据库操作熔断降级");
        return Result.error(SERVICE_DEGRADED, "数据库响应较慢，请稍后重试");
    }

    /**
     * 用户操作热点限流处理
     */
    public static Result<?> handleUserOperationBlock(Long userId, BlockException ex) {
        log.warn("用户操作热点限流: userId={}", userId);
        return Result.error(RATE_LIMITED, "操作过于频繁，请稍后重试");
    }

    /**
     * 租户操作热点限流处理
     */
    public static Result<?> handleTenantOperationBlock(Long tenantId, BlockException ex) {
        log.warn("租户操作热点限流: tenantId={}", tenantId);
        return Result.error(RATE_LIMITED, "租户请求过于频繁，请稍后重试");
    }

    /**
     * 数据源查询热点限流处理
     */
    public static Result<?> handleDatasourceQueryBlock(Long datasourceId, BlockException ex) {
        log.warn("数据源查询热点限流: datasourceId={}", datasourceId);
        return Result.error(RATE_LIMITED, "该数据源查询过于频繁，请稍后重试");
    }
}
