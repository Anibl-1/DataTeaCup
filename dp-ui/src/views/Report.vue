<template>
  <div class="report-page">
    <!-- 查询面板 -->
    <n-card class="query-card">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="20" color="#2563EB" class="header-icon"><SearchOutline /></n-icon>
          <span>报表查询</span>
        </div>
      </template>
      <template #header-extra>
        <FilterPanel
          v-if="filterFields.length > 0"
          :fields="filterFields"
          :model-value="activeFilters"
          @apply="handleFilterApply"
        />
      </template>
      
      <div class="query-form-wrapper">
        <n-form inline :model="queryForm" class="query-form">
          <n-form-item label="数据源">
            <n-select
              v-model:value="queryForm.dataSourceId"
              :options="dataSourceOptions"
              placeholder="请选择数据源"
              :loading="dataSourceLoading"
              filterable
              clearable
              style="width: 220px"
            />
          </n-form-item>
          <n-form-item label="表名">
            <n-input
              v-model:value="queryForm.tableName"
              placeholder="请输入表名"
              style="width: 220px"
              @keydown.enter.prevent="handleQuery"
            >
              <template #prefix>
                <n-icon color="#94a3b8"><GridOutline /></n-icon>
              </template>
            </n-input>
          </n-form-item>
          <n-form-item>
            <n-space>
              <n-button type="primary" :loading="loading" @click="handleQuery">
                <template #icon>
                  <n-icon><SearchOutline /></n-icon>
                </template>
                查询
              </n-button>
              <n-button @click="handleReset">
                <template #icon>
                  <n-icon><RefreshOutline /></n-icon>
                </template>
                重置
              </n-button>
              <n-dropdown trigger="click" :options="exportDropdownOptions" @select="onExportSelect">
                <n-button :loading="exporting">
                  <template #icon>
                    <n-icon><DownloadOutline /></n-icon>
                  </template>
                  导出
                </n-button>
              </n-dropdown>
            </n-space>
          </n-form-item>
        </n-form>
      </div>
    </n-card>

    <!-- 数据表格 -->
    <n-card class="data-card">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="18" color="#059669" class="header-icon"><DocumentTextOutline /></n-icon>
          <span>查询结果</span>
          <div class="header-actions">
            <n-tag v-if="total > 0" type="success" size="small" round>
              {{ total }} 条
            </n-tag>
          </div>
        </div>
      </template>
      
      <div class="table-container custom-table">
        <n-data-table
          :columns="smartColumns"
          :data="tableData"
          :loading="loading"
          :pagination="false"
          :scroll-x="tableScrollX"
          :max-height="tableMaxHeight"
          size="medium"
          striped
          class="smart-report-table"
        >
          <template #empty>
            <n-empty description="暂无数据" size="large">
              <template #icon>
                <n-icon :size="48" :component="SearchOutline" class="text-muted" />
              </template>
              <template #extra>
                <n-text depth="3">请选择数据源并输入表名后点击查询</n-text>
              </template>
            </n-empty>
          </template>
        </n-data-table>
      </div>
      
      <div class="pagination-wrapper">
        <div class="pagination-info">
          <n-tag type="info" size="small" round>
            共 {{ total }} 条记录
          </n-tag>
        </div>
        <n-pagination
          v-model:page="paginationPage"
          v-model:page-size="paginationPageSize"
          :item-count="total"
          :page-sizes="PAGE_SIZES"
          show-size-picker
          show-quick-jumper
          size="medium"
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </n-card>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, onMounted, computed } from 'vue'
import { useMessage, NSpace, NDropdown, NIcon, NTag } from 'naive-ui'
import { SearchOutline, GridOutline, RefreshOutline, DownloadOutline, DocumentTextOutline } from '@vicons/ionicons5'
import { queryReport, exportReport, exportReportAsCsv } from '@/api/report'
import { getDataSourceList, getTableColumns } from '@/api/dataSource'
import { useExport } from '@/composables/useExport'
import { initMessage } from '@/utils/message'
import { handleApiError } from '@/utils/error'
import { formatTableCellValue } from '@/utils/format'
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE, PAGE_SIZES } from '@/constants'
import type { DataSource } from '@/types/dataSource'
import type { PageResult, FilterCondition } from '@/types/api'
import type { DataTableColumn } from 'naive-ui'
import FilterPanel, { type FilterField } from '@/components/FilterPanel.vue'
import { filtersToApiParam } from '@/utils/filterParams'

const message = useMessage()
initMessage(message)

// --- 使用 useExport 管理导出逻辑 (Req 6.1) ---
const { exporting, exportOptions: exportDropdownOptions, handleExportSelect: baseExportSelect } = useExport({
  backendExportFn: async (params: any) => {
    if (!queryForm.dataSourceId || !queryForm.tableName?.trim()) {
      throw new Error('请先选择数据源和输入表名')
    }
    const filtersParam = filtersToApiParam(activeFilters.value)
    const apiParams = {
      dataSourceId: queryForm.dataSourceId!,
      tableName: queryForm.tableName.trim(),
      filters: filtersParam
    }
    if (params.format === 'csv') {
      return await exportReportAsCsv(apiParams) as unknown as Blob
    }
    return await exportReport(apiParams) as unknown as Blob
  },
  defaultFilename: '报表数据'
})

/** 处理导出选择 — 验证输入后委托给 useExport */
const onExportSelect = async (key: string) => {
  if (!queryForm.dataSourceId) {
    message.warning('请选择数据源')
    return
  }
  if (!queryForm.tableName || queryForm.tableName.trim() === '') {
    message.warning('请输入表名')
    return
  }
  // 传入当前表格数据，useExport 会根据阈值决定前端/后端导出
  await baseExportSelect(key, tableData.value)
}

// --- 分页状态 ---
const loading = ref(false)
const tableData = ref<Record<string, any>[]>([])
const total = ref(0)
const paginationPage = ref(DEFAULT_PAGE)
const paginationPageSize = ref(DEFAULT_PAGE_SIZE)

// 动态计算表格最大高度（根据视口高度）
const tableMaxHeight = computed(() => {
  const viewportHeight = window.innerHeight
  return Math.max(400, viewportHeight - 312)
})

const dataSourceOptions = ref<Array<{ label: string; value: number }>>([])
const activeFilters = ref<FilterCondition[]>([])

const queryForm = reactive({
  dataSourceId: null as number | null,
  tableName: ''
})

const columns = ref<Array<{ title: string; key: string }>>([])

/**
 * 计算字符串显示宽度（估算值）
 */
const calculateTextWidth = (text: string): number => {
  if (!text) return 80
  const chineseRegex = /[\u4e00-\u9fa5]/g
  const chineseCount = (text.match(chineseRegex) || []).length
  const otherCount = text.length - chineseCount
  return chineseCount * 14 + otherCount * 8 + 40
}

/**
 * 智能计算列宽
 */
const calculateColumnWidth = (column: { title: string; key: string }, data: any[]): number => {
  const MIN_WIDTH = 100
  const MAX_WIDTH = 350
  
  let maxWidth = calculateTextWidth(column.title || column.key)

  const sampleSize = Math.min(data.length, 20)
  for (let i = 0; i < sampleSize; i++) {
    const value = data[i][column.key]
    if (value !== null && value !== undefined) {
      const text = String(value)
      const width = calculateTextWidth(text)
      maxWidth = Math.max(maxWidth, width)
    }
  }

  return Math.min(Math.max(maxWidth, MIN_WIDTH), MAX_WIDTH)
}

/**
 * 智能列配置（带宽度计算和值处理）
 */
const smartColumns = computed<DataTableColumn[]>(() => {
  if (columns.value.length === 0) {
    return []
  }
  
  return columns.value.map(col => {
    const width = calculateColumnWidth(col, tableData.value)
    
    return {
      title: col.title,
      key: col.key,
      width,
      minWidth: 100,
      ellipsis: {
        tooltip: true
      },
      resizable: true,
      render: (row: any) => {
        return formatTableCellValue(row[col.key])
      }
    } as DataTableColumn
  })
})

/**
 * 动态计算水平滚动宽度
 */
const tableScrollX = computed(() => {
  if (smartColumns.value.length === 0) {
    return undefined
  }
  
  const totalWidth = smartColumns.value.reduce((sum, col: any) => {
    return sum + (col.width || 100)
  }, 0)
  
  return totalWidth > 1200 ? totalWidth : undefined
})

// 动态生成筛选字段（根据查询到的数据列）
const filterFields = computed<FilterField[]>(() => {
  if (columns.value.length === 0) {
    return []
  }
  return columns.value.map(col => {
    const firstRow = tableData.value[0]
    const value = firstRow?.[col.key]
    let type: 'string' | 'number' = 'string'
    if (typeof value === 'number') {
      type = 'number'
    }
    return {
      label: col.title,
      value: col.key,
      type
    }
  })
})

const dataSourceLoading = ref(false)

const fetchDataSources = async () => {
  dataSourceLoading.value = true
  try {
    const res = await getDataSourceList({ page: 1, pageSize: 1000 })
    let dataList: DataSource[] = []
    if (res && res.data) {
      if (Array.isArray(res.data)) {
        dataList = res.data
      } else if (res.data.list && Array.isArray(res.data.list)) {
        dataList = res.data.list
      } else if ((res.data as any).list) {
        dataList = (res.data as any).list
      }
    }
    
    dataSourceOptions.value = dataList.map((item: DataSource) => ({
      label: `${item.name} (${item.dbType || '未知'})`,
      value: item.id
    }))
    
    if (dataSourceOptions.value.length === 0) {
      message.warning('暂无可用数据源，请先在"数据源管理"中添加数据源')
    }
  } catch (error) {
    const errorMsg = handleApiError(error, '获取数据源列表', '获取数据源列表失败，请检查网络连接')
    message.error(errorMsg)
    dataSourceOptions.value = []
  } finally {
    dataSourceLoading.value = false
  }
}

const handleQuery = async () => {
  if (!queryForm.dataSourceId) {
    message.warning('请选择数据源')
    return
  }
  if (!queryForm.tableName || queryForm.tableName.trim() === '') {
    message.warning('请输入表名')
    return
  }
  
  loading.value = true
  try {
    const filtersParam = filtersToApiParam(activeFilters.value)
    const res = await queryReport({
      dataSourceId: queryForm.dataSourceId!,
      tableName: queryForm.tableName.trim(),
      page: paginationPage.value,
      pageSize: paginationPageSize.value,
      filters: filtersParam
    })
    
    let pageResult: PageResult<Record<string, any>> | null = null
    
    if (res && typeof res === 'object') {
      if (res.data && typeof res.data === 'object' && 'list' in res.data && 'total' in res.data) {
        pageResult = res.data as PageResult<Record<string, any>>
      } else if ('list' in res && 'total' in res) {
        pageResult = res as PageResult<Record<string, any>>
      }
    }
    
    tableData.value = pageResult?.list || []
    
    let totalCount = 0
    if (pageResult && pageResult.total !== undefined && pageResult.total !== null) {
      totalCount = typeof pageResult.total === 'number' 
        ? Math.max(0, pageResult.total)
        : (() => {
            const num = Number(pageResult.total)
            return isNaN(num) ? 0 : Math.max(0, num)
          })()
      total.value = totalCount
    } else {
      total.value = 0
    }
    
    // 动态生成列（按照数据库表的字段顺序）
    if (pageResult?.list && pageResult.list.length > 0) {
      const firstRowKeys = Object.keys(pageResult.list[0])
      
      try {
        const tableColumnsRes = await getTableColumns(queryForm.dataSourceId!, queryForm.tableName.trim())
        
        let dbColumns: any[] = []
        if (tableColumnsRes?.data && Array.isArray(tableColumnsRes.data)) {
          dbColumns = tableColumnsRes.data
        }
        
        if (dbColumns.length > 0) {
          const dataKeyMap = new Map<string, string>()
          firstRowKeys.forEach(key => {
            dataKeyMap.set(key.toLowerCase(), key)
          })
          
          const orderedColumns: Array<{title: string; key: string}> = []
          
          dbColumns.forEach((col: any) => {
            const dbColName = col.columnName || col.COLUMN_NAME || ''
            const matchedKey = dataKeyMap.get(dbColName.toLowerCase())
            if (matchedKey) {
              orderedColumns.push({
                title: matchedKey,
                key: matchedKey
              })
            }
          })
          
          const orderedKeys = new Set(orderedColumns.map(col => col.key))
          const additionalColumns = firstRowKeys
            .filter(key => !orderedKeys.has(key))
            .map(key => ({
              title: key,
              key: key
            }))
          
          columns.value = [...orderedColumns, ...additionalColumns]
        } else {
          columns.value = firstRowKeys.map(key => ({
            title: key,
            key: key
          }))
        }
      } catch {
        columns.value = firstRowKeys.map(key => ({
          title: key,
          key: key
        }))
      }
    } else {
      if (columns.value.length === 0) {
        columns.value = []
      }
      if (totalCount === 0 && columns.value.length === 0) {
        message.info('查询结果为空')
      }
    }
  } catch (error: any) {
    const errorMsg = handleApiError(error, '查询报表', '查询失败，请检查数据源配置和表名是否正确')
    message.error(errorMsg)
    if (tableData.value.length === 0) {
      total.value = 0
    }
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  queryForm.dataSourceId = null
  queryForm.tableName = ''
  tableData.value = []
  columns.value = []
  activeFilters.value = []
  paginationPage.value = DEFAULT_PAGE
  total.value = 0
}

const handleFilterApply = (filters: FilterCondition[]) => {
  activeFilters.value = filters
  paginationPage.value = 1
  if (queryForm.dataSourceId && queryForm.tableName) {
    handleQuery()
  }
}

const handlePageChange = (page: number) => {
  paginationPage.value = page
  handleQuery()
}

const handlePageSizeChange = (pageSize: number) => {
  paginationPageSize.value = pageSize
  paginationPage.value = 1
  handleQuery()
}

onMounted(() => {
  fetchDataSources()
})
</script>

<style scoped>
.report-page {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* 查询卡片 */
.query-card {
  margin-bottom: var(--dp-spacing-md);
}

.query-form-wrapper {
  padding: 0;
}

/* 数据卡片 */
.header-actions {
  display: flex;
  align-items: center;
  gap: var(--dp-spacing-sm);
  margin-left: auto;
}

.table-container {
  margin-top: var(--dp-spacing-sm);
  border-radius: var(--dp-radius-lg);
  overflow: hidden;
}

/* 表格样式 */
:deep(.smart-report-table) {
  font-size: var(--dp-font-sm);
}

:deep(.smart-report-table .n-data-table-th) {
  font-weight: 600;
  background: var(--dp-table-header-bg);
  color: var(--text-secondary);
  border-bottom: 1px solid var(--border-light);
  padding: 12px 16px;
  white-space: nowrap;
  font-size: var(--dp-font-sm);
}

:deep(.smart-report-table .n-data-table-td) {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-light);
  font-size: var(--dp-font-sm);
  color: var(--text-primary);
}

:deep(.smart-report-table .n-data-table-tr:hover .n-data-table-td) {
  background-color: var(--dp-table-hover-bg) !important;
}

/* 条纹行 */
:deep(.smart-report-table .n-data-table-tr--striped .n-data-table-td) {
  background-color: var(--dp-table-stripe-bg);
}

:deep(.smart-report-table .n-data-table-tr--striped:hover .n-data-table-td) {
  background-color: var(--dp-table-hover-bg) !important;
}

/* 滚动条 */
:deep(.n-data-table-base-table-header::-webkit-scrollbar),
:deep(.n-data-table-base-table-body::-webkit-scrollbar) {
  height: 8px;
  width: 8px;
}

:deep(.n-data-table-base-table-header::-webkit-scrollbar-track),
:deep(.n-data-table-base-table-body::-webkit-scrollbar-track) {
  background: var(--bg-tertiary);
  border-radius: var(--dp-radius-sm);
}

:deep(.n-data-table-base-table-header::-webkit-scrollbar-thumb),
:deep(.n-data-table-base-table-body::-webkit-scrollbar-thumb) {
  background: var(--border-default);
  border-radius: var(--dp-radius-sm);
}

:deep(.n-data-table-base-table-header::-webkit-scrollbar-thumb:hover),
:deep(.n-data-table-base-table-body::-webkit-scrollbar-thumb:hover) {
  background: var(--text-tertiary);
}

/* 空状态 */
:deep(.n-data-table-empty) {
  padding: 48px 0;
}

/* 响应式 - 移动端样式 */
@media (max-width: 768px) {
  .query-form {
    flex-direction: column;
    align-items: stretch;
  }
  
  :deep(.smart-report-table .n-data-table-th),
  :deep(.smart-report-table .n-data-table-td) {
    padding: 10px 12px;
    font-size: var(--dp-font-xs);
  }

  .pagination-wrapper {
    flex-direction: column;
    gap: 12px;
    align-items: center;
  }
  
  .pagination-info {
    order: 2;
  }
  
  :deep(.n-pagination) {
    flex-wrap: wrap;
    justify-content: center;
  }
  
  :deep(.n-pagination .n-pagination-prefix),
  :deep(.n-pagination .n-pagination-suffix) {
    display: none;
  }
}

/* 移动端专用样式 */
.is-mobile {
  padding: 12px;
}

.is-mobile .query-card,
.is-mobile .data-card {
  border-radius: var(--dp-radius-lg);
  margin-bottom: 12px;
}

.is-mobile .card-header-custom {
  justify-content: space-between;
}

.mobile-query-form {
  padding: 12px 0;
}

.mobile-form-actions {
  display: flex;
  gap: 12px;
  margin-top: var(--dp-spacing-md);
}

.mobile-form-actions .n-button {
  flex: 1;
  height: 44px;
  border-radius: 10px;
}

/* 移动端表格优化 */
.is-mobile .table-container {
  border-radius: var(--dp-radius-md);
  overflow: hidden;
}

.is-mobile :deep(.n-data-table-th),
.is-mobile :deep(.n-data-table-td) {
  padding: 8px 10px !important;
  font-size: var(--dp-font-xs) !important;
}

.is-mobile :deep(.n-data-table-th) {
  white-space: nowrap;
}

/* 移动端分页优化 */
.mobile-pagination {
  justify-content: center !important;
  padding: 12px 0 !important;
}

.mobile-pagination :deep(.n-pagination-item) {
  min-width: 32px;
  height: 32px;
}

/* 移动端数据卡片列表 */
.mobile-data-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mobile-data-card {
  background: #fff;
  border-radius: var(--dp-radius-lg);
  padding: var(--dp-spacing-md);
  border: 1px solid var(--border-light);
  box-shadow: var(--dp-shadow-sm);
}

.mobile-data-card:active {
  background: var(--bg-hover);
}

.mobile-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}

.mobile-card-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
}

.mobile-card-body {
  display: flex;
  flex-direction: column;
  gap: var(--dp-spacing-sm);
}

.mobile-card-row {
  display: flex;
  justify-content: space-between;
  font-size: var(--dp-font-sm);
}

.mobile-card-row .label {
  color: var(--text-tertiary);
  flex-shrink: 0;
  margin-right: 12px;
}

.mobile-card-row .value {
  color: var(--text-primary);
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}


</style>

<style>
/* Report 深色模式（非 scoped） */
html.dark .report-container { color: #e2e8f0 !important; }
</style>
