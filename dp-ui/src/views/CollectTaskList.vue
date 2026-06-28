<template>
  <div>
    <!-- 页面头部统计 -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><DocumentOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ totalCount }}</span>
          <span class="stat-label">任务总数</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><PlayCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ runningCount }}</span>
          <span class="stat-label">运行中</span>
        </div>
        <div v-if="runningCount > 0" class="stat-badge stat-badge-success">
          <span class="pulse-dot"></span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="24"><TimeOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ scheduledCount }}</span>
          <span class="stat-label">定时任务</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-error">
          <n-icon size="24"><WarningOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ errorCount }}</span>
          <span class="stat-label">异常任务</span>
        </div>
        <div v-if="errorCount > 0" class="stat-badge stat-badge-error">
          <n-icon size="12"><AlertCircleOutline /></n-icon>
        </div>
      </div>
    </div>

    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <div class="header-icon-wrapper">
            <n-icon size="18"><CloudDownloadOutline /></n-icon>
          </div>
          <div class="header-text">
            <span class="header-title">数据采集</span>
            <span class="header-subtitle">管理数据采集任务与导入</span>
          </div>
        </div>
      </template>
      <template #header-extra>
        <n-space>
          <FilterPanel
            :fields="filterFields"
            :model-value="activeFilters"
            @apply="handleFilterApply"
          />
          <n-button v-permission="'data:import'" type="info" @click="$emit('showImport')">
            <template #icon><n-icon><CloudUploadOutline /></n-icon></template>
            导入数据
          </n-button>
          <n-button v-permission="'data:collect:add'" type="primary" @click="$emit('create')">
            <template #icon><n-icon><AddOutline /></n-icon></template>
            新建采集任务
          </n-button>
          <n-button v-permission="'data:collect:debug'" @click="$emit('debugScheduler')">
            调试定时
          </n-button>
        </n-space>
      </template>

      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="false"
        :row-props="getRowProps"
        :scroll-x="1200"
        striped
        class="custom-table"
      />
      <div class="pagination-wrapper">
        <div class="pagination-info">
          <n-tag type="info" size="small" round>
            共 {{ totalCount }} 条记录
          </n-tag>
        </div>
        <n-pagination
          v-model:page="currentPage"
          v-model:page-size="currentPageSize"
          :item-count="totalCount"
          :page-sizes="PAGE_SIZES"
          show-size-picker
          show-quick-jumper
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import { NButton, NTag, NIcon, NSpace } from 'naive-ui'
import {
  AddOutline,
  CloudUploadOutline,
  DocumentOutline,
  PlayCircleOutline,
  WarningOutline,
  EyeOutline,
  TimeOutline,
  AlertCircleOutline,
  CloudDownloadOutline,
  CreateOutline
} from '@vicons/ionicons5'
import { PAGE_SIZES, TASK_STATUS_MAP } from '@/constants'
import type { CollectTask } from '@/types/collectTask'
import type { FilterCondition } from '@/types/api'
import { formatDateTime } from '@/utils/format'
import { hasPermission } from '@/utils/permission'
import FilterPanel, { type FilterField } from '@/components/FilterPanel.vue'

const props = defineProps<{
  tableData: CollectTask[]
  loading: boolean
  totalCount: number
  page: number
  pageSize: number
  activeFilters: FilterCondition[]
}>()

const emit = defineEmits<{
  (e: 'create'): void
  (e: 'edit', row: CollectTask): void
  (e: 'viewDetail', row: CollectTask): void
  (e: 'showImport'): void
  (e: 'debugScheduler'): void
  (e: 'pageChange', page: number): void
  (e: 'pageSizeChange', pageSize: number): void
  (e: 'filterApply', filters: FilterCondition[]): void
}>()

const currentPage = computed({
  get: () => props.page,
  set: (val: number) => emit('pageChange', val)
})

const currentPageSize = computed({
  get: () => props.pageSize,
  set: (val: number) => emit('pageSizeChange', val)
})

// 统计信息
const taskStats = computed(() => {
  const stats = { running: 0, error: 0, scheduled: 0 }
  props.tableData.forEach(task => {
    if (task.status === 'running') stats.running++
    else if (task.status === 'error') stats.error++
    if (task.scheduleEnabled) stats.scheduled++
  })
  return stats
})

const runningCount = computed(() => taskStats.value.running)
const errorCount = computed(() => taskStats.value.error)
const scheduledCount = computed(() => taskStats.value.scheduled)

const filterFields: FilterField[] = [
  { label: 'ID', value: 'id', type: 'number' },
  { label: '任务名称', value: 'taskName', type: 'string' },
  { label: '数据源', value: 'dataSourceName', type: 'string' },
  { label: '采集表', value: 'tableName', type: 'string' },
  { label: '状态', value: 'status', type: 'string' },
  { label: '创建时间', value: 'createTime', type: 'string' }
]

const columns = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '任务名称', key: 'taskName', ellipsis: { tooltip: true } },
  { title: '数据源', key: 'dataSourceName', ellipsis: { tooltip: true } },
  {
    title: '采集表/模式', key: 'tableName', width: 150, ellipsis: { tooltip: true },
    render: (row: CollectTask) => {
      if (row.collectMode === 'custom') return h(NTag, { type: 'info', size: 'small' }, { default: () => '自定义SQL' })
      return row.tableName || '-'
    }
  },
  {
    title: '状态', key: 'status', width: 100,
    render: (row: CollectTask) => {
      const status = TASK_STATUS_MAP[row.status] || TASK_STATUS_MAP.stopped
      return h(NTag, { type: status.type, size: 'small' }, { default: () => status.text })
    }
  },
  {
    title: '定时任务', key: 'scheduleEnabled', width: 120,
    render: (row: CollectTask) => {
      if (!row.scheduleEnabled) return h(NTag, { type: 'default', size: 'small' }, { default: () => '未启用' })
      return h(NSpace, { vertical: true, size: 2 }, {
        default: () => [
          h(NTag, { type: 'success', size: 'small' }, {
            default: () => '已启用',
            icon: () => h(NIcon, { size: 12 }, { default: () => h(TimeOutline) })
          }),
          row.scheduleDescription ? h('span', { style: 'font-size: 11px; color: #999;' }, row.scheduleDescription) : null
        ]
      })
    }
  },
  {
    title: '执行统计', key: 'executeCount', width: 140,
    render: (row: CollectTask) => {
      const total = row.executeCount || 0
      const success = row.successCount || 0
      const fail = row.failCount || 0
      if (total === 0) return h('span', { style: 'color: #999; font-size: 12px;' }, '暂无执行记录')
      return h(NSpace, { size: 4 }, {
        default: () => [
          h(NTag, { type: 'info', size: 'small', round: true }, { default: () => `共${total}` }),
          h(NTag, { type: 'success', size: 'small', round: true }, { default: () => `成功${success}` }),
          fail > 0 ? h(NTag, { type: 'error', size: 'small', round: true }, { default: () => `失败${fail}` }) : null
        ]
      })
    }
  },
  {
    title: '最后执行', key: 'lastExecuteTime', width: 160,
    render: (row: CollectTask) => {
      if (!row.lastExecuteTime) return h('span', { style: 'color: #999;' }, '未执行')
      return h(NSpace, { vertical: true, size: 0 }, {
        default: () => [
          h('span', { style: 'font-size: 12px;' }, formatDateTime(row.lastExecuteTime)),
          row.lastExecuteResult ? h('span', {
            style: `font-size: 11px; color: ${row.lastExecuteResult.includes('失败') ? '#d03050' : '#18a058'};`
          }, row.lastExecuteResult.substring(0, 20) + (row.lastExecuteResult.length > 20 ? '...' : '')) : null
        ]
      })
    }
  },
  {
    title: '操作', key: 'actions', width: 150, fixed: 'right' as const,
    render: (row: CollectTask) => {
      const buttons = []
      buttons.push(
        h(NButton, { size: 'small', quaternary: true, onClick: () => emit('viewDetail', row) }, {
          default: () => '详情',
          icon: () => h(NIcon, null, { default: () => h(EyeOutline) })
        })
      )
      if (hasPermission('data:collect:edit')) {
        buttons.push(
          h(NButton, { size: 'small', quaternary: true, type: 'info', onClick: () => emit('edit', row) }, {
            default: () => '编辑',
            icon: () => h(NIcon, null, { default: () => h(CreateOutline) })
          })
        )
      }
      return h('div', { style: 'display: flex; gap: 8px; flex-wrap: wrap;' }, buttons)
    }
  }
]

const getRowProps = (row: CollectTask) => ({
  style: { cursor: 'pointer' },
  onClick: () => emit('viewDetail', row)
})

const handlePageChange = (page: number) => emit('pageChange', page)
const handlePageSizeChange = (pageSize: number) => emit('pageSizeChange', pageSize)
const handleFilterApply = (filters: FilterCondition[]) => emit('filterApply', filters)
</script>

<style scoped>

.stat-badge {
  position: absolute; top: 12px; right: 12px;
  display: flex; align-items: center; justify-content: center;
}
.stat-badge-success .pulse-dot {
  width: 10px; height: 10px; background: var(--color-success);
  border-radius: 50%; animation: pulse 2s infinite;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.5);
}
.stat-badge-error { color: var(--color-error); animation: shake 0.5s ease-in-out infinite; }

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(1.1); }
}
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-2px); }
  75% { transform: translateX(2px); }
}

.main-card { border-radius: var(--radius-lg); }

/* ========== 卡片头部增强 ========== */
.header-icon-wrapper {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #fff;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.25);
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-subtitle {
  font-size: 12px;
  color: var(--text-tertiary);
}

@media (max-width: 768px) {
  .page-header-stats { flex-direction: row; overflow-x: auto; flex-wrap: nowrap; }
  .page-header-stats .stat-item { min-width: 140px; flex: 0 0 auto; }
  .main-card { border-radius: 14px !important; }
}
</style>

<style>
/* CollectTaskList 深色模式（非 scoped） */
html.dark .header-icon-wrapper { box-shadow: 0 2px 8px rgba(16, 185, 129, 0.15) !important; }
html.dark .header-title { color: #f1f5f9 !important; }
html.dark .header-subtitle { color: #64748b !important; }
</style>
