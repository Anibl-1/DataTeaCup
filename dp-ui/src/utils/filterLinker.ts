/**
 * 筛选器联动工具
 * 用于处理筛选器与图表/SQL的联动逻辑
 */

import type { FilterValue } from '@/composables/useDashboardFilters'

/**
 * 筛选器占位符信息
 */
export interface FilterPlaceholder {
  /** 原始占位符文本 */
  placeholder: string
  /** 字段名 */
  field: string
  /** 占位符格式类型 */
  format: 'dollar' | 'mustache'
}

/**
 * 筛选参数
 */
export interface FilterParam {
  /** 字段名 */
  field: string
  /** 筛选值 */
  value: FilterValue
}

/**
 * 图表联动配置
 */
export interface ChartLinkMapping {
  /** 图表ID */
  chartId: string
  /** 关联的筛选器字段列表 */
  filterFields: string[]
}

/**
 * 解析 SQL 中的筛选器占位符
 * 支持两种格式：${field} 和 {{field}}
 * @param sql SQL 语句
 * @returns 占位符信息数组
 */
export function parseFilterPlaceholders(sql: string): FilterPlaceholder[] {
  const placeholders: FilterPlaceholder[] = []
  
  if (!sql) return placeholders
  
  // 匹配 ${field} 格式
  const dollarRegex = /\$\{(\w+)\}/g
  let match: RegExpExecArray | null
  
  while ((match = dollarRegex.exec(sql)) !== null) {
    placeholders.push({
      placeholder: match[0],
      field: match[1],
      format: 'dollar'
    })
  }
  
  // 匹配 {{field}} 格式
  const mustacheRegex = /\{\{(\w+)\}\}/g
  
  while ((match = mustacheRegex.exec(sql)) !== null) {
    placeholders.push({
      placeholder: match[0],
      field: match[1],
      format: 'mustache'
    })
  }
  
  return placeholders
}

/**
 * 格式化筛选值用于 SQL
 * @param value 筛选值
 * @returns 格式化后的值
 */
function formatValueForSql(value: FilterValue): string {
  if (value === null || value === undefined) {
    return 'NULL'
  }
  
  if (typeof value === 'string') {
    // 转义单引号，防止 SQL 注入
    return `'${value.replace(/'/g, "''")}'`
  }
  
  if (typeof value === 'number') {
    return String(value)
  }
  
  if (typeof value === 'boolean') {
    return value ? '1' : '0'
  }
  
  if (Array.isArray(value)) {
    // 日期范围或多选值
    if (value.length === 2 && typeof value[0] === 'string' && typeof value[1] === 'string') {
      // 日期范围，返回两个值用于 BETWEEN
      return `'${value[0].replace(/'/g, "''")}' AND '${value[1].replace(/'/g, "''")}'`
    }
    // 多选值，返回 IN 格式
    const formattedValues = value.map(v => 
      typeof v === 'string' ? `'${v.replace(/'/g, "''")}'` : String(v)
    )
    return `(${formattedValues.join(', ')})`
  }
  
  return String(value)
}

/**
 * 将筛选器值应用到 SQL 占位符
 * @param sql 原始 SQL 语句
 * @param filters 筛选器值对象 { field: value }
 * @returns 替换后的 SQL 语句
 */
export function applyFiltersToSql(
  sql: string,
  filters: Record<string, FilterValue>
): string {
  if (!sql) return sql
  
  let result = sql
  
  // 解析所有占位符
  const placeholders = parseFilterPlaceholders(sql)
  
  // 替换占位符
  placeholders.forEach(({ placeholder, field }) => {
    const value = filters[field]
    
    if (value !== null && value !== undefined && value !== '') {
      const formattedValue = formatValueForSql(value)
      result = result.replace(placeholder, formattedValue)
    }
  })
  
  return result
}

/**
 * 构建筛选参数用于 API 调用
 * @param filters 筛选器值对象 { filterId: value }
 * @param filterConfigs 筛选器配置 { filterId: { field, linkedCharts } }
 * @param linkedCharts 目标图表ID列表（可选，用于过滤）
 * @returns 筛选参数数组
 */
export function buildFilterParams(
  filters: Record<string, FilterValue>,
  filterConfigs: Record<string, { field: string; linkedCharts: string[] }>,
  linkedCharts?: string[]
): FilterParam[] {
  const params: FilterParam[] = []
  
  Object.entries(filters).forEach(([filterId, value]) => {
    // 跳过空值
    if (value === null || value === undefined || value === '') {
      return
    }
    
    const config = filterConfigs[filterId]
    if (!config || !config.field) {
      return
    }
    
    // 如果指定了目标图表，检查筛选器是否关联到这些图表
    if (linkedCharts && linkedCharts.length > 0) {
      const hasLinkedChart = config.linkedCharts.some(
        chartId => linkedCharts.includes(chartId)
      )
      if (!hasLinkedChart) {
        return
      }
    }
    
    params.push({
      field: config.field,
      value
    })
  })
  
  return params
}

/**
 * 将筛选参数转换为查询字符串
 * @param params 筛选参数数组
 * @returns 查询字符串
 */
export function filterParamsToQueryString(params: FilterParam[]): string {
  if (!params || params.length === 0) {
    return ''
  }
  
  const queryParts = params.map(param => {
    const value = param.value
    
    if (Array.isArray(value)) {
      // 数组值，使用逗号分隔
      return `${encodeURIComponent(param.field)}=${encodeURIComponent(value.join(','))}`
    }
    
    return `${encodeURIComponent(param.field)}=${encodeURIComponent(String(value))}`
  })
  
  return queryParts.join('&')
}

/**
 * 将筛选参数转换为 JSON 对象
 * @param params 筛选参数数组
 * @returns JSON 对象
 */
export function filterParamsToJson(params: FilterParam[]): Record<string, FilterValue> {
  const result: Record<string, FilterValue> = {}
  
  params.forEach(param => {
    result[param.field] = param.value
  })
  
  return result
}

/**
 * 检查 SQL 是否包含筛选器占位符
 * @param sql SQL 语句
 * @returns 是否包含占位符
 */
export function hasFilterPlaceholders(sql: string): boolean {
  if (!sql) return false
  return parseFilterPlaceholders(sql).length > 0
}

/**
 * 获取 SQL 中所有需要的筛选字段
 * @param sql SQL 语句
 * @returns 字段名数组
 */
export function getRequiredFilterFields(sql: string): string[] {
  const placeholders = parseFilterPlaceholders(sql)
  return [...new Set(placeholders.map(p => p.field))]
}

/**
 * 验证筛选器值是否满足 SQL 要求
 * @param sql SQL 语句
 * @param filters 筛选器值对象
 * @returns 缺失的字段名数组
 */
export function validateFiltersForSql(
  sql: string,
  filters: Record<string, FilterValue>
): string[] {
  const requiredFields = getRequiredFilterFields(sql)
  const missingFields: string[] = []
  
  requiredFields.forEach(field => {
    const value = filters[field]
    if (value === null || value === undefined || value === '') {
      missingFields.push(field)
    }
  })
  
  return missingFields
}

/**
 * 构建带有默认值的 SQL
 * 对于缺失的筛选值，使用默认值或移除条件
 * @param sql SQL 语句
 * @param filters 筛选器值对象
 * @param defaultValues 默认值对象
 * @returns 处理后的 SQL
 */
export function buildSqlWithDefaults(
  sql: string,
  filters: Record<string, FilterValue>,
  defaultValues: Record<string, FilterValue> = {}
): string {
  const mergedFilters = { ...defaultValues, ...filters }
  return applyFiltersToSql(sql, mergedFilters)
}

/**
 * 创建筛选器联动管理器
 * 用于管理多个图表与筛选器的联动关系
 */
export function createFilterLinker() {
  // 图表与筛选器的映射关系
  const chartFilterMap = new Map<string, Set<string>>()
  
  // 筛选器与图表的映射关系
  const filterChartMap = new Map<string, Set<string>>()
  
  /**
   * 注册图表与筛选器的联动关系
   * @param chartId 图表ID
   * @param filterFields 关联的筛选器字段列表
   */
  const registerChartLink = (chartId: string, filterFields: string[]) => {
    chartFilterMap.set(chartId, new Set(filterFields))
    
    filterFields.forEach(field => {
      if (!filterChartMap.has(field)) {
        filterChartMap.set(field, new Set())
      }
      filterChartMap.get(field)!.add(chartId)
    })
  }
  
  /**
   * 注销图表的联动关系
   * @param chartId 图表ID
   */
  const unregisterChartLink = (chartId: string) => {
    const fields = chartFilterMap.get(chartId)
    if (fields) {
      fields.forEach(field => {
        filterChartMap.get(field)?.delete(chartId)
      })
    }
    chartFilterMap.delete(chartId)
  }
  
  /**
   * 获取受筛选器影响的图表列表
   * @param filterField 筛选器字段
   * @returns 图表ID数组
   */
  const getAffectedCharts = (filterField: string): string[] => {
    return Array.from(filterChartMap.get(filterField) || [])
  }
  
  /**
   * 获取图表关联的筛选器字段
   * @param chartId 图表ID
   * @returns 筛选器字段数组
   */
  const getChartFilters = (chartId: string): string[] => {
    return Array.from(chartFilterMap.get(chartId) || [])
  }
  
  /**
   * 清除所有联动关系
   */
  const clearAllLinks = () => {
    chartFilterMap.clear()
    filterChartMap.clear()
  }
  
  return {
    registerChartLink,
    unregisterChartLink,
    getAffectedCharts,
    getChartFilters,
    clearAllLinks
  }
}
