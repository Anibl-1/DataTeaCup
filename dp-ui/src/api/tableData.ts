/* eslint-disable @typescript-eslint/no-explicit-any */
import request from '@/api/request'

// 获取数据源列表
export const getDataSources = () => {
  return request.get('/data-source/list')
}

// 获取数据源下的表列表
export const getTables = (dataSourceId: number) => {
  return request.get(`/tabledata/tables/${dataSourceId}`)
}

// 获取表结构
export const getTableStructure = (dataSourceId: number, tableName: string) => {
  return request.get(`/tabledata/structure/${dataSourceId}/${tableName}`)
}

// 获取表数据（分页）
export const getTableData = (params: {
  dataSourceId: number
  tableName: string
  page: number
  pageSize: number
  where?: string
  orderBy?: string
  searchKeyword?: string
  searchColumns?: string[]
}) => {
  return request.post('/tabledata/query', params)
}

// 新增数据行
export const insertRow = (params: {
  dataSourceId: number
  tableName: string
  data: Record<string, any>
}) => {
  return request.post('/tabledata/insert', params)
}

// 更新数据行（支持复合主键）
export const updateRow = (params: {
  dataSourceId: number
  tableName: string
  data: Record<string, any>
  // 支持单主键（向后兼容）
  primaryKey?: string
  primaryValue?: any
  // 支持复合主键
  primaryKeys?: string[]
  primaryValues?: Record<string, any>
}) => {
  return request.post('/tabledata/update', params)
}

// 删除数据行（支持复合主键）
export const deleteRow = (params: {
  dataSourceId: number
  tableName: string
  // 支持单主键（向后兼容）
  primaryKey?: string
  primaryValue?: any
  // 支持复合主键
  primaryKeys?: string[]
  primaryValues?: Record<string, any>
}) => {
  return request.post('/tabledata/delete', params)
}

// 批量删除数据行（支持复合主键）
export const batchDeleteRows = (params: {
  dataSourceId: number
  tableName: string
  // 支持单主键（向后兼容）
  primaryKey?: string
  primaryValues?: any[]
  // 支持复合主键
  primaryKeys?: string[]
  primaryValuesArray?: Record<string, any>[]
}) => {
  return request.post('/tabledata/batch-delete', params)
}

// 导入数据（Excel/CSV）
export const importData = (formData: FormData) => {
  return request.post('/tabledata/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 导出数据
export const exportData = (params: {
  dataSourceId: number
  tableName: string
  format: 'excel' | 'csv'
  where?: string
}) => {
  return request.post('/tabledata/export', params, { responseType: 'blob' })
}

// 执行SQL
export const executeSql = (params: {
  dataSourceId: number
  sql: string
}) => {
  return request.post('/tabledata/execute-sql', params)
}

// 字段映射导入
export const importDataWithMapping = (formData: FormData) => {
  return request.post('/tabledata/import-mapped', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000
  })
}

// 查询导入进度
export const getImportProgress = (taskId: string) => {
  return request.get(`/tabledata/import-progress/${taskId}`)
}

// 下载导入模板
export const downloadImportTemplate = (dataSourceId: number, tableName: string) => {
  return request.get('/tabledata/import/template', {
    params: { dataSourceId, tableName },
    responseType: 'blob'
  })
}

// 建表（DDL）
export const createTable = (params: {
  dataSourceId: number
  tableName: string
  columns: Array<{ columnName: string, dataType: string, columnSize?: string, nullable?: boolean, defaultValue?: string, comment?: string }>
  primaryKey?: string
  tableComment?: string
}) => {
  return request.post('/tabledata/create-table', params)
}

// 改表（DDL）
export const alterTable = (params: {
  dataSourceId: number
  tableName: string
  action: 'ADD' | 'DROP' | 'MODIFY'
  columns: Array<{ columnName: string, dataType?: string, columnSize?: string, comment?: string }>
}) => {
  return request.post('/tabledata/alter-table', params)
}
