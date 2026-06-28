/**
 * Dashboard Widget Components
 * 
 * Reusable widget components for the dashboard designer
 */

export { default as WidgetWrapper } from './WidgetWrapper.vue'
export { default as ChartWidget } from './ChartWidget.vue'
export { default as KpiWidget } from './KpiWidget.vue'
export { default as TextWidget } from './TextWidget.vue'
export { default as FilterWidget } from './FilterWidget.vue'

// Re-export types for convenience
export type {
  ChartWidgetConfig,
  KpiWidgetConfig,
  TextWidgetConfig,
  FilterWidgetConfig,
  DashboardWidget
} from '@/types/dashboard'
