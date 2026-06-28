import request from './request'
import type { ApiResponse } from '@/types/api'

/**
 * SQL 自动补全 API
 * 需求: 2.3 - WHEN 用户在 SQL 编辑器中输入时，THE Report_Designer SHALL 提供表名和字段名的自动补全建议
 */

/** 补全项类型 */
export interface CompletionItem {
  label: string       // 显示文本
  type: 'keyword' | 'table' | 'column'  // 类型
  detail?: string     // 详细信息（如数据类型）
  info?: string       // 附加信息（如所属表名）
  documentation?: string  // 文档/注释
}

/** 字段信息 */
export interface ColumnInfo {
  name: string
  dataType: string
  nullable: boolean
  remarks?: string
  columnSize?: number
}

/**
 * 获取自动补全建议
 * @param dataSourceId 数据源ID
 * @param prefix 输入前缀
 * @param context SQL上下文（可选）
 */
export const getCompletions = (dataSourceId?: number, prefix?: string, context?: string) => {
  return request.get<ApiResponse<CompletionItem[]>>('/sql-autocomplete/completions', {
    params: { dataSourceId, prefix, context }
  })
}

/**
 * 获取数据源的表名列表
 * @param dataSourceId 数据源ID
 */
export const getTableNames = (dataSourceId: number) => {
  return request.get<ApiResponse<string[]>>('/sql-autocomplete/tables', {
    params: { dataSourceId }
  })
}

/**
 * 获取表的字段列表
 * @param dataSourceId 数据源ID
 * @param tableName 表名
 */
export const getTableColumns = (dataSourceId: number, tableName: string) => {
  return request.get<ApiResponse<ColumnInfo[]>>('/sql-autocomplete/columns', {
    params: { dataSourceId, tableName }
  })
}

/**
 * 获取SQL关键字列表
 */
export const getSqlKeywords = () => {
  return request.get<ApiResponse<string[]>>('/sql-autocomplete/keywords')
}

/**
 * 刷新数据源元数据缓存
 * @param dataSourceId 数据源ID
 */
export const refreshMetadata = (dataSourceId: number) => {
  return request.post<ApiResponse<void>>('/sql-autocomplete/refresh', null, {
    params: { dataSourceId }
  })
}
