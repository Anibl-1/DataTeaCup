/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 表格交互增强 Composable
 * Enhanced Table Interaction Composable
 * 
 * 提供单元格点击、悬停提示、排序、筛选、列拖拽等功能
 * Provides cell click, tooltip, sorting, filtering, column drag features
 * 
 * 需求: 14.6.28, 14.6.29, 14.6.30, 14.6.31, 14.6.32, 14.6.33
 */

import { ref, computed, watch, type Ref, type ComputedRef } from 'vue'
import type {
  ColumnConfig,
  CellClickEvent,
  CellClickAction,
  DrillDownConfig,
  TooltipConfig,
  TooltipState,
  SortConfig,
  SortDirection,
  FilterState,
  FilterValue,
  ColumnFilterConfig,
  ColumnDragConfig,
  TableInteractionConfig,
} from '@/components/EnhancedTable/types'

// ============================================================================
// Types
// ============================================================================

export interface UseTableInteractionOptions {
  /** Table data */
  data: Ref<Record<string, any>[]>
  /** Column configurations */
  columns: Ref<ColumnConfig[]>
  /** Interaction configuration */
  config?: Ref<TableInteractionConfig | undefined>
  /** Column widths map (from useTableLayout) */
  columnWidths?: Ref<Map<string, number>>
}

export interface UseTableInteractionReturn {
  // Cell Click
  handleCellClick: (event: CellClickEvent) => void
  getCellClickAction: (columnKey: string) => CellClickAction | undefined
  
  // Tooltip
  tooltipState: Ref<TooltipState>
  showTooltip: (event: MouseEvent, rowIndex: number, columnKey: string, value: any, row: Record<string, any>, column: ColumnConfig) => void
  hideTooltip: () => void
  shouldShowTooltip: (columnKey: string) => boolean
  
  // Sort
  sortConfig: Ref<SortConfig>
  sortedData: ComputedRef<Record<string, any>[]>
  handleSort: (columnKey: string) => void
  clearSort: () => void
  getSortDirection: (columnKey: string) => SortDirection
  isSortable: (column: ColumnConfig) => boolean
  
  // Filter
  filterState: Ref<FilterState>
  filteredData: ComputedRef<Record<string, any>[]>
  setFilter: (columnKey: string, value: FilterValue | null) => void
  clearFilter: (columnKey: string) => void
  clearAllFilters: () => void
  getFilterValue: (columnKey: string) => FilterValue | null
  isFilterable: (column: ColumnConfig) => boolean
  getFilterConfig: (columnKey: string) => ColumnFilterConfig | undefined
  
  // Column Drag - Resize
  isResizing: Ref<boolean>
  resizingColumn: Ref<string | null>
  handleResizeStart: (columnKey: string, event: MouseEvent) => void
  handleResizeMove: (event: MouseEvent) => void
  handleResizeEnd: () => void
  
  // Column Drag - Reorder
  isDragging: Ref<boolean>
  draggingColumn: Ref<string | null>
  dragOverColumn: Ref<string | null>
  columnOrder: Ref<string[]>
  handleDragStart: (columnKey: string, event: DragEvent) => void
  handleDragOver: (columnKey: string, event: DragEvent) => void
  handleDragEnd: (event: DragEvent) => void
  handleDrop: (columnKey: string, event: DragEvent) => void
  reorderedColumns: ComputedRef<ColumnConfig[]>
  
  // Combined processed data (sorted + filtered)
  processedData: ComputedRef<Record<string, any>[]>
}

// ============================================================================
// Default Configurations
// ============================================================================

const DEFAULT_TOOLTIP_CONFIG: TooltipConfig = {
  enabled: true,
  showOnTruncate: true,
  placement: 'top',
  showDelay: 500,
  hideDelay: 100,
  maxWidth: 300,
}

const DEFAULT_SORT_CONFIG: SortConfig = {
  column: null,
  direction: null,
  multiple: false,
  remote: false,
}

const DEFAULT_COLUMN_DRAG_CONFIG: ColumnDragConfig = {
  resizable: true,
  reorderable: true,
  minWidth: 50,
  maxWidth: 500,
}

// ============================================================================
// Helper Functions
// ============================================================================

/**
 * Compare values for sorting
 */
function compareValues(a: any, b: any, direction: SortDirection): number {
  if (a === b) return 0
  if (a === null || a === undefined) return direction === 'asc' ? 1 : -1
  if (b === null || b === undefined) return direction === 'asc' ? -1 : 1
  
  // Handle numbers
  if (typeof a === 'number' && typeof b === 'number') {
    return direction === 'asc' ? a - b : b - a
  }
  
  // Handle dates
  if (a instanceof Date && b instanceof Date) {
    return direction === 'asc' ? a.getTime() - b.getTime() : b.getTime() - a.getTime()
  }
  
  // Handle strings
  const strA = String(a).toLowerCase()
  const strB = String(b).toLowerCase()
  const result = strA.localeCompare(strB)
  return direction === 'asc' ? result : -result
}

/**
 * Apply filter to a value
 */
function applyFilter(value: any, filter: FilterValue): boolean {
  const { operator, value: filterValue, value2 } = filter
  
  // Handle null/undefined values
  if (value === null || value === undefined) {
    if (operator === 'isEmpty') return true
    if (operator === 'isNotEmpty') return false
    return false
  }
  
  const strValue = String(value).toLowerCase()
  const strFilterValue = filterValue != null ? String(filterValue).toLowerCase() : ''
  
  switch (operator) {
    case 'equals':
      return value === filterValue || strValue === strFilterValue
    case 'notEquals':
      return value !== filterValue && strValue !== strFilterValue
    case 'contains':
      return strValue.includes(strFilterValue)
    case 'notContains':
      return !strValue.includes(strFilterValue)
    case 'startsWith':
      return strValue.startsWith(strFilterValue)
    case 'endsWith':
      return strValue.endsWith(strFilterValue)
    case 'greaterThan':
      return Number(value) > Number(filterValue)
    case 'lessThan':
      return Number(value) < Number(filterValue)
    case 'greaterThanOrEqual':
      return Number(value) >= Number(filterValue)
    case 'lessThanOrEqual':
      return Number(value) <= Number(filterValue)
    case 'between':
      return Number(value) >= Number(filterValue) && Number(value) <= Number(value2)
    case 'in':
      return Array.isArray(filterValue) && filterValue.includes(value)
    case 'notIn':
      return Array.isArray(filterValue) && !filterValue.includes(value)
    case 'isEmpty':
      return value === '' || value === null || value === undefined
    case 'isNotEmpty':
      return value !== '' && value !== null && value !== undefined
    default:
      return true
  }
}

/**
 * Check if element content is truncated
 */
function isContentTruncated(element: HTMLElement): boolean {
  return element.scrollWidth > element.clientWidth || element.scrollHeight > element.clientHeight
}

// ============================================================================
// Main Composable
// ============================================================================

export function useTableInteraction(options: UseTableInteractionOptions): UseTableInteractionReturn {
  const { data, columns, config, columnWidths } = options

  // ============================================================================
  // Normalized Configs
  // ============================================================================

  const normalizedTooltipConfig = computed<TooltipConfig>(() => ({
    ...DEFAULT_TOOLTIP_CONFIG,
    ...config?.value?.tooltip,
  }))

  const normalizedSortConfig = computed<Partial<SortConfig>>(() => ({
    ...DEFAULT_SORT_CONFIG,
    ...config?.value?.sort,
  }))

  const normalizedDragConfig = computed<ColumnDragConfig>(() => ({
    ...DEFAULT_COLUMN_DRAG_CONFIG,
    ...config?.value?.columnDrag,
  }))

  // ============================================================================
  // Cell Click Handling
  // Validates: 14.6.28 - 单元格点击事件（钻取、跳转、弹窗）
  // ============================================================================

  const getCellClickAction = (columnKey: string): CellClickAction | undefined => {
    const actions = config?.value?.cellClickActions
    if (!actions?.length) return undefined
    
    return actions.find(action => {
      if (!action.columns?.length) return true // Apply to all columns
      return action.columns.includes(columnKey)
    })
  }

  const handleCellClick = (event: CellClickEvent): void => {
    const action = getCellClickAction(event.column.key)
    if (!action) return

    switch (action.type) {
      case 'drill-down':
        handleDrillDown(event, action.drillDown)
        break
      case 'navigate':
        handleNavigate(event, action.navigateUrl)
        break
      case 'popup':
        handlePopup(event, action.popupConfig)
        break
      case 'custom':
        action.handler?.(event)
        break
    }
  }

  const handleDrillDown = (event: CellClickEvent, drillDown?: DrillDownConfig): void => {
    if (!drillDown) return
    
    if (drillDown.handler) {
      drillDown.handler(event.row, event.column, event.value)
      return
    }

    // Build parameters from mapping
    const params: Record<string, any> = { ...drillDown.params }
    if (drillDown.paramMapping) {
      for (const [paramKey, fieldKey] of Object.entries(drillDown.paramMapping)) {
        params[paramKey] = event.row[fieldKey]
      }
    }

    // Navigate to target
    if (drillDown.targetUrl) {
      const url = new URL(drillDown.targetUrl, window.location.origin)
      for (const [key, value] of Object.entries(params)) {
        url.searchParams.set(key, String(value))
      }
      window.location.href = url.toString()
    }
  }

  const handleNavigate = (event: CellClickEvent, urlTemplate?: string): void => {
    if (!urlTemplate) return
    
    // Replace placeholders with row values
    let url = urlTemplate
    const placeholderRegex = /\{(\w+)\}/g
    url = url.replace(placeholderRegex, (_, field) => {
      return encodeURIComponent(String(event.row[field] ?? ''))
    })
    
    window.location.href = url
  }

  const handlePopup = (event: CellClickEvent, popupConfig?: CellClickAction['popupConfig']): void => {
    if (!popupConfig) return
    
    // Emit popup event - actual popup implementation depends on UI framework
    const content = typeof popupConfig.content === 'function'
      ? popupConfig.content(event.row, event.value)
      : popupConfig.content ?? String(event.value)
    
    // This would typically emit an event or use a modal service
    void content
  }

  // ============================================================================
  // Tooltip Handling
  // Validates: 14.6.29 - 单元格悬停提示（Tooltip）
  // ============================================================================

  const tooltipState = ref<TooltipState>({
    visible: false,
    content: '',
    position: { x: 0, y: 0 },
  })

  let tooltipShowTimer: ReturnType<typeof setTimeout> | null = null
  let tooltipHideTimer: ReturnType<typeof setTimeout> | null = null

  const shouldShowTooltip = (columnKey: string): boolean => {
    const tooltipConfig = normalizedTooltipConfig.value
    if (!tooltipConfig.enabled) return false
    if (tooltipConfig.columns?.length && !tooltipConfig.columns.includes(columnKey)) return false
    return true
  }

  const showTooltip = (
    event: MouseEvent,
    rowIndex: number,
    columnKey: string,
    value: any,
    row: Record<string, any>,
    column: ColumnConfig
  ): void => {
    if (!shouldShowTooltip(columnKey)) return

    const tooltipConfig = normalizedTooltipConfig.value
    const target = event.currentTarget as HTMLElement

    // Check if content is truncated (if showOnTruncate is enabled)
    if (tooltipConfig.showOnTruncate) {
      const contentElement = target.querySelector('.enhanced-table__cell-content') as HTMLElement
      if (contentElement && !isContentTruncated(contentElement)) {
        return
      }
    }

    // Clear any pending hide timer
    if (tooltipHideTimer) {
      clearTimeout(tooltipHideTimer)
      tooltipHideTimer = null
    }

    // Set show timer
    tooltipShowTimer = setTimeout(() => {
      const content = tooltipConfig.content
        ? tooltipConfig.content(row, column, value)
        : String(value ?? '')

      const rect = target.getBoundingClientRect()
      let x = rect.left + rect.width / 2
      let y = rect.top

      // Adjust position based on placement
      switch (tooltipConfig.placement) {
        case 'bottom':
          y = rect.bottom
          break
        case 'left':
          x = rect.left
          y = rect.top + rect.height / 2
          break
        case 'right':
          x = rect.right
          y = rect.top + rect.height / 2
          break
        default: // top
          y = rect.top
      }

      tooltipState.value = {
        visible: true,
        content,
        position: { x, y },
        cell: { rowIndex, columnKey, value },
      }
    }, tooltipConfig.showDelay)
  }

  const hideTooltip = (): void => {
    // Clear any pending show timer
    if (tooltipShowTimer) {
      clearTimeout(tooltipShowTimer)
      tooltipShowTimer = null
    }

    const tooltipConfig = normalizedTooltipConfig.value
    
    tooltipHideTimer = setTimeout(() => {
      tooltipState.value = {
        ...tooltipState.value,
        visible: false,
      }
    }, tooltipConfig.hideDelay)
  }

  // ============================================================================
  // Sort Handling
  // Validates: 14.6.30 - 列排序（点击表头排序）
  // ============================================================================

  const sortConfig = ref<SortConfig>({
    column: null,
    direction: null,
    multiple: normalizedSortConfig.value.multiple ?? false,
    sortOrder: [],
    remote: normalizedSortConfig.value.remote ?? false,
  })

  const isSortable = (column: ColumnConfig): boolean => {
    return column.sortable === true
  }

  const getSortDirection = (columnKey: string): SortDirection => {
    if (sortConfig.value.column === columnKey) {
      return sortConfig.value.direction
    }
    return null
  }

  const handleSort = (columnKey: string): void => {
    const column = columns.value.find(c => c.key === columnKey)
    if (!column || !isSortable(column)) return

    let newDirection: SortDirection
    const currentDirection = getSortDirection(columnKey)

    // Cycle through: null -> asc -> desc -> null
    if (currentDirection === null) {
      newDirection = 'asc'
    } else if (currentDirection === 'asc') {
      newDirection = 'desc'
    } else {
      newDirection = null
    }

    sortConfig.value = {
      ...sortConfig.value,
      column: newDirection ? columnKey : null,
      direction: newDirection,
    }
  }

  const clearSort = (): void => {
    sortConfig.value = {
      ...sortConfig.value,
      column: null,
      direction: null,
      sortOrder: [],
    }
  }

  const sortedData = computed<Record<string, any>[]>(() => {
    const { column, direction, remote, comparator } = sortConfig.value
    
    // If remote sorting, return original data
    if (remote || !column || !direction) {
      return data.value
    }

    const col = columns.value.find(c => c.key === column)
    if (!col) return data.value

    return [...data.value].sort((a, b) => {
      const valueA = a[col.dataIndex]
      const valueB = b[col.dataIndex]
      
      if (comparator) {
        return comparator(valueA, valueB, column, direction)
      }
      
      return compareValues(valueA, valueB, direction)
    })
  })

  // ============================================================================
  // Filter Handling
  // Validates: 14.6.31 - 列筛选（下拉筛选器）
  // ============================================================================

  const filterState = ref<FilterState>({
    filters: new Map(),
    configs: new Map(),
  })

  // Initialize filter configs from options
  watch(
    () => config?.value?.filters,
    (filterConfigs) => {
      if (filterConfigs?.length) {
        const configMap = new Map<string, ColumnFilterConfig>()
        for (const fc of filterConfigs) {
          configMap.set(fc.column, fc)
        }
        filterState.value.configs = configMap
      }
    },
    { immediate: true }
  )

  const isFilterable = (column: ColumnConfig): boolean => {
    return column.filterable === true || filterState.value.configs.has(column.key)
  }

  const getFilterConfig = (columnKey: string): ColumnFilterConfig | undefined => {
    return filterState.value.configs.get(columnKey)
  }

  const getFilterValue = (columnKey: string): FilterValue | null => {
    return filterState.value.filters.get(columnKey) ?? null
  }

  const setFilter = (columnKey: string, value: FilterValue | null): void => {
    const newFilters = new Map(filterState.value.filters)
    
    if (value === null) {
      newFilters.delete(columnKey)
    } else {
      newFilters.set(columnKey, value)
    }
    
    filterState.value = {
      ...filterState.value,
      filters: newFilters,
    }
  }

  const clearFilter = (columnKey: string): void => {
    setFilter(columnKey, null)
  }

  const clearAllFilters = (): void => {
    filterState.value = {
      ...filterState.value,
      filters: new Map(),
    }
  }

  const filteredData = computed<Record<string, any>[]>(() => {
    const filters = filterState.value.filters
    if (filters.size === 0) {
      return sortedData.value
    }

    return sortedData.value.filter(row => {
      for (const [columnKey, filterValue] of filters) {
        const col = columns.value.find(c => c.key === columnKey)
        if (!col) continue

        const value = row[col.dataIndex]
        const filterConfig = getFilterConfig(columnKey)
        
        // Use custom filter function if provided
        if (filterConfig?.filterFn) {
          if (!filterConfig.filterFn(value, filterValue, row)) {
            return false
          }
        } else {
          if (!applyFilter(value, filterValue)) {
            return false
          }
        }
      }
      return true
    })
  })

  // ============================================================================
  // Column Resize Handling
  // Validates: 14.6.32 - 列拖拽调整宽度
  // ============================================================================

  const isResizing = ref(false)
  const resizingColumn = ref<string | null>(null)
  const resizeStartX = ref(0)
  const resizeStartWidth = ref(0)

  const handleResizeStart = (columnKey: string, event: MouseEvent): void => {
    const dragConfig = normalizedDragConfig.value
    if (!dragConfig.resizable) return
    if (dragConfig.nonResizableColumns?.includes(columnKey)) return

    event.preventDefault()
    event.stopPropagation()

    isResizing.value = true
    resizingColumn.value = columnKey
    resizeStartX.value = event.clientX
    
    // Get current width
    const currentWidth = columnWidths?.value.get(columnKey) ?? 100
    resizeStartWidth.value = currentWidth

    // Add global event listeners
    document.addEventListener('mousemove', handleResizeMove)
    document.addEventListener('mouseup', handleResizeEnd)
  }

  const handleResizeMove = (event: MouseEvent): void => {
    if (!isResizing.value || !resizingColumn.value) return

    const dragConfig = normalizedDragConfig.value
    const delta = event.clientX - resizeStartX.value
    let newWidth = resizeStartWidth.value + delta

    // Apply constraints
    if (dragConfig.minWidth) {
      newWidth = Math.max(newWidth, dragConfig.minWidth)
    }
    if (dragConfig.maxWidth) {
      newWidth = Math.min(newWidth, dragConfig.maxWidth)
    }

    // Update column width
    if (columnWidths?.value) {
      columnWidths.value.set(resizingColumn.value, newWidth)
    }
  }

  const handleResizeEnd = (): void => {
    isResizing.value = false
    resizingColumn.value = null

    // Remove global event listeners
    document.removeEventListener('mousemove', handleResizeMove)
    document.removeEventListener('mouseup', handleResizeEnd)
  }

  // ============================================================================
  // Column Reorder Handling
  // Validates: 14.6.33 - 列拖拽调整顺序
  // ============================================================================

  const isDragging = ref(false)
  const draggingColumn = ref<string | null>(null)
  const dragOverColumn = ref<string | null>(null)
  const columnOrder = ref<string[]>([])

  // Initialize column order from columns
  watch(
    columns,
    (cols) => {
      if (columnOrder.value.length === 0) {
        columnOrder.value = cols.map(c => c.key)
      }
    },
    { immediate: true }
  )

  const handleDragStart = (columnKey: string, event: DragEvent): void => {
    const dragConfig = normalizedDragConfig.value
    if (!dragConfig.reorderable) return
    if (dragConfig.nonReorderableColumns?.includes(columnKey)) return

    isDragging.value = true
    draggingColumn.value = columnKey

    // Set drag data
    event.dataTransfer?.setData('text/plain', columnKey)
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move'
    }
  }

  const handleDragOver = (columnKey: string, event: DragEvent): void => {
    if (!isDragging.value) return
    
    event.preventDefault()
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move'
    }
    
    dragOverColumn.value = columnKey
  }

  const handleDragEnd = (_event: DragEvent): void => {
    isDragging.value = false
    draggingColumn.value = null
    dragOverColumn.value = null
  }

  const handleDrop = (columnKey: string, event: DragEvent): void => {
    event.preventDefault()
    
    const dragConfig = normalizedDragConfig.value
    if (!dragConfig.reorderable) return
    if (dragConfig.nonReorderableColumns?.includes(columnKey)) return

    const sourceKey = draggingColumn.value
    if (!sourceKey || sourceKey === columnKey) {
      handleDragEnd(event)
      return
    }

    // Reorder columns
    const newOrder = [...columnOrder.value]
    const sourceIndex = newOrder.indexOf(sourceKey)
    const targetIndex = newOrder.indexOf(columnKey)

    if (sourceIndex !== -1 && targetIndex !== -1) {
      newOrder.splice(sourceIndex, 1)
      newOrder.splice(targetIndex, 0, sourceKey)
      columnOrder.value = newOrder
    }

    handleDragEnd(event)
  }

  const reorderedColumns = computed<ColumnConfig[]>(() => {
    const order = columnOrder.value
    if (order.length === 0) return columns.value

    const columnMap = new Map(columns.value.map(c => [c.key, c]))
    return order
      .map(key => columnMap.get(key))
      .filter((c): c is ColumnConfig => c !== undefined)
  })

  // ============================================================================
  // Combined Processed Data
  // ============================================================================

  const processedData = computed<Record<string, any>[]>(() => {
    return filteredData.value
  })

  // ============================================================================
  // Return
  // ============================================================================

  return {
    // Cell Click
    handleCellClick,
    getCellClickAction,
    
    // Tooltip
    tooltipState,
    showTooltip,
    hideTooltip,
    shouldShowTooltip,
    
    // Sort
    sortConfig,
    sortedData,
    handleSort,
    clearSort,
    getSortDirection,
    isSortable,
    
    // Filter
    filterState,
    filteredData,
    setFilter,
    clearFilter,
    clearAllFilters,
    getFilterValue,
    isFilterable,
    getFilterConfig,
    
    // Column Resize
    isResizing,
    resizingColumn,
    handleResizeStart,
    handleResizeMove,
    handleResizeEnd,
    
    // Column Reorder
    isDragging,
    draggingColumn,
    dragOverColumn,
    columnOrder,
    handleDragStart,
    handleDragOver,
    handleDragEnd,
    handleDrop,
    reorderedColumns,
    
    // Combined
    processedData,
  }
}

export default useTableInteraction
