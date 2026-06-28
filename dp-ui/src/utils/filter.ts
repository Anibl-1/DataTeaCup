/* eslint-disable @typescript-eslint/no-explicit-any */
import type { FilterCondition } from '@/types/api'

/**
 * 筛选数据
 * @param data 原始数据数组
 * @param filters 筛选条件数组
 * @returns 筛选后的数据数组
 */
export function filterData<T extends Record<string, any>>(
  data: T[],
  filters: FilterCondition[]
): T[] {
  if (!filters || filters.length === 0) {
    return data
  }

  return data.filter(item => {
    return filters.every(filter => {
      if (!filter.field || !filter.operator) {
        return true
      }

      const fieldValue = getNestedValue(item, filter.field)
      const filterValue = filter.value

      switch (filter.operator) {
        case 'eq':
          return String(fieldValue) === String(filterValue)
        case 'ne':
          return String(fieldValue) !== String(filterValue)
        case 'contains':
          return String(fieldValue).toLowerCase().includes(String(filterValue).toLowerCase())
        case 'notContains':
          return !String(fieldValue).toLowerCase().includes(String(filterValue).toLowerCase())
        case 'startsWith':
          return String(fieldValue).toLowerCase().startsWith(String(filterValue).toLowerCase())
        case 'endsWith':
          return String(fieldValue).toLowerCase().endsWith(String(filterValue).toLowerCase())
        case 'gt':
          return Number(fieldValue) > Number(filterValue)
        case 'gte':
          return Number(fieldValue) >= Number(filterValue)
        case 'lt':
          return Number(fieldValue) < Number(filterValue)
        case 'lte':
          return Number(fieldValue) <= Number(filterValue)
        case 'like':
          return String(fieldValue).toLowerCase().includes(String(filterValue).toLowerCase())
        case 'in':
          if (Array.isArray(filterValue)) {
            return filterValue.some(v => String(fieldValue) === String(v))
          }
          return String(fieldValue) === String(filterValue)
        case 'isNull':
          return fieldValue === null || fieldValue === undefined || fieldValue === ''
        case 'isNotNull':
          return fieldValue !== null && fieldValue !== undefined && fieldValue !== ''
        default:
          return true
      }
    })
  })
}

/**
 * 获取嵌套对象的值
 * @param obj 对象
 * @param path 路径，支持点号分隔的嵌套路径
 * @returns 值
 */
function getNestedValue(obj: any, path: string): any {
  if (!path) return undefined
  
  const keys = path.split('.')
  let value = obj
  
  for (const key of keys) {
    if (value === null || value === undefined) {
      return undefined
    }
    value = value[key]
  }
  
  return value
}

