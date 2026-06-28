/**
 * 数据透视表属性测试
 * Pivot Table Property Tests
 * 
 * **属性 79: 数据透视表正确性**
 * **Validates: Requirements 22.3**
 * 
 * 测试内容:
 * 1. 多级分组汇总正确性
 * 2. 聚合计算正确性
 * 3. 小计和总计位置配置
 * 4. 行列维度配置
 * 5. 数据筛选正确性
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import type {
  DimensionField,
  ValueField,
  PivotConfig,
  TotalConfig,
  PivotRowHeader,
  PivotColumnHeader,
  AggregationType,
} from '../types'
import {
  DEFAULT_TOTAL_CONFIG,
  DEFAULT_PIVOT_CONFIG,
} from '../types'
import {
  calculatePivotData,
  calculateAggregation,
  formatNumber,
  applyFilters,
  groupByDimensions,
  flattenRowHeaders,
  flattenColumnHeaders,
  getUniqueValues,
} from '../pivotUtils'

// ============================================================================
// Test Data Generators (测试数据生成器)
// ============================================================================

/** Generate dimension field */
const dimensionFieldArb = (field: string, label: string): fc.Arbitrary<DimensionField> => {
  return fc.constant({
    id: field,
    field,
    label,
    dataType: 'string' as const,
    sortOrder: 'asc' as const,
  })
}

/** Generate value field */
const valueFieldArb = (field: string, label: string): fc.Arbitrary<ValueField> => {
  return fc.constantFrom('sum', 'count', 'avg', 'min', 'max').map(agg => ({
    id: `${field}_${agg}`,
    field,
    label: `${label} (${agg})`,
    aggregation: agg as AggregationType,
  }))
}

/** Generate sample data row */
const dataRowArb = fc.record({
  category: fc.constantFrom('A', 'B', 'C'),
  region: fc.constantFrom('North', 'South', 'East', 'West'),
  product: fc.constantFrom('Product1', 'Product2', 'Product3'),
  amount: fc.integer({ min: 100, max: 10000 }),
  quantity: fc.integer({ min: 1, max: 100 }),
})

/** Generate sample dataset */
const datasetArb = fc.array(dataRowArb, { minLength: 5, maxLength: 50 })

/** Generate total config */
const totalConfigArb: fc.Arbitrary<TotalConfig> = fc.record({
  showRowSubtotals: fc.boolean(),
  rowSubtotalPosition: fc.constantFrom('top', 'bottom'),
  showColumnSubtotals: fc.boolean(),
  columnSubtotalPosition: fc.constantFrom('left', 'right'),
  showRowGrandTotal: fc.boolean(),
  rowGrandTotalPosition: fc.constantFrom('top', 'bottom'),
  showColumnGrandTotal: fc.boolean(),
  columnGrandTotalPosition: fc.constantFrom('left', 'right'),
  subtotalLabel: fc.constant('小计'),
  grandTotalLabel: fc.constant('总计'),
})

// ============================================================================
// Property Tests
// ============================================================================

describe('PivotTable Property Tests', () => {

  // ==========================================================================
  // 属性 79.1: 聚合计算正确性
  // ==========================================================================
  
  describe('Property 79.1: Aggregation Calculation Correctness', () => {
    /**
     * **Validates: Requirements 22.3.8**
     * 
     * 属性 79.1.1: SUM聚合应等于所有值的总和
     */
    it('Property 79.1.1: SUM aggregation should equal sum of all values', () => {
      fc.assert(
        fc.property(
          fc.array(fc.integer({ min: -1000, max: 1000 }), { minLength: 1, maxLength: 100 }),
          (values) => {
            const result = calculateAggregation(values, 'sum')
            const expected = values.reduce((acc, val) => acc + val, 0)
            return result === expected
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 22.3.8**
     * 
     * 属性 79.1.2: COUNT聚合应等于值的数量
     */
    it('Property 79.1.2: COUNT aggregation should equal number of values', () => {
      fc.assert(
        fc.property(
          fc.array(fc.integer(), { minLength: 1, maxLength: 100 }),
          (values) => {
            const result = calculateAggregation(values, 'count')
            return result === values.length
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 22.3.8**
     * 
     * 属性 79.1.3: AVG聚合应等于总和除以数量
     */
    it('Property 79.1.3: AVG aggregation should equal sum divided by count', () => {
      fc.assert(
        fc.property(
          fc.array(fc.integer({ min: 1, max: 1000 }), { minLength: 1, maxLength: 100 }),
          (values) => {
            const result = calculateAggregation(values, 'avg')
            const expected = values.reduce((acc, val) => acc + val, 0) / values.length
            return Math.abs(result - expected) < 0.0001
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 22.3.8**
     * 
     * 属性 79.1.4: MIN聚合应等于最小值
     */
    it('Property 79.1.4: MIN aggregation should equal minimum value', () => {
      fc.assert(
        fc.property(
          fc.array(fc.integer({ min: -1000, max: 1000 }), { minLength: 1, maxLength: 100 }),
          (values) => {
            const result = calculateAggregation(values, 'min')
            const expected = Math.min(...values)
            return result === expected
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 22.3.8**
     * 
     * 属性 79.1.5: MAX聚合应等于最大值
     */
    it('Property 79.1.5: MAX aggregation should equal maximum value', () => {
      fc.assert(
        fc.property(
          fc.array(fc.integer({ min: -1000, max: 1000 }), { minLength: 1, maxLength: 100 }),
          (values) => {
            const result = calculateAggregation(values, 'max')
            const expected = Math.max(...values)
            return result === expected
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 22.3.8**
     * 
     * 属性 79.1.6: COUNT_DISTINCT聚合应等于唯一值数量
     */
    it('Property 79.1.6: COUNT_DISTINCT aggregation should equal unique value count', () => {
      fc.assert(
        fc.property(
          fc.array(fc.constantFrom('a', 'b', 'c', 'd', 'e'), { minLength: 1, maxLength: 50 }),
          (values) => {
            const result = calculateAggregation(values, 'countDistinct')
            const expected = new Set(values).size
            return result === expected
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  // ==========================================================================
  // 属性 79.2: 多级分组正确性
  // ==========================================================================
  
  describe('Property 79.2: Multi-Level Grouping Correctness', () => {
    /**
     * **Validates: Requirements 22.3.8**
     * 
     * 属性 79.2.1: 分组应包含所有原始数据
     */
    it('Property 79.2.1: Grouping should contain all original data', () => {
      fc.assert(
        fc.property(
          datasetArb,
          (data) => {
            const dimensions: DimensionField[] = [
              { id: 'category', field: 'category', label: '类别', dataType: 'string' },
            ]
            
            const groups = groupByDimensions(data, dimensions)
            
            // 所有分组中的数据总数应等于原始数据数量
            let totalCount = 0
            groups.forEach(groupData => {
              totalCount += groupData.length
            })
            
            return totalCount === data.length
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 22.3.8**
     * 
     * 属性 79.2.2: 同一分组内的数据应具有相同的维度值
     */
    it('Property 79.2.2: Data in same group should have same dimension values', () => {
      fc.assert(
        fc.property(
          datasetArb,
          (data) => {
            const dimensions: DimensionField[] = [
              { id: 'category', field: 'category', label: '类别', dataType: 'string' },
              { id: 'region', field: 'region', label: '区域', dataType: 'string' },
            ]
            
            const groups = groupByDimensions(data, dimensions)
            
            // 验证每个分组内的数据具有相同的维度值
            for (const [key, groupData] of groups) {
              if (groupData.length > 1) {
                const firstRow = groupData[0]
                for (const row of groupData) {
                  for (const dim of dimensions) {
                    if (row[dim.field] !== firstRow[dim.field]) {
                      return false
                    }
                  }
                }
              }
            }
            
            return true
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 22.3.8**
     * 
     * 属性 79.2.3: 分组数量应不超过维度值组合数
     */
    it('Property 79.2.3: Group count should not exceed dimension value combinations', () => {
      fc.assert(
        fc.property(
          datasetArb,
          (data) => {
            if (data.length === 0) return true
            
            const dimensions: DimensionField[] = [
              { id: 'category', field: 'category', label: '类别', dataType: 'string' },
            ]
            
            const groups = groupByDimensions(data, dimensions)
            const uniqueValues = getUniqueValues(data, 'category')
            
            return groups.size <= uniqueValues.length
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  // ==========================================================================
  // 属性 79.3: 小计和总计位置配置
  // ==========================================================================
  
  describe('Property 79.3: Subtotal and Total Position Configuration', () => {
    /**
     * **Validates: Requirements 22.3.9**
     * 
     * 属性 79.3.1: 启用总计时应包含总计行
     */
    it('Property 79.3.1: Grand total row should exist when enabled', () => {
      fc.assert(
        fc.property(
          datasetArb,
          fc.boolean(),
          (data, showGrandTotal) => {
            if (data.length === 0) return true
            
            const config: PivotConfig = {
              rows: [{ id: 'category', field: 'category', label: '类别', dataType: 'string' }],
              columns: [],
              values: [{ id: 'amount_sum', field: 'amount', label: '金额', aggregation: 'sum' }],
            }
            
            const totalConfig: TotalConfig = {
              ...DEFAULT_TOTAL_CONFIG,
              showRowGrandTotal: showGrandTotal,
            }
            
            const pivotData = calculatePivotData(data, config, totalConfig)
            const hasGrandTotal = pivotData.flattenedRowHeaders.some(h => h.isGrandTotal)
            
            return showGrandTotal === hasGrandTotal
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 22.3.9**
     * 
     * 属性 79.3.2: 总计位置应符合配置
     */
    it('Property 79.3.2: Grand total position should match configuration', () => {
      fc.assert(
        fc.property(
          datasetArb,
          fc.constantFrom('top', 'bottom'),
          (data, position) => {
            if (data.length === 0) return true
            
            const config: PivotConfig = {
              rows: [{ id: 'category', field: 'category', label: '类别', dataType: 'string' }],
              columns: [],
              values: [{ id: 'amount_sum', field: 'amount', label: '金额', aggregation: 'sum' }],
            }
            
            const totalConfig: TotalConfig = {
              ...DEFAULT_TOTAL_CONFIG,
              showRowGrandTotal: true,
              rowGrandTotalPosition: position as 'top' | 'bottom',
            }
            
            const pivotData = calculatePivotData(data, config, totalConfig)
            const grandTotalIndex = pivotData.rowHeaders.findIndex(h => h.isGrandTotal)
            
            if (grandTotalIndex === -1) return false
            
            if (position === 'top') {
              return grandTotalIndex === 0
            } else {
              return grandTotalIndex === pivotData.rowHeaders.length - 1
            }
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 22.3.9**
     * 
     * 属性 79.3.3: 总计值应等于所有数据的聚合
     */
    it('Property 79.3.3: Grand total value should equal aggregation of all data', () => {
      fc.assert(
        fc.property(
          datasetArb.filter(d => d.length > 0),
          (data) => {
            const config: PivotConfig = {
              rows: [{ id: 'category', field: 'category', label: '类别', dataType: 'string' }],
              columns: [],
              values: [{ id: 'amount_sum', field: 'amount', label: '金额', aggregation: 'sum' }],
            }
            
            const totalConfig: TotalConfig = {
              ...DEFAULT_TOTAL_CONFIG,
              showRowGrandTotal: true,
            }
            
            const pivotData = calculatePivotData(data, config, totalConfig)
            
            // 找到总计行
            const grandTotalHeader = pivotData.flattenedRowHeaders.find(h => h.isGrandTotal)
            if (!grandTotalHeader) return false
            
            // 找到总计单元格 - 遍历所有单元格找到总计单元格
            let grandTotalCell = null
            for (const [key, cell] of pivotData.cells) {
              if (cell.isGrandTotal && cell.valueFieldId === 'amount_sum') {
                grandTotalCell = cell
                break
              }
            }
            
            if (!grandTotalCell) return false
            
            // 计算预期总计
            const expectedTotal = data.reduce((acc, row) => acc + row.amount, 0)
            
            return grandTotalCell.value === expectedTotal
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  // ==========================================================================
  // 属性 79.4: 数据筛选正确性
  // ==========================================================================
  
  describe('Property 79.4: Data Filtering Correctness', () => {
    /**
     * **Validates: Requirements 22.3.7**
     * 
     * 属性 79.4.1: 筛选后的数据应满足筛选条件
     */
    it('Property 79.4.1: Filtered data should satisfy filter conditions', () => {
      fc.assert(
        fc.property(
          datasetArb,
          fc.constantFrom('A', 'B', 'C'),
          (data, filterValue) => {
            const filters = [{
              id: 'category',
              field: 'category',
              label: '类别',
              filterType: 'select' as const,
              values: [filterValue],
            }]
            
            const filtered = applyFilters(data, filters)
            
            // 所有筛选后的数据应满足条件
            return filtered.every(row => row.category === filterValue)
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 22.3.7**
     * 
     * 属性 79.4.2: 空筛选应返回所有数据
     */
    it('Property 79.4.2: Empty filter should return all data', () => {
      fc.assert(
        fc.property(
          datasetArb,
          (data) => {
            const filtered = applyFilters(data, [])
            return filtered.length === data.length
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 22.3.7**
     * 
     * 属性 79.4.3: 多选筛选应返回匹配任一值的数据
     */
    it('Property 79.4.3: Multi-select filter should return data matching any value', () => {
      fc.assert(
        fc.property(
          datasetArb,
          fc.subarray(['A', 'B', 'C'], { minLength: 1 }),
          (data, filterValues) => {
            const filters = [{
              id: 'category',
              field: 'category',
              label: '类别',
              filterType: 'multiSelect' as const,
              values: filterValues,
            }]
            
            const filtered = applyFilters(data, filters)
            
            // 所有筛选后的数据应匹配任一筛选值
            return filtered.every(row => filterValues.includes(row.category))
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  // ==========================================================================
  // 属性 79.5: 透视数据完整性
  // ==========================================================================
  
  describe('Property 79.5: Pivot Data Integrity', () => {
    /**
     * **Validates: Requirements 22.3.7, 22.3.8**
     * 
     * 属性 79.5.1: 透视数据应包含所有维度组合
     */
    it('Property 79.5.1: Pivot data should contain all dimension combinations', () => {
      fc.assert(
        fc.property(
          datasetArb.filter(d => d.length > 0),
          (data) => {
            const config: PivotConfig = {
              rows: [{ id: 'category', field: 'category', label: '类别', dataType: 'string' }],
              columns: [],
              values: [{ id: 'amount_sum', field: 'amount', label: '金额', aggregation: 'sum' }],
            }
            
            const totalConfig: TotalConfig = {
              ...DEFAULT_TOTAL_CONFIG,
              showRowGrandTotal: false,
              showRowSubtotals: false,
            }
            
            const pivotData = calculatePivotData(data, config, totalConfig)
            
            // 获取数据中的唯一类别
            const uniqueCategories = getUniqueValues(data, 'category')
            
            // 透视数据的行数应等于唯一类别数
            const dataRows = pivotData.flattenedRowHeaders.filter(
              h => !h.isGrandTotal && !h.isSubtotal
            )
            
            return dataRows.length === uniqueCategories.length
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 22.3.8**
     * 
     * 属性 79.5.2: 单元格记录数应等于匹配的原始数据数量
     */
    it('Property 79.5.2: Cell record count should equal matching original data count', () => {
      fc.assert(
        fc.property(
          datasetArb.filter(d => d.length > 0),
          (data) => {
            const config: PivotConfig = {
              rows: [{ id: 'category', field: 'category', label: '类别', dataType: 'string' }],
              columns: [],
              values: [{ id: 'amount_sum', field: 'amount', label: '金额', aggregation: 'sum' }],
            }
            
            const totalConfig: TotalConfig = {
              ...DEFAULT_TOTAL_CONFIG,
              showRowGrandTotal: false,
              showRowSubtotals: false,
            }
            
            const pivotData = calculatePivotData(data, config, totalConfig)
            
            // 验证每个单元格的记录数
            for (const [key, cell] of pivotData.cells) {
              if (!cell.isGrandTotal && !cell.isSubtotal) {
                const category = cell.rowPath[0]
                const expectedCount = data.filter(row => row.category === category).length
                if (cell.recordCount !== expectedCount) {
                  return false
                }
              }
            }
            
            return true
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 22.3.8**
     * 
     * 属性 79.5.3: 所有单元格值的总和应等于总计值
     */
    it('Property 79.5.3: Sum of all cell values should equal grand total', () => {
      fc.assert(
        fc.property(
          datasetArb.filter(d => d.length > 0),
          (data) => {
            const config: PivotConfig = {
              rows: [{ id: 'category', field: 'category', label: '类别', dataType: 'string' }],
              columns: [],
              values: [{ id: 'amount_sum', field: 'amount', label: '金额', aggregation: 'sum' }],
            }
            
            const totalConfig: TotalConfig = {
              ...DEFAULT_TOTAL_CONFIG,
              showRowGrandTotal: true,
              showRowSubtotals: false,
            }
            
            const pivotData = calculatePivotData(data, config, totalConfig)
            
            // 计算所有非总计单元格的值总和
            let cellSum = 0
            let grandTotalValue = 0
            
            for (const [key, cell] of pivotData.cells) {
              if (cell.isGrandTotal) {
                grandTotalValue = cell.value
              } else if (!cell.isSubtotal) {
                cellSum += cell.value
              }
            }
            
            return Math.abs(cellSum - grandTotalValue) < 0.0001
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  // ==========================================================================
  // 属性 79.6: 数值格式化正确性
  // ==========================================================================
  
  describe('Property 79.6: Number Formatting Correctness', () => {
    /**
     * **Validates: Requirements 22.3.8**
     * 
     * 属性 79.6.1: 千分位格式化应正确插入分隔符
     */
    it('Property 79.6.1: Thousands separator should be correctly inserted', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 1000, max: 999999999 }),
          (value) => {
            const formatted = formatNumber(value, {
              useThousandsSeparator: true,
              decimalPlaces: 0,
            })
            
            // 移除逗号后应等于原始值
            const withoutCommas = formatted.replace(/,/g, '')
            return parseInt(withoutCommas, 10) === value
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 22.3.8**
     * 
     * 属性 79.6.2: 小数位数应符合配置
     */
    it('Property 79.6.2: Decimal places should match configuration', () => {
      fc.assert(
        fc.property(
          fc.float({ min: Math.fround(0.001), max: Math.fround(1000), noNaN: true }),
          fc.integer({ min: 0, max: 6 }),
          (value, decimalPlaces) => {
            const formatted = formatNumber(value, {
              useThousandsSeparator: false,
              decimalPlaces,
            })
            
            const parts = formatted.split('.')
            if (decimalPlaces === 0) {
              return parts.length === 1 || parts[1] === undefined
            }
            return parts.length === 2 && parts[1].length === decimalPlaces
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 22.3.8**
     * 
     * 属性 79.6.3: 前缀和后缀应正确添加
     */
    it('Property 79.6.3: Prefix and suffix should be correctly added', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 0, max: 10000 }),
          fc.constantFrom('$', '¥', '€', ''),
          fc.constantFrom('%', '元', '', ' units'),
          (value, prefix, suffix) => {
            const formatted = formatNumber(value, {
              prefix,
              suffix,
              decimalPlaces: 0,
              useThousandsSeparator: false,
            })
            
            return formatted.startsWith(prefix) && formatted.endsWith(suffix)
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 22.3.8**
     * 
     * 属性 79.6.4: 百分比格式化应乘以100
     */
    it('Property 79.6.4: Percentage formatting should multiply by 100', () => {
      fc.assert(
        fc.property(
          fc.float({ min: 0, max: 1, noNaN: true }),
          (value) => {
            const formatted = formatNumber(value, {
              asPercentage: true,
              decimalPlaces: 0,
              useThousandsSeparator: false,
            })
            
            // 移除%符号后应等于原始值*100
            const numericPart = formatted.replace('%', '')
            const parsedValue = parseInt(numericPart, 10)
            const expectedValue = Math.round(value * 100)
            
            return parsedValue === expectedValue
          }
        ),
        { numRuns: 100 }
      )
    })
  })
})
