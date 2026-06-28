<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 数据透视表组件
 * Pivot Table Component
 * 
 * 支持行列维度拖拽配置、多级分组汇总、小计和总计位置配置
 * Supports row/column dimension drag-drop configuration, multi-level grouping, subtotals and totals positioning
 * 
 * 需求: 22.3.7, 22.3.8, 22.3.9
 */

import { ref, computed, watch, onMounted, nextTick } from 'vue'
import PivotConfig from './PivotConfig.vue'
import type {
  DimensionField,
  PivotConfig as PivotConfigType,
  TotalConfig,
  PivotData,
  PivotRowHeader,
  PivotColumnHeader,
  PivotCell,
  PivotCellClickEvent,
  RowExpandEvent,
  DrillDownEvent,
  ConfigChangeEvent,
} from './types'
import {
  DEFAULT_TOTAL_CONFIG,
  DEFAULT_PIVOT_CONFIG,
  defaultCellKeyGenerator,
} from './types'
import { calculatePivotData } from './pivotUtils'

// ============================================================================
// Props
// ============================================================================

interface Props {
  /** Raw data array */
  data: Record<string, any>[]
  /** Available fields for pivot */
  availableFields: DimensionField[]
  /** Initial pivot configuration */
  config?: PivotConfigType
  /** Total configuration */
  totalConfig?: TotalConfig
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
  /** Enable row expand/collapse */
  expandable?: boolean
  /** Default expanded row paths */
  defaultExpandedPaths?: any[][]
  /** Enable cell click for drill-down */
  drillDownEnabled?: boolean
  /** Show configuration panel */
  showConfigPanel?: boolean
  /** Configuration panel position */
  configPanelPosition?: 'left' | 'right' | 'top'
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  emptyText: '暂无数据',
  bordered: true,
  size: 'medium',
  expandable: true,
  drillDownEnabled: false,
  showConfigPanel: true,
  configPanelPosition: 'left',
})

// ============================================================================
// Emits
// ============================================================================

const emit = defineEmits<{
  (e: 'update:config', config: PivotConfigType): void
  (e: 'update:totalConfig', config: TotalConfig): void
  (e: 'cell-click', event: PivotCellClickEvent): void
  (e: 'row-expand', event: RowExpandEvent): void
  (e: 'drill-down', event: DrillDownEvent): void
  (e: 'config-change', event: ConfigChangeEvent): void
}>()

// ============================================================================
// Refs
// ============================================================================

const tableContainerRef = ref<HTMLElement | null>(null)

// ============================================================================
// State
// ============================================================================

const internalConfig = ref<PivotConfigType>({ ...DEFAULT_PIVOT_CONFIG, ...props.config })
const internalTotalConfig = ref<TotalConfig>({ ...DEFAULT_TOTAL_CONFIG, ...props.totalConfig })
const expandedPaths = ref<Set<string>>(new Set())
const configPanelCollapsed = ref(false)

// ============================================================================
// Computed
// ============================================================================

/**
 * Effective pivot configuration
 */
const effectiveConfig = computed<PivotConfigType>(() => {
  return props.config ?? internalConfig.value
})

/**
 * Effective total configuration
 */
const effectiveTotalConfig = computed<TotalConfig>(() => {
  return { ...DEFAULT_TOTAL_CONFIG, ...props.totalConfig, ...internalTotalConfig.value }
})

/**
 * Calculated pivot data
 * 计算后的透视数据
 * 
 * Validates: 22.3.8 - 支持多级分组汇总
 */
const pivotData = computed<PivotData>(() => {
  return calculatePivotData(
    props.data,
    effectiveConfig.value,
    effectiveTotalConfig.value
  )
})

/**
 * Visible row headers (respecting expand state)
 */
const visibleRowHeaders = computed<PivotRowHeader[]>(() => {
  if (!props.expandable) {
    return pivotData.value.flattenedRowHeaders
  }

  const result: PivotRowHeader[] = []
  
  const processHeaders = (headers: PivotRowHeader[], parentExpanded: boolean) => {
    for (const header of headers) {
      if (!parentExpanded) continue
      
      result.push(header)
      
      if (header.children.length > 0) {
        const pathKey = header.path.join('|')
        const isExpanded = expandedPaths.value.has(pathKey) || header.isGrandTotal || header.isSubtotal
        processHeaders(header.children as PivotRowHeader[], isExpanded)
      }
    }
  }

  processHeaders(pivotData.value.rowHeaders, true)
  return result
})

/**
 * Column header levels for rendering
 */
const columnHeaderLevels = computed<PivotColumnHeader[][]>(() => {
  const levels: PivotColumnHeader[][] = []
  const maxDepth = Math.max(
    ...pivotData.value.flattenedColumnHeaders.map(h => h.depth),
    0
  )

  for (let depth = 0; depth <= maxDepth; depth++) {
    const levelHeaders: PivotColumnHeader[] = []
    
    const collectAtDepth = (headers: PivotColumnHeader[]) => {
      for (const header of headers) {
        if (header.depth === depth) {
          levelHeaders.push(header)
        }
        if (header.children.length > 0) {
          collectAtDepth(header.children as PivotColumnHeader[])
        }
      }
    }

    collectAtDepth(pivotData.value.columnHeaders)
    levels.push(levelHeaders)
  }

  // Add value field headers if multiple values
  if (effectiveConfig.value.values.length > 1) {
    const valueHeaders: PivotColumnHeader[] = []
    for (const colHeader of pivotData.value.flattenedColumnHeaders) {
      for (const valueField of effectiveConfig.value.values) {
        valueHeaders.push({
          value: valueField.id,
          label: valueField.label,
          depth: maxDepth + 1,
          parent: colHeader,
          children: [],
          isSubtotal: colHeader.isSubtotal,
          isGrandTotal: colHeader.isGrandTotal,
          colSpan: 1,
          path: [...colHeader.path, valueField.id],
        })
      }
    }
    levels.push(valueHeaders)
  }

  return levels
})

/**
 * Row dimension count for header columns
 */
const rowDimensionCount = computed(() => {
  return Math.max(effectiveConfig.value.rows.length, 1)
})

const tableContainerStyle = computed(() => ({
  height: typeof props.height === 'number' ? `${props.height}px` : props.height,
  maxHeight: typeof props.maxHeight === 'number' ? `${props.maxHeight}px` : props.maxHeight,
}))

const tableClasses = computed(() => [
  'pivot-table',
  `pivot-table--${props.size}`,
  {
    'pivot-table--bordered': props.bordered,
    'pivot-table--loading': props.loading,
  },
])

const wrapperClasses = computed(() => [
  'pivot-table-wrapper',
  `pivot-table-wrapper--config-${props.configPanelPosition}`,
])

const rowHeight = computed(() => {
  switch (props.size) {
    case 'small': return 32
    case 'large': return 48
    default: return 40
  }
})

// ============================================================================
// Methods
// ============================================================================

/**
 * Get cell data for a specific position
 * 获取特定位置的单元格数据
 */
const getCell = (rowHeader: PivotRowHeader, colHeader: PivotColumnHeader, valueFieldId: string): PivotCell | undefined => {
  const key = defaultCellKeyGenerator(rowHeader.path, colHeader.path, valueFieldId)
  return pivotData.value.cells.get(key)
}

/**
 * Handle row expand/collapse
 * 处理行展开/折叠
 */
const handleRowExpand = (rowHeader: PivotRowHeader) => {
  if (!props.expandable || rowHeader.children.length === 0) return

  const pathKey = rowHeader.path.join('|')
  const isExpanded = expandedPaths.value.has(pathKey)

  if (isExpanded) {
    expandedPaths.value.delete(pathKey)
  } else {
    expandedPaths.value.add(pathKey)
  }

  emit('row-expand', {
    rowHeader,
    expanded: !isExpanded,
    expandedPaths: Array.from(expandedPaths.value).map(p => p.split('|')),
  })
}

/**
 * Check if row is expanded
 */
const isRowExpanded = (rowHeader: PivotRowHeader): boolean => {
  if (rowHeader.isGrandTotal || rowHeader.isSubtotal) return true
  const pathKey = rowHeader.path.join('|')
  return expandedPaths.value.has(pathKey)
}

/**
 * Handle cell click
 * 处理单元格点击
 */
const handleCellClick = (
  cell: PivotCell | undefined,
  rowHeader: PivotRowHeader,
  colHeader: PivotColumnHeader,
  event: MouseEvent
) => {
  if (!cell) return

  emit('cell-click', {
    cell,
    rowHeader,
    columnHeader: colHeader,
    event,
  })

  if (props.drillDownEnabled && cell.rawValues) {
    emit('drill-down', {
      cell,
      records: cell.rawValues as any,
      rowPath: cell.rowPath,
      columnPath: cell.columnPath,
    })
  }
}

/**
 * Handle config update
 * 处理配置更新
 */
const handleConfigUpdate = (newConfig: PivotConfigType) => {
  internalConfig.value = newConfig
  emit('update:config', newConfig)
  emit('config-change', { config: newConfig, changedZone: 'rows' })
}

/**
 * Handle total config update
 * 处理总计配置更新
 * 
 * Validates: 22.3.9 - 支持小计和总计显示位置配置
 */
const handleTotalConfigUpdate = (newConfig: TotalConfig) => {
  internalTotalConfig.value = newConfig
  emit('update:totalConfig', newConfig)
}

/**
 * Expand all rows
 * 展开所有行
 */
const expandAll = () => {
  const collectPaths = (headers: PivotRowHeader[]) => {
    for (const header of headers) {
      if (header.children.length > 0) {
        expandedPaths.value.add(header.path.join('|'))
        collectPaths(header.children as PivotRowHeader[])
      }
    }
  }
  collectPaths(pivotData.value.rowHeaders)
}

/**
 * Collapse all rows
 * 折叠所有行
 */
const collapseAll = () => {
  expandedPaths.value.clear()
}

/**
 * Get row header cell classes
 */
const getRowHeaderCellClasses = (header: PivotRowHeader) => [
  'pivot-table__row-header-cell',
  {
    'pivot-table__row-header-cell--subtotal': header.isSubtotal,
    'pivot-table__row-header-cell--grandtotal': header.isGrandTotal,
    'pivot-table__row-header-cell--expandable': header.children.length > 0,
    'pivot-table__row-header-cell--expanded': isRowExpanded(header),
  },
]

/**
 * Get data cell classes
 */
const getDataCellClasses = (cell: PivotCell | undefined) => [
  'pivot-table__data-cell',
  {
    'pivot-table__data-cell--subtotal': cell?.isSubtotal,
    'pivot-table__data-cell--grandtotal': cell?.isGrandTotal,
    'pivot-table__data-cell--clickable': props.drillDownEnabled,
  },
]

// ============================================================================
// Initialization
// ============================================================================

const initializeExpandedState = () => {
  if (props.defaultExpandedPaths) {
    for (const path of props.defaultExpandedPaths) {
      expandedPaths.value.add(path.join('|'))
    }
  } else {
    // Default: expand first level
    for (const header of pivotData.value.rowHeaders) {
      if (header.children.length > 0 && !header.isGrandTotal && !header.isSubtotal) {
        expandedPaths.value.add(header.path.join('|'))
      }
    }
  }
}

// ============================================================================
// Watchers
// ============================================================================

watch(() => props.config, (newConfig) => {
  if (newConfig) {
    internalConfig.value = { ...DEFAULT_PIVOT_CONFIG, ...newConfig }
  }
}, { deep: true })

watch(() => props.totalConfig, (newConfig) => {
  if (newConfig) {
    internalTotalConfig.value = { ...DEFAULT_TOTAL_CONFIG, ...newConfig }
  }
}, { deep: true })

// ============================================================================
// Lifecycle
// ============================================================================

onMounted(() => {
  initializeExpandedState()
})

// ============================================================================
// Expose
// ============================================================================

defineExpose({
  expandAll,
  collapseAll,
  pivotData,
  visibleRowHeaders,
})
</script>

<template>
  <div :class="wrapperClasses">
    <!-- Configuration Panel -->
    <PivotConfig
      v-if="showConfigPanel"
      :available-fields="availableFields"
      :config="effectiveConfig"
      :total-config="effectiveTotalConfig"
      :position="configPanelPosition"
      :collapsed="configPanelCollapsed"
      @update:config="handleConfigUpdate"
      @update:total-config="handleTotalConfigUpdate"
      @update:collapsed="configPanelCollapsed = $event"
    />

    <!-- Pivot Table -->
    <div
      ref="tableContainerRef"
      :class="tableClasses"
      :style="tableContainerStyle"
      role="grid"
      :aria-busy="loading"
    >
      <!-- Loading Overlay -->
      <div v-if="loading" class="pivot-table__loading">
        <div class="pivot-table__loading-spinner" />
      </div>

      <!-- Empty State -->
      <div
        v-if="!loading && (effectiveConfig.values.length === 0 || data.length === 0)"
        class="pivot-table__empty"
      >
        <slot name="empty">
          <div class="pivot-table__empty-content">
            {{ effectiveConfig.values.length === 0 ? '请添加值字段' : emptyText }}
          </div>
        </slot>
      </div>

      <!-- Table -->
      <table
        v-else
        class="pivot-table__table"
      >
        <!-- Column Headers -->
        <thead class="pivot-table__header">
          <!-- Row dimension headers + Column headers -->
          <tr
            v-for="(levelHeaders, levelIndex) in columnHeaderLevels"
            :key="`level-${levelIndex}`"
            class="pivot-table__header-row"
            :style="{ height: `${rowHeight}px` }"
          >
            <!-- Row dimension header cells (only on first level) -->
            <th
              v-for="(rowDim, dimIndex) in effectiveConfig.rows"
              v-if="levelIndex === 0"
              :key="`row-dim-${dimIndex}`"
              class="pivot-table__corner-cell"
              :rowspan="columnHeaderLevels.length"
            >
              {{ rowDim.label }}
            </th>
            <!-- Empty corner cell if no row dimensions -->
            <th
              v-if="levelIndex === 0 && effectiveConfig.rows.length === 0"
              class="pivot-table__corner-cell"
              :rowspan="columnHeaderLevels.length"
            >
              &nbsp;
            </th>

            <!-- Column header cells -->
            <th
              v-for="colHeader in levelHeaders"
              :key="`col-${colHeader.path.join('-')}`"
              class="pivot-table__column-header-cell"
              :class="{
                'pivot-table__column-header-cell--subtotal': colHeader.isSubtotal,
                'pivot-table__column-header-cell--grandtotal': colHeader.isGrandTotal,
              }"
              :colspan="colHeader.colSpan * (levelIndex < columnHeaderLevels.length - 1 ? effectiveConfig.values.length : 1)"
            >
              {{ colHeader.label }}
            </th>
          </tr>
        </thead>

        <!-- Data Rows -->
        <tbody class="pivot-table__body">
          <tr
            v-for="(rowHeader, rowIndex) in visibleRowHeaders"
            :key="`row-${rowHeader.path.join('-')}`"
            class="pivot-table__data-row"
            :class="{
              'pivot-table__data-row--subtotal': rowHeader.isSubtotal,
              'pivot-table__data-row--grandtotal': rowHeader.isGrandTotal,
            }"
            :style="{ height: `${rowHeight}px` }"
          >
            <!-- Row header cells -->
            <td
              v-for="(rowDim, dimIndex) in effectiveConfig.rows"
              :key="`row-header-${dimIndex}`"
              :class="getRowHeaderCellClasses(rowHeader)"
              :style="{
                paddingLeft: dimIndex === 0 ? `${rowHeader.depth * 20 + 8}px` : undefined,
              }"
              @click="dimIndex === 0 && handleRowExpand(rowHeader)"
            >
              <template v-if="dimIndex === rowHeader.depth || rowHeader.isGrandTotal || rowHeader.isSubtotal">
                <!-- Expand icon -->
                <span
                  v-if="dimIndex === 0 && rowHeader.children.length > 0 && !rowHeader.isGrandTotal && !rowHeader.isSubtotal"
                  class="pivot-table__expand-icon"
                >
                  {{ isRowExpanded(rowHeader) ? '▼' : '▶' }}
                </span>
                <!-- Label -->
                <span class="pivot-table__row-label">
                  {{ rowHeader.label }}
                </span>
              </template>
            </td>
            <!-- Empty row header if no row dimensions -->
            <td
              v-if="effectiveConfig.rows.length === 0"
              :class="getRowHeaderCellClasses(rowHeader)"
            >
              {{ rowHeader.label }}
            </td>

            <!-- Data cells -->
            <template v-for="colHeader in pivotData.flattenedColumnHeaders" :key="`col-${colHeader.path.join('-')}`">
              <td
                v-for="valueField in effectiveConfig.values"
                :key="`cell-${valueField.id}`"
                :class="getDataCellClasses(getCell(rowHeader, colHeader, valueField.id))"
                @click="handleCellClick(getCell(rowHeader, colHeader, valueField.id), rowHeader, colHeader, $event)"
              >
                {{ getCell(rowHeader, colHeader, valueField.id)?.formattedValue ?? '-' }}
              </td>
            </template>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.pivot-table-wrapper {
  display: flex;
  gap: 16px;
  width: 100%;
}

.pivot-table-wrapper--config-left {
  flex-direction: row;
}

.pivot-table-wrapper--config-right {
  flex-direction: row-reverse;
}

.pivot-table-wrapper--config-top {
  flex-direction: column;
}

.pivot-table {
  flex: 1;
  position: relative;
  overflow: auto;
  background-color: var(--pivot-table-bg, #fff);
  border-radius: 4px;
}

.pivot-table--bordered {
  border: 1px solid var(--pivot-table-border, #e8e8e8);
}

.pivot-table__table {
  width: 100%;
  border-collapse: collapse;
  table-layout: auto;
}

/* Header */
.pivot-table__header {
  position: sticky;
  top: 0;
  z-index: 2;
  background-color: var(--pivot-table-header-bg, #fafafa);
}

.pivot-table__header-row {
  height: 40px;
}

.pivot-table__corner-cell {
  padding: 8px 12px;
  font-weight: 600;
  color: var(--pivot-table-header-color, #262626);
  text-align: left;
  background-color: var(--pivot-table-header-bg, #fafafa);
  border: 1px solid var(--pivot-table-border, #e8e8e8);
  white-space: nowrap;
  position: sticky;
  left: 0;
  z-index: 3;
}

.pivot-table__column-header-cell {
  padding: 8px 12px;
  font-weight: 600;
  color: var(--pivot-table-header-color, #262626);
  text-align: center;
  background-color: var(--pivot-table-header-bg, #fafafa);
  border: 1px solid var(--pivot-table-border, #e8e8e8);
  white-space: nowrap;
}

.pivot-table__column-header-cell--subtotal {
  background-color: var(--pivot-table-subtotal-bg, #f0f5ff);
  font-style: italic;
}

.pivot-table__column-header-cell--grandtotal {
  background-color: var(--pivot-table-grandtotal-bg, #e6f7ff);
  font-weight: 700;
}

/* Body */
.pivot-table__body {
  background-color: var(--pivot-table-body-bg, #fff);
}

.pivot-table__data-row {
  height: 40px;
}

.pivot-table__data-row:hover {
  background-color: var(--pivot-table-hover-bg, #f5f5f5);
}

.pivot-table__data-row--subtotal {
  background-color: var(--pivot-table-subtotal-bg, #f0f5ff);
}

.pivot-table__data-row--subtotal:hover {
  background-color: var(--pivot-table-subtotal-hover-bg, #e6f0ff);
}

.pivot-table__data-row--grandtotal {
  background-color: var(--pivot-table-grandtotal-bg, #e6f7ff);
  font-weight: 600;
}

.pivot-table__data-row--grandtotal:hover {
  background-color: var(--pivot-table-grandtotal-hover-bg, #d6efff);
}

/* Row Header Cells */
.pivot-table__row-header-cell {
  padding: 8px 12px;
  font-weight: 500;
  color: var(--pivot-table-row-header-color, #262626);
  text-align: left;
  background-color: var(--pivot-table-row-header-bg, #fafafa);
  border: 1px solid var(--pivot-table-border, #e8e8e8);
  white-space: nowrap;
  position: sticky;
  left: 0;
  z-index: 1;
}

.pivot-table__row-header-cell--expandable {
  cursor: pointer;
}

.pivot-table__row-header-cell--expandable:hover {
  background-color: var(--pivot-table-row-header-hover-bg, #f0f0f0);
}

.pivot-table__row-header-cell--subtotal {
  background-color: var(--pivot-table-subtotal-bg, #f0f5ff);
  font-style: italic;
}

.pivot-table__row-header-cell--grandtotal {
  background-color: var(--pivot-table-grandtotal-bg, #e6f7ff);
  font-weight: 700;
}

.pivot-table__expand-icon {
  display: inline-block;
  width: 16px;
  font-size: 10px;
  color: var(--pivot-table-expand-icon, #666);
  transition: transform 0.15s ease;
}

.pivot-table__row-label {
  vertical-align: middle;
}

/* Data Cells */
.pivot-table__data-cell {
  padding: 8px 12px;
  text-align: right;
  border: 1px solid var(--pivot-table-border, #e8e8e8);
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.pivot-table__data-cell--subtotal {
  background-color: var(--pivot-table-subtotal-bg, #f0f5ff);
  font-style: italic;
}

.pivot-table__data-cell--grandtotal {
  background-color: var(--pivot-table-grandtotal-bg, #e6f7ff);
  font-weight: 600;
}

.pivot-table__data-cell--clickable {
  cursor: pointer;
}

.pivot-table__data-cell--clickable:hover {
  background-color: var(--pivot-table-cell-hover-bg, #e6f7ff);
}

/* Empty State */
.pivot-table__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  color: var(--pivot-table-empty-color, #999);
}

.pivot-table__empty-content {
  text-align: center;
  padding: 32px;
}

/* Loading */
.pivot-table__loading {
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

.pivot-table__loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #1890ff;
  border-radius: 50%;
  animation: pivot-table-spin 1s linear infinite;
}

@keyframes pivot-table-spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Size Variants */
.pivot-table--small .pivot-table__header-row,
.pivot-table--small .pivot-table__data-row {
  height: 32px;
}

.pivot-table--small .pivot-table__corner-cell,
.pivot-table--small .pivot-table__column-header-cell,
.pivot-table--small .pivot-table__row-header-cell,
.pivot-table--small .pivot-table__data-cell {
  padding: 4px 8px;
  font-size: 12px;
}

.pivot-table--large .pivot-table__header-row,
.pivot-table--large .pivot-table__data-row {
  height: 48px;
}

.pivot-table--large .pivot-table__corner-cell,
.pivot-table--large .pivot-table__column-header-cell,
.pivot-table--large .pivot-table__row-header-cell,
.pivot-table--large .pivot-table__data-cell {
  padding: 12px 16px;
  font-size: 14px;
}
</style>
