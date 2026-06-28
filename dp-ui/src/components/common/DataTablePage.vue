<template>
  <n-card :bordered="false">
    <!-- Card header -->
    <template #header>
      <div class="card-header-custom">
        <n-icon v-if="icon" :component="icon" class="header-icon" />
        <span>{{ title }}</span>
      </div>
    </template>

    <!-- Header extra: toolbar slot + export -->
    <template #header-extra>
      <n-space :size="8">
        <slot name="toolbar" :data="tableState.data.value" :checked-row-keys="tableState.checkedRowKeys.value" />
        <n-dropdown
          v-if="showExport"
          :options="exportState.exportOptions"
          @select="handleExportDropdown"
        >
          <n-button :loading="exportState.exporting.value" size="small">
            导出
          </n-button>
        </n-dropdown>
      </n-space>
    </template>

    <!-- Search area -->
    <div v-if="showSearch" class="query-form">
      <slot name="search" :search-params="tableState.searchParams" :load="tableState.load" :reset="tableState.reset" />
    </div>

    <!-- Extra slot -->
    <slot name="extra" />

    <!-- Data table -->
    <div class="custom-table">
      <n-data-table
        :columns="columns"
        :data="tableState.data.value"
        :loading="tableState.loading.value"
        :row-key="rowKeyFn"
        :checked-row-keys="showSelection ? tableState.checkedRowKeys.value : undefined"
        :remote="true"
        :pagination="false"
        @update:sorter="tableState.handleSorterChange"
        @update:checked-row-keys="tableState.handleCheck"
      />
    </div>

    <!-- Pagination -->
    <div class="pagination-wrapper">
      <n-pagination
        :page="tableState.pagination.value.page"
        :page-size="tableState.pagination.value.pageSize"
        :item-count="tableState.pagination.value.itemCount"
        :page-sizes="[10, 20, 50, 100]"
        show-size-picker
        @update:page="tableState.handlePageChange"
        @update:page-size="tableState.handlePageSizeChange"
      />
    </div>
  </n-card>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * DataTablePage - 统一的数据表格页面组件
 * 组合 NCard + 搜索区域 + NDataTable + NPagination，内部使用 useDataTable 管理数据。
 *
 * Requirements: 5.5, 5.6
 */
import { type Component } from 'vue'
import { NCard, NDataTable, NPagination, NButton, NSpace, NIcon, NDropdown } from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import { useDataTable } from '@/composables/useDataTable'
import { useExport } from '@/composables/useExport'

export interface DataTablePageProps {
  /** 表格列配置 */
  columns: DataTableColumns
  /** API 请求函数 */
  apiFn: (params: any) => Promise<any>
  /** 行唯一键 */
  rowKey?: string
  /** 页面标题 */
  title?: string
  /** 页面图标 */
  icon?: Component
  /** 是否显示搜索区域 */
  showSearch?: boolean
  /** 是否显示导出按钮 */
  showExport?: boolean
  /** 是否显示选中功能 */
  showSelection?: boolean
}

const props = withDefaults(defineProps<DataTablePageProps>(), {
  rowKey: 'id',
  title: '',
  icon: undefined,
  showSearch: false,
  showExport: false,
  showSelection: false
})

// Internal useDataTable state
const tableState = useDataTable({
  apiFn: props.apiFn,
  rowKey: props.rowKey as any,
  immediate: true
})

// Internal useExport state (only used when showExport is true)
const exportState = useExport()

// Export dropdown handler
function handleExportDropdown(key: string) {
  exportState.handleExportSelect(key, tableState.data.value)
}

// Row key function for NDataTable
const rowKeyFn = (row: Record<string, unknown>) => row[props.rowKey] as string | number

// Expose table state for parent access
defineExpose({
  load: tableState.load,
  refresh: tableState.refresh,
  reset: tableState.reset,
  data: tableState.data,
  checkedRowKeys: tableState.checkedRowKeys,
  checkedRows: tableState.checkedRows,
  searchParams: tableState.searchParams,
  clearChecked: tableState.clearChecked
})
</script>
