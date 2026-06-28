/* eslint-disable @typescript-eslint/no-explicit-any */
import request from './request'
import type { ApiResponse } from '@/types/api'

// ==================== TypeScript 接口定义 ====================

/** 告警静默规则 */
export interface SilenceRule {
  id: string
  name: string
  /** 匹配模式，支持 * 通配 */
  matchPattern: string
  startTime: string
  endTime: string
  createdBy: string
  createdAt?: string
}

/** 运维审计记录 */
export interface OpsAuditRecord {
  id: string
  operator: string
  /** 操作类型: cache_clear, config_update, service_restart 等 */
  operationType: string
  target: string
  detail: string
  success: boolean
  ipAddress: string
  timestamp: string
}

/** 性能异常记录 */
export interface AnomalyRecord {
  metricName: string
  value: number
  mean: number
  stdDev: number
  threshold: number
  timestamp: string
}

/** 告警升级策略 */
export interface EscalationPolicy {
  level: string
  escalateAfterMinutes: number
  escalateToLevel: string
  notifyTargets: string[]
}

/** 值班班次 */
export interface OnCallShift {
  name: string
  daysOfWeek: string[]
  startTime: string
  endTime: string
  persons: string[]
}

/** 值班表 */
export interface OnCallSchedule {
  id: string
  name: string
  shifts: OnCallShift[]
  enabled: boolean
}

/** 创建静默规则请求 */
export interface SilenceRuleRequest {
  name: string
  matchPattern: string
  startTime: string
  endTime: string
  createdBy: string
}

// ==================== 系统概览 ====================

/** 获取系统概览 */
export const getSystemOverview = () => {
  return request.get<ApiResponse<Record<string, any>>>('/ops/overview')
}

// ==================== 性能异常 ====================

/** 获取性能异常记录 */
export const getAnomalies = (limit?: number) => {
  return request.get<ApiResponse<AnomalyRecord[]>>('/ops/anomalies', {
    params: { limit: limit ?? 100 }
  })
}

// ==================== 告警聚合 ====================

/** 获取告警聚合统计 */
export const getAlertAggregation = () => {
  return request.get<ApiResponse<Record<string, any>>>('/ops/alert-aggregation')
}

// ==================== 告警静默规则 ====================

/** 获取告警静默规则列表 */
export const getSilenceRules = () => {
  return request.get<ApiResponse<SilenceRule[]>>('/ops/silence-rules')
}

/** 创建告警静默规则 */
export const createSilenceRule = (data: SilenceRuleRequest) => {
  return request.post<ApiResponse<string>>('/ops/silence-rules', data)
}

/** 删除告警静默规则 */
export const deleteSilenceRule = (id: string) => {
  return request.delete<ApiResponse<void>>(`/ops/silence-rules/${id}`)
}

// ==================== 告警升级策略 ====================

/** 获取告警升级策略列表 */
export const getEscalationPolicies = () => {
  return request.get<ApiResponse<EscalationPolicy[]>>('/ops/escalation-policies')
}

// ==================== 值班表 ====================

/** 获取值班表列表 */
export const getOncallSchedules = () => {
  return request.get<ApiResponse<OnCallSchedule[]>>('/ops/oncall-schedules')
}

/** 获取当前值班人员 */
export const getCurrentOncall = () => {
  return request.get<ApiResponse<string[]>>('/ops/oncall/current')
}

// ==================== 运维审计 ====================

/** 获取运维审计记录 */
export const getAuditRecords = (params?: { operationType?: string; limit?: number }) => {
  return request.get<ApiResponse<OpsAuditRecord[]>>('/ops/audit/records', {
    params: { operationType: params?.operationType, limit: params?.limit ?? 100 }
  })
}

/** 获取运维审计统计 */
export const getAuditStats = () => {
  return request.get<ApiResponse<Record<string, any>>>('/ops/audit/stats')
}
