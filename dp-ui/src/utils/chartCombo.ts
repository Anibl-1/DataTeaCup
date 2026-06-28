/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 组合图工具函数
 * 提供柱线混合图的 ECharts 配置构建
 * 支持双 Y 轴配置
 */

import type { EChartsOption } from '@/utils/echarts'

/**
 * 组合图数据项接口
 */
export interface ComboDataItem {
  /** X 轴类别值 */
  category: string
  /** 数据值（键为系列名称） */
  [key: string]: string | number
}

/**
 * 系列配置接口
 */
export interface ComboSeriesConfig {
  /** 系列名称（对应数据中的键） */
  name: string
  /** 图表类型 */
  type: 'bar' | 'line'
  /** Y 轴索引（0 为左轴，1 为右轴） */
  yAxisIndex: 0 | 1
  /** 系列颜色 */
  color?: string
  /** 是否平滑曲线（仅折线图） */
  smooth?: boolean
  /** 是否显示数据点（仅折线图） */
  showSymbol?: boolean
  /** 数据点大小（仅折线图） */
  symbolSize?: number
  /** 线条宽度（仅折线图） */
  lineWidth?: number
  /** 柱子宽度（仅柱状图） */
  barWidth?: number | string
  /** 柱子圆角（仅柱状图） */
  barBorderRadius?: number | [number, number, number, number]
  /** 是否堆叠 */
  stack?: string
  /** 区域填充（仅折线图） */
  areaStyle?: {
    color?: string
    opacity?: number
  }
}

/**
 * Y 轴配置接口
 */
export interface ComboYAxisConfig {
  /** 轴名称 */
  name?: string
  /** 最小值 */
  min?: number | 'dataMin'
  /** 最大值 */
  max?: number | 'dataMax'
  /** 分割段数 */
  splitNumber?: number
  /** 轴线颜色 */
  axisLineColor?: string
  /** 标签格式化函数 */
  formatter?: string | ((value: number) => string)
}

/**
 * 组合图配置选项
 */
export interface ComboChartOptions {
  /** 图表标题 */
  title?: string
  /** 左 Y 轴配置 */
  leftYAxis?: ComboYAxisConfig
  /** 右 Y 轴配置 */
  rightYAxis?: ComboYAxisConfig
  /** 是否显示图例 */
  showLegend?: boolean
  /** 图例位置 */
  legendPosition?: 'top' | 'bottom' | 'left' | 'right'
  /** 是否显示工具箱 */
  showToolbox?: boolean
  /** 是否显示数据缩放 */
  showDataZoom?: boolean
  /** X 轴标签旋转角度 */
  xAxisLabelRotate?: number
  /** 网格配置 */
  grid?: {
    top?: number | string
    right?: number | string
    bottom?: number | string
    left?: number | string
  }
  /** 默认颜色数组 */
  colors?: string[]
}

/**
 * 默认配置
 */
const DEFAULT_OPTIONS: ComboChartOptions = {
  title: '',
  leftYAxis: { name: '' },
  rightYAxis: { name: '' },
  showLegend: true,
  legendPosition: 'top',
  showToolbox: false,
  showDataZoom: false,
  xAxisLabelRotate: 0,
  grid: {
    top: 60,
    right: 60,
    bottom: 40,
    left: 60
  },
  colors: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']
}

/**
 * 默认系列配置
 */
const DEFAULT_SERIES_CONFIG: Partial<ComboSeriesConfig> = {
  yAxisIndex: 0,
  smooth: true,
  showSymbol: true,
  symbolSize: 6,
  lineWidth: 2,
  barWidth: '40%',
  barBorderRadius: [4, 4, 0, 0]
}

/**
 * 构建组合图 ECharts 配置
 * @param data 数据数组
 * @param seriesConfig 系列配置数组
 * @param options 图表配置选项
 * @returns ECharts 配置对象
 */
export function buildComboOption(
  data: ComboDataItem[],
  seriesConfig: ComboSeriesConfig[],
  options: ComboChartOptions = {}
): EChartsOption {
  const mergedOptions = { ...DEFAULT_OPTIONS, ...options }
  const {
    title,
    leftYAxis,
    rightYAxis,
    showLegend,
    legendPosition,
    showToolbox,
    showDataZoom,
    xAxisLabelRotate,
    grid,
    colors
  } = mergedOptions

  // 处理空数据
  if (!data || data.length === 0 || !seriesConfig || seriesConfig.length === 0) {
    return {
      title: title ? {
        text: title,
        left: 'center',
        top: 10
      } : undefined,
      graphic: [{
        type: 'text',
        left: 'center',
        top: 'middle',
        style: {
          text: '暂无数据',
          fontSize: 16,
          fill: '#999'
        }
      }]
    }
  }

  // 提取类别数据
  const categories = data.map(item => item.category)

  // 检查是否需要双 Y 轴
  const hasRightAxis = seriesConfig.some(s => s.yAxisIndex === 1)

  // 构建系列配置
  const series = seriesConfig.map((config, index) => {
    const mergedConfig = { ...DEFAULT_SERIES_CONFIG, ...config }
    const seriesData = data.map(item => {
      const value = item[config.name]
      return typeof value === 'number' ? value : (parseFloat(value as string) || 0)
    })

    const baseSeries: any = {
      name: config.name,
      type: config.type,
      yAxisIndex: config.yAxisIndex,
      data: seriesData,
      itemStyle: {
        color: config.color || colors?.[index % (colors?.length || 8)]
      }
    }

    if (config.type === 'line') {
      baseSeries.smooth = mergedConfig.smooth
      baseSeries.symbol = mergedConfig.showSymbol ? 'circle' : 'none'
      baseSeries.symbolSize = mergedConfig.symbolSize
      baseSeries.lineStyle = {
        width: mergedConfig.lineWidth
      }
      if (config.areaStyle) {
        baseSeries.areaStyle = config.areaStyle
      }
    } else if (config.type === 'bar') {
      baseSeries.barWidth = mergedConfig.barWidth
      baseSeries.itemStyle.borderRadius = mergedConfig.barBorderRadius
    }

    if (config.stack) {
      baseSeries.stack = config.stack
    }

    return baseSeries
  })

  // 构建 Y 轴配置
  const yAxisConfigs: any[] = [
    // 左 Y 轴
    {
      type: 'value',
      name: leftYAxis?.name || '',
      position: 'left',
      min: leftYAxis?.min,
      max: leftYAxis?.max,
      splitNumber: leftYAxis?.splitNumber || 5,
      axisLine: {
        show: true,
        lineStyle: {
          color: leftYAxis?.axisLineColor || '#333'
        }
      },
      axisLabel: {
        formatter: leftYAxis?.formatter
      },
      splitLine: {
        show: true,
        lineStyle: {
          type: 'dashed',
          color: '#e0e0e0'
        }
      }
    }
  ]

  // 添加右 Y 轴（如果需要）
  if (hasRightAxis) {
    yAxisConfigs.push({
      type: 'value',
      name: rightYAxis?.name || '',
      position: 'right',
      min: rightYAxis?.min,
      max: rightYAxis?.max,
      splitNumber: rightYAxis?.splitNumber || 5,
      axisLine: {
        show: true,
        lineStyle: {
          color: rightYAxis?.axisLineColor || '#333'
        }
      },
      axisLabel: {
        formatter: rightYAxis?.formatter
      },
      splitLine: {
        show: false
      }
    })
  }

  // 构建图例配置
  let legendConfig: any = undefined
  if (showLegend) {
    legendConfig = {
      data: seriesConfig.map(s => s.name),
      top: legendPosition === 'top' ? (title ? 35 : 10) : undefined,
      bottom: legendPosition === 'bottom' ? 10 : undefined,
      left: legendPosition === 'left' ? 10 : (legendPosition === 'right' ? undefined : 'center'),
      right: legendPosition === 'right' ? 10 : undefined,
      orient: (legendPosition === 'left' || legendPosition === 'right') ? 'vertical' : 'horizontal'
    }
  }

  // 构建工具箱配置
  let toolboxConfig: any = undefined
  if (showToolbox) {
    toolboxConfig = {
      show: true,
      right: 20,
      top: 5,
      feature: {
        dataZoom: { yAxisIndex: 'none' },
        restore: {},
        saveAsImage: {}
      }
    }
  }

  // 构建数据缩放配置
  let dataZoomConfig: any = undefined
  if (showDataZoom) {
    dataZoomConfig = [
      {
        type: 'inside',
        start: 0,
        end: 100
      },
      {
        type: 'slider',
        start: 0,
        end: 100,
        bottom: 10
      }
    ]
  }

  const option: EChartsOption = {
    title: title ? {
      text: title,
      left: 'center',
      top: 10,
      textStyle: {
        fontSize: 16,
        fontWeight: 'bold'
      }
    } : undefined,
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        crossStyle: {
          color: '#999'
        }
      }
    },
    legend: legendConfig,
    toolbox: toolboxConfig,
    dataZoom: dataZoomConfig,
    grid: {
      top: grid?.top ?? (title ? 80 : 60),
      right: grid?.right ?? (hasRightAxis ? 80 : 40),
      bottom: grid?.bottom ?? (showDataZoom ? 60 : 40),
      left: grid?.left ?? 60,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: categories,
      axisPointer: {
        type: 'shadow'
      },
      axisLabel: {
        rotate: xAxisLabelRotate,
        fontSize: 12
      },
      axisTick: {
        alignWithLabel: true
      }
    },
    yAxis: yAxisConfigs,
    series
  }

  return option
}

/**
 * 验证组合图双 Y 轴配置
 * 检查是否正确配置了双 Y 轴
 * @param option 生成的 ECharts 配置
 * @param seriesConfig 系列配置
 * @returns 验证结果
 */
export function validateComboChartDualAxis(
  option: EChartsOption,
  seriesConfig: ComboSeriesConfig[]
): { valid: boolean; message?: string } {
  const yAxis = option.yAxis as any[]
  const series = option.series as any[]

  if (!yAxis || !series) {
    return { valid: false, message: 'Missing yAxis or series configuration' }
  }

  // 检查是否有使用右轴的系列
  const hasRightAxisSeries = seriesConfig.some(s => s.yAxisIndex === 1)

  if (hasRightAxisSeries) {
    // 应该有两个 Y 轴
    if (yAxis.length !== 2) {
      return { valid: false, message: `Expected 2 yAxis, got ${yAxis.length}` }
    }

    // 检查左右轴位置
    if (yAxis[0].position !== 'left') {
      return { valid: false, message: 'First yAxis should be on left' }
    }
    if (yAxis[1].position !== 'right') {
      return { valid: false, message: 'Second yAxis should be on right' }
    }
  }

  // 检查每个系列的 yAxisIndex 是否正确
  for (let i = 0; i < seriesConfig.length; i++) {
    const config = seriesConfig[i]
    const seriesItem = series[i]

    if (seriesItem.yAxisIndex !== config.yAxisIndex) {
      return {
        valid: false,
        message: `Series "${config.name}" has wrong yAxisIndex: expected ${config.yAxisIndex}, got ${seriesItem.yAxisIndex}`
      }
    }
  }

  return { valid: true }
}

/**
 * 验证组合图数据完整性
 * 检查所有输入数据是否都在输出配置中
 * @param data 输入数据
 * @param seriesConfig 系列配置
 * @param option 生成的 ECharts 配置
 * @returns 是否完整
 */
export function validateComboDataIntegrity(
  data: ComboDataItem[],
  seriesConfig: ComboSeriesConfig[],
  option: EChartsOption
): boolean {
  if (!data || data.length === 0) {
    return true
  }

  const series = option.series as any[]
  const xAxis = option.xAxis as any

  if (!series || !xAxis) {
    return false
  }

  // 检查类别数据
  const categories = data.map(item => item.category)
  if (JSON.stringify(xAxis.data) !== JSON.stringify(categories)) {
    return false
  }

  // 检查每个系列的数据
  for (let i = 0; i < seriesConfig.length; i++) {
    const config = seriesConfig[i]
    const seriesItem = series[i]

    if (!seriesItem || seriesItem.name !== config.name) {
      return false
    }

    const expectedData = data.map(item => {
      const value = item[config.name]
      return typeof value === 'number' ? value : (parseFloat(value as string) || 0)
    })

    if (JSON.stringify(seriesItem.data) !== JSON.stringify(expectedData)) {
      return false
    }
  }

  return true
}

/**
 * 创建简单的组合图配置
 * 便捷方法，用于快速创建柱线混合图
 * @param data 数据数组
 * @param barFields 柱状图字段名数组
 * @param lineFields 折线图字段名数组
 * @param options 图表配置选项
 * @returns ECharts 配置对象
 */
export function buildSimpleComboOption(
  data: ComboDataItem[],
  barFields: string[],
  lineFields: string[],
  options: ComboChartOptions = {}
): EChartsOption {
  const seriesConfig: ComboSeriesConfig[] = [
    ...barFields.map((name, index) => ({
      name,
      type: 'bar' as const,
      yAxisIndex: 0 as const
    })),
    ...lineFields.map((name, index) => ({
      name,
      type: 'line' as const,
      yAxisIndex: lineFields.length > 0 ? 1 as const : 0 as const,
      smooth: true
    }))
  ]

  return buildComboOption(data, seriesConfig, options)
}
