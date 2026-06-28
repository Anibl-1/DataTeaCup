package com.dataplatform.data.service.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 分页结果封装类
 * 
 * 用于封装分页查询的结果数据，包含当前页数据和分页元信息。
 * 
 * @param <T> 数据项类型
 * 
 * @see PagedCacheService 分页缓存服务
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResult<T> {
    
    /**
     * 当前页数据列表
     */
    private List<T> data;
    
    /**
     * 当前页码（从 1 开始）
     */
    private int page;
    
    /**
     * 每页大小
     */
    private int pageSize;
    
    /**
     * 总记录数
     */
    private long totalCount;
    
    /**
     * 总页数
     */
    private int totalPages;
    
    /**
     * 是否有下一页
     */
    private boolean hasNext;
    
    /**
     * 是否有上一页
     */
    private boolean hasPrevious;
    
    /**
     * 创建空的分页结果
     * 
     * @param page     页码
     * @param pageSize 每页大小
     * @param <T>      数据类型
     * @return 空的分页结果
     */
    public static <T> PagedResult<T> empty(int page, int pageSize) {
        return PagedResult.<T>builder()
                .data(Collections.emptyList())
                .page(page)
                .pageSize(pageSize)
                .totalCount(0)
                .totalPages(0)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }
    
    /**
     * 创建分页结果
     * 
     * @param data       当前页数据
     * @param page       页码（从 1 开始）
     * @param pageSize   每页大小
     * @param totalCount 总记录数
     * @param <T>        数据类型
     * @return 分页结果
     */
    public static <T> PagedResult<T> of(List<T> data, int page, int pageSize, long totalCount) {
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalCount / pageSize) : 0;
        
        return PagedResult.<T>builder()
                .data(data != null ? data : Collections.emptyList())
                .page(page)
                .pageSize(pageSize)
                .totalCount(totalCount)
                .totalPages(totalPages)
                .hasNext(page < totalPages)
                .hasPrevious(page > 1)
                .build();
    }
    
    /**
     * 获取当前页数据条数
     * 
     * @return 当前页数据条数
     */
    public int getCurrentPageSize() {
        return data != null ? data.size() : 0;
    }
    
    /**
     * 判断是否为空结果
     * 
     * @return 如果没有数据返回 true
     */
    public boolean isEmpty() {
        return data == null || data.isEmpty();
    }
}
