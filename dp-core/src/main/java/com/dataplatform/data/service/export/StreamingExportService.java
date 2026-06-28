package com.dataplatform.data.service.export;

import com.dataplatform.data.dto.export.ExportConfig;
import com.dataplatform.data.dto.export.ExportProgress;
import com.dataplatform.data.dto.export.ExportRequest;
import com.dataplatform.data.entity.ExportTask;
import com.dataplatform.data.service.masking.MaskingRule;

import java.io.OutputStream;
import java.util.List;

/**
 * 流式导出服务接口
 * 支持异步导出任务管理、进度跟踪和断点续传
 * 
 * 需求: 17.2 - 当导出数据量超过50000行时，创建后台异步任务处理
 */
public interface StreamingExportService {
    
    /**
     * 流式导出Excel
     * 
     * @param out 输出流
     * @param sql SQL查询语句
     * @param dataSourceId 数据源ID
     * @param maskingRules 脱敏规则列表
     * @param config 导出配置
     */
    void exportExcelStream(OutputStream out, String sql, Long dataSourceId,
                          List<MaskingRule> maskingRules, ExportConfig config);
    
    /**
     * 流式导出CSV
     * 
     * @param out 输出流
     * @param sql SQL查询语句
     * @param dataSourceId 数据源ID
     * @param maskingRules 脱敏规则列表
     * @param config 导出配置
     */
    void exportCsvStream(OutputStream out, String sql, Long dataSourceId,
                        List<MaskingRule> maskingRules, ExportConfig config);
    
    /**
     * 创建异步导出任务
     * 
     * @param request 导出请求
     * @return 任务ID
     */
    Long createAsyncExportTask(ExportRequest request);
    
    /**
     * 异步执行导出任务
     * 
     * @param taskId 任务ID
     */
    void executeAsyncExport(Long taskId);
    
    /**
     * 获取导出进度
     * 
     * @param taskId 任务ID
     * @return 导出进度信息
     */
    ExportProgress getProgress(Long taskId);
    
    /**
     * 恢复导出任务（断点续传）
     * 需求: 17.3 - 支持导出任务的断点续传功能
     * 
     * @param taskId 任务ID
     */
    void resumeExport(Long taskId);
    
    /**
     * 暂停导出任务（保存断点）
     * 需求: 17.3 - 支持导出任务的断点续传功能
     * 
     * @param taskId 任务ID
     * @return 是否暂停成功
     */
    boolean pauseExport(Long taskId);
    
    /**
     * 取消导出任务
     * 
     * @param taskId 任务ID
     * @return 是否取消成功
     */
    boolean cancelExport(Long taskId);
    
    /**
     * 获取导出任务详情
     * 
     * @param taskId 任务ID
     * @return 导出任务实体
     */
    ExportTask getExportTask(Long taskId);
    
    /**
     * 获取用户的导出任务列表
     * 
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 导出任务列表
     */
    List<ExportTask> getUserExportTasks(Long userId, int page, int pageSize);
    
    /**
     * 获取导出文件下载URL
     * 
     * @param taskId 任务ID
     * @return 下载URL或文件路径
     */
    String getDownloadUrl(Long taskId);
    
    /**
     * 定时清理过期导出文件及DB记录
     */
    void cleanupExpiredExportFiles();
}
