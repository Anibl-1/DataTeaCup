/**
 * 操作日志类型定义
 */

export interface OperationLog {
  id: number
  userId?: number
  username: string
  operationType: string
  moduleName: string
  operationDesc: string
  requestMethod: string
  requestUrl: string
  requestParams?: string
  responseResult?: string
  ipAddress: string
  durationMs: number
  status: 'success' | 'failed'
  errorMessage?: string
  createTime: string
}

export interface OperationLogQuery {
  userId?: number
  username?: string
  moduleName?: string
  operationType?: string
  status?: string
  startTime?: string
  endTime?: string
  keyword?: string
  page?: number
  pageSize?: number
}

export interface OperationStats {
  total: number
  successCount: number
  failedCount: number
  successRate: number
}

export interface UserStats {
  username: string
  count: number
  success_count: number
  failed_count: number
}

export interface ModuleStats {
  module_name: string
  count: number
}

export interface OperationTypeStats {
  operation_type: string
  count: number
}

export interface TrendData {
  time_key: string
  count: number
  success_count: number
  failed_count: number
}
