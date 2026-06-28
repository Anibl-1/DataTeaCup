import request from './request'
import type { ApiResponse, PageResult } from '@/types/api'

// 导出任务超时：10分钟
const EXPORT_TIMEOUT = 600000

export interface ExportTask {
  id: number
  taskName: string
  taskType: string
  refId?: number
  refCode?: string
  filters?: string
  status: number  // 0-等待中, 1-处理中, 2-已完成, 3-失败
  progress: number
  filePath?: string
  fileName?: string
  fileSize?: number
  totalRows?: number
  dataType?: string  // xlsx-Excel文件, zip-压缩包
  errorMsg?: string
  createBy: number
  createTime: string
  startTime?: string
  finishTime?: string
  expireTime?: string
}

/**
 * 创建导出任务
 */
export const createExportTask = (data: {
  taskName: string
  taskType?: string
  refId?: number
  refCode?: string
  filters?: string
  params?: string
}) => {
  return request.post<ApiResponse<ExportTask>>('/export-task/create', data, {
    timeout: EXPORT_TIMEOUT
  })
}

/**
 * 获取任务列表
 */
export const getExportTaskList = (params: { 
  page?: number
  pageSize?: number
  taskName?: string
  startDate?: string
  endDate?: string
}) => {
  return request.get<ApiResponse<PageResult<ExportTask>>>('/export-task/list', { params })
}

/**
 * 获取进行中的任务
 */
export const getPendingTasks = () => {
  return request.get<ApiResponse<ExportTask[]>>('/export-task/pending')
}

/**
 * 获取任务详情
 */
export const getExportTaskById = (id: number) => {
  return request.get<ApiResponse<ExportTask>>(`/export-task/${id}`)
}

/**
 * 下载导出文件
 */
export const downloadExportFile = (id: number) => {
  return request.get(`/export-task/${id}/download`, {
    responseType: 'blob',
    timeout: EXPORT_TIMEOUT
  })
}

/**
 * 删除任务
 */
export const deleteExportTask = (id: number) => {
  return request.delete<ApiResponse<void>>(`/export-task/${id}`)
}

/**
 * 获取状态文本
 */
export const getStatusText = (status: number): string => {
  switch (status) {
    case 0: return '等待中'
    case 1: return '处理中'
    case 2: return '已完成'
    case 3: return '失败'
    default: return '未知'
  }
}

/**
 * 获取状态类型
 */
export const getStatusType = (status: number): 'default' | 'info' | 'success' | 'error' => {
  switch (status) {
    case 0: return 'default'
    case 1: return 'info'
    case 2: return 'success'
    case 3: return 'error'
    default: return 'default'
  }
}
