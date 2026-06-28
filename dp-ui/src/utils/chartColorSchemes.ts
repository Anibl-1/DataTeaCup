/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 图表颜色方案配置
 * 修复颜色方案无效的问题
 */

export interface ColorScheme {
  name: string
  label: string
  colors: string[]
  description: string
}

// 预设颜色方案
export const COLOR_SCHEMES: Record<string, ColorScheme> = {
  default: {
    name: 'default',
    label: '默认',
    colors: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc'],
    description: 'ECharts 默认配色方案'
  },
  bright: {
    name: 'bright',
    label: '明亮',
    colors: ['#FF6B6B', '#4ECDC4', '#45B7D1', '#FFA07A', '#98D8C8', '#F7DC6F', '#BB8FCE', '#85C1E2', '#F8B739'],
    description: '鲜艳明亮的配色，适合展示型图表'
  },
  dark: {
    name: 'dark',
    label: '暗色',
    colors: ['#2C3E50', '#34495E', '#7F8C8D', '#95A5A6', '#BDC3C7', '#566573', '#5D6D7E', '#717D7E', '#808B96'],
    description: '深色系配色，适合深色背景'
  },
  business: {
    name: 'business',
    label: '商务',
    colors: ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728', '#9467bd', '#8c564b', '#e377c2', '#7f7f7f', '#bcbd22'],
    description: '专业商务配色，适合正式报告'
  },
  warm: {
    name: 'warm',
    label: '暖色调',
    colors: ['#E74C3C', '#E67E22', '#F39C12', '#F1C40F', '#D35400', '#E59866', '#F8C471', '#F9E79F', '#FAD7A0'],
    description: '温暖的色调，适合活力主题'
  },
  cool: {
    name: 'cool',
    label: '冷色调',
    colors: ['#3498DB', '#2ECC71', '#1ABC9C', '#9B59B6', '#34495E', '#5DADE2', '#52BE80', '#48C9B0', '#AF7AC5'],
    description: '清新冷色调，适合科技主题'
  },
  pastel: {
    name: 'pastel',
    label: '柔和',
    colors: ['#FFB6C1', '#B0E0E6', '#FFE4B5', '#E6E6FA', '#F0E68C', '#DDA0DD', '#F5DEB3', '#D8BFD8', '#FFDAB9'],
    description: '柔和淡雅的配色，适合简洁风格'
  },
  vibrant: {
    name: 'vibrant',
    label: '活力',
    colors: ['#FF1744', '#00E676', '#00B0FF', '#FFEA00', '#D500F9', '#FF6E40', '#76FF03', '#18FFFF', '#F50057'],
    description: '鲜艳活力的配色，适合年轻化设计'
  },
  earth: {
    name: 'earth',
    label: '大地',
    colors: ['#8D6E63', '#A1887F', '#BCAAA4', '#D7CCC8', '#6D4C41', '#5D4037', '#4E342E', '#3E2723', '#EFEBE9'],
    description: '大地色系，沉稳自然'
  },
  ocean: {
    name: 'ocean',
    label: '海洋',
    colors: ['#006064', '#00838F', '#0097A7', '#00ACC1', '#00BCD4', '#26C6DA', '#4DD0E1', '#80DEEA', '#B2EBF2'],
    description: '海洋色系，清新通透'
  },
  sunset: {
    name: 'sunset',
    label: '日落',
    colors: ['#FF5722', '#FF7043', '#FF8A65', '#FFAB91', '#FFCCBC', '#FF6E40', '#FF9E80', '#FFAB40', '#FFD180'],
    description: '日落色系，温暖渐变'
  },
  forest: {
    name: 'forest',
    label: '森林',
    colors: ['#1B5E20', '#2E7D32', '#388E3C', '#43A047', '#4CAF50', '#66BB6A', '#81C784', '#A5D6A7', '#C8E6C9'],
    description: '森林绿色系，自然清新'
  },
  purple: {
    name: 'purple',
    label: '紫罗兰',
    colors: ['#9C27B0', '#AB47BC', '#BA68C8', '#CE93D8', '#E1BEE7', '#8E24AA', '#7B1FA2', '#6A1B9A', '#4A148C'],
    description: '优雅的紫色系，适合创意设计'
  },
  neon: {
    name: 'neon',
    label: '霓虹',
    colors: ['#FF00FF', '#00FFFF', '#00FF00', '#FFFF00', '#FF0080', '#00FF80', '#8000FF', '#FF0040', '#00FFBF'],
    description: '霓虹荧光色，炫酷科技感'
  },
  vintage: {
    name: 'vintage',
    label: '复古',
    colors: ['#8B4513', '#A0522D', '#CD853F', '#DEB887', '#F4A460', '#D2691E', '#BC8F8F', '#CD5C5C', '#FA8072'],
    description: '复古怀旧配色'
  },
  modern: {
    name: 'modern',
    label: '现代',
    colors: ['#263238', '#37474F', '#455A64', '#546E7A', '#607D8B', '#78909C', '#90A4AE', '#B0BEC5', '#CFD8DC'],
    description: '简约现代风格'
  },
  candy: {
    name: 'candy',
    label: '糖果',
    colors: ['#FF69B4', '#FFB6C1', '#FFC0CB', '#FFE4E1', '#FFF0F5', '#DB7093', '#C71585', '#FF1493', '#FF69B4'],
    description: '甜美糖果色系'
  },
  tech: {
    name: 'tech',
    label: '科技蓝',
    colors: ['#0D47A1', '#1565C0', '#1976D2', '#1E88E5', '#2196F3', '#42A5F5', '#64B5F6', '#90CAF9', '#BBDEFB'],
    description: '科技感蓝色系'
  },
  autumn: {
    name: 'autumn',
    label: '秋日',
    colors: ['#FF6347', '#FF8C00', '#FFA500', '#FFD700', '#F0E68C', '#DEB887', '#D2691E', '#BC8F8F', '#CD853F'],
    description: '秋天的金黄色系'
  },
  spring: {
    name: 'spring',
    label: '春天',
    colors: ['#98FB98', '#90EE90', '#00FA9A', '#7FFF00', '#ADFF2F', '#9ACD32', '#00FF7F', '#00FF00', '#32CD32'],
    description: '春天的清新绿色'
  },
  rainbow: {
    name: 'rainbow',
    label: '彩虹',
    colors: ['#FF0000', '#FF7F00', '#FFFF00', '#00FF00', '#0000FF', '#4B0082', '#9400D3', '#FF1493', '#00CED1'],
    description: '彩虹七彩配色'
  },
  monochrome: {
    name: 'monochrome',
    label: '黑白',
    colors: ['#000000', '#2C2C2C', '#525252', '#737373', '#969696', '#B7B7B7', '#D4D4D4', '#E8E8E8', '#FFFFFF'],
    description: '经典黑白配色'
  },
  gradient: {
    name: 'gradient',
    label: '渐变蓝',
    colors: ['#667eea', '#764ba2', '#f093fb', '#4facfe', '#00f2fe', '#43e97b', '#38f9d7', '#fa709a', '#fee140'],
    description: '流行的渐变色系'
  }
}

// 获取颜色方案列表
export const getColorSchemeOptions = () => {
  return Object.values(COLOR_SCHEMES).map(scheme => ({
    label: scheme.label,
    value: scheme.name,
    colors: scheme.colors
  }))
}

// 获取颜色方案
export const getColorScheme = (name: string): ColorScheme => {
  return COLOR_SCHEMES[name] || COLOR_SCHEMES.default
}

// 应用颜色方案到 ECharts option
export const applyColorScheme = (
  option: any,
  schemeName: string,
  customColor?: string
): any => {
  if (!option) return option

  // 如果是自定义颜色
  if (schemeName === 'custom' && customColor) {
    option.color = [customColor]
    return option
  }

  // 获取预设颜色方案
  const scheme = getColorScheme(schemeName)
  option.color = scheme.colors

  // 应用到所有系列
  if (option.series && Array.isArray(option.series)) {
    option.series.forEach((series: any, index: number) => {
      if (!series.itemStyle) {
        series.itemStyle = {}
      }
      // 循环使用颜色
      const colorIndex = index % scheme.colors.length
      series.itemStyle.color = scheme.colors[colorIndex]
    })
  }

  return option
}

// 生成渐变色
export const generateGradientColors = (
  baseColor: string,
  count: number = 5
): string[] => {
  // 简单的渐变色生成逻辑
  // 实际项目中可以使用更复杂的颜色处理库
  const colors: string[] = []
  
  // 将 hex 转 rgb
  const hex = baseColor.replace('#', '')
  const r = parseInt(hex.substring(0, 2), 16)
  const g = parseInt(hex.substring(2, 4), 16)
  const b = parseInt(hex.substring(4, 6), 16)

  for (let i = 0; i < count; i++) {
    const factor = 1 - (i / count) * 0.3 // 逐渐变暗
    const newR = Math.floor(r * factor)
    const newG = Math.floor(g * factor)
    const newB = Math.floor(b * factor)
    colors.push(`rgb(${newR}, ${newG}, ${newB})`)
  }

  return colors
}

// 根据背景色自动选择合适的颜色方案
export const suggestColorScheme = (backgroundColor: string): string => {
  // 简单判断：深色背景用亮色方案，浅色背景用默认或暗色方案
  const hex = backgroundColor.replace('#', '')
  const r = parseInt(hex.substring(0, 2), 16)
  const g = parseInt(hex.substring(2, 4), 16)
  const b = parseInt(hex.substring(4, 6), 16)
  const brightness = (r * 299 + g * 587 + b * 114) / 1000

  if (brightness < 128) {
    // 深色背景
    return 'bright'
  } else {
    // 浅色背景
    return 'default'
  }
}

// 导出默认配色
export default COLOR_SCHEMES
