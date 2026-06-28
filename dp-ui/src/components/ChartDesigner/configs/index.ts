/**
 * Chart Designer Configuration Panels
 * 
 * Chart-specific configuration panels for the new chart types:
 * - Map Chart (China/World)
 * - KPI Card
 * - Waterfall Chart
 * - Word Cloud Chart
 * - Combo Chart (Bar + Line with dual Y-axis)
 */

export { default as MapChartConfig } from './MapChartConfig.vue'
export { default as KpiChartConfig } from './KpiChartConfig.vue'
export { default as WaterfallChartConfig } from './WaterfallChartConfig.vue'
export { default as WordCloudChartConfig } from './WordCloudChartConfig.vue'
export { default as ComboChartConfig } from './ComboChartConfig.vue'

/**
 * Get the appropriate config component for a chart type
 */
export function getChartConfigComponent(chartType: string): string | null {
  const configMap: Record<string, string> = {
    'map': 'MapChartConfig',
    'chinaMap': 'MapChartConfig',
    'worldMap': 'MapChartConfig',
    'kpi': 'KpiChartConfig',
    'waterfall': 'WaterfallChartConfig',
    'wordCloud': 'WordCloudChartConfig',
    'combo': 'ComboChartConfig'
  }
  
  return configMap[chartType] || null
}

/**
 * Check if a chart type has a specific config panel
 */
export function hasChartConfigPanel(chartType: string): boolean {
  const typesWithConfig = ['map', 'chinaMap', 'worldMap', 'kpi', 'waterfall', 'wordCloud', 'combo']
  return typesWithConfig.includes(chartType)
}
