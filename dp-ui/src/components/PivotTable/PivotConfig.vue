<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 透视表配置面板组件
 * Pivot Table Configuration Panel Component
 * 
 * 支持行列维度拖拽配置
 * Supports row/column dimension drag-drop configuration
 * 
 * 需求: 22.3.7 - 支持行列维度拖拽配置
 */

import { ref, computed, watch } from 'vue'
import type {
  DimensionField,
  ValueField,
  FilterField,
  PivotConfig,
  DragZoneType,
  DragItem,
  DropEvent,
  AggregationType,
  TotalConfig,
} from './types'
import { DEFAULT_TOTAL_CONFIG } from './types'

// ============================================================================
// Props
// ============================================================================

interface Props {
  /** Available fields for pivot */
  availableFields: DimensionField[]
  /** Current pivot configuration */
  config: PivotConfig
  /** Total configuration */
  totalConfig?: TotalConfig
  /** Panel position */
  position?: 'left' | 'right' | 'top'
  /** Whether panel is collapsed */
  collapsed?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  position: 'left',
  collapsed: false,
})

// ============================================================================
// Emits
// ============================================================================

const emit = defineEmits<{
  (e: 'update:config', config: PivotConfig): void
  (e: 'update:totalConfig', config: TotalConfig): void
  (e: 'update:collapsed', collapsed: boolean): void
  (e: 'drop', event: DropEvent): void
}>()

// ============================================================================
// State
// ============================================================================

const draggedItem = ref<DragItem | null>(null)
const dragOverZone = ref<DragZoneType | null>(null)
const isCollapsed = ref(props.collapsed)

// ============================================================================
// Computed
// ============================================================================

const normalizedTotalConfig = computed(() => ({
  ...DEFAULT_TOTAL_CONFIG,
  ...props.totalConfig,
}))

/**
 * Fields not yet used in any zone
 * 尚未使用的可用字段
 */
const unusedFields = computed(() => {
  const usedFieldIds = new Set([
    ...props.config.rows.map(f => f.id),
    ...props.config.columns.map(f => f.id),
    ...props.config.values.map(f => f.field),
    ...(props.config.filters?.map(f => f.id) ?? []),
  ])
  
  return props.availableFields.filter(f => !usedFieldIds.has(f.id))
})

const panelClasses = computed(() => [
  'pivot-config',
  `pivot-config--${props.position}`,
  {
    'pivot-config--collapsed': isCollapsed.value,
  },
])

// ============================================================================
// Drag and Drop Handlers
// ============================================================================

/**
 * Handle drag start
 * 处理拖拽开始
 * 
 * Validates: 22.3.7 - 支持行列维度拖拽配置
 */
const handleDragStart = (
  event: DragEvent,
  field: DimensionField | ValueField | FilterField,
  sourceZone: DragZoneType,
  type: 'dimension' | 'value' | 'filter'
) => {
  if (!event.dataTransfer) return

  draggedItem.value = {
    type,
    field,
    sourceZone,
  }

  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', JSON.stringify(draggedItem.value))
  
  // Add dragging class to element
  const target = event.target as HTMLElement
  target.classList.add('dragging')
}

/**
 * Handle drag end
 * 处理拖拽结束
 */
const handleDragEnd = (event: DragEvent) => {
  draggedItem.value = null
  dragOverZone.value = null
  
  const target = event.target as HTMLElement
  target.classList.remove('dragging')
}

/**
 * Handle drag over
 * 处理拖拽经过
 */
const handleDragOver = (event: DragEvent, zone: DragZoneType) => {
  event.preventDefault()
  if (!event.dataTransfer) return
  
  event.dataTransfer.dropEffect = 'move'
  dragOverZone.value = zone
}

/**
 * Handle drag leave
 * 处理拖拽离开
 */
const handleDragLeave = () => {
  dragOverZone.value = null
}

/**
 * Handle drop
 * 处理放置
 * 
 * Validates: 22.3.7 - 支持行列维度拖拽配置
 */
const handleDrop = (event: DragEvent, targetZone: DragZoneType, targetIndex?: number) => {
  event.preventDefault()
  
  if (!draggedItem.value) return

  const item = draggedItem.value
  const newConfig = { ...props.config }

  // Remove from source zone
  if (item.sourceZone !== 'available') {
    switch (item.sourceZone) {
      case 'rows':
        newConfig.rows = newConfig.rows.filter(f => f.id !== (item.field as DimensionField).id)
        break
      case 'columns':
        newConfig.columns = newConfig.columns.filter(f => f.id !== (item.field as DimensionField).id)
        break
      case 'values':
        newConfig.values = newConfig.values.filter(f => f.id !== (item.field as ValueField).id)
        break
      case 'filters':
        newConfig.filters = (newConfig.filters ?? []).filter(f => f.id !== (item.field as FilterField).id)
        break
    }
  }

  // Add to target zone
  const idx = targetIndex ?? 0
  switch (targetZone) {
    case 'rows':
      if (item.type === 'dimension') {
        const field = item.field as DimensionField
        newConfig.rows = [...newConfig.rows.slice(0, idx), field, ...newConfig.rows.slice(idx)]
      }
      break
    case 'columns':
      if (item.type === 'dimension') {
        const field = item.field as DimensionField
        newConfig.columns = [...newConfig.columns.slice(0, idx), field, ...newConfig.columns.slice(idx)]
      }
      break
    case 'values':
      if (item.type === 'dimension') {
        // Convert dimension to value field
        const dimField = item.field as DimensionField
        const valueField: ValueField = {
          id: `${dimField.id}_sum`,
          field: dimField.field,
          label: `${dimField.label} (求和)`,
          aggregation: 'sum',
        }
        newConfig.values = [...newConfig.values.slice(0, idx), valueField, ...newConfig.values.slice(idx)]
      } else if (item.type === 'value') {
        const field = item.field as ValueField
        newConfig.values = [...newConfig.values.slice(0, idx), field, ...newConfig.values.slice(idx)]
      }
      break
    case 'filters':
      if (item.type === 'dimension') {
        const dimField = item.field as DimensionField
        const filterField: FilterField = {
          id: dimField.id,
          field: dimField.field,
          label: dimField.label,
          filterType: 'multiSelect',
          values: [],
        }
        newConfig.filters = [...(newConfig.filters ?? []).slice(0, idx), filterField, ...(newConfig.filters ?? []).slice(idx)]
      }
      break
    case 'available':
      // Just remove from source, already done above
      break
  }

  emit('update:config', newConfig)
  emit('drop', { item, targetZone, targetIndex: idx })

  draggedItem.value = null
  dragOverZone.value = null
}

/**
 * Remove field from zone
 * 从区域移除字段
 */
const removeField = (zone: DragZoneType, fieldId: string) => {
  const newConfig = { ...props.config }

  switch (zone) {
    case 'rows':
      newConfig.rows = newConfig.rows.filter(f => f.id !== fieldId)
      break
    case 'columns':
      newConfig.columns = newConfig.columns.filter(f => f.id !== fieldId)
      break
    case 'values':
      newConfig.values = newConfig.values.filter(f => f.id !== fieldId)
      break
    case 'filters':
      newConfig.filters = (newConfig.filters ?? []).filter(f => f.id !== fieldId)
      break
  }

  emit('update:config', newConfig)
}

/**
 * Change aggregation type for value field
 * 更改值字段的聚合类型
 */
const changeAggregation = (fieldId: string, aggregation: AggregationType) => {
  const newConfig = { ...props.config }
  const valueIndex = newConfig.values.findIndex(f => f.id === fieldId)
  
  if (valueIndex >= 0) {
    const field = newConfig.values[valueIndex]
    newConfig.values[valueIndex] = {
      ...field,
      aggregation,
      id: `${field.field}_${aggregation}`,
      label: `${field.field} (${getAggregationLabel(aggregation)})`,
    }
    emit('update:config', newConfig)
  }
}

/**
 * Get aggregation label
 * 获取聚合类型标签
 */
const getAggregationLabel = (type: AggregationType): string => {
  const labels: Record<AggregationType, string> = {
    sum: '求和',
    count: '计数',
    avg: '平均值',
    min: '最小值',
    max: '最大值',
    countDistinct: '去重计数',
    first: '首个',
    last: '末个',
    custom: '自定义',
  }
  return labels[type]
}

// ============================================================================
// Total Config Handlers
// ============================================================================

/**
 * Update total configuration
 * 更新总计配置
 * 
 * Validates: 22.3.9 - 支持小计和总计显示位置配置
 */
const updateTotalConfig = (key: keyof TotalConfig, value: any) => {
  emit('update:totalConfig', {
    ...normalizedTotalConfig.value,
    [key]: value,
  })
}

// ============================================================================
// Panel Toggle
// ============================================================================

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
  emit('update:collapsed', isCollapsed.value)
}

// ============================================================================
// Watchers
// ============================================================================

watch(() => props.collapsed, (val) => {
  isCollapsed.value = val
})
</script>

<template>
  <div :class="panelClasses">
    <!-- Collapse Toggle -->
    <button
      class="pivot-config__toggle"
      :aria-expanded="!isCollapsed"
      aria-label="切换配置面板"
      @click="toggleCollapse"
    >
      <span class="pivot-config__toggle-icon">
        {{ isCollapsed ? '▶' : '◀' }}
      </span>
    </button>

    <div v-show="!isCollapsed" class="pivot-config__content">
      <!-- Available Fields -->
      <div class="pivot-config__section">
        <h4 class="pivot-config__section-title">可用字段</h4>
        <div
          class="pivot-config__zone pivot-config__zone--available"
          :class="{ 'pivot-config__zone--drag-over': dragOverZone === 'available' }"
          @dragover="handleDragOver($event, 'available')"
          @dragleave="handleDragLeave"
          @drop="handleDrop($event, 'available')"
        >
          <div
            v-for="field in unusedFields"
            :key="field.id"
            class="pivot-config__field"
            draggable="true"
            @dragstart="handleDragStart($event, field, 'available', 'dimension')"
            @dragend="handleDragEnd"
          >
            <span class="pivot-config__field-icon">📊</span>
            <span class="pivot-config__field-label">{{ field.label }}</span>
          </div>
          <div v-if="unusedFields.length === 0" class="pivot-config__empty">
            所有字段已使用
          </div>
        </div>
      </div>

      <!-- Row Dimensions -->
      <div class="pivot-config__section">
        <h4 class="pivot-config__section-title">行维度</h4>
        <div
          class="pivot-config__zone pivot-config__zone--rows"
          :class="{ 'pivot-config__zone--drag-over': dragOverZone === 'rows' }"
          @dragover="handleDragOver($event, 'rows')"
          @dragleave="handleDragLeave"
          @drop="handleDrop($event, 'rows', config.rows.length)"
        >
          <div
            v-for="(field, index) in config.rows"
            :key="field.id"
            class="pivot-config__field pivot-config__field--active"
            draggable="true"
            @dragstart="handleDragStart($event, field, 'rows', 'dimension')"
            @dragend="handleDragEnd"
            @drop.stop="handleDrop($event, 'rows', index)"
          >
            <span class="pivot-config__field-icon">📋</span>
            <span class="pivot-config__field-label">{{ field.label }}</span>
            <button
              class="pivot-config__field-remove"
              aria-label="移除字段"
              @click="removeField('rows', field.id)"
            >
              ×
            </button>
          </div>
          <div v-if="config.rows.length === 0" class="pivot-config__placeholder">
            拖拽字段到此处
          </div>
        </div>
      </div>

      <!-- Column Dimensions -->
      <div class="pivot-config__section">
        <h4 class="pivot-config__section-title">列维度</h4>
        <div
          class="pivot-config__zone pivot-config__zone--columns"
          :class="{ 'pivot-config__zone--drag-over': dragOverZone === 'columns' }"
          @dragover="handleDragOver($event, 'columns')"
          @dragleave="handleDragLeave"
          @drop="handleDrop($event, 'columns', config.columns.length)"
        >
          <div
            v-for="(field, index) in config.columns"
            :key="field.id"
            class="pivot-config__field pivot-config__field--active"
            draggable="true"
            @dragstart="handleDragStart($event, field, 'columns', 'dimension')"
            @dragend="handleDragEnd"
            @drop.stop="handleDrop($event, 'columns', index)"
          >
            <span class="pivot-config__field-icon">📊</span>
            <span class="pivot-config__field-label">{{ field.label }}</span>
            <button
              class="pivot-config__field-remove"
              aria-label="移除字段"
              @click="removeField('columns', field.id)"
            >
              ×
            </button>
          </div>
          <div v-if="config.columns.length === 0" class="pivot-config__placeholder">
            拖拽字段到此处
          </div>
        </div>
      </div>

      <!-- Values -->
      <div class="pivot-config__section">
        <h4 class="pivot-config__section-title">值</h4>
        <div
          class="pivot-config__zone pivot-config__zone--values"
          :class="{ 'pivot-config__zone--drag-over': dragOverZone === 'values' }"
          @dragover="handleDragOver($event, 'values')"
          @dragleave="handleDragLeave"
          @drop="handleDrop($event, 'values', config.values.length)"
        >
          <div
            v-for="(field, index) in config.values"
            :key="field.id"
            class="pivot-config__field pivot-config__field--value"
            draggable="true"
            @dragstart="handleDragStart($event, field, 'values', 'value')"
            @dragend="handleDragEnd"
            @drop.stop="handleDrop($event, 'values', index)"
          >
            <span class="pivot-config__field-icon">∑</span>
            <span class="pivot-config__field-label">{{ field.label }}</span>
            <select
              class="pivot-config__aggregation-select"
              :value="field.aggregation"
              @change="changeAggregation(field.id, ($event.target as HTMLSelectElement).value as AggregationType)"
              @click.stop
            >
              <option value="sum">求和</option>
              <option value="count">计数</option>
              <option value="avg">平均值</option>
              <option value="min">最小值</option>
              <option value="max">最大值</option>
              <option value="countDistinct">去重计数</option>
            </select>
            <button
              class="pivot-config__field-remove"
              aria-label="移除字段"
              @click="removeField('values', field.id)"
            >
              ×
            </button>
          </div>
          <div v-if="config.values.length === 0" class="pivot-config__placeholder">
            拖拽字段到此处
          </div>
        </div>
      </div>

      <!-- Filters -->
      <div class="pivot-config__section">
        <h4 class="pivot-config__section-title">筛选</h4>
        <div
          class="pivot-config__zone pivot-config__zone--filters"
          :class="{ 'pivot-config__zone--drag-over': dragOverZone === 'filters' }"
          @dragover="handleDragOver($event, 'filters')"
          @dragleave="handleDragLeave"
          @drop="handleDrop($event, 'filters', (config.filters ?? []).length)"
        >
          <div
            v-for="(field, index) in config.filters ?? []"
            :key="field.id"
            class="pivot-config__field pivot-config__field--filter"
            draggable="true"
            @dragstart="handleDragStart($event, field, 'filters', 'filter')"
            @dragend="handleDragEnd"
            @drop.stop="handleDrop($event, 'filters', index)"
          >
            <span class="pivot-config__field-icon">🔍</span>
            <span class="pivot-config__field-label">{{ field.label }}</span>
            <button
              class="pivot-config__field-remove"
              aria-label="移除字段"
              @click="removeField('filters', field.id)"
            >
              ×
            </button>
          </div>
          <div v-if="!config.filters || config.filters.length === 0" class="pivot-config__placeholder">
            拖拽字段到此处
          </div>
        </div>
      </div>

      <!-- Total Configuration -->
      <div class="pivot-config__section">
        <h4 class="pivot-config__section-title">汇总设置</h4>
        <div class="pivot-config__total-options">
          <!-- Row Subtotals -->
          <label class="pivot-config__checkbox">
            <input
              type="checkbox"
              :checked="normalizedTotalConfig.showRowSubtotals"
              @change="updateTotalConfig('showRowSubtotals', ($event.target as HTMLInputElement).checked)"
            />
            <span>显示行小计</span>
          </label>
          <div v-if="normalizedTotalConfig.showRowSubtotals" class="pivot-config__position-select">
            <span>位置:</span>
            <select
              :value="normalizedTotalConfig.rowSubtotalPosition"
              @change="updateTotalConfig('rowSubtotalPosition', ($event.target as HTMLSelectElement).value)"
            >
              <option value="top">顶部</option>
              <option value="bottom">底部</option>
            </select>
          </div>

          <!-- Row Grand Total -->
          <label class="pivot-config__checkbox">
            <input
              type="checkbox"
              :checked="normalizedTotalConfig.showRowGrandTotal"
              @change="updateTotalConfig('showRowGrandTotal', ($event.target as HTMLInputElement).checked)"
            />
            <span>显示行总计</span>
          </label>
          <div v-if="normalizedTotalConfig.showRowGrandTotal" class="pivot-config__position-select">
            <span>位置:</span>
            <select
              :value="normalizedTotalConfig.rowGrandTotalPosition"
              @change="updateTotalConfig('rowGrandTotalPosition', ($event.target as HTMLSelectElement).value)"
            >
              <option value="top">顶部</option>
              <option value="bottom">底部</option>
            </select>
          </div>

          <!-- Column Subtotals -->
          <label class="pivot-config__checkbox">
            <input
              type="checkbox"
              :checked="normalizedTotalConfig.showColumnSubtotals"
              @change="updateTotalConfig('showColumnSubtotals', ($event.target as HTMLInputElement).checked)"
            />
            <span>显示列小计</span>
          </label>
          <div v-if="normalizedTotalConfig.showColumnSubtotals" class="pivot-config__position-select">
            <span>位置:</span>
            <select
              :value="normalizedTotalConfig.columnSubtotalPosition"
              @change="updateTotalConfig('columnSubtotalPosition', ($event.target as HTMLSelectElement).value)"
            >
              <option value="left">左侧</option>
              <option value="right">右侧</option>
            </select>
          </div>

          <!-- Column Grand Total -->
          <label class="pivot-config__checkbox">
            <input
              type="checkbox"
              :checked="normalizedTotalConfig.showColumnGrandTotal"
              @change="updateTotalConfig('showColumnGrandTotal', ($event.target as HTMLInputElement).checked)"
            />
            <span>显示列总计</span>
          </label>
          <div v-if="normalizedTotalConfig.showColumnGrandTotal" class="pivot-config__position-select">
            <span>位置:</span>
            <select
              :value="normalizedTotalConfig.columnGrandTotalPosition"
              @change="updateTotalConfig('columnGrandTotalPosition', ($event.target as HTMLSelectElement).value)"
            >
              <option value="left">左侧</option>
              <option value="right">右侧</option>
            </select>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.pivot-config {
  display: flex;
  flex-direction: column;
  background-color: var(--pivot-config-bg, #f5f5f5);
  border: 1px solid var(--pivot-config-border, #e8e8e8);
  border-radius: 4px;
  min-width: 240px;
  max-width: 300px;
  transition: width 0.2s ease;
}

.pivot-config--collapsed {
  min-width: 32px;
  max-width: 32px;
}

.pivot-config--top {
  flex-direction: row;
  min-width: 100%;
  max-width: 100%;
  min-height: auto;
}

.pivot-config__toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 32px;
  background: none;
  border: none;
  border-bottom: 1px solid var(--pivot-config-border, #e8e8e8);
  cursor: pointer;
  color: var(--pivot-config-text, #666);
}

.pivot-config--collapsed .pivot-config__toggle {
  border-bottom: none;
  height: 100%;
  min-height: 200px;
}

.pivot-config__toggle:hover {
  background-color: var(--pivot-config-hover, #e8e8e8);
}

.pivot-config__toggle-icon {
  font-size: 10px;
}

.pivot-config__content {
  padding: 12px;
  overflow-y: auto;
  max-height: calc(100vh - 200px);
}

.pivot-config__section {
  margin-bottom: 16px;
}

.pivot-config__section:last-child {
  margin-bottom: 0;
}

.pivot-config__section-title {
  margin: 0 0 8px 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--pivot-config-title, #333);
}

.pivot-config__zone {
  min-height: 60px;
  padding: 8px;
  background-color: var(--pivot-config-zone-bg, #fff);
  border: 2px dashed var(--pivot-config-zone-border, #d9d9d9);
  border-radius: 4px;
  transition: all 0.2s ease;
}

.pivot-config__zone--drag-over {
  border-color: var(--pivot-config-zone-active, #1890ff);
  background-color: var(--pivot-config-zone-active-bg, #e6f7ff);
}

.pivot-config__field {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  margin-bottom: 4px;
  background-color: var(--pivot-field-bg, #fafafa);
  border: 1px solid var(--pivot-field-border, #e8e8e8);
  border-radius: 4px;
  cursor: grab;
  font-size: 13px;
  transition: all 0.15s ease;
}

.pivot-config__field:last-child {
  margin-bottom: 0;
}

.pivot-config__field:hover {
  background-color: var(--pivot-field-hover, #f0f0f0);
  border-color: var(--pivot-field-hover-border, #d9d9d9);
}

.pivot-config__field.dragging {
  opacity: 0.5;
  cursor: grabbing;
}

.pivot-config__field--active {
  background-color: var(--pivot-field-active-bg, #e6f7ff);
  border-color: var(--pivot-field-active-border, #91d5ff);
}

.pivot-config__field--value {
  background-color: var(--pivot-field-value-bg, #f6ffed);
  border-color: var(--pivot-field-value-border, #b7eb8f);
}

.pivot-config__field--filter {
  background-color: var(--pivot-field-filter-bg, #fff7e6);
  border-color: var(--pivot-field-filter-border, #ffd591);
}

.pivot-config__field-icon {
  flex-shrink: 0;
  font-size: 14px;
}

.pivot-config__field-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pivot-config__field-remove {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  padding: 0;
  background: none;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  color: var(--pivot-field-remove, #999);
  transition: all 0.15s ease;
}

.pivot-config__field-remove:hover {
  background-color: var(--pivot-field-remove-hover-bg, #ff4d4f);
  color: #fff;
}

.pivot-config__aggregation-select {
  flex-shrink: 0;
  padding: 2px 4px;
  font-size: 11px;
  border: 1px solid var(--pivot-select-border, #d9d9d9);
  border-radius: 2px;
  background-color: #fff;
  cursor: pointer;
}

.pivot-config__placeholder,
.pivot-config__empty {
  padding: 12px;
  text-align: center;
  color: var(--pivot-placeholder, #999);
  font-size: 12px;
}

.pivot-config__total-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pivot-config__checkbox {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  cursor: pointer;
}

.pivot-config__checkbox input {
  cursor: pointer;
}

.pivot-config__position-select {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 20px;
  font-size: 12px;
  color: var(--pivot-config-text, #666);
}

.pivot-config__position-select select {
  padding: 2px 6px;
  font-size: 12px;
  border: 1px solid var(--pivot-select-border, #d9d9d9);
  border-radius: 2px;
  background-color: #fff;
}
</style>
