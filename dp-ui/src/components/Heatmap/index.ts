/**
 * Heatmap 模块导出
 * Heatmap Module Exports
 * 
 * 热力图组件，用于二维数据可视化展示
 * Heatmap component for 2D data visualization
 * 
 * 参考: Tableau/PowerBI 热力图设计
 * Reference: Tableau/PowerBI heatmap design
 * 
 * 需求: 22.1.1, 22.1.2, 22.1.3
 */

// ============================================================================
// Type Exports
// ============================================================================

export type {
  // Color Scale
  ColorStop,
  ColorScaleConfig,
  // Legend
  LegendPosition,
  LegendConfig,
  // Cell
  CellConfig,
  // Tooltip
  TooltipConfig,
  TooltipParams,
  // Data
  HeatmapCell,
  HeatmapData,
  // Props
  HeatmapProps,
  // Preset types
  PresetColorScaleType,
} from './types'

// ============================================================================
// Constant Exports
// ============================================================================

export {
  // Default configurations
  DEFAULT_COLOR_SCALE,
  DEFAULT_LEGEND_CONFIG,
  DEFAULT_CELL_CONFIG,
  DEFAULT_TOOLTIP_CONFIG,
  // Preset color scales
  PRESET_COLOR_SCALES,
} from './types'

// ============================================================================
// Utility Function Exports
// ============================================================================

export {
  normalizeData,
  calculateDataRange,
  interpolateColor,
  parseColor,
  getColorForValue,
  interpolateColorStops,
  getContrastColor,
  formatValue,
  generateLegendTicks,
} from './types'

// ============================================================================
// Component Exports
// ============================================================================

export { default as Heatmap } from './Heatmap.vue'
export { default as HeatmapLegend } from './HeatmapLegend.vue'

// ============================================================================
// Default Export
// ============================================================================

import Heatmap from './Heatmap.vue'
import HeatmapLegend from './HeatmapLegend.vue'

export default {
  Heatmap,
  HeatmapLegend,
}
