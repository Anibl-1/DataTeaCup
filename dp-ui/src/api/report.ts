/* eslint-disable @typescript-eslint/no-explicit-any */
import request from './request'
import type { ApiResponse, PageResult, PageParams } from '@/types/api'

/**
 * 查询报表数据
 */
export const queryReport = (params: PageParams & { dataSourceId: number; tableName: string }) => {
  return request.get<ApiResponse<PageResult<Record<string, any>>>>('/report/query', { params })
}

/**
 * 导出报表为Excel
 */
export const exportReport = (params: { dataSourceId: number; tableName: string; filters?: string }) => {
  return request.get<Blob>('/report/export', { 
    params,
    responseType: 'blob'
  }).then((response: any) => {
    // 如果响应是blob，直接返回data
    if (response instanceof Blob) {
      return response
    }
    // 如果响应包含data字段，返回data
    if (response.data) {
      return response.data
    }
    return response
  })
}

/**
 * 导出报表为CSV
 */
export const exportReportAsCsv = (params: { dataSourceId: number; tableName: string; filters?: string }) => {
  return request.get<Blob>('/report/export/csv', { 
    params,
    responseType: 'blob'
  }).then((response: any) => {
    if (response instanceof Blob) {
      return response
    }
    if (response.data) {
      return response.data
    }
    return response
  })
}


/**
 * 获取报表列表（分页）
 * 用于报表版本管理等场景选择报表
 */
export const getReportList = (params: PageParams) => {
  return request.get<ApiResponse<PageResult<Record<string, any>>>>('/report-definition/list', { params })
}
