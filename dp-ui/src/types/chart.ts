/**
 * 图表相关类型定义
 */

/**
 * 图表参数类型
 */
export type ChartParameterType = 'text' | 'number' | 'date' | 'dateRange' | 'select' | 'multiSelect'

/**
 * 图表参数选项（用于下拉选择）
 */
export interface ChartParameterOption {
  label: string
  value: string | number
}

/**
 * 图表查询参数定义
 */
export interface ChartParameter {
  /** 关联的字段名 */
  field: string
  /** 运算符 */
  operator: '=' | '!=' | '>' | '>=' | '<' | '<=' | 'LIKE' | 'NOT LIKE' | 'IN' | 'NOT IN' | 'BETWEEN'
  /** 参数名称（自动生成，用于内部标识） */
  name: string
  /** 参数标签（显示名称） */
  label: string
  /** 参数类型 */
  type: ChartParameterType
  /** 是否必填 */
  required: boolean
  /** 默认值 */
  defaultValue?: string | number | string[] | [string, string] | null
  /** 日期快捷值预设 */
  datePreset?: string
  /** 下拉选项（type为select/multiSelect时使用） */
  options?: ChartParameterOption[]
  /** 选项来源：manual-手动配置，sql-SQL查询 */
  optionSource?: 'manual' | 'sql'
  /** 选项标签（用于动态标签输入） */
  optionTags?: string[]
  /** 选项SQL（optionSource为sql时使用） */
  optionSql?: string
  /** 占位提示文本 */
  placeholder?: string
  /** 参数描述 */
  description?: string
  /** 日期格式（type为date/dateRange时使用） */
  dateFormat?: string
  /** 最小值（type为number时使用） */
  min?: number
  /** 最大值（type为number时使用） */
  max?: number
}

/**
 * 图表参数值（运行时用户输入的值）
 */
export interface ChartParameterValue {
  [paramName: string]: string | number | string[] | [number, number] | null
}

/**
 * 图表定义
 */
export interface ChartDefinition {
  /** 图表ID */
  id: number
  /** 图表名称 */
  chartName: string
  /** 图表编码 */
  chartCode: string
  /** 图表类型 */
  chartType: string
  /** 数据源ID */
  dataSourceId: number
  /** SQL查询语句 */
  sqlContent: string
  /** 图表配置（JSON格式） */
  chartConfig?: string
  /** 图表描述 */
  description?: string
  /** 查询参数定义（JSON格式） */
  parameters?: ChartParameter[]
  /** 状态：1-启用，0-禁用 */
  status: number
  /** 创建人ID */
  createBy?: number
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime?: string
  /** 水印类型: none-无水印, user_ip-用户名_IP, custom-自定义文本 */
  watermarkType?: string
  /** PDF水印（自定义文本） */
  pdfWatermark?: string
  /** 允许导出Excel */
  allowExportExcel?: boolean
  /** 允许导出PDF */
  allowExportPdf?: boolean
  /** 是否发布到移动端：1-是，0-否 */
  mobileEnabled?: number
  /** 移动端排序 */
  mobileSortOrder?: number
  /** 移动端分类标签 */
  mobileCategory?: string
}

/**
 * 图表类型分类
 */
export type ChartCategory = 'basic' | 'advanced' | 'special' | 'geographic' | 'statistical' | 'financial' | 'relationship'

/**
 * 图表类型分类标签
 */
export const CHART_CATEGORY_LABELS: Record<ChartCategory, string> = {
  basic: '基础图表',
  advanced: '高级图表',
  special: '特殊图表',
  geographic: '地理图表',
  statistical: '统计图表',
  financial: '金融图表',
  relationship: '关系图表'
}

/**
 * 图表类型选项
 */
export const CHART_TYPES = [
  // 基础图表
  { label: '折线图', value: 'line', category: 'basic' as ChartCategory, icon: 'TrendingUpOutline' },
  { label: '柱状图', value: 'bar', category: 'basic' as ChartCategory, icon: 'BarChartOutline' },
  { label: '饼图', value: 'pie', category: 'basic' as ChartCategory, icon: 'PieChartOutline' },
  { label: '数据表格', value: 'table', category: 'basic' as ChartCategory, icon: 'GridOutline' },
  { label: '汇总表', value: 'summaryTable', category: 'basic' as ChartCategory, icon: 'CalculatorOutline' },
  { label: '透视表', value: 'pivotTable', category: 'basic' as ChartCategory, icon: 'AppsOutline' },
  { label: '散点图', value: 'scatter', category: 'basic' as ChartCategory, icon: 'StatsChartOutline' },
  { label: '雷达图', value: 'radar', category: 'basic' as ChartCategory, icon: 'PulseOutline' },
  
  // 高级图表
  { label: 'KPI 卡片', value: 'kpi', category: 'advanced' as ChartCategory, icon: 'SpeedometerOutline' },
  { label: '组合图', value: 'combo', category: 'advanced' as ChartCategory, icon: 'LayersOutline' },
  { label: '瀑布图', value: 'waterfall', category: 'advanced' as ChartCategory, icon: 'BarChartOutline' },
  { label: '词云图', value: 'wordCloud', category: 'advanced' as ChartCategory, icon: 'CloudOutline' },
  
  // 特殊图表
  { label: '仪表盘', value: 'gauge', category: 'special' as ChartCategory, icon: 'SpeedometerOutline' },
  { label: '漏斗图', value: 'funnel', category: 'special' as ChartCategory, icon: 'FunnelOutline' },
  { label: '热力图', value: 'heatmap', category: 'special' as ChartCategory, icon: 'GridOutline' },
  { label: '树图', value: 'tree', category: 'special' as ChartCategory, icon: 'GitBranchOutline' },
  { label: '桑基图', value: 'sankey', category: 'special' as ChartCategory, icon: 'GitMergeOutline' },
  { label: '平行坐标', value: 'parallel', category: 'special' as ChartCategory, icon: 'ResizeOutline' },
  
  // 地理图表
  { label: '中国地图', value: 'chinaMap', category: 'geographic' as ChartCategory, icon: 'MapOutline' },
  { label: '世界地图', value: 'worldMap', category: 'geographic' as ChartCategory, icon: 'GlobeOutline' },
  { label: '地图', value: 'map', category: 'geographic' as ChartCategory, icon: 'MapOutline' },
  
  // 统计图表
  { label: '盒须图', value: 'boxplot', category: 'statistical' as ChartCategory, icon: 'CubeOutline' },
  
  // 金融图表
  { label: 'K线图', value: 'candlestick', category: 'financial' as ChartCategory, icon: 'TrendingUpOutline' },
  
  // 关系图表
  { label: '关系图', value: 'graph', category: 'relationship' as ChartCategory, icon: 'ShareSocialOutline' }
] as const

/**
 * 按分类分组的图表类型
 */
export function getChartTypesByCategory(): Record<ChartCategory, typeof CHART_TYPES[number][]> {
  const grouped: Record<ChartCategory, typeof CHART_TYPES[number][]> = {
    basic: [],
    advanced: [],
    special: [],
    geographic: [],
    statistical: [],
    financial: [],
    relationship: []
  }
  
  for (const chartType of CHART_TYPES) {
    grouped[chartType.category].push(chartType)
  }
  
  return grouped
}

/**
 * 图表类型配置 - 定义每种图表类型需要的字段
 */
export interface ChartTypeConfig {
  /** 图表类型 */
  type: string
  /** 需要的X轴字段数量 */
  xAxisCount: number
  /** 需要的Y轴字段数量 */
  yAxisCount: number
  /** 是否需要分组字段 */
  needGroup: boolean
  /** 是否需要系列字段 */
  needSeries: boolean
  /** 特殊字段要求 */
  specialFields?: string[]
  /** 字段说明 */
  fieldDescription?: string
}

/**
 * 图表类型配置映射
 */
export const CHART_TYPE_CONFIGS: Record<string, ChartTypeConfig> = {
  line: {
    type: 'line',
    xAxisCount: 1,
    yAxisCount: 1,
    needGroup: false,
    needSeries: true
  },
  bar: {
    type: 'bar',
    xAxisCount: 1,
    yAxisCount: 1,
    needGroup: false,
    needSeries: true
  },
  pie: {
    type: 'pie',
    xAxisCount: 1,
    yAxisCount: 1,
    needGroup: false,
    needSeries: false
  },
  table: {
    type: 'table',
    xAxisCount: 0,
    yAxisCount: 0,
    needGroup: false,
    needSeries: false,
    fieldDescription: '表格类型会显示所有查询字段'
  },
  summaryTable: {
    type: 'summaryTable',
    xAxisCount: 0,
    yAxisCount: 0,
    needGroup: false,
    needSeries: false,
    fieldDescription: '汇总表支持底部汇总行，可对数值列进行求和、平均等计算'
  },
  pivotTable: {
    type: 'pivotTable',
    xAxisCount: 1,
    yAxisCount: 1,
    needGroup: true,
    needSeries: false,
    specialFields: ['rowField', 'colField', 'valueField'],
    fieldDescription: '透视表需要配置行字段、列字段和值字段'
  },
  scatter: {
    type: 'scatter',
    xAxisCount: 1,
    yAxisCount: 1,
    needGroup: false,
    needSeries: true
  },
  radar: {
    type: 'radar',
    xAxisCount: 1,
    yAxisCount: 1,
    needGroup: false,
    needSeries: true
  },
  gauge: {
    type: 'gauge',
    xAxisCount: 0,
    yAxisCount: 1,
    needGroup: false,
    needSeries: false
  },
  funnel: {
    type: 'funnel',
    xAxisCount: 1,
    yAxisCount: 1,
    needGroup: false,
    needSeries: false
  },
  heatmap: {
    type: 'heatmap',
    xAxisCount: 1,
    yAxisCount: 1,
    needGroup: false,
    needSeries: false,
    specialFields: ['value']
  },
  map: {
    type: 'map',
    xAxisCount: 0,
    yAxisCount: 1,
    needGroup: false,
    needSeries: false,
    specialFields: ['region']
  },
  chinaMap: {
    type: 'chinaMap',
    xAxisCount: 0,
    yAxisCount: 1,
    needGroup: false,
    needSeries: false,
    specialFields: ['region', 'value'],
    fieldDescription: '中国地图需要配置省份名称字段和数值字段'
  },
  worldMap: {
    type: 'worldMap',
    xAxisCount: 0,
    yAxisCount: 1,
    needGroup: false,
    needSeries: false,
    specialFields: ['country', 'value'],
    fieldDescription: '世界地图需要配置国家名称字段和数值字段'
  },
  kpi: {
    type: 'kpi',
    xAxisCount: 0,
    yAxisCount: 1,
    needGroup: false,
    needSeries: false,
    specialFields: ['value', 'previousValue', 'periodPreviousValue'],
    fieldDescription: 'KPI 卡片需要配置当前值字段，可选配置同比和环比字段'
  },
  waterfall: {
    type: 'waterfall',
    xAxisCount: 1,
    yAxisCount: 1,
    needGroup: false,
    needSeries: false,
    specialFields: ['name', 'value', 'isTotal'],
    fieldDescription: '瀑布图需要配置名称字段和数值字段，可选标记总计项'
  },
  wordCloud: {
    type: 'wordCloud',
    xAxisCount: 0,
    yAxisCount: 0,
    needGroup: false,
    needSeries: false,
    specialFields: ['word', 'weight'],
    fieldDescription: '词云图需要配置词语字段和权重字段'
  },
  combo: {
    type: 'combo',
    xAxisCount: 1,
    yAxisCount: 2,
    needGroup: false,
    needSeries: true,
    specialFields: ['category', 'barValue', 'lineValue'],
    fieldDescription: '组合图支持柱线混合，可配置双 Y 轴'
  },
  tree: {
    type: 'tree',
    xAxisCount: 0,
    yAxisCount: 0,
    needGroup: false,
    needSeries: false,
    specialFields: ['parent', 'name', 'value']
  },
  sankey: {
    type: 'sankey',
    xAxisCount: 0,
    yAxisCount: 0,
    needGroup: false,
    needSeries: false,
    specialFields: ['source', 'target', 'value']
  },
  parallel: {
    type: 'parallel',
    xAxisCount: 1,
    yAxisCount: 3,
    needGroup: false,
    needSeries: true
  },
  boxplot: {
    type: 'boxplot',
    xAxisCount: 1,
    yAxisCount: 1,
    needGroup: false,
    needSeries: false
  },
  candlestick: {
    type: 'candlestick',
    xAxisCount: 1,
    yAxisCount: 0,
    needGroup: false,
    needSeries: false,
    specialFields: ['open', 'close', 'low', 'high']
  },
  graph: {
    type: 'graph',
    xAxisCount: 0,
    yAxisCount: 0,
    needGroup: false,
    needSeries: false,
    specialFields: ['source', 'target', 'value']
  }
}

/**
 * 地图图表配置
 */
export interface MapChartConfig {
  /** 地图类型 */
  mapType: 'china' | 'world'
  /** 视觉映射配置 */
  visualMapConfig?: {
    min?: number
    max?: number
    inRange?: { color?: string[] }
    text?: [string, string]
  }
  /** 是否显示标签 */
  showLabel?: boolean
  /** 是否启用下钻 */
  enableDrillDown?: boolean
  /** 区域名称字段 */
  regionField?: string
  /** 数值字段 */
  valueField?: string
}

/**
 * KPI 卡片配置
 */
export interface KpiChartConfig {
  /** 值格式 */
  format: 'number' | 'currency' | 'percentage' | 'compact'
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
  /** 是否显示趋势 */
  showTrend?: boolean
  /** 正向趋势是否为好 */
  positiveIsGood?: boolean
  /** 是否显示迷你图 */
  showSparkline?: boolean
  /** 迷你图类型 */
  sparklineType?: 'line' | 'bar'
  /** 当前值字段 */
  valueField?: string
  /** 上期值字段（同比） */
  previousValueField?: string
  /** 上周期值字段（环比） */
  periodPreviousField?: string
}

/**
 * 瀑布图配置
 */
export interface WaterfallChartConfig {
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
  /** 名称字段 */
  nameField?: string
  /** 数值字段 */
  valueField?: string
  /** 是否为总计字段 */
  isTotalField?: string
}

/**
 * 词云图配置
 */
export interface WordCloudChartConfig {
  /** 词云形状 */
  shape?: 'circle' | 'cardioid' | 'diamond' | 'triangle-forward' | 'triangle' | 'pentagon' | 'star'
  /** 颜色范围 */
  colorRange?: string[]
  /** 最小字体大小 */
  minFontSize?: number
  /** 最大字体大小 */
  maxFontSize?: number
  /** 旋转角度范围 */
  rotationRange?: [number, number]
  /** 词语字段 */
  wordField?: string
  /** 权重字段 */
  weightField?: string
}

/**
 * 组合图配置
 */
export interface ComboChartConfig {
  /** 系列配置 */
  seriesConfig?: Array<{
    name: string
    type: 'bar' | 'line'
    yAxisIndex: 0 | 1
    color?: string
    smooth?: boolean
  }>
  /** 左 Y 轴标签 */
  leftAxisLabel?: string
  /** 右 Y 轴标签 */
  rightAxisLabel?: string
  /** 类别字段 */
  categoryField?: string
  /** 柱状图字段列表 */
  barFields?: string[]
  /** 折线图字段列表 */
  lineFields?: string[]
}
