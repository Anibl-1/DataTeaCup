/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * ECharts 图表主题配置
 * 提供 6 套预设主题，支持主题一键切换
 * 验证需求: 3.2, 3.3
 */

import echarts from '@/utils/echarts'

/**
 * 图表主题配置接口
 * 定义主题的完整配置结构
 */
export interface ChartThemeConfig {
  /** 主题名称 */
  name: string
  /** 主题显示标签 */
  label: string
  /** 主题描述 */
  description: string
  /** 主色调数组 */
  colors: string[]
  /** 背景颜色 */
  backgroundColor: string
  /** 文本颜色 */
  textColor: string
  /** 坐标轴颜色 */
  axisColor: string
  /** 网格线颜色 */
  gridColor: string
  /** 图例文本颜色 */
  legendColor: string
  /** 标题颜色 */
  titleColor: string
  /** 副标题颜色 */
  subtitleColor: string
  /** 提示框配置 */
  tooltip: {
    backgroundColor: string
    borderColor: string
    textColor: string
  }
}

/**
 * 主题切换选项
 */
export interface ThemeSwitchOptions {
  /** 是否使用动画过渡 */
  animation?: boolean
  /** 动画持续时间（毫秒），默认 150ms 以确保在 200ms 内完成 */
  animationDuration?: number
}

// ============================================================================
// 主题 1: 默认主题 (Default)
// 清新简洁的默认配色，适合大多数场景
// ============================================================================
export const defaultTheme: ChartThemeConfig = {
  name: 'default',
  label: '默认',
  description: '清新简洁的默认配色，适合大多数场景',
  colors: [
    '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
    '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc'
  ],
  backgroundColor: '#ffffff',
  textColor: '#333333',
  axisColor: '#6E7079',
  gridColor: '#E0E6F1',
  legendColor: '#333333',
  titleColor: '#464646',
  subtitleColor: '#6E7079',
  tooltip: {
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    borderColor: '#ccc',
    textColor: '#333333'
  }
}

// ============================================================================
// 主题 2: 商务主题 (Business)
// 专业稳重的商务配色，适合企业报表
// ============================================================================
export const businessTheme: ChartThemeConfig = {
  name: 'business',
  label: '商务',
  description: '专业稳重的商务配色，适合企业报表',
  colors: [
    '#2f4554', '#61a0a8', '#d48265', '#91c7ae', '#749f83',
    '#ca8622', '#bda29a', '#6e7074', '#546570', '#c4ccd3'
  ],
  backgroundColor: '#ffffff',
  textColor: '#333333',
  axisColor: '#333333',
  gridColor: '#cccccc',
  legendColor: '#333333',
  titleColor: '#333333',
  subtitleColor: '#aaaaaa',
  tooltip: {
    backgroundColor: 'rgba(50, 50, 50, 0.9)',
    borderColor: '#333',
    textColor: '#ffffff'
  }
}

// ============================================================================
// 主题 3: 科技主题 (Tech)
// 现代科技感配色，适合数据大屏和技术报告
// ============================================================================
export const techTheme: ChartThemeConfig = {
  name: 'tech',
  label: '科技',
  description: '现代科技感配色，适合数据大屏和技术报告',
  colors: [
    '#00d4ff', '#00ff88', '#ff6b6b', '#ffd93d', '#6bcb77',
    '#4d96ff', '#ff6b9d', '#c56cf0', '#ff9f43', '#54a0ff'
  ],
  backgroundColor: '#0d1117',
  textColor: '#c9d1d9',
  axisColor: '#484f58',
  gridColor: '#21262d',
  legendColor: '#c9d1d9',
  titleColor: '#f0f6fc',
  subtitleColor: '#8b949e',
  tooltip: {
    backgroundColor: 'rgba(22, 27, 34, 0.95)',
    borderColor: '#30363d',
    textColor: '#c9d1d9'
  }
}

// ============================================================================
// 主题 4: 暖色主题 (Warm)
// 温暖舒适的暖色调，适合营销和用户体验报告
// ============================================================================
export const warmTheme: ChartThemeConfig = {
  name: 'warm',
  label: '暖色',
  description: '温暖舒适的暖色调，适合营销和用户体验报告',
  colors: [
    '#FF6B6B', '#FF8E53', '#FFA726', '#FFD54F', '#FFAB91',
    '#FF7043', '#F4511E', '#E64A19', '#D84315', '#BF360C'
  ],
  backgroundColor: '#FFF8F0',
  textColor: '#5D4037',
  axisColor: '#8D6E63',
  gridColor: '#EFEBE9',
  legendColor: '#5D4037',
  titleColor: '#4E342E',
  subtitleColor: '#8D6E63',
  tooltip: {
    backgroundColor: 'rgba(255, 248, 240, 0.95)',
    borderColor: '#FFCCBC',
    textColor: '#5D4037'
  }
}

// ============================================================================
// 主题 5: 冷色主题 (Cool)
// 清爽冷静的冷色调，适合数据分析和科学报告
// ============================================================================
export const coolTheme: ChartThemeConfig = {
  name: 'cool',
  label: '冷色',
  description: '清爽冷静的冷色调，适合数据分析和科学报告',
  colors: [
    '#3498DB', '#2ECC71', '#1ABC9C', '#9B59B6', '#34495E',
    '#16A085', '#27AE60', '#2980B9', '#8E44AD', '#2C3E50'
  ],
  backgroundColor: '#F5F7FA',
  textColor: '#2C3E50',
  axisColor: '#7F8C8D',
  gridColor: '#ECF0F1',
  legendColor: '#2C3E50',
  titleColor: '#2C3E50',
  subtitleColor: '#7F8C8D',
  tooltip: {
    backgroundColor: 'rgba(245, 247, 250, 0.95)',
    borderColor: '#BDC3C7',
    textColor: '#2C3E50'
  }
}

// ============================================================================
// 主题 6: 暗黑主题 (Dark)
// 深色背景配柔和色彩，适合夜间模式，文字清晰不刺眼
// ============================================================================
export const darkTheme: ChartThemeConfig = {
  name: 'dark',
  label: '暗黑',
  description: '深色背景配柔和色彩，适合夜间模式，文字清晰不刺眼',
  colors: [
    '#60A5FA', '#34D399', '#FBBF24', '#F472B6', '#A78BFA',
    '#38BDF8', '#4ADE80', '#FB923C', '#E879F9', '#818CF8'
  ],
  backgroundColor: '#0f172a',
  textColor: '#e2e8f0',
  axisColor: '#64748b',
  gridColor: '#334155',
  legendColor: '#cbd5e1',
  titleColor: '#f1f5f9',
  subtitleColor: '#94a3b8',
  tooltip: {
    backgroundColor: 'rgba(30, 41, 59, 0.95)',
    borderColor: '#475569',
    textColor: '#f1f5f9'
  }
}

// ============================================================================
// 主题集合
// ============================================================================

/**
 * 所有可用主题的映射
 */
export const chartThemes: Record<string, ChartThemeConfig> = {
  default: defaultTheme,
  business: businessTheme,
  tech: techTheme,
  warm: warmTheme,
  cool: coolTheme,
  dark: darkTheme
}

/**
 * 主题选项列表（用于下拉选择）
 */
export const themeOptions = Object.values(chartThemes).map(theme => ({
  label: theme.label,
  value: theme.name,
  description: theme.description
}))

// ============================================================================
// 主题转换为 ECharts 格式
// ============================================================================

/**
 * 将主题配置转换为 ECharts 主题格式
 * @param theme 主题配置
 * @returns ECharts 主题对象
 */
export function convertToEChartsTheme(theme: ChartThemeConfig): Record<string, any> {
  return {
    color: theme.colors,
    backgroundColor: theme.backgroundColor === '#ffffff' ? 'transparent' : theme.backgroundColor,
    textStyle: {
      color: theme.textColor
    },
    title: {
      textStyle: { color: theme.titleColor },
      subtextStyle: { color: theme.subtitleColor }
    },
    line: {
      itemStyle: { borderWidth: 1 },
      lineStyle: { width: 2 },
      symbolSize: 4,
      symbol: 'emptyCircle',
      smooth: false
    },
    radar: {
      itemStyle: { borderWidth: 1 },
      lineStyle: { width: 2 },
      symbolSize: 4,
      symbol: 'emptyCircle',
      smooth: false
    },
    bar: {
      itemStyle: { barBorderWidth: 0, barBorderColor: theme.gridColor }
    },
    pie: {
      itemStyle: { borderWidth: 0, borderColor: theme.gridColor }
    },
    scatter: {
      itemStyle: { borderWidth: 0, borderColor: theme.gridColor }
    },
    boxplot: {
      itemStyle: { borderWidth: 0, borderColor: theme.gridColor }
    },
    parallel: {
      itemStyle: { borderWidth: 0, borderColor: theme.gridColor }
    },
    sankey: {
      itemStyle: { borderWidth: 0, borderColor: theme.gridColor }
    },
    funnel: {
      itemStyle: { borderWidth: 0, borderColor: theme.gridColor }
    },
    gauge: {
      itemStyle: { borderWidth: 0, borderColor: theme.gridColor }
    },
    candlestick: {
      itemStyle: {
        color: theme.colors[0],
        color0: theme.colors[1],
        borderColor: theme.colors[0],
        borderColor0: theme.colors[1],
        borderWidth: 1
      }
    },
    graph: {
      itemStyle: { borderWidth: 0, borderColor: theme.gridColor },
      lineStyle: { width: 1, color: theme.axisColor },
      symbolSize: 4,
      symbol: 'emptyCircle',
      smooth: false,
      color: theme.colors,
      label: { color: theme.textColor }
    },
    map: {
      itemStyle: {
        areaColor: theme.backgroundColor === '#ffffff' ? '#eee' : theme.gridColor,
        borderColor: theme.axisColor,
        borderWidth: 0.5
      },
      label: { color: theme.textColor },
      emphasis: {
        itemStyle: { 
          areaColor: 'rgba(255,215,0,0.8)', 
          borderColor: theme.axisColor, 
          borderWidth: 1 
        },
        label: { color: theme.titleColor }
      }
    },
    geo: {
      itemStyle: {
        areaColor: theme.backgroundColor === '#ffffff' ? '#eee' : theme.gridColor,
        borderColor: theme.axisColor,
        borderWidth: 0.5
      },
      label: { color: theme.textColor },
      emphasis: {
        itemStyle: { 
          areaColor: 'rgba(255,215,0,0.8)', 
          borderColor: theme.axisColor, 
          borderWidth: 1 
        },
        label: { color: theme.titleColor }
      }
    },
    categoryAxis: {
      axisLine: { show: true, lineStyle: { color: theme.axisColor } },
      axisTick: { show: true, lineStyle: { color: theme.axisColor } },
      axisLabel: { show: true, color: theme.axisColor },
      splitLine: { show: false, lineStyle: { color: [theme.gridColor] } },
      splitArea: { show: false, areaStyle: { color: ['rgba(250,250,250,0.2)', 'rgba(210,219,238,0.2)'] } }
    },
    valueAxis: {
      axisLine: { show: false, lineStyle: { color: theme.axisColor } },
      axisTick: { show: false, lineStyle: { color: theme.axisColor } },
      axisLabel: { show: true, color: theme.axisColor },
      splitLine: { show: true, lineStyle: { color: [theme.gridColor] } },
      splitArea: { show: false, areaStyle: { color: ['rgba(250,250,250,0.2)', 'rgba(210,219,238,0.2)'] } }
    },
    logAxis: {
      axisLine: { show: false, lineStyle: { color: theme.axisColor } },
      axisTick: { show: false, lineStyle: { color: theme.axisColor } },
      axisLabel: { show: true, color: theme.axisColor },
      splitLine: { show: true, lineStyle: { color: [theme.gridColor] } },
      splitArea: { show: false, areaStyle: { color: ['rgba(250,250,250,0.2)', 'rgba(210,219,238,0.2)'] } }
    },
    timeAxis: {
      axisLine: { show: true, lineStyle: { color: theme.axisColor } },
      axisTick: { show: true, lineStyle: { color: theme.axisColor } },
      axisLabel: { show: true, color: theme.axisColor },
      splitLine: { show: false, lineStyle: { color: [theme.gridColor] } },
      splitArea: { show: false, areaStyle: { color: ['rgba(250,250,250,0.2)', 'rgba(210,219,238,0.2)'] } }
    },
    toolbox: {
      iconStyle: { borderColor: theme.axisColor },
      emphasis: { iconStyle: { borderColor: theme.textColor } }
    },
    legend: { 
      textStyle: { color: theme.legendColor } 
    },
    tooltip: {
      backgroundColor: theme.tooltip.backgroundColor,
      borderColor: theme.tooltip.borderColor,
      textStyle: { color: theme.tooltip.textColor },
      axisPointer: {
        lineStyle: { color: theme.axisColor, width: 1 },
        crossStyle: { color: theme.axisColor, width: 1 }
      }
    },
    timeline: {
      lineStyle: { color: theme.gridColor, width: 2 },
      itemStyle: { color: theme.colors[0], borderWidth: 1 },
      controlStyle: {
        color: theme.colors[0],
        borderColor: theme.colors[0],
        borderWidth: 1
      },
      checkpointStyle: { color: theme.colors[0], borderColor: theme.backgroundColor },
      label: { color: theme.axisColor },
      emphasis: {
        itemStyle: { color: theme.colors[1] },
        controlStyle: { color: theme.colors[0], borderColor: theme.colors[0], borderWidth: 1 }
      }
    },
    visualMap: { 
      color: [theme.colors[0], theme.colors[2], theme.colors[4]] 
    },
    dataZoom: {
      handleSize: 'undefined%',
      textStyle: { color: theme.axisColor }
    },
    markPoint: {
      label: { color: theme.textColor },
      emphasis: { label: { color: theme.textColor } }
    }
  }
}

// ============================================================================
// 主题注册和管理
// ============================================================================

/** 已注册的主题名称集合 */
const registeredThemes = new Set<string>()

/**
 * 注册所有预设主题到 ECharts
 */
export function registerThemes(): void {
  Object.values(chartThemes).forEach(theme => {
    if (!registeredThemes.has(theme.name)) {
      const echartsTheme = convertToEChartsTheme(theme)
      echarts.registerTheme(theme.name, echartsTheme)
      registeredThemes.add(theme.name)
    }
  })
}

/**
 * 注册单个主题到 ECharts
 * @param theme 主题配置
 */
export function registerTheme(theme: ChartThemeConfig): void {
  if (!registeredThemes.has(theme.name)) {
    const echartsTheme = convertToEChartsTheme(theme)
    echarts.registerTheme(theme.name, echartsTheme)
    registeredThemes.add(theme.name)
  }
}

/**
 * 获取主题配置
 * @param themeName 主题名称
 * @returns 主题配置，如果不存在则返回默认主题
 */
export function getTheme(themeName: string): ChartThemeConfig {
  return chartThemes[themeName] || defaultTheme
}

/**
 * 获取主题的 ECharts 格式
 * @param themeName 主题名称
 * @returns ECharts 主题对象
 */
export function getEChartsTheme(themeName: string): Record<string, any> {
  const theme = getTheme(themeName)
  return convertToEChartsTheme(theme)
}

// ============================================================================
// 主题切换功能
// ============================================================================

/**
 * 切换图表主题
 * 确保主题切换在 200ms 内完成（需求 3.3）
 * 
 * @param chartInstance ECharts 实例
 * @param themeName 目标主题名称
 * @param options 切换选项
 * @returns Promise，在主题切换完成后 resolve
 */
export async function switchTheme(
  chartInstance: echarts.ECharts | null,
  themeName: string,
  options: ThemeSwitchOptions = {}
): Promise<void> {
  if (!chartInstance) {
    return
  }

  const { animation = false, animationDuration = 150 } = options
  const theme = getTheme(themeName)
  
  // 确保主题已注册
  registerTheme(theme)
  
  // 获取当前配置
  const currentOption = chartInstance.getOption()
  
  // 构建新的配置，应用主题颜色
  const newOption: echarts.EChartsOption = {
    // 禁用或使用短动画以确保在 200ms 内完成
    animation: animation,
    animationDuration: animation ? Math.min(animationDuration, 150) : 0,
    animationEasing: 'cubicOut',
    
    // 应用主题颜色
    color: theme.colors,
    backgroundColor: theme.backgroundColor === '#ffffff' ? 'transparent' : theme.backgroundColor,
    
    // 更新文本样式
    textStyle: {
      color: theme.textColor
    },
    
    // 更新标题样式
    title: Array.isArray(currentOption?.title) 
      ? currentOption.title.map((t: any) => ({
          ...t,
          textStyle: { ...t?.textStyle, color: theme.titleColor },
          subtextStyle: { ...t?.subtextStyle, color: theme.subtitleColor }
        }))
      : currentOption?.title 
        ? [{
            ...(currentOption.title as any),
            textStyle: { ...(currentOption.title as any)?.textStyle, color: theme.titleColor },
            subtextStyle: { ...(currentOption.title as any)?.subtextStyle, color: theme.subtitleColor }
          }]
        : undefined,
    
    // 更新图例样式
    legend: Array.isArray(currentOption?.legend)
      ? currentOption.legend.map((l: any) => ({
          ...l,
          textStyle: { ...l?.textStyle, color: theme.legendColor }
        }))
      : currentOption?.legend
        ? [{
            ...(currentOption.legend as any),
            textStyle: { ...(currentOption.legend as any)?.textStyle, color: theme.legendColor }
          }]
        : undefined,
    
    // 更新 X 轴样式
    xAxis: Array.isArray(currentOption?.xAxis)
      ? currentOption.xAxis.map((axis: any) => ({
          ...axis,
          axisLine: { ...axis?.axisLine, lineStyle: { ...axis?.axisLine?.lineStyle, color: theme.axisColor } },
          axisTick: { ...axis?.axisTick, lineStyle: { ...axis?.axisTick?.lineStyle, color: theme.axisColor } },
          axisLabel: { ...axis?.axisLabel, color: theme.axisColor },
          splitLine: { ...axis?.splitLine, lineStyle: { ...axis?.splitLine?.lineStyle, color: theme.gridColor } }
        }))
      : currentOption?.xAxis
        ? [{
            ...(currentOption.xAxis as any),
            axisLine: { ...(currentOption.xAxis as any)?.axisLine, lineStyle: { ...(currentOption.xAxis as any)?.axisLine?.lineStyle, color: theme.axisColor } },
            axisTick: { ...(currentOption.xAxis as any)?.axisTick, lineStyle: { ...(currentOption.xAxis as any)?.axisTick?.lineStyle, color: theme.axisColor } },
            axisLabel: { ...(currentOption.xAxis as any)?.axisLabel, color: theme.axisColor },
            splitLine: { ...(currentOption.xAxis as any)?.splitLine, lineStyle: { ...(currentOption.xAxis as any)?.splitLine?.lineStyle, color: theme.gridColor } }
          }]
        : undefined,
    
    // 更新 Y 轴样式
    yAxis: Array.isArray(currentOption?.yAxis)
      ? currentOption.yAxis.map((axis: any) => ({
          ...axis,
          axisLine: { ...axis?.axisLine, lineStyle: { ...axis?.axisLine?.lineStyle, color: theme.axisColor } },
          axisTick: { ...axis?.axisTick, lineStyle: { ...axis?.axisTick?.lineStyle, color: theme.axisColor } },
          axisLabel: { ...axis?.axisLabel, color: theme.axisColor },
          splitLine: { ...axis?.splitLine, lineStyle: { ...axis?.splitLine?.lineStyle, color: theme.gridColor } }
        }))
      : currentOption?.yAxis
        ? [{
            ...(currentOption.yAxis as any),
            axisLine: { ...(currentOption.yAxis as any)?.axisLine, lineStyle: { ...(currentOption.yAxis as any)?.axisLine?.lineStyle, color: theme.axisColor } },
            axisTick: { ...(currentOption.yAxis as any)?.axisTick, lineStyle: { ...(currentOption.yAxis as any)?.axisTick?.lineStyle, color: theme.axisColor } },
            axisLabel: { ...(currentOption.yAxis as any)?.axisLabel, color: theme.axisColor },
            splitLine: { ...(currentOption.yAxis as any)?.splitLine, lineStyle: { ...(currentOption.yAxis as any)?.splitLine?.lineStyle, color: theme.gridColor } }
          }]
        : undefined,
    
    // 更新提示框样式
    tooltip: currentOption?.tooltip
      ? {
          ...(currentOption.tooltip as any),
          backgroundColor: theme.tooltip.backgroundColor,
          borderColor: theme.tooltip.borderColor,
          textStyle: { ...(currentOption.tooltip as any)?.textStyle, color: theme.tooltip.textColor }
        }
      : undefined
  }
  
  // 应用新配置
  chartInstance.setOption(newOption, { notMerge: false, lazyUpdate: false })
  
  // 如果使用动画，等待动画完成
  if (animation && animationDuration > 0) {
    await new Promise(resolve => setTimeout(resolve, animationDuration))
  }
}

/**
 * 应用主题到图表选项
 * 用于在创建图表时直接应用主题配置
 * 
 * @param option 原始图表选项
 * @param themeName 主题名称
 * @returns 应用主题后的图表选项
 */
export function applyThemeToOption(
  option: echarts.EChartsOption,
  themeName: string
): echarts.EChartsOption {
  const theme = getTheme(themeName)
  
  return {
    ...option,
    color: theme.colors,
    backgroundColor: theme.backgroundColor === '#ffffff' ? 'transparent' : theme.backgroundColor,
    textStyle: {
      ...(option.textStyle as any),
      color: theme.textColor
    },
    title: option.title
      ? Array.isArray(option.title)
        ? option.title.map(t => ({
            ...t,
            textStyle: { ...t.textStyle, color: theme.titleColor },
            subtextStyle: { ...t.subtextStyle, color: theme.subtitleColor }
          }))
        : {
            ...option.title,
            textStyle: { ...(option.title as any).textStyle, color: theme.titleColor },
            subtextStyle: { ...(option.title as any).subtextStyle, color: theme.subtitleColor }
          }
      : undefined,
    legend: option.legend
      ? Array.isArray(option.legend)
        ? option.legend.map(l => ({
            ...l,
            textStyle: { ...l.textStyle, color: theme.legendColor }
          }))
        : {
            ...option.legend,
            textStyle: { ...(option.legend as any).textStyle, color: theme.legendColor }
          }
      : undefined,
    tooltip: option.tooltip
      ? {
          ...(option.tooltip as any),
          backgroundColor: theme.tooltip.backgroundColor,
          borderColor: theme.tooltip.borderColor,
          textStyle: { ...(option.tooltip as any).textStyle, color: theme.tooltip.textColor }
        }
      : undefined
  }
}

/**
 * 获取主题的预览颜色
 * 用于在主题选择器中显示颜色预览
 * 
 * @param themeName 主题名称
 * @returns 预览颜色数组（前 4 个主色）
 */
export function getThemePreviewColors(themeName: string): string[] {
  const theme = getTheme(themeName)
  return theme.colors.slice(0, 4)
}

/**
 * 检查主题是否为深色主题
 * @param themeName 主题名称
 * @returns 是否为深色主题
 */
export function isDarkTheme(themeName: string): boolean {
  const theme = getTheme(themeName)
  // 通过背景色亮度判断
  const bg = theme.backgroundColor
  if (bg === 'transparent' || bg === '#ffffff') return false
  
  // 简单的亮度检测
  const hex = bg.replace('#', '')
  const r = parseInt(hex.substr(0, 2), 16)
  const g = parseInt(hex.substr(2, 2), 16)
  const b = parseInt(hex.substr(4, 2), 16)
  const brightness = (r * 299 + g * 587 + b * 114) / 1000
  
  return brightness < 128
}

/**
 * 获取所有主题名称
 * @returns 主题名称数组
 */
export function getAllThemeNames(): string[] {
  return Object.keys(chartThemes)
}

/**
 * 获取所有主题配置
 * @returns 主题配置数组
 */
export function getAllThemes(): ChartThemeConfig[] {
  return Object.values(chartThemes)
}
