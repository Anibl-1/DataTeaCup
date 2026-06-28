/**
 * Chart Components
 * 
 * Reusable chart components for data visualization
 */

export { default as MapChart } from './MapChart.vue'
export { default as KpiCard } from './KpiCard.vue'
export { default as WaterfallChart } from './WaterfallChart.vue'
export { default as WordCloudChart } from './WordCloudChart.vue'
export { default as ComboChart } from './ComboChart.vue'

// Re-export types from chartMap
export type {
  MapDataItem,
  VisualMapConfig,
  MapChartOptions
} from '@/utils/chartMap'

// Re-export types from chartWaterfall
export type {
  WaterfallDataItem,
  WaterfallChartOptions,
  WaterfallCalculation
} from '@/utils/chartWaterfall'

// Re-export types from chartWordCloud
export type {
  WordCloudDataItem,
  WordCloudShape,
  WordCloudChartOptions
} from '@/utils/chartWordCloud'

// Re-export types from chartCombo
export type {
  ComboDataItem,
  ComboSeriesConfig,
  ComboYAxisConfig,
  ComboChartOptions
} from '@/utils/chartCombo'

// Re-export KPI card utilities
export {
  calculateTrend,
  formatKpiValue,
  buildSparklineOption,
  buildKpiCardConfig,
  getTrendColor,
  formatChangePercent,
  getKpiSizeConfig,
  type TrendDirection,
  type ValueFormat,
  type TrendResult,
  type KpiCardData,
  type KpiConfig,
  type SparklineOptions,
  type KpiSizeConfig
} from '@/utils/kpiCard'

// Re-export waterfall chart utilities
export {
  buildWaterfallOption,
  calculateWaterfallData,
  validateWaterfallInvariant,
  validateWaterfallSteps
} from '@/utils/chartWaterfall'

// Re-export word cloud utilities
export {
  buildWordCloudOption,
  validateWordCloudDataIntegrity,
  extractWordFrequency,
  generateRandomWordCloudData
} from '@/utils/chartWordCloud'

// Re-export combo chart utilities
export {
  buildComboOption,
  buildSimpleComboOption,
  validateComboChartDualAxis,
  validateComboDataIntegrity
} from '@/utils/chartCombo'
