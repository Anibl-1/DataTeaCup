/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 仪表盘相关类型定义
 */

/**
 * 仪表盘统计数据
 */
export interface DashboardStats {
  /** 数据源总数 */
  dataSourceCount: number
  /** 采集任务数 */
  collectTaskCount: number
  /** 今日采集量 */
  todayCollectCount: number
  /** 用户总数 */
  userCount: number
  /** 报表数量 */
  reportCount?: number
  /** 图表数量 */
  chartCount?: number
  /** 本周采集量 */
  weekCollectCount?: number
  /** 成功率 */
  successRate?: string
}

/**
 * 图表组件配置
 */
export interface ChartWidgetConfig {
  /** 图表ID */
  chartId?: number
  /** 图表编码 */
  chartCode?: string
  /** 标题 */
  title?: string
  /** 刷新间隔（秒） */
  refreshInterval?: number
}

/**
 * KPI 卡片配置
 */
export interface KpiWidgetConfig {
  /** 标题 */
  title: string
  /** 数据源ID */
  dataSourceId: number
  /** SQL查询语句 */
  sql: string
  /** 值字段 */
  valueField: string
  /** 上期值字段（用于同比计算） */
  previousValueField?: string
  /** 上周期值字段（用于环比计算） */
  periodPreviousField?: string
  /** 单位 */
  unit?: string
  /** 前缀 */
  prefix?: string
  /** 后缀 */
  suffix?: string
  /** 趋势方向 */
  trend?: 'up' | 'down' | 'neutral'
  /** 值颜色 */
  color?: string
  /** 背景颜色 */
  backgroundColor?: string
}

/**
 * 文本组件配置
 */
export interface TextWidgetConfig {
  /** 文本内容 */
  content: string
  /** 字体大小 */
  fontSize?: number
  /** 文字颜色 */
  color?: string
  /** 背景颜色 */
  backgroundColor?: string
  /** 对齐方式 */
  align?: 'left' | 'center' | 'right'
}

/**
 * 筛选器组件配置
 */
export interface FilterWidgetConfig {
  /** 筛选器类型 */
  filterType: 'select' | 'date' | 'dateRange' | 'input'
  /** 标签 */
  label: string
  /** 字段名 */
  field: string
  /** 数据源ID */
  dataSourceId?: number
  /** 选项SQL */
  optionsSql?: string
  /** 默认值 */
  defaultValue?: any
  /** 关联的图表ID列表 */
  linkedCharts?: string[]
}

/**
 * 仪表盘组件类型
 */
export interface DashboardWidget {
  /** 唯一标识 */
  i: string
  /** 网格 x 坐标 */
  x: number
  /** 网格 y 坐标 */
  y: number
  /** 宽度（网格单位） */
  w: number
  /** 高度（网格单位） */
  h: number
  /** 组件类型 */
  type: 'chart' | 'kpi' | 'text' | 'filter'
  /** 组件配置 */
  config: ChartWidgetConfig | KpiWidgetConfig | TextWidgetConfig | FilterWidgetConfig
}

/**
 * 全局筛选器
 */
export interface GlobalFilter {
  /** 筛选器ID */
  id: string
  /** 筛选器类型 */
  type: 'select' | 'date' | 'dateRange' | 'input'
  /** 标签 */
  label: string
  /** 字段名 */
  field: string
  /** 当前值 */
  value?: any
  /** 关联的组件ID列表 */
  linkedWidgets: string[]
}

/**
 * 仪表盘布局配置
 */
export interface DashboardLayout {
  /** 仪表盘ID */
  id?: number
  /** 仪表盘名称 */
  name: string
  /** 描述 */
  description?: string
  /** 组件列表 */
  widgets: DashboardWidget[]
  /** 全局筛选器列表 */
  globalFilters: GlobalFilter[]
  /** 主题 */
  theme?: string
  /** 创建人ID */
  createBy?: number
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
  /** 状态：1-启用，0-禁用 */
  status?: number
}

/**
 * 仪表盘模板
 */
export interface DashboardTemplate {
  /** 模板ID */
  id: number
  /** 模板名称 */
  name: string
  /** 分类 */
  category: string
  /** 布局配置JSON */
  layoutJson: string
  /** 缩略图URL */
  thumbnail?: string
  /** 描述 */
  description?: string
}

/**
 * 图表联动配置
 */
export interface ChartLinkConfig {
  /** 配置唯一标识 */
  id?: string
  /** 源图表ID */
  sourceChartId: string
  /** 目标图表ID */
  targetChartId: string
  /** 源字段 */
  sourceField: string
  /** 目标字段 */
  targetField: string
  /** 联动类型 */
  linkType?: 'filter' | 'drillDown' | 'highlight'
}

/**
 * 下钻配置
 */
export interface DrillDownConfig {
  /** 下钻层级配置 */
  levels: Array<{ field: string; granularity: 'year' | 'quarter' | 'month' | 'day' }>
  /** 当前层级 */
  currentLevel: number
}
