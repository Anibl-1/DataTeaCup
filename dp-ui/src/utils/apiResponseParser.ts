/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * API 响应解析器
 * 将各种格式的分页 API 响应统一解析为标准格式
 */

/** 标准列表响应格式 */
export interface StandardListResponse<T> {
  list: T[]
  total: number
}

/**
 * 将各种格式的分页响应统一解析为标准格式
 *
 * 支持的格式：
 * - { data: { records: [], total } }
 * - { data: { list: [], total } }
 * - { data: { rows: [], total } }
 * - { data: [] }  (data 为数组)
 * - { list: [], total }  (顶层 list)
 * - { records: [], total }  (顶层 records)
 * - { rows: [], total }  (顶层 rows)
 * - 直接数组
 *
 * 无法识别时返回 { list: [], total: 0 }，不抛出异常
 */
export function parseListResponse<T>(response: any): StandardListResponse<T> {
  const empty: StandardListResponse<T> = { list: [], total: 0 }

  if (response == null) return empty

  // 直接是数组
  if (Array.isArray(response)) {
    return { list: response, total: response.length }
  }

  const data = response.data

  // data 嵌套对象：data.records / data.list / data.rows
  if (data != null && typeof data === 'object' && !Array.isArray(data)) {
    const list = extractList<T>(data)
    if (list) {
      return { list, total: toNumber(data.total, list.length) }
    }
  }

  // data 是数组
  if (Array.isArray(data)) {
    return { list: data, total: data.length }
  }

  // 顶层 list / records / rows
  const list = extractList<T>(response)
  if (list) {
    return { list, total: toNumber(response.total, list.length) }
  }

  return empty
}

/** 从对象中按优先级提取列表字段 */
function extractList<T>(obj: any): T[] | null {
  for (const key of ['records', 'list', 'rows']) {
    if (Array.isArray(obj[key])) {
      return obj[key]
    }
  }
  return null
}

/** 安全转换为数字，无效时使用 fallback */
function toNumber(value: unknown, fallback: number): number {
  if (typeof value === 'number' && !Number.isNaN(value)) return value
  return fallback
}

/** 序列化标准列表响应为 JSON 字符串 */
export function serializeListResponse<T>(response: StandardListResponse<T>): string {
  return JSON.stringify(response)
}

/** 反序列化 JSON 字符串为标准列表响应 */
export function deserializeListResponse<T>(json: string): StandardListResponse<T> {
  return JSON.parse(json) as StandardListResponse<T>
}
