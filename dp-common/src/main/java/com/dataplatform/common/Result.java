package com.dataplatform.common;

import com.dataplatform.common.exception.ErrorCode;
import lombok.Data;

/**
 * 统一响应结果类
 * 用于封装所有API接口的返回结果
 * 
 * @param <T> 数据类型
 * @author dataplatform
 */
@Data
public class Result<T> {
    /** 响应码 */
    private int code;
    
    /** 响应消息 */
    private String msg;
    
    /** 响应数据 */
    private T data;
    
    /** 响应时间戳 */
    private Long timestamp;

    /**
     * 构造函数
     * 自动设置当前时间戳
     */
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 判断响应是否成功
     * 
     * @return 如果响应码为成功码则返回true
     */
    public boolean isSuccess() {
        return this.code == ErrorCode.SUCCESS;
    }

    /**
     * 成功响应（带数据）
     * 
     * @param data 响应数据
     * @return 响应结果
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ErrorCode.SUCCESS);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    /**
     * 成功响应（带消息和数据）
     * 
     * @param msg 响应消息
     * @param data 响应数据
     * @return 响应结果
     */
    public static <T> Result<T> success(String msg, T data) {
        Result<T> result = new Result<>();
        result.setCode(ErrorCode.SUCCESS);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    /**
     * 成功响应（无数据）
     * 
     * @return 响应结果
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 错误响应（使用默认错误码）
     * 
     * @param msg 错误消息
     * @return 响应结果
     */
    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.setCode(ErrorCode.ERROR);
        result.setMsg(msg);
        return result;
    }

    /**
     * 错误响应（指定错误码）
     * 
     * @param code 错误码
     * @param msg 错误消息
     * @return 响应结果
     */
    public static <T> Result<T> error(int code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}
