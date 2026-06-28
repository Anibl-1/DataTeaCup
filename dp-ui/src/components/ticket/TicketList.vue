<template>
  <div class="ticket-list">
    <n-card title="工单列表">
      <template #header-extra>
        <n-space>
          <n-select
            v-model:value="filters.status"
            :options="statusOptions"
            placeholder="状态"
            clearable
            style="width: 120px"
            @update:value="loadTickets"
          />
          <n-select
            v-model:value="filters.category"
            :options="categoryOptions"
            placeholder="分类"
            clearable
            style="width: 120px"
            @update:value="loadTickets"
          />
        </n-space>
      </template>

      <n-data-table
        :columns="columns"
        :data="tickets"
        :loading="loading"
        :row-key="(row: any) => row.id"
      />
    </n-card>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, onMounted, h } from 'vue'
import { NCard, NDataTable, NSelect, NSpace, NTag, NButton } from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import request from '@/api/request'

const loading = ref(false)
const tickets = ref<any[]>([])

const filters = reactive({
  status: null as string | null,
  category: null as string | null
})

const statusOptions = [
  { label: '待处理', value: 'pending' },
  { label: '处理中', value: 'processing' },
  { label: '已解决', value: 'resolved' },
  { label: '已关闭', value: 'closed' }
]

const categoryOptions = [
  { label: 'Bug报告', value: 'bug' },
  { label: '功能建议', value: 'feature_request' },
  { label: '使用咨询', value: 'consultation' }
]

const priorityMap: Record<string, { label: string; type: 'default' | 'info' | 'warning' | 'error' }> = {
  low: { label: '低', type: 'default' },
  medium: { label: '中', type: 'info' },
  high: { label: '高', type: 'warning' },
  urgent: { label: '紧急', type: 'error' }
}

const statusMap: Record<string, { label: string; type: 'default' | 'info' | 'warning' | 'success' }> = {
  pending: { label: '待处理', type: 'default' },
  processing: { label: '处理中', type: 'info' },
  resolved: { label: '已解决', type: 'success' },
  closed: { label: '已关闭', type: 'warning' }
}

const emit = defineEmits<{ (e: 'select', ticket: any): void }>()

const columns: DataTableColumns<any> = [
  { title: '工单号', key: 'ticketNo', width: 120 },
  { title: '标题', key: 'title', ellipsis: { tooltip: true } },
  {
    title: '优先级', key: 'priority', width: 80,
    render: (row) => {
      const p = priorityMap[row.priority] || { label: row.priority, type: 'default' as const }
      return h(NTag, { type: p.type, size: 'small' }, { default: () => p.label })
    }
  },
  {
    title: '状态', key: 'status', width: 80,
    render: (row) => {
      const s = statusMap[row.status] || { label: row.status, type: 'default' as const }
      return h(NTag, { type: s.type, size: 'small' }, { default: () => s.label })
    }
  },
  { title: '提交人', key: 'submitterName', width: 100 },
  { title: '处理人', key: 'assigneeName', width: 100 },
  { title: '创建时间', key: 'createTime', width: 160 },
  {
    title: '操作', key: 'actions', width: 80,
    render: (row) => h(NButton, { size: 'small', onClick: () => emit('select', row) }, { default: () => '查看' })
  }
]

async function loadTickets() {
  loading.value = true
  try {
    const params: any = { page: 0, size: 50 }
    if (filters.status) params.status = filters.status
    if (filters.category) params.category = filters.category
    const { data } = await request.get('/api/tickets', { params })
    tickets.value = data || []
  } catch {
    tickets.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadTickets)

defineExpose({ loadTickets })
</script>
