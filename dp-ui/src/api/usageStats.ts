import request from './request'
import type { ApiResponse } from '@/types/api'

// ==================== TypeScript 接口定义 ====================

/** 使用统计记录 */
export interface UsageStats {
  id?: number
  userId?: string
  featureCode?: string
  /** 操作类型: view, query, export, create, edit, delete */
  action?: string
  /** 资源类型: dashboard, report, datasource */
  resourceType?: string
  resourceId?: string
  /** 操作耗时(毫秒) */
  duration?: number
  /** 查询结果行数 */
  resultCount?: number
  /** 错误码(如有) */
  errorCode?: string
  /** 客户端类型: web, mobile, api */
  clientType?: string
  createTime?: string
}

/** 资源访问排行 */
export interface ResourceAccessRank {
  resourceId: string
  resourceType: string
  accessCount: number
  uniqueUsers: number
}

// ==================== DAU / WAU / MAU ====================

/** 获取日活跃用户数 */
export const getDAU = (date: string) => {
  return request.get<ApiResponse<number>>('/usage-stats/dau', { params: { date } })
}

/** 获取周活跃用户数 */
export const getWAU = (weekStart: string) => {
  return request.get<ApiResponse<number>>('/usage-stats/wau', { params: { weekStart } })
}

/** 获取月活跃用户数 */
export const getMAU = (year: number, month: number) => {
  return request.get<ApiResponse<number>>('/usage-stats/mau', { params: { year, month } })
}

// ==================== 功能使用频率 ====================

/** 获取功能使用频率统计 */
export const getFeatureUsage = (startDate: string, endDate: string) => {
  return request.get<ApiResponse<Record<string, number>>>('/usage-stats/feature-usage', {
    params: { startDate, endDate }
  })
}

// ==================== 资源排行 ====================

/** 获取资源访问排行 */
export const getResourceRank = (params: {
  resourceType: string
  topN?: number
  startDate: string
  endDate: string
}) => {
  return request.get<ApiResponse<ResourceAccessRank[]>>('/usage-stats/resource-rank', { params })
}

// ==================== 性能分布 ====================

/** 获取查询性能分布 */
export const getQueryPerformance = (startDate: string, endDate: string) => {
  return request.get<ApiResponse<Record<string, number>>>('/usage-stats/query-performance', {
    params: { startDate, endDate }
  })
}

// ==================== 错误率 ====================

/** 获取错误率统计 */
export const getErrorRate = (startDate: string, endDate: string) => {
  return request.get<ApiResponse<Record<string, number>>>('/usage-stats/error-rate', {
    params: { startDate, endDate }
  })
}

// ==================== CSV 导出 ====================

/** 导出统计数据为 CSV 文件 */
export const exportCSV = (startDate: string, endDate: string, limit?: number) => {
  return request.get<Blob>('/usage-stats/export/csv', {
    params: { startDate, endDate, limit },
    responseType: 'blob'
  })
}
