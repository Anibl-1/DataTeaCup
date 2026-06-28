/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 动态参数 API
 * 支持从数据库动态获取参数选项
 * 
 * @validates 需求 13.2 - 从数据库动态获取参数选项值
 * @validates 需求 13.3 - 基于用户历史使用记录智能推荐参数默认值
 * @validates 需求 13.4 - 实现前后端双重参数校验
 */
import httpRequest from './request'
import type { 
  ParameterValidationConfig, 
  ValidationResult, 
  BatchValidationResult 
} from '@/services/parameterValidationService'

/**
 * 参数选项
 */
export interface ParameterOption {
  /** 显示标签 */
  label: string
  /** 选项值 */
  value: string
  /** 是否禁用 */
  disabled?: boolean
  /** 子选项（用于级联选择器） */
  children?: ParameterOption[]
}

/**
 * 参数选项请求
 */
export interface ParameterOptionsRequest {
  /** 数据源ID */
  dataSourceId: number
  /** SQL查询语句，支持 ${paramName} 占位符 */
  sql: string
  /** 依赖参数值映射 */
  dependencies?: Record<string, any>
  /** 标签字段名（可选，默认自动检测） */
  labelField?: string
  /** 值字段名（可选，默认自动检测） */
  valueField?: string
  /** 是否使用缓存（默认true） */
  useCache?: boolean
}

/**
 * 缓存统计信息
 */
export interface CacheStats {
  /** 命中次数 */
  hitCount: number
  /** 未命中次数 */
  missCount: number
  /** 命中率 */
  hitRate: number
  /** 淘汰次数 */
  evictionCount: number
  /** 缓存大小 */
  size: number
}

// ============================================================================
// 参数默认值推荐相关类型
// @validates 需求 13.3 - 基于用户历史使用记录智能推荐参数默认值
// ============================================================================

/**
 * 推荐策略
 */
export type RecommendStrategy = 'FREQUENCY' | 'RECENT' | 'PREFERENCE'

/**
 * 推荐结果
 */
export interface RecommendResult {
  /** 推荐值 */
  value: any
  /** 推荐分数（0-1） */
  score: number
  /** 使用次数 */
  usageCount: number
  /** 最后使用时间 */
  lastUsedAt: string
  /** 推荐原因 */
  reason: string
}

/**
 * 推荐请求
 */
export interface RecommendRequest {
  /** 参数名称 */
  paramName: string
  /** 报表ID（可选） */
  reportId?: number
  /** 图表ID（可选） */
  chartId?: number
  /** 推荐策略 */
  strategy?: RecommendStrategy
  /** 推荐数量 */
  limit?: number
  /** 是否包含全局推荐 */
  includeGlobal?: boolean
}

/**
 * 批量获取默认值请求
 */
export interface BatchDefaultsRequest {
  /** 参数名称列表 */
  paramNames: string[]
  /** 报表ID（可选） */
  reportId?: number
  /** 图表ID（可选） */
  chartId?: number
}

/**
 * 记录使用请求
 */
export interface RecordUsageRequest {
  /** 参数名称（单个记录时使用） */
  paramName?: string
  /** 参数值（单个记录时使用） */
  paramValue?: any
  /** 参数值映射（批量记录时使用） */
  paramValues?: Record<string, any>
  /** 报表ID（可选） */
  reportId?: number
  /** 图表ID（可选） */
  chartId?: number
}

/**
 * 获取参数选项
 * 执行SQL查询获取参数选项，支持参数替换
 * 
 * @param req 参数选项请求
 * @returns 参数选项列表
 */
export function getParameterOptions(req: ParameterOptionsRequest) {
  return httpRequest.post<ParameterOption[]>('/dynamic-parameter/options', req)
}

/**
 * 预览SQL参数替换结果
 * 用于调试SQL参数替换
 * 
 * @param req 参数选项请求
 * @returns 替换后的SQL
 */
export function previewSql(req: ParameterOptionsRequest) {
  return httpRequest.post<string>('/dynamic-parameter/preview-sql', req)
}

/**
 * 清除指定数据源的缓存
 * 
 * @param dataSourceId 数据源ID
 */
export function clearCache(dataSourceId: number) {
  return httpRequest.delete<void>(`/dynamic-parameter/cache/${dataSourceId}`)
}

/**
 * 清除所有缓存
 */
export function clearAllCache() {
  return httpRequest.delete<void>('/dynamic-parameter/cache')
}

/**
 * 获取缓存统计信息
 * 
 * @returns 缓存统计信息
 */
export function getCacheStats() {
  return httpRequest.get<CacheStats>('/dynamic-parameter/cache/stats')
}

// ============================================================================
// 参数默认值推荐 API
// @validates 需求 13.3 - 基于用户历史使用记录智能推荐参数默认值
// ============================================================================

/**
 * 获取参数默认值推荐
 * 基于用户历史使用记录推荐参数默认值
 * 
 * @param req 推荐请求
 * @returns 推荐结果列表
 */
export function getRecommendations(req: RecommendRequest) {
  return httpRequest.post<RecommendResult[]>('/dynamic-parameter/recommend', req)
}

/**
 * 获取单个参数的推荐默认值
 * 
 * @param paramName 参数名称
 * @param reportId 报表ID（可选）
 * @param chartId 图表ID（可选）
 * @returns 推荐的默认值
 */
export function getDefaultValue(paramName: string, reportId?: number, chartId?: number) {
  const params: Record<string, any> = { paramName }
  if (reportId !== undefined) params.reportId = reportId
  if (chartId !== undefined) params.chartId = chartId
  return httpRequest.get<any>('/dynamic-parameter/recommend/default', { params })
}

/**
 * 批量获取参数默认值推荐
 * 
 * @param req 批量请求
 * @returns 参数名到推荐值的映射
 */
export function getDefaultValues(req: BatchDefaultsRequest) {
  return httpRequest.post<Record<string, any>>('/dynamic-parameter/recommend/defaults', req)
}

/**
 * 记录参数使用
 * 记录用户的参数使用历史，用于后续推荐
 * 
 * @param req 记录请求
 */
export function recordUsage(req: RecordUsageRequest) {
  return httpRequest.post<void>('/dynamic-parameter/usage/record', req)
}

/**
 * 清除参数使用历史
 * 
 * @param paramName 参数名称（可选，为空则清除所有）
 */
export function clearUsageHistory(paramName?: string) {
  const params: Record<string, any> = {}
  if (paramName) params.paramName = paramName
  return httpRequest.delete<void>('/dynamic-parameter/usage/history', { params })
}

// ============================================================================
// 参数校验 API
// @validates 需求 13.4 - 实现前后端双重参数校验
// ============================================================================

/**
 * 校验单个参数请求
 */
export interface ValidateParameterRequest {
  /** 参数值 */
  value: any
  /** 校验配置 */
  config: ParameterValidationConfig
}

/**
 * 批量校验参数请求
 */
export interface ValidateParametersRequest {
  /** 参数值映射 */
  values: Record<string, any>
  /** 校验配置列表 */
  configs: ParameterValidationConfig[]
}

/**
 * 校验单个参数（后端校验）
 * 
 * @param req 校验请求
 * @returns 校验结果
 */
export function validateParameter(req: ValidateParameterRequest) {
  return httpRequest.post<ValidationResult>('/dynamic-parameter/validate', req)
}

/**
 * 批量校验参数（后端校验）
 * 
 * @param req 批量校验请求
 * @returns 批量校验结果
 */
export function validateParameters(req: ValidateParametersRequest) {
  return httpRequest.post<BatchValidationResult>('/dynamic-parameter/validate/batch', req)
}

/**
 * 获取内置校验器列表
 * 
 * @returns 校验器名称列表
 */
export function getBuiltInValidators() {
  return httpRequest.get<string[]>('/dynamic-parameter/validate/validators')
}

// 重新导出校验服务类型
export type { 
  ParameterValidationConfig, 
  ValidationResult, 
  BatchValidationResult,
  ValidationRule,
  ValidationError
} from '@/services/parameterValidationService'
