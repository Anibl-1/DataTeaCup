/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 图表联动引擎
 * 用于管理图表之间的联动和交叉筛选
 */

import type { ECharts } from '@/utils/echarts'
import type { ChartLinkConfig } from '@/types/dashboard'
import request from '@/api/request'
import type { ApiResponse } from '@/types/api'

/**
 * 扩展的图表联动配置（包含ID）
 */
export interface ChartLinkConfigWithId extends ChartLinkConfig {
  /** 配置唯一标识 */
  id: string
  /** 联动类型 */
  linkType: 'filter' | 'drillDown' | 'highlight'
}

/**
 * 图表点击事件参数
 */
export interface ChartClickParams {
  /** 维度值 */
  dimensionValue: any
  /** 系列名称 */
  seriesName?: string
  /** 数据索引 */
  dataIndex?: number
  /** 原始数据 */
  data?: any
}

/**
 * 联动事件类型
 */
export type LinkEventType = 'link-update' | 'link-highlight' | 'link-drilldown' | 'link-reset'

/**
 * 联动事件监听器
 */
export type LinkEventListener = (event: {
  type: LinkEventType
  sourceChartId: string
  targetChartIds: string[]
  dimensionValue: any
  filters: Record<string, any>
}) => void

/**
 * 缓存条目
 */
interface CacheEntry {
  data: Map<string, any[]>
  timestamp: number
}

/**
 * 图表联动管理器接口
 */
export interface ChartLinker {
  /** 注册图表 */
  registerChart: (chartId: string, chartInstance: ECharts) => void
  /** 注销图表 */
  unregisterChart: (chartId: string) => void
  /** 设置联动配置 */
  setLinkConfig: (configs: ChartLinkConfigWithId[]) => void
  /** 获取联动配置 */
  getLinkConfig: () => ChartLinkConfigWithId[]
  /** 处理图表点击事件 */
  handleChartClick: (sourceChartId: string, params: ChartClickParams) => Promise<void>
  /** 重置所有联动 */
  resetAllLinks: () => void
  /** 添加事件监听器 */
  addEventListener: (listener: LinkEventListener) => void
  /** 移除事件监听器 */
  removeEventListener: (listener: LinkEventListener) => void
  /** 获取已注册的图表ID列表 */
  getRegisteredCharts: () => string[]
  /** 获取指定图表的联动目标 */
  getLinkedTargets: (sourceChartId: string) => string[]
  /** 清除缓存 */
  clearCache: () => void
  /** 销毁实例 */
  destroy: () => void
}

/**
 * 联动数据请求参数
 */
export interface LinkedDataRequest {
  sourceChartId: number
  dimensionValue: string
  targetChartIds: number[]
}

/**
 * 获取联动图表数据的API
 * 从后端获取根据源图表筛选条件过滤后的目标图表数据
 */
export async function fetchLinkedChartData(
  sourceChartId: string,
  dimensionValue: any,
  targetChartIds: string[]
): Promise<Map<string, any[]>> {
  const result = new Map<string, any[]>()
  
  if (!targetChartIds || targetChartIds.length === 0) {
    return result
  }

  try {
    // 将字符串ID转换为数字ID（如果需要）
    const numericTargetIds = targetChartIds
      .map(id => {
        const numId = parseInt(id, 10)
        return isNaN(numId) ? null : numId
      })
      .filter((id): id is number => id !== null)

    if (numericTargetIds.length === 0) {
      return result
    }

    const sourceNumericId = parseInt(sourceChartId, 10)
    if (isNaN(sourceNumericId)) {
      return result
    }

    const response = await request.post<ApiResponse<Record<string, any[]>>>(
      '/chart-link/linked-data',
      {
        sourceChartId: sourceNumericId,
        dimensionValue: String(dimensionValue),
        targetChartIds: numericTargetIds
      }
    )

    const data = response.data?.data || response.data
    if (data && typeof data === 'object') {
      Object.entries(data).forEach(([chartId, chartData]) => {
        if (Array.isArray(chartData)) {
          result.set(chartId, chartData)
        }
      })
    }
  } catch (error) {
    console.error('Failed to fetch linked chart data:', error)
  }

  return result
}

/**
 * 保存图表联动配置
 */
export async function saveLinkConfig(
  dashboardId: number,
  configs: ChartLinkConfigWithId[]
): Promise<void> {
  await request.post(`/chart-link/config/${dashboardId}`, configs)
}

/**
 * 获取图表联动配置
 */
export async function getLinkConfigFromServer(
  dashboardId: number
): Promise<ChartLinkConfigWithId[]> {
  const response = await request.get<ApiResponse<ChartLinkConfigWithId[]>>(
    `/chart-link/config/${dashboardId}`
  )
  const responseData = response.data as any
  if (responseData && typeof responseData === 'object' && 'data' in responseData) {
    return responseData.data || []
  }
  return Array.isArray(responseData) ? responseData : []
}

/**
 * 创建图表联动管理器
 */
export function createChartLinker(): ChartLinker {
  // 已注册的图表实例
  const chartInstances = new Map<string, ECharts>()
  
  // 联动配置列表
  let linkConfigs: ChartLinkConfigWithId[] = []
  
  // 事件监听器列表
  const eventListeners: Set<LinkEventListener> = new Set()
  
  // 数据缓存（key: sourceChartId-dimensionValue）
  const dataCache = new Map<string, CacheEntry>()
  
  // 缓存过期时间（5分钟）
  const CACHE_TTL = 5 * 60 * 1000
  
  /**
   * 生成缓存键
   */
  const getCacheKey = (sourceChartId: string, dimensionValue: any): string => {
    return `${sourceChartId}-${String(dimensionValue)}`
  }
  
  /**
   * 检查缓存是否有效
   */
  const isCacheValid = (entry: CacheEntry | undefined): boolean => {
    if (!entry) return false
    return Date.now() - entry.timestamp < CACHE_TTL
  }
  
  /**
   * 触发事件
   */
  const emitEvent = (event: Parameters<LinkEventListener>[0]) => {
    eventListeners.forEach(listener => {
      try {
        listener(event)
      } catch (error) {
        console.error('Error in link event listener:', error)
      }
    })
  }
  
  /**
   * 根据源图表ID获取所有联动配置
   */
  const getConfigsBySource = (sourceChartId: string): ChartLinkConfigWithId[] => {
    return linkConfigs.filter(config => config.sourceChartId === sourceChartId)
  }
  
  /**
   * 应用高亮效果到目标图表
   */
  const applyHighlight = (
    targetChartId: string,
    dimensionValue: any,
    _targetField: string
  ) => {
    const chartInstance = chartInstances.get(targetChartId)
    if (!chartInstance) return
    
    // 先取消之前的高亮
    chartInstance.dispatchAction({
      type: 'downplay'
    })
    
    // 应用新的高亮
    chartInstance.dispatchAction({
      type: 'highlight',
      name: String(dimensionValue)
    })
    
    // 显示tooltip
    chartInstance.dispatchAction({
      type: 'showTip',
      name: String(dimensionValue)
    })
  }
  
  /**
   * 重置图表高亮
   */
  const resetHighlight = (chartId: string) => {
    const chartInstance = chartInstances.get(chartId)
    if (!chartInstance) return
    
    chartInstance.dispatchAction({
      type: 'downplay'
    })
    
    chartInstance.dispatchAction({
      type: 'hideTip'
    })
  }
  
  /**
   * 注册图表
   */
  const registerChart = (chartId: string, chartInstance: ECharts) => {
    chartInstances.set(chartId, chartInstance)
  }
  
  /**
   * 注销图表
   */
  const unregisterChart = (chartId: string) => {
    chartInstances.delete(chartId)
  }
  
  /**
   * 设置联动配置
   */
  const setLinkConfig = (configs: ChartLinkConfigWithId[]) => {
    linkConfigs = configs || []
  }
  
  /**
   * 获取联动配置
   */
  const getLinkConfig = (): ChartLinkConfigWithId[] => {
    return [...linkConfigs]
  }
  
  /**
   * 处理图表点击事件
   */
  const handleChartClick = async (
    sourceChartId: string,
    params: ChartClickParams
  ): Promise<void> => {
    const { dimensionValue } = params
    
    if (dimensionValue === null || dimensionValue === undefined) {
      return
    }
    
    // 获取该源图表的所有联动配置
    const configs = getConfigsBySource(sourceChartId)
    
    if (configs.length === 0) {
      return
    }
    
    // 按联动类型分组
    const filterConfigs = configs.filter(c => c.linkType === 'filter')
    const highlightConfigs = configs.filter(c => c.linkType === 'highlight')
    const drillDownConfigs = configs.filter(c => c.linkType === 'drillDown')
    
    // 构建筛选条件
    const filters: Record<string, any> = {}
    configs.forEach(config => {
      filters[config.targetField] = dimensionValue
    })
    
    // 处理高亮类型联动
    if (highlightConfigs.length > 0) {
      const highlightTargetIds = highlightConfigs.map(c => c.targetChartId)
      
      highlightConfigs.forEach(config => {
        applyHighlight(config.targetChartId, dimensionValue, config.targetField)
      })
      
      emitEvent({
        type: 'link-highlight',
        sourceChartId,
        targetChartIds: highlightTargetIds,
        dimensionValue,
        filters
      })
    }
    
    // 处理筛选类型联动
    if (filterConfigs.length > 0) {
      const filterTargetIds = filterConfigs.map(c => c.targetChartId)
      
      // 检查缓存
      const cacheKey = getCacheKey(sourceChartId, dimensionValue)
      const cachedEntry = dataCache.get(cacheKey)
      
      let linkedData: Map<string, any[]>
      
      if (isCacheValid(cachedEntry)) {
        linkedData = cachedEntry!.data
      } else {
        // 从后端获取联动数据
        linkedData = await fetchLinkedChartData(
          sourceChartId,
          dimensionValue,
          filterTargetIds
        )
        
        // 更新缓存
        dataCache.set(cacheKey, {
          data: linkedData,
          timestamp: Date.now()
        })
      }
      
      emitEvent({
        type: 'link-update',
        sourceChartId,
        targetChartIds: filterTargetIds,
        dimensionValue,
        filters
      })
    }
    
    // 处理下钻类型联动
    if (drillDownConfigs.length > 0) {
      const drillDownTargetIds = drillDownConfigs.map(c => c.targetChartId)
      
      emitEvent({
        type: 'link-drilldown',
        sourceChartId,
        targetChartIds: drillDownTargetIds,
        dimensionValue,
        filters
      })
    }
  }
  
  /**
   * 重置所有联动
   */
  const resetAllLinks = () => {
    // 重置所有图表的高亮状态
    chartInstances.forEach((_, chartId) => {
      resetHighlight(chartId)
    })
    
    // 触发重置事件
    emitEvent({
      type: 'link-reset',
      sourceChartId: '',
      targetChartIds: Array.from(chartInstances.keys()),
      dimensionValue: null,
      filters: {}
    })
  }
  
  /**
   * 添加事件监听器
   */
  const addEventListener = (listener: LinkEventListener) => {
    eventListeners.add(listener)
  }
  
  /**
   * 移除事件监听器
   */
  const removeEventListener = (listener: LinkEventListener) => {
    eventListeners.delete(listener)
  }
  
  /**
   * 获取已注册的图表ID列表
   */
  const getRegisteredCharts = (): string[] => {
    return Array.from(chartInstances.keys())
  }
  
  /**
   * 获取指定图表的联动目标
   */
  const getLinkedTargets = (sourceChartId: string): string[] => {
    const configs = getConfigsBySource(sourceChartId)
    return [...new Set(configs.map(c => c.targetChartId))]
  }
  
  /**
   * 清除缓存
   */
  const clearCache = () => {
    dataCache.clear()
  }
  
  /**
   * 销毁实例
   */
  const destroy = () => {
    chartInstances.clear()
    linkConfigs = []
    eventListeners.clear()
    dataCache.clear()
  }
  
  return {
    registerChart,
    unregisterChart,
    setLinkConfig,
    getLinkConfig,
    handleChartClick,
    resetAllLinks,
    addEventListener,
    removeEventListener,
    getRegisteredCharts,
    getLinkedTargets,
    clearCache,
    destroy
  }
}

/**
 * 根据联动配置查找目标图表
 */
export function findLinkedCharts(
  sourceChartId: string,
  configs: ChartLinkConfigWithId[]
): ChartLinkConfigWithId[] {
  return configs.filter(config => config.sourceChartId === sourceChartId)
}

/**
 * 检查两个图表是否存在联动关系
 */
export function hasLinkRelation(
  sourceChartId: string,
  targetChartId: string,
  configs: ChartLinkConfigWithId[]
): boolean {
  return configs.some(
    config => 
      config.sourceChartId === sourceChartId && 
      config.targetChartId === targetChartId
  )
}

/**
 * 获取图表的所有上游联动源
 */
export function getUpstreamSources(
  targetChartId: string,
  configs: ChartLinkConfigWithId[]
): string[] {
  const sources = configs
    .filter(config => config.targetChartId === targetChartId)
    .map(config => config.sourceChartId)
  
  return [...new Set(sources)]
}

/**
 * 验证联动配置的有效性
 */
export function validateLinkConfig(config: ChartLinkConfigWithId): string[] {
  const errors: string[] = []
  
  if (!config.id) {
    errors.push('配置ID不能为空')
  }
  
  if (!config.sourceChartId) {
    errors.push('源图表ID不能为空')
  }
  
  if (!config.targetChartId) {
    errors.push('目标图表ID不能为空')
  }
  
  if (config.sourceChartId === config.targetChartId) {
    errors.push('源图表和目标图表不能相同')
  }
  
  if (!config.sourceField) {
    errors.push('源字段不能为空')
  }
  
  if (!config.targetField) {
    errors.push('目标字段不能为空')
  }
  
  if (!['filter', 'drillDown', 'highlight'].includes(config.linkType)) {
    errors.push('联动类型无效，必须是 filter、drillDown 或 highlight')
  }
  
  return errors
}

/**
 * 检测联动配置中是否存在循环依赖
 */
export function detectCyclicDependency(configs: ChartLinkConfigWithId[]): boolean {
  // 构建邻接表
  const graph = new Map<string, Set<string>>()
  
  configs.forEach(config => {
    if (!graph.has(config.sourceChartId)) {
      graph.set(config.sourceChartId, new Set())
    }
    graph.get(config.sourceChartId)!.add(config.targetChartId)
  })
  
  // DFS检测环
  const visited = new Set<string>()
  const recursionStack = new Set<string>()
  
  const hasCycle = (node: string): boolean => {
    visited.add(node)
    recursionStack.add(node)
    
    const neighbors = graph.get(node) || new Set()
    for (const neighbor of neighbors) {
      if (!visited.has(neighbor)) {
        if (hasCycle(neighbor)) {
          return true
        }
      } else if (recursionStack.has(neighbor)) {
        return true
      }
    }
    
    recursionStack.delete(node)
    return false
  }
  
  for (const node of graph.keys()) {
    if (!visited.has(node)) {
      if (hasCycle(node)) {
        return true
      }
    }
  }
  
  return false
}

/**
 * 生成唯一的联动配置ID
 */
export function generateLinkConfigId(): string {
  return `link-${Date.now()}-${Math.random().toString(36).substring(2, 8)}`
}
