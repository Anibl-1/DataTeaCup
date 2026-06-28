/**
 * MiniChart 模块导出
 * MiniChart Module Exports
 * 
 * 迷你图表组件集合，用于在单元格内显示数据可视化
 * Mini chart component collection for displaying data visualization within cells
 * 
 * 参考: 帆软FineReport迷你图、Excel条件格式、PowerBI迷你图
 * Reference: FineReport mini charts, Excel conditional formatting, PowerBI sparklines
 * 
 * 需求: 14.4.16, 14.4.17, 14.4.18, 14.4.19, 14.4.20
 */

// ============================================================================
// Type Exports
// ============================================================================

export type {
  // Data Bar
  DataBarConfig,
  DataBarProps,
  // Sparkline
  SparklineConfig,
  SparklineProps,
  // Icon Set
  IconSetItem,
  IconSetConfig,
  IconSetProps,
  PresetIconSetType,
  // Progress Bar
  ProgressSegment,
  ProgressBarConfig,
  ProgressBarProps,
  // Common
  MiniChartType,
  MiniChartProps,
} from './types'

// ============================================================================
// Constant Exports
// ============================================================================

export {
  // Preset icon sets
  PRESET_ICON_SETS,
  // Default configurations
  DEFAULT_DATA_BAR_CONFIG,
  DEFAULT_SPARKLINE_CONFIG,
  DEFAULT_ICON_SET_CONFIG,
  DEFAULT_PROGRESS_BAR_CONFIG,
} from './types'

// ============================================================================
// Utility Function Exports
// ============================================================================

export {
  calculatePercentage,
  getIconForValue,
  formatProgressLabel,
  calculateSparklinePoints,
  generateSparklinePath,
  generateAreaPath,
} from './types'

// ============================================================================
// Component Exports
// ============================================================================

export { default as DataBar } from './DataBar.vue'
export { default as Sparkline } from './Sparkline.vue'
export { default as IconSet } from './IconSet.vue'
export { default as ProgressBar } from './ProgressBar.vue'

// ============================================================================
// Default Export (all components)
// ============================================================================

import DataBar from './DataBar.vue'
import Sparkline from './Sparkline.vue'
import IconSet from './IconSet.vue'
import ProgressBar from './ProgressBar.vue'

export default {
  DataBar,
  Sparkline,
  IconSet,
  ProgressBar,
}
