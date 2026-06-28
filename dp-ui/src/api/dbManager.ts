/* eslint-disable @typescript-eslint/no-explicit-any */
import request from './request'

// 连接信息类型
export interface ConnectionInfo {
  dbType: string
  host: string
  port: string
  dbName: string
  username: string
  password?: string
}

// 表信息类型
export interface TableInfo {
  tableName: string
  viewName?: string
  remarks?: string
}

// 存储过程信息类型
export interface ProcedureInfo {
  procedureName: string
  procedureType?: number
  remarks?: string
}

// 列信息类型
export interface ColumnInfo {
  columnName: string
  dataType: string
  columnSize: number
  nullable: string
  remarks?: string
  defaultValue?: string
  isPrimaryKey?: string
}

// 查询结果类型
export interface QueryResult {
  columns: string[]
  data: Record<string, any>[]
  total: number
  page: number
  pageSize: number
}

// SQL执行结果类型
export interface SqlResult {
  type: string
  columns?: string[]
  data?: Record<string, any>[]
  rowCount?: number
  affectedRows?: number
  message: string
  success: boolean
  executeTime: number
}

/**
 * 验证访问密码
 */
export const verifyPassword = (password: string): Promise<{ data: boolean }> => {
  return request.post('/db-manager/verify-password', { password }) as any
}

/**
 * 一键连接系统数据库
 */
export const connectSystemDb = (): Promise<{ data: string }> => {
  return request.post('/db-manager/connect-system') as any
}

/**
 * 创建代理连接
 */
export const createConnection = (data: ConnectionInfo): Promise<{ data: string }> => {
  return request.post('/db-manager/connect', data) as any
}

/**
 * 关闭代理连接
 */
export const closeConnection = (sessionId: string): Promise<{ data: string }> => {
  return request.post('/db-manager/disconnect', { sessionId }) as any
}

/**
 * 测试数据库连接
 */
export const testConnection = (data: ConnectionInfo): Promise<{ data: string }> => {
  return request.post('/db-manager/test', data) as any
}

/**
 * 获取表列表
 */
export const getTables = (sessionId: string): Promise<{ data: TableInfo[] }> => {
  return request.post('/db-manager/tables', { sessionId }) as any
}

/**
 * 获取视图列表
 */
export const getViews = (sessionId: string): Promise<{ data: TableInfo[] }> => {
  return request.post('/db-manager/views', { sessionId }) as any
}

/**
 * 获取存储过程/函数列表
 */
export const getProcedures = (sessionId: string): Promise<{ data: ProcedureInfo[] }> => {
  return request.post('/db-manager/procedures', { sessionId }) as any
}

/**
 * 获取表结构
 */
export const getTableStructure = (sessionId: string, tableName: string, silent = false): Promise<{ data: ColumnInfo[] }> => {
  return request.post('/db-manager/table-structure', { sessionId, tableName }, silent ? { __silent: true } as any : undefined) as any
}

/**
 * 查询表数据
 */
export const queryTableData = (sessionId: string, tableName: string, page?: number, pageSize?: number, whereClause?: string): Promise<{ data: QueryResult }> => {
  return request.post('/db-manager/query-data', { sessionId, tableName, page, pageSize, whereClause }) as any
}

/**
 * 执行SQL语句
 */
export const executeSql = (sessionId: string, sql: string): Promise<{ data: SqlResult }> => {
  return request.post('/db-manager/execute-sql', { sessionId, sql }) as any
}

/**
 * 获取视图定义
 */
export const getViewDefinition = (sessionId: string, viewName: string): Promise<{ data: string }> => {
  return request.post('/db-manager/view-definition', { sessionId, viewName }) as any
}

/**
 * 获取存储过程定义
 */
export const getProcedureDefinition = (sessionId: string, procedureName: string): Promise<{ data: string }> => {
  return request.post('/db-manager/procedure-definition', { sessionId, procedureName }) as any
}

/**
 * 获取表索引
 */
export const getTableIndexes = (sessionId: string, tableName: string): Promise<{ data: any[] }> => {
  return request.post('/db-manager/table-indexes', { sessionId, tableName }) as any
}

// ==================== SQL历史 ====================

/**
 * 获取SQL执行历史
 */
export const getSqlHistory = (params: {
  sessionId?: string
  keyword?: string
  status?: string
  page?: number
  pageSize?: number
}): Promise<{ data: { records: any[]; total: number } }> => {
  return request.get('/db-manager/sql-history', { params }) as any
}

/**
 * 清空会话SQL历史
 */
export const clearSqlHistory = (sessionId: string): Promise<{ data: string }> => {
  return request.delete(`/db-manager/sql-history/${sessionId}`) as any
}

// ==================== SQL收藏 ====================

export const saveSnippet = (data: { name: string; sqlContent: string; description?: string; dbType?: string }): Promise<{ data: any }> => {
  return request.post('/db-manager/sql-snippet', data) as any
}

export const updateSnippet = (id: number, data: { name?: string; sqlContent?: string; description?: string }): Promise<{ data: string }> => {
  return request.put(`/db-manager/sql-snippet/${id}`, data) as any
}

export const deleteSnippet = (id: number): Promise<{ data: string }> => {
  return request.delete(`/db-manager/sql-snippet/${id}`) as any
}

export const listSnippets = (params?: { keyword?: string; dbType?: string; page?: number; pageSize?: number }): Promise<{ data: { list: any[]; total: number } }> => {
  return request.get('/db-manager/sql-snippets', { params }) as any
}

// ==================== 导出查询结果 ====================

/**
 * 导出查询结果为 Excel
 */
export const exportQueryResult = (sessionId: string, sql: string) => {
  return request.post('/db-manager/export-query', { sessionId, sql }, {
    responseType: 'blob'
  })
}

// ==================== EXPLAIN 执行计划 ====================

/**
 * 执行EXPLAIN执行计划
 */
export const explainSql = (sessionId: string, sql: string): Promise<{ data: any }> => {
  return request.post('/db-manager/explain', { sessionId, sql }) as any
}
