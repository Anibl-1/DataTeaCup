/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 图表数据处理工具
 * 提供数据聚合、转换、过滤等功能
 */

export type AggregateFunction = 'SUM' | 'AVG' | 'COUNT' | 'MAX' | 'MIN' | 'NONE'

export interface DataProcessOptions {
  groupBy?: string
  aggregates?: { [field: string]: AggregateFunction }
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
  limit?: number
  filters?: Array<{
    field: string
    operator: string
    value: any
  }>
}

/**
 * 数据聚合处理
 */
export function aggregateData(
  data: any[],
  groupByField: string,
  aggregates: { [field: string]: AggregateFunction }
): any[] {
  if (!data || data.length === 0) return []

  const groups = new Map<string, any[]>()

  // 分组
  data.forEach(row => {
    const key = String(row[groupByField] ?? 'null')
    if (!groups.has(key)) {
      groups.set(key, [])
    }
    groups.get(key)!.push(row)
  })

  // 聚合
  const result: any[] = []
  groups.forEach((rows, key) => {
    const aggregatedRow: any = { [groupByField]: key === 'null' ? null : key }

    Object.entries(aggregates).forEach(([field, func]) => {
      aggregatedRow[field] = calculateAggregate(rows, field, func)
    })

    result.push(aggregatedRow)
  })

  return result
}

/**
 * 计算聚合值
 */
function calculateAggregate(
  rows: any[],
  field: string,
  func: AggregateFunction
): number | null {
  const values = rows
    .map(row => row[field])
    .filter(v => v != null && !isNaN(Number(v)))
    .map(Number)

  if (values.length === 0) return null

  switch (func) {
    case 'SUM':
      return values.reduce((a, b) => a + b, 0)
    case 'AVG':
      return values.reduce((a, b) => a + b, 0) / values.length
    case 'COUNT':
      return values.length
    case 'MAX':
      return Math.max(...values)
    case 'MIN':
      return Math.min(...values)
    case 'NONE':
    default:
      return values[0] ?? null
  }
}

/**
 * 数据排序
 */
export function sortData(
  data: any[],
  sortBy: string,
  order: 'asc' | 'desc' = 'asc'
): any[] {
  return [...data].sort((a, b) => {
    const aVal = a[sortBy]
    const bVal = b[sortBy]

    if (aVal == null && bVal == null) return 0
    if (aVal == null) return order === 'asc' ? 1 : -1
    if (bVal == null) return order === 'asc' ? -1 : 1

    if (typeof aVal === 'number' && typeof bVal === 'number') {
      return order === 'asc' ? aVal - bVal : bVal - aVal
    }

    const strA = String(aVal)
    const strB = String(bVal)
    return order === 'asc' ? strA.localeCompare(strB) : strB.localeCompare(strA)
  })
}

/**
 * 数据过滤
 */
export function filterData(
  data: any[],
  filters: Array<{ field: string; operator: string; value: any }>
): any[] {
  if (!filters || filters.length === 0) return data

  return data.filter(row => {
    return filters.every(filter => {
      const fieldValue = row[filter.field]
      const filterValue = filter.value

      switch (filter.operator) {
        case '=':
        case '==':
          return fieldValue == filterValue
        case '!=':
        case '<>':
          return fieldValue != filterValue
        case '>':
          return Number(fieldValue) > Number(filterValue)
        case '>=':
          return Number(fieldValue) >= Number(filterValue)
        case '<':
          return Number(fieldValue) < Number(filterValue)
        case '<=':
          return Number(fieldValue) <= Number(filterValue)
        case 'LIKE':
          return String(fieldValue).includes(String(filterValue))
        case 'NOT LIKE':
          return !String(fieldValue).includes(String(filterValue))
        case 'IN': {
          const inValues = Array.isArray(filterValue) ? filterValue : [filterValue]
          return inValues.includes(fieldValue)
        }
        case 'NOT IN': {
          const notInValues = Array.isArray(filterValue) ? filterValue : [filterValue]
          return !notInValues.includes(fieldValue)
        }
        case 'IS NULL':
          return fieldValue == null
        case 'IS NOT NULL':
          return fieldValue != null
        case 'BETWEEN':
          if (Array.isArray(filterValue) && filterValue.length === 2) {
            const num = Number(fieldValue)
            return num >= Number(filterValue[0]) && num <= Number(filterValue[1])
          }
          return true
        default:
          return true
      }
    })
  })
}

/**
 * 数据透视
 */
export function pivotData(
  data: any[],
  rowField: string,
  colField: string,
  valueField: string,
  aggregateFunc: AggregateFunction = 'SUM'
): { headers: string[]; rows: any[] } {
  if (!data || data.length === 0) {
    return { headers: [], rows: [] }
  }

  // 获取所有列值
  const colValues = [...new Set(data.map(row => String(row[colField] ?? 'null')))]
  colValues.sort()

  // 按行分组
  const rowGroups = new Map<string, Map<string, any[]>>()
  data.forEach(row => {
    const rowKey = String(row[rowField] ?? 'null')
    const colKey = String(row[colField] ?? 'null')

    if (!rowGroups.has(rowKey)) {
      rowGroups.set(rowKey, new Map())
    }
    const colMap = rowGroups.get(rowKey)!
    if (!colMap.has(colKey)) {
      colMap.set(colKey, [])
    }
    colMap.get(colKey)!.push(row)
  })

  // 构建结果
  const headers = [rowField, ...colValues]
  const rows: any[] = []

  rowGroups.forEach((colMap, rowKey) => {
    const row: any = { [rowField]: rowKey === 'null' ? null : rowKey }
    colValues.forEach(colKey => {
      const cellData = colMap.get(colKey) || []
      row[colKey] = calculateAggregate(cellData, valueField, aggregateFunc)
    })
    rows.push(row)
  })

  return { headers, rows }
}

/**
 * 计算统计信息
 */
export function calculateStatistics(
  data: any[],
  field: string
): {
  count: number
  sum: number
  avg: number
  min: number
  max: number
  median: number
  stdDev: number
} {
  const values = data
    .map(row => row[field])
    .filter(v => v != null && !isNaN(Number(v)))
    .map(Number)
    .sort((a, b) => a - b)

  if (values.length === 0) {
    return { count: 0, sum: 0, avg: 0, min: 0, max: 0, median: 0, stdDev: 0 }
  }

  const count = values.length
  const sum = values.reduce((a, b) => a + b, 0)
  const avg = sum / count
  const min = values[0]!
  const max = values[count - 1]!

  // 中位数
  const median = count % 2 === 0
    ? (values[count / 2 - 1]! + values[count / 2]!) / 2
    : values[Math.floor(count / 2)]!

  // 标准差
  const squaredDiffs = values.map(v => Math.pow(v - avg, 2))
  const avgSquaredDiff = squaredDiffs.reduce((a, b) => a + b, 0) / count
  const stdDev = Math.sqrt(avgSquaredDiff)

  return { count, sum, avg, min, max, median, stdDev }
}

/**
 * 数据分桶（用于直方图）
 */
export function bucketData(
  data: any[],
  field: string,
  bucketCount: number = 10
): Array<{ range: string; count: number; min: number; max: number }> {
  const values = data
    .map(row => row[field])
    .filter(v => v != null && !isNaN(Number(v)))
    .map(Number)

  if (values.length === 0) return []

  const min = Math.min(...values)
  const max = Math.max(...values)
  const bucketSize = (max - min) / bucketCount

  const buckets: Array<{ range: string; count: number; min: number; max: number }> = []

  for (let i = 0; i < bucketCount; i++) {
    const bucketMin = min + i * bucketSize
    const bucketMax = i === bucketCount - 1 ? max : min + (i + 1) * bucketSize
    const count = values.filter(v =>
      i === bucketCount - 1
        ? v >= bucketMin && v <= bucketMax
        : v >= bucketMin && v < bucketMax
    ).length

    buckets.push({
      range: `${bucketMin.toFixed(2)} - ${bucketMax.toFixed(2)}`,
      count,
      min: bucketMin,
      max: bucketMax
    })
  }

  return buckets
}

/**
 * 时间序列数据处理
 */
export function processTimeSeriesData(
  data: any[],
  timeField: string,
  valueField: string,
  interval: 'day' | 'week' | 'month' | 'year' = 'day'
): any[] {
  if (!data || data.length === 0) return []

  const groups = new Map<string, number[]>()

  data.forEach(row => {
    const date = new Date(row[timeField])
    if (isNaN(date.getTime())) return

    let key: string
    switch (interval) {
      case 'year':
        key = `${date.getFullYear()}`
        break
      case 'month':
        key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
        break
      case 'week': {
        const weekStart = new Date(date)
        weekStart.setDate(date.getDate() - date.getDay())
        key = weekStart.toISOString().split('T')[0] ?? ''
        break
      }
      case 'day':
      default:
        key = date.toISOString().split('T')[0] ?? ''
    }

    if (!groups.has(key)) {
      groups.set(key, [])
    }
    const value = Number(row[valueField])
    if (!isNaN(value)) {
      groups.get(key)!.push(value)
    }
  })

  return Array.from(groups.entries())
    .map(([time, values]) => ({
      [timeField]: time,
      [valueField]: values.reduce((a, b) => a + b, 0),
      count: values.length,
      avg: values.reduce((a, b) => a + b, 0) / values.length
    }))
    .sort((a, b) => String(a[timeField]).localeCompare(String(b[timeField])))
}

/**
 * 计算同比/环比
 */
export function calculateGrowthRate(
  data: any[],
  timeField: string,
  valueField: string,
  type: 'yoy' | 'mom' = 'mom'
): any[] {
  if (!data || data.length < 2) return data

  const sorted = [...data].sort((a, b) =>
    String(a[timeField]).localeCompare(String(b[timeField]))
  )

  return sorted.map((row, index) => {
    const currentValue = Number(row[valueField]) || 0
    let previousValue = 0
    let growthRate = null

    if (type === 'mom' && index > 0) {
      // 环比：与上一期比较
      previousValue = Number(sorted[index - 1][valueField]) || 0
    } else if (type === 'yoy' && index >= 12) {
      // 同比：与去年同期比较（假设月度数据）
      previousValue = Number(sorted[index - 12][valueField]) || 0
    }

    if (previousValue !== 0) {
      growthRate = ((currentValue - previousValue) / previousValue) * 100
    }

    return {
      ...row,
      previousValue,
      growthRate: growthRate !== null ? Number(growthRate.toFixed(2)) : null
    }
  })
}

/**
 * 数据去重
 */
export function deduplicateData(
  data: any[],
  keyFields: string[]
): any[] {
  const seen = new Set<string>()
  return data.filter(row => {
    const key = keyFields.map(f => String(row[f] ?? '')).join('|')
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

/**
 * 数据填充（处理缺失值）
 */
export function fillMissingValues(
  data: any[],
  field: string,
  method: 'zero' | 'mean' | 'median' | 'forward' | 'backward' = 'zero'
): any[] {
  const values = data
    .map(row => row[field])
    .filter(v => v != null && !isNaN(Number(v)))
    .map(Number)

  let fillValue: number
  switch (method) {
    case 'mean':
      fillValue = values.length > 0 ? values.reduce((a, b) => a + b, 0) / values.length : 0
      break
    case 'median': {
      const sorted = [...values].sort((a, b) => a - b)
      fillValue = sorted.length > 0
        ? sorted.length % 2 === 0
          ? (sorted[sorted.length / 2 - 1]! + sorted[sorted.length / 2]!) / 2
          : sorted[Math.floor(sorted.length / 2)]!
        : 0
      break
    }
    case 'zero':
    default:
      fillValue = 0
  }

  if (method === 'forward' || method === 'backward') {
    const result = [...data]
    if (method === 'forward') {
      let lastValue = fillValue
      for (let i = 0; i < result.length; i++) {
        if (result[i][field] == null || isNaN(Number(result[i][field]))) {
          result[i] = { ...result[i], [field]: lastValue }
        } else {
          lastValue = Number(result[i][field])
        }
      }
    } else {
      let lastValue = fillValue
      for (let i = result.length - 1; i >= 0; i--) {
        if (result[i][field] == null || isNaN(Number(result[i][field]))) {
          result[i] = { ...result[i], [field]: lastValue }
        } else {
          lastValue = Number(result[i][field])
        }
      }
    }
    return result
  }

  return data.map(row => ({
    ...row,
    [field]: row[field] == null || isNaN(Number(row[field])) ? fillValue : row[field]
  }))
}
