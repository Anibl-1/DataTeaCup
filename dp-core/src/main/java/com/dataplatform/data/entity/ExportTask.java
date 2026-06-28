package com.dataplatform.data.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 导出任务实体
 */
@Data
public class ExportTask {
    private Long id;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 任务类型: report-报表导出, table-表数据导出, chart-图表数据导出
     */
    private String taskType;
    
    /**
     * 关联ID（报表ID/表ID等）
     */
    private Long refId;
    
    /**
     * 关联编码（报表编码等）
     */
    private String refCode;
    
    /**
     * 筛选条件JSON
     */
    private String filters;
    
    /**
     * 自定义查询参数JSON（报表 ${} 参数值）
     */
    private String params;
    
    /**
     * 状态: 0-等待中, 1-处理中, 2-已完成, 3-失败
     */
    private Integer status;
    
    /**
     * 进度百分比 0-100
     */
    private Integer progress;
    
    /**
     * 文件路径（完成后）
     */
    private String filePath;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 总行数
     */
    private Long totalRows;
    
    /**
     * 数据类型: xlsx-Excel文件, zip-压缩包
     */
    private String dataType;
    
    /**
     * 错误信息
     */
    private String errorMsg;
    
    /**
     * 断点位置（已处理的行数，用于断点续传）
     * 需求: 17.3 - 支持导出任务的断点续传功能
     */
    private Long checkpointOffset;
    
    /**
     * 已处理行数（实时更新）
     */
    private Long processedRows;
    
    /**
     * 临时文件路径（用于断点续传时保存中间结果）
     */
    private String tempFilePath;
    
    /**
     * 导出SQL（用于断点续传时重新执行查询）
     */
    private String exportSql;
    
    /**
     * 数据源ID（用于断点续传）
     */
    private Long dataSourceId;
    
    /**
     * 创建用户ID
     */
    private Long createBy;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 完成时间
     */
    private LocalDateTime finishTime;
    
    /**
     * 过期时间（文件保留时间）
     */
    private LocalDateTime expireTime;
}
