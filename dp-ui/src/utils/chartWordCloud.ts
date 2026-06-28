/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 词云图工具函数
 * 提供词云图的 ECharts 配置构建
 * 用于展示文本频率分析
 * 注意：需要安装 echarts-wordcloud 扩展
 */

import type { EChartsOption } from '@/utils/echarts'

/**
 * 词云数据项接口
 */
export interface WordCloudDataItem {
  /** 词语/文本 */
  name: string
  /** 权重/频率 */
  value: number
  /** 额外数据 */
  [key: string]: any
}

/**
 * 词云形状类型
 */
export type WordCloudShape = 'circle' | 'cardioid' | 'diamond' | 'triangle-forward' | 'triangle' | 'pentagon' | 'star'

/**
 * 词云配置选项
 */
export interface WordCloudChartOptions {
  /** 图表标题 */
  title?: string
  /** 词云形状 */
  shape?: WordCloudShape
  /** 颜色范围 */
  colorRange?: string[]
  /** 最小字体大小 */
  minFontSize?: number
  /** 最大字体大小 */
  maxFontSize?: number
  /** 字体 */
  fontFamily?: string
  /** 字体粗细 */
  fontWeight?: 'normal' | 'bold' | 'bolder' | number
  /** 网格大小（影响词语密度） */
  gridSize?: number
  /** 旋转角度范围 [min, max] */
  rotationRange?: [number, number]
  /** 旋转步进 */
  rotationStep?: number
  /** 是否允许词语超出边界 */
  drawOutOfBound?: boolean
  /** 布局动画 */
  layoutAnimation?: boolean
  /** 左侧位置 */
  left?: string | number
  /** 顶部位置 */
  top?: string | number
  /** 宽度 */
  width?: string | number
  /** 高度 */
  height?: string | number
}

/**
 * 默认配置
 */
const DEFAULT_OPTIONS: Required<WordCloudChartOptions> = {
  title: '',
  shape: 'circle',
  colorRange: ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2', '#eb2f96'],
  minFontSize: 12,
  maxFontSize: 60,
  fontFamily: 'sans-serif',
  fontWeight: 'bold',
  gridSize: 8,
  rotationRange: [-45, 45],
  rotationStep: 15,
  drawOutOfBound: false,
  layoutAnimation: true,
  left: 'center',
  top: 'center',
  width: '90%',
  height: '80%'
}

/**
 * 根据权重计算字体大小
 * @param value 当前值
 * @param minValue 最小值
 * @param maxValue 最大值
 * @param minFontSize 最小字体
 * @param maxFontSize 最大字体
 * @returns 字体大小
 */
function calculateFontSize(
  value: number,
  minValue: number,
  maxValue: number,
  minFontSize: number,
  maxFontSize: number
): number {
  if (maxValue === minValue) {
    return (minFontSize + maxFontSize) / 2
  }
  
  const ratio = (value - minValue) / (maxValue - minValue)
  return minFontSize + ratio * (maxFontSize - minFontSize)
}

/**
 * 从颜色范围中获取随机颜色
 * @param colorRange 颜色范围数组
 * @param index 索引
 * @returns 颜色值
 */
function getColorFromRange(colorRange: string[], index: number): string {
  return colorRange[index % colorRange.length]
}

/**
 * 构建词云图 ECharts 配置
 * @param data 词云数据
 * @param options 配置选项
 * @returns ECharts 配置对象
 */
export function buildWordCloudOption(
  data: WordCloudDataItem[],
  options: WordCloudChartOptions = {}
): EChartsOption {
  const mergedOptions = { ...DEFAULT_OPTIONS, ...options }
  const {
    title,
    shape,
    colorRange,
    minFontSize,
    maxFontSize,
    fontFamily,
    fontWeight,
    gridSize,
    rotationRange,
    rotationStep,
    drawOutOfBound,
    layoutAnimation,
    left,
    top,
    width,
    height
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

  // 计算数据范围
  const values = data.map(item => item.value)
  const minValue = Math.min(...values)
  const maxValue = Math.max(...values)

  // 处理数据，添加字体大小和颜色
  const processedData = data.map((item, index) => ({
    name: item.name,
    value: item.value,
    textStyle: {
      color: getColorFromRange(colorRange, index),
      fontFamily,
      fontWeight
    }
  }))

  // 按权重排序（大的在前，有助于布局）
  processedData.sort((a, b) => b.value - a.value)

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
      show: true,
      formatter: (params: any) => {
        return `${params.name}: ${params.value}`
      }
    },
    series: [{
      type: 'wordCloud',
      shape,
      left,
      top,
      width,
      height,
      gridSize,
      sizeRange: [minFontSize, maxFontSize],
      rotationRange,
      rotationStep,
      drawOutOfBound,
      layoutAnimation,
      textStyle: {
        fontFamily,
        fontWeight
      },
      emphasis: {
        focus: 'self',
        textStyle: {
          textShadowBlur: 10,
          textShadowColor: '#333'
        }
      },
      data: processedData
    }] as any
  }

  return option
}

/**
 * 验证词云数据完整性
 * 检查所有输入数据是否都在输出配置中
 * @param data 输入数据
 * @param option 生成的 ECharts 配置
 * @returns 是否完整
 */
export function validateWordCloudDataIntegrity(
  data: WordCloudDataItem[],
  option: EChartsOption
): boolean {
  if (!data || data.length === 0) {
    return true
  }

  const series = option.series as any[]
  if (!series || series.length === 0) {
    return false
  }

  const seriesData = series[0].data as WordCloudDataItem[]
  if (!seriesData) {
    return false
  }

  // 检查每个输入数据项是否都在输出中
  for (const item of data) {
    const found = seriesData.find(
      d => d.name === item.name && d.value === item.value
    )
    if (!found) {
      return false
    }
  }

  return true
}

/**
 * 从文本中提取词频数据
 * @param text 文本内容
 * @param minLength 最小词长度
 * @param topN 取前 N 个高频词
 * @returns 词云数据数组
 */
export function extractWordFrequency(
  text: string,
  minLength: number = 2,
  topN: number = 50
): WordCloudDataItem[] {
  if (!text) {
    return []
  }

  // 简单的中英文分词（实际项目中可能需要更复杂的分词库）
  const words = text
    .toLowerCase()
    .replace(/[^\u4e00-\u9fa5a-zA-Z0-9\s]/g, ' ')
    .split(/\s+/)
    .filter(word => word.length >= minLength)

  // 统计词频
  const frequency: Record<string, number> = {}
  for (const word of words) {
    frequency[word] = (frequency[word] || 0) + 1
  }

  // 转换为数组并排序
  const result = Object.entries(frequency)
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value)
    .slice(0, topN)

  return result
}

/**
 * 生成随机词云测试数据
 * @param count 数据数量
 * @param minValue 最小值
 * @param maxValue 最大值
 * @returns 词云数据数组
 */
export function generateRandomWordCloudData(
  count: number = 30,
  minValue: number = 10,
  maxValue: number = 100
): WordCloudDataItem[] {
  const sampleWords = [
    '数据分析', '可视化', '机器学习', '人工智能', '大数据',
    '云计算', '深度学习', '神经网络', '自然语言', '图像识别',
    '数据挖掘', '预测模型', '算法', '统计学', '数据科学',
    'Python', 'JavaScript', 'TypeScript', 'Vue', 'React',
    'ECharts', '图表', '仪表盘', '报表', '指标',
    '趋势', '分布', '相关性', '聚类', '分类'
  ]

  const result: WordCloudDataItem[] = []
  const usedWords = new Set<string>()

  for (let i = 0; i < Math.min(count, sampleWords.length); i++) {
    let word: string
    do {
      word = sampleWords[Math.floor(Math.random() * sampleWords.length)]
    } while (usedWords.has(word))
    
    usedWords.add(word)
    result.push({
      name: word,
      value: Math.floor(Math.random() * (maxValue - minValue + 1)) + minValue
    })
  }

  return result
}
