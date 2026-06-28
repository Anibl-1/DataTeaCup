<template>
  <div class="virtual-table" :style="{ height: tableHeight }">
    <!-- Table Header -->
    <div class="virtual-table__header" role="rowgroup">
      <div class="virtual-table__header-row" role="row">
        <div
          v-for="col in columns"
          :key="col.key"
          class="virtual-table__header-cell"
          role="columnheader"
          :style="getColumnStyle(col)"
          :aria-label="col.title"
        >
          {{ col.title }}
        </div>
      </div>
    </div>

    <!-- Scrollable Body -->
    <div
      ref="containerRef"
      class="virtual-table__body"
      role="rowgroup"
      :aria-rowcount="data.length"
      @scroll="onScroll"
    >
      <!-- Spacer to create full scrollable height -->
      <div class="virtual-table__spacer" :style="{ height: `${totalHeight}px` }">
        <!-- Visible rows positioned via transform -->
        <div
          class="virtual-table__visible-rows"
          :style="{ transform: `translateY(${offsetY}px)` }"
        >
          <div
            v-for="index in visibleRange"
            :key="getRowKey(data[index], index)"
            class="virtual-table__row"
            role="row"
            :aria-rowindex="index + 1"
            :class="{ 'virtual-table__row--striped': index % 2 === 1 }"
            :style="{ height: `${rowHeight}px` }"
          >
            <div
              v-for="col in columns"
              :key="col.key"
              class="virtual-table__cell"
              role="cell"
              :style="getColumnStyle(col)"
            >
              <slot :name="`cell-${col.key}`" :row="data[index]" :index="index" :value="getCellValue(data[index], col.key)">
                {{ getCellValue(data[index], col.key) }}
              </slot>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Loading indicator -->
    <div v-if="isLoadingMore" class="virtual-table__loading" role="status" aria-label="Loading more data">
      <slot name="loading">
        <span class="virtual-table__loading-text">加载中...</span>
      </slot>
    </div>

    <!-- Empty state -->
    <div v-if="data.length === 0" class="virtual-table__empty" role="status">
      <slot name="empty">
        <span>暂无数据</span>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { computed, toRef } from 'vue'
import { useVirtualScroll } from '@/composables/useVirtualScroll'

export interface TableColumn {
  /** Column unique key */
  key: string
  /** Column display title */
  title: string
  /** Column width (px or CSS string) */
  width?: number | string
  /** Minimum column width */
  minWidth?: number
  /** Text alignment */
  align?: 'left' | 'center' | 'right'
}

export interface VirtualTableProps {
  /** Table data array */
  data: any[]
  /** Column definitions */
  columns: TableColumn[]
  /** Row height in pixels */
  rowHeight?: number
  /** Buffer rows above/below visible area */
  bufferSize?: number
  /** Table container height (CSS value) */
  height?: string
  /** Row key field or function */
  rowKey?: string | ((row: any, index: number) => string | number)
  /** Callback to load more data */
  onLoadMore?: () => Promise<void>
  /** Enable striped rows */
  striped?: boolean
}

const props = withDefaults(defineProps<VirtualTableProps>(), {
  rowHeight: 48,
  bufferSize: 5,
  height: '400px',
  rowKey: 'id',
  striped: true
})

const tableHeight = computed(() => props.height)

const itemCount = computed(() => props.data.length)

const {
  containerRef,
  visibleRange,
  totalHeight,
  offsetY,
  onScroll,
  isLoadingMore
} = useVirtualScroll({
  itemCount,
  itemHeight: props.rowHeight,
  bufferSize: props.bufferSize,
  onLoadMore: props.onLoadMore
})

function getRowKey(row: any, index: number): string | number {
  if (!row) return index
  if (typeof props.rowKey === 'function') {
    return props.rowKey(row, index)
  }
  return row[props.rowKey] ?? index
}

function getCellValue(row: any, key: string): any {
  if (!row) return ''
  // Support nested keys like "user.name"
  const keys = key.split('.')
  let value: any = row
  for (const k of keys) {
    if (value == null) return ''
    value = value[k]
  }
  return value ?? ''
}

function getColumnStyle(col: TableColumn): Record<string, string> {
  const style: Record<string, string> = {}
  if (col.width) {
    style.width = typeof col.width === 'number' ? `${col.width}px` : col.width
    style.flexShrink = '0'
    style.flexGrow = '0'
  } else {
    style.flex = '1'
    if (col.minWidth) {
      style.minWidth = `${col.minWidth}px`
    }
  }
  if (col.align) {
    style.textAlign = col.align
  }
  return style
}
</script>

<style scoped>
.virtual-table {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--border-color, #e8e8e8);
  border-radius: 4px;
  overflow: hidden;
  background: var(--bg-color, #fff);
}

.virtual-table__header {
  flex-shrink: 0;
  border-bottom: 1px solid var(--border-color, #e8e8e8);
  background: var(--header-bg, #fafafa);
}

.virtual-table__header-row {
  display: flex;
}

.virtual-table__header-cell {
  padding: 12px 16px;
  font-weight: 600;
  font-size: 14px;
  color: var(--header-text-color, #333);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  box-sizing: border-box;
}

.virtual-table__body {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  will-change: transform;
}

.virtual-table__spacer {
  position: relative;
}

.virtual-table__visible-rows {
  will-change: transform;
}

.virtual-table__row {
  display: flex;
  border-bottom: 1px solid var(--row-border-color, #f0f0f0);
  transition: background-color 0.15s ease;
}

.virtual-table__row:hover {
  background-color: var(--row-hover-bg, #f5f7fa);
}

.virtual-table__row--striped {
  background-color: var(--row-stripe-bg, #fafafa);
}

.virtual-table__row--striped:hover {
  background-color: var(--row-hover-bg, #f5f7fa);
}

.virtual-table__cell {
  padding: 0 16px;
  display: flex;
  align-items: center;
  font-size: 14px;
  color: var(--cell-text-color, #666);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  box-sizing: border-box;
}

.virtual-table__loading {
  padding: 8px 16px;
  text-align: center;
  border-top: 1px solid var(--border-color, #e8e8e8);
}

.virtual-table__loading-text {
  color: var(--loading-text-color, #999);
  font-size: 13px;
}

.virtual-table__empty {
  padding: 32px 16px;
  text-align: center;
  color: var(--empty-text-color, #999);
  font-size: 14px;
}
</style>
