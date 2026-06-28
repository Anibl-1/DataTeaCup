package com.dataplatform.common.exception;

/**
 * 业务异常类
 * 用于处理业务逻辑中的异常情况
 * 
 * @author dataplatform
 */
public class BusinessException extends RuntimeException {
    /** 错误码 */
    private int code;
    
    /** 错误信息 */
    private String message;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
        this.message = message;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
