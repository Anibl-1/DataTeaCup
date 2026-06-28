/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * API响应类型定义
 */

/**
 * 统一API响应结构
 * 注意：request拦截器会自动解包，实际返回的就是这个结构
 */
export interface ApiResponse<T = any> {
  /** 响应码 */
  code: number
  /** 响应消息 */
  msg: string
  /** 响应数据 */
  data: T
  /** 响应时间戳 */
  timestamp?: number
}

/**
 * 分页响应数据
 */
export interface PageResult<T> {
  /** 数据列表 */
  list: T[]
  /** 总记录数 */
  total: number
  /** 当前页码 */
  page?: number
  /** 每页大小 */
  pageSize?: number
}

/**
 * 分页列表接口的统一响应类型
 * 等价于 ApiResponse<PageResult<T>>，简化 API 函数签名
 */
export type ApiListResponse<T> = ApiResponse<PageResult<T>>

/**
 * 单条数据接口的统一响应类型
 * 等价于 ApiResponse<T>，语义更明确
 */
export type ApiDetailResponse<T> = ApiResponse<T>

/**
 * 无数据返回的操作响应类型（如删除、更新）
 */
export type ApiVoidResponse = ApiResponse<void>

/**
 * 从API响应中提取数据的辅助类型
 */
export type ExtractData<T> = T extends ApiResponse<infer U> ? U : T

/**
 * 安全获取分页数据
 */
export function getPageData<T>(res: ApiResponse<PageResult<T>> | any): T[] {
  if (!res) return []
  // 直接是数组
  if (Array.isArray(res)) return res
  // res.data.list 模式
  if (res.data?.list) return res.data.list
  // res.list 模式
  if (res.list) return res.list
  // res.data 是数组
  if (Array.isArray(res.data)) return res.data
  return []
}

/**
 * 安全获取分页总数
 */
export function getPageTotal<T>(res: ApiResponse<PageResult<T>> | any): number {
  if (!res) return 0
  if (res.data?.total !== undefined) return res.data.total
  if (res.total !== undefined) return res.total
  return 0
}

/**
 * 安全获取响应数据
 */
export function getData<T>(res: ApiResponse<T> | any): T | null {
  if (!res) return null
  if (res.data !== undefined) return res.data
  return res
}

/**
 * 筛选条件
 */
export interface FilterCondition {
  /** 字段名 */
  field: string
  /** 操作符 */
  operator: 'eq' | 'ne' | 'contains' | 'notContains' | 'startsWith' | 'endsWith' | 'gt' | 'gte' | 'lt' | 'lte' | 'like' | 'in' | 'isNull' | 'isNotNull'
  /** 筛选值 */
  value?: unknown
}

/**
 * 分页请求参数
 */
export interface PageParams {
  /** 页码（从1开始） */
  page?: number
  /** 每页大小 */
  pageSize?: number
  /** 搜索关键词（可选） */
  keyword?: string | undefined
  /** 筛选条件（可选） */
  filters?: FilterCondition[] | string | undefined
}

