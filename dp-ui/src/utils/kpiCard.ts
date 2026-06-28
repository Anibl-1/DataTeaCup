/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * KPI 卡片工具函数
 * 提供 KPI 计算、格式化和迷你图配置构建
 */

import type { EChartsOption } from '@/utils/echarts'

/**
 * 趋势方向类型
 */
export type TrendDirection = 'up' | 'down' | 'neutral'

/**
 * 值格式化类型
 */
export type ValueFormat = 'number' | 'currency' | 'percentage' | 'compact'

/**
 * 趋势计算结果
 */
export interface TrendResult {
  /** 趋势方向 */
  direction: TrendDirection
  /** 变化百分比 */
  percentage: number
  /** 变化绝对值 */
  change: number
}

/**
 * KPI 卡片数据
 */
export interface KpiCardData {
  /** 当前值 */
  value: number
  /** 格式化后的值 */
  formattedValue: string
  /** 同比变化率 */
  yoyChange: number | null
  /** 环比变化率 */
  momChange: number | null
  /** 趋势方向 */
  trend: TrendDirection
  /** 趋势百分比 */
  trendPercent: number | null
}

/**
 * KPI 配置
 */
export interface KpiConfig {
  /** 值格式 */
  format?: ValueFormat
  /** 货币符号 */
  currencySymbol?: string
  /** 小数位数 */
  decimals?: number
  /** 单位 */
  unit?: string
  /** 前缀 */
  prefix?: string
  /** 后缀 */
  suffix?: string
  /** 正向趋势是否为好（默认 true，如成本类指标可设为 false） */
  positiveIsGood?: boolean
}

/**
 * 迷你图配置选项
 */
export interface SparklineOptions {
  /** 线条颜色 */
  color?: string
  /** 区域填充颜色 */
  areaColor?: string
  /** 线条宽度 */
  lineWidth?: number
  /** 是否平滑曲线 */
  smooth?: boolean
  /** 是否显示数据点 */
  showSymbol?: boolean
  /** 图表类型 */
  type?: 'line' | 'bar'
}

/**
 * 计算趋势方向和百分比
 * @param current 当前值
 * @param previous 上期值
 * @returns 趋势计算结果
 */
export function calculateTrend(current: number, previous: number): TrendResult {
  // 处理边界情况
  if (previous === 0) {
    if (current === 0) {
      return { direction: 'neutral', percentage: 0, change: 0 }
    }
    // 从 0 变为非 0，视为无穷大变化，返回 100% 或 -100%
    return {
      direction: current > 0 ? 'up' : 'down',
      percentage: current > 0 ? 100 : -100,
      change: current
    }
  }

  const change = current - previous
  const percentage = (change / Math.abs(previous)) * 100

  let direction: TrendDirection
  if (change > 0) {
    direction = 'up'
  } else if (change < 0) {
    direction = 'down'
  } else {
    direction = 'neutral'
  }

  return {
    direction,
    percentage,
    change
  }
}

/**
 * 格式化 KPI 值
 * @param value 原始值
 * @param format 格式类型
 * @param options 格式化选项
 * @returns 格式化后的字符串
 */
export function formatKpiValue(
  value: number | null | undefined,
  format: ValueFormat = 'number',
  options: Partial<KpiConfig> = {}
): string {
  if (value === null || value === undefined || isNaN(value)) {
    return '--'
  }

  const {
    currencySymbol = '¥',
    decimals = 2,
    unit = '',
    prefix = '',
    suffix = ''
  } = options

  let formattedValue: string

  switch (format) {
    case 'currency':
      formattedValue = formatCurrency(value, currencySymbol, decimals)
      break
    case 'percentage':
      formattedValue = formatPercentage(value, decimals)
      break
    case 'compact':
      formattedValue = formatCompact(value, decimals)
      break
    case 'number':
    default:
      formattedValue = formatNumber(value, decimals)
      break
  }

  // 添加前缀、后缀和单位
  let result = formattedValue
  if (prefix) result = prefix + result
  if (unit) result = result + unit
  if (suffix) result = result + suffix

  return result
}

/**
 * 格式化数字（带千分位）
 */
function formatNumber(value: number, decimals: number): string {
  if (Number.isInteger(value)) {
    return value.toLocaleString('zh-CN')
  }
  return value.toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: decimals
  })
}

/**
 * 格式化货币
 */
function formatCurrency(value: number, symbol: string, decimals: number): string {
  const formatted = value.toLocaleString('zh-CN', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  })
  return `${symbol}${formatted}`
}

/**
 * 格式化百分比
 */
function formatPercentage(value: number, decimals: number): string {
  return `${value.toFixed(decimals)}%`
}

/**
 * 格式化为紧凑形式（K, M, B）
 */
function formatCompact(value: number, decimals: number): string {
  const absValue = Math.abs(value)
  const sign = value < 0 ? '-' : ''

  if (absValue >= 1e9) {
    return `${sign}${(absValue / 1e9).toFixed(decimals)}B`
  }
  if (absValue >= 1e6) {
    return `${sign}${(absValue / 1e6).toFixed(decimals)}M`
  }
  if (absValue >= 1e3) {
    return `${sign}${(absValue / 1e3).toFixed(decimals)}K`
  }
  return formatNumber(value, decimals)
}

/**
 * 构建迷你图 ECharts 配置
 * @param data 数据数组
 * @param options 配置选项
 * @returns ECharts 配置对象
 */
export function buildSparklineOption(
  data: number[],
  options: SparklineOptions = {}
): EChartsOption {
  const {
    color = '#1890ff',
    areaColor,
    lineWidth = 2,
    smooth = true,
    showSymbol = false,
    type = 'line'
  } = options

  // 处理空数据
  if (!data || data.length === 0) {
    return {
      grid: { top: 0, right: 0, bottom: 0, left: 0 },
      xAxis: { show: false, type: 'category' },
      yAxis: { show: false, type: 'value' },
      series: []
    }
  }

  // 计算 Y 轴范围，留出一些边距
  const minValue = Math.min(...data)
  const maxValue = Math.max(...data)
  const padding = (maxValue - minValue) * 0.1 || 1

  const baseOption: EChartsOption = {
    grid: {
      top: 2,
      right: 2,
      bottom: 2,
      left: 2
    },
    xAxis: {
      show: false,
      type: 'category',
      boundaryGap: type === 'bar',
      data: data.map((_, index) => index)
    },
    yAxis: {
      show: false,
      type: 'value',
      min: minValue - padding,
      max: maxValue + padding
    },
    tooltip: {
      show: false
    }
  }

  if (type === 'bar') {
    return {
      ...baseOption,
      series: [{
        type: 'bar',
        data: data,
        itemStyle: {
          color: color,
          borderRadius: [2, 2, 0, 0]
        },
        barWidth: '60%'
      }]
    }
  }

  // 折线图配置
  const seriesConfig: any = {
    type: 'line',
    data: data,
    smooth,
    symbol: showSymbol ? 'circle' : 'none',
    symbolSize: 4,
    lineStyle: {
      width: lineWidth,
      color: color
    },
    itemStyle: {
      color: color
    }
  }

  // 添加区域填充
  if (areaColor) {
    seriesConfig.areaStyle = {
      color: {
        type: 'linear',
        x: 0,
        y: 0,
        x2: 0,
        y2: 1,
        colorStops: [
          { offset: 0, color: areaColor },
          { offset: 1, color: 'rgba(255, 255, 255, 0)' }
        ]
      }
    }
  }

  return {
    ...baseOption,
    series: [seriesConfig]
  }
}

/**
 * 构建 KPI 卡片数据
 * @param current 当前值
 * @param previous 上期值（用于同比）
 * @param periodPrevious 上周期值（用于环比）
 * @param config KPI 配置
 * @returns KPI 卡片数据
 */
export function buildKpiCardConfig(
  current: number,
  previous: number | null,
  periodPrevious: number | null,
  config: KpiConfig = {}
): KpiCardData {
  const { format = 'number', positiveIsGood = true } = config

  // 格式化当前值
  const formattedValue = formatKpiValue(current, format, config)

  // 计算同比变化率 (Year-over-Year)
  let yoyChange: number | null = null
  if (previous !== null && previous !== 0) {
    yoyChange = ((current - previous) / Math.abs(previous)) * 100
  }

  // 计算环比变化率 (Month-over-Month)
  let momChange: number | null = null
  if (periodPrevious !== null && periodPrevious !== 0) {
    momChange = ((current - periodPrevious) / Math.abs(periodPrevious)) * 100
  }

  // 确定趋势方向（优先使用同比）
  let trend: TrendDirection = 'neutral'
  let trendPercent: number | null = null

  if (previous !== null) {
    const trendResult = calculateTrend(current, previous)
    trend = trendResult.direction
    trendPercent = trendResult.percentage
  } else if (periodPrevious !== null) {
    const trendResult = calculateTrend(current, periodPrevious)
    trend = trendResult.direction
    trendPercent = trendResult.percentage
  }

  return {
    value: current,
    formattedValue,
    yoyChange,
    momChange,
    trend,
    trendPercent
  }
}

/**
 * 获取趋势颜色
 * @param trend 趋势方向
 * @param positiveIsGood 正向趋势是否为好
 * @returns 颜色值
 */
export function getTrendColor(
  trend: TrendDirection,
  positiveIsGood: boolean = true
): string {
  if (trend === 'neutral') {
    return '#999999'
  }

  const isGood = positiveIsGood ? trend === 'up' : trend === 'down'
  return isGood ? '#52c41a' : '#ff4d4f'
}

/**
 * 格式化变化百分比
 * @param change 变化百分比
 * @param decimals 小数位数
 * @returns 格式化后的字符串
 */
export function formatChangePercent(
  change: number | null,
  decimals: number = 1
): string {
  if (change === null || isNaN(change)) {
    return '--'
  }
  const sign = change >= 0 ? '+' : ''
  return `${sign}${change.toFixed(decimals)}%`
}

/**
 * KPI 卡片尺寸配置
 */
export interface KpiSizeConfig {
  /** 主值字体大小 */
  valueFontSize: number
  /** 标题字体大小 */
  titleFontSize: number
  /** 趋势字体大小 */
  trendFontSize: number
  /** 内边距 */
  padding: number
  /** 图标大小 */
  iconSize: number
}

/**
 * 获取 KPI 卡片尺寸配置
 * @param size 尺寸类型
 * @returns 尺寸配置
 */
export function getKpiSizeConfig(size: 'small' | 'medium' | 'large' = 'medium'): KpiSizeConfig {
  const configs: Record<string, KpiSizeConfig> = {
    small: {
      valueFontSize: 24,
      titleFontSize: 12,
      trendFontSize: 12,
      padding: 12,
      iconSize: 20
    },
    medium: {
      valueFontSize: 36,
      titleFontSize: 14,
      trendFontSize: 14,
      padding: 16,
      iconSize: 24
    },
    large: {
      valueFontSize: 48,
      titleFontSize: 16,
      trendFontSize: 16,
      padding: 24,
      iconSize: 32
    }
  }

  return configs[size] || configs.medium
}
