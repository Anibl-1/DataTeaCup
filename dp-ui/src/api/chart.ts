/* eslint-disable @typescript-eslint/no-explicit-any */
import request from './request'
import type { ApiResponse, PageResult } from '@/types/api'
import type { ChartDefinition, ChartParameterValue } from '@/types/chart'

/**
 * 获取图表定义列表
 */
export const getChartDefinitionList = (params: {
  page?: number
  pageSize?: number
  keyword?: string
  chartType?: string
}) => {
  return request.get<ApiResponse<PageResult<ChartDefinition>>>('/chart-definition/list', { params })
}

/**
 * 根据ID获取图表定义
 */
export const getChartDefinitionById = (id: number) => {
  return request.get<ApiResponse<ChartDefinition>>(`/chart-definition/${id}`)
}

/**
 * 根据编码获取图表定义
 */
export const getChartDefinitionByCode = (code: string) => {
  return request.get<ApiResponse<ChartDefinition>>(`/chart-definition/code/${code}`)
}

/**
 * 创建图表定义
 */
export const createChartDefinition = (chart: ChartDefinition) => {
  return request.post<ApiResponse<ChartDefinition>>('/chart-definition', chart)
}

/**
 * 更新图表定义
 */
export const updateChartDefinition = (id: number, chart: ChartDefinition) => {
  return request.put<ApiResponse<ChartDefinition>>(`/chart-definition/${id}`, chart)
}

/**
 * 删除图表定义
 */
export const deleteChartDefinition = (id: number) => {
  return request.delete<ApiResponse<void>>(`/chart-definition/${id}`)
}

/**
 * 获取图表数据（支持筛选条件、参数和缓存）
 */
export const getChartData = (id: number, params?: {
  filters?: string
  limit?: number
  parameters?: ChartParameterValue
  useCache?: boolean
}) => {
  const queryParams: { filters?: string; limit?: number; useCache?: boolean; parameters?: string } = {}
  if (params?.filters) queryParams.filters = params.filters
  if (params?.limit) queryParams.limit = params.limit
  if (params?.useCache !== undefined) queryParams.useCache = params.useCache
  if (params?.parameters) queryParams.parameters = JSON.stringify(params.parameters)
  return request.get<ApiResponse<Record<string, unknown>[]>>(`/chart-definition/${id}/data`, { params: queryParams })
}

/**
 * 测试SQL并获取数据（用于预览，支持参数）
 */
export const testChartSql = (params: {
  dataSourceId: number
  sqlContent: string
  limit?: number
  parameters?: ChartParameterValue
}) => {
  return request.post<ApiResponse<any[]>>('/chart-definition/test-sql', params)
}

/**
 * 清除图表缓存
 */
export const clearChartCache = (id: number) => {
  return request.delete<ApiResponse<void>>(`/chart-definition/${id}/cache`)
}

/**
 * 获取缓存统计信息
 */
export const getCacheStats = () => {
  return request.get<ApiResponse<{
    totalEntries: number
    expiredEntries: number
    activeEntries: number
    maxSize: number
  }>>('/chart-definition/cache/stats')
}

/**
 * 导出图表数据为Excel
 */
export const exportChartToExcel = (id: number, params?: {
  filters?: string
  limit?: number
}) => {
  const queryParams: { filters?: string; limit?: number } = {}
  if (params?.filters) queryParams.filters = params.filters
  if (params?.limit) queryParams.limit = params.limit
  
  return request.get(`/chart-definition/${id}/export/excel`, {
    params: queryParams,
    responseType: 'blob'
  })
}

/**
 * 导出图表数据为CSV
 */
export const exportChartToCsv = (id: number, params?: {
  filters?: string
  limit?: number
}) => {
  const queryParams: { filters?: string; limit?: number } = {}
  if (params?.filters) queryParams.filters = params.filters
  if (params?.limit) queryParams.limit = params.limit
  
  return request.get(`/chart-definition/${id}/export/csv`, {
    params: queryParams,
    responseType: 'blob'
  })
}

/**
 * 导出图表数据为JSON
 */
export const exportChartToJson = (id: number, params?: {
  filters?: string
  limit?: number
}) => {
  const queryParams: { filters?: string; limit?: number } = {}
  if (params?.filters) queryParams.filters = params.filters
  if (params?.limit) queryParams.limit = params.limit
  
  return request.get(`/chart-definition/${id}/export/json`, {
    params: queryParams,
    responseType: 'blob'
  })
}



/**
 * 获取图表数据统计信息
 */
export const getChartStatistics = (id: number, field: string, filters?: string) => {
  const params: { field: string; filters?: string } = { field }
  if (filters) params.filters = filters
  
  return request.get<ApiResponse<{
    count: number
    sum: number
    avg: number
    min: number
    max: number
    median: number
  }>>(`/chart-definition/${id}/statistics`, { params })
}

/**
 * 复制图表
 */
export const copyChartDefinition = (id: number) => {
  return request.post<ApiResponse<ChartDefinition>>(`/chart-definition/${id}/copy`)
}

/**
 * 更新图表状态
 */
export const updateChartStatus = (id: number, status: number) => {
  return request.put<ApiResponse<ChartDefinition>>(`/chart-definition/${id}/status`, null, {
    params: { status }
  })
}

/**
 * 批量更新图表状态
 */
export const batchUpdateChartStatus = (ids: number[], status: number) => {
  return request.put<ApiResponse<number>>('/chart-definition/batch/status', { ids, status })
}

/**
 * 批量删除图表
 */
export const batchDeleteChartDefinition = (ids: number[]) => {
  return request.delete<ApiResponse<number>>('/chart-definition/batch', { data: { ids } })
}

// ==================== 图表分享/嵌入 ====================

export interface ChartShare {
  id: number
  targetId: number
  targetType: string
  shareToken: string
  password?: string
  expireTime?: string
  maxAccessCount: number
  accessCount: number
  status: number
  createTime: string
}

/**
 * 创建图表分享/嵌入链接
 */
export const createChartShare = (id: number, params: {
  password?: string
  expireHours?: number
  maxAccessCount?: number
}) => {
  return request.post<ApiResponse<ChartShare>>(`/chart-definition/${id}/share`, params)
}

/**
 * 获取图表分享列表
 */
export const getChartShares = (id: number) => {
  return request.get<ApiResponse<ChartShare[]>>(`/chart-definition/${id}/shares`)
}

/**
 * 获取图表嵌入URL
 */
export const getChartEmbedUrl = (id: number) => {
  const baseUrl = window.location.origin
  return `${baseUrl}/embed/chart/${id}`
}

/**
 * 更新图表移动端配置
 */
export const updateChartMobileEnabled = (id: number, mobileEnabled: number) => {
  return request.put<ApiResponse<void>>(`/chart-definition/${id}/mobile`, { mobileEnabled })
}

// ==================== AI 图表生成相关接口 ====================

/**
 * AI生成图表配置
 * 根据用户需求自动生成SQL、图表类型和ECharts配置
 */
export const aiGenerateChart = (params: {
  requirement: string
  dataSourceId?: number
  dbType?: string
  context?: {
    preferredChartType?: string
    colorTheme?: string
    dbType?: string
  }
}) => {
  return request.post<ApiResponse<{
    success: boolean
    content: string
    error?: string
    chartConfig?: {
      chartName: string
      chartCode: string
      chartType: string
      description: string
      sql: string
      chartConfig: Record<string, any>
      dataMapping: {
        xField?: string
        yField?: string | string[]
        nameField?: string
        valueField?: string
      }
    }
    parseable: boolean
  }>>('/ai/generate-chart', params)
}

/**
 * AI创建图表
 * 根据配置直接创建图表定义
 */
export const aiCreateChart = (params: {
  chartName: string
  chartCode?: string
  chartType: string
  description?: string
  dataSourceId: number
  sql: string
  chartConfig?: string | Record<string, any>
  queryParams?: Array<{
    name: string
    label: string
    type: string
    defaultValue?: string
  }>
}) => {
  return request.post<ApiResponse<{
    success: boolean
    chartId: number
    chartCode: string
    chartName: string
    message: string
    error?: string
  }>>('/ai/create-chart', params)
}

/**
 * AI一键生成并创建图表
 */
export const aiGenerateAndCreateChart = (params: {
  requirement: string
  dataSourceId: number
  options?: {
    preferredChartType?: string
    colorTheme?: string
  }
}) => {
  return request.post<ApiResponse<{
    success: boolean
    chartId: number
    chartCode: string
    chartName: string
    chartType: string
    sql: string
    aiResponse: string
    message: string
  }>>('/ai/generate-and-create-chart', params)
}

/**
 * AI优化图表样式
 */
export const aiOptimizeChartStyle = (params: {
  chartId: number
  stylePreference?: string
}) => {
  return request.post<ApiResponse<{
    success: boolean
    content: string
    optimizedConfig?: Record<string, any>
    parseable: boolean
  }>>('/ai/optimize-chart-style', params)
}
