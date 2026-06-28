/* eslint-disable @typescript-eslint/no-explicit-any */
import request from './request'
import type { ApiResponse, PageResult } from '@/types/api'

// DataX任务参数定义
export interface DataxJobParameter {
  name: string
  label: string
  type: 'text' | 'number' | 'date' | 'datetime' | 'select'
  required?: boolean
  defaultValue?: string | number
  options?: { label: string; value: string | number }[]
  placeholder?: string
  description?: string
}

// DataX任务类型
export interface DataxJob {
  id?: number
  jobName: string
  jobDesc?: string
  jobType: number // 1-数据库同步, 2-模板
  sourceDataSourceId: number
  sourceTable?: string
  sourceQuerySql?: string
  targetDataSourceId: number
  targetTable?: string
  writeMode?: string // insert, update, replace
  columnMapping?: string
  cronExpression?: string
  jobStatus?: number // 0-停止, 1-运行中
  incrementType?: number // 0-全量, 1-增量
  incrementColumn?: string
  incrementValue?: string
  lastIncrementValue?: string // 上次增量同步的值
  channelCount?: number
  batchSize?: number // 批量提交大小
  parameterDefinition?: string // JSON格式的参数定义
  defaultParameters?: string // JSON格式的默认参数值
  lastExecuteTime?: string
  createTime?: string
  updateTime?: string
  sourceDataSourceName?: string
  targetDataSourceName?: string
}

// DataX任务日志类型
export interface DataxJobLog {
  id: number
  jobId: number
  jobName: string
  startTime: string
  endTime?: string
  executeStatus: number // 0-失败, 1-成功, 2-运行中
  triggerType?: number // 1-手动, 2-定时
  readCount?: number
  writeCount?: number
  errorCount?: number
  executeTime?: number
  executeLog?: string
  errorMsg?: string
}

// 执行进度类型
export interface ExecuteProgress {
  status: number
  percent: number
  readCount: number
  writeCount: number
  message: string
}

/**
 * 获取任务列表
 */
export const getJobList = (params: { page?: number; pageSize?: number; keyword?: string; jobStatus?: number; incrementType?: number }) => {
  return request.get<ApiResponse<PageResult<DataxJob>>>('/datax/job/list', { params })
}

/**
 * 获取任务详情
 */
export const getJobById = (id: number) => {
  return request.get<ApiResponse<DataxJob>>(`/datax/job/${id}`)
}

/**
 * 创建任务
 */
export const createJob = (data: DataxJob) => {
  return request.post<ApiResponse<number>>('/datax/job/create', data)
}

/**
 * 更新任务
 */
export const updateJob = (data: DataxJob) => {
  return request.post<ApiResponse<void>>('/datax/job/update', data)
}

/**
 * 删除任务
 */
export const deleteJob = (id: number) => {
  return request.delete<ApiResponse<void>>(`/datax/job/${id}`)
}

/**
 * 批量删除任务
 */
export const batchDeleteJobs = (ids: number[]) => {
  return request.post<ApiResponse<void>>('/datax/job/batch-delete', ids)
}

/**
 * 复制任务
 */
export const copyJob = (id: number) => {
  return request.post<ApiResponse<number>>(`/datax/job/${id}/copy`)
}

/**
 * 执行任务
 */
export const executeJob = (id: number) => {
  return request.post<ApiResponse<number>>(`/datax/job/${id}/execute`)
}

/**
 * 带参数执行任务
 */
export const executeJobWithParams = (id: number, parameters: Record<string, any>) => {
  return request.post<ApiResponse<number>>(`/datax/job/${id}/execute-with-params`, parameters)
}

/**
 * 获取任务参数定义
 */
export const getJobParameters = (id: number) => {
  return request.get<ApiResponse<DataxJobParameter[]>>(`/datax/job/${id}/parameters`)
}

/**
 * 更新任务参数定义
 */
export const updateJobParameters = (id: number, parameterDefinition: string, defaultParameters: string) => {
  return request.post<ApiResponse<void>>(`/datax/job/${id}/parameters`, {
    parameterDefinition,
    defaultParameters
  })
}

/**
 * 批量执行任务
 */
export const batchExecuteJobs = (ids: number[]) => {
  return request.post<ApiResponse<void>>('/datax/job/batch-execute', ids)
}

/**
 * 启动任务调度
 */
export const startJob = (id: number) => {
  return request.post<ApiResponse<void>>(`/datax/job/${id}/start`)
}

/**
 * 停止任务调度
 */
export const stopJob = (id: number) => {
  return request.post<ApiResponse<void>>(`/datax/job/${id}/stop`)
}

/**
 * 获取执行进度
 */
export const getProgress = (logId: number) => {
  return request.get<ApiResponse<ExecuteProgress>>(`/datax/job/progress/${logId}`)
}

/**
 * 获取任务日志列表
 */
export const getJobLogList = (params: { page?: number; pageSize?: number; jobId?: number; status?: number; jobName?: string }) => {
  return request.get<ApiResponse<PageResult<DataxJobLog>>>('/datax/job/logs', { params })
}

/**
 * 预览DataX JSON配置
 */
export const previewJson = (data: DataxJob) => {
  return request.post<ApiResponse<string>>('/datax/job/preview-json', data)
}

/**
 * 获取源表字段列表
 */
export const getSourceColumns = (dataSourceId: number, tableName: string) => {
  return request.get<ApiResponse<Array<{
    columnName: string
    dataType: string
    columnSize: number
    nullable: boolean
    remarks?: string
  }>>>('/datax/job/columns', { params: { dataSourceId, tableName } })
}

// ==================== 任务模板功能 ====================

/**
 * 获取任务模板列表
 */
export const getTemplates = () => {
  return request.get<ApiResponse<DataxJob[]>>('/datax/job/templates')
}

/**
 * 保存为模板
 */
export const saveAsTemplate = (id: number, templateName: string) => {
  return request.post<ApiResponse<number>>(`/datax/job/${id}/save-as-template`, null, {
    params: { templateName }
  })
}

/**
 * 从模板创建任务
 */
export const createFromTemplate = (templateId: number, jobName: string) => {
  return request.post<ApiResponse<number>>('/datax/job/create-from-template', null, {
    params: { templateId, jobName }
  })
}

/**
 * 删除模板
 */
export const deleteTemplate = (id: number) => {
  return request.delete<ApiResponse<void>>(`/datax/job/template/${id}`)
}

// ==================== 增量同步配置 ====================

/**
 * 获取增量同步状态
 */
export const getIncrementStatus = (id: number) => {
  return request.get<ApiResponse<{
    incrementType: number
    incrementColumn: string
    lastIncrementValue: string
    lastExecuteTime: string
  }>>(`/datax/job/${id}/increment-status`)
}

/**
 * 重置增量同步位置
 */
export const resetIncrement = (id: number) => {
  return request.post<ApiResponse<void>>(`/datax/job/${id}/reset-increment`)
}

// ==================== 统计与监控 ====================

/**
 * 获取任务执行趋势（最近7天）
 */
export const getExecutionTrend = () => {
  return request.get<ApiResponse<Array<{
    date: string
    total: number
    success: number
    failed: number
  }>>>('/datax/job/execution-trend')
}

/**
 * 获取任务概览统计
 */
export const getOverviewStatistics = () => {
  return request.get<ApiResponse<{
    totalJobs: number
    runningJobs: number
    templateCount: number
    todayTotal: number
    todaySuccess: number
    todayFailed: number
    dataSourceCount: number
  }>>('/datax/job/overview-statistics')
}

/**
 * 获取日志详情
 */
export const getLogById = (id: number) => {
  return request.get<ApiResponse<DataxJobLog>>(`/datax/job/log/${id}`)
}

/**
 * 获取运行中的任务队列
 */
export const getRunningQueue = () => {
  return request.get<ApiResponse<Array<{
    logId: number
    status: number
    percent: number
    readCount: number
    writeCount: number
    message: string
  }>>>('/datax/job/queue')
}

/**
 * 对比两次执行记录
 */
export const compareExecutions = (logId1: number, logId2: number) => {
  return request.post<ApiResponse<{
    execution1: any
    execution2: any
    diff: {
      readCountDiff: number
      writeCountDiff: number
      durationDiff: number
      throughput1: number
      throughput2: number
    }
  }>>('/datax/job/compare-executions', { logId1, logId2 })
}

/**
 * 获取日志统计
 */
export const getLogStatistics = (jobId?: number) => {
  return request.get<ApiResponse<{
    total: number
    success: number
    failed: number
    running: number
  }>>('/datax/job/log-statistics', { params: { jobId } })
}
