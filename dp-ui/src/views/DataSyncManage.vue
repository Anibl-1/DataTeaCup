<template>
  <div class="data-sync-page">
    <!-- 页面头部统计 -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><SyncOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ total }}</span>
          <span class="stat-label">同步任务总数</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><CheckmarkCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ runningCount }}</span>
          <span class="stat-label">运行中</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="24"><PauseCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ pausedCount }}</span>
          <span class="stat-label">已暂停</span>
        </div>
      </div>
    </div>

    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="22" color="var(--color-primary)" class="header-icon"><SyncOutline /></n-icon>
          <span>数据同步管理</span>
        </div>
      </template>
      <template #header-extra>
        <n-button type="primary" @click="modal.openCreate()">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          新建任务
        </n-button>
      </template>

      <!-- Query_Form: 搜索筛选 (Req 1.4) -->
      <n-form class="query-form" inline>
        <n-form-item>
          <n-input
            v-model:value="searchKeyword"
            placeholder="搜索任务名称..."
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearchReset"
          />
        </n-form-item>
        <n-form-item>
          <n-select
            v-model:value="searchStatus"
            :options="statusFilterOptions"
            placeholder="任务状态"
            clearable
            style="min-width: 140px"
            @update:value="handleSearch"
          />
        </n-form-item>
        <n-form-item class="query-form-actions">
          <n-button type="primary" @click="handleSearch">搜索</n-button>
          <n-button @click="handleSearchReset">重置</n-button>
        </n-form-item>
      </n-form>

      <n-data-table
        :columns="columns"
        :data="filteredData"
        :loading="loading"
        :pagination="false"
        :row-key="(row: any) => row.id"
        :scroll-x="1100"
        striped
        class="custom-table"
      />
      <div class="pagination-wrapper">
        <div class="pagination-info">
          <n-tag type="info" size="small" round>
            共 {{ total }} 条记录
          </n-tag>
        </div>
        <n-pagination
          :page="pagination.page"
          :page-size="pagination.pageSize"
          :item-count="pagination.itemCount"
          :page-sizes="[10, 20, 50]"
          show-size-picker
          show-quick-jumper
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </n-card>

    <!-- 新建/编辑任务弹窗 -->
    <n-modal v-model:show="modal.visible.value" preset="card" :title="modalTitle" style="width: 600px; border-radius: 16px;">
      <n-form ref="formRef" :model="modal.formData.value" :rules="formRules" label-placement="left" label-width="120px">
        <n-form-item label="任务名称" path="name">
          <n-input v-model:value="modal.formData.value.name" placeholder="请输入任务名称" />
        </n-form-item>
        <n-form-item label="源连接器类型" path="sourceConnectorType">
          <n-select v-model:value="modal.formData.value.sourceConnectorType" :options="connectorTypeOptions" placeholder="请选择源连接器类型" />
        </n-form-item>
        <n-form-item label="源表名" path="sourceTable">
          <n-input v-model:value="modal.formData.value.sourceTable" placeholder="请输入源表名" />
        </n-form-item>
        <n-form-item label="目标表名" path="targetTable">
          <n-input v-model:value="modal.formData.value.targetTable" placeholder="请输入目标表名" />
        </n-form-item>
        <n-form-item label="同步模式" path="syncMode">
          <n-select v-model:value="modal.formData.value.syncMode" :options="syncModeOptions" placeholder="请选择同步模式" />
        </n-form-item>
        <n-form-item label="增量字段" path="syncField">
          <n-input v-model:value="modal.formData.value.syncField" placeholder="增量模式下的时间戳或自增ID字段" />
        </n-form-item>
        <n-form-item label="批次大小" path="batchSize">
          <n-input-number v-model:value="modal.formData.value.batchSize" :min="100" :max="100000" placeholder="默认 1000" style="width: 100%;" />
        </n-form-item>
        <n-form-item label="Cron 表达式" path="cronExpression">
          <n-input v-model:value="modal.formData.value.cronExpression" placeholder="例如: 0 0 2 * * ? (每天凌晨2点)" />
        </n-form-item>
        <n-form-item>
          <n-space style="width: 100%; justify-content: flex-end;">
            <n-button @click="modal.close()">取消</n-button>
            <n-button type="primary" :loading="modal.submitting.value" @click="handleSubmit">
              {{ modal.mode.value === 'create' ? '创建' : '保存' }}
            </n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </n-modal>

    <!-- 同步日志抽屉 -->
    <n-drawer v-model:show="showLogDrawer" :width="580" placement="right">
      <n-drawer-content :title="`同步日志 - ${currentTaskName}`">
        <n-spin :show="logLoading">
          <n-empty v-if="!logLoading && syncLogs.length === 0" description="暂无同步日志" />
          <n-timeline v-else>
            <n-timeline-item
              v-for="log in syncLogs"
              :key="log.id"
              :type="logStatusType(log.status)"
              :title="logStatusLabel(log.status)"
              :time="formatDateTime(log.startTime)"
            >
              <div class="log-detail">同步模式: {{ log.syncMode === 'incremental' ? '增量' : '全量' }}</div>
              <div class="log-detail">同步行数: {{ log.syncedRows }}</div>
              <div class="log-detail">耗时: {{ log.durationMs }}ms</div>
              <div v-if="log.retryCount > 0" class="log-detail">重试次数: {{ log.retryCount }}</div>
              <div v-if="log.errorMessage" class="log-error">{{ log.errorMessage }}</div>
            </n-timeline-item>
          </n-timeline>
        </n-spin>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, h } from 'vue'
import { NTag, type FormInst } from 'naive-ui'
import { SyncOutline, AddOutline, PlayCircleOutline, StopCircleOutline, ListOutline, CreateOutline, TrashOutline, CheckmarkCircleOutline, PauseCircleOutline } from '@vicons/ionicons5'
import {
  getSyncTasks, createSyncTask, updateSyncTask, deleteSyncTask, startSyncTask, stopSyncTask, getSyncLogs,
  type SyncTask, type SyncExecutionLog
} from '@/api/dataSync'
import { formatDateTime } from '@/utils/format'
import { handleApiError } from '@/utils/error'
import { message } from '@/utils/message'
import { useCrudPage } from '@/composables/useCrudPage'
import StatusTag from '@/components/common/StatusTag.vue'
import ActionButtons, { type ActionConfig } from '@/components/common/ActionButtons.vue'

// --- 状态映射（用于 StatusTag）---
const syncStatusMap: Record<string, { label: string; type: 'success' | 'warning' | 'error' | 'info' | 'default' }> = {
  CREATED: { label: '已创建', type: 'default' },
  RUNNING: { label: '运行中', type: 'info' },
  PAUSED: { label: '已暂停', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  FAILED: { label: '失败', type: 'error' }
}

// --- 搜索筛选状态 ---
const searchKeyword = ref('')
const searchStatus = ref<string | null>(null)
const statusFilterOptions = [
  { label: '已创建', value: 'CREATED' },
  { label: '运行中', value: 'RUNNING' },
  { label: '已暂停', value: 'PAUSED' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '失败', value: 'FAILED' }
]

// --- 同步任务表单类型 ---
type SyncTaskForm = {
  id?: string
  name: string
  sourceConnectorType: string | null
  sourceTable: string
  targetTable: string
  syncMode: string
  syncField: string
  batchSize: number
  cronExpression: string
} & Record<string, unknown>

// --- 使用 useCrudPage 统一管理 CRUD (Req 3.7) ---
const {
  data, loading, total, pagination, modal, load,
  handlePageChange, handlePageSizeChange, handleDelete: crudHandleDelete
} = useCrudPage<SyncTask & Record<string, unknown>, SyncTaskForm>({
  listApi: async () => getSyncTasks(),
  createApi: async (formData) => {
    await createSyncTask({ ...formData, sourceConnectorType: formData.sourceConnectorType || '' })
  },
  updateApi: async (formData) => {
    await updateSyncTask({ ...formData, sourceConnectorType: formData.sourceConnectorType || '' })
  },
  deleteApi: (id: number | string) => deleteSyncTask(String(id)),
  defaultFormData: () => ({
    name: '', sourceConnectorType: null, sourceTable: '', targetTable: '',
    syncMode: 'incremental', syncField: '', batchSize: 1000, cronExpression: ''
  }),
  rowKey: 'id',
  defaultPageSize: 10,
  createSuccessMsg: '同步任务创建成功',
  updateSuccessMsg: '同步任务更新成功',
  deleteSuccessMsg: '同步任务已删除'
})

// --- 前端筛选（API 不支持搜索参数时的客户端过滤）---
const filteredData = computed(() => {
  let result = data.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    result = result.filter(t => t.name?.toLowerCase().includes(kw))
  }
  if (searchStatus.value) {
    result = result.filter(t => t.status === searchStatus.value)
  }
  return result
})

const runningCount = computed(() => data.value.filter(t => t.status === 'RUNNING').length)
const pausedCount = computed(() => data.value.filter(t => t.status === 'PAUSED').length)

const modalTitle = computed(() => modal.mode.value === 'create' ? '新建同步任务' : '编辑同步任务')
const formRef = ref<FormInst | null>(null)

const connectorTypeOptions = [
  { label: 'MySQL', value: 'mysql' }, { label: 'PostgreSQL', value: 'postgresql' },
  { label: 'Oracle', value: 'oracle' }, { label: 'SQL Server', value: 'sqlserver' },
  { label: 'CSV 文件', value: 'csv' }
]
const syncModeOptions = [
  { label: '增量同步', value: 'incremental' }, { label: '全量同步', value: 'full' }
]
const formRules = {
  name: { required: true, message: '请输入任务名称', trigger: 'blur' },
  sourceConnectorType: { required: true, message: '请选择源连接器类型', trigger: 'change' },
  sourceTable: { required: true, message: '请输入源表名', trigger: 'blur' },
  targetTable: { required: true, message: '请输入目标表名', trigger: 'blur' },
  syncMode: { required: true, message: '请选择同步模式', trigger: 'change' }
}

const handleSearch = () => { /* filteredData computed handles it reactively */ }
const handleSearchReset = () => {
  searchKeyword.value = ''
  searchStatus.value = null
}

// --- 同步日志 ---
const showLogDrawer = ref(false)
const logLoading = ref(false)
const syncLogs = ref<SyncExecutionLog[]>([])
const currentTaskName = ref('')

const logStatusType = (status: string): 'success' | 'error' | 'warning' => {
  const map: Record<string, 'success' | 'error' | 'warning'> = { SUCCESS: 'success', FAILED: 'error', PARTIAL: 'warning' }
  return map[status] || 'warning'
}
const logStatusLabel = (status: string): string => {
  const map: Record<string, string> = { SUCCESS: '同步成功', FAILED: '同步失败', PARTIAL: '部分成功' }
  return map[status] || status
}

// --- 表格行操作 (Req 3.7: CRUD + 启动/暂停) ---
const getRowActions = (row: SyncTask): ActionConfig[] => {
  const canStart = ['CREATED', 'PAUSED', 'FAILED', 'COMPLETED'].includes(row.status)
  const canStop = row.status === 'RUNNING'
  const actions: ActionConfig[] = []

  actions.push({
    label: '编辑',
    type: 'info',
    icon: CreateOutline,
    onClick: () => modal.openEdit(row as SyncTask & Record<string, unknown>)
  })

  if (canStart) {
    actions.push({ label: '启动', type: 'success', icon: PlayCircleOutline, confirm: `确定要启动同步任务「${row.name}」吗？`, onClick: () => handleStart(row) })
  }
  if (canStop) {
    actions.push({ label: '暂停', type: 'warning', icon: StopCircleOutline, confirm: `确定要暂停同步任务「${row.name}」吗？`, onClick: () => handleStop(row) })
  }

  actions.push({ label: '日志', type: 'info', icon: ListOutline, onClick: () => handleViewLogs(row) })
  actions.push({
    label: '删除',
    type: 'error',
    icon: TrashOutline,
    confirm: `确定要删除同步任务「${row.name}」吗？此操作不可恢复。`,
    onClick: () => crudHandleDelete(row.id)
  })

  return actions
}

// --- 表格列定义 ---
const columns = [
  { title: '任务名称', key: 'name', ellipsis: { tooltip: true }, render: (row: SyncTask) => row.name || '-' },
  { title: '源表', key: 'sourceTable', width: 150, ellipsis: { tooltip: true }, render: (row: SyncTask) => row.sourceTable || '-' },
  { title: '目标表', key: 'targetTable', width: 150, ellipsis: { tooltip: true }, render: (row: SyncTask) => row.targetTable || '-' },
  { title: '同步模式', key: 'syncMode', width: 100, render: (row: SyncTask) => h(NTag, { size: 'small', type: row.syncMode === 'incremental' ? 'info' : 'success' }, { default: () => row.syncMode === 'incremental' ? '增量' : '全量' }) },
  { title: '状态', key: 'status', width: 90, render: (row: SyncTask) => h(StatusTag, { status: row.status, statusMap: syncStatusMap }) },
  { title: '最近执行', key: 'lastRunAt', width: 170, render: (row: SyncTask) => formatDateTime(row.lastRunAt) },
  { title: '操作', key: 'actions', width: 280, fixed: 'right' as const, render: (row: SyncTask) => h(ActionButtons, { actions: getRowActions(row), row, maxVisible: 4 }) }
]

// --- 事件处理 ---
const handleSubmit = async () => {
  if (!formRef.value) return
  try { await formRef.value.validate(); await modal.submit() } catch { /* validation failed */ }
}

const handleStart = async (row: SyncTask) => {
  try { await startSyncTask(row.id); message.success('任务已启动'); load() }
  catch (error) { message.error(handleApiError(error, '启动同步任务')) }
}

const handleStop = async (row: SyncTask) => {
  try { await stopSyncTask(row.id); message.success('任务已暂停'); load() }
  catch (error) { message.error(handleApiError(error, '暂停同步任务')) }
}

const handleViewLogs = async (row: SyncTask) => {
  currentTaskName.value = row.name
  showLogDrawer.value = true
  logLoading.value = true
  try {
    const res = await getSyncLogs(row.id)
    const resData = (res as any).data || res
    syncLogs.value = Array.isArray(resData) ? resData : []
  } catch (error) { message.error(handleApiError(error, '获取同步日志')) }
  finally { logLoading.value = false }
}
</script>

<style scoped>
.data-sync-page {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* stat-icon colors now use global gradient styles from style.css */

.main-card {
  border-radius: var(--dp-card-radius);
}

.log-detail { font-size: var(--dp-font-sm); color: var(--text-secondary); }
.log-error { font-size: var(--dp-font-sm); color: var(--color-error, #EF4444); margin-top: var(--dp-spacing-xs); }

@media (max-width: 768px) {
  .page-header-stats { flex-direction: row; overflow-x: auto; flex-wrap: nowrap; }
  .page-header-stats .stat-item { min-width: 140px; flex: 0 0 auto; }
  .main-card { border-radius: 14px !important; }
}
</style>
