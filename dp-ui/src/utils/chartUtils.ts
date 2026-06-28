/**
 * 图表共享工具函数
 * 集中管理 ChartCenter / ChartGrid / ChartListView 等组件中的重复工具函数
 */
import {
  BarChartOutline, PieChartOutline, TrendingUpOutline, StatsChartOutline,
  PulseOutline, AnalyticsOutline, SpeedometerOutline, GitNetworkOutline
} from '@vicons/ionicons5'
import { CHART_TYPES } from '@/types/chart'
import type { ChartDefinition } from '@/types/chart'

/**
 * 图标组件映射表
 */
export const chartIconComponents: Record<string, any> = {
  BarChartOutline, PieChartOutline, TrendingUpOutline, StatsChartOutline,
  PulseOutline, AnalyticsOutline, SpeedometerOutline, GitNetworkOutline
}

/**
 * 根据图表类型获取对应的图标组件
 */
export const getChartIconComponent = (chartType?: string) => {
  if (!chartType) return BarChartOutline
  const type = CHART_TYPES.find(t => t.value === chartType)
  const iconName = type?.icon || 'BarChartOutline'
  return chartIconComponents[iconName] || BarChartOutline
}

/**
 * 根据图表类型获取中文标签
 */
export const getChartTypeLabel = (chartType?: string) => {
  if (!chartType) return '未知'
  const type = CHART_TYPES.find(t => t.value === chartType)
  return type?.label || chartType
}

/**
 * 格式化日期为本地化字符串
 */
export const formatChartDate = (date?: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}

/**
 * 判断图表是否有查询参数
 */
export const hasChartParameters = (chart: ChartDefinition) => {
  if (!chart.chartConfig) return false
  try {
    const config = JSON.parse(chart.chartConfig)
    return config.queryParameters?.length > 0 || config.metadata?.chartParameters?.length > 0
  } catch {
    return false
  }
}

/**
 * 格式化相对时间（如"3分钟前"）
 */
export const formatRelativeTime = (timestamp: number) => {
  const now = Date.now()
  const diff = now - timestamp
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return new Date(timestamp).toLocaleDateString('zh-CN')
}

/**
 * 网格卡片背景渐变色映射
 */
export const getChartBgColor = (chartType: string) => {
  const colors: Record<string, string> = {
    bar: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    line: 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)',
    pie: 'linear-gradient(135deg, #ee0979 0%, #ff6a00 100%)',
    scatter: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    radar: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
    gauge: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
    funnel: 'linear-gradient(135deg, #d299c2 0%, #fef9d7 100%)',
    heatmap: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    table: 'linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)',
    summaryTable: 'linear-gradient(135deg, #6366f1 0%, #4f46e5 100%)',
    pivotTable: 'linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%)',
    kpi: 'linear-gradient(135deg, #f97316 0%, #ef4444 100%)',
    wordCloud: 'linear-gradient(135deg, #06b6d4 0%, #3b82f6 100%)',
    combo: 'linear-gradient(135deg, #8b5cf6 0%, #ec4899 100%)',
    tree: 'linear-gradient(135deg, #059669 0%, #10b981 100%)',
    sankey: 'linear-gradient(135deg, #7c3aed 0%, #2563eb 100%)',
    chinaMap: 'linear-gradient(135deg, #dc2626 0%, #f97316 100%)',
    worldMap: 'linear-gradient(135deg, #0284c7 0%, #06b6d4 100%)',
    boxplot: 'linear-gradient(135deg, #475569 0%, #64748b 100%)',
    candlestick: 'linear-gradient(135deg, #16a34a 0%, #dc2626 100%)',
    graph: 'linear-gradient(135deg, #7c3aed 0%, #a855f7 100%)',
    parallel: 'linear-gradient(135deg, #0891b2 0%, #6366f1 100%)'
  }
  return colors[chartType] || 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
}
