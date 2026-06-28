/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 表格布局增强 Composable
 * Enhanced Table Layout Composable
 * 
 * 提供行列合并、冻结窗格、列宽自适应、斑马纹等功能
 * Provides cell merge, freeze panes, auto column width, zebra stripe features
 * 
 * 需求: 14.5.21, 14.5.22, 14.5.23, 14.5.24, 14.5.25, 14.5.26
 */

import { ref, computed, watch, type Ref, type ComputedRef } from 'vue'
import type {
  ColumnConfig,
  MergeConfig,
  FreezeConfig,
  AutoSizeConfig,
  ZebraStripeConfig,
  CellMergeSpan,
  ComputedColumnInfo,
  TableScrollState,
} from '@/components/EnhancedTable/types'

// ============================================================================
// Types
// ============================================================================

export interface UseTableLayoutOptions {
  data: Ref<Record<string, any>[]>
  columns: Ref<ColumnConfig[]>
  mergeConfig?: Ref<MergeConfig | undefined>
  freezeConfig?: Ref<FreezeConfig | undefined>
  autoSizeConfig?: Ref<AutoSizeConfig | undefined>
  zebraStripeConfig?: Ref<ZebraStripeConfig | boolean | undefined>
  containerRef?: Ref<HTMLElement | null>
}

export interface UseTableLayoutReturn {
  // Computed columns with layout info
  computedColumns: ComputedRef<ComputedColumnInfo[]>
  // Cell merge map
  cellMergeMap: ComputedRef<Map<string, CellMergeSpan>>
  // Get cell merge span
  getCellMergeSpan: (rowIndex: number, columnKey: string) => CellMergeSpan
  // Check if cell is hidden (merged into another)
  isCellHidden: (rowIndex: number, columnKey: string) => boolean
  // Get zebra stripe class for row
  getZebraClass: (rowIndex: number) => string
  // Frozen columns info
  frozenLeftColumns: ComputedRef<ComputedColumnInfo[]>
  frozenRightColumns: ComputedRef<ComputedColumnInfo[]>
  scrollableColumns: ComputedRef<ComputedColumnInfo[]>
  // Frozen widths
  leftFrozenWidth: ComputedRef<number>
  rightFrozenWidth: ComputedRef<number>
  // Auto-calculated column widths
  columnWidths: Ref<Map<string, number>>
  // Calculate column widths based on content
  calculateColumnWidths: (tableElement?: HTMLElement) => void
  // Scroll state
  scrollState: Ref<TableScrollState>
  // Update scroll state
  updateScrollState: (scrollLeft: number, scrollTop: number) => void
  // Total table width
  totalWidth: ComputedRef<number>
}

// ============================================================================
// Default Configurations (inline to avoid circular imports)
// ============================================================================

const DEFAULT_MERGE: MergeConfig = {
  autoRowMerge: false,
  autoRowMergeColumns: [],
  rules: [],
  manualMerges: [],
}

const DEFAULT_FREEZE: FreezeConfig = {
  freezeHeader: true,
  freezeHeaderRows: 1,
  freezeLeftColumns: 0,
  freezeRightColumns: 0,
}

const DEFAULT_AUTO_SIZE: AutoSizeConfig = {
  autoColumnWidth: false,
  autoWidthColumns: [],
  includeHeader: true,
  padding: 16,
  maxAutoWidth: 300,
  minAutoWidth: 50,
  autoRowHeight: false,
  minRowHeight: 40,
  maxRowHeight: 200,
}

const DEFAULT_ZEBRA: ZebraStripeConfig = {
  enabled: true,
  evenRowColor: '#fafafa',
  oddRowColor: '#ffffff',
  applyToHeader: false,
  stripeInterval: 1,
}

// ============================================================================
// Cell Key Generator
// ============================================================================

const generateCellKey = (rowIndex: number, columnKey: string): string => 
  `${rowIndex}-${columnKey}`

// ============================================================================
// Main Composable
// ============================================================================

export function useTableLayout(options: UseTableLayoutOptions): UseTableLayoutReturn {
  const {
    data,
    columns,
    mergeConfig,
    freezeConfig,
    autoSizeConfig,
    zebraStripeConfig,
    containerRef,
  } = options

  // ============================================================================
  // Reactive State
  // ============================================================================

  const columnWidths = ref<Map<string, number>>(new Map())
  
  const scrollState = ref<TableScrollState>({
    scrollLeft: 0,
    scrollTop: 0,
    isScrolledLeft: true,
    isScrolledRight: false,
    isScrolledTop: true,
    isScrolledBottom: false,
  })

  // ============================================================================
  // Normalized Configs
  // ============================================================================

  const normalizedMergeConfig = computed<MergeConfig>(() => ({
    ...DEFAULT_MERGE,
    ...mergeConfig?.value,
  }))

  const normalizedFreezeConfig = computed<FreezeConfig>(() => ({
    ...DEFAULT_FREEZE,
    ...freezeConfig?.value,
  }))

  const normalizedAutoSizeConfig = computed<AutoSizeConfig>(() => ({
    ...DEFAULT_AUTO_SIZE,
    ...autoSizeConfig?.value,
  }))

  const normalizedZebraConfig = computed<ZebraStripeConfig>(() => {
    const config = zebraStripeConfig?.value
    if (typeof config === 'boolean') {
      return { ...DEFAULT_ZEBRA, enabled: config }
    }
    return { ...DEFAULT_ZEBRA, ...config }
  })

  // ============================================================================
  // Cell Merge Calculation
  // Validates: 14.5.21 - 行合并 - 支持相同值自动合并
  // Validates: 14.5.22 - 列合并 - 支持跨列合并
  // ============================================================================

  const cellMergeMap = computed<Map<string, CellMergeSpan>>(() => {
    const mergeMap = new Map<string, CellMergeSpan>()
    const config = normalizedMergeConfig.value
    const rows = data.value
    const cols = columns.value

    if (!rows.length || !cols.length) {
      return mergeMap
    }

    // Initialize all cells with default span
    for (let rowIdx = 0; rowIdx < rows.length; rowIdx++) {
      for (const col of cols) {
        mergeMap.set(generateCellKey(rowIdx, col.key), { rowspan: 1, colspan: 1 })
      }
    }

    // Apply manual merges first
    if (config.manualMerges?.length) {
      for (const merge of config.manualMerges) {
        const key = generateCellKey(merge.rowIndex, merge.columnKey)
        mergeMap.set(key, { rowspan: merge.rowspan, colspan: merge.colspan })

        // Mark merged cells as hidden (rowspan: 0, colspan: 0)
        const colIndex = cols.findIndex(c => c.key === merge.columnKey)
        if (colIndex >= 0) {
          for (let r = 0; r < merge.rowspan; r++) {
            for (let c = 0; c < merge.colspan; c++) {
              if (r === 0 && c === 0) continue // Skip the main cell
              const targetCol = cols[colIndex + c]
              if (targetCol) {
                const hiddenKey = generateCellKey(merge.rowIndex + r, targetCol.key)
                mergeMap.set(hiddenKey, { rowspan: 0, colspan: 0 })
              }
            }
          }
        }
      }
    }

    // Apply auto row merge for same values
    if (config.autoRowMerge && config.autoRowMergeColumns?.length) {
      for (const columnKey of config.autoRowMergeColumns) {
        const col = cols.find(c => c.key === columnKey)
        if (!col) continue

        let mergeStartRow = 0
        let mergeCount = 1

        for (let rowIdx = 1; rowIdx <= rows.length; rowIdx++) {
          const currentValue = rowIdx < rows.length ? rows[rowIdx]![col.dataIndex] : null
          const previousValue = rows[rowIdx - 1]![col.dataIndex]

          if (rowIdx < rows.length && currentValue === previousValue) {
            mergeCount++
          } else {
            // Apply merge if count > 1
            if (mergeCount > 1) {
              const startKey = generateCellKey(mergeStartRow, columnKey)
              mergeMap.set(startKey, { rowspan: mergeCount, colspan: 1 })

              // Mark subsequent cells as hidden
              for (let i = 1; i < mergeCount; i++) {
                const hiddenKey = generateCellKey(mergeStartRow + i, columnKey)
                mergeMap.set(hiddenKey, { rowspan: 0, colspan: 0 })
              }
            }

            // Reset for next group
            mergeStartRow = rowIdx
            mergeCount = 1
          }
        }
      }
    }

    // Apply merge rules
    if (config.rules?.length) {
      for (const rule of config.rules) {
        const col = cols.find(c => c.key === rule.columnKey)
        if (!col) continue

        if (rule.type === 'row' || rule.type === 'both') {
          let mergeStartRow = 0
          let mergeCount = 1

          for (let rowIdx = 1; rowIdx <= rows.length; rowIdx++) {
            const currentRow = rowIdx < rows.length ? rows[rowIdx]! : null
            const previousRow = rows[rowIdx - 1]!
            const currentValue = currentRow?.[col.dataIndex]
            const previousValue = previousRow[col.dataIndex]

            let shouldMerge = false
            if (currentRow) {
              if (rule.condition) {
                shouldMerge = rule.condition(currentValue, previousValue, currentRow, previousRow)
              } else if (rule.mergeOnSameValue !== false) {
                shouldMerge = currentValue === previousValue
              }
            }

            if (shouldMerge) {
              mergeCount++
            } else {
              if (mergeCount > 1) {
                const startKey = generateCellKey(mergeStartRow, rule.columnKey)
                mergeMap.set(startKey, { rowspan: mergeCount, colspan: 1 })

                for (let i = 1; i < mergeCount; i++) {
                  const hiddenKey = generateCellKey(mergeStartRow + i, rule.columnKey)
                  mergeMap.set(hiddenKey, { rowspan: 0, colspan: 0 })
                }
              }

              mergeStartRow = rowIdx
              mergeCount = 1
            }
          }
        }
      }
    }

    // Apply custom spanMethod if provided
    if (config.spanMethod) {
      for (let rowIdx = 0; rowIdx < rows.length; rowIdx++) {
        for (let colIdx = 0; colIdx < cols.length; colIdx++) {
          const col = cols[colIdx]!
          const span = config.spanMethod({
            row: rows[rowIdx]!,
            column: col,
            rowIndex: rowIdx,
            columnIndex: colIdx,
          })
          if (span) {
            const key = generateCellKey(rowIdx, col.key)
            mergeMap.set(key, span)
          }
        }
      }
    }

    return mergeMap
  })

  const getCellMergeSpan = (rowIndex: number, columnKey: string): CellMergeSpan => {
    const key = generateCellKey(rowIndex, columnKey)
    return cellMergeMap.value.get(key) ?? { rowspan: 1, colspan: 1 }
  }

  const isCellHidden = (rowIndex: number, columnKey: string): boolean => {
    const span = getCellMergeSpan(rowIndex, columnKey)
    return span.rowspan === 0 || span.colspan === 0
  }

  // ============================================================================
  // Freeze Pane Calculation
  // Validates: 14.5.23 - 冻结表头 - 固定表头滚动
  // Validates: 14.5.24 - 冻结列 - 固定左侧列滚动
  // ============================================================================

  const computedColumns = computed<ComputedColumnInfo[]>(() => {
    const cols = columns.value
    const freeze = normalizedFreezeConfig.value

    let leftOffset = 0

    return cols.map((col, index) => {
      // Determine if column is frozen
      let frozen = false
      let frozenPosition: 'left' | 'right' | undefined
      let frozenOffset: number | undefined

      // Check by column key
      if (freeze.freezeLeftColumnKeys?.includes(col.key)) {
        frozen = true
        frozenPosition = 'left'
      } else if (freeze.freezeRightColumnKeys?.includes(col.key)) {
        frozen = true
        frozenPosition = 'right'
      }
      // Check by column index
      else if (freeze.freezeLeftColumns && index < freeze.freezeLeftColumns) {
        frozen = true
        frozenPosition = 'left'
      } else if (freeze.freezeRightColumns && index >= cols.length - freeze.freezeRightColumns) {
        frozen = true
        frozenPosition = 'right'
      }
      // Check by column fixed property
      else if (col.fixed === 'left') {
        frozen = true
        frozenPosition = 'left'
      } else if (col.fixed === 'right') {
        frozen = true
        frozenPosition = 'right'
      }

      // Calculate width
      let computedWidth = col.width === 'auto' ? 100 : (col.width ?? 100)
      
      // Apply auto-calculated width if available
      if (columnWidths.value.has(col.key)) {
        computedWidth = columnWidths.value.get(col.key)!
      }

      // Apply min/max constraints
      if (col.minWidth && computedWidth < col.minWidth) {
        computedWidth = col.minWidth
      }
      if (col.maxWidth && computedWidth > col.maxWidth) {
        computedWidth = col.maxWidth
      }

      // Calculate frozen offset
      if (frozenPosition === 'left') {
        frozenOffset = leftOffset
        leftOffset += computedWidth
      }

      const result: ComputedColumnInfo = {
        ...col,
        computedWidth,
        frozen,
      }
      if (frozenPosition !== undefined) result.frozenPosition = frozenPosition
      if (frozenOffset !== undefined) result.frozenOffset = frozenOffset

      return result
    })
  })

  // Calculate right frozen offsets (need to process from right to left)
  const computedColumnsWithRightOffset = computed<ComputedColumnInfo[]>(() => {
    const cols = [...computedColumns.value]
    let rightOffset = 0

    // Process from right to left for right frozen columns
    for (let i = cols.length - 1; i >= 0; i--) {
      const col = cols[i]!
      if (col.frozenPosition === 'right') {
        cols[i] = { ...col, frozenOffset: rightOffset }
        rightOffset += cols[i]!.computedWidth
      }
    }

    return cols
  })

  const frozenLeftColumns = computed<ComputedColumnInfo[]>(() =>
    computedColumnsWithRightOffset.value.filter(col => col.frozenPosition === 'left')
  )

  const frozenRightColumns = computed<ComputedColumnInfo[]>(() =>
    computedColumnsWithRightOffset.value.filter(col => col.frozenPosition === 'right')
  )

  const scrollableColumns = computed<ComputedColumnInfo[]>(() =>
    computedColumnsWithRightOffset.value.filter(col => !col.frozen)
  )

  const leftFrozenWidth = computed<number>(() =>
    frozenLeftColumns.value.reduce((sum, col) => sum + col.computedWidth, 0)
  )

  const rightFrozenWidth = computed<number>(() =>
    frozenRightColumns.value.reduce((sum, col) => sum + col.computedWidth, 0)
  )

  const totalWidth = computed<number>(() =>
    computedColumnsWithRightOffset.value.reduce((sum, col) => sum + col.computedWidth, 0)
  )

  // ============================================================================
  // Auto Column Width Calculation
  // Validates: 14.5.25 - 列宽自适应 - 根据内容自动调整列宽
  // ============================================================================

  const calculateColumnWidths = (_tableElement?: HTMLElement): void => {
    const autoSize = normalizedAutoSizeConfig.value
    if (!autoSize.autoColumnWidth) return

    const cols = columns.value
    const rows = data.value
    const newWidths = new Map<string, number>()

    // Create a temporary canvas for text measurement
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    // Set font for measurement (should match table font)
    ctx.font = '14px -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial'

    for (const col of cols) {
      // Skip if not in auto width columns list (when specified)
      if (autoSize.autoWidthColumns?.length && !autoSize.autoWidthColumns.includes(col.key)) {
        continue
      }

      // Skip if column has explicit width
      if (col.width && col.width !== 'auto' && !col.autoWidth) {
        continue
      }

      let maxWidth = autoSize.minAutoWidth ?? 50

      // Measure header text
      if (autoSize.includeHeader) {
        const headerWidth = ctx.measureText(col.title).width + (autoSize.padding ?? 16) * 2
        maxWidth = Math.max(maxWidth, headerWidth)
      }

      // Measure cell content
      for (const row of rows) {
        const value = row[col.dataIndex]
        if (value != null) {
          const text = String(value)
          const cellWidth = ctx.measureText(text).width + (autoSize.padding ?? 16) * 2
          maxWidth = Math.max(maxWidth, cellWidth)
        }
      }

      // Apply max constraint
      if (autoSize.maxAutoWidth) {
        maxWidth = Math.min(maxWidth, autoSize.maxAutoWidth)
      }

      newWidths.set(col.key, Math.ceil(maxWidth))
    }

    columnWidths.value = newWidths
  }

  // ============================================================================
  // Zebra Stripe
  // Validates: 14.5.26 - 斑马纹 - 交替行背景色
  // ============================================================================

  const getZebraClass = (rowIndex: number): string => {
    const config = normalizedZebraConfig.value
    if (!config.enabled) return ''

    const interval = config.stripeInterval ?? 1
    const isEven = Math.floor(rowIndex / interval) % 2 === 0

    return isEven ? 'enhanced-table-row--even' : 'enhanced-table-row--odd'
  }

  // ============================================================================
  // Scroll State Management
  // ============================================================================

  const updateScrollState = (scrollLeft: number, scrollTop: number): void => {
    const container = containerRef?.value
    if (!container) {
      scrollState.value = {
        scrollLeft,
        scrollTop,
        isScrolledLeft: scrollLeft <= 0,
        isScrolledRight: false,
        isScrolledTop: scrollTop <= 0,
        isScrolledBottom: false,
      }
      return
    }

    const { scrollWidth, clientWidth, scrollHeight, clientHeight } = container

    scrollState.value = {
      scrollLeft,
      scrollTop,
      isScrolledLeft: scrollLeft <= 0,
      isScrolledRight: scrollLeft + clientWidth >= scrollWidth - 1,
      isScrolledTop: scrollTop <= 0,
      isScrolledBottom: scrollTop + clientHeight >= scrollHeight - 1,
    }
  }

  // ============================================================================
  // Auto-calculate widths on data/columns change
  // ============================================================================

  watch(
    [data, columns],
    () => {
      if (normalizedAutoSizeConfig.value.autoColumnWidth) {
        // Use nextTick to ensure DOM is updated
        setTimeout(() => calculateColumnWidths(), 0)
      }
    },
    { immediate: true }
  )

  // ============================================================================
  // Return
  // ============================================================================

  return {
    computedColumns: computedColumnsWithRightOffset,
    cellMergeMap,
    getCellMergeSpan,
    isCellHidden,
    getZebraClass,
    frozenLeftColumns,
    frozenRightColumns,
    scrollableColumns,
    leftFrozenWidth,
    rightFrozenWidth,
    columnWidths,
    calculateColumnWidths,
    scrollState,
    updateScrollState,
    totalWidth,
  }
}

export default useTableLayout
