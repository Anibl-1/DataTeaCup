package com.dataplatform.common;

import cn.dev33.satoken.exception.DisableServiceException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.apache.catalina.connector.ClientAbortException;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理系统中所有异常，返回统一的错误响应格式
 * 
 * @author dataplatform
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ==================== Sa-Token 安全异常 ====================

    @ExceptionHandler(NotLoginException.class)
    public Result<Object> handleNotLoginException(NotLoginException e) {
        String message;
        switch (e.getType()) {
            case NotLoginException.NOT_TOKEN:
                message = "未提供Token，请先登录";
                break;
            case NotLoginException.INVALID_TOKEN:
                message = "Token无效，请重新登录";
                break;
            case NotLoginException.TOKEN_TIMEOUT:
                message = "Token已过期，请重新登录";
                break;
            case NotLoginException.BE_REPLACED:
                message = "账号已在其他设备登录";
                break;
            case NotLoginException.KICK_OUT:
                message = "账号已被强制下线";
                break;
            default:
                message = "未登录或登录已过期";
        }
        logger.warn("[Auth] 未登录: type={}, message={}", e.getType(), message);
        return Result.error(ErrorCode.UNAUTHORIZED, message);
    }

    @ExceptionHandler(NotPermissionException.class)
    public Result<Object> handleNotPermissionException(NotPermissionException e) {
        logger.warn("[Auth] 权限不足: permission={}", e.getPermission());
        return Result.error(ErrorCode.FORBIDDEN, "权限不足，缺少权限: " + e.getPermission());
    }

    @ExceptionHandler(NotRoleException.class)
    public Result<Object> handleNotRoleException(NotRoleException e) {
        logger.warn("[Auth] 角色不足: role={}", e.getRole());
        return Result.error(ErrorCode.FORBIDDEN, "角色权限不足，需要角色: " + e.getRole());
    }

    @ExceptionHandler(DisableServiceException.class)
    public Result<Object> handleDisableServiceException(DisableServiceException e) {
        logger.warn("[Auth] 账号已被封禁: service={}, level={}", e.getService(), e.getLevel());
        return Result.error(ErrorCode.FORBIDDEN, "账号已被封禁，请联系管理员");
    }

    // ==================== 业务异常 ====================

    @ExceptionHandler(BusinessException.class)
    public Result<Object> handleBusinessException(BusinessException e) {
        logger.warn("Business exception: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        logger.warn("Validation failed: {}", message);
        return Result.error(ErrorCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Object> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        logger.warn("Binding failed: {}", message);
        return Result.error(ErrorCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Object> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        logger.warn("Constraint violation: {}", message);
        return Result.error(ErrorCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Object> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        logger.warn("Method not supported: {} {}", e.getMethod(), e.getMessage());
        return Result.error(405, "Method " + e.getMethod() + " not supported");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Object> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        logger.warn("Missing required parameter: {}", e.getParameterName());
        return Result.error(ErrorCode.PARAM_ERROR, "Missing required parameter: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Object> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        logger.warn("Parameter type mismatch: {} should be {}", e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown");
        return Result.error(ErrorCode.PARAM_ERROR, "Parameter type mismatch: " + e.getName());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<Object> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        logger.warn("Unsupported media type: {}", e.getContentType());
        return Result.error(415, "Unsupported media type");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Object> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        logger.warn("Request body parse failed: {}", e.getMessage());
        return Result.error(ErrorCode.PARAM_ERROR, "Invalid request data format");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Object> handleNoHandlerFoundException(NoHandlerFoundException e) {
        logger.warn("Resource not found: {} {}", e.getHttpMethod(), e.getRequestURL());
        return Result.error(404, "Resource not found");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        logger.warn("Upload file size exceeded: {}", e.getMessage());
        return Result.error(ErrorCode.PARAM_ERROR, "Upload file size exceeded");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Object> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        logger.warn("Data integrity violation: {}", e.getMessage());
        String msg = "Data operation failed, possible duplicate data or referential constraint";
        if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
            msg = "Data already exists, please do not repeat the operation";
        }
        return Result.error(ErrorCode.ERROR, msg);
    }

    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbortException(ClientAbortException e) {
        logger.debug("Client connection aborted: {}", e.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public void handleIOException(IOException e) {
        String message = e.getMessage();
        if (message != null && (message.contains("Broken pipe") || 
            message.contains("Connection reset") ||
            message.contains("ClosedChannelException"))) {
            logger.debug("Client connection error: {}", message);
            return;
        }
        logger.error("IO exception", e);
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<Object> handleRuntimeException(RuntimeException e) {
        if (isClientAbortException(e)) {
            logger.debug("Client disconnected (Runtime): {}", e.getMessage());
            return null;
        }
        logger.error("Runtime exception", e);
        return Result.error(ErrorCode.ERROR, "Internal server error, please try again later");
    }

    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception e) {
        if (isClientAbortException(e)) {
            logger.debug("Client disconnected (Exception): {}", e.getMessage());
            return null;
        }
        logger.error("System exception", e);
        return Result.error(ErrorCode.ERROR, "System error, please try again later");
    }

    private boolean isClientAbortException(Throwable e) {
        if (e == null) return false;
        if (e instanceof ClientAbortException) return true;
        String className = e.getClass().getName();
        if (className.contains("ClientAbortException") || 
            className.contains("ClosedChannelException")) {
            return true;
        }
        String message = e.getMessage();
        if (message != null && (message.contains("Broken pipe") || 
            message.contains("Connection reset") ||
            message.contains("ClosedChannelException"))) {
            return true;
        }
        return isClientAbortException(e.getCause());
    }
}
