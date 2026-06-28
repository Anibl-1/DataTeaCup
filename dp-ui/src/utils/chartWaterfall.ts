/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 瀑布图工具函数
 * 提供瀑布图的 ECharts 配置构建
 * 用于展示财务增减分析，显示累计效果
 */

import type { EChartsOption } from '@/utils/echarts'

/**
 * 瀑布图数据项接口
 */
export interface WaterfallDataItem {
  /** 名称/标签 */
  name: string
  /** 数值（正数为增加，负数为减少） */
  value: number
  /** 是否为总计项 */
  isTotal?: boolean
}

/**
 * 瀑布图配置选项
 */
export interface WaterfallChartOptions {
  /** 图表标题 */
  title?: string
  /** 是否显示总计 */
  showTotal?: boolean
  /** 总计标签 */
  totalLabel?: string
  /** 起始值 */
  startValue?: number
  /** 起始标签 */
  startLabel?: string
  /** 正值颜色 */
  positiveColor?: string
  /** 负值颜色 */
  negativeColor?: string
  /** 总计颜色 */
  totalColor?: string
  /** 透明占位颜色 */
  transparentColor?: string
  /** 是否显示标签 */
  showLabel?: boolean
  /** 标签位置 */
  labelPosition?: 'top' | 'inside' | 'bottom'
  /** 柱子宽度 */
  barWidth?: number | string
  /** 是否显示连接线 */
  showConnectLine?: boolean
}

/**
 * 瀑布图计算结果
 */
export interface WaterfallCalculation {
  /** 类别数组 */
  categories: string[]
  /** 透明占位数据 */
  transparentData: number[]
  /** 正值数据 */
  positiveData: (number | '-')[]
  /** 负值数据 */
  negativeData: (number | '-')[]
  /** 总计数据 */
  totalData: (number | '-')[]
  /** 累计值数组 */
  runningTotals: number[]
}

/**
 * 默认配置
 */
const DEFAULT_OPTIONS: Required<WaterfallChartOptions> = {
  title: '',
  showTotal: true,
  totalLabel: '总计',
  startValue: 0,
  startLabel: '起始',
  positiveColor: '#52c41a',
  negativeColor: '#ff4d4f',
  totalColor: '#1890ff',
  transparentColor: 'transparent',
  showLabel: true,
  labelPosition: 'top',
  barWidth: '40%',
  showConnectLine: false
}

/**
 * 计算瀑布图数据
 * @param data 原始数据
 * @param options 配置选项
 * @returns 计算结果
 */
export function calculateWaterfallData(
  data: WaterfallDataItem[],
  options: WaterfallChartOptions = {}
): WaterfallCalculation {
  const {
    showTotal = DEFAULT_OPTIONS.showTotal,
    totalLabel = DEFAULT_OPTIONS.totalLabel,
    startValue = DEFAULT_OPTIONS.startValue,
    startLabel = DEFAULT_OPTIONS.startLabel
  } = options

  const categories: string[] = []
  const transparentData: number[] = []
  const positiveData: (number | '-')[] = []
  const negativeData: (number | '-')[] = []
  const totalData: (number | '-')[] = []
  const runningTotals: number[] = []

  let runningTotal = startValue

  // 添加起始值（如果有）
  if (startValue !== 0 || startLabel) {
    categories.push(startLabel)
    transparentData.push(0)
    positiveData.push('-')
    negativeData.push('-')
    totalData.push(startValue)
    runningTotals.push(startValue)
  }

  // 处理每个数据项
  for (const item of data) {
    if (item.isTotal) {
      // 总计项
      categories.push(item.name)
      transparentData.push(0)
      positiveData.push('-')
      negativeData.push('-')
      totalData.push(runningTotal)
      runningTotals.push(runningTotal)
    } else {
      categories.push(item.name)
      
      if (item.value >= 0) {
        // 正值：透明部分为当前累计值
        transparentData.push(runningTotal)
        positiveData.push(item.value)
        negativeData.push('-')
        totalData.push('-')
      } else {
        // 负值：透明部分为当前累计值加上负值（即新的累计值）
        transparentData.push(runningTotal + item.value)
        positiveData.push('-')
        negativeData.push(Math.abs(item.value))
        totalData.push('-')
      }
      
      runningTotal += item.value
      runningTotals.push(runningTotal)
    }
  }

  // 添加总计（如果需要且数据中没有总计项）
  if (showTotal && !data.some(item => item.isTotal)) {
    categories.push(totalLabel)
    transparentData.push(0)
    positiveData.push('-')
    negativeData.push('-')
    totalData.push(runningTotal)
    runningTotals.push(runningTotal)
  }

  return {
    categories,
    transparentData,
    positiveData,
    negativeData,
    totalData,
    runningTotals
  }
}

/**
 * 构建瀑布图 ECharts 配置
 * @param data 瀑布图数据
 * @param options 配置选项
 * @returns ECharts 配置对象
 */
export function buildWaterfallOption(
  data: WaterfallDataItem[],
  options: WaterfallChartOptions = {}
): EChartsOption {
  const mergedOptions = { ...DEFAULT_OPTIONS, ...options }
  const {
    title,
    positiveColor,
    negativeColor,
    totalColor,
    transparentColor,
    showLabel,
    labelPosition,
    barWidth
  } = mergedOptions

  // 处理空数据
  if (!data || data.length === 0) {
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

  // 计算瀑布图数据
  const calculation = calculateWaterfallData(data, options)
  const { categories, transparentData, positiveData, negativeData, totalData, runningTotals } = calculation

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
        type: 'shadow'
      },
      formatter: (params: any) => {
        if (!Array.isArray(params) || params.length === 0) {
          return ''
        }
        
        const index = params[0].dataIndex
        const name = categories[index]
        const runningTotal = runningTotals[index]
        
        // 找到非透明的数据
        let value = 0
        let type = ''
        
        for (const param of params) {
          if (param.seriesName === '增加' && param.value !== '-') {
            value = param.value
            type = '增加'
          } else if (param.seriesName === '减少' && param.value !== '-') {
            value = -param.value
            type = '减少'
          } else if (param.seriesName === '总计' && param.value !== '-') {
            value = param.value
            type = '总计'
          }
        }
        
        if (type === '总计') {
          return `${name}<br/>总计: ${value}`
        }
        
        return `${name}<br/>${type}: ${value}<br/>累计: ${runningTotal}`
      }
    },
    legend: {
      data: ['增加', '减少', '总计'],
      top: title ? 40 : 10,
      itemWidth: 14,
      itemHeight: 14
    },
    grid: {
      top: title ? 80 : 50,
      left: 60,
      right: 20,
      bottom: 40,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: categories,
      axisLabel: {
        rotate: categories.length > 8 ? 30 : 0,
        fontSize: 12
      },
      axisTick: {
        alignWithLabel: true
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        fontSize: 12
      }
    },
    series: [
      // 透明占位系列
      {
        name: '占位',
        type: 'bar',
        stack: 'waterfall',
        itemStyle: {
          color: transparentColor,
          borderColor: transparentColor
        },
        emphasis: {
          itemStyle: {
            color: transparentColor,
            borderColor: transparentColor
          }
        },
        data: transparentData,
        barWidth
      },
      // 正值系列
      {
        name: '增加',
        type: 'bar',
        stack: 'waterfall',
        itemStyle: {
          color: positiveColor,
          borderRadius: [4, 4, 0, 0]
        },
        label: {
          show: showLabel,
          position: labelPosition,
          formatter: (params: any) => {
            if (params.value === '-' || params.value === 0) return ''
            return `+${params.value}`
          },
          fontSize: 11,
          color: positiveColor
        },
        data: positiveData,
        barWidth
      },
      // 负值系列
      {
        name: '减少',
        type: 'bar',
        stack: 'waterfall',
        itemStyle: {
          color: negativeColor,
          borderRadius: [4, 4, 0, 0]
        },
        label: {
          show: showLabel,
          position: labelPosition,
          formatter: (params: any) => {
            if (params.value === '-' || params.value === 0) return ''
            return `-${params.value}`
          },
          fontSize: 11,
          color: negativeColor
        },
        data: negativeData,
        barWidth
      },
      // 总计系列
      {
        name: '总计',
        type: 'bar',
        stack: 'waterfall',
        itemStyle: {
          color: totalColor,
          borderRadius: [4, 4, 0, 0]
        },
        label: {
          show: showLabel,
          position: labelPosition,
          formatter: (params: any) => {
            if (params.value === '-') return ''
            return params.value
          },
          fontSize: 11,
          color: totalColor
        },
        data: totalData,
        barWidth
      }
    ]
  }

  return option
}

/**
 * 验证瀑布图累计总额不变量
 * 检查最终累计值是否等于起始值加上所有值之和
 * @param data 输入数据
 * @param startValue 起始值
 * @returns 是否满足不变量
 */
export function validateWaterfallInvariant(
  data: WaterfallDataItem[],
  startValue: number = 0
): boolean {
  if (!data || data.length === 0) {
    return true
  }

  // 计算所有非总计项的值之和
  const sum = data
    .filter(item => !item.isTotal)
    .reduce((acc, item) => acc + item.value, 0)

  // 计算瀑布图数据
  const calculation = calculateWaterfallData(data, { startValue, showTotal: true })
  const finalTotal = calculation.runningTotals[calculation.runningTotals.length - 1]

  // 验证最终累计值等于起始值加上所有值之和
  return Math.abs(finalTotal - (startValue + sum)) < 0.0001
}

/**
 * 验证瀑布图每一步累计值正确性
 * @param data 输入数据
 * @param startValue 起始值
 * @returns 是否所有步骤的累计值都正确
 */
export function validateWaterfallSteps(
  data: WaterfallDataItem[],
  startValue: number = 0
): boolean {
  if (!data || data.length === 0) {
    return true
  }

  const calculation = calculateWaterfallData(data, { startValue, showTotal: false })
  
  let expectedTotal = startValue
  let dataIndex = 0
  
  // 跳过起始项（如果有）
  const startIndex = (startValue !== 0 || calculation.categories[0] === '起始') ? 1 : 0
  
  for (let i = startIndex; i < calculation.runningTotals.length; i++) {
    if (dataIndex < data.length && !data[dataIndex].isTotal) {
      expectedTotal += data[dataIndex].value
    }
    
    if (Math.abs(calculation.runningTotals[i] - expectedTotal) > 0.0001) {
      return false
    }
    
    dataIndex++
  }

  return true
}
