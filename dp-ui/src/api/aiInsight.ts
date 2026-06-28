import request from './request'
import type { InsightReport, AnomalyRecord, TrendAnalysis } from '@/types/insight'

/**
 * AI 洞察 API
 */

/**
 * 分析数据集（HTTP POST 替代原 SSE 流式版本）
 */
export function analyzeDataset(dataSourceId: number, sql: string, tableName?: string) {
  return request<InsightReport>({
    url: '/ai/insight/analyze',
    method: 'post',
    params: { dataSourceId, sql, tableName }
  })
}

/**
 * 生成洞察报告
 */
export function generateInsightReport(dataSourceId: number, sql: string) {
  return request<InsightReport>({
    url: '/ai/insight/report',
    method: 'post',
    params: { dataSourceId, sql }
  })
}

/**
 * 检测异常值
 */
export function detectAnomalies(
  dataSourceId: number,
  sql: string,
  threshold: number = 3.0
) {
  return request<AnomalyRecord[]>({
    url: '/ai/insight/anomalies',
    method: 'post',
    params: { dataSourceId, sql, threshold }
  })
}

/**
 * 分析趋势
 */
export function analyzeTrend(
  dataSourceId: number,
  sql: string,
  timeField?: string
) {
  return request<TrendAnalysis>({
    url: '/ai/insight/trend',
    method: 'post',
    params: { dataSourceId, sql, timeField }
  })
}

/**
 * 检测并告警
 */
export function detectAndAlert(
  dataSourceId: number,
  tableName: string,
  sql: string,
  threshold: number = 3.0
) {
  return request<void>({
    url: '/ai/insight/detect-and-alert',
    method: 'post',
    params: { dataSourceId, tableName, sql, threshold }
  })
}
