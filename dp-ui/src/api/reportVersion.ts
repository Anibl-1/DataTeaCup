import request from './request'
import type { ReportVersion, VersionCompareResult } from '@/types/version'

/**
 * 创建版本
 */
export function createVersion(reportId: number, summary?: string) {
  return request<ReportVersion>({
    url: '/report-version/create',
    method: 'post',
    params: { reportId, summary }
  })
}

/**
 * 获取版本历史
 */
export function getVersionHistory(reportId: number) {
  return request<ReportVersion[]>({
    url: `/report-version/history/${reportId}`,
    method: 'get'
  })
}

/**
 * 获取版本详情
 */
export function getVersion(versionId: number) {
  return request<ReportVersion>({
    url: `/report-version/${versionId}`,
    method: 'get'
  })
}

/**
 * 比较两个版本
 */
export function compareVersions(versionId1: number, versionId2: number) {
  return request<VersionCompareResult>({
    url: '/report-version/compare',
    method: 'get',
    params: { versionId1, versionId2 }
  })
}

/**
 * 回滚到指定版本
 */
export function rollbackToVersion(reportId: number, versionId: number) {
  return request<void>({
    url: '/report-version/rollback',
    method: 'post',
    params: { reportId, versionId }
  })
}

/**
 * 删除版本
 */
export function deleteVersion(versionId: number) {
  return request<void>({
    url: `/report-version/${versionId}`,
    method: 'delete'
  })
}
