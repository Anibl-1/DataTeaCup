/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 参数默认值推荐服务
 * 提供参数默认值推荐和使用记录功能
 * 
 * 功能：
 * - 获取参数默认值推荐
 * - 记录参数使用历史
 * - 自动应用推荐默认值
 * - 缓存推荐结果
 * 
 * @validates 需求 13.3 - 基于用户历史使用记录智能推荐参数默认值
 */
import {
  getRecommendations,
  getDefaultValue,
  getDefaultValues,
  recordUsage,
  clearUsageHistory,
  type RecommendRequest,
  type RecommendResult,
  type RecommendStrategy,
  type RecordUsageRequest
} from '@/api/dynamicParameter'
import { logger } from '@/utils/logger'

/**
 * 推荐服务配置
 */
export interface RecommendServiceConfig {
  /** 是否启用客户端缓存（默认true） */
  useCache?: boolean
  /** 缓存TTL（毫秒，默认5分钟） */
  cacheTTL?: number
  /** 默认推荐策略 */
  defaultStrategy?: RecommendStrategy
  /** 默认推荐数量 */
  defaultLimit?: number
  /** 是否自动记录使用（默认true） */
  autoRecord?: boolean
}

/**
 * 默认配置
 */
const DEFAULT_CONFIG: Required<RecommendServiceConfig> = {
  useCache: true,
  cacheTTL: 5 * 60 * 1000, // 5分钟
  defaultStrategy: 'PREFERENCE',
  defaultLimit: 5,
  autoRecord: true
}

/**
 * 缓存条目
 */
interface CacheEntry<T> {
  data: T
  timestamp: number
}

/**
 * 参数默认值推荐服务类
 */
export class ParameterRecommendServiceClass {
  private config: Required<RecommendServiceConfig>
  private cache: Map<string, CacheEntry<any>>
  
  constructor(config: RecommendServiceConfig = {}) {
    this.config = { ...DEFAULT_CONFIG, ...config }
    this.cache = new Map()
  }
  
  /**
   * 获取参数推荐列表
   * 
   * @param paramName 参数名称
   * @param options 可选配置
   * @returns 推荐结果列表
   */
  async getRecommendations(
    paramName: string,
    options: {
      reportId?: number
      chartId?: number
      strategy?: RecommendStrategy
      limit?: number
      includeGlobal?: boolean
      useCache?: boolean
    } = {}
  ): Promise<RecommendResult[]> {
    const cacheKey = this.generateCacheKey('recommendations', paramName, options)
    const useCache = options.useCache ?? this.config.useCache
    
    // 检查缓存
    if (useCache) {
      const cached = this.getFromCache<RecommendResult[]>(cacheKey)
      if (cached) {
        logger.debug(`从缓存获取参数推荐: ${paramName}`)
        return cached
      }
    }
    
    try {
      const request: RecommendRequest = {
        paramName,
        reportId: options.reportId,
        chartId: options.chartId,
        strategy: options.strategy ?? this.config.defaultStrategy,
        limit: options.limit ?? this.config.defaultLimit,
        includeGlobal: options.includeGlobal ?? true
      }
      
      const response = await getRecommendations(request)
      const results = (response as any)?.data ?? response ?? []
      
      // 写入缓存
      if (useCache && results.length > 0) {
        this.setCache(cacheKey, results)
      }
      
      logger.debug(`获取参数推荐成功: ${paramName}, count=${results.length}`)
      return results
    } catch (error) {
      logger.error(`获取参数推荐失败: ${paramName}`, error)
      return []
    }
  }
  
  /**
   * 获取单个参数的推荐默认值
   * 
   * @param paramName 参数名称
   * @param options 可选配置
   * @returns 推荐的默认值，如果没有推荐则返回undefined
   */
  async getDefaultValue(
    paramName: string,
    options: {
      reportId?: number
      chartId?: number
      useCache?: boolean
    } = {}
  ): Promise<any | undefined> {
    const cacheKey = this.generateCacheKey('default', paramName, options)
    const useCache = options.useCache ?? this.config.useCache
    
    // 检查缓存
    if (useCache) {
      const cached = this.getFromCache<any>(cacheKey)
      if (cached !== undefined) {
        logger.debug(`从缓存获取参数默认值: ${paramName}`)
        return cached
      }
    }
    
    try {
      const response = await getDefaultValue(paramName, options.reportId, options.chartId)
      const value = (response as any)?.data ?? response
      
      // 写入缓存（即使是null也缓存，避免重复请求）
      if (useCache) {
        this.setCache(cacheKey, value)
      }
      
      logger.debug(`获取参数默认值成功: ${paramName}, value=${value}`)
      return value
    } catch (error) {
      logger.error(`获取参数默认值失败: ${paramName}`, error)
      return undefined
    }
  }
  
  /**
   * 批量获取参数默认值
   * 
   * @param paramNames 参数名称列表
   * @param options 可选配置
   * @returns 参数名到默认值的映射
   */
  async getDefaultValues(
    paramNames: string[],
    options: {
      reportId?: number
      chartId?: number
      useCache?: boolean
    } = {}
  ): Promise<Record<string, any>> {
    if (paramNames.length === 0) {
      return {}
    }
    
    const useCache = options.useCache ?? this.config.useCache
    const result: Record<string, any> = {}
    const uncachedParams: string[] = []
    
    // 先从缓存获取
    if (useCache) {
      for (const paramName of paramNames) {
        const cacheKey = this.generateCacheKey('default', paramName, options)
        const cached = this.getFromCache<any>(cacheKey)
        if (cached !== undefined) {
          result[paramName] = cached
        } else {
          uncachedParams.push(paramName)
        }
      }
    } else {
      uncachedParams.push(...paramNames)
    }
    
    // 批量获取未缓存的参数
    if (uncachedParams.length > 0) {
      try {
        const response = await getDefaultValues({
          paramNames: uncachedParams,
          reportId: options.reportId,
          chartId: options.chartId
        })
        const values = (response as any)?.data ?? response ?? {}
        
        // 合并结果并写入缓存
        for (const [paramName, value] of Object.entries(values)) {
          result[paramName] = value
          if (useCache) {
            const cacheKey = this.generateCacheKey('default', paramName, options)
            this.setCache(cacheKey, value)
          }
        }
        
        logger.debug(`批量获取参数默认值成功: count=${Object.keys(values).length}`)
      } catch (error) {
        logger.error('批量获取参数默认值失败', error)
      }
    }
    
    return result
  }
  
  /**
   * 记录参数使用
   * 
   * @param paramName 参数名称
   * @param paramValue 参数值
   * @param options 可选配置
   */
  async recordUsage(
    paramName: string,
    paramValue: any,
    options: {
      reportId?: number
      chartId?: number
    } = {}
  ): Promise<void> {
    if (!this.config.autoRecord) {
      return
    }
    
    try {
      await recordUsage({
        paramName,
        paramValue,
        reportId: options.reportId,
        chartId: options.chartId
      })
      
      // 清除相关缓存
      this.invalidateCache(paramName, options)
      
      logger.debug(`记录参数使用成功: ${paramName}`)
    } catch (error) {
      // 记录失败不影响主流程
      logger.warn(`记录参数使用失败: ${paramName}`, error)
    }
  }
  
  /**
   * 批量记录参数使用
   * 
   * @param paramValues 参数值映射
   * @param options 可选配置
   */
  async recordUsageBatch(
    paramValues: Record<string, any>,
    options: {
      reportId?: number
      chartId?: number
    } = {}
  ): Promise<void> {
    if (!this.config.autoRecord || Object.keys(paramValues).length === 0) {
      return
    }
    
    try {
      await recordUsage({
        paramValues,
        reportId: options.reportId,
        chartId: options.chartId
      })
      
      // 清除相关缓存
      for (const paramName of Object.keys(paramValues)) {
        this.invalidateCache(paramName, options)
      }
      
      logger.debug(`批量记录参数使用成功: count=${Object.keys(paramValues).length}`)
    } catch (error) {
      logger.warn('批量记录参数使用失败', error)
    }
  }
  
  /**
   * 清除使用历史
   * 
   * @param paramName 参数名称（可选，为空则清除所有）
   */
  async clearHistory(paramName?: string): Promise<void> {
    try {
      await clearUsageHistory(paramName)
      
      // 清除所有缓存
      this.clearCache()
      
      logger.info(`清除参数使用历史成功: ${paramName ?? '全部'}`)
    } catch (error) {
      logger.error('清除参数使用历史失败', error)
      throw error
    }
  }
  
  /**
   * 应用推荐默认值到参数对象
   * 
   * @param params 参数配置数组
   * @param options 可选配置
   * @returns 应用了默认值的参数配置
   */
  async applyRecommendedDefaults<T extends { name: string; defaultValue?: any }>(
    params: T[],
    options: {
      reportId?: number
      chartId?: number
      overwriteExisting?: boolean
    } = {}
  ): Promise<T[]> {
    const paramNames = params
      .filter(p => options.overwriteExisting || p.defaultValue === undefined || p.defaultValue === null)
      .map(p => p.name)
    
    if (paramNames.length === 0) {
      return params
    }
    
    const defaults = await this.getDefaultValues(paramNames, options)
    
    return params.map(param => {
      if (defaults[param.name] !== undefined) {
        if (options.overwriteExisting || param.defaultValue === undefined || param.defaultValue === null) {
          return { ...param, defaultValue: defaults[param.name] }
        }
      }
      return param
    })
  }
  
  /**
   * 生成缓存键
   */
  private generateCacheKey(
    type: string,
    paramName: string,
    options: { reportId?: number; chartId?: number }
  ): string {
    const parts = [type, paramName]
    if (options.reportId) parts.push(`r${options.reportId}`)
    if (options.chartId) parts.push(`c${options.chartId}`)
    return parts.join(':')
  }
  
  /**
   * 从缓存获取
   */
  private getFromCache<T>(key: string): T | undefined {
    const entry = this.cache.get(key)
    if (!entry) return undefined
    
    // 检查是否过期
    if (Date.now() - entry.timestamp > this.config.cacheTTL) {
      this.cache.delete(key)
      return undefined
    }
    
    return entry.data as T
  }
  
  /**
   * 写入缓存
   */
  private setCache(key: string, data: any): void {
    this.cache.set(key, {
      data,
      timestamp: Date.now()
    })
  }
  
  /**
   * 使缓存失效
   */
  private invalidateCache(
    paramName: string,
    options: { reportId?: number; chartId?: number }
  ): void {
    // 删除相关的缓存条目
    const prefixes = [
      this.generateCacheKey('recommendations', paramName, options),
      this.generateCacheKey('default', paramName, options),
      this.generateCacheKey('recommendations', paramName, {}),
      this.generateCacheKey('default', paramName, {})
    ]
    
    for (const prefix of prefixes) {
      this.cache.delete(prefix)
    }
  }
  
  /**
   * 清除所有缓存
   */
  clearCache(): void {
    this.cache.clear()
  }
  
  /**
   * 更新配置
   */
  updateConfig(config: Partial<RecommendServiceConfig>): void {
    this.config = { ...this.config, ...config }
  }
}

/**
 * 默认的参数推荐服务实例
 */
export const parameterRecommendService = new ParameterRecommendServiceClass()

/**
 * 创建自定义配置的参数推荐服务
 */
export function createParameterRecommendService(
  config: RecommendServiceConfig = {}
): ParameterRecommendServiceClass {
  return new ParameterRecommendServiceClass(config)
}

// 导出类型
export type { RecommendResult, RecommendStrategy, RecommendRequest, RecordUsageRequest }
