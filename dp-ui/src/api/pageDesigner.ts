/* eslint-disable @typescript-eslint/no-explicit-any */
import request from './request'
import type { ApiResponse } from '@/types/api'
import type { InlineChartConfig } from '@/types/page'

/**
 * 测试内联图表SQL并获取数据预览
 */
export const testInlineChartSql = (params: {
  dataSourceId: number
  sqlContent: string
  limit?: number
}) => {
  return request.post<ApiResponse<any[]>>('/chart-definition/test-sql', params)
}

/**
 * AI 为内联图表生成配置
 */
export const aiGenerateInlineChart = (params: {
  requirement: string
  dataSourceId?: number
  context?: {
    preferredChartType?: string
    colorTheme?: string
    tables?: string[]
  }
}) => {
  return request.post<ApiResponse<{
    success: boolean
    content: string
    error?: string
    chartConfig?: {
      chartName: string
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
 * AI 优化内联图表样式
 */
export const aiOptimizeInlineStyle = (params: {
  chartConfig: string
  chartType: string
  stylePreference?: string
}) => {
  return request.post<ApiResponse<{
    success: boolean
    content: string
    optimizedConfig?: Record<string, any>
    parseable: boolean
  }>>('/ai/optimize-chart-style', params)
}

/**
 * AI 分析数据源推荐图表
 */
export const aiAnalyzeDataSource = (params: {
  dataSourceId: number
  tableName?: string
  requirement?: string
}) => {
  return request.post<ApiResponse<{
    success: boolean
    content: string
    recommendations?: Array<{
      chartType: string
      chartName: string
      sql: string
      description: string
    }>
  }>>('/ai/generate-chart', {
    requirement: params.requirement || `分析数据源中的数据，推荐适合的可视化方案`,
    dataSourceId: params.dataSourceId
  })
}

/**
 * 获取页面模板列表
 */
export const getPageTemplates = () => {
  return request.get<ApiResponse<any[]>>('/page-definition/templates')
}

/**
 * 基于模板创建页面
 */
export const createPageFromTemplate = (templateId: string, pageName: string) => {
  return request.post<ApiResponse<any>>('/page-definition/from-template', { templateId, pageName })
}

/**
 * 获取页面版本历史
 */
export const getPageVersions = (pageId: number) => {
  return request.get<ApiResponse<any[]>>(`/page-definition/${pageId}/versions`)
}

/**
 * 恢复页面版本
 */
export const restorePageVersion = (pageId: number, versionId: number) => {
  return request.post<ApiResponse<void>>(`/page-definition/${pageId}/versions/${versionId}/restore`)
}

/**
 * 将内联图表转换为公共图表
 */
export const convertInlineToPublic = (params: {
  inlineConfig: InlineChartConfig
  chartCode?: string
}) => {
  return request.post<ApiResponse<{
    chartId: number
    chartCode: string
  }>>('/chart-definition', {
    chartName: params.inlineConfig.chartName,
    chartCode: params.chartCode || `inline_${Date.now()}`,
    chartType: params.inlineConfig.chartType,
    dataSourceId: params.inlineConfig.dataSourceId,
    sqlContent: params.inlineConfig.sqlContent,
    chartConfig: params.inlineConfig.chartConfig,
    description: params.inlineConfig.description,
    status: 1
  })
}
