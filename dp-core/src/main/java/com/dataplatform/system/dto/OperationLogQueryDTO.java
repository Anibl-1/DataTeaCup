package com.dataplatform.system.dto;

import java.util.Date;

/**
 * 操作日志查询条件DTO
 * 支持按操作类型、操作人、时间范围、模块名称组合筛选
 */
public class OperationLogQueryDTO {
    /** 用户ID */
    private Long userId;
    /** 操作人用户名（模糊匹配） */
    private String username;
    /** 操作类型（精确匹配） */
    private String operationType;
    /** 模块名称（精确匹配） */
    private String moduleName;
    /** 操作状态 */
    private String status;
    /** 开始时间 */
    private Date startTime;
    /** 结束时间 */
    private Date endTime;
    /** 关键字（模糊匹配操作描述和请求URL） */
    private String keyword;
    /** 页码 */
    private Integer page;
    /** 每页大小 */
    private Integer pageSize;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    /** 计算偏移量 */
    public int getOffset() {
        int p = (page != null && page > 0) ? page : 1;
        int ps = (pageSize != null && pageSize > 0) ? pageSize : 10;
        return (p - 1) * ps;
    }
}
