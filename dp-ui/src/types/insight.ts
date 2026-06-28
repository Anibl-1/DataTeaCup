/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * AI 数据洞察相关类型定义
 */

/**
 * 字段统计信息
 */
export interface FieldStats {
  field: string
  type: 'numeric' | 'categorical'
  nonNullCount: number
  nullCount: number
  nullRate: number
  // 数值型字段
  min?: number
  max?: number
  mean?: number
  sum?: number
  stdDev?: number
  // 分类型字段
  uniqueCount?: number
  topValues?: Array<{ value: any; count: number }>
}

/**
 * 统计摘要
 */
export interface StatisticsSummary {
  totalRows: number
  fields: FieldStats[]
  message?: string
}

/**
 * 异常记录
 */
export interface AnomalyRecord {
  field: string
  value: number
  mean: number
  stdDev: number
  deviation: number
  rowIndex: number
}

/**
 * 字段趋势
 */
export interface FieldTrend {
  field: string
  trend: 'increasing' | 'decreasing' | 'stable'
  changeRate: number
}

/**
 * 趋势分析结果
 */
export interface TrendAnalysis {
  trend: string
  changeRate: number
  fieldTrends: FieldTrend[]
  hasCyclicPattern: boolean
}

/**
 * 洞察报告
 */
export interface InsightReport {
  summary: StatisticsSummary
  anomalies: AnomalyRecord[]
  trends: TrendAnalysis
  aiInsight: string
  generatedAt: string
  error?: string
}

/**
 * SSE 洞察事件类型
 */
export type InsightEventType = 'start' | 'summary' | 'anomalies' | 'trends' | 'insight' | 'complete' | 'error'

/**
 * SSE 洞察事件数据
 */
export interface InsightEventData {
  type: InsightEventType
  data: any
}
