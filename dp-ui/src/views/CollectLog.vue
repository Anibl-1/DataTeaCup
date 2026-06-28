<template>
  <div class="collect-log-page">
    <!-- Page_Header_Stats: 采集日志统计 (Req 1.1, 15.3) -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="22"><ListOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ pagination.itemCount }}</span>
          <span class="stat-label">日志总数</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="22"><CheckmarkCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ successCount }}</span>
          <span class="stat-label">采集成功</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-error">
          <n-icon size="22"><CloseCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ failedCount }}</span>
          <span class="stat-label">采集失败</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="22"><TimeOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ runningCount }}</span>
          <span class="stat-label">运行中</span>
        </div>
      </div>
    </div>

    <!-- Main_Card: 采集日志列表 (Req 1.1, 15.3) -->
    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="22" color="var(--color-primary)" class="header-icon"><DocumentTextOutline /></n-icon>
          <span>采集日志</span>
        </div>
      </template>
      <template #header-extra>
        <n-button quaternary circle :loading="loading" @click="handleRefresh">
          <template #icon><n-icon size="18"><RefreshOutline /></n-icon></template>
        </n-button>
      </template>

      <!-- Query_Form: 搜索筛选 (Req 15.3) -->
      <n-form class="query-form" inline>
        <n-form-item>
          <n-select
            v-model:value="searchParams.taskId"
            :options="taskOptions"
            placeholder="选择任务"
            clearable
            style="width: 200px;"
          />
        </n-form-item>
        <n-form-item>
          <n-select
            v-model:value="searchParams.status"
            :options="statusOptions"
            placeholder="执行状态"
            clearable
            style="width: 150px;"
          />
        </n-form-item>
        <n-form-item>
          <n-date-picker
            v-model:value="searchParams.dateRange"
            type="daterange"
            clearable
            placeholder="选择日期范围"
          />
        </n-form-item>
        <n-form-item class="query-form-actions">
          <n-button type="primary" @click="handleSearch">搜索</n-button>
          <n-button @click="handleReset">重置</n-button>
        </n-form-item>
      </n-form>

      <!-- 数据表格 -->
      <n-data-table
        :columns="columns"
        :data="logList"
        :loading="loading"
        :pagination="false"
        :row-key="rowKey"
        :scroll-x="1100"
        remote
        striped
        class="custom-table"
      />

      <!-- 空状态 (Req 16.7) -->
      <n-empty v-if="!loading && logList.length === 0" description="暂无采集日志数据" style="margin: 32px 0;" />

      <!-- Pagination_Wrapper -->
      <div class="pagination-wrapper">
        <div class="pagination-info">
          <n-tag type="info" size="small" round>共 {{ pagination.itemCount }} 条记录</n-tag>
        </div>
        <n-pagination
          v-model:page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :item-count="pagination.itemCount"
          :page-sizes="PAGE_SIZES"
          show-size-picker
          show-quick-jumper
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </n-card>

    <!-- 日志详情弹窗 -->
    <n-modal v-model:show="showDetail" preset="card" title="日志详情" style="width: 700px; border-radius: 16px;">
      <div v-if="currentLog" class="log-detail">
        <n-descriptions :column="2" label-placement="left" bordered>
          <n-descriptions-item label="任务名称">{{ currentLog.taskName }}</n-descriptions-item>
          <n-descriptions-item label="执行状态">
            <n-tag :type="getStatusType(currentLog.status)" size="small">
              {{ getStatusText(currentLog.status) }}
            </n-tag>
          </n-descriptions-item>
          <n-descriptions-item label="开始时间">{{ currentLog.startTime }}</n-descriptions-item>
          <n-descriptions-item label="结束时间">{{ currentLog.endTime || '-' }}</n-descriptions-item>
          <n-descriptions-item label="执行耗时">{{ currentLog.duration || '-' }}</n-descriptions-item>
          <n-descriptions-item label="采集行数">{{ currentLog.rowCount || 0 }}</n-descriptions-item>
          <n-descriptions-item label="源表" :span="2">{{ currentLog.sourceTable }}</n-descriptions-item>
          <n-descriptions-item label="目标表" :span="2">{{ currentLog.targetTable }}</n-descriptions-item>
        </n-descriptions>

        <div v-if="currentLog.errorMessage" class="error-section">
          <div class="error-title">
            <n-icon color="#f5222d"><CloseCircleOutline /></n-icon>
            错误信息
          </div>
          <n-code :code="currentLog.errorMessage" language="text" />
        </div>

        <div v-if="currentLog.executeSql" class="sql-section">
          <div class="sql-title">执行SQL</div>
          <n-code :code="currentLog.executeSql" language="sql" />
        </div>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { logger } from '@/utils/logger'
import { ref, reactive, onMounted, h, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useMessage, NTag, NButton, NIcon } from 'naive-ui'
import {
  ListOutline, RefreshOutline, CheckmarkCircleOutline,
  CloseCircleOutline, TimeOutline, EyeOutline, DocumentTextOutline
} from '@vicons/ionicons5'
import { getCollectTaskList, getCollectLogList, type CollectLog } from '@/api/dataCollect'
import { initMessage } from '@/utils/message'
import { formatDateTime } from '@/utils/format'
import { PAGE_SIZES } from '@/constants'

const route = useRoute()
const message = useMessage()
initMessage(message)

const loading = ref(false)
const rowKey = (row: Record<string, unknown>) => row['id']
const logList = ref<CollectLog[]>([])
const taskOptions = ref<Array<{ label: string; value: number }>>([])
const showDetail = ref(false)
const currentLog = ref<CollectLog | null>(null)

const searchParams = reactive({
  taskId: null as number | null,
  status: null as string | null,
  dateRange: null as [number, number] | null
})

const statusOptions = [
  { label: '成功', value: 'success' },
  { label: '失败', value: 'failed' },
  { label: '运行中', value: 'running' }
]

const pagination = reactive({
  page: 1,
  pageSize: 20,
  itemCount: 0
})

const successCount = computed(() => logList.value.filter(r => r.status === 'success').length)
const failedCount = computed(() => logList.value.filter(r => r.status === 'failed').length)
const runningCount = computed(() => logList.value.filter(r => r.status === 'running').length)

const getStatusType = (status: string) => {
  const map: Record<string, 'success' | 'error' | 'warning' | 'info'> = { success: 'success', failed: 'error', running: 'warning' }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = { success: '成功', failed: '失败', running: '运行中' }
  return map[status] || status
}

const columns = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '任务名称', key: 'taskName', width: 150, ellipsis: { tooltip: true } },
  { title: '源表', key: 'sourceTable', width: 150, ellipsis: { tooltip: true } },
  { title: '目标表', key: 'targetTable', width: 150, ellipsis: { tooltip: true } },
  { title: '采集行数', key: 'rowCount', width: 100 },
  {
    title: '执行状态', key: 'status', width: 100,
    render: (row: CollectLog) => h(NTag, { type: getStatusType(row.status), size: 'small' },
      {
        default: () => getStatusText(row.status),
        icon: () => h(NIcon, null, () => {
          if (row.status === 'success') return h(CheckmarkCircleOutline)
          if (row.status === 'failed') return h(CloseCircleOutline)
          return h(TimeOutline)
        })
      })
  },
  {
    title: '开始时间', key: 'startTime', width: 180,
    render: (row: CollectLog) => formatDateTime(row.startTime)
  },
  {
    title: '耗时', key: 'duration', width: 100,
    render: (row: CollectLog) => {
      if (!row.duration) return '-'
      if (row.duration < 1000) return row.duration + 'ms'
      return (row.duration / 1000).toFixed(1) + 's'
    }
  },
  {
    title: '操作', key: 'actions', width: 80,
    render: (row: CollectLog) => h(NButton, { size: 'small', quaternary: true, onClick: () => handleViewDetail(row) },
      { default: () => '详情', icon: () => h(NIcon, null, () => h(EyeOutline)) })
  }
]

const loadTasks = async () => {
  try {
    const res = await getCollectTaskList({ page: 1, pageSize: 100 })
    const list = res.data?.list || []
    taskOptions.value = list.map((task: any) => ({ label: task.taskName, value: task.id }))
  } catch (error) {
    logger.error('加载任务列表失败:', error)
  }
}

const loadLogs = async () => {
  loading.value = true
  try {
    const params: any = { page: pagination.page, pageSize: pagination.pageSize }
    if (searchParams.taskId) params.taskId = searchParams.taskId
    if (searchParams.status) params.status = searchParams.status
    if (searchParams.dateRange && searchParams.dateRange.length === 2) {
      params.startDate = new Date(searchParams.dateRange[0]).toISOString().split('T')[0]
      params.endDate = new Date(searchParams.dateRange[1]).toISOString().split('T')[0]
    }
    const res = await getCollectLogList(params)
    logList.value = res.data?.list || []
    pagination.itemCount = res.data?.total || 0
  } catch (error: any) {
    message.error(error.message || '加载日志失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.page = 1; loadLogs() }
const handleReset = () => {
  searchParams.taskId = null
  searchParams.status = null
  searchParams.dateRange = null
  pagination.page = 1
  loadLogs()
}
const handleRefresh = () => { loadLogs() }
const handlePageChange = (page: number) => { pagination.page = page; loadLogs() }
const handlePageSizeChange = (pageSize: number) => { pagination.pageSize = pageSize; pagination.page = 1; loadLogs() }
const handleViewDetail = (row: CollectLog) => { currentLog.value = row; showDetail.value = true }

onMounted(() => {
  const taskIdParam = route.query['taskId']
  if (taskIdParam) searchParams.taskId = Number(taskIdParam)
  loadTasks()
  loadLogs()
})
</script>

<style scoped>
.collect-log-page {
  animation: fadeIn 0.3s ease-out;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
.log-detail { padding: 8px 0; }
.error-section {
  margin-top: 16px;
  padding: 12px;
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 6px;
}
.error-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  color: #f5222d;
  margin-bottom: 8px;
}
.sql-section { margin-top: 16px; }
.sql-title { font-weight: 500; margin-bottom: 8px; color: #666; }

</style>

<style>
/* CollectLog 深色模式（非 scoped） */
html.dark .error-row { background: rgba(239, 68, 68, 0.08) !important; }
html.dark .error-text { color: #f87171 !important; }
html.dark .log-time { color: #64748b !important; }
</style>
