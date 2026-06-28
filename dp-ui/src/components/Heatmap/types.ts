/**
 * 热力图类型定义
 * Heatmap Type Definitions
 * 
 * 参考 Tableau/PowerBI 热力图设计
 * Reference: Tableau/PowerBI heatmap design
 * 
 * 需求: 22.1.1, 22.1.2, 22.1.3
 */

// ============================================================================
// Color Scale Types (色阶配置)
// ============================================================================

/**
 * Color stop for gradient
 * 渐变色阶点
 */
export interface ColorStop {
  /** Position in range 0-1 */
  position: number
  /** Color value (hex, rgb, etc.) */
  color: string
}

/**
 * Color scale configuration
 * 色阶配置
 * 
 * Validates: 22.1.2 - 支持自定义热力图色阶（支持多色渐变）
 */
export interface ColorScaleConfig {
  /** Minimum value color */
  minColor: string
  /** Middle value color (optional, for 3-color scale) */
  midColor?: string
  /** Maximum value color */
  maxColor: string
  /** Custom color stops for multi-color gradient */
  colorStops?: ColorStop[]
  /** Minimum value for scale (auto-calculated if not provided) */
  minValue?: number
  /** Maximum value for scale (auto-calculated if not provided) */
  maxValue?: number
  /** Middle value for 3-color scale (auto-calculated as midpoint if not provided) */
  midValue?: number
}

// ============================================================================
// Legend Types (图例配置)
// ============================================================================

/**
 * Legend position
 * 图例位置
 */
export type LegendPosition = 'top' | 'bottom' | 'left' | 'right' | 'none'

/**
 * Legend configuration
 * 图例配置
 * 
 * Validates: 22.1.3 - 支持热力图图例显示
 */
export interface LegendConfig {
  /** Whether to show legend */
  show: boolean
  /** Legend position */
  position: LegendPosition
  /** Legend width (for horizontal) or height (for vertical) */
  size?: number
  /** Number of tick marks to show */
  ticks?: number
  /** Custom tick values */
  tickValues?: number[]
  /** Value format function or pattern */
  valueFormat?: string | ((value: number) => string)
  /** Legend title */
  title?: string
}

// ============================================================================
// Cell Types (单元格配置)
// ============================================================================

/**
 * Cell configuration
 * 单元格配置
 */
export interface CellConfig {
  /** Cell width in pixels */
  width?: number
  /** Cell height in pixels */
  height?: number
  /** Gap between cells */
  gap?: number
  /** Border radius for rounded corners */
  borderRadius?: number
  /** Border color */
  borderColor?: string
  /** Border width */
  borderWidth?: number
  /** Whether to show cell value */
  showValue?: boolean
  /** Value format function or pattern */
  valueFormat?: string | ((value: number) => string)
  /** Font size for cell value */
  fontSize?: number
  /** Font color for cell value (auto-contrast if not provided) */
  fontColor?: string
  /** Whether to auto-contrast font color based on background */
  autoContrast?: boolean
}

// ============================================================================
// Tooltip Types (提示框配置)
// ============================================================================

/**
 * Tooltip configuration
 * 提示框配置
 */
export interface TooltipConfig {
  /** Whether to show tooltip on hover */
  show: boolean
  /** Custom tooltip formatter */
  formatter?: (params: TooltipParams) => string
}

/**
 * Tooltip parameters
 * 提示框参数
 */
export interface TooltipParams {
  /** Row index */
  rowIndex: number
  /** Column index */
  colIndex: number
  /** Cell value */
  value: number | null
  /** X-axis label */
  xLabel: string
  /** Y-axis label */
  yLabel: string
}

// ============================================================================
// Heatmap Data Types (热力图数据)
// ============================================================================

/**
 * Heatmap data cell
 * 热力图数据单元格
 */
export interface HeatmapCell {
  /** Row index */
  row: number
  /** Column index */
  col: number
  /** Cell value */
  value: number | null
}

/**
 * Heatmap data format
 * 热力图数据格式
 * 
 * Validates: 22.1.1 - 支持数值热力图（基于数值大小显示颜色深浅）
 */
export type HeatmapData = number[][] | HeatmapCell[]

// ============================================================================
// Main Heatmap Props
// ============================================================================

/**
 * Heatmap component props
 * 热力图组件属性
 */
export interface HeatmapProps {
  /** 2D array of values or array of cell objects */
  data: HeatmapData
  /** X-axis labels (column headers) */
  xLabels?: string[]
  /** Y-axis labels (row headers) */
  yLabels?: string[]
  /** Color scale configuration */
  colorScale?: ColorScaleConfig
  /** Legend configuration */
  legend?: LegendConfig
  /** Cell configuration */
  cell?: CellConfig
  /** Tooltip configuration */
  tooltip?: TooltipConfig
  /** Component width (auto if not provided) */
  width?: number | string
  /** Component height (auto if not provided) */
  height?: number | string
  /** Null value display color */
  nullColor?: string
  /** Null value display text */
  nullText?: string
}

// ============================================================================
// Default Configurations
// ============================================================================

/**
 * Default color scale configuration
 */
export const DEFAULT_COLOR_SCALE: Required<Omit<ColorScaleConfig, 'colorStops' | 'midColor' | 'midValue'>> & Pick<ColorScaleConfig, 'colorStops' | 'midColor' | 'midValue'> = {
  minColor: '#f0f9ff',
  midColor: '#3b82f6',
  maxColor: '#1e3a8a',
  minValue: 0,
  maxValue: 100,
  midValue: undefined,
  colorStops: undefined,
}

/**
 * Default legend configuration
 */
export const DEFAULT_LEGEND_CONFIG: Required<Omit<LegendConfig, 'tickValues' | 'title'>> & Pick<LegendConfig, 'tickValues' | 'title'> = {
  show: true,
  position: 'right',
  size: 20,
  ticks: 5,
  tickValues: undefined,
  valueFormat: (v: number) => v.toLocaleString(),
  title: undefined,
}

/**
 * Default cell configuration
 */
export const DEFAULT_CELL_CONFIG: Required<Omit<CellConfig, 'fontColor'>> & Pick<CellConfig, 'fontColor'> = {
  width: 40,
  height: 40,
  gap: 1,
  borderRadius: 2,
  borderColor: '#e5e7eb',
  borderWidth: 0,
  showValue: false,
  valueFormat: (v: number) => v.toLocaleString(),
  fontSize: 11,
  fontColor: undefined,
  autoContrast: true,
}

/**
 * Default tooltip configuration
 */
export const DEFAULT_TOOLTIP_CONFIG: Required<Omit<TooltipConfig, 'formatter'>> & Pick<TooltipConfig, 'formatter'> = {
  show: true,
  formatter: undefined,
}

// ============================================================================
// Utility Functions
// ============================================================================

/**
 * Type guard to check if data is a 2D number array
 */
function is2DArray(data: HeatmapData): data is number[][] {
  return data.length > 0 && Array.isArray(data[0])
}

/**
 * Type guard to check if data is a HeatmapCell array
 */
function isCellArray(data: HeatmapData): data is HeatmapCell[] {
  if (data.length === 0) return false
  const first = data[0]
  return typeof first === 'object' && first !== null && 'row' in first && 'col' in first
}

/**
 * Convert HeatmapData to 2D array format
 */
export function normalizeData(data: HeatmapData): (number | null)[][] {
  if (data.length === 0) return []
  
  // Check if it's already a 2D array (array of arrays)
  if (is2DArray(data)) {
    return data
  }
  
  // Check if it's a cell array
  if (isCellArray(data)) {
    const cells = data
    const maxRow = Math.max(...cells.map(c => c.row))
    const maxCol = Math.max(...cells.map(c => c.col))
    
    const result: (number | null)[][] = Array(maxRow + 1)
      .fill(null)
      .map(() => Array(maxCol + 1).fill(null))
    
    for (const cell of cells) {
      result[cell.row][cell.col] = cell.value
    }
    
    return result
  }
  
  // Fallback: treat as empty
  return []
}

/**
 * Calculate min and max values from data
 */
export function calculateDataRange(data: (number | null)[][]): { min: number; max: number } {
  let min = Infinity
  let max = -Infinity
  
  for (const row of data) {
    for (const value of row) {
      if (value !== null && value !== undefined && !isNaN(value)) {
        min = Math.min(min, value)
        max = Math.max(max, value)
      }
    }
  }
  
  // Handle edge cases
  if (min === Infinity) min = 0
  if (max === -Infinity) max = 100
  if (min === max) {
    min = min - 1
    max = max + 1
  }
  
  return { min, max }
}

/**
 * Interpolate between two colors
 */
export function interpolateColor(color1: string, color2: string, factor: number): string {
  const c1 = parseColor(color1)
  const c2 = parseColor(color2)
  
  const r = Math.round(c1.r + (c2.r - c1.r) * factor)
  const g = Math.round(c1.g + (c2.g - c1.g) * factor)
  const b = Math.round(c1.b + (c2.b - c1.b) * factor)
  
  return `rgb(${r}, ${g}, ${b})`
}

/**
 * Parse color string to RGB object
 */
export function parseColor(color: string): { r: number; g: number; b: number } {
  // Handle hex colors
  if (color.startsWith('#')) {
    const hex = color.slice(1)
    const fullHex = hex.length === 3
      ? hex.split('').map(c => c + c).join('')
      : hex
    
    return {
      r: parseInt(fullHex.slice(0, 2), 16),
      g: parseInt(fullHex.slice(2, 4), 16),
      b: parseInt(fullHex.slice(4, 6), 16),
    }
  }
  
  // Handle rgb/rgba colors
  const match = color.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)/)
  if (match) {
    return {
      r: parseInt(match[1], 10),
      g: parseInt(match[2], 10),
      b: parseInt(match[3], 10),
    }
  }
  
  // Default to white
  return { r: 255, g: 255, b: 255 }
}

/**
 * Get color for a value based on color scale
 * 
 * Validates: 22.1.1 - 基于数值大小显示颜色深浅
 */
export function getColorForValue(
  value: number | null,
  config: ColorScaleConfig,
  dataMin: number,
  dataMax: number,
  nullColor: string = '#f3f4f6'
): string {
  if (value === null || value === undefined || isNaN(value)) {
    return nullColor
  }
  
  const min = config.minValue ?? dataMin
  const max = config.maxValue ?? dataMax
  
  // Clamp value to range
  const clampedValue = Math.max(min, Math.min(max, value))
  
  // Calculate position in range (0-1)
  const range = max - min
  const position = range === 0 ? 0.5 : (clampedValue - min) / range
  
  // Use custom color stops if provided
  if (config.colorStops && config.colorStops.length >= 2) {
    return interpolateColorStops(config.colorStops, position)
  }
  
  // Use 3-color scale if midColor is provided
  if (config.midColor) {
    const midValue = config.midValue ?? (min + max) / 2
    const midPosition = range === 0 ? 0.5 : (midValue - min) / range
    
    if (position <= midPosition) {
      const factor = midPosition === 0 ? 0 : position / midPosition
      return interpolateColor(config.minColor, config.midColor, factor)
    } else {
      const factor = midPosition === 1 ? 1 : (position - midPosition) / (1 - midPosition)
      return interpolateColor(config.midColor, config.maxColor, factor)
    }
  }
  
  // Use 2-color scale
  return interpolateColor(config.minColor, config.maxColor, position)
}

/**
 * Interpolate color from color stops
 */
export function interpolateColorStops(stops: ColorStop[], position: number): string {
  // Sort stops by position
  const sortedStops = [...stops].sort((a, b) => a.position - b.position)
  
  // Find surrounding stops
  let lowerStop = sortedStops[0]
  let upperStop = sortedStops[sortedStops.length - 1]
  
  for (let i = 0; i < sortedStops.length - 1; i++) {
    if (position >= sortedStops[i].position && position <= sortedStops[i + 1].position) {
      lowerStop = sortedStops[i]
      upperStop = sortedStops[i + 1]
      break
    }
  }
  
  // Calculate interpolation factor
  const range = upperStop.position - lowerStop.position
  const factor = range === 0 ? 0 : (position - lowerStop.position) / range
  
  return interpolateColor(lowerStop.color, upperStop.color, factor)
}

/**
 * Calculate contrasting text color (black or white) based on background
 */
export function getContrastColor(backgroundColor: string): string {
  const { r, g, b } = parseColor(backgroundColor)
  
  // Calculate relative luminance
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
  
  return luminance > 0.5 ? '#000000' : '#ffffff'
}

/**
 * Format value for display
 */
export function formatValue(
  value: number | null,
  format: string | ((value: number) => string) | undefined,
  nullText: string = '-'
): string {
  if (value === null || value === undefined || isNaN(value)) {
    return nullText
  }
  
  if (typeof format === 'function') {
    return format(value)
  }
  
  if (typeof format === 'string') {
    // Simple pattern replacement
    return format.replace('{value}', value.toLocaleString())
  }
  
  return value.toLocaleString()
}

/**
 * Generate legend tick values
 */
export function generateLegendTicks(
  min: number,
  max: number,
  count: number,
  customTicks?: number[]
): number[] {
  if (customTicks && customTicks.length > 0) {
    return customTicks.filter(v => v >= min && v <= max).sort((a, b) => a - b)
  }
  
  const ticks: number[] = []
  const step = (max - min) / (count - 1)
  
  for (let i = 0; i < count; i++) {
    ticks.push(min + step * i)
  }
  
  return ticks
}

// ============================================================================
// Preset Color Scales
// ============================================================================

/**
 * Preset color scale type
 */
export type PresetColorScaleType = 
  | 'blue'
  | 'green'
  | 'red'
  | 'orange'
  | 'purple'
  | 'blueRed'
  | 'greenRed'
  | 'rainbow'
  | 'grayscale'

/**
 * Preset color scales
 * 预设色阶
 */
export const PRESET_COLOR_SCALES: Record<PresetColorScaleType, ColorScaleConfig> = {
  // 蓝色渐变
  blue: {
    minColor: '#eff6ff',
    midColor: '#3b82f6',
    maxColor: '#1e3a8a',
  },
  // 绿色渐变
  green: {
    minColor: '#f0fdf4',
    midColor: '#22c55e',
    maxColor: '#14532d',
  },
  // 红色渐变
  red: {
    minColor: '#fef2f2',
    midColor: '#ef4444',
    maxColor: '#7f1d1d',
  },
  // 橙色渐变
  orange: {
    minColor: '#fff7ed',
    midColor: '#f97316',
    maxColor: '#7c2d12',
  },
  // 紫色渐变
  purple: {
    minColor: '#faf5ff',
    midColor: '#a855f7',
    maxColor: '#581c87',
  },
  // 蓝红双色（冷热）
  blueRed: {
    minColor: '#3b82f6',
    midColor: '#fef3c7',
    maxColor: '#ef4444',
  },
  // 绿红双色（好坏）
  greenRed: {
    minColor: '#22c55e',
    midColor: '#fef3c7',
    maxColor: '#ef4444',
  },
  // 彩虹色
  rainbow: {
    minColor: '#3b82f6',
    maxColor: '#ef4444',
    colorStops: [
      { position: 0, color: '#3b82f6' },
      { position: 0.25, color: '#22c55e' },
      { position: 0.5, color: '#eab308' },
      { position: 0.75, color: '#f97316' },
      { position: 1, color: '#ef4444' },
    ],
  },
  // 灰度
  grayscale: {
    minColor: '#f9fafb',
    midColor: '#9ca3af',
    maxColor: '#111827',
  },
}
