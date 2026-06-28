/* eslint-disable @typescript-eslint/no-explicit-any */
import request from './request'
import type { ApiResponse, PageResult, PageParams } from '@/types/api'
import type { ReportDefinition, ReportDefinitionForm, ReportField } from '@/types/reportDefinition'

/**
 * 获取所有可用的报表列表（用于图表设计器选择）
 */
export const getAvailableReports = () => {
  return request.get<ApiResponse<ReportDefinition[]>>('/report-definition/available')
}

/**
 * 获取报表定义列表（分页）
 */
export const getReportDefinitionList = (params: PageParams) => {
  return request.get<ApiResponse<PageResult<ReportDefinition>>>('/report-definition/list', { params })
}

/**
 * 根据ID获取报表定义
 */
export const getReportDefinitionById = (id: number) => {
  return request.get<ApiResponse<ReportDefinition>>(`/report-definition/${id}`)
}

/**
 * 根据编码获取报表定义
 */
export const getReportDefinitionByCode = (code: string) => {
  return request.get<ApiResponse<ReportDefinition>>(`/report-definition/code/${code}`)
}

/**
 * 创建报表定义
 */
export const createReportDefinition = (data: ReportDefinitionForm) => {
  return request.post<ApiResponse<number>>('/report-definition/create', data)
}

/**
 * 更新报表定义
 */
export const updateReportDefinition = (data: ReportDefinitionForm) => {
  return request.post<ApiResponse<void>>('/report-definition/update', data)
}

/**
 * 删除报表定义
 */
export const deleteReportDefinition = (id: number) => {
  return request.delete<ApiResponse<void>>(`/report-definition/${id}`)
}

/**
 * 复制报表定义
 */
export const copyReportDefinition = (id: number) => {
  return request.post<ApiResponse<ReportDefinition>>(`/report-definition/${id}/copy`)
}

// 大数据查询超时时间：5分钟
const LARGE_DATA_TIMEOUT = 300000

/**
 * 执行报表查询
 */
export const executeReportQuery = (id: number, params?: PageParams & { params?: string }) => {
  return request.get<ApiResponse<PageResult<Record<string, any>>>>(`/report-definition/${id}/execute`, {
    params: { page: params?.page || 1, pageSize: params?.pageSize || 10, filters: params?.filters, params: params?.params },
    timeout: LARGE_DATA_TIMEOUT
  })
}

/**
 * 执行报表查询（根据编码）
 */
export const executeReportQueryByCode = (code: string, params?: PageParams & { params?: string }) => {
  return request.get<ApiResponse<PageResult<Record<string, any>>>>(`/report-definition/code/${code}/execute`, {
    params: { page: params?.page || 1, pageSize: params?.pageSize || 10, filters: params?.filters, params: params?.params },
    timeout: LARGE_DATA_TIMEOUT
  })
}

/**
 * 测试SQL并获取字段信息
 */
export const testSqlAndGetFields = (dataSourceId: number, sql: string) => {
  return request.post<ApiResponse<ReportField[]>>('/report-definition/test-sql', {
    dataSourceId,
    sql
  })
}

/**
 * 导出报表数据为Excel（根据ID）
 */
export const exportReportDefinition = (id: number, filters?: string, params?: string) => {
  const queryParams: Record<string, string> = {}
  if (filters) queryParams.filters = filters
  if (params) queryParams.params = params
  return request.get(`/report-definition/${id}/export`, {
    params: Object.keys(queryParams).length > 0 ? queryParams : undefined,
    responseType: 'blob',
    timeout: LARGE_DATA_TIMEOUT * 2  // 导出超时10分钟
  })
}

/**
 * 导出报表数据为Excel（根据编码）
 */
export const exportReportDefinitionByCode = (code: string, filters?: string, params?: string) => {
  const queryParams: Record<string, string> = {}
  if (filters) queryParams.filters = filters
  if (params) queryParams.params = params
  return request.get(`/report-definition/code/${code}/export`, {
    params: Object.keys(queryParams).length > 0 ? queryParams : undefined,
    responseType: 'blob',
    timeout: LARGE_DATA_TIMEOUT * 2  // 导出超时10分钟
  })
}

/**
 * 导出报表数据为PDF（根据ID）
 */
export const exportReportAsPdf = (id: number, filters?: string, params?: string) => {
  const queryParams: Record<string, string> = {}
  if (filters) queryParams.filters = filters
  if (params) queryParams.params = params
  return request.get(`/report-definition/${id}/export-pdf`, {
    params: Object.keys(queryParams).length > 0 ? queryParams : undefined,
    responseType: 'blob',
    timeout: LARGE_DATA_TIMEOUT * 2
  })
}

// ==================== 报表分享 ====================

export interface ReportShare {
  id: number
  targetId: number
  targetType: string
  shareToken: string
  password?: string
  expireTime?: string
  maxAccessCount: number
  accessCount: number
  status: number
  createBy?: number
  createTime: string
}

/**
 * 创建报表分享链接
 */
export const createReportShare = (id: number, params: {
  password?: string
  expireHours?: number
  maxAccessCount?: number
}) => {
  return request.post<ApiResponse<ReportShare>>(`/report-definition/${id}/share`, params)
}

/**
 * 获取报表分享列表
 */
export const getReportShares = (id: number) => {
  return request.get<ApiResponse<ReportShare[]>>(`/report-definition/${id}/shares`)
}

// ==================== 报表订阅 ====================

/**
 * 订阅报表
 */
export const subscribeReport = (id: number, params: { email: string, cron?: string }) => {
  return request.post<ApiResponse<any>>(`/report-definition/${id}/subscribe`, params)
}

/**
 * 获取报表订阅列表
 */
export const getReportSubscriptions = (id: number) => {
  return request.get<ApiResponse<any[]>>(`/report-definition/${id}/subscriptions`)
}

/**
 * 取消报表订阅
 */
export const unsubscribeReport = (id: number, subId: number) => {
  return request.delete<ApiResponse<void>>(`/report-definition/${id}/subscriptions/${subId}`)
}

/**
 * 更新报表移动端配置
 */
export const updateReportMobileEnabled = (id: number, mobileEnabled: number) => {
  return request.put<ApiResponse<void>>(`/report-definition/${id}/mobile`, { mobileEnabled })
}

