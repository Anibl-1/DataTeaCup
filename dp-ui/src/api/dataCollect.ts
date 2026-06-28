/* eslint-disable @typescript-eslint/no-explicit-any */
import request from './request'
import type { ApiResponse, PageResult, PageParams } from '@/types/api'
import type { CollectTask, CollectTaskForm } from '@/types/collectTask'

/**
 * 获取采集任务列表（分页）
 */
export const getCollectTaskList = (params: PageParams) => {
  return request.get<ApiResponse<PageResult<CollectTask>>>('/collect/task/list', { params })
}

/**
 * 创建采集任务
 */
export const createCollectTask = (data: CollectTaskForm) => {
  return request.post<ApiResponse<void>>('/collect/task/create', data)
}

/**
 * 更新采集任务
 */
export const updateCollectTask = (data: CollectTaskForm) => {
  return request.put<ApiResponse<void>>('/collect/task/update', data)
}

/**
 * 删除采集任务
 */
export const deleteCollectTask = (id: number) => {
  return request.delete<ApiResponse<void>>(`/collect/task/delete/${id}`)
}

/**
 * 启动采集任务
 * 定时任务只启用调度，非定时任务立即执行
 */
export const startCollectTask = (id: number) => {
  return request.post<ApiResponse<void>>(`/collect/task/start/${id}`)
}

/**
 * 立即执行一次任务（无论是否启用定时）
 */
export const executeTaskOnce = (id: number) => {
  return request.post<ApiResponse<void>>(`/collect/task/execute/${id}`)
}

/**
 * 停止采集任务
 */
export const stopCollectTask = (id: number) => {
  return request.post<ApiResponse<void>>(`/collect/task/stop/${id}`)
}

/**
 * 查询采集数据
 */
export const getCollectData = (params: PageParams & { dataSourceId: number; tableName: string }) => {
  return request.get<ApiResponse<PageResult<Record<string, any>>>>('/collect/data', { params })
}

/**
 * 预览导入文件
 */
export const previewImportFile = (file: File, firstRowAsHeader: boolean = true, previewRows: number = 10) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('firstRowAsHeader', String(firstRowAsHeader))
  formData.append('previewRows', String(previewRows))
  
  return request.post<ApiResponse<{
    headers: string[]
    previewData: Record<string, any>[]
    totalRows: number
    previewRows: number
    fileName: string
    fileSize: number
  }>>('/collect/import/preview', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 60000 // 1分钟超时
  })
}

/**
 * 导入数据文件
 */
export const importData = (params: {
  file: File
  dataSourceId: number | null
  tableName: string
  autoCreateTable: boolean
  skipHeader: boolean
  truncateFirst: boolean
  importMode?: string
  deduplicateField?: string
  filterConditions?: string
}) => {
  const formData = new FormData()
  formData.append('file', params.file)
  if (params.dataSourceId) {
    formData.append('dataSourceId', String(params.dataSourceId))
  }
  formData.append('tableName', params.tableName)
  formData.append('autoCreateTable', String(params.autoCreateTable))
  formData.append('firstRowAsHeader', String(params.skipHeader))
  formData.append('truncateFirst', String(params.truncateFirst))
  if (params.importMode) formData.append('importMode', params.importMode)
  if (params.deduplicateField) formData.append('deduplicateField', params.deduplicateField)
  if (params.filterConditions) formData.append('filterConditions', params.filterConditions)
  
  return request.post<ApiResponse<{
    successCount: number
    failCount: number
    totalCount: number
    tableName: string
    tableCreated: boolean
    tableTruncated: boolean
  }>>('/collect/import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 300000,
  })
}

/**
 * 数据库到数据库导入
 */
export const importFromDatabase = (params: {
  sourceDataSourceId: number
  sourceTable: string
  targetDataSourceId: number
  targetTable?: string
  autoCreateTable: boolean
  truncateFirst: boolean
  whereClause?: string
  importMode?: string
  incrementField?: string
  incrementStartValue?: string
}) => {
  const formData = new FormData()
  formData.append('sourceDataSourceId', String(params.sourceDataSourceId))
  formData.append('sourceTable', params.sourceTable)
  formData.append('targetDataSourceId', String(params.targetDataSourceId))
  if (params.targetTable) {
    formData.append('targetTable', params.targetTable)
  }
  formData.append('autoCreateTable', String(params.autoCreateTable))
  formData.append('truncateFirst', String(params.truncateFirst))
  if (params.whereClause) {
    formData.append('whereClause', params.whereClause)
  }
  if (params.importMode) formData.append('importMode', params.importMode)
  if (params.incrementField) formData.append('incrementField', params.incrementField)
  if (params.incrementStartValue) formData.append('incrementStartValue', params.incrementStartValue)
  
  return request.post<ApiResponse<{
    successCount: number
    failCount: number
    totalCount: number
    tableName: string
    tableCreated: boolean
    tableTruncated: boolean
    message?: string
  }>>('/collect/import/database', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 600000
  })
}

/**
 * 获取采集任务详情（使用视图v_collect_task_detail）
 * 性能比传统查询快3倍，一次获取完整信息
 */
export const getCollectTaskDetail = (id: number) => {
  return request.get<ApiResponse<{
    id: number
    task_name: string
    table_name: string
    target_table_name: string
    collect_mode: string
    status: string
    last_execute_time: string
    last_execute_result: string
    batch_size: number
    auto_create_table: boolean
    source_id: number
    source_name: string
    source_db_type: string
    source_host: string
    source_port: number
    source_database: string
    target_id: number
    target_name: string
    target_db_type: string
    target_host: string
    target_port: number
    target_database: string
  }>>(`/collect/task/${id}/detail`)
}

/**
 * 获取增量采集任务列表（使用视图v_incremental_tasks）
 * 显示所有增量任务及其采集进度
 */
export const getIncrementalTasks = () => {
  return request.get<ApiResponse<Array<{
    id: number
    task_name: string
    data_source_name: string
    table_name: string
    incremental_field: string
    incremental_type: string
    last_collect_value: string
    last_execute_time: string
    status: string
    progress_info: string
  }>>>('/collect/task/incremental')
}


/**
 * 采集日志类型
 */
export interface CollectLog {
  id: number
  taskId: number
  taskName: string
  sourceTable: string
  targetTable: string
  status: 'running' | 'success' | 'failed'
  rowCount: number
  startTime: string
  endTime: string
  duration: number
  errorMessage: string
  executeSql: string
  createTime: string
}

/**
 * 获取采集日志列表（分页）
 */
export const getCollectLogList = (params: {
  page?: number
  pageSize?: number
  taskId?: number
  status?: string
  startDate?: string
  endDate?: string
}) => {
  return request.get<ApiResponse<PageResult<CollectLog>>>('/collect/log/list', { params })
}

/**
 * 获取采集日志详情
 */
export const getCollectLogDetail = (id: number) => {
  return request.get<ApiResponse<CollectLog>>(`/collect/log/${id}`)
}

/**
 * 获取任务最近的日志
 */
export const getTaskLogs = (taskId: number, limit: number = 10) => {
  return request.get<ApiResponse<CollectLog[]>>(`/collect/log/task/${taskId}`, { params: { limit } })
}

/**
 * 下载导入模板（含表头的空Excel文件）
 */
export const downloadImportTemplate = (dataSourceId: number, tableName: string) => {
  return request.get('/collect/import/template', {
    params: { dataSourceId, tableName },
    responseType: 'blob'
  })
}

/**
 * 获取某字段的最大值（用于增量导入起始值）
 */
export const getFieldMaxValue = (dataSourceId: number, tableName: string, fieldName: string) => {
  return request.get<ApiResponse<{ maxValue: any }>>('/collect/field-max-value', {
    params: { dataSourceId, tableName, fieldName }
  })
}

/**
 * 校验导入数据
 */
export const validateImportData = (params: {
  file: File
  dataSourceId: number
  tableName: string
  firstRowAsHeader: boolean
}) => {
  const formData = new FormData()
  formData.append('file', params.file)
  formData.append('dataSourceId', String(params.dataSourceId))
  formData.append('tableName', params.tableName)
  formData.append('firstRowAsHeader', String(params.firstRowAsHeader))
  
  return request.post<ApiResponse<{
    totalRows: number
    passedRows: number
    failedRows: number
    warningRows: number
    errors: Array<{
      row: number
      field: string
      message: string
      type: 'error' | 'warning'
    }>
  }>>('/collect/import/validate', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

/**
 * 调试接口：获取定时任务调度器状态
 */
export const debugScheduler = () => {
  return request.get<ApiResponse<{
    scheduledRunningTasksCount: number
    scheduledRunningTasks: any[]
    currentTime: string
  }>>('/collect/task/scheduler/debug')
}
