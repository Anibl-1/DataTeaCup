/**
 * 迷你图表类型定义
 * Mini Chart Type Definitions
 * 
 * 参考帆软FineReport迷你图、Excel条件格式数据条、PowerBI迷你图
 * Reference: FineReport mini charts, Excel conditional formatting data bars, PowerBI sparklines
 * 
 * 需求: 14.4.16, 14.4.17, 14.4.18, 14.4.19, 14.4.20
 */

// ============================================================================
// Data Bar Types (数据条)
// ============================================================================

/**
 * Data bar configuration
 * 数据条配置
 * 
 * Validates: 14.4.16 - 数据条 - 根据数值大小显示条形图
 */
export interface DataBarConfig {
  /** Minimum value for scale (auto-calculated if not provided) */
  min?: number
  /** Maximum value for scale (auto-calculated if not provided) */
  max?: number
  /** Positive value bar color */
  color?: string
  /** Negative value bar color */
  negativeColor?: string
  /** Whether to show the numeric value */
  showValue?: boolean
  /** Value display position */
  valuePosition?: 'inside' | 'outside' | 'hidden'
  /** Bar direction */
  direction?: 'horizontal' | 'vertical'
  /** Border radius for rounded corners */
  borderRadius?: number
  /** Background/track color */
  backgroundColor?: string
  /** Fill type */
  fillType?: 'solid' | 'gradient'
  /** Gradient end color (used when fillType is 'gradient') */
  gradientEndColor?: string
}

/**
 * Data bar props
 */
export interface DataBarProps {
  /** The value to display */
  value: number
  /** Configuration options */
  config?: DataBarConfig
}

// ============================================================================
// Sparkline Types (迷你折线图)
// ============================================================================

/**
 * Sparkline configuration
 * 迷你折线图配置
 * 
 * Validates: 14.4.17 - 迷你折线图 - 在单元格内显示趋势线
 */
export interface SparklineConfig {
  /** Chart type */
  type?: 'line' | 'area' | 'bar'
  /** Line/bar color */
  color?: string
  /** Area fill color (for area type) */
  fillColor?: string
  /** Whether to show data points */
  showPoints?: boolean
  /** Point radius */
  pointRadius?: number
  /** Whether to show area fill */
  showArea?: boolean
  /** Whether to use smooth curves */
  smooth?: boolean
  /** Line stroke width */
  strokeWidth?: number
  /** Minimum value for Y axis (auto-calculated if not provided) */
  minY?: number
  /** Maximum value for Y axis (auto-calculated if not provided) */
  maxY?: number
  /** Background color */
  backgroundColor?: string
  /** Highlight min point */
  highlightMin?: boolean
  /** Highlight max point */
  highlightMax?: boolean
  /** Min point color */
  minColor?: string
  /** Max point color */
  maxColor?: string
  /** Negative bar color (for bar type) */
  negativeColor?: string
}

/**
 * Sparkline props
 */
export interface SparklineProps {
  /** Array of data values */
  data: number[]
  /** Configuration options */
  config?: SparklineConfig
  /** Chart width */
  width?: number
  /** Chart height */
  height?: number
}

// ============================================================================
// Icon Set Types (图标集)
// ============================================================================

/**
 * Icon set item definition
 * 图标集项定义
 */
export interface IconSetItem {
  /** Icon name or SVG content */
  icon: string
  /** Icon color */
  color: string
  /** Threshold value */
  threshold: number
  /** Comparison operator */
  operator: 'gte' | 'gt' | 'lte' | 'lt' | 'eq'
}

/**
 * Icon set configuration
 * 图标集配置
 * 
 * Validates: 14.4.18 - 图标集 - 根据阈值显示不同图标（箭头、星级、信号等）
 */
export interface IconSetConfig {
  /** Icon set items (evaluated in order) */
  icons: IconSetItem[]
  /** Whether to show the numeric value alongside the icon */
  showValue?: boolean
  /** Value format (for display) */
  valueFormat?: 'number' | 'percent'
  /** Icon size */
  iconSize?: number
  /** Gap between icon and value */
  gap?: number
}

/**
 * Icon set props
 */
export interface IconSetProps {
  /** The value to evaluate */
  value: number
  /** Configuration options */
  config?: IconSetConfig
}

/**
 * Preset icon set type
 */
export type PresetIconSetType = 'arrows' | 'arrows3' | 'stars' | 'traffic' | 'flags' | 'ratings' | 'signals' | 'quarters'

// ============================================================================
// Progress Bar Types (进度条)
// ============================================================================

/**
 * Progress bar segment
 * 进度条分段
 */
export interface ProgressSegment {
  /** Segment threshold value */
  value: number
  /** Segment color */
  color: string
}

/**
 * Progress bar configuration
 * 进度条配置
 * 
 * Validates: 14.4.19 - 进度条 - 显示完成百分比
 */
export interface ProgressBarConfig {
  /** Maximum value (default: 100) */
  max?: number
  /** Whether to show percentage label */
  showLabel?: boolean
  /** Label format string (use {value} and {percent} placeholders) */
  labelFormat?: string
  /** Label position */
  labelPosition?: 'inside' | 'outside' | 'top' | 'bottom'
  /** Progress bar color */
  color?: string
  /** Track/background color */
  trackColor?: string
  /** Border radius */
  borderRadius?: number
  /** Whether to show striped pattern */
  striped?: boolean
  /** Whether to animate stripes */
  animated?: boolean
  /** Progress segments for multi-color progress */
  segments?: ProgressSegment[]
  /** Bar height */
  height?: number
}

/**
 * Progress bar props
 */
export interface ProgressBarProps {
  /** Current value */
  value: number
  /** Configuration options */
  config?: ProgressBarConfig
}

// ============================================================================
// Common Types
// ============================================================================

/**
 * Mini chart type
 */
export type MiniChartType = 'dataBar' | 'sparkline' | 'iconSet' | 'progressBar'

/**
 * Unified mini chart props
 */
export interface MiniChartProps {
  /** Chart type */
  type: MiniChartType
  /** Data value(s) */
  data: number | number[]
  /** Configuration based on type */
  config?: DataBarConfig | SparklineConfig | IconSetConfig | ProgressBarConfig
  /** Chart width */
  width?: number
  /** Chart height */
  height?: number
}

// ============================================================================
// Preset Icon Sets
// ============================================================================

/**
 * Preset icon sets
 * 预设图标集
 * 
 * Validates: 14.4.20 - 迷你图表配置 - 支持颜色、方向、阈值配置
 */
export const PRESET_ICON_SETS: Record<PresetIconSetType, IconSetItem[]> = {
  // 三向箭头 (上/平/下)
  arrows: [
    { icon: 'arrow-up', color: '#52c41a', threshold: 0, operator: 'gt' },
    { icon: 'arrow-right', color: '#faad14', threshold: 0, operator: 'eq' },
    { icon: 'arrow-down', color: '#ff4d4f', threshold: 0, operator: 'lt' },
  ],
  // 三向箭头 (基于百分比阈值)
  arrows3: [
    { icon: 'arrow-up', color: '#52c41a', threshold: 0.67, operator: 'gte' },
    { icon: 'arrow-right', color: '#faad14', threshold: 0.33, operator: 'gte' },
    { icon: 'arrow-down', color: '#ff4d4f', threshold: 0, operator: 'gte' },
  ],
  // 星级评分
  stars: [
    { icon: 'star-filled', color: '#faad14', threshold: 0.8, operator: 'gte' },
    { icon: 'star-half', color: '#faad14', threshold: 0.5, operator: 'gte' },
    { icon: 'star-empty', color: '#d9d9d9', threshold: 0, operator: 'gte' },
  ],
  // 交通灯 (红黄绿)
  traffic: [
    { icon: 'circle-filled', color: '#52c41a', threshold: 0.7, operator: 'gte' },
    { icon: 'circle-filled', color: '#faad14', threshold: 0.4, operator: 'gte' },
    { icon: 'circle-filled', color: '#ff4d4f', threshold: 0, operator: 'gte' },
  ],
  // 旗帜
  flags: [
    { icon: 'flag', color: '#52c41a', threshold: 0.7, operator: 'gte' },
    { icon: 'flag', color: '#faad14', threshold: 0.4, operator: 'gte' },
    { icon: 'flag', color: '#ff4d4f', threshold: 0, operator: 'gte' },
  ],
  // 评级 (5级)
  ratings: [
    { icon: 'rating-5', color: '#52c41a', threshold: 0.8, operator: 'gte' },
    { icon: 'rating-4', color: '#73d13d', threshold: 0.6, operator: 'gte' },
    { icon: 'rating-3', color: '#faad14', threshold: 0.4, operator: 'gte' },
    { icon: 'rating-2', color: '#fa8c16', threshold: 0.2, operator: 'gte' },
    { icon: 'rating-1', color: '#ff4d4f', threshold: 0, operator: 'gte' },
  ],
  // 信号强度
  signals: [
    { icon: 'signal-full', color: '#52c41a', threshold: 0.75, operator: 'gte' },
    { icon: 'signal-high', color: '#73d13d', threshold: 0.5, operator: 'gte' },
    { icon: 'signal-medium', color: '#faad14', threshold: 0.25, operator: 'gte' },
    { icon: 'signal-low', color: '#ff4d4f', threshold: 0, operator: 'gte' },
  ],
  // 四分位
  quarters: [
    { icon: 'quarter-full', color: '#52c41a', threshold: 0.75, operator: 'gte' },
    { icon: 'quarter-three', color: '#73d13d', threshold: 0.5, operator: 'gte' },
    { icon: 'quarter-half', color: '#faad14', threshold: 0.25, operator: 'gte' },
    { icon: 'quarter-one', color: '#ff4d4f', threshold: 0, operator: 'gte' },
  ],
}

// ============================================================================
// Default Configurations
// ============================================================================

/**
 * Default data bar configuration
 */
export const DEFAULT_DATA_BAR_CONFIG: Required<DataBarConfig> = {
  min: 0,
  max: 100,
  color: '#1890ff',
  negativeColor: '#ff4d4f',
  showValue: true,
  valuePosition: 'outside',
  direction: 'horizontal',
  borderRadius: 2,
  backgroundColor: '#f0f0f0',
  fillType: 'solid',
  gradientEndColor: '#69c0ff',
}

/**
 * Default sparkline configuration
 */
export const DEFAULT_SPARKLINE_CONFIG: Required<SparklineConfig> = {
  type: 'line',
  color: '#1890ff',
  fillColor: 'rgba(24, 144, 255, 0.2)',
  showPoints: false,
  pointRadius: 2,
  showArea: false,
  smooth: true,
  strokeWidth: 1.5,
  minY: 0,
  maxY: 100,
  backgroundColor: 'transparent',
  highlightMin: false,
  highlightMax: false,
  minColor: '#ff4d4f',
  maxColor: '#52c41a',
  negativeColor: '#ff4d4f',
}

/**
 * Default icon set configuration
 */
export const DEFAULT_ICON_SET_CONFIG: Required<IconSetConfig> = {
  icons: PRESET_ICON_SETS.traffic,
  showValue: false,
  valueFormat: 'number',
  iconSize: 16,
  gap: 4,
}

/**
 * Default progress bar configuration
 */
export const DEFAULT_PROGRESS_BAR_CONFIG: Required<ProgressBarConfig> = {
  max: 100,
  showLabel: true,
  labelFormat: '{percent}%',
  labelPosition: 'inside',
  color: '#1890ff',
  trackColor: '#f0f0f0',
  borderRadius: 4,
  striped: false,
  animated: false,
  segments: [],
  height: 16,
}

// ============================================================================
// Utility Functions
// ============================================================================

/**
 * Calculate percentage from value and range
 */
export function calculatePercentage(value: number, min: number, max: number): number {
  if (max === min) return 0
  return Math.max(0, Math.min(100, ((value - min) / (max - min)) * 100))
}

/**
 * Get icon for value based on icon set configuration
 */
export function getIconForValue(value: number, icons: IconSetItem[]): IconSetItem | null {
  // Icons should be evaluated in order (highest threshold first)
  const sortedIcons = [...icons].sort((a, b) => b.threshold - a.threshold)
  
  for (const item of sortedIcons) {
    let matches = false
    switch (item.operator) {
      case 'gte':
        matches = value >= item.threshold
        break
      case 'gt':
        matches = value > item.threshold
        break
      case 'lte':
        matches = value <= item.threshold
        break
      case 'lt':
        matches = value < item.threshold
        break
      case 'eq':
        matches = value === item.threshold
        break
    }
    if (matches) {
      return item
    }
  }
  
  // Return last icon as fallback
  return sortedIcons[sortedIcons.length - 1] || null
}

/**
 * Format progress label
 */
export function formatProgressLabel(value: number, max: number, format: string): string {
  const percent = Math.round((value / max) * 100)
  return format
    .replace('{value}', String(value))
    .replace('{max}', String(max))
    .replace('{percent}', String(percent))
}

/**
 * Calculate sparkline points for SVG path
 */
export function calculateSparklinePoints(
  data: number[],
  width: number,
  height: number,
  minY: number,
  maxY: number,
  padding: number = 2
): { x: number; y: number }[] {
  if (data.length === 0) return []
  
  const effectiveWidth = width - padding * 2
  const effectiveHeight = height - padding * 2
  const range = maxY - minY || 1
  
  return data.map((value, index) => ({
    x: padding + (index / Math.max(1, data.length - 1)) * effectiveWidth,
    y: padding + effectiveHeight - ((value - minY) / range) * effectiveHeight,
  }))
}

/**
 * Generate SVG path from points
 */
export function generateSparklinePath(
  points: { x: number; y: number }[],
  smooth: boolean = true
): string {
  if (points.length === 0) return ''
  if (points.length === 1) return `M ${points[0].x} ${points[0].y}`
  
  if (!smooth) {
    return points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ')
  }
  
  // Generate smooth curve using quadratic bezier
  let path = `M ${points[0].x} ${points[0].y}`
  
  for (let i = 1; i < points.length; i++) {
    const prev = points[i - 1]
    const curr = points[i]
    const midX = (prev.x + curr.x) / 2
    const midY = (prev.y + curr.y) / 2
    
    if (i === 1) {
      path += ` Q ${prev.x} ${prev.y} ${midX} ${midY}`
    } else {
      path += ` T ${midX} ${midY}`
    }
  }
  
  // Connect to last point
  const last = points[points.length - 1]
  path += ` T ${last.x} ${last.y}`
  
  return path
}

/**
 * Generate area path (closed path for fill)
 */
export function generateAreaPath(
  points: { x: number; y: number }[],
  height: number,
  smooth: boolean = true
): string {
  if (points.length === 0) return ''
  
  const linePath = generateSparklinePath(points, smooth)
  const firstX = points[0].x
  const lastX = points[points.length - 1].x
  
  return `${linePath} L ${lastX} ${height} L ${firstX} ${height} Z`
}
