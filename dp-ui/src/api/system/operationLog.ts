/**
 * 操作日志API
 */
import request from '../request'
import type { 
  OperationLog, 
  OperationLogQuery, 
  OperationStats,
  UserStats,
  ModuleStats,
  OperationTypeStats,
  TrendData
} from '@/types/operationLog'
import type { PageResult } from '@/types/api'

/**
 * 获取操作日志列表
 */
export function getOperationLogList(params: OperationLogQuery) {
  return request.get<PageResult<OperationLog>>('/operation-log/list', { params })
}

/**
 * 获取日志详情
 */
export function getOperationLogDetail(id: number) {
  return request.get<OperationLog>(`/operation-log/detail/${id}`)
}

/**
 * 获取统计概览
 */
export function getStatsOverview(startTime?: string, endTime?: string) {
  return request.get<OperationStats>('/operation-log/stats/overview', {
    params: { startTime, endTime }
  })
}

/**
 * 按用户统计
 */
export function getStatsByUser(startTime?: string, endTime?: string) {
  return request.get<UserStats[]>('/operation-log/stats/by-user', {
    params: { startTime, endTime }
  })
}

/**
 * 按模块统计
 */
export function getStatsByModule(startTime?: string, endTime?: string) {
  return request.get<ModuleStats[]>('/operation-log/stats/by-module', {
    params: { startTime, endTime }
  })
}

/**
 * 按操作类型统计
 */
export function getStatsByOperation(startTime?: string, endTime?: string) {
  return request.get<OperationTypeStats[]>('/operation-log/stats/by-operation', {
    params: { startTime, endTime }
  })
}

/**
 * 获取操作趋势
 */
export function getTrend(startTime?: string, endTime?: string, groupBy: 'hour' | 'day' | 'month' = 'day') {
  return request.get<TrendData[]>('/operation-log/stats/trend', {
    params: { startTime, endTime, groupBy }
  })
}

/**
 * 清理历史日志
 */
export function cleanHistory(days: number = 90) {
  return request.delete<number>('/operation-log/clean', {
    params: { days }
  })
}

/**
 * 导出日志
 */
export function exportLogs(params: OperationLogQuery) {
  return request.post('/operation-log/export', params, {
    responseType: 'blob'
  })
}
