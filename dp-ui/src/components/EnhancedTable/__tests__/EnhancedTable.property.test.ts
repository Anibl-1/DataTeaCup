/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 表格布局属性测试
 * Enhanced Table Layout Property Tests
 * 
 * **属性 71: 表格冻结窗格正确性**
 * **属性 72: 斑马纹交替正确性**
 * **属性 73: 单元格合并数据完整性**
 * **Validates: Requirements 14.5**
 * 
 * 测试内容:
 * 1. 冻结窗格 - 冻结列识别、偏移量计算
 * 2. 斑马纹 - 奇偶行类名、间隔配置
 * 3. 单元格合并 - 自动合并、手动合并、隐藏单元格
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { ref } from 'vue'
import { useTableLayout } from '@/composables/useTableLayout'
import type {
  ColumnConfig,
  MergeConfig,
  FreezeConfig,
  ZebraStripeConfig,
  ManualMergeCell,
} from '../types'

// ============================================================================
// Arbitraries (测试数据生成器)
// ============================================================================

/** 生成有效的列键 */
const columnKeyArb = fc.stringMatching(/^[a-z][a-zA-Z0-9_]{0,19}$/)

/** 生成列宽度 */
const columnWidthArb = fc.integer({ min: 50, max: 500 })

/** 生成列配置 */
const columnConfigArb: fc.Arbitrary<ColumnConfig> = fc.record({
  key: columnKeyArb,
  title: fc.string({ minLength: 1, maxLength: 20 }),
  dataIndex: columnKeyArb,
  width: fc.option(columnWidthArb, { nil: undefined }),
  minWidth: fc.option(fc.integer({ min: 30, max: 100 }), { nil: undefined }),
  maxWidth: fc.option(fc.integer({ min: 200, max: 600 }), { nil: undefined }),
  fixed: fc.option(fc.constantFrom('left', 'right', false), { nil: undefined }),
})


/** 生成唯一列配置数组 */
const uniqueColumnsArb = (minLength: number = 2, maxLength: number = 10): fc.Arbitrary<ColumnConfig[]> =>
  fc.array(columnConfigArb, { minLength, maxLength })
    .map(cols => {
      // 确保key唯一
      const seen = new Set<string>()
      return cols.filter(col => {
        if (seen.has(col.key)) return false
        seen.add(col.key)
        return true
      })
    })
    .filter(cols => cols.length >= minLength)

/** 生成行数据 */
const rowDataArb = (columns: ColumnConfig[]): fc.Arbitrary<Record<string, any>> =>
  fc.record(
    Object.fromEntries(
      columns.map(col => [col.dataIndex, fc.oneof(
        fc.string({ minLength: 1, maxLength: 20 }),
        fc.integer({ min: 0, max: 1000 }),
        fc.constant(null)
      )])
    )
  )

/** 生成表格数据 */
const tableDataArb = (columns: ColumnConfig[], minRows: number = 1, maxRows: number = 20): fc.Arbitrary<Record<string, any>[]> =>
  fc.array(rowDataArb(columns), { minLength: minRows, maxLength: maxRows })

/** 生成冻结配置 */
const freezeConfigArb: fc.Arbitrary<FreezeConfig> = fc.record({
  freezeHeader: fc.boolean(),
  freezeHeaderRows: fc.integer({ min: 0, max: 3 }),
  freezeLeftColumns: fc.integer({ min: 0, max: 5 }),
  freezeRightColumns: fc.integer({ min: 0, max: 5 }),
})

/** 生成十六进制颜色 */
const hexColorArb = fc.tuple(
  fc.integer({ min: 0, max: 255 }),
  fc.integer({ min: 0, max: 255 }),
  fc.integer({ min: 0, max: 255 })
).map(([r, g, b]) => `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`)

/** 生成斑马纹配置 */
const zebraConfigArb: fc.Arbitrary<ZebraStripeConfig> = fc.record({
  enabled: fc.boolean(),
  evenRowColor: hexColorArb,
  oddRowColor: hexColorArb,
  applyToHeader: fc.boolean(),
  stripeInterval: fc.integer({ min: 1, max: 5 }),
})

/** 生成行索引 */
const rowIndexArb = (maxRows: number) => fc.integer({ min: 0, max: Math.max(0, maxRows - 1) })

/** 生成手动合并单元格配置 */
const manualMergeCellArb = (maxRows: number, columnKeys: string[]): fc.Arbitrary<ManualMergeCell> =>
  fc.record({
    rowIndex: fc.integer({ min: 0, max: Math.max(0, maxRows - 1) }),
    columnKey: fc.constantFrom(...columnKeys),
    rowspan: fc.integer({ min: 1, max: Math.min(3, maxRows) }),
    colspan: fc.integer({ min: 1, max: Math.min(3, columnKeys.length) }),
  })


// ============================================================================
// Helper Functions
// ============================================================================

/**
 * 创建测试用的useTableLayout实例
 */
function createTableLayout(
  data: Record<string, any>[],
  columns: ColumnConfig[],
  options?: {
    mergeConfig?: MergeConfig
    freezeConfig?: FreezeConfig
    zebraStripeConfig?: ZebraStripeConfig | boolean
  }
) {
  return useTableLayout({
    data: ref(data),
    columns: ref(columns),
    mergeConfig: options?.mergeConfig ? ref(options.mergeConfig) : undefined,
    freezeConfig: options?.freezeConfig ? ref(options.freezeConfig) : undefined,
    zebraStripeConfig: options?.zebraStripeConfig !== undefined 
      ? ref(options.zebraStripeConfig) 
      : undefined,
  })
}

// ============================================================================
// 属性测试
// ============================================================================

describe('EnhancedTable Property Tests', () => {

  // ==========================================================================
  // 属性 71: 表格冻结窗格正确性
  // ==========================================================================
  
  describe('Property 71: Frozen Pane Correctness', () => {
    
    // ------------------------------------------------------------------------
    // 71.1 冻结列识别正确性
    // ------------------------------------------------------------------------
    
    describe('71.1 Frozen Column Identification', () => {
      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 71.1.1: 左冻结列数量应与配置一致
       * 当配置freezeLeftColumns=N时，frozenLeftColumns应包含N列（不含fixed属性的列）
       */
      it('Property 71.1.1: Left frozen columns count should match configuration', () => {
        fc.assert(
          fc.property(
            uniqueColumnsArb(3, 10).map(cols => 
              // 移除所有fixed属性以确保只测试freezeLeftColumns配置
              cols.map(c => ({ ...c, fixed: undefined }))
            ),
            fc.integer({ min: 0, max: 3 }),
            (columns, freezeCount) => {
              const actualFreezeCount = Math.min(freezeCount, columns.length)
              const { frozenLeftColumns } = createTableLayout([], columns, {
                freezeConfig: { freezeLeftColumns: actualFreezeCount }
              })
              
              return frozenLeftColumns.value.length === actualFreezeCount
            }
          ),
          { numRuns: 100 }
        )
      })


      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 71.1.2: 右冻结列数量应与配置一致
       * 当配置freezeRightColumns=N时，frozenRightColumns应包含N列（不含fixed属性的列）
       */
      it('Property 71.1.2: Right frozen columns count should match configuration', () => {
        fc.assert(
          fc.property(
            uniqueColumnsArb(3, 10).map(cols => 
              // 移除所有fixed属性以确保只测试freezeRightColumns配置
              cols.map(c => ({ ...c, fixed: undefined }))
            ),
            fc.integer({ min: 0, max: 3 }),
            (columns, freezeCount) => {
              const actualFreezeCount = Math.min(freezeCount, columns.length)
              const { frozenRightColumns } = createTableLayout([], columns, {
                freezeConfig: { freezeRightColumns: actualFreezeCount }
              })
              
              return frozenRightColumns.value.length === actualFreezeCount
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 71.1.3: 通过column.fixed='left'设置的列应被识别为左冻结
       * 列配置中fixed='left'的列应出现在frozenLeftColumns中
       */
      it('Property 71.1.3: Columns with fixed=left should be identified as left frozen', () => {
        fc.assert(
          fc.property(
            uniqueColumnsArb(3, 8),
            fc.integer({ min: 0, max: 2 }),
            (columns, fixedCount) => {
              // 将前fixedCount列设置为fixed='left'
              const modifiedColumns = columns.map((col, idx) => ({
                ...col,
                fixed: idx < fixedCount ? 'left' as const : undefined
              }))
              
              const { frozenLeftColumns } = createTableLayout([], modifiedColumns)
              
              // 验证所有fixed='left'的列都在frozenLeftColumns中
              const fixedLeftKeys = modifiedColumns
                .filter(c => c.fixed === 'left')
                .map(c => c.key)
              
              return fixedLeftKeys.every(key => 
                frozenLeftColumns.value.some(c => c.key === key)
              )
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 71.1.4: 通过column.fixed='right'设置的列应被识别为右冻结
       * 列配置中fixed='right'的列应出现在frozenRightColumns中
       */
      it('Property 71.1.4: Columns with fixed=right should be identified as right frozen', () => {
        fc.assert(
          fc.property(
            uniqueColumnsArb(3, 8),
            fc.integer({ min: 0, max: 2 }),
            (columns, fixedCount) => {
              // 将后fixedCount列设置为fixed='right'
              const modifiedColumns = columns.map((col, idx) => ({
                ...col,
                fixed: idx >= columns.length - fixedCount ? 'right' as const : undefined
              }))
              
              const { frozenRightColumns } = createTableLayout([], modifiedColumns)
              
              // 验证所有fixed='right'的列都在frozenRightColumns中
              const fixedRightKeys = modifiedColumns
                .filter(c => c.fixed === 'right')
                .map(c => c.key)
              
              return fixedRightKeys.every(key => 
                frozenRightColumns.value.some(c => c.key === key)
              )
            }
          ),
          { numRuns: 100 }
        )
      })


      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 71.1.5: 冻结列和可滚动列应覆盖所有列
       * frozenLeftColumns + scrollableColumns + frozenRightColumns = 所有列
       */
      it('Property 71.1.5: Frozen and scrollable columns should cover all columns', () => {
        fc.assert(
          fc.property(
            uniqueColumnsArb(3, 10),
            fc.integer({ min: 0, max: 2 }),
            fc.integer({ min: 0, max: 2 }),
            (columns, leftFreeze, rightFreeze) => {
              const { frozenLeftColumns, frozenRightColumns, scrollableColumns } = createTableLayout(
                [], 
                columns, 
                {
                  freezeConfig: { 
                    freezeLeftColumns: Math.min(leftFreeze, columns.length),
                    freezeRightColumns: Math.min(rightFreeze, columns.length - leftFreeze)
                  }
                }
              )
              
              const totalColumns = 
                frozenLeftColumns.value.length + 
                scrollableColumns.value.length + 
                frozenRightColumns.value.length
              
              return totalColumns === columns.length
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ------------------------------------------------------------------------
    // 71.2 冻结列偏移量计算
    // ------------------------------------------------------------------------
    
    describe('71.2 Frozen Column Offset Calculation', () => {
      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 71.2.1: 左冻结列的偏移量应累加
       * 第一个左冻结列offset=0，后续列offset=前面列宽度之和
       */
      it('Property 71.2.1: Left frozen column offsets should accumulate', () => {
        fc.assert(
          fc.property(
            uniqueColumnsArb(4, 8).map(cols => 
              cols.map(c => ({ ...c, width: c.width ?? 100 }))
            ),
            fc.integer({ min: 2, max: 3 }),
            (columns, freezeCount) => {
              const actualFreezeCount = Math.min(freezeCount, columns.length)
              const { frozenLeftColumns } = createTableLayout([], columns, {
                freezeConfig: { freezeLeftColumns: actualFreezeCount }
              })
              
              // 验证偏移量累加
              let expectedOffset = 0
              for (const col of frozenLeftColumns.value) {
                if (col.frozenOffset !== expectedOffset) {
                  return false
                }
                expectedOffset += col.computedWidth
              }
              return true
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 71.2.2: 右冻结列的偏移量应从右向左累加
       * 最右边的冻结列offset=0，向左累加
       */
      it('Property 71.2.2: Right frozen column offsets should accumulate from right', () => {
        fc.assert(
          fc.property(
            uniqueColumnsArb(4, 8).map(cols => 
              cols.map(c => ({ ...c, width: c.width ?? 100 }))
            ),
            fc.integer({ min: 2, max: 3 }),
            (columns, freezeCount) => {
              const actualFreezeCount = Math.min(freezeCount, columns.length)
              const { frozenRightColumns } = createTableLayout([], columns, {
                freezeConfig: { freezeRightColumns: actualFreezeCount }
              })
              
              // 验证偏移量从右向左累加
              const rightCols = frozenRightColumns.value
              let expectedOffset = 0
              for (let i = rightCols.length - 1; i >= 0; i--) {
                if (rightCols[i].frozenOffset !== expectedOffset) {
                  return false
                }
                expectedOffset += rightCols[i].computedWidth
              }
              return true
            }
          ),
          { numRuns: 100 }
        )
      })


      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 71.2.3: leftFrozenWidth应等于所有左冻结列宽度之和
       */
      it('Property 71.2.3: leftFrozenWidth should equal sum of left frozen column widths', () => {
        fc.assert(
          fc.property(
            uniqueColumnsArb(4, 8).map(cols => 
              cols.map(c => ({ ...c, width: c.width ?? 100 }))
            ),
            fc.integer({ min: 1, max: 3 }),
            (columns, freezeCount) => {
              const actualFreezeCount = Math.min(freezeCount, columns.length)
              const { frozenLeftColumns, leftFrozenWidth } = createTableLayout([], columns, {
                freezeConfig: { freezeLeftColumns: actualFreezeCount }
              })
              
              const expectedWidth = frozenLeftColumns.value.reduce(
                (sum, col) => sum + col.computedWidth, 
                0
              )
              
              return leftFrozenWidth.value === expectedWidth
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 71.2.4: rightFrozenWidth应等于所有右冻结列宽度之和
       */
      it('Property 71.2.4: rightFrozenWidth should equal sum of right frozen column widths', () => {
        fc.assert(
          fc.property(
            uniqueColumnsArb(4, 8).map(cols => 
              cols.map(c => ({ ...c, width: c.width ?? 100 }))
            ),
            fc.integer({ min: 1, max: 3 }),
            (columns, freezeCount) => {
              const actualFreezeCount = Math.min(freezeCount, columns.length)
              const { frozenRightColumns, rightFrozenWidth } = createTableLayout([], columns, {
                freezeConfig: { freezeRightColumns: actualFreezeCount }
              })
              
              const expectedWidth = frozenRightColumns.value.reduce(
                (sum, col) => sum + col.computedWidth, 
                0
              )
              
              return rightFrozenWidth.value === expectedWidth
            }
          ),
          { numRuns: 100 }
        )
      })
    })
  })


  // ==========================================================================
  // 属性 72: 斑马纹交替正确性
  // ==========================================================================
  
  describe('Property 72: Zebra Stripe Alternation Correctness', () => {
    
    // ------------------------------------------------------------------------
    // 72.1 奇偶行类名
    // ------------------------------------------------------------------------
    
    describe('72.1 Even/Odd Row Class Names', () => {
      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 72.1.1: 偶数行应获得even类名
       * 当stripeInterval=1时，行索引0,2,4...应返回even类
       */
      it('Property 72.1.1: Even rows should get even class', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 0, max: 100 }),
            (rowIndex) => {
              const columns: ColumnConfig[] = [
                { key: 'col1', title: 'Col 1', dataIndex: 'col1' }
              ]
              const { getZebraClass } = createTableLayout([], columns, {
                zebraStripeConfig: { enabled: true, stripeInterval: 1 }
              })
              
              const zebraClass = getZebraClass(rowIndex)
              const isEvenRow = rowIndex % 2 === 0
              
              if (isEvenRow) {
                return zebraClass === 'enhanced-table-row--even'
              } else {
                return zebraClass === 'enhanced-table-row--odd'
              }
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 72.1.2: 奇数行应获得odd类名
       * 当stripeInterval=1时，行索引1,3,5...应返回odd类
       */
      it('Property 72.1.2: Odd rows should get odd class', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 0, max: 100 }),
            (rowIndex) => {
              const columns: ColumnConfig[] = [
                { key: 'col1', title: 'Col 1', dataIndex: 'col1' }
              ]
              const { getZebraClass } = createTableLayout([], columns, {
                zebraStripeConfig: { enabled: true, stripeInterval: 1 }
              })
              
              const zebraClass = getZebraClass(rowIndex)
              const isOddRow = rowIndex % 2 === 1
              
              if (isOddRow) {
                return zebraClass === 'enhanced-table-row--odd'
              } else {
                return zebraClass === 'enhanced-table-row--even'
              }
            }
          ),
          { numRuns: 100 }
        )
      })


      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 72.1.3: 相邻行应有不同的斑马纹类名
       * 对于任意相邻的两行，它们的斑马纹类名应不同
       */
      it('Property 72.1.3: Adjacent rows should have different zebra classes', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 0, max: 99 }),
            (rowIndex) => {
              const columns: ColumnConfig[] = [
                { key: 'col1', title: 'Col 1', dataIndex: 'col1' }
              ]
              const { getZebraClass } = createTableLayout([], columns, {
                zebraStripeConfig: { enabled: true, stripeInterval: 1 }
              })
              
              const currentClass = getZebraClass(rowIndex)
              const nextClass = getZebraClass(rowIndex + 1)
              
              return currentClass !== nextClass
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ------------------------------------------------------------------------
    // 72.2 条纹间隔配置
    // ------------------------------------------------------------------------
    
    describe('72.2 Stripe Interval Configuration', () => {
      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 72.2.1: stripeInterval应正确控制条纹分组
       * 当stripeInterval=N时，每N行为一组，组内类名相同
       */
      it('Property 72.2.1: Stripe interval should control stripe grouping', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 1, max: 5 }),
            fc.integer({ min: 0, max: 50 }),
            (interval, rowIndex) => {
              const columns: ColumnConfig[] = [
                { key: 'col1', title: 'Col 1', dataIndex: 'col1' }
              ]
              const { getZebraClass } = createTableLayout([], columns, {
                zebraStripeConfig: { enabled: true, stripeInterval: interval }
              })
              
              const zebraClass = getZebraClass(rowIndex)
              const groupIndex = Math.floor(rowIndex / interval)
              const isEvenGroup = groupIndex % 2 === 0
              
              if (isEvenGroup) {
                return zebraClass === 'enhanced-table-row--even'
              } else {
                return zebraClass === 'enhanced-table-row--odd'
              }
            }
          ),
          { numRuns: 200 }
        )
      })

      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 72.2.2: 同一条纹组内的行应有相同类名
       * 当stripeInterval=N时，行索引i和i+1（在同一组内）应有相同类名
       */
      it('Property 72.2.2: Rows in same stripe group should have same class', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 2, max: 5 }),
            fc.integer({ min: 0, max: 20 }),
            (interval, groupIndex) => {
              const columns: ColumnConfig[] = [
                { key: 'col1', title: 'Col 1', dataIndex: 'col1' }
              ]
              const { getZebraClass } = createTableLayout([], columns, {
                zebraStripeConfig: { enabled: true, stripeInterval: interval }
              })
              
              // 获取同一组内的所有行
              const startRow = groupIndex * interval
              const classes: string[] = []
              for (let i = 0; i < interval; i++) {
                classes.push(getZebraClass(startRow + i))
              }
              
              // 同一组内所有行应有相同类名
              return classes.every(c => c === classes[0])
            }
          ),
          { numRuns: 100 }
        )
      })


      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 72.2.3: 相邻条纹组应有不同类名
       * 当stripeInterval=N时，组i和组i+1应有不同类名
       */
      it('Property 72.2.3: Adjacent stripe groups should have different classes', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 1, max: 5 }),
            fc.integer({ min: 0, max: 20 }),
            (interval, groupIndex) => {
              const columns: ColumnConfig[] = [
                { key: 'col1', title: 'Col 1', dataIndex: 'col1' }
              ]
              const { getZebraClass } = createTableLayout([], columns, {
                zebraStripeConfig: { enabled: true, stripeInterval: interval }
              })
              
              const currentGroupFirstRow = groupIndex * interval
              const nextGroupFirstRow = (groupIndex + 1) * interval
              
              const currentClass = getZebraClass(currentGroupFirstRow)
              const nextClass = getZebraClass(nextGroupFirstRow)
              
              return currentClass !== nextClass
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ------------------------------------------------------------------------
    // 72.3 禁用斑马纹
    // ------------------------------------------------------------------------
    
    describe('72.3 Disabled Zebra Stripes', () => {
      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 72.3.1: 禁用斑马纹时应返回空字符串
       * 当enabled=false时，getZebraClass应返回空字符串
       */
      it('Property 72.3.1: Disabled zebra should return empty class', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 0, max: 100 }),
            (rowIndex) => {
              const columns: ColumnConfig[] = [
                { key: 'col1', title: 'Col 1', dataIndex: 'col1' }
              ]
              const { getZebraClass } = createTableLayout([], columns, {
                zebraStripeConfig: { enabled: false }
              })
              
              return getZebraClass(rowIndex) === ''
            }
          ),
          { numRuns: 50 }
        )
      })

      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 72.3.2: 使用布尔值false配置时应禁用斑马纹
       */
      it('Property 72.3.2: Boolean false config should disable zebra stripes', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 0, max: 100 }),
            (rowIndex) => {
              const columns: ColumnConfig[] = [
                { key: 'col1', title: 'Col 1', dataIndex: 'col1' }
              ]
              const { getZebraClass } = createTableLayout([], columns, {
                zebraStripeConfig: false
              })
              
              return getZebraClass(rowIndex) === ''
            }
          ),
          { numRuns: 50 }
        )
      })

      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 72.3.3: 使用布尔值true配置时应启用斑马纹
       */
      it('Property 72.3.3: Boolean true config should enable zebra stripes', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 0, max: 100 }),
            (rowIndex) => {
              const columns: ColumnConfig[] = [
                { key: 'col1', title: 'Col 1', dataIndex: 'col1' }
              ]
              const { getZebraClass } = createTableLayout([], columns, {
                zebraStripeConfig: true
              })
              
              const zebraClass = getZebraClass(rowIndex)
              return zebraClass === 'enhanced-table-row--even' || 
                     zebraClass === 'enhanced-table-row--odd'
            }
          ),
          { numRuns: 50 }
        )
      })
    })
  })


  // ==========================================================================
  // 属性 73: 单元格合并数据完整性
  // ==========================================================================
  
  describe('Property 73: Cell Merge Data Integrity', () => {
    
    // ------------------------------------------------------------------------
    // 73.1 自动行合并
    // ------------------------------------------------------------------------
    
    describe('73.1 Auto Row Merge for Same Values', () => {
      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 73.1.1: 相同值的连续行应被合并
       * 当autoRowMerge启用时，相同值的连续行应合并为一个单元格
       */
      it('Property 73.1.1: Consecutive rows with same value should be merged', () => {
        const columns: ColumnConfig[] = [
          { key: 'category', title: 'Category', dataIndex: 'category' },
          { key: 'value', title: 'Value', dataIndex: 'value' }
        ]
        
        // 创建有重复值的数据
        const data = [
          { category: 'A', value: 1 },
          { category: 'A', value: 2 },
          { category: 'A', value: 3 },
          { category: 'B', value: 4 },
          { category: 'B', value: 5 },
        ]
        
        const { getCellMergeSpan, isCellHidden } = createTableLayout(data, columns, {
          mergeConfig: {
            autoRowMerge: true,
            autoRowMergeColumns: ['category']
          }
        })
        
        // 第一个A应该有rowspan=3
        const firstASpan = getCellMergeSpan(0, 'category')
        expect(firstASpan.rowspan).toBe(3)
        expect(firstASpan.colspan).toBe(1)
        
        // 第二、三个A应该被隐藏
        expect(isCellHidden(1, 'category')).toBe(true)
        expect(isCellHidden(2, 'category')).toBe(true)
        
        // 第一个B应该有rowspan=2
        const firstBSpan = getCellMergeSpan(3, 'category')
        expect(firstBSpan.rowspan).toBe(2)
        
        // 第二个B应该被隐藏
        expect(isCellHidden(4, 'category')).toBe(true)
      })

      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 73.1.2: 不同值的行不应被合并
       * 当值不同时，每个单元格应保持独立
       */
      it('Property 73.1.2: Rows with different values should not be merged', () => {
        fc.assert(
          fc.property(
            fc.array(fc.string({ minLength: 1, maxLength: 5 }), { minLength: 3, maxLength: 10 })
              .filter(arr => new Set(arr).size === arr.length), // 确保所有值不同
            (uniqueValues) => {
              const columns: ColumnConfig[] = [
                { key: 'category', title: 'Category', dataIndex: 'category' }
              ]
              
              const data = uniqueValues.map(v => ({ category: v }))
              
              const { getCellMergeSpan, isCellHidden } = createTableLayout(data, columns, {
                mergeConfig: {
                  autoRowMerge: true,
                  autoRowMergeColumns: ['category']
                }
              })
              
              // 所有单元格都应该是独立的（rowspan=1）且不被隐藏
              for (let i = 0; i < data.length; i++) {
                const span = getCellMergeSpan(i, 'category')
                if (span.rowspan !== 1 || isCellHidden(i, 'category')) {
                  return false
                }
              }
              return true
            }
          ),
          { numRuns: 50 }
        )
      })


      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 73.1.3: 未配置自动合并的列不应被合并
       * 只有在autoRowMergeColumns中的列才会被自动合并
       */
      it('Property 73.1.3: Columns not in autoRowMergeColumns should not be merged', () => {
        const columns: ColumnConfig[] = [
          { key: 'category', title: 'Category', dataIndex: 'category' },
          { key: 'type', title: 'Type', dataIndex: 'type' }
        ]
        
        const data = [
          { category: 'A', type: 'X' },
          { category: 'A', type: 'X' },
          { category: 'A', type: 'X' },
        ]
        
        const { getCellMergeSpan, isCellHidden } = createTableLayout(data, columns, {
          mergeConfig: {
            autoRowMerge: true,
            autoRowMergeColumns: ['category'] // 只合并category列
          }
        })
        
        // category列应该被合并
        expect(getCellMergeSpan(0, 'category').rowspan).toBe(3)
        
        // type列不应该被合并（即使值相同）
        for (let i = 0; i < data.length; i++) {
          const span = getCellMergeSpan(i, 'type')
          expect(span.rowspan).toBe(1)
          expect(isCellHidden(i, 'type')).toBe(false)
        }
      })
    })

    // ------------------------------------------------------------------------
    // 73.2 手动合并单元格
    // ------------------------------------------------------------------------
    
    describe('73.2 Manual Merge Cells', () => {
      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 73.2.1: 手动合并配置应正确应用
       * manualMerges中的配置应正确设置rowspan和colspan
       */
      it('Property 73.2.1: Manual merge configuration should be applied correctly', () => {
        const columns: ColumnConfig[] = [
          { key: 'col1', title: 'Col 1', dataIndex: 'col1' },
          { key: 'col2', title: 'Col 2', dataIndex: 'col2' },
          { key: 'col3', title: 'Col 3', dataIndex: 'col3' }
        ]
        
        const data = [
          { col1: 'A', col2: 'B', col3: 'C' },
          { col1: 'D', col2: 'E', col3: 'F' },
          { col1: 'G', col2: 'H', col3: 'I' },
        ]
        
        const { getCellMergeSpan } = createTableLayout(data, columns, {
          mergeConfig: {
            manualMerges: [
              { rowIndex: 0, columnKey: 'col1', rowspan: 2, colspan: 2 }
            ]
          }
        })
        
        const span = getCellMergeSpan(0, 'col1')
        expect(span.rowspan).toBe(2)
        expect(span.colspan).toBe(2)
      })

      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 73.2.2: 被合并的单元格应被标记为隐藏
       * 手动合并区域内的非主单元格应有rowspan=0, colspan=0
       */
      it('Property 73.2.2: Merged cells should be marked as hidden', () => {
        const columns: ColumnConfig[] = [
          { key: 'col1', title: 'Col 1', dataIndex: 'col1' },
          { key: 'col2', title: 'Col 2', dataIndex: 'col2' },
          { key: 'col3', title: 'Col 3', dataIndex: 'col3' }
        ]
        
        const data = [
          { col1: 'A', col2: 'B', col3: 'C' },
          { col1: 'D', col2: 'E', col3: 'F' },
          { col1: 'G', col2: 'H', col3: 'I' },
        ]
        
        const { isCellHidden, getCellMergeSpan } = createTableLayout(data, columns, {
          mergeConfig: {
            manualMerges: [
              { rowIndex: 0, columnKey: 'col1', rowspan: 2, colspan: 2 }
            ]
          }
        })
        
        // 主单元格不应被隐藏
        expect(isCellHidden(0, 'col1')).toBe(false)
        
        // 被合并的单元格应被隐藏
        expect(isCellHidden(0, 'col2')).toBe(true) // 同行右侧
        expect(isCellHidden(1, 'col1')).toBe(true) // 下一行同列
        expect(isCellHidden(1, 'col2')).toBe(true) // 下一行右侧
        
        // 验证隐藏单元格的span值
        const hiddenSpan = getCellMergeSpan(0, 'col2')
        expect(hiddenSpan.rowspan).toBe(0)
        expect(hiddenSpan.colspan).toBe(0)
      })


      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 73.2.3: 多个手动合并配置应同时生效
       */
      it('Property 73.2.3: Multiple manual merge configurations should work together', () => {
        const columns: ColumnConfig[] = [
          { key: 'col1', title: 'Col 1', dataIndex: 'col1' },
          { key: 'col2', title: 'Col 2', dataIndex: 'col2' },
          { key: 'col3', title: 'Col 3', dataIndex: 'col3' }
        ]
        
        const data = [
          { col1: 'A', col2: 'B', col3: 'C' },
          { col1: 'D', col2: 'E', col3: 'F' },
          { col1: 'G', col2: 'H', col3: 'I' },
          { col1: 'J', col2: 'K', col3: 'L' },
        ]
        
        const { getCellMergeSpan, isCellHidden } = createTableLayout(data, columns, {
          mergeConfig: {
            manualMerges: [
              { rowIndex: 0, columnKey: 'col1', rowspan: 2, colspan: 1 },
              { rowIndex: 2, columnKey: 'col2', rowspan: 2, colspan: 2 }
            ]
          }
        })
        
        // 第一个合并区域
        expect(getCellMergeSpan(0, 'col1').rowspan).toBe(2)
        expect(isCellHidden(1, 'col1')).toBe(true)
        
        // 第二个合并区域
        expect(getCellMergeSpan(2, 'col2').rowspan).toBe(2)
        expect(getCellMergeSpan(2, 'col2').colspan).toBe(2)
        expect(isCellHidden(2, 'col3')).toBe(true)
        expect(isCellHidden(3, 'col2')).toBe(true)
        expect(isCellHidden(3, 'col3')).toBe(true)
      })
    })

    // ------------------------------------------------------------------------
    // 73.3 单元格总数完整性
    // ------------------------------------------------------------------------
    
    describe('73.3 Cell Count Integrity', () => {
      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 73.3.1: 可见单元格+隐藏单元格=总单元格数
       * 对于任意合并配置，可见和隐藏单元格的总数应等于行数×列数
       */
      it('Property 73.3.1: Visible cells + hidden cells = total cells', () => {
        const columns: ColumnConfig[] = [
          { key: 'col1', title: 'Col 1', dataIndex: 'col1' },
          { key: 'col2', title: 'Col 2', dataIndex: 'col2' },
          { key: 'col3', title: 'Col 3', dataIndex: 'col3' }
        ]
        
        const data = [
          { col1: 'A', col2: 'B', col3: 'C' },
          { col1: 'A', col2: 'E', col3: 'F' },
          { col1: 'A', col2: 'H', col3: 'I' },
          { col1: 'B', col2: 'K', col3: 'L' },
        ]
        
        const { isCellHidden } = createTableLayout(data, columns, {
          mergeConfig: {
            autoRowMerge: true,
            autoRowMergeColumns: ['col1']
          }
        })
        
        let visibleCount = 0
        let hiddenCount = 0
        
        for (let row = 0; row < data.length; row++) {
          for (const col of columns) {
            if (isCellHidden(row, col.key)) {
              hiddenCount++
            } else {
              visibleCount++
            }
          }
        }
        
        const totalCells = data.length * columns.length
        expect(visibleCount + hiddenCount).toBe(totalCells)
      })


      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 73.3.2: 隐藏单元格应有rowspan=0和colspan=0
       * 所有被合并的单元格应有rowspan=0, colspan=0
       */
      it('Property 73.3.2: Hidden cells should have rowspan=0 and colspan=0', () => {
        const columns: ColumnConfig[] = [
          { key: 'col1', title: 'Col 1', dataIndex: 'col1' },
          { key: 'col2', title: 'Col 2', dataIndex: 'col2' }
        ]
        
        const data = [
          { col1: 'A', col2: 'B' },
          { col1: 'A', col2: 'E' },
          { col1: 'A', col2: 'H' },
        ]
        
        const { isCellHidden, getCellMergeSpan } = createTableLayout(data, columns, {
          mergeConfig: {
            autoRowMerge: true,
            autoRowMergeColumns: ['col1']
          }
        })
        
        for (let row = 0; row < data.length; row++) {
          for (const col of columns) {
            if (isCellHidden(row, col.key)) {
              const span = getCellMergeSpan(row, col.key)
              expect(span.rowspan).toBe(0)
              expect(span.colspan).toBe(0)
            }
          }
        }
      })

      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 73.3.3: 无合并配置时所有单元格应可见
       * 当没有合并配置时，所有单元格都应该是可见的
       */
      it('Property 73.3.3: All cells should be visible without merge config', () => {
        fc.assert(
          fc.property(
            uniqueColumnsArb(2, 5),
            fc.integer({ min: 2, max: 10 }),
            (columns, rowCount) => {
              const data = Array.from({ length: rowCount }, (_, i) => 
                Object.fromEntries(columns.map(c => [c.dataIndex, `value_${i}`]))
              )
              
              const { isCellHidden } = createTableLayout(data, columns)
              
              for (let row = 0; row < data.length; row++) {
                for (const col of columns) {
                  if (isCellHidden(row, col.key)) {
                    return false
                  }
                }
              }
              return true
            }
          ),
          { numRuns: 50 }
        )
      })

      /**
       * **Validates: Requirements 14.5**
       * 
       * 属性 73.3.4: 默认单元格span应为rowspan=1, colspan=1
       * 未合并的单元格应有默认的span值
       */
      it('Property 73.3.4: Default cell span should be rowspan=1, colspan=1', () => {
        fc.assert(
          fc.property(
            uniqueColumnsArb(2, 5),
            fc.integer({ min: 2, max: 10 }),
            (columns, rowCount) => {
              const data = Array.from({ length: rowCount }, (_, i) => 
                Object.fromEntries(columns.map(c => [c.dataIndex, `unique_${i}_${c.key}`]))
              )
              
              const { getCellMergeSpan } = createTableLayout(data, columns)
              
              for (let row = 0; row < data.length; row++) {
                for (const col of columns) {
                  const span = getCellMergeSpan(row, col.key)
                  if (span.rowspan !== 1 || span.colspan !== 1) {
                    return false
                  }
                }
              }
              return true
            }
          ),
          { numRuns: 50 }
        )
      })
    })
  })
})
