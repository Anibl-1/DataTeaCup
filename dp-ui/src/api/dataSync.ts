import request from './request'
import type { ApiResponse } from '@/types/api'

// ==================== TypeScript 接口定义 ====================

/** 数据同步任务 */
export interface SyncTask {
  id: string
  name: string
  /** 源连接器类型 */
  sourceConnectorType: string
  /** 源连接配置 */
  sourceConfig: Record<string, string>
  /** 源表名 */
  sourceTable: string
  /** 目标表名 */
  targetTable: string
  /** 同步模式: incremental, full */
  syncMode: string
  /** 增量字段（时间戳或自增ID） */
  syncField?: string
  /** 批次大小 */
  batchSize: number
  /** 最大重试次数 */
  maxRetries: number
  /** 重试间隔(ms) */
  retryIntervalMs: number
  /** 调度 cron 表达式 */
  cronExpression?: string
  /** 依赖的任务ID列表 */
  dependsOn: string[]
  /** 状态: CREATED, RUNNING, PAUSED, COMPLETED, FAILED */
  status: string
  createdAt?: string
  lastRunAt?: string
}

/** 同步执行日志 */
export interface SyncExecutionLog {
  id: string
  taskId: string
  taskName: string
  /** 同步模式 */
  syncMode: string
  /** 同步行数 */
  syncedRows: number
  /** 耗时(ms) */
  durationMs: number
  /** 状态: SUCCESS, FAILED, PARTIAL */
  status: string
  /** 错误信息 */
  errorMessage?: string
  /** 重试次数 */
  retryCount: number
  startTime?: string
  endTime?: string
}

// ==================== 同步任务管理 ====================

/** 获取同步任务列表 */
export const getSyncTasks = () => {
  return request.get<ApiResponse<SyncTask[]>>('/data-sync/tasks')
}

/** 获取同步任务详情 */
export const getSyncTask = (taskId: string) => {
  return request.get<ApiResponse<SyncTask>>(`/data-sync/tasks/${taskId}`)
}

/** 创建同步任务 */
export const createSyncTask = (data: Partial<SyncTask>) => {
  return request.post<ApiResponse<SyncTask>>('/data-sync/tasks', data)
}

/** 启动同步任务（恢复） */
export const startSyncTask = (taskId: string) => {
  return request.post<ApiResponse<void>>(`/data-sync/tasks/${taskId}/start`)
}

/** 停止同步任务（暂停） */
export const stopSyncTask = (taskId: string) => {
  return request.post<ApiResponse<void>>(`/data-sync/tasks/${taskId}/stop`)
}

/** 更新同步任务 */
export const updateSyncTask = (data: Partial<SyncTask>) => {
  return request.put<ApiResponse<SyncTask>>(`/data-sync/tasks/${data.id}`, data)
}

/** 删除同步任务 */
export const deleteSyncTask = (taskId: string) => {
  return request.delete<ApiResponse<void>>(`/data-sync/tasks/${taskId}`)
}

/** 执行同步任务 */
export const executeSyncTask = (taskId: string) => {
  return request.post<ApiResponse<SyncExecutionLog>>(`/data-sync/tasks/${taskId}/execute`)
}

// ==================== 同步日志 ====================

/** 获取同步执行日志 */
export const getSyncLogs = (taskId: string, limit?: number) => {
  return request.get<ApiResponse<SyncExecutionLog[]>>(`/data-sync/tasks/${taskId}/logs`, {
    params: { limit: limit ?? 50 }
  })
}
