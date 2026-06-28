/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 报表版本管理相关类型定义
 */

/**
 * 报表版本
 */
export interface ReportVersion {
  /** 版本ID */
  id?: number
  /** 报表ID */
  reportId: number
  /** 版本号 */
  versionNo: number
  /** 配置快照JSON */
  configSnapshot?: string
  /** SQL快照 */
  sqlSnapshot?: string
  /** 修改摘要 */
  summary?: string
  /** 创建人ID */
  createBy?: number
  /** 创建时间 */
  createTime?: string
}

/**
 * 版本差异项
 */
export interface VersionDiff {
  /** 字段名 */
  field: string
  /** 旧值 */
  oldValue: any
  /** 新值 */
  newValue: any
  /** 变更类型 */
  changeType: 'added' | 'removed' | 'modified'
}

/**
 * 版本对比结果
 */
export interface VersionCompareResult {
  /** 版本1 */
  version1: ReportVersion
  /** 版本2 */
  version2: ReportVersion
  /** 差异列表 */
  diffs: VersionDiff[]
}
