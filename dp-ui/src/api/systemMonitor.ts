/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 系统监控API
 */
import request from './request'

export interface SystemHealth {
  'CPU Usage': number
  'Total Memory (GB)': number
  'Used Memory (GB)': number
  'Memory Usage (%)': number
  'Total Disk Space (GB)': number
  'Used Disk Space (GB)': number
  'Available Disk Space (GB)': number
  'Disk Usage (%)': number
  'Database Size (MB)': number
  'Data Size (MB)': number
  'Index Size (MB)': number
  'Free Size (MB)': number
  'Total Tables': number
  'Running Tasks': number
  'Today Operations': number
  'Unread Notifications': number
  'Active DB Connections': number
  'Slow Queries': number
}

export interface ActiveTask {
  id: number
  task_name: string
  data_source_name: string
  table_name: string
  status: string
  last_execute_time: string
  running_minutes: number
}

export interface DataSourceUsage {
  id: number
  name: string
  db_type: string
  host: string
  port: number
  source_task_count: number
  target_task_count: number
  total_task_count: number
  create_time: string
}

export interface OperationLogSummary {
  module_name: string
  operation_type: string
  operation_count: number
  success_count: number
  failure_count: number
  avg_duration_ms: number
  last_operation_time: string
}

export interface TaskReport {
  execute_date: string
  total_executions: number
  running_count: number
  stopped_count: number
  error_count: number
  success_count: number
}

/**
 * 系统监控API服务
 */
export const systemMonitorApi = {
  /**
   * 获取系统健康状态
   */
  getHealth() {
    return request.get<SystemHealth>('/system/monitor/health')
  },

  /**
   * 获取活跃任务列表
   */
  getActiveTasks() {
    return request.get<ActiveTask[]>('/system/monitor/active-tasks')
  },

  /**
   * 获取数据源使用统计
   */
  getDataSourceUsage() {
    return request.get<DataSourceUsage[]>('/system/monitor/datasource-usage')
  },

  /**
   * 获取操作日志汇总
   */
  getOperationLogs() {
    return request.get<OperationLogSummary[]>('/system/monitor/operation-logs')
  },

  /**
   * 清理过期日志
   */
  cleanLogs(days: number = 90) {
    return request.post<{ deleted_records: number; cleanup_time: string }>(`/system/monitor/clean-logs?days=${days}`)
  },

  /**
   * 重置超时任务
   */
  resetStuckTasks(hours: number = 24) {
    return request.post<{ reset_tasks: number; reset_time: string }>(`/system/monitor/reset-stuck-tasks?hours=${hours}`)
  },

  /**
   * 获取任务执行报告
   */
  getTaskReport(startDate: string, endDate: string) {
    return request.get<TaskReport[]>('/system/monitor/task-report', {
      params: { startDate, endDate }
    })
  },

  /**
   * 分析数据源使用情况
   */
  analyzeDataSource() {
    return request.get<DataSourceUsage[]>('/system/monitor/analyze-datasource')
  },

  /**
   * 获取JVM监控指标
   */
  getJvmMetrics() {
    return request.get<any>('/system/monitor/jvm')
  },

  /**
   * 获取监控指标历史趋势
   */
  getMetricsHistory(hours: number = 24) {
    return request.get<any[]>('/system/monitor/metrics/history', {
      params: { hours }
    })
  },

  /**
   * 获取通知列表
   */
  getNotifications(limit: number = 20) {
    return request.get<any[]>('/system/monitor/notifications', {
      params: { limit }
    })
  },

  /**
   * 获取未读通知数量（轻量级接口）
   */
  getUnreadCount() {
    return request.get<{ unreadCount: number }>('/system/monitor/unread-count')
  },

  /**
   * 标记通知为已读
   */
  markNotificationRead(id: number) {
    return request.put(`/system/monitor/notifications/${id}/read`)
  },

  /**
   * 标记所有通知为已读
   */
  markAllNotificationsRead() {
    return request.put('/system/monitor/notifications/read-all')
  },

  // ==================== 慢查询分析 ====================

  /**
   * 获取慢查询列表
   */
  getSlowQueryList(page: number = 1, size: number = 20, startDate?: string, endDate?: string, dataSourceId?: number, minTime?: number) {
    const params: Record<string, any> = { page, size }
    if (startDate) params['startDate'] = startDate
    if (endDate) params['endDate'] = endDate
    if (dataSourceId != null) params['dataSourceId'] = dataSourceId
    if (minTime != null) params['minTime'] = minTime
    return request.get<any>('/system/monitor/slow-query/list', { params })
  },

  /**
   * 获取出现过慢查询的数据源列表
   */
  getSlowQueryDataSources() {
    return request.get<any>('/system/monitor/slow-query/datasources')
  },

  /**
   * 获取当前慢查询阈值
   */
  getSlowQueryThreshold() {
    return request.get<any>('/system/monitor/slow-query/threshold')
  },

  /**
   * 更新慢查询阈值（毫秒）
   */
  updateSlowQueryThreshold(thresholdMs: number) {
    return request.put<any>('/system/monitor/slow-query/threshold', null, {
      params: { thresholdMs }
    })
  },

  /**
   * 获取慢查询统计
   */
  getSlowQueryStats(hours: number = 24) {
    return request.get<any>('/system/monitor/slow-query/stats', {
      params: { hours }
    })
  },

  // ==================== 健康检查 ====================

  /**
   * 获取所有组件最新健康状态
   */
  getHealthStatus() {
    return request.get<any[]>('/system/monitor/health/status')
  },

  /**
   * 手动触发健康检查
   */
  runHealthCheck() {
    return request.post<any[]>('/system/monitor/health/check')
  },

  /**
   * 获取健康检查历史
   */
  getHealthHistory(hours: number = 24, limit: number = 100) {
    return request.get<any[]>('/system/monitor/health/history', {
      params: { hours, limit }
    })
  },

  // ==================== 调度器管理 ====================
  
  /**
   * 获取调度器状态
   */
  getSchedulerStatus() {
    return request.get<any>('/scheduler/status')
  },

  /**
   * 手动触发调度
   */
  triggerScheduler() {
    return request.post<any>('/scheduler/trigger')
  },

  /**
   * 获取调度历史
   */
  getSchedulerHistory() {
    return request.get<any[]>('/scheduler/history')
  }
}
