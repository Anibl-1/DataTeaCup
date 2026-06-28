<template>
  <div class="tab-content">
    <div v-if="currentTable" class="toolbar">
      <span class="toolbar-hint">ℹ️ 默认显示前100条数据</span>
      <n-input v-model:value="whereClause" placeholder="WHERE条件 (例如: id > 10)" style="width: 250px;" />
      <n-select v-model:value="pageSize" :options="pageSizeOptions" style="width: 120px;" />
      <n-button type="primary" @click="loadTableData">🔍 查询</n-button>
      <n-button @click="whereClause = ''; loadTableData()">✕ 清除</n-button>
    </div>
    <div v-if="!currentTable" class="empty-content">
      <div class="empty-icon">👈</div>
      <h3>请从左侧选择一个表</h3>
      <p>选择表后可以查看数据（默认显示前100条）</p>
    </div>
    <div v-else class="query-result">
      <div class="result-header">
        <span class="result-title">📋 {{ currentTable }}</span>
        <span class="result-count">共 {{ tableDataTotal }} 条记录{{ tableDataTotal > tableDataList.length ? `，当前显示前 ${tableDataList.length} 条` : '' }}</span>
      </div>
      <n-spin :show="loadingData">
        <n-data-table :columns="dataColumns" :data="tableDataList" :max-height="400" size="small" striped :scroll-x="dataColumns.length * 150" />
      </n-spin>
    </div>

    <!-- SQL历史标签页内容 -->
    <slot name="history" />
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, watch, h } from 'vue'
import { useMessage } from 'naive-ui'
import * as dbManagerApi from '@/api/dbManager'

const props = defineProps<{
  sessionId: string | null
  currentTable: string | null
}>()

const message = useMessage()

const whereClause = ref('')
const pageSize = ref(100)
const pageSizeOptions = [
  { label: '50条/页', value: 50 },
  { label: '100条/页', value: 100 },
  { label: '200条/页', value: 200 },
  { label: '500条/页', value: 500 },
]
const loadingData = ref(false)
const tableDataList = ref<any[]>([])
const tableDataTotal = ref(0)
const dataColumns = ref<any[]>([])

const loadTableData = async () => {
  if (!props.sessionId || !props.currentTable) return
  loadingData.value = true
  try {
    const res = await dbManagerApi.queryTableData(props.sessionId, props.currentTable, 1, pageSize.value, whereClause.value || undefined)
    const data = res.data
    tableDataList.value = data?.data || []
    tableDataTotal.value = data?.total || 0
    dataColumns.value = (data?.columns || []).map((col: string) => ({
      title: col,
      key: col,
      width: 120,
      ellipsis: { tooltip: true },
      render: (row: any) => {
        const val = row[col]
        if (val === null || val === undefined) return h('span', { style: 'color: #999; font-style: italic;' }, 'NULL')
        return String(val).length > 50 ? String(val).substring(0, 50) + '...' : String(val)
      },
    }))
  } catch {
    message.error('加载表数据失败')
  } finally {
    loadingData.value = false
  }
}

// 当 currentTable 变化时自动加载数据
watch(() => props.currentTable, (val) => {
  if (val) loadTableData()
})

defineExpose({ loadTableData })
</script>

<style scoped>
.tab-content { flex: 1; padding: 20px; overflow: auto; }

/* 工具栏 */
.toolbar { display: flex; gap: var(--dp-spacing-sm); margin-bottom: 15px; padding: 15px; background: var(--bg-secondary); border-radius: var(--dp-radius-md); align-items: center; flex-wrap: wrap; }
.toolbar-hint { color: #666; font-size: var(--dp-font-sm); }

/* 空内容 */
.empty-content { text-align: center; color: #999; padding: 60px 20px; }
.empty-content .empty-icon { font-size: 48px; margin-bottom: 20px; }
.empty-content h3 { margin: 0 0 10px; color: #666; }
.empty-content p { margin: 0; }

/* 查询结果 */
.result-header { margin-bottom: 15px; display: flex; align-items: center; gap: var(--dp-spacing-sm); }
.result-title { font-weight: 600; color: #333; }
.result-count { color: #666; font-size: var(--dp-font-sm); }

</style>

<style>
/* DbQueryResult 深色模式（非 scoped） */
html.dark .result-header { background: #1a2536 !important; color: #e2e8f0 !important; }
html.dark .result-info { color: #94a3b8 !important; }
html.dark .no-data { color: #64748b !important; }
html.dark .error-msg { color: #64748b !important; }
</style>
