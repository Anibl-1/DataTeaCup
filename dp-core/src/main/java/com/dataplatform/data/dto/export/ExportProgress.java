package com.dataplatform.data.dto.export;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 导出进度信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportProgress {
    
    /**
     * 任务ID
     */
    private Long taskId;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 任务状态: PENDING, RUNNING, COMPLETED, FAILED, CANCELLED, PAUSED
     */
    private String status;
    
    /**
     * 进度百分比 (0-100)
     */
    private Integer progressPercent;
    
    /**
     * 总行数
     */
    private Long totalRows;
    
    /**
     * 已处理行数
     */
    private Long processedRows;
    
    /**
     * 当前处理速度（行/秒）
     */
    private Long rowsPerSecond;
    
    /**
     * 预计剩余时间（秒）
     */
    private Long estimatedRemainingSeconds;
    
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
     * 下载URL
     */
    private String downloadUrl;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 断点位置（用于续传）
     */
    private Long checkpointOffset;
    
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
     * 过期时间
     */
    private LocalDateTime expireTime;
    
    /**
     * 任务状态枚举
     */
    public enum Status {
        PENDING("等待中"),
        RUNNING("处理中"),
        COMPLETED("已完成"),
        FAILED("失败"),
        CANCELLED("已取消"),
        PAUSED("已暂停"),
        /**
         * 部分完成状态
         * 需求: 17.6 - 导出任务失败时保存已完成的部分并支持恢复
         */
        PARTIAL_COMPLETED("部分完成");
        
        private final String description;
        
        Status(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static Status fromCode(Integer code) {
            if (code == null) return PENDING;
            switch (code) {
                case 0: return PENDING;
                case 1: return RUNNING;
                case 2: return COMPLETED;
                case 3: return FAILED;
                case 4: return CANCELLED;
                case 5: return PAUSED;
                case 6: return PARTIAL_COMPLETED;
                default: return PENDING;
            }
        }
        
        public Integer toCode() {
            switch (this) {
                case PENDING: return 0;
                case RUNNING: return 1;
                case COMPLETED: return 2;
                case FAILED: return 3;
                case CANCELLED: return 4;
                case PAUSED: return 5;
                case PARTIAL_COMPLETED: return 6;
                default: return 0;
            }
        }
    }
    
    /**
     * 判断任务是否完成（成功或失败）
     */
    public boolean isFinished() {
        return "COMPLETED".equals(status) || "FAILED".equals(status) || 
               "CANCELLED".equals(status) || "PARTIAL_COMPLETED".equals(status);
    }
    
    /**
     * 判断任务是否可以恢复
     */
    public boolean isResumable() {
        return "PAUSED".equals(status) || "FAILED".equals(status) || "PARTIAL_COMPLETED".equals(status);
    }
    
    /**
     * 判断任务是否有部分数据可下载
     * 需求: 17.6 - 导出任务失败时保存已完成的部分并支持恢复
     */
    public boolean hasPartialData() {
        return "PARTIAL_COMPLETED".equals(status) && filePath != null && fileSize != null && fileSize > 0;
    }
}
