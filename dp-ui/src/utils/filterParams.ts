import type { FilterCondition } from '@/types/api'

/**
 * 将筛选条件转换为后端API参数格式
 * 后端通常接收 JSON 字符串格式的筛选条件
 * @param filters 筛选条件数组
 * @returns JSON 字符串或 undefined
 */
export function filtersToApiParam(filters: FilterCondition[]): string | undefined {
  if (!filters || filters.length === 0) {
    return undefined
  }

  // 过滤掉无效的筛选条件（没有字段或操作符的）
  const validFilters = filters.filter(
    filter => filter.field && filter.operator && 
    (filter.operator === 'isNull' || filter.operator === 'isNotNull' || filter.value !== undefined)
  )

  if (validFilters.length === 0) {
    return undefined
  }

  // 转换为后端需要的格式
  const apiFilters = validFilters.map(filter => ({
    field: filter.field,
    operator: filter.operator,
    value: filter.value
  }))

  // 返回 JSON 字符串，后端可以解析
  return JSON.stringify(apiFilters)
}

/**
 * 从后端API参数格式解析筛选条件
 * @param filtersParam JSON 字符串
 * @returns 筛选条件数组
 */
export function apiParamToFilters(filtersParam?: string): FilterCondition[] {
  if (!filtersParam) {
    return []
  }

  try {
    const parsed = JSON.parse(filtersParam)
    if (Array.isArray(parsed)) {
      return parsed as FilterCondition[]
    }
    return []
  } catch (error) {
    console.error('解析筛选条件失败:', error)
    return []
  }
}

