/* eslint-disable @typescript-eslint/no-explicit-any */
import request from './request'

/** 告警规则 */
export interface AlertRule {
  id?: number
  ruleName: string
  ruleCode?: string
  metricType: string
  metricName: string
  thresholdType: string
  thresholdValue: number
  durationSeconds?: number
  alertLevel: string
  alertMessage?: string
  notificationChannels?: string
  notificationUsers?: string
  isEnabled?: number
  createBy?: number
  createTime?: string
  updateTime?: string
}

/** 告警记录 */
export interface AlertRecord {
  id: number
  ruleId: number
  ruleName: string
  metricType: string
  metricName: string
  metricValue: number
  thresholdValue: number
  alertLevel: string
  alertMessage: string
  alertTime: string
  isNotified: number
  notificationTime?: string
  isResolved: number
  resolveTime?: string
  resolveBy?: number
  resolveNote?: string
  createTime: string
}

export const alertApi = {
  /** 获取告警规则列表 */
  getRules() {
    return request.get<AlertRule[]>('/alert/rules')
  },

  /** 获取单个告警规则 */
  getRule(id: number) {
    return request.get<AlertRule>(`/alert/rule/${id}`)
  },

  /** 创建告警规则 */
  createRule(rule: AlertRule) {
    return request.post('/alert/rule', rule)
  },

  /** 更新告警规则 */
  updateRule(id: number, rule: AlertRule) {
    return request.put(`/alert/rule/${id}`, rule)
  },

  /** 删除告警规则 */
  deleteRule(id: number) {
    return request.delete(`/alert/rule/${id}`)
  },

  /** 启用/禁用告警规则 */
  toggleRule(id: number) {
    return request.put(`/alert/rule/${id}/toggle`)
  },

  /** 获取告警记录列表 */
  getRecords(page: number = 1, size: number = 20) {
    return request.get<{ list: AlertRecord[]; total: number }>('/alert/records', {
      params: { page, size }
    })
  },

  /** 解决告警 */
  resolveRecord(id: number, note?: string) {
    return request.post(`/alert/record/${id}/resolve`, null, {
      params: { note }
    })
  },

  /** 获取告警统计 */
  getStats() {
    return request.get<any>('/alert/stats')
  }
}
