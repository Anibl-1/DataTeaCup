package com.dataplatform.common;

import lombok.Data;
import java.util.List;

/**
 * 分页结果类
 * 
 * @param <T> 数据类型
 * @author dataplatform
 */
@Data
public class PageResult<T> {
    /** 数据列表 */
    private List<T> list;
    
    /** 总记录数 */
    private long total;

    /**
     * 无参构造函数
     */
    public PageResult() {
    }

    /**
     * 带参构造函数
     * 
     * @param list 数据列表
     * @param total 总记录数
     */
    public PageResult(List<T> list, long total) {
        this.list = list;
        this.total = total;
    }

    /**
     * 静态工厂方法
     *
     * @param list 数据列表
     * @param total 总记录数
     * @return PageResult 实例
     */
    public static <T> PageResult<T> of(List<T> list, long total) {
        return new PageResult<>(list, total);
    }
}
