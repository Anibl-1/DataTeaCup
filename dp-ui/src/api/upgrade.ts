import request from './request'
import type { ApiResponse } from '@/types/api'

// ==================== TypeScript 接口定义 ====================

/** 版本信息 */
export interface VersionInfo {
  currentVersion: string
  latestVersion: string
  /** 是否有可用更新 */
  updateAvailable: boolean
  releaseNotes: string
  downloadUrl: string
  /** 安装包大小(字节) */
  fileSize: number
  /** 是否增量更新 */
  incremental: boolean
}

/** 升级结果 */
export interface UpgradeResult {
  success: boolean
  message: string
  fromVersion: string
  toVersion: string
}

/** 升级记录 */
export interface UpgradeRecord {
  fromVersion: string
  toVersion: string
  /** 类型: upgrade, rollback, hotfix */
  type: string
  success: boolean
  message: string
  timestamp: string
}

/** 备份信息 */
export interface BackupInfo {
  backupId: string
  version: string
  path: string
  /** 备份大小(字节) */
  size: number
  timestamp: string
}

// ==================== 版本检查 ====================

/** 检查新版本 */
export const checkUpdate = () => {
  return request.get<ApiResponse<VersionInfo>>('/upgrade/check')
}

/** 获取当前版本 */
export const getCurrentVersion = () => {
  return request.get<ApiResponse<string>>('/upgrade/version')
}

// ==================== 升级操作 ====================

/** 执行升级 */
export const performUpgrade = (targetVersion: string) => {
  return request.post<ApiResponse<UpgradeResult>>('/upgrade/perform', { targetVersion })
}

/** 回滚到指定版本 */
export const rollback = (targetVersion: string) => {
  return request.post<ApiResponse<UpgradeResult>>('/upgrade/rollback', { targetVersion })
}

/** 应用热补丁 */
export const applyHotfix = (hotfixId: string) => {
  return request.post<ApiResponse<UpgradeResult>>('/upgrade/hotfix', { hotfixId })
}

// ==================== 历史与备份 ====================

/** 获取升级历史 */
export const getHistory = () => {
  return request.get<ApiResponse<UpgradeRecord[]>>('/upgrade/history')
}

/** 创建系统备份 */
export const createBackup = () => {
  return request.post<ApiResponse<BackupInfo>>('/upgrade/backup')
}
