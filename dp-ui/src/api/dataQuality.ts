import request from './request'
import type { DataQualityRule, QualityReport } from '@/types/dataQuality'

/**
 * 检查数据质量
 */
export function checkQuality(dataSourceId: number, tableName: string) {
  return request<QualityReport>({
    url: '/data-quality/check',
    method: 'post',
    params: { dataSourceId, tableName }
  })
}

/**
 * 保存质量规则
 */
export function saveRule(rule: DataQualityRule) {
  return request<DataQualityRule>({
    url: '/data-quality/rule',
    method: 'post',
    data: rule
  })
}

/**
 * 获取质量规则列表
 */
export function getRules(dataSourceId?: number) {
  return request<DataQualityRule[]>({
    url: '/data-quality/rules',
    method: 'get',
    params: { dataSourceId }
  })
}

/**
 * 删除质量规则
 */
export function deleteRule(id: number) {
  return request<void>({
    url: `/data-quality/rule/${id}`,
    method: 'delete'
  })
}

/**
 * 检查并告警
 */
export function checkAndAlert(dataSourceId: number, tableName: string) {
  return request<void>({
    url: '/data-quality/check-and-alert',
    method: 'post',
    params: { dataSourceId, tableName }
  })
}

/**
 * 获取质量报告历史
 */
export function getReportHistory(params: {
  dataSourceId?: number
  tableName?: string
  page?: number
  size?: number
}) {
  return request<{ list: QualityReport[]; total: number }>({
    url: '/data-quality/reports',
    method: 'get',
    params
  })
}

/**
 * 获取最新报告
 */
export function getLatestReport(dataSourceId: number, tableName: string) {
  return request<QualityReport>({
    url: '/data-quality/latest-report',
    method: 'get',
    params: { dataSourceId, tableName }
  })
}
