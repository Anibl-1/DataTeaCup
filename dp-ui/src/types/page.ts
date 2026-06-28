/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 页面相关类型定义
 */

import type { PageParameterPanel } from './pageParameter'

/**
 * 页面主题配置
 */
export interface PageTheme {
  /** 主题名称 */
  name: string
  /** 主题值 */
  value: string
  /** 主题描述 */
  description?: string
  /** 背景颜色 */
  backgroundColor?: string
  /** 背景渐变 */
  backgroundGradient?: string
  /** 文字颜色 */
  textColor?: string
  /** 次要文字颜色 */
  subTextColor?: string
  /** 主色调 */
  primaryColor?: string
  /** 次要颜色 */
  secondaryColor?: string
  /** 边框颜色 */
  borderColor?: string
  /** 卡片背景色 */
  cardBackgroundColor?: string
  /** 卡片边框色 */
  cardBorderColor?: string
  /** 图表配色方案 */
  chartColorScheme?: string[]
  /** 是否为深色主题 */
  isDark?: boolean
  /** 毛玻璃效果 */
  glassEffect?: boolean
  /** 发光效果 */
  glowEffect?: boolean
}

/**
 * 页面主题选项
 */
export const PAGE_THEMES: PageTheme[] = [
  {
    name: '默认主题',
    value: 'default',
    description: '简洁清爽的白色主题',
    backgroundColor: '#f5f7fa',
    backgroundGradient: 'linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%)',
    textColor: '#333333',
    subTextColor: '#666666',
    primaryColor: '#18a058',
    secondaryColor: '#f5f5f5',
    borderColor: '#e8e8e8',
    cardBackgroundColor: '#ffffff',
    cardBorderColor: '#e8e8e8',
    chartColorScheme: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc'],
    isDark: false,
    glassEffect: false,
    glowEffect: false
  },
  {
    name: '深色主题',
    value: 'dark',
    description: '护眼深色主题',
    backgroundColor: '#1e1e1e',
    backgroundGradient: 'linear-gradient(135deg, #1e1e1e 0%, #2d2d2d 100%)',
    textColor: '#ffffff',
    subTextColor: '#a0a0a0',
    primaryColor: '#18a058',
    secondaryColor: '#2d2d2d',
    borderColor: '#404040',
    cardBackgroundColor: '#2d2d2d',
    cardBorderColor: '#404040',
    chartColorScheme: ['#4992ff', '#7cffb2', '#fddd60', '#ff6e76', '#58d9f9', '#05c091', '#ff8a45', '#8d98b3', '#e690d1'],
    isDark: true,
    glassEffect: false,
    glowEffect: false
  },
  {
    name: '浅色主题',
    value: 'light',
    description: '柔和浅色主题',
    backgroundColor: '#f8f9fa',
    backgroundGradient: 'linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%)',
    textColor: '#212529',
    subTextColor: '#6c757d',
    primaryColor: '#007bff',
    secondaryColor: '#e9ecef',
    borderColor: '#dee2e6',
    cardBackgroundColor: '#ffffff',
    cardBorderColor: '#dee2e6',
    chartColorScheme: ['#4dabf7', '#69db7c', '#ffd43b', '#ff6b6b', '#74c0fc', '#51cf66', '#ffa94d', '#9775fa', '#f783ac'],
    isDark: false,
    glassEffect: false,
    glowEffect: false
  },
  {
    name: '商务主题',
    value: 'business',
    description: '专业商务风格',
    backgroundColor: '#ffffff',
    backgroundGradient: 'linear-gradient(135deg, #ffffff 0%, #f0f2f5 100%)',
    textColor: '#1a1a1a',
    subTextColor: '#666666',
    primaryColor: '#1890ff',
    secondaryColor: '#f0f2f5',
    borderColor: '#d9d9d9',
    cardBackgroundColor: '#ffffff',
    cardBorderColor: '#d9d9d9',
    chartColorScheme: ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2', '#eb2f96', '#fa8c16', '#2f54eb'],
    isDark: false,
    glassEffect: false,
    glowEffect: false
  },
  {
    name: '科技主题',
    value: 'tech',
    description: '炫酷科技风格，毛玻璃效果',
    backgroundColor: '#0f0f23',
    backgroundGradient: 'linear-gradient(135deg, #0f0f23 0%, #1a1a3e 50%, #0d0d1f 100%)',
    textColor: '#e0e0e0',
    subTextColor: '#a0a0a0',
    primaryColor: '#667eea',
    secondaryColor: '#1a1a3e',
    borderColor: 'rgba(255, 255, 255, 0.08)',
    cardBackgroundColor: 'rgba(255, 255, 255, 0.03)',
    cardBorderColor: 'rgba(255, 255, 255, 0.08)',
    chartColorScheme: ['#667eea', '#00ff88', '#00d4ff', '#ffd700', '#ff6b6b', '#4ecdc4', '#f9ca24', '#a29bfe', '#fd79a8'],
    isDark: true,
    glassEffect: true,
    glowEffect: true
  },
  {
    name: '霓虹主题',
    value: 'neon',
    description: '赛博朋克霓虹风格',
    backgroundColor: '#0a0a0a',
    backgroundGradient: 'linear-gradient(135deg, #0a0a0a 0%, #1a0a2e 50%, #0a1a1a 100%)',
    textColor: '#ffffff',
    subTextColor: '#b0b0b0',
    primaryColor: '#ff00ff',
    secondaryColor: '#1a1a2e',
    borderColor: 'rgba(255, 0, 255, 0.2)',
    cardBackgroundColor: 'rgba(26, 26, 46, 0.8)',
    cardBorderColor: 'rgba(255, 0, 255, 0.3)',
    chartColorScheme: ['#ff00ff', '#00ffff', '#00ff00', '#ffff00', '#ff0080', '#00ff80', '#8000ff', '#ff0040', '#00ffbf'],
    isDark: true,
    glassEffect: true,
    glowEffect: true
  },
  {
    name: '大屏深蓝',
    value: 'bigscreen-dark',
    description: '专业数据大屏深蓝主题',
    backgroundColor: '#0c1527',
    backgroundGradient: 'linear-gradient(135deg, #0c1527 0%, #162d50 50%, #0a1628 100%)',
    textColor: '#e0ecff',
    subTextColor: '#8ca8c8',
    primaryColor: '#00d4ff',
    secondaryColor: '#0e1e36',
    borderColor: 'rgba(64, 158, 255, 0.3)',
    cardBackgroundColor: 'rgba(6, 30, 60, 0.8)',
    cardBorderColor: 'rgba(64, 158, 255, 0.4)',
    chartColorScheme: ['#00d4ff', '#00ffcc', '#ffd700', '#ff6b6b', '#36cfc9', '#5b8ff9', '#ff9a45', '#a78bfa', '#f472b6'],
    isDark: true,
    glassEffect: false,
    glowEffect: true
  },
  {
    name: '大屏科幻',
    value: 'bigscreen-scifi',
    description: '科幻风格大屏主题，适合高端展示',
    backgroundColor: '#050d1a',
    backgroundGradient: 'linear-gradient(135deg, #050d1a 0%, #0a1930 50%, #071422 100%)',
    textColor: '#c8e6ff',
    subTextColor: '#5b8fae',
    primaryColor: '#00ffcc',
    secondaryColor: '#0a1930',
    borderColor: 'rgba(0, 255, 204, 0.2)',
    cardBackgroundColor: 'rgba(0, 20, 40, 0.7)',
    cardBorderColor: 'rgba(0, 255, 204, 0.3)',
    chartColorScheme: ['#00ffcc', '#00d4ff', '#7b68ee', '#ffa500', '#ff4757', '#2ed573', '#1e90ff', '#ff6b81', '#70a1ff'],
    isDark: true,
    glassEffect: true,
    glowEffect: true
  }
]

/**
 * 页面布局模式
 */
export type PageLayoutMode = 'desktop' | 'mobile' | 'bigscreen'

/**
 * 大屏分辨率预设
 */
export interface BigscreenResolution {
  /** 宽度 */
  width: number
  /** 高度 */
  height: number
  /** 标签 */
  label: string
}

/**
 * 大屏分辨率预设列表
 */
export const BIGSCREEN_RESOLUTIONS: BigscreenResolution[] = [
  { width: 1920, height: 1080, label: '1080P (1920×1080)' },
  { width: 2560, height: 1440, label: '2K (2560×1440)' },
  { width: 3840, height: 2160, label: '4K (3840×2160)' },
  { width: 1920, height: 720, label: '超宽 (1920×720)' },
  { width: 3840, height: 1080, label: '双屏 (3840×1080)' },
  { width: 5760, height: 1080, label: '三屏 (5760×1080)' }
]

/**
 * 大屏缩放模式
 */
export type BigscreenScaleMode = 'fit' | 'fill' | 'width' | 'height' | 'none'

/**
 * 大屏配置
 */
export interface BigscreenConfig {
  /** 设计分辨率宽度 */
  designWidth: number
  /** 设计分辨率高度 */
  designHeight: number
  /** 缩放模式: fit-等比适配 fill-铺满 width-宽度适配 height-高度适配 none-不缩放 */
  scaleMode: BigscreenScaleMode
  /** 自动轮播: 多页面时自动切换 */
  autoCarousel?: boolean
  /** 轮播间隔(秒) */
  carouselInterval?: number
  /** 自动刷新间隔(秒), 0=不刷新 */
  refreshInterval?: number
  /** 背景类型: color / gradient / image */
  backgroundType?: 'color' | 'gradient' | 'image'
  /** 背景值 */
  backgroundValue?: string
  /** 是否显示页头 */
  showHeader?: boolean
  /** 页头高度(px) */
  headerHeight?: number
  /** 页头标题 */
  headerTitle?: string
  /** 是否显示时钟 */
  showClock?: boolean
  /** 是否显示全屏按钮 */
  showFullscreen?: boolean
  /** 网格吸附 */
  gridSnap?: boolean
  /** 网格大小 */
  gridSize?: number
}

/**
 * 移动端布局配置
 */
export interface MobileLayoutConfig {
  /** 布局模式: single-单列 cards-卡片 tabs-标签页 */
  layoutType: 'single' | 'cards' | 'tabs'
  /** 卡片间距(px) */
  cardGap?: number
  /** 卡片圆角(px) */
  cardRadius?: number
  /** 是否显示标题栏 */
  showTitleBar?: boolean
  /** 下拉刷新 */
  pullRefresh?: boolean
  /** 自动刷新间隔(秒) */
  refreshInterval?: number
}

/**
 * 大屏项目 (多个大屏页面的集合)
 */
export interface BigscreenProject {
  /** 项目ID */
  id?: number
  /** 项目名称 */
  projectName: string
  /** 项目编码 */
  projectCode: string
  /** 项目描述 */
  description?: string
  /** 封面图 */
  coverImage?: string
  /** 状态: 1-启用 0-禁用 */
  status?: number
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
  /** 关联的页面ID列表 */
  pageIds?: number[]
  /** 关联的页面列表 */
  pages?: PageDefinition[]
  /** 默认大屏配置(新页面继承) */
  defaultConfig?: BigscreenConfig
}

/**
 * 创建默认大屏配置
 */
export function createDefaultBigscreenConfig(): BigscreenConfig {
  return {
    designWidth: 1920,
    designHeight: 1080,
    scaleMode: 'fit',
    autoCarousel: false,
    carouselInterval: 30,
    refreshInterval: 0,
    backgroundType: 'gradient',
    backgroundValue: 'linear-gradient(135deg, #0f0f23 0%, #1a1a3e 50%, #0d0d1f 100%)',
    showHeader: true,
    headerHeight: 64,
    headerTitle: '',
    showClock: true,
    showFullscreen: true,
    gridSnap: true,
    gridSize: 8
  }
}

/**
 * 创建默认移动端布局配置
 */
export function createDefaultMobileLayoutConfig(): MobileLayoutConfig {
  return {
    layoutType: 'cards',
    cardGap: 12,
    cardRadius: 12,
    showTitleBar: true,
    pullRefresh: true,
    refreshInterval: 0
  }
}

/**
 * 页面定义
 */
export interface PageDefinition {
  /** 页面ID */
  id?: number
  /** 页面名称 */
  pageName: string
  /** 页面编码 */
  pageCode: string
  /** 布局配置（JSON格式） */
  layoutConfig?: string
  /** 页面描述 */
  description?: string
  /** 页面主题 */
  theme?: string
  /** 主题配置（JSON格式，自定义主题时使用） */
  themeConfig?: string
  /** 状态：1-启用，0-禁用 */
  status?: number
  /** 创建人ID */
  createBy?: number
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
  /** 页面图表列表 */
  charts?: PageChart[]
  /** 参数面板配置 */
  parameterPanel?: PageParameterPanel
  /** 是否发布到移动端：1-是，0-否 */
  mobileEnabled?: number
  /** 移动端排序 */
  mobileSortOrder?: number
  /** 布局模式: desktop-桌面端 mobile-移动端 bigscreen-大屏 */
  layoutMode?: PageLayoutMode
  /** 大屏配置(layoutMode='bigscreen'时使用) */
  bigscreenConfig?: BigscreenConfig
  /** 移动端布局配置(layoutMode='mobile'时使用) */
  mobileLayoutConfig?: MobileLayoutConfig
  /** 所属大屏项目ID */
  projectId?: number
}

/**
 * 内联图表字段映射
 */
export interface InlineFieldMapping {
  /** X轴/维度字段 */
  xAxis?: string
  /** Y轴/指标字段（可多个） */
  yAxis?: string | string[]
  /** 分组字段 */
  group?: string
  /** 名称字段（饼图等） */
  nameField?: string
  /** 值字段 */
  valueField?: string
}

/**
 * 内联图表配置（页面私有图表）
 */
export interface InlineChartConfig {
  /** 图表名称 */
  chartName: string
  /** 图表类型 */
  chartType: string
  /** 数据源ID */
  dataSourceId?: number
  /** SQL查询语句 */
  sqlContent?: string
  /** ECharts JSON 配置 */
  chartConfig?: string
  /** 字段映射 */
  fieldMapping?: InlineFieldMapping
  /** 查询参数 */
  parameters?: import('./chart').ChartParameter[]
  /** 配色方案 */
  colorScheme?: string
  /** 图表描述 */
  description?: string
  /** 数据限制 */
  dataLimit?: number
}

/**
 * 页面图表关联
 */
export interface PageChart {
  /** 关联ID */
  id?: number
  /** 页面ID */
  pageId?: number
  /** 图表ID（引用模式使用） */
  chartId?: number
  /** 图表模式：inline-内联(页面私有), referenced-引用(公共图表), static-静态组件 */
  mode?: 'inline' | 'referenced' | 'static'
  /** 内联图表配置（mode='inline' 时使用） */
  inlineConfig?: InlineChartConfig
  /** 静态组件类型（mode='static' 时使用）：kpiCard, text, divider, image, headerBar, numberFlipper, decorBorder, progressBar, countdown, scrollList, marquee */
  staticType?: string
  /** 静态组件配置（mode='static' 时使用） */
  staticConfig?: any
  /** X坐标（网格位置，兼容旧格式） */
  x?: number
  /** Y坐标（网格位置，兼容旧格式） */
  y?: number
  /** 宽度（网格单位，12列网格，兼容旧格式） */
  w?: number
  /** 高度（网格单位，兼容旧格式） */
  h?: number
  /** 左坐标（像素，新格式） */
  left?: number
  /** 上坐标（像素，新格式） */
  top?: number
  /** 宽度（像素，新格式） */
  width?: number
  /** 高度（像素，新格式） */
  height?: number
  /** 排序顺序 */
  sortOrder?: number
  /** 图表信息（关联查询） */
  chart?: any
}

/**
 * 创建默认内联图表配置
 */
export function createDefaultInlineConfig(chartType: string): InlineChartConfig {
  return {
    chartName: `新建${getChartTypeLabel(chartType)}`,
    chartType,
    colorScheme: 'default',
    dataLimit: 1000
  }
}

/**
 * 获取图表类型中文标签
 */
export function getChartTypeLabel(chartType: string): string {
  const map: Record<string, string> = {
    line: '折线图', bar: '柱状图', pie: '饼图', table: '数据表格',
    summaryTable: '汇总表', pivotTable: '透视表', scatter: '散点图',
    radar: '雷达图', gauge: '仪表盘', funnel: '漏斗图', heatmap: '热力图',
    map: '地图', tree: '树图', sankey: '桑基图', parallel: '平行坐标',
    boxplot: '盒须图', candlestick: 'K线图', graph: '关系图'
  }
  return map[chartType] || chartType
}

