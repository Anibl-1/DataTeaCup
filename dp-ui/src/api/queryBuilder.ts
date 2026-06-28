/* eslint-disable @typescript-eslint/no-explicit-any */
import request from './request'
import type { QueryModel, TableMeta } from '@/types/queryBuilder'

/**
 * 生成 SQL
 */
export function generateSql(model: QueryModel) {
  return request<string>({
    url: '/query-builder/generate-sql',
    method: 'post',
    data: model
  })
}

/**
 * 解析 SQL
 */
export function parseSql(sql: string) {
  return request<QueryModel>({
    url: '/query-builder/parse-sql',
    method: 'post',
    data: sql,
    headers: { 'Content-Type': 'text/plain' }
  })
}

/**
 * 预览查询结果
 */
export function previewQuery(dataSourceId: number, model: QueryModel, limit: number = 100) {
  return request<Record<string, any>[]>({
    url: '/query-builder/preview',
    method: 'post',
    params: { dataSourceId, limit },
    data: model
  })
}

/**
 * 执行 SQL 查询
 */
export function executeQuery(dataSourceId: number, sql: string) {
  return request<Record<string, any>[]>({
    url: '/query-builder/execute',
    method: 'post',
    params: { dataSourceId },
    data: sql,
    headers: { 'Content-Type': 'text/plain' }
  })
}

/**
 * 获取表元数据（全量，含列信息）
 */
export function getTableMeta(dataSourceId: number) {
  return request<TableMeta[]>({
    url: '/query-builder/table-meta',
    method: 'get',
    params: { dataSourceId }
  })
}

/**
 * 获取表名列表（轻量，不含列信息）
 */
export function getTableNames(dataSourceId: number) {
  return request<string[]>({
    url: '/query-builder/table-names',
    method: 'get',
    params: { dataSourceId }
  })
}

/**
 * 获取单个表的列元数据（懒加载）
 */
export function getColumnMeta(dataSourceId: number, tableName: string) {
  return request<TableMeta>({
    url: '/query-builder/column-meta',
    method: 'get',
    params: { dataSourceId, tableName }
  })
}
