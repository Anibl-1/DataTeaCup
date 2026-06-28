/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 数据透视计算工具函数
 * Pivot Table Calculation Utilities
 * 
 * 支持多级分组汇总、聚合计算、小计和总计生成
 * Supports multi-level grouping, aggregation calculations, subtotals and totals generation
 * 
 * 需求: 22.3.8, 22.3.9
 */

import type {
  DimensionField,
  ValueField,
  FilterField,
  PivotConfig,
  TotalConfig,
  PivotCell,
  PivotRowHeader,
  PivotColumnHeader,
  PivotData,
  AggregationType,
  NumberFormat,
  CellKeyGenerator,
} from './types'
import {
  DEFAULT_TOTAL_CONFIG,
  DEFAULT_NUMBER_FORMAT,
  defaultCellKeyGenerator,
} from './types'

// ============================================================================
// Aggregation Functions (聚合函数)
// ============================================================================

/**
 * Aggregation function map
 * 聚合函数映射
 * 
 * Validates: 22.3.8 - 支持多级分组汇总
 */
export const aggregationFunctions: Record<AggregationType, (values: any[]) => any> = {
  sum: (values) => values.reduce((acc, val) => acc + (Number(val) || 0), 0),
  count: (values) => values.length,
  avg: (values) => {
    const nums = values.filter(v => v != null && !isNaN(Number(v)))
    if (nums.length === 0) return 0
    return nums.reduce((acc, val) => acc + Number(val), 0) / nums.length
  },
  min: (values) => {
    const nums = values.filter(v => v != null && !isNaN(Number(v))).map(Number)
    return nums.length > 0 ? Math.min(...nums) : null
  },
  max: (values) => {
    const nums = values.filter(v => v != null && !isNaN(Number(v))).map(Number)
    return nums.length > 0 ? Math.max(...nums) : null
  },
  countDistinct: (values) => new Set(values.filter(v => v != null)).size,
  first: (values) => values.length > 0 ? values[0] : null,
  last: (values) => values.length > 0 ? values[values.length - 1] : null,
  custom: (values) => values, // Custom aggregation handled separately
}

/**
 * Calculate aggregated value
 * 计算聚合值
 */
export function calculateAggregation(
  values: any[],
  aggregationType: AggregationType,
  customFn?: (values: any[]) => any
): any {
  if (aggregationType === 'custom' && customFn) {
    return customFn(values)
  }
  return aggregationFunctions[aggregationType](values)
}

// ============================================================================
// Number Formatting (数值格式化)
// ============================================================================

/**
 * Format number value
 * 格式化数值
 */
export function formatNumber(value: any, format: NumberFormat = DEFAULT_NUMBER_FORMAT): string {
  if (value == null || isNaN(Number(value))) {
    return '-'
  }

  let num = Number(value)
  
  // Handle percentage
  if (format.asPercentage) {
    num = num * 100
  }

  // Apply decimal places
  const decimalPlaces = format.decimalPlaces ?? 2
  let formatted = num.toFixed(decimalPlaces)

  // Apply thousands separator
  if (format.useThousandsSeparator) {
    const parts = formatted.split('.')
    parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')
    formatted = parts.join('.')
  }

  // Apply prefix and suffix
  const prefix = format.prefix ?? ''
  const suffix = format.suffix ?? (format.asPercentage ? '%' : '')
  
  return `${prefix}${formatted}${suffix}`
}

// ============================================================================
// Data Filtering (数据筛选)
// ============================================================================

/**
 * Apply filters to data
 * 应用筛选条件
 */
export function applyFilters(
  data: Record<string, any>[],
  filters: FilterField[]
): Record<string, any>[] {
  if (!filters || filters.length === 0) {
    return data
  }

  return data.filter(row => {
    return filters.every(filter => {
      if (!filter.values || filter.values.length === 0) {
        return true
      }

      const value = row[filter.field]
      
      switch (filter.filterType) {
        case 'select':
          return filter.values.includes(value)
        case 'multiSelect':
          return filter.values.includes(value)
        case 'range':
          if (filter.values.length === 2) {
            const [min, max] = filter.values
            return value >= min && value <= max
          }
          return true
        case 'search':
          const searchTerm = filter.values[0]?.toString().toLowerCase()
          return value?.toString().toLowerCase().includes(searchTerm)
        default:
          return true
      }
    })
  })
}

// ============================================================================
// Grouping Functions (分组函数)
// ============================================================================

/**
 * Group data by dimension fields
 * 按维度字段分组数据
 * 
 * Validates: 22.3.8 - 支持多级分组汇总
 */
export function groupByDimensions(
  data: Record<string, any>[],
  dimensions: DimensionField[]
): Map<string, Record<string, any>[]> {
  const groups = new Map<string, Record<string, any>[]>()

  if (dimensions.length === 0) {
    groups.set('', data)
    return groups
  }

  for (const row of data) {
    const key = dimensions.map(dim => row[dim.field] ?? '').join('|')
    
    if (!groups.has(key)) {
      groups.set(key, [])
    }
    groups.get(key)!.push(row)
  }

  return groups
}

/**
 * Build hierarchical headers from grouped data
 * 从分组数据构建层级表头
 */
export function buildHierarchicalHeaders(
  data: Record<string, any>[],
  dimensions: DimensionField[],
  isRow: boolean,
  totalConfig: TotalConfig
): PivotRowHeader[] | PivotColumnHeader[] {
  if (dimensions.length === 0) {
    return []
  }

  const uniqueValues = new Map<string, Set<any>>()

  // Collect unique values for each dimension level
  for (const dim of dimensions) {
    uniqueValues.set(dim.field, new Set())
  }

  for (const row of data) {
    for (const dim of dimensions) {
      uniqueValues.get(dim.field)!.add(row[dim.field])
    }
  }

  // Build tree structure recursively
  const buildLevel = (
    depth: number,
    parentPath: any[],
    parent?: PivotRowHeader | PivotColumnHeader
  ): any[] => {
    if (depth >= dimensions.length) {
      return []
    }

    const dim = dimensions[depth]
    const values = Array.from(uniqueValues.get(dim.field)!)
    
    // Sort values
    if (dim.sortComparator) {
      values.sort(dim.sortComparator)
    } else if (dim.sortOrder === 'asc') {
      values.sort((a, b) => String(a).localeCompare(String(b)))
    } else if (dim.sortOrder === 'desc') {
      values.sort((a, b) => String(b).localeCompare(String(a)))
    }

    const levelHeaders: any[] = []

    for (const value of values) {
      const path = [...parentPath, value]
      const label = dim.formatter ? dim.formatter(value) : String(value ?? '')
      
      const header: any = {
        value,
        label,
        depth,
        parent: parent as any,
        children: [],
        isSubtotal: false,
        isGrandTotal: false,
        expanded: true,
        rowSpan: 1,
        colSpan: 1,
        path,
      }

      // Build children
      header.children = buildLevel(depth + 1, path, header)
      
      // Calculate span
      if (header.children.length > 0) {
        if (isRow) {
          header.rowSpan = header.children.reduce(
            (sum: number, child: any) => sum + (child.rowSpan || 1), 0
          )
        } else {
          header.colSpan = header.children.reduce(
            (sum: number, child: any) => sum + (child.colSpan || 1), 0
          )
        }
      }

      levelHeaders.push(header)
    }

    // Add subtotal at this level if configured
    const showSubtotals = isRow ? totalConfig.showRowSubtotals : totalConfig.showColumnSubtotals
    const subtotalPosition = isRow ? totalConfig.rowSubtotalPosition : totalConfig.columnSubtotalPosition

    if (showSubtotals && depth < dimensions.length - 1 && levelHeaders.length > 0) {
      const subtotalHeader: any = {
        value: null,
        label: totalConfig.subtotalLabel ?? '小计',
        depth,
        parent: parent as any,
        children: [],
        isSubtotal: true,
        isGrandTotal: false,
        expanded: true,
        rowSpan: 1,
        colSpan: 1,
        path: [...parentPath, '__subtotal__'],
      }

      if (subtotalPosition === 'top' || subtotalPosition === 'left') {
        levelHeaders.unshift(subtotalHeader)
      } else {
        levelHeaders.push(subtotalHeader)
      }
    }

    return levelHeaders
  }

  const result = buildLevel(0, [], undefined)

  // Add grand total if configured
  const showGrandTotal = isRow ? totalConfig.showRowGrandTotal : totalConfig.showColumnGrandTotal
  const grandTotalPosition = isRow ? totalConfig.rowGrandTotalPosition : totalConfig.columnGrandTotalPosition

  if (showGrandTotal) {
    const grandTotalHeader: any = {
      value: null,
      label: totalConfig.grandTotalLabel ?? '总计',
      depth: 0,
      parent: undefined,
      children: [],
      isSubtotal: false,
      isGrandTotal: true,
      expanded: true,
      rowSpan: 1,
      colSpan: 1,
      path: ['__grandtotal__'],
    }

    if (grandTotalPosition === 'top' || grandTotalPosition === 'left') {
      result.unshift(grandTotalHeader)
    } else {
      result.push(grandTotalHeader)
    }
  }

  return result as (PivotRowHeader[] | PivotColumnHeader[])
}

// ============================================================================
// Flatten Headers (扁平化表头)
// ============================================================================

/**
 * Flatten hierarchical row headers for rendering
 * 扁平化行表头用于渲染
 */
export function flattenRowHeaders(headers: PivotRowHeader[]): PivotRowHeader[] {
  const result: PivotRowHeader[] = []

  const flatten = (items: PivotRowHeader[]) => {
    for (const item of items) {
      if (item.children.length === 0 || item.isSubtotal || item.isGrandTotal) {
        result.push(item)
      }
      if (item.children.length > 0 && item.expanded) {
        flatten(item.children as PivotRowHeader[])
      }
    }
  }

  flatten(headers)
  return result
}

/**
 * Flatten hierarchical column headers for rendering
 * 扁平化列表头用于渲染
 */
export function flattenColumnHeaders(headers: PivotColumnHeader[]): PivotColumnHeader[] {
  const result: PivotColumnHeader[] = []

  const flatten = (items: PivotColumnHeader[]) => {
    for (const item of items) {
      if (item.children.length === 0 || item.isSubtotal || item.isGrandTotal) {
        result.push(item)
      }
      if (item.children.length > 0) {
        flatten(item.children as PivotColumnHeader[])
      }
    }
  }

  flatten(headers)
  return result
}

// ============================================================================
// Cell Calculation (单元格计算)
// ============================================================================

/**
 * Calculate pivot cells
 * 计算透视单元格
 * 
 * Validates: 22.3.8 - 支持多级分组汇总
 */
export function calculatePivotCells(
  data: Record<string, any>[],
  config: PivotConfig,
  rowHeaders: PivotRowHeader[],
  columnHeaders: PivotColumnHeader[],
  _totalConfig: TotalConfig,
  cellKeyGenerator: CellKeyGenerator = defaultCellKeyGenerator
): Map<string, PivotCell> {
  const cells = new Map<string, PivotCell>()
  const flatRows = flattenRowHeaders(rowHeaders)
  const flatCols = flattenColumnHeaders(columnHeaders)

  // Group data by row and column dimensions
  for (const rowHeader of flatRows) {
    for (const colHeader of flatCols) {
      for (const valueField of config.values) {
        // Filter data matching this cell
        const matchingData = filterDataForCell(
          data,
          config.rows,
          config.columns,
          rowHeader,
          colHeader
        )

        // Extract values for aggregation
        const values = matchingData.map(row => row[valueField.field])

        // Calculate aggregated value
        const aggregatedValue = calculateAggregation(
          values,
          valueField.aggregation,
          valueField.customAggregation
        )

        // Format value
        const formattedValue = formatNumber(aggregatedValue, valueField.format)

        // Create cell
        const cell: PivotCell = {
          value: aggregatedValue,
          formattedValue,
          rowPath: rowHeader.path,
          columnPath: colHeader.path,
          valueFieldId: valueField.id,
          isSubtotal: rowHeader.isSubtotal || colHeader.isSubtotal,
          isGrandTotal: rowHeader.isGrandTotal || colHeader.isGrandTotal,
          rawValues: values,
          recordCount: matchingData.length,
        }

        const key = cellKeyGenerator(rowHeader.path, colHeader.path, valueField.id)
        cells.set(key, cell)
      }
    }
  }

  return cells
}

/**
 * Filter data matching a specific cell
 * 筛选匹配特定单元格的数据
 */
function filterDataForCell(
  data: Record<string, any>[],
  rowDimensions: DimensionField[],
  columnDimensions: DimensionField[],
  rowHeader: PivotRowHeader,
  colHeader: PivotColumnHeader
): Record<string, any>[] {
  return data.filter(row => {
    // Check row dimensions
    if (!rowHeader.isGrandTotal) {
      for (let i = 0; i < rowHeader.path.length; i++) {
        const pathValue = rowHeader.path[i]
        if (pathValue === '__subtotal__' || pathValue === '__grandtotal__') {
          break
        }
        if (i < rowDimensions.length) {
          const dimField = rowDimensions[i].field
          if (row[dimField] !== pathValue) {
            return false
          }
        }
      }
    }

    // Check column dimensions
    if (!colHeader.isGrandTotal) {
      for (let i = 0; i < colHeader.path.length; i++) {
        const pathValue = colHeader.path[i]
        if (pathValue === '__subtotal__' || pathValue === '__grandtotal__') {
          break
        }
        if (i < columnDimensions.length) {
          const dimField = columnDimensions[i].field
          if (row[dimField] !== pathValue) {
            return false
          }
        }
      }
    }

    return true
  })
}

// ============================================================================
// Main Pivot Calculation (主透视计算)
// ============================================================================

/**
 * Calculate complete pivot data
 * 计算完整透视数据
 * 
 * Validates: 22.3.7, 22.3.8, 22.3.9
 */
export function calculatePivotData(
  rawData: Record<string, any>[],
  config: PivotConfig,
  totalConfig: TotalConfig = DEFAULT_TOTAL_CONFIG
): PivotData {
  // Apply filters
  const filteredData = applyFilters(rawData, config.filters ?? [])

  // Build row headers
  const rowHeaders = buildHierarchicalHeaders(
    filteredData,
    config.rows,
    true,
    totalConfig
  ) as PivotRowHeader[]

  // Build column headers
  const columnHeaders = buildHierarchicalHeaders(
    filteredData,
    config.columns,
    false,
    totalConfig
  ) as PivotColumnHeader[]

  // Handle case with no dimensions
  if (config.rows.length === 0 && rowHeaders.length === 0) {
    // Add a single row for totals
    if (totalConfig.showRowGrandTotal) {
      rowHeaders.push({
        value: null,
        label: totalConfig.grandTotalLabel ?? '总计',
        depth: 0,
        parent: undefined,
        children: [],
        isSubtotal: false,
        isGrandTotal: true,
        expanded: true,
        rowSpan: 1,
        path: ['__grandtotal__'],
      })
    }
  }

  if (config.columns.length === 0 && columnHeaders.length === 0) {
    // Add columns for each value field
    for (const valueField of config.values) {
      columnHeaders.push({
        value: valueField.id,
        label: valueField.label,
        depth: 0,
        parent: undefined,
        children: [],
        isSubtotal: false,
        isGrandTotal: false,
        colSpan: 1,
        path: [valueField.id],
      })
    }
  }

  // Flatten headers
  const flattenedRowHeaders = flattenRowHeaders(rowHeaders)
  const flattenedColumnHeaders = flattenColumnHeaders(columnHeaders)

  // Calculate cells
  const cells = calculatePivotCells(
    filteredData,
    config,
    rowHeaders,
    columnHeaders,
    totalConfig
  )

  return {
    rowHeaders,
    columnHeaders,
    cells,
    flattenedRowHeaders,
    flattenedColumnHeaders,
    totalRows: flattenedRowHeaders.length,
    totalColumns: flattenedColumnHeaders.length * config.values.length,
  }
}

// ============================================================================
// Utility Functions (工具函数)
// ============================================================================

/**
 * Get unique values for a field
 * 获取字段的唯一值
 */
export function getUniqueValues(
  data: Record<string, any>[],
  field: string
): any[] {
  const values = new Set<any>()
  for (const row of data) {
    if (row[field] != null) {
      values.add(row[field])
    }
  }
  return Array.from(values)
}

/**
 * Infer field data type from values
 * 从值推断字段数据类型
 */
export function inferFieldDataType(
  data: Record<string, any>[],
  field: string
): 'string' | 'number' | 'date' | 'boolean' {
  const sampleSize = Math.min(100, data.length)
  let numberCount = 0
  let dateCount = 0
  let booleanCount = 0

  for (let i = 0; i < sampleSize; i++) {
    const value = data[i][field]
    if (value == null) continue

    if (typeof value === 'boolean') {
      booleanCount++
    } else if (typeof value === 'number' || !isNaN(Number(value))) {
      numberCount++
    } else if (value instanceof Date || !isNaN(Date.parse(value))) {
      dateCount++
    }
  }

  const threshold = sampleSize * 0.8
  if (booleanCount >= threshold) return 'boolean'
  if (numberCount >= threshold) return 'number'
  if (dateCount >= threshold) return 'date'
  return 'string'
}

/**
 * Create dimension field from data field name
 * 从数据字段名创建维度字段
 */
export function createDimensionField(
  field: string,
  data: Record<string, any>[],
  label?: string
): DimensionField {
  return {
    id: field,
    field,
    label: label ?? field,
    dataType: inferFieldDataType(data, field),
    sortOrder: 'asc',
    active: false,
  }
}

/**
 * Create value field from data field name
 * 从数据字段名创建值字段
 */
export function createValueField(
  field: string,
  label?: string,
  aggregation: AggregationType = 'sum'
): ValueField {
  return {
    id: `${field}_${aggregation}`,
    field,
    label: label ?? `${field} (${aggregation})`,
    aggregation,
    format: DEFAULT_NUMBER_FORMAT,
  }
}
