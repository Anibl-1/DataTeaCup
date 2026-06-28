package com.dataplatform.api.common;

import com.dataplatform.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 前端错误上报控制器
 * 
 * 接收前端自动收集的错误并记录到日志系统。
 * 
 * 需求 18.3: THE Error_Handler SHALL 自动收集前端错误并上报到服务端
 */
@Tag(name = "错误上报", description = "前端错误上报接口")
@RestController
@RequestMapping("/error-report")
public class ErrorReportController {

    private static final Logger logger = LoggerFactory.getLogger(ErrorReportController.class);
    private static final Logger errorReportLogger = LoggerFactory.getLogger("frontend.error");

    /**
     * 接收前端错误上报
     * 
     * @param request 错误上报请求
     * @return 上报结果
     */
    @Operation(summary = "上报前端错误", description = "接收前端自动收集的错误信息")
    @PostMapping("/report")
    public Result<Void> reportErrors(@RequestBody ErrorReportRequest request) {
        if (request == null || request.getErrors() == null || request.getErrors().isEmpty()) {
            return Result.success();
        }

        for (ErrorReport error : request.getErrors()) {
            logError(error);
        }

        logger.info("接收到前端错误上报，数量: {}", request.getErrors().size());
        return Result.success();
    }

    /**
     * 记录单个错误到日志
     */
    private void logError(ErrorReport error) {
        String logMessage = String.format(
            "[%s] TraceId: %s | Type: %s | Severity: %s | URL: %s | Message: %s",
            error.getType(),
            error.getTraceId(),
            error.getType(),
            error.getSeverity(),
            error.getUrl(),
            error.getMessage()
        );

        // 根据严重级别选择日志级别
        switch (error.getSeverity()) {
            case "critical":
            case "high":
                errorReportLogger.error(logMessage);
                if (error.getStack() != null) {
                    errorReportLogger.error("Stack: {}", error.getStack());
                }
                break;
            case "medium":
                errorReportLogger.warn(logMessage);
                break;
            default:
                errorReportLogger.info(logMessage);
        }

        // 如果是 Vue 组件错误，记录组件信息
        if (error.getComponentInfo() != null) {
            errorReportLogger.debug("Vue Component: {} | Hook: {}", 
                error.getComponentInfo().get("name"),
                error.getComponentInfo().get("hook"));
        }
    }

    // ==================== 请求/响应类 ====================

    /**
     * 错误上报请求
     */
    public static class ErrorReportRequest {
        private List<ErrorReport> errors;
        private Map<String, Object> meta;

        public List<ErrorReport> getErrors() {
            return errors;
        }

        public void setErrors(List<ErrorReport> errors) {
            this.errors = errors;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }
    }

    /**
     * 错误报告
     */
    public static class ErrorReport {
        private String traceId;
        private String type;
        private String message;
        private String stack;
        private String source;
        private Integer lineno;
        private Integer colno;
        private Long timestamp;
        private String url;
        private String userAgent;
        private String severity;
        private Map<String, Object> componentInfo;
        private Map<String, Object> context;

        // Getters and Setters
        public String getTraceId() {
            return traceId;
        }

        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getStack() {
            return stack;
        }

        public void setStack(String stack) {
            this.stack = stack;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public Integer getLineno() {
            return lineno;
        }

        public void setLineno(Integer lineno) {
            this.lineno = lineno;
        }

        public Integer getColno() {
            return colno;
        }

        public void setColno(Integer colno) {
            this.colno = colno;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public Map<String, Object> getComponentInfo() {
            return componentInfo;
        }

        public void setComponentInfo(Map<String, Object> componentInfo) {
            this.componentInfo = componentInfo;
        }

        public Map<String, Object> getContext() {
            return context;
        }

        public void setContext(Map<String, Object> context) {
            this.context = context;
        }
    }
}
