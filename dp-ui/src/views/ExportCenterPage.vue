<template>
  <div class="export-center-page">
    <!-- Page_Header_Stats: 导出任务统计 (Req 1.1) -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="22"><DownloadOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ pagination.itemCount }}</span>
          <span class="stat-label">{{ t('exportCenter.totalTasks') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="22"><CheckmarkCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ completedCount }}</span>
          <span class="stat-label">{{ t('exportCenter.completed') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="22"><SyncOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ pendingCount }}</span>
          <span class="stat-label">{{ t('exportCenter.inProgress') }}</span>
        </div>
      </div>
    </div>

    <!-- Main_Card (Req 1.1) -->
    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="20" color="var(--color-primary)" class="header-icon"><DownloadOutline /></n-icon>
          <span>{{ t('exportCenter.title') }}</span>
        </div>
      </template>

      <!-- Query_Form: 搜索筛选 (Req 1.4) -->
      <n-form class="query-form" inline>
        <n-form-item>
          <n-input
            v-model:value="searchForm.taskName"
            :placeholder="t('exportCenter.searchPlaceholder')"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
            @clear="handleReset"
          />
        </n-form-item>
        <n-form-item>
          <n-date-picker
            v-model:value="searchForm.dateRange"
            type="daterange"
            clearable
            :default-time="['00:00:00', '23:59:59']"
            format="yyyy-MM-dd"
            :start-placeholder="t('exportCenter.startDate')"
            :end-placeholder="t('exportCenter.endDate')"
            style="width: 280px"
          />
        </n-form-item>
        <n-form-item class="query-form-actions">
          <n-button type="primary" @click="handleSearch">
            <template #icon><n-icon><SearchOutline /></n-icon></template>
            {{ t('exportCenter.search') }}
          </n-button>
          <n-button @click="handleReset">{{ t('exportCenter.reset') }}</n-button>
        </n-form-item>
      </n-form>

      <!-- Empty State (Req 16.7) -->
      <n-empty v-if="!loading && tasks.length === 0" :description="t('exportCenter.noTasks')" style="margin: 32px 0;" />

      <n-data-table
        v-else
        :columns="columns"
        :data="tasks"
        :loading="loading"
        :pagination="false"
        :row-key="(row: ExportTask) => row.id"
        :scroll-x="1100"
        remote
        striped
        class="custom-table"
      />

      <!-- Pagination_Wrapper (Req 1.5) -->
      <div class="pagination-wrapper">
        <div class="pagination-info">
          <n-tag type="info" size="small" round>
            {{ t('exportCenter.totalRecords', { n: pagination.itemCount }) }}
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
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { logger } from '@/utils/logger'
import { ref, h, onMounted, onUnmounted, reactive, computed } from 'vue'
import { NButton, NTag, NProgress, NIcon, NSpace, NTooltip, useMessage } from 'naive-ui'
import { DownloadOutline, TrashOutline, CheckmarkCircleOutline, CloseCircleOutline, TimeOutline, SyncOutline, SearchOutline } from '@vicons/ionicons5'
import { 
  getExportTaskList, 
  downloadExportFile, 
  deleteExportTask,
  type ExportTask 
} from '@/api/exportTask'
import { useI18n } from '@/i18n'

const { t } = useI18n()
const message = useMessage()
const loading = ref(false)
const tasks = ref<ExportTask[]>([])

// 统计数据
const completedCount = computed(() => tasks.value.filter(t => t.status === 2).length)
const pendingCount = computed(() => tasks.value.filter(t => t.status === 0 || t.status === 1).length)

// 格式化日期时间
const formatDateTime = (dateValue: any): string => {
  if (!dateValue) return '-'
  try {
    // 处理数组格式 [2024, 1, 25, 17, 30, 0]
    if (Array.isArray(dateValue)) {
      const [year, month, day, hour = 0, minute = 0, second = 0] = dateValue
      return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')} ${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:${String(second).padStart(2, '0')}`
    }
    // 处理字符串格式
    if (typeof dateValue === 'string') {
      // 替换T为空格，处理ISO格式
      const normalized = dateValue.replace('T', ' ').split('.')[0] ?? dateValue
      return normalized
    }
    // 处理时间戳
    if (typeof dateValue === 'number') {
      const date = new Date(dateValue)
      return date.toLocaleString('zh-CN', { hour12: false })
    }
    return '-'
  } catch {
    return '-'
  }
}

// 获取默认日期范围（近3天）
const getDefaultDateRange = (): [number, number] => {
  const end = new Date()
  end.setHours(23, 59, 59, 999)
  const start = new Date()
  start.setDate(start.getDate() - 2)
  start.setHours(0, 0, 0, 0)
  return [start.getTime(), end.getTime()]
}

// 搜索表单
const searchForm = reactive({
  taskName: '',
  dateRange: getDefaultDateRange() as [number, number] | null
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

let pollTimer: number | null = null
// 浏览器通知权限
const notificationPermission = ref<'default' | 'granted' | 'denied'>('default')
const previousTaskStatuses = ref<Map<number, number>>(new Map())

const columns = computed(() => [
  {
    title: t('exportCenter.taskName'),
    key: 'taskName',
    width: 200,
    ellipsis: { tooltip: true }
  },
  {
    title: t('exportCenter.status'),
    key: 'status',
    width: 120,
    render: (row: ExportTask) => {
      const statusMap: Record<number, { type: 'default' | 'info' | 'success' | 'error', text: string, icon: any }> = {
        0: { type: 'default', text: t('exportCenter.statusWaiting'), icon: TimeOutline },
        1: { type: 'info', text: t('exportCenter.statusProcessing'), icon: SyncOutline },
        2: { type: 'success', text: t('exportCenter.statusCompleted'), icon: CheckmarkCircleOutline },
        3: { type: 'error', text: t('exportCenter.statusFailed'), icon: CloseCircleOutline }
      }
      const status = statusMap[row.status] ?? statusMap[0]!
      return h(NTag, { type: status!.type, size: 'small' }, {
        default: () => status!.text,
        icon: () => h(NIcon, { component: status!.icon })
      })
    }
  },
  {
    title: t('exportCenter.progress'),
    key: 'progress',
    width: 150,
    render: (row: ExportTask) => {
      if (row.status === 1) {
        return h(NProgress, {
          type: 'line',
          percentage: row.progress,
          indicatorPlacement: 'inside',
          height: 20
        })
      }
      return row.status === 2 ? '100%' : '-'
    }
  },
  {
    title: t('exportCenter.dataRows'),
    key: 'totalRows',
    width: 120,
    render: (row: ExportTask) => {
      return row.totalRows ? t('exportCenter.rows', { n: row.totalRows.toLocaleString() }) : '-'
    }
  },
  {
    title: t('exportCenter.fileSize'),
    key: 'fileSize',
    width: 100,
    render: (row: ExportTask) => {
      if (!row.fileSize) return '-'
      const size = row.fileSize
      if (size < 1024) return size + ' B'
      if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
      if (size < 1024 * 1024 * 1024) return (size / 1024 / 1024).toFixed(1) + ' MB'
      return (size / 1024 / 1024 / 1024).toFixed(1) + ' GB'
    }
  },
  {
    title: t('exportCenter.fileType'),
    key: 'dataType',
    width: 90,
    render: (row: ExportTask) => {
      const typeMap: Record<string, { type: 'info' | 'success', text: string }> = {
        'xlsx': { type: 'success', text: 'Excel' },
        'zip': { type: 'info', text: t('exportCenter.zipArchive') }
      }
      const dataType = row.dataType || 'xlsx'
      const typeInfo = typeMap[dataType] ?? typeMap['xlsx']!
      return h(NTag, { type: typeInfo!.type, size: 'small' }, { default: () => typeInfo!.text })
    }
  },
  {
    title: t('exportCenter.createTime'),
    key: 'createTime',
    width: 170,
    render: (row: ExportTask) => formatDateTime(row.createTime)
  },
  {
    title: t('exportCenter.finishTime'),
    key: 'finishTime',
    width: 170,
    render: (row: ExportTask) => formatDateTime(row.finishTime)
  },
  {
    title: t('exportCenter.errorMsg'),
    key: 'errorMsg',
    width: 200,
    ellipsis: { tooltip: true },
    render: (row: ExportTask) => {
      return row.errorMsg || '-'
    }
  },
  {
    title: t('exportCenter.actions'),
    key: 'actions',
    width: 150,
    fixed: 'right' as const,
    render: (row: ExportTask) => {
      return h(NSpace, { size: 'small' }, {
        default: () => [
          row.status === 2 ? h(NTooltip, null, {
            trigger: () => h(NButton, {
              type: 'primary',
              size: 'small',
              onClick: () => handleDownload(row)
            }, { default: () => t('exportCenter.download') }),
            default: () => t('exportCenter.downloadFile')
          }) : null,
          h(NTooltip, null, {
            trigger: () => h(NButton, {
              type: 'error',
              size: 'small',
              quaternary: true,
              onClick: () => handleDelete(row)
            }, {
              icon: () => h(NIcon, { component: TrashOutline })
            }),
            default: () => t('exportCenter.deleteTask')
          })
        ].filter(Boolean)
      })
    }
  }
])

// 格式化日期为字符串
const formatDate = (timestamp: number): string => {
  const d = new Date(timestamp)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const fetchTasks = async () => {
  loading.value = true
  try {
    const params: any = {
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    
    if (searchForm.taskName) {
      params.taskName = searchForm.taskName
    }
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = formatDate(searchForm.dateRange[0])
      params.endDate = formatDate(searchForm.dateRange[1])
    }
    
    const res = await getExportTaskList(params) as any
    
    if (res.code === 200) {
      tasks.value = res.data.list || []
      pagination.itemCount = res.data.total || 0
    }
  } catch (e) {
    logger.error('获取导出任务失败', e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchTasks()
}

const handleReset = () => {
  searchForm.taskName = ''
  searchForm.dateRange = getDefaultDateRange()
  pagination.page = 1
  fetchTasks()
}

const handlePageChange = (page: number) => {
  pagination.page = page
  fetchTasks()
}

const handlePageSizeChange = (pageSize: number) => {
  pagination.pageSize = pageSize
  pagination.page = 1
  fetchTasks()
}

const handleDownload = async (task: ExportTask) => {
  const loadingMsg = message.loading(t('exportCenter.downloading'), { duration: 0 })
  try {
    const res = await downloadExportFile(task.id)
    const blob = res as unknown as Blob
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = task.fileName || 'export.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    loadingMsg.destroy()
    message.success(t('exportCenter.downloadSuccess'))
  } catch (e: any) {
    loadingMsg.destroy()
    message.error(t('exportCenter.downloadFailed', { error: e.message || '' }))
  }
}

const handleDelete = async (task: ExportTask) => {
  try {
    const res = await deleteExportTask(task.id) as any
    if (res.code === 200) {
      message.success(t('exportCenter.deleteSuccess'))
      fetchTasks()
    } else {
      message.error(res.msg || t('exportCenter.deleteFailed'))
    }
  } catch (e: any) {
    message.error(t('exportCenter.deleteFailedMsg', { error: e.message || '' }))
  }
}

// 请求浏览器通知权限
const requestNotificationPermission = async () => {
  if (!('Notification' in window)) return
  notificationPermission.value = Notification.permission
  if (Notification.permission === 'default') {
    const permission = await Notification.requestPermission()
    notificationPermission.value = permission
  }
}

// 发送浏览器通知
const sendBrowserNotification = (task: ExportTask, success: boolean) => {
  if (notificationPermission.value !== 'granted') return
  try {
    const title = success ? t('exportCenter.exportComplete') : t('exportCenter.exportFailed')
    const body = success
      ? t('exportCenter.taskCompletedBody', { name: task.taskName || t('exportCenter.exportTask') })
      : t('exportCenter.taskFailedBody', { name: task.taskName || t('exportCenter.exportTask'), error: task.errorMsg || '' })
    const notification = new Notification(title, {
      body,
      icon: '/favicon.ico',
      tag: `export-${task.id}`
    })
    notification.onclick = () => {
      window.focus()
      notification.close()
    }
  } catch (e) {
    // 通知发送失败不影响主流程
  }
}

// 轮询检查进行中的任务并检测完成状态变化
const startPolling = () => {
  // 初始化状态快照
  tasks.value.forEach(t => previousTaskStatuses.value.set(t.id, t.status))
  
  pollTimer = window.setInterval(async () => {
    const hasPending = tasks.value.some(t => t.status === 0 || t.status === 1)
    if (hasPending) {
      const oldStatuses = new Map(previousTaskStatuses.value)
      await fetchTasks()
      // 检测状态变化，发送通知
      tasks.value.forEach(task => {
        const oldStatus = oldStatuses.get(task.id)
        if (oldStatus !== undefined && oldStatus !== task.status) {
          if (task.status === 2) {
            sendBrowserNotification(task, true)
            message.success(t('exportCenter.taskCompleted', { name: task.taskName }))
          } else if (task.status === 3) {
            sendBrowserNotification(task, false)
            message.error(t('exportCenter.taskFailed', { name: task.taskName }))
          }
        }
      })
      // 更新状态快照
      previousTaskStatuses.value.clear()
      tasks.value.forEach(t => previousTaskStatuses.value.set(t.id, t.status))
    }
  }, 5000)
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onMounted(async () => {
  await requestNotificationPermission()
  await fetchTasks()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.export-center-page {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@media (max-width: 768px) {
  .page-header-stats {
    flex-direction: column;
  }
}
</style>
