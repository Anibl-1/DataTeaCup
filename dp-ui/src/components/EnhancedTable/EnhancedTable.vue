<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 增强表格组件
 * Enhanced Table Component
 * 
 * 支持行列合并、冻结窗格、列宽自适应、斑马纹等功能
 * 支持单元格点击、悬停提示、排序、筛选、列拖拽等交互功能
 * Supports cell merge, freeze panes, auto column width, zebra stripes
 * Supports cell click, tooltip, sorting, filtering, column drag interactions
 * 
 * 需求: 14.5.21, 14.5.22, 14.5.23, 14.5.24, 14.5.25, 14.5.26
 * 需求: 14.6.28, 14.6.29, 14.6.30, 14.6.31, 14.6.32, 14.6.33
 */

import { ref, computed, toRef, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useTableLayout } from '@/composables/useTableLayout'
import { useTableInteraction } from '@/composables/useTableInteraction'
import type {
  ColumnConfig,
  MergeConfig,
  FreezeConfig,
  AutoSizeConfig,
  ZebraStripeConfig,
  TableStyleConfig,
  CellClickEvent,
  ColumnResizeEvent,
  TableScrollEvent,
  ComputedColumnInfo,
  TableInteractionConfig,
  SortChangeEvent,
  FilterChangeEvent,
  ColumnReorderEvent,
  FilterValue,
  SortDirection,
} from './types'
import {
  DEFAULT_TABLE_STYLE_CONFIG,
  DEFAULT_ZEBRA_STRIPE_CONFIG,
} from './types'

// ============================================================================
// Props
// ============================================================================

interface Props {
  /** Table data */
  data: Record<string, any>[]
  /** Column configurations */
  columns: ColumnConfig[]
  /** Merge configuration */
  mergeConfig?: MergeConfig
  /** Freeze configuration */
  freezeConfig?: FreezeConfig
  /** Auto size configuration */
  autoSizeConfig?: AutoSizeConfig
  /** Zebra stripe configuration */
  zebraStripe?: boolean | ZebraStripeConfig
  /** Table style configuration */
  tableStyle?: TableStyleConfig
  /** Row key field */
  rowKey?: string | ((row: Record<string, any>) => string)
  /** Table height */
  height?: number | string
  /** Maximum table height */
  maxHeight?: number | string
  /** Whether to show loading state */
  loading?: boolean
  /** Empty state text */
  emptyText?: string
  /** Whether table is bordered */
  bordered?: boolean
  /** Table size */
  size?: 'small' | 'medium' | 'large'
  /** Interaction configuration - 14.6.28-14.6.33 */
  interactionConfig?: TableInteractionConfig
}

const props = withDefaults(defineProps<Props>(), {
  rowKey: 'id',
  loading: false,
  emptyText: '暂无数据',
  bordered: true,
  size: 'medium',
})

// ============================================================================
// Emits
// ============================================================================

const emit = defineEmits<{
  (e: 'cell-click', event: CellClickEvent): void
  (e: 'column-resize', event: ColumnResizeEvent): void
  (e: 'scroll', event: TableScrollEvent): void
  (e: 'sort-change', event: SortChangeEvent): void
  (e: 'filter-change', event: FilterChangeEvent): void
  (e: 'column-reorder', event: ColumnReorderEvent): void
}>()

// ============================================================================
// Refs
// ============================================================================

const tableContainerRef = ref<HTMLElement | null>(null)
const tableBodyRef = ref<HTMLElement | null>(null)
const filterDropdownRef = ref<HTMLElement | null>(null)
const activeFilterColumn = ref<string | null>(null)
const filterInputValue = ref<string>('')

// ============================================================================
// Use Table Layout Composable
// ============================================================================

const {
  computedColumns,
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
} = useTableLayout({
  data: toRef(props, 'data'),
  columns: toRef(props, 'columns'),
  mergeConfig: toRef(props, 'mergeConfig'),
  freezeConfig: toRef(props, 'freezeConfig'),
  autoSizeConfig: toRef(props, 'autoSizeConfig'),
  zebraStripeConfig: toRef(props, 'zebraStripe'),
  containerRef: tableContainerRef,
})

// ============================================================================
// Use Table Interaction Composable
// Validates: 14.6.28, 14.6.29, 14.6.30, 14.6.31, 14.6.32, 14.6.33
// ============================================================================

const {
  // Cell Click
  handleCellClick: handleInteractionCellClick,
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
} = useTableInteraction({
  data: toRef(props, 'data'),
  columns: toRef(props, 'columns'),
  config: toRef(props, 'interactionConfig'),
  columnWidths,
})

// Use processed data (sorted + filtered) for display
const displayData = computed(() => processedData.value)

// ============================================================================
// Computed Styles
// ============================================================================

const normalizedTableStyle = computed<TableStyleConfig>(() => ({
  ...DEFAULT_TABLE_STYLE_CONFIG,
  ...props.tableStyle,
}))

const normalizedZebraConfig = computed<ZebraStripeConfig>(() => {
  if (typeof props.zebraStripe === 'boolean') {
    return { ...DEFAULT_ZEBRA_STRIPE_CONFIG, enabled: props.zebraStripe }
  }
  return { ...DEFAULT_ZEBRA_STRIPE_CONFIG, ...props.zebraStripe }
})

const tableContainerStyle = computed(() => ({
  height: typeof props.height === 'number' ? `${props.height}px` : props.height,
  maxHeight: typeof props.maxHeight === 'number' ? `${props.maxHeight}px` : props.maxHeight,
}))

const tableStyle = computed(() => ({
  width: `${totalWidth.value}px`,
  minWidth: '100%',
}))

const rowHeight = computed(() => {
  switch (props.size) {
    case 'small': return 36
    case 'large': return 56
    default: return 48
  }
})

// ============================================================================
// Row Key Helper
// ============================================================================

const getRowKey = (row: Record<string, any>, index: number): string => {
  if (typeof props.rowKey === 'function') {
    return props.rowKey(row)
  }
  return row[props.rowKey]?.toString() ?? index.toString()
}

// ============================================================================
// Cell Click Handler
// Validates: 14.6.28 - 单元格点击事件（钻取、跳转、弹窗）
// ============================================================================

const handleCellClick = (
  row: Record<string, any>,
  column: ComputedColumnInfo,
  rowIndex: number,
  columnIndex: number,
  event: MouseEvent
) => {
  const cellClickEvent: CellClickEvent = {
    row,
    column,
    rowIndex,
    columnIndex,
    value: row[column.dataIndex],
    event,
  }
  
  // Emit event for external handling
  emit('cell-click', cellClickEvent)
  
  // Handle internal interaction (drill-down, navigate, popup)
  handleInteractionCellClick(cellClickEvent)
}

// ============================================================================
// Cell Hover Handlers (Tooltip)
// Validates: 14.6.29 - 单元格悬停提示（Tooltip）
// ============================================================================

const handleCellMouseEnter = (
  event: MouseEvent,
  row: Record<string, any>,
  column: ComputedColumnInfo,
  rowIndex: number
) => {
  const value = row[column.dataIndex]
  showTooltip(event, rowIndex, column.key, value, row, column)
}

const handleCellMouseLeave = () => {
  hideTooltip()
}

// ============================================================================
// Sort Handler
// Validates: 14.6.30 - 列排序（点击表头排序）
// ============================================================================

const handleHeaderClick = (column: ComputedColumnInfo) => {
  if (!isSortable(column)) return
  
  handleSort(column.key)
  
  // Emit sort change event
  emit('sort-change', {
    column: column.key,
    direction: getSortDirection(column.key),
    sortConfig: sortConfig.value,
  })
}

// ============================================================================
// Filter Handlers
// Validates: 14.6.31 - 列筛选（下拉筛选器）
// ============================================================================

const toggleFilterDropdown = (columnKey: string, event: MouseEvent) => {
  event.stopPropagation()
  
  if (activeFilterColumn.value === columnKey) {
    activeFilterColumn.value = null
  } else {
    activeFilterColumn.value = columnKey
    filterInputValue.value = ''
    
    // Get current filter value
    const currentFilter = getFilterValue(columnKey)
    if (currentFilter) {
      filterInputValue.value = String(currentFilter.value ?? '')
    }
  }
}

const applyFilter = (columnKey: string) => {
  if (filterInputValue.value.trim()) {
    const filterValue: FilterValue = {
      operator: 'contains',
      value: filterInputValue.value.trim(),
    }
    setFilter(columnKey, filterValue)
    
    emit('filter-change', {
      column: columnKey,
      value: filterValue,
      filters: filterState.value.filters,
    })
  } else {
    clearFilter(columnKey)
    
    emit('filter-change', {
      column: columnKey,
      value: null,
      filters: filterState.value.filters,
    })
  }
  
  activeFilterColumn.value = null
}

const handleFilterClear = (columnKey: string) => {
  clearFilter(columnKey)
  filterInputValue.value = ''
  activeFilterColumn.value = null
  
  emit('filter-change', {
    column: columnKey,
    value: null,
    filters: filterState.value.filters,
  })
}

// Close filter dropdown when clicking outside
const handleDocumentClick = (event: MouseEvent) => {
  if (activeFilterColumn.value && filterDropdownRef.value) {
    if (!filterDropdownRef.value.contains(event.target as Node)) {
      activeFilterColumn.value = null
    }
  }
}

// ============================================================================
// Column Resize Handlers
// Validates: 14.6.32 - 列拖拽调整宽度
// ============================================================================

const onResizeStart = (column: ComputedColumnInfo, event: MouseEvent) => {
  if (!column.resizable) return
  handleResizeStart(column.key, event)
}

// Watch for resize end to emit event
watch(isResizing, (newVal, oldVal) => {
  if (oldVal && !newVal && resizingColumn.value) {
    const column = computedColumns.value.find(c => c.key === resizingColumn.value)
    if (column) {
      emit('column-resize', {
        column,
        width: columnWidths.value.get(resizingColumn.value) ?? column.computedWidth,
        oldWidth: column.computedWidth,
      })
    }
  }
})

// ============================================================================
// Column Reorder Handlers
// Validates: 14.6.33 - 列拖拽调整顺序
// ============================================================================

const onDragStart = (column: ComputedColumnInfo, event: DragEvent) => {
  handleDragStart(column.key, event)
}

const onDragOver = (column: ComputedColumnInfo, event: DragEvent) => {
  handleDragOver(column.key, event)
}

const onDrop = (column: ComputedColumnInfo, event: DragEvent) => {
  const fromIndex = columnOrder.value.indexOf(draggingColumn.value ?? '')
  const toIndex = columnOrder.value.indexOf(column.key)
  
  handleDrop(column.key, event)
  
  if (draggingColumn.value && fromIndex !== toIndex) {
    emit('column-reorder', {
      column: props.columns.find(c => c.key === draggingColumn.value)!,
      fromIndex,
      toIndex,
      newOrder: columnOrder.value,
    })
  }
}

// ============================================================================
// Scroll Handler
// ============================================================================

const handleScroll = (event: Event) => {
  const target = event.target as HTMLElement
  updateScrollState(target.scrollLeft, target.scrollTop)
  
  emit('scroll', {
    scrollLeft: target.scrollLeft,
    scrollTop: target.scrollTop,
    event,
  })
}

// ============================================================================
// Cell Style Helpers
// ============================================================================

const getCellStyle = (column: ComputedColumnInfo, rowIndex: number) => {
  const style: Record<string, any> = {
    width: `${column.computedWidth}px`,
    minWidth: `${column.computedWidth}px`,
    textAlign: column.align ?? 'left',
  }

  // Frozen column positioning
  if (column.frozen && column.frozenOffset !== undefined) {
    style.position = 'sticky'
    if (column.frozenPosition === 'left') {
      style.left = `${column.frozenOffset}px`
    } else {
      style.right = `${column.frozenOffset}px`
    }
    style.zIndex = 2
  }

  return style
}

const getHeaderCellStyle = (column: ComputedColumnInfo) => {
  const style = getCellStyle(column, -1)
  
  // Header has higher z-index for frozen columns
  if (column.frozen) {
    style.zIndex = 3
  }

  return style
}

const getRowStyle = (rowIndex: number) => {
  const style: Record<string, any> = {
    height: `${rowHeight.value}px`,
  }

  // Apply zebra stripe colors
  if (normalizedZebraConfig.value.enabled) {
    const interval = normalizedZebraConfig.value.stripeInterval ?? 1
    const isEven = Math.floor(rowIndex / interval) % 2 === 0
    style.backgroundColor = isEven 
      ? normalizedZebraConfig.value.evenRowColor 
      : normalizedZebraConfig.value.oddRowColor
  }

  return style
}

// ============================================================================
// CSS Classes
// ============================================================================

const tableClasses = computed(() => [
  'enhanced-table',
  `enhanced-table--${props.size}`,
  {
    'enhanced-table--bordered': props.bordered,
    'enhanced-table--loading': props.loading,
    'enhanced-table--has-frozen-left': frozenLeftColumns.value.length > 0,
    'enhanced-table--has-frozen-right': frozenRightColumns.value.length > 0,
    'enhanced-table--scrolled-left': !scrollState.value.isScrolledLeft,
    'enhanced-table--scrolled-right': !scrollState.value.isScrolledRight,
  },
])

const getCellClasses = (column: ComputedColumnInfo, rowIndex: number) => [
  'enhanced-table__cell',
  {
    'enhanced-table__cell--frozen': column.frozen,
    'enhanced-table__cell--frozen-left': column.frozenPosition === 'left',
    'enhanced-table__cell--frozen-right': column.frozenPosition === 'right',
    'enhanced-table__cell--ellipsis': column.ellipsis,
    'enhanced-table__cell--clickable': getCellClickAction(column.key) !== undefined,
  },
  column.className,
]

const getHeaderCellClasses = (column: ComputedColumnInfo) => [
  'enhanced-table__header-cell',
  {
    'enhanced-table__header-cell--frozen': column.frozen,
    'enhanced-table__header-cell--frozen-left': column.frozenPosition === 'left',
    'enhanced-table__header-cell--frozen-right': column.frozenPosition === 'right',
    'enhanced-table__header-cell--sortable': isSortable(column),
    'enhanced-table__header-cell--sorted': getSortDirection(column.key) !== null,
    'enhanced-table__header-cell--filterable': isFilterable(column),
    'enhanced-table__header-cell--filtered': getFilterValue(column.key) !== null,
    'enhanced-table__header-cell--resizable': column.resizable,
    'enhanced-table__header-cell--dragging': draggingColumn.value === column.key,
    'enhanced-table__header-cell--drag-over': dragOverColumn.value === column.key,
  },
  column.headerClassName,
]

// ============================================================================
// Sort Icon Helper
// ============================================================================

const getSortIcon = (column: ComputedColumnInfo): string => {
  const direction = getSortDirection(column.key)
  if (direction === 'asc') return '↑'
  if (direction === 'desc') return '↓'
  return '↕'
}

// ============================================================================
// Lifecycle
// ============================================================================

onMounted(() => {
  // Calculate column widths after mount
  nextTick(() => {
    calculateColumnWidths(tableContainerRef.value ?? undefined)
  })
  
  // Add document click listener for filter dropdown
  document.addEventListener('click', handleDocumentClick)
})

onUnmounted(() => {
  document.removeEventListener('mousemove', handleResizeMove)
  document.removeEventListener('mouseup', handleResizeEnd)
  document.removeEventListener('click', handleDocumentClick)
})

// ============================================================================
// Expose
// ============================================================================

defineExpose({
  calculateColumnWidths,
  scrollState,
  updateScrollState,
  // Sort
  sortConfig,
  handleSort,
  clearSort,
  // Filter
  filterState,
  setFilter,
  clearFilter,
  clearAllFilters,
  // Column order
  columnOrder,
})
</script>

<template>
  <div
    ref="tableContainerRef"
    :class="tableClasses"
    :style="tableContainerStyle"
    @scroll="handleScroll"
  >
    <!-- Loading Overlay -->
    <div v-if="loading" class="enhanced-table__loading">
      <div class="enhanced-table__loading-spinner" />
    </div>

    <!-- Tooltip - 14.6.29 -->
    <div
      v-if="tooltipState.visible"
      class="enhanced-table__tooltip"
      :style="{
        left: `${tooltipState.position.x}px`,
        top: `${tooltipState.position.y}px`,
      }"
    >
      {{ tooltipState.content }}
    </div>

    <!-- Table -->
    <table :style="tableStyle" class="enhanced-table__table">
      <!-- Header -->
      <thead 
        class="enhanced-table__header"
        :class="{ 'enhanced-table__header--sticky': freezeConfig?.freezeHeader !== false }"
      >
        <tr class="enhanced-table__header-row">
          <th
            v-for="(column, colIndex) in computedColumns"
            :key="column.key"
            :class="getHeaderCellClasses(column)"
            :style="getHeaderCellStyle(column)"
            :draggable="interactionConfig?.columnDrag?.reorderable && !interactionConfig?.columnDrag?.nonReorderableColumns?.includes(column.key)"
            @click="handleHeaderClick(column)"
            @dragstart="onDragStart(column, $event)"
            @dragover="onDragOver(column, $event)"
            @dragend="handleDragEnd"
            @drop="onDrop(column, $event)"
          >
            <div class="enhanced-table__header-cell-content">
              <span class="enhanced-table__header-cell-title">{{ column.title }}</span>
              
              <!-- Sort Indicator - 14.6.30 -->
              <span
                v-if="isSortable(column)"
                class="enhanced-table__sort-indicator"
                :class="{ 'enhanced-table__sort-indicator--active': getSortDirection(column.key) !== null }"
              >
                {{ getSortIcon(column) }}
              </span>
              
              <!-- Filter Button - 14.6.31 -->
              <button
                v-if="isFilterable(column)"
                class="enhanced-table__filter-button"
                :class="{ 'enhanced-table__filter-button--active': getFilterValue(column.key) !== null }"
                @click.stop="toggleFilterDropdown(column.key, $event)"
              >
                <span class="enhanced-table__filter-icon">▼</span>
              </button>
              
              <!-- Filter Dropdown - 14.6.31 -->
              <div
                v-if="activeFilterColumn === column.key"
                ref="filterDropdownRef"
                class="enhanced-table__filter-dropdown"
                @click.stop
              >
                <input
                  v-model="filterInputValue"
                  type="text"
                  class="enhanced-table__filter-input"
                  placeholder="输入筛选值..."
                  @keyup.enter="applyFilter(column.key)"
                />
                <div class="enhanced-table__filter-actions">
                  <button
                    class="enhanced-table__filter-apply"
                    @click="applyFilter(column.key)"
                  >
                    应用
                  </button>
                  <button
                    class="enhanced-table__filter-clear"
                    @click="handleFilterClear(column.key)"
                  >
                    清除
                  </button>
                </div>
              </div>
              
              <!-- Resize Handle - 14.6.32 -->
              <div
                v-if="column.resizable"
                class="enhanced-table__resize-handle"
                @mousedown.stop="onResizeStart(column, $event)"
              />
            </div>
          </th>
        </tr>
      </thead>

      <!-- Body -->
      <tbody ref="tableBodyRef" class="enhanced-table__body">
        <!-- Empty State -->
        <tr v-if="displayData.length === 0" class="enhanced-table__empty-row">
          <td :colspan="computedColumns.length" class="enhanced-table__empty-cell">
            <slot name="empty">
              <div class="enhanced-table__empty-content">
                {{ emptyText }}
              </div>
            </slot>
          </td>
        </tr>

        <!-- Data Rows -->
        <template v-else>
          <tr
            v-for="(row, rowIndex) in displayData"
            :key="getRowKey(row, rowIndex)"
            :class="['enhanced-table__row', getZebraClass(rowIndex)]"
            :style="getRowStyle(rowIndex)"
          >
            <template v-for="(column, colIndex) in computedColumns" :key="column.key">
              <!-- Skip hidden cells (merged into another cell) -->
              <td
                v-if="!isCellHidden(rowIndex, column.key)"
                :class="getCellClasses(column, rowIndex)"
                :style="getCellStyle(column, rowIndex)"
                :rowspan="getCellMergeSpan(rowIndex, column.key).rowspan || undefined"
                :colspan="getCellMergeSpan(rowIndex, column.key).colspan || undefined"
                @click="handleCellClick(row, column, rowIndex, colIndex, $event)"
                @mouseenter="handleCellMouseEnter($event, row, column, rowIndex)"
                @mouseleave="handleCellMouseLeave"
              >
                <div class="enhanced-table__cell-content">
                  <!-- Custom Render -->
                  <template v-if="column.render">
                    <component
                      :is="{ render: () => column.render!(row[column.dataIndex], row, rowIndex) }"
                    />
                  </template>
                  <!-- Default Render -->
                  <template v-else>
                    {{ row[column.dataIndex] }}
                  </template>
                </div>
              </td>
            </template>
          </tr>
        </template>
      </tbody>
    </table>

    <!-- Frozen Column Shadows -->
    <div
      v-if="frozenLeftColumns.length > 0"
      class="enhanced-table__frozen-shadow enhanced-table__frozen-shadow--left"
      :style="{ left: `${leftFrozenWidth}px` }"
    />
    <div
      v-if="frozenRightColumns.length > 0"
      class="enhanced-table__frozen-shadow enhanced-table__frozen-shadow--right"
      :style="{ right: `${rightFrozenWidth}px` }"
    />
  </div>
</template>

<style scoped>
.enhanced-table {
  position: relative;
  overflow: auto;
  background-color: var(--table-bg, #fff);
  border-radius: 4px;
}

.enhanced-table--bordered {
  border: 1px solid var(--table-border-color, #e8e8e8);
}

.enhanced-table__table {
  border-collapse: separate;
  border-spacing: 0;
  table-layout: fixed;
}

/* Header */
.enhanced-table__header {
  background-color: var(--table-header-bg, #fafafa);
}

.enhanced-table__header--sticky {
  position: sticky;
  top: 0;
  z-index: 4;
}

.enhanced-table__header-row {
  height: 48px;
}

.enhanced-table__header-cell {
  padding: 12px 16px;
  font-weight: 600;
  color: var(--table-header-color, #262626);
  text-align: left;
  background-color: var(--table-header-bg, #fafafa);
  border-bottom: 1px solid var(--table-border-color, #e8e8e8);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.enhanced-table--bordered .enhanced-table__header-cell {
  border-right: 1px solid var(--table-border-color, #e8e8e8);
}

.enhanced-table--bordered .enhanced-table__header-cell:last-child {
  border-right: none;
}

.enhanced-table__header-cell--frozen {
  background-color: var(--table-header-bg, #fafafa);
}

.enhanced-table__header-cell-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
}

.enhanced-table__header-cell-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Resize Handle */
.enhanced-table__resize-handle {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 8px;
  cursor: col-resize;
  user-select: none;
}

.enhanced-table__resize-handle:hover {
  background-color: rgba(0, 0, 0, 0.06);
}

.enhanced-table__header-cell--resizable {
  position: relative;
}

/* Body */
.enhanced-table__body {
  background-color: var(--table-body-bg, #fff);
}

.enhanced-table__row {
  transition: background-color 0.2s;
}

.enhanced-table__row:hover {
  background-color: var(--table-hover-bg, #f5f5f5) !important;
}

/* Zebra Stripes */
.enhanced-table-row--even {
  background-color: var(--table-even-row-bg, #fafafa);
}

.enhanced-table-row--odd {
  background-color: var(--table-odd-row-bg, #fff);
}

/* Cells */
.enhanced-table__cell {
  padding: 12px 16px;
  color: var(--table-cell-color, #262626);
  border-bottom: 1px solid var(--table-border-color, #e8e8e8);
  vertical-align: middle;
  background-color: inherit;
}

.enhanced-table--bordered .enhanced-table__cell {
  border-right: 1px solid var(--table-border-color, #e8e8e8);
}

.enhanced-table--bordered .enhanced-table__cell:last-child {
  border-right: none;
}

.enhanced-table__cell--frozen {
  background-color: inherit;
}

.enhanced-table__cell--ellipsis .enhanced-table__cell-content {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.enhanced-table__cell-content {
  display: flex;
  align-items: center;
}

/* Frozen Column Shadows */
.enhanced-table__frozen-shadow {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 10px;
  pointer-events: none;
  transition: box-shadow 0.3s;
}

.enhanced-table__frozen-shadow--left {
  box-shadow: none;
}

.enhanced-table__frozen-shadow--right {
  box-shadow: none;
}

.enhanced-table--scrolled-left .enhanced-table__frozen-shadow--left {
  box-shadow: inset 10px 0 8px -8px rgba(0, 0, 0, 0.15);
}

.enhanced-table--scrolled-right .enhanced-table__frozen-shadow--right {
  box-shadow: inset -10px 0 8px -8px rgba(0, 0, 0, 0.15);
}

/* Empty State */
.enhanced-table__empty-row {
  height: 100px;
}

.enhanced-table__empty-cell {
  text-align: center;
  color: var(--table-empty-color, #999);
  padding: 32px;
}

.enhanced-table__empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

/* Loading */
.enhanced-table__loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.enhanced-table__loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #1890ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Size Variants */
.enhanced-table--small .enhanced-table__header-row {
  height: 36px;
}

.enhanced-table--small .enhanced-table__header-cell,
.enhanced-table--small .enhanced-table__cell {
  padding: 8px 12px;
  font-size: 13px;
}

.enhanced-table--large .enhanced-table__header-row {
  height: 56px;
}

.enhanced-table--large .enhanced-table__header-cell,
.enhanced-table--large .enhanced-table__cell {
  padding: 16px 20px;
  font-size: 15px;
}

/* ============================================================================
 * Interaction Styles - 14.6.28, 14.6.29, 14.6.30, 14.6.31, 14.6.32, 14.6.33
 * ============================================================================ */

/* Clickable Cells - 14.6.28 */
.enhanced-table__cell--clickable {
  cursor: pointer;
}

.enhanced-table__cell--clickable:hover {
  background-color: var(--table-cell-hover-bg, #e6f7ff) !important;
}

/* Tooltip - 14.6.29 */
.enhanced-table__tooltip {
  position: fixed;
  z-index: 1000;
  padding: 8px 12px;
  background-color: rgba(0, 0, 0, 0.85);
  color: #fff;
  font-size: 13px;
  border-radius: 4px;
  max-width: 300px;
  word-wrap: break-word;
  transform: translate(-50%, -100%);
  margin-top: -8px;
  pointer-events: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.enhanced-table__tooltip::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border: 6px solid transparent;
  border-top-color: rgba(0, 0, 0, 0.85);
}

/* Sort Indicator - 14.6.30 */
.enhanced-table__header-cell--sortable {
  cursor: pointer;
  user-select: none;
}

.enhanced-table__header-cell--sortable:hover {
  background-color: var(--table-header-hover-bg, #f0f0f0);
}

.enhanced-table__sort-indicator {
  margin-left: 4px;
  font-size: 12px;
  color: var(--table-sort-color, #bfbfbf);
  transition: color 0.2s;
}

.enhanced-table__sort-indicator--active {
  color: var(--table-sort-active-color, #1890ff);
}

.enhanced-table__header-cell--sorted {
  background-color: var(--table-header-sorted-bg, #f0f5ff);
}

/* Filter - 14.6.31 */
.enhanced-table__filter-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  margin-left: 4px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 2px;
  transition: background-color 0.2s;
}

.enhanced-table__filter-button:hover {
  background-color: rgba(0, 0, 0, 0.06);
}

.enhanced-table__filter-button--active {
  color: var(--table-filter-active-color, #1890ff);
}

.enhanced-table__filter-icon {
  font-size: 10px;
  color: inherit;
}

.enhanced-table__filter-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  z-index: 100;
  min-width: 180px;
  padding: 12px;
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 3px 6px -4px rgba(0, 0, 0, 0.12), 0 6px 16px 0 rgba(0, 0, 0, 0.08), 0 9px 28px 8px rgba(0, 0, 0, 0.05);
}

.enhanced-table__filter-input {
  width: 100%;
  padding: 6px 8px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s;
}

.enhanced-table__filter-input:focus {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.enhanced-table__filter-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.enhanced-table__filter-apply,
.enhanced-table__filter-clear {
  flex: 1;
  padding: 4px 8px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.enhanced-table__filter-apply {
  background-color: #1890ff;
  border-color: #1890ff;
  color: #fff;
}

.enhanced-table__filter-apply:hover {
  background-color: #40a9ff;
  border-color: #40a9ff;
}

.enhanced-table__filter-clear {
  background-color: #fff;
  color: #595959;
}

.enhanced-table__filter-clear:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.enhanced-table__header-cell--filtered {
  background-color: var(--table-header-filtered-bg, #e6f7ff);
}

/* Column Resize - 14.6.32 */
.enhanced-table__resize-handle {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 8px;
  cursor: col-resize;
  user-select: none;
  z-index: 1;
}

.enhanced-table__resize-handle:hover,
.enhanced-table__resize-handle:active {
  background-color: var(--table-resize-handle-bg, rgba(24, 144, 255, 0.3));
}

/* Column Reorder - 14.6.33 */
.enhanced-table__header-cell--dragging {
  opacity: 0.5;
  background-color: var(--table-drag-bg, #f0f5ff);
}

.enhanced-table__header-cell--drag-over {
  border-left: 2px solid var(--table-drag-indicator-color, #1890ff);
}

.enhanced-table__header-cell[draggable="true"] {
  cursor: grab;
}

.enhanced-table__header-cell[draggable="true"]:active {
  cursor: grabbing;
}
</style>
