/* eslint-disable @typescript-eslint/no-explicit-any */
import request from './request'
import type { LineageGraph, LineageMetadata } from '@/types/lineage'

/**
 * 获取血缘图谱
 */
export function getLineageGraph(tableName: string, depth: number = 3) {
  return request<LineageGraph>({
    url: `/data-lineage/graph/${tableName}`,
    method: 'get',
    params: { depth }
  })
}

/**
 * 获取全链路追溯
 */
export function getFullChainAnalysis(tableName: string, depth: number = 5) {
  return request<any>({
    url: `/data-lineage/full-chain/${tableName}`,
    method: 'get',
    params: { depth }
  })
}

/**
 * 获取影响分析
 */
export function getImpactAnalysis(tableName: string, depth: number = 5) {
  return request<any>({
    url: `/data-lineage/impact/${tableName}`,
    method: 'get',
    params: { depth }
  })
}

/**
 * 获取热点表分析
 */
export function getHotspotAnalysis(topN: number = 10) {
  return request<any>({
    url: '/data-lineage/hotspot',
    method: 'get',
    params: { topN }
  })
}

/**
 * 获取孤岛检测
 */
export function getOrphanAnalysis() {
  return request<any>({
    url: '/data-lineage/orphan',
    method: 'get'
  })
}

/**
 * 获取血缘健康报告
 */
export function getHealthReport() {
  return request<any>({
    url: '/data-lineage/health',
    method: 'get'
  })
}

/**
 * 获取血缘统计
 */
export function getStatistics() {
  return request<any>({
    url: '/data-lineage/statistics',
    method: 'get'
  })
}

/**
 * 获取表依赖摘要
 */
export function getTableSummary(tableName: string) {
  return request<any>({
    url: `/data-lineage/summary/${tableName}`,
    method: 'get'
  })
}

/**
 * 自动发现血缘
 */
export function autoDiscover() {
  return request<any>({
    url: '/data-lineage/discover',
    method: 'post'
  })
}
