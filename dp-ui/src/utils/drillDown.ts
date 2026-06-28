/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 图表下钻引擎
 * 用于管理图表的数据下钻功能
 * 支持时间维度下钻（年→季→月→日）和类别维度下钻（区域→城市→门店）
 */

import request from '@/api/request'
import type { ApiResponse } from '@/types/api'

/**
 * 下钻层级配置
 */
export interface DrillLevel {
  /** 字段名 */
  field: string
  /** 粒度类型 */
  granularity: 'year' | 'quarter' | 'month' | 'day' | 'custom'
  /** 层级标签（用于面包屑显示） */
  label?: string
}

/**
 * 下钻配置
 */
export interface DrillDownConfig {
  /** 图表ID */
  chartId: number
  /** 下钻层级列表 */
  levels: DrillLevel[]
  /** 当前层级索引（从0开始） */
  currentLevel: number
}

/**
 * 面包屑项
 */
export interface BreadcrumbItem {
  /** 层级索引 */
  level: number
  /** 层级标签 */
  label: string
  /** 维度值 */
  value: any
  /** 字段名 */
  field: string
}

/**
 * 下钻状态
 */
export interface DrillDownState {
  /** 当前层级索引 */
  currentLevel: number
  /** 面包屑路径 */
  breadcrumb: BreadcrumbItem[]
  /** 当前筛选条件 */
  filters: Record<string, any>
  /** 当前数据 */
  data: any[] | null
}

/**
 * 下钻事件类型
 */
export type DrillEventType = 'drill-down' | 'drill-up' | 'drill-reset' | 'data-loaded'

/**
 * 下钻事件监听器
 */
export type DrillEventListener = (event: {
  type: DrillEventType
  chartId: number
  level: number
  dimensionValue?: any
  data?: any[]
  breadcrumb: BreadcrumbItem[]
}) => void

/**
 * 下钻管理器接口
 */
export interface DrillDownManager {
  /** 设置下钻配置 */
  setDrillConfig: (config: Omit<DrillDownConfig, 'chartId'>) => void
  /** 获取下钻配置 */
  getDrillConfig: () => DrillDownConfig | null
  /** 下钻到下一层级 */
  drillDown: (dimensionValue: any) => Promise<void>
  /** 返回上一层级 */
  drillUp: () => Promise<void>
  /** 重置到顶层 */
  resetDrill: () => Promise<void>
  /** 跳转到指定层级 */
  drillToLevel: (level: number) => Promise<void>
  /** 获取当前层级 */
  getCurrentLevel: () => number
  /** 获取面包屑路径 */
  getBreadcrumb: () => BreadcrumbItem[]
  /** 获取当前状态 */
  getState: () => DrillDownState
  /** 检查是否可以下钻 */
  canDrillDown: () => boolean
  /** 检查是否可以上钻 */
  canDrillUp: () => boolean
  /** 添加事件监听器 */
  addEventListener: (listener: DrillEventListener) => void
  /** 移除事件监听器 */
  removeEventListener: (listener: DrillEventListener) => void
  /** 销毁实例 */
  destroy: () => void
}

/**
 * 下钻数据响应
 */
export interface DrillDownDataResponse {
  /** 数据列表 */
  data: any[]
  /** 当前层级 */
  level: number
  /** 字段名 */
  field: string
}

/**
 * 从后端获取下钻数据
 * @param chartId 图表ID
 * @param dimensionValue 维度值
 * @param level 目标层级
 * @param filters 当前筛选条件
 */
export async function fetchDrillDownData(
  chartId: number,
  dimensionValue: any,
  level: number,
  filters?: Record<string, any>
): Promise<any[]> {
  try {
    const response = await request.post<ApiResponse<DrillDownDataResponse>>(
      '/chart-link/drill-down',
      {
        chartId,
        dimensionValue: dimensionValue !== null && dimensionValue !== undefined 
          ? String(dimensionValue) 
          : null,
        level,
        filters: filters || {}
      }
    )

    const responseData = response as any
    if (responseData && typeof responseData === 'object') {
      if ('data' in responseData && responseData.data) {
        // 响应格式: { code, msg, data: { data: [...], level, field } }
        if (Array.isArray(responseData.data.data)) {
          return responseData.data.data
        }
        // 响应格式: { code, msg, data: [...] }
        if (Array.isArray(responseData.data)) {
          return responseData.data
        }
      }
    }
    
    return []
  } catch (error) {
    console.error('Failed to fetch drill-down data:', error)
    return []
  }
}

/**
 * 获取默认层级标签
 */
function getDefaultLevelLabel(granularity: DrillLevel['granularity'], level: number): string {
  const labels: Record<string, string> = {
    year: '年',
    quarter: '季度',
    month: '月',
    day: '日',
    custom: `层级${level + 1}`
  }
  return labels[granularity] || `层级${level + 1}`
}

/**
 * 创建下钻管理器
 * @param chartId 图表ID
 */
export function createDrillDownManager(chartId: number): DrillDownManager {
  // 下钻配置
  let config: DrillDownConfig | null = null
  
  // 当前状态
  let state: DrillDownState = {
    currentLevel: 0,
    breadcrumb: [],
    filters: {},
    data: null
  }
  
  // 事件监听器
  const eventListeners: Set<DrillEventListener> = new Set()
  
  /**
   * 触发事件
   */
  const emitEvent = (event: Parameters<DrillEventListener>[0]) => {
    eventListeners.forEach(listener => {
      try {
        listener(event)
      } catch (error) {
        console.error('Error in drill event listener:', error)
      }
    })
  }
  
  /**
   * 设置下钻配置
   */
  const setDrillConfig = (newConfig: Omit<DrillDownConfig, 'chartId'>) => {
    config = {
      chartId,
      levels: newConfig.levels || [],
      currentLevel: newConfig.currentLevel || 0
    }
    
    // 重置状态
    state = {
      currentLevel: config.currentLevel,
      breadcrumb: [],
      filters: {},
      data: null
    }
    
    // 初始化面包屑（添加根层级）
    if (config.levels.length > 0) {
      const rootLevel = config.levels[0]
      state.breadcrumb.push({
        level: 0,
        label: rootLevel.label || getDefaultLevelLabel(rootLevel.granularity, 0),
        value: null,
        field: rootLevel.field
      })
    }
  }
  
  /**
   * 获取下钻配置
   */
  const getDrillConfig = (): DrillDownConfig | null => {
    return config ? { ...config } : null
  }
  
  /**
   * 下钻到下一层级
   */
  const drillDown = async (dimensionValue: any): Promise<void> => {
    if (!config || !canDrillDown()) {
      return
    }
    
    const nextLevel = state.currentLevel + 1
    const nextLevelConfig = config.levels[nextLevel]
    
    if (!nextLevelConfig) {
      return
    }
    
    // 更新筛选条件
    const currentLevelConfig = config.levels[state.currentLevel]
    state.filters[currentLevelConfig.field] = dimensionValue
    
    // 获取下钻数据
    const data = await fetchDrillDownData(
      chartId,
      dimensionValue,
      nextLevel,
      state.filters
    )
    
    // 更新状态
    state.currentLevel = nextLevel
    state.data = data
    
    // 更新面包屑
    state.breadcrumb.push({
      level: nextLevel,
      label: nextLevelConfig.label || getDefaultLevelLabel(nextLevelConfig.granularity, nextLevel),
      value: dimensionValue,
      field: nextLevelConfig.field
    })
    
    // 同步配置中的当前层级
    config.currentLevel = nextLevel
    
    // 触发事件
    emitEvent({
      type: 'drill-down',
      chartId,
      level: nextLevel,
      dimensionValue,
      data,
      breadcrumb: [...state.breadcrumb]
    })
  }
  
  /**
   * 返回上一层级
   */
  const drillUp = async (): Promise<void> => {
    if (!config || !canDrillUp()) {
      return
    }
    
    const prevLevel = state.currentLevel - 1
    
    // 移除当前层级的筛选条件
    const currentLevelConfig = config.levels[state.currentLevel]
    if (currentLevelConfig) {
      delete state.filters[currentLevelConfig.field]
    }
    
    // 移除上一层级的筛选条件（因为要重新显示该层级的数据）
    if (prevLevel > 0) {
      const prevLevelConfig = config.levels[prevLevel]
      if (prevLevelConfig) {
        delete state.filters[prevLevelConfig.field]
      }
    }
    
    // 获取上一层级的维度值
    const prevBreadcrumb = state.breadcrumb[prevLevel]
    const dimensionValue = prevLevel > 0 ? state.breadcrumb[prevLevel - 1]?.value : null
    
    // 获取上钻数据
    const data = await fetchDrillDownData(
      chartId,
      dimensionValue,
      prevLevel,
      state.filters
    )
    
    // 更新状态
    state.currentLevel = prevLevel
    state.data = data
    
    // 更新面包屑（移除当前及之后的层级）
    state.breadcrumb = state.breadcrumb.slice(0, prevLevel + 1)
    
    // 同步配置中的当前层级
    config.currentLevel = prevLevel
    
    // 触发事件
    emitEvent({
      type: 'drill-up',
      chartId,
      level: prevLevel,
      dimensionValue: prevBreadcrumb?.value,
      data,
      breadcrumb: [...state.breadcrumb]
    })
  }
  
  /**
   * 重置到顶层
   */
  const resetDrill = async (): Promise<void> => {
    if (!config) {
      return
    }
    
    // 获取顶层数据
    const data = await fetchDrillDownData(chartId, null, 0, {})
    
    // 重置状态
    state.currentLevel = 0
    state.filters = {}
    state.data = data
    
    // 重置面包屑
    if (config.levels.length > 0) {
      const rootLevel = config.levels[0]
      state.breadcrumb = [{
        level: 0,
        label: rootLevel.label || getDefaultLevelLabel(rootLevel.granularity, 0),
        value: null,
        field: rootLevel.field
      }]
    } else {
      state.breadcrumb = []
    }
    
    // 同步配置中的当前层级
    config.currentLevel = 0
    
    // 触发事件
    emitEvent({
      type: 'drill-reset',
      chartId,
      level: 0,
      data,
      breadcrumb: [...state.breadcrumb]
    })
  }
  
  /**
   * 跳转到指定层级
   */
  const drillToLevel = async (targetLevel: number): Promise<void> => {
    if (!config) {
      return
    }
    
    // 验证目标层级
    if (targetLevel < 0 || targetLevel >= config.levels.length) {
      return
    }
    
    // 如果目标层级等于当前层级，不做任何操作
    if (targetLevel === state.currentLevel) {
      return
    }
    
    // 如果目标层级是0，直接重置
    if (targetLevel === 0) {
      await resetDrill()
      return
    }
    
    // 重建筛选条件（只保留目标层级之前的筛选）
    const newFilters: Record<string, any> = {}
    for (let i = 0; i < targetLevel; i++) {
      const levelConfig = config.levels[i]
      const breadcrumbItem = state.breadcrumb[i + 1] // +1 因为面包屑第一项是根层级
      if (levelConfig && breadcrumbItem && breadcrumbItem.value !== null) {
        newFilters[levelConfig.field] = breadcrumbItem.value
      }
    }
    
    // 获取目标层级的维度值
    const dimensionValue = targetLevel > 0 ? state.breadcrumb[targetLevel - 1]?.value : null
    
    // 获取目标层级数据
    const data = await fetchDrillDownData(
      chartId,
      dimensionValue,
      targetLevel,
      newFilters
    )
    
    // 更新状态
    state.currentLevel = targetLevel
    state.filters = newFilters
    state.data = data
    
    // 更新面包屑（保留到目标层级）
    state.breadcrumb = state.breadcrumb.slice(0, targetLevel + 1)
    
    // 同步配置中的当前层级
    config.currentLevel = targetLevel
    
    // 触发事件
    emitEvent({
      type: 'drill-down',
      chartId,
      level: targetLevel,
      dimensionValue,
      data,
      breadcrumb: [...state.breadcrumb]
    })
  }
  
  /**
   * 获取当前层级
   */
  const getCurrentLevel = (): number => {
    return state.currentLevel
  }
  
  /**
   * 获取面包屑路径
   */
  const getBreadcrumb = (): BreadcrumbItem[] => {
    return [...state.breadcrumb]
  }
  
  /**
   * 获取当前状态
   */
  const getState = (): DrillDownState => {
    return {
      currentLevel: state.currentLevel,
      breadcrumb: [...state.breadcrumb],
      filters: { ...state.filters },
      data: state.data ? [...state.data] : null
    }
  }
  
  /**
   * 检查是否可以下钻
   */
  const canDrillDown = (): boolean => {
    if (!config) return false
    return state.currentLevel < config.levels.length - 1
  }
  
  /**
   * 检查是否可以上钻
   */
  const canDrillUp = (): boolean => {
    return state.currentLevel > 0
  }
  
  /**
   * 添加事件监听器
   */
  const addEventListener = (listener: DrillEventListener) => {
    eventListeners.add(listener)
  }
  
  /**
   * 移除事件监听器
   */
  const removeEventListener = (listener: DrillEventListener) => {
    eventListeners.delete(listener)
  }
  
  /**
   * 销毁实例
   */
  const destroy = () => {
    config = null
    state = {
      currentLevel: 0,
      breadcrumb: [],
      filters: {},
      data: null
    }
    eventListeners.clear()
  }
  
  return {
    setDrillConfig,
    getDrillConfig,
    drillDown,
    drillUp,
    resetDrill,
    drillToLevel,
    getCurrentLevel,
    getBreadcrumb,
    getState,
    canDrillDown,
    canDrillUp,
    addEventListener,
    removeEventListener,
    destroy
  }
}

/**
 * 预设的时间维度下钻配置
 * 年 → 季度 → 月 → 日
 */
export const TIME_DRILL_CONFIG: DrillLevel[] = [
  { field: 'year', granularity: 'year', label: '年' },
  { field: 'quarter', granularity: 'quarter', label: '季度' },
  { field: 'month', granularity: 'month', label: '月' },
  { field: 'day', granularity: 'day', label: '日' }
]

/**
 * 预设的区域维度下钻配置
 * 区域 → 城市 → 门店
 */
export const REGION_DRILL_CONFIG: DrillLevel[] = [
  { field: 'region', granularity: 'custom', label: '区域' },
  { field: 'city', granularity: 'custom', label: '城市' },
  { field: 'store', granularity: 'custom', label: '门店' }
]

/**
 * 创建自定义下钻配置
 * @param levels 层级配置数组
 */
export function createDrillConfig(
  levels: Array<{ field: string; label?: string; granularity?: DrillLevel['granularity'] }>
): DrillLevel[] {
  return levels.map((level, index) => ({
    field: level.field,
    granularity: level.granularity || 'custom',
    label: level.label || `层级${index + 1}`
  }))
}

/**
 * 验证下钻配置的有效性
 * @param config 下钻配置
 */
export function validateDrillConfig(config: DrillDownConfig): string[] {
  const errors: string[] = []
  
  if (!config.chartId || config.chartId <= 0) {
    errors.push('图表ID无效')
  }
  
  if (!config.levels || config.levels.length === 0) {
    errors.push('下钻层级配置不能为空')
  }
  
  if (config.levels) {
    const fields = new Set<string>()
    config.levels.forEach((level, index) => {
      if (!level.field) {
        errors.push(`层级${index + 1}的字段名不能为空`)
      }
      
      if (fields.has(level.field)) {
        errors.push(`层级${index + 1}的字段名"${level.field}"重复`)
      }
      fields.add(level.field)
      
      const validGranularities = ['year', 'quarter', 'month', 'day', 'custom']
      if (!validGranularities.includes(level.granularity)) {
        errors.push(`层级${index + 1}的粒度类型"${level.granularity}"无效`)
      }
    })
  }
  
  if (config.currentLevel < 0) {
    errors.push('当前层级不能为负数')
  }
  
  if (config.levels && config.currentLevel >= config.levels.length) {
    errors.push('当前层级超出层级配置范围')
  }
  
  return errors
}

/**
 * 格式化面包屑显示文本
 * @param breadcrumb 面包屑数组
 * @param separator 分隔符
 */
export function formatBreadcrumb(
  breadcrumb: BreadcrumbItem[],
  separator: string = ' > '
): string {
  return breadcrumb
    .map(item => {
      if (item.value === null || item.value === undefined) {
        return item.label
      }
      return `${item.label}: ${item.value}`
    })
    .join(separator)
}

/**
 * 根据粒度获取时间格式化函数
 * @param granularity 粒度类型
 */
export function getTimeFormatter(
  granularity: DrillLevel['granularity']
): (value: any) => string {
  switch (granularity) {
    case 'year':
      return (value) => `${value}年`
    case 'quarter':
      return (value) => `Q${value}`
    case 'month':
      return (value) => `${value}月`
    case 'day':
      return (value) => `${value}日`
    default:
      return (value) => String(value)
  }
}
