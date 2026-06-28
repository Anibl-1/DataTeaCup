/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 动态参数选项加载器
 * 提供与 useParameterLinkage 集成的选项加载函数
 * 
 * 功能：
 * - 从数据库动态获取参数选项
 * - 支持SQL参数替换（${parentValue}）
 * - 缓存常用选项
 * - 错误处理和重试
 * - 参数默认值推荐
 * 
 * @validates 需求 13.2 - 从数据库动态获取参数选项值
 * @validates 需求 13.3 - 基于用户历史使用记录智能推荐参数默认值
 */
import { getParameterOptions as fetchParameterOptions } from '@/api/dynamicParameter'
import type { ParameterOption as ApiParameterOption, ParameterOptionsRequest } from '@/api/dynamicParameter'
import type { ParameterOption, CascadeConfig, OptionsLoader } from '@/composables/useParameterLinkage'
import type { ApiResponse } from '@/types/api'
import { logger } from '@/utils/logger'
import { parameterRecommendService } from './parameterRecommendService'

/**
 * 动态参数加载器配置
 */
export interface DynamicParameterLoaderConfig {
  /** 是否使用缓存（默认true） */
  useCache?: boolean
  /** 重试次数（默认1） */
  retryCount?: number
  /** 重试延迟（毫秒，默认500） */
  retryDelay?: number
  /** 请求超时（毫秒，默认10000） */
  timeout?: number
}

/**
 * 默认配置
 */
const DEFAULT_CONFIG: Required<DynamicParameterLoaderConfig> = {
  useCache: true,
  retryCount: 1,
  retryDelay: 500,
  timeout: 10000
}

/**
 * 将API返回的选项转换为useParameterLinkage使用的格式
 */
function convertOptions(apiOptions: ApiParameterOption[]): ParameterOption[] {
  return apiOptions.map(opt => ({
    label: opt.label,
    value: opt.value,
    disabled: opt.disabled,
    children: opt.children ? convertOptions(opt.children) : undefined
  }))
}

/**
 * 延迟函数
 */
function delay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms))
}

/**
 * 创建动态参数选项加载器
 * 返回一个可以直接传递给 useParameterLinkage 的 optionsLoader 函数
 * 
 * @param config 加载器配置
 * @returns OptionsLoader 函数
 * 
 * @example
 * ```typescript
 * const optionsLoader = createDynamicParameterLoader({ useCache: true })
 * 
 * const { registerParameter, addLinkage } = useParameterLinkage({
 *   optionsLoader
 * })
 * 
 * // 注册参数
 * registerParameter('province')
 * registerParameter('city')
 * 
 * // 添加级联配置
 * addLinkage({
 *   sourceParam: 'province',
 *   targetParam: 'city',
 *   linkageType: 'cascade',
 *   linkageConfig: {
 *     sourceField: 'province_code',
 *     targetField: 'city_code',
 *     dataSourceId: 1,
 *     sql: 'SELECT name AS label, code AS value FROM cities WHERE province_code = ${province}',
 *     labelField: 'label',
 *     valueField: 'value'
 *   }
 * })
 * ```
 */
export function createDynamicParameterLoader(
  config: DynamicParameterLoaderConfig = {}
): OptionsLoader {
  const mergedConfig = { ...DEFAULT_CONFIG, ...config }
  
  return async (
    paramName: string,
    dependencies: Record<string, any>,
    cascadeConfig: CascadeConfig
  ): Promise<ParameterOption[]> => {
    const { dataSourceId, sql, labelField, valueField } = cascadeConfig
    
    logger.debug(`加载参数选项: paramName=${paramName}, dataSourceId=${dataSourceId}`)
    logger.debug(`SQL: ${sql}`)
    logger.debug(`依赖参数:`, dependencies)
    
    // 构建请求
    const request: ParameterOptionsRequest = {
      dataSourceId,
      sql,
      dependencies,
      labelField,
      valueField,
      useCache: mergedConfig.useCache
    }
    
    // 带重试的请求
    let lastError: Error | null = null
    for (let attempt = 0; attempt <= mergedConfig.retryCount; attempt++) {
      try {
        if (attempt > 0) {
          logger.debug(`重试加载参数选项: paramName=${paramName}, attempt=${attempt}`)
          await delay(mergedConfig.retryDelay)
        }
        
        const response = await fetchParameterOptions(request)
        
        // 处理响应 - API返回的是 ApiResponse 结构
        // 但由于拦截器的处理，可能直接返回data数组
        const responseData = (response as unknown as ApiResponse<ApiParameterOption[]>)?.data ?? response
        
        if (responseData && Array.isArray(responseData)) {
          const options = convertOptions(responseData as ApiParameterOption[])
          logger.debug(`参数选项加载成功: paramName=${paramName}, count=${options.length}`)
          return options
        }
        
        return []
      } catch (error) {
        lastError = error instanceof Error ? error : new Error(String(error))
        logger.warn(`参数选项加载失败: paramName=${paramName}, attempt=${attempt}, error=${lastError.message}`)
      }
    }
    
    // 所有重试都失败
    logger.error(`参数选项加载最终失败: paramName=${paramName}`, lastError)
    throw lastError || new Error('加载参数选项失败')
  }
}

/**
 * 默认的动态参数选项加载器实例
 * 可以直接使用，无需创建新实例
 */
export const defaultDynamicParameterLoader = createDynamicParameterLoader()

/**
 * 创建带缓存的动态参数选项加载器
 * 在客户端额外缓存选项，减少API调用
 */
export function createCachedDynamicParameterLoader(
  config: DynamicParameterLoaderConfig = {}
): OptionsLoader {
  const baseLoader = createDynamicParameterLoader(config)
  const cache = new Map<string, { options: ParameterOption[]; timestamp: number }>()
  const CACHE_TTL = 5 * 60 * 1000 // 5分钟
  
  return async (
    paramName: string,
    dependencies: Record<string, any>,
    cascadeConfig: CascadeConfig
  ): Promise<ParameterOption[]> => {
    // 生成缓存键
    const cacheKey = generateCacheKey(paramName, dependencies, cascadeConfig)
    
    // 检查缓存
    const cached = cache.get(cacheKey)
    if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
      logger.debug(`从客户端缓存获取参数选项: paramName=${paramName}`)
      return cached.options
    }
    
    // 调用基础加载器
    const options = await baseLoader(paramName, dependencies, cascadeConfig)
    
    // 写入缓存
    cache.set(cacheKey, { options, timestamp: Date.now() })
    
    // 清理过期缓存
    cleanExpiredCache(cache, CACHE_TTL)
    
    return options
  }
}

/**
 * 生成缓存键
 */
function generateCacheKey(
  paramName: string,
  dependencies: Record<string, any>,
  cascadeConfig: CascadeConfig
): string {
  const parts = [
    paramName,
    cascadeConfig.dataSourceId,
    cascadeConfig.sql,
    JSON.stringify(dependencies)
  ]
  return parts.join('|')
}

/**
 * 清理过期缓存
 */
function cleanExpiredCache(
  cache: Map<string, { options: ParameterOption[]; timestamp: number }>,
  ttl: number
): void {
  const now = Date.now()
  for (const [key, value] of cache.entries()) {
    if (now - value.timestamp > ttl) {
      cache.delete(key)
    }
  }
}

/**
 * 创建模拟的动态参数选项加载器（用于测试）
 */
export function createMockDynamicParameterLoader(
  mockData: Record<string, ParameterOption[]>
): OptionsLoader {
  return async (
    paramName: string,
    _dependencies: Record<string, any>,
    _cascadeConfig: CascadeConfig
  ): Promise<ParameterOption[]> => {
    // 模拟网络延迟
    await delay(100)
    return mockData[paramName] || []
  }
}

/**
 * 辅助函数：创建级联配置
 * 简化级联配置的创建过程
 * 
 * @example
 * ```typescript
 * const cityConfig = createCascadeConfigForDynamic({
 *   dataSourceId: 1,
 *   sql: 'SELECT name AS label, code AS value FROM cities WHERE province_code = ${province}'
 * })
 * ```
 */
export function createCascadeConfigForDynamic(config: {
  dataSourceId: number
  sql: string
  sourceField?: string
  targetField?: string
  labelField?: string
  valueField?: string
}): CascadeConfig {
  return {
    sourceField: config.sourceField || 'parent_value',
    targetField: config.targetField || 'value',
    dataSourceId: config.dataSourceId,
    sql: config.sql,
    labelField: config.labelField || 'label',
    valueField: config.valueField || 'value'
  }
}

// ============================================================================
// 参数默认值推荐相关功能
// @validates 需求 13.3 - 基于用户历史使用记录智能推荐参数默认值
// ============================================================================

/**
 * 获取参数推荐默认值
 * 
 * @param paramName 参数名称
 * @param options 可选配置
 * @returns 推荐的默认值
 */
export async function getRecommendedDefault(
  paramName: string,
  options: {
    reportId?: number
    chartId?: number
  } = {}
): Promise<any | undefined> {
  return parameterRecommendService.getDefaultValue(paramName, options)
}

/**
 * 批量获取参数推荐默认值
 * 
 * @param paramNames 参数名称列表
 * @param options 可选配置
 * @returns 参数名到默认值的映射
 */
export async function getRecommendedDefaults(
  paramNames: string[],
  options: {
    reportId?: number
    chartId?: number
  } = {}
): Promise<Record<string, any>> {
  return parameterRecommendService.getDefaultValues(paramNames, options)
}

/**
 * 记录参数使用
 * 
 * @param paramName 参数名称
 * @param paramValue 参数值
 * @param options 可选配置
 */
export async function recordParameterUsage(
  paramName: string,
  paramValue: any,
  options: {
    reportId?: number
    chartId?: number
  } = {}
): Promise<void> {
  return parameterRecommendService.recordUsage(paramName, paramValue, options)
}

/**
 * 批量记录参数使用
 * 
 * @param paramValues 参数值映射
 * @param options 可选配置
 */
export async function recordParameterUsageBatch(
  paramValues: Record<string, any>,
  options: {
    reportId?: number
    chartId?: number
  } = {}
): Promise<void> {
  return parameterRecommendService.recordUsageBatch(paramValues, options)
}

/**
 * 应用推荐默认值到参数配置
 * 
 * @param params 参数配置数组
 * @param options 可选配置
 * @returns 应用了默认值的参数配置
 */
export async function applyRecommendedDefaults<T extends { name: string; defaultValue?: any }>(
  params: T[],
  options: {
    reportId?: number
    chartId?: number
    overwriteExisting?: boolean
  } = {}
): Promise<T[]> {
  return parameterRecommendService.applyRecommendedDefaults(params, options)
}

/**
 * 创建带推荐功能的动态参数加载器
 * 在加载选项的同时，自动获取推荐默认值
 * 
 * @param config 加载器配置
 * @returns 增强的选项加载器
 */
export function createDynamicParameterLoaderWithRecommend(
  config: DynamicParameterLoaderConfig & {
    reportId?: number
    chartId?: number
    onDefaultRecommended?: (paramName: string, defaultValue: any) => void
  } = {}
): OptionsLoader {
  const baseLoader = createDynamicParameterLoader(config)
  
  return async (
    paramName: string,
    dependencies: Record<string, any>,
    cascadeConfig: CascadeConfig
  ): Promise<ParameterOption[]> => {
    // 并行加载选项和推荐默认值
    const [options, recommendedDefault] = await Promise.all([
      baseLoader(paramName, dependencies, cascadeConfig),
      parameterRecommendService.getDefaultValue(paramName, {
        reportId: config.reportId,
        chartId: config.chartId
      })
    ])
    
    // 如果有推荐默认值，通知调用方
    if (recommendedDefault !== undefined && config.onDefaultRecommended) {
      config.onDefaultRecommended(paramName, recommendedDefault)
    }
    
    return options
  }
}

// 导出推荐服务实例，方便直接使用
export { parameterRecommendService }

