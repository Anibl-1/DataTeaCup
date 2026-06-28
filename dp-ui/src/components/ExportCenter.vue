<template>
  <div class="export-center">
    <n-popover trigger="click" placement="bottom-end" :width="400">
      <template #trigger>
        <n-badge :value="pendingCount" :max="99" :show="pendingCount > 0">
          <n-button quaternary circle>
            <template #icon>
              <n-icon size="20">
                <DownloadOutline />
              </n-icon>
            </template>
          </n-button>
        </n-badge>
      </template>
      
      <div class="export-panel">
        <div class="panel-header">
          <span class="title">导出中心</span>
          <n-button text size="small" @click="refreshTasks">
            <template #icon>
              <n-icon><RefreshOutline /></n-icon>
            </template>
          </n-button>
        </div>
        
        <n-scrollbar style="max-height: 400px">
          <div v-if="loading" class="loading-container">
            <n-spin size="small" />
          </div>
          
          <div v-else-if="tasks.length === 0" class="empty-container">
            <n-empty description="暂无导出任务" size="small" />
          </div>
          
          <div v-else class="task-list">
            <div v-for="task in tasks" :key="task.id" class="task-item">
              <div class="task-info">
                <div class="task-name">{{ task.taskName }}</div>
                <div class="task-meta">
                  <n-tag :type="getStatusType(task.status)" size="small">
                    {{ getStatusText(task.status) }}
                  </n-tag>
                  <span class="task-time">{{ formatTime(task.createTime) }}</span>
                </div>
                <div v-if="task.status === 1" class="task-progress">
                  <n-progress
                    type="line"
                    :percentage="task.progress"
                    :show-indicator="false"
                    :height="4"
                  />
                </div>
                <div v-if="task.status === 2" class="task-result">
                  <span>{{ formatFileSize(task.fileSize) }}</span>
                  <span v-if="task.totalRows">· {{ task.totalRows.toLocaleString() }} 行</span>
                </div>
                <div v-if="task.status === 3" class="task-error">
                  {{ task.errorMsg }}
                </div>
              </div>
              <div class="task-actions">
                <n-button
                  v-if="task.status === 2"
                  type="primary"
                  size="small"
                  @click="downloadTask(task)"
                >
                  下载
                </n-button>
                <n-button
                  text
                  size="small"
                  @click="deleteTask(task)"
                >
                  <n-icon><TrashOutline /></n-icon>
                </n-button>
              </div>
            </div>
          </div>
        </n-scrollbar>
        
        <div v-if="total > tasks.length" class="panel-footer">
          <n-button text size="small" @click="loadMore">
            加载更多 ({{ tasks.length }}/{{ total }})
          </n-button>
        </div>
      </div>
    </n-popover>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { NPopover, NBadge, NButton, NIcon, NScrollbar, NSpin, NEmpty, NTag, NProgress, useMessage } from 'naive-ui'
import { DownloadOutline, RefreshOutline, TrashOutline } from '@vicons/ionicons5'
import { 
  getExportTaskList, 
  downloadExportFile, 
  deleteExportTask,
  getStatusText,
  getStatusType,
  type ExportTask 
} from '@/api/exportTask'

const message = useMessage()
const loading = ref(false)
const tasks = ref<ExportTask[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 10

let pollTimer: number | null = null

const pendingCount = computed(() => {
  return tasks.value.filter(t => t.status === 0 || t.status === 1).length
})

const fetchTasks = async (append = false) => {
  loading.value = true
  try {
    const res = await getExportTaskList({ page: page.value, pageSize }) as any
    if (res.code === 200) {
      if (append) {
        tasks.value = [...tasks.value, ...res.data.list]
      } else {
        tasks.value = res.data.list || []
      }
      total.value = res.data.total || 0
    }
  } catch (e) {
    console.error('获取导出任务失败', e)
  } finally {
    loading.value = false
  }
}

const refreshTasks = () => {
  page.value = 1
  fetchTasks()
}

const loadMore = () => {
  page.value++
  fetchTasks(true)
}

const downloadTask = async (task: ExportTask) => {
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
    message.success('下载成功')
  } catch (e: any) {
    message.error('下载失败: ' + (e.message || '未知错误'))
  }
}

const deleteTask = async (task: ExportTask) => {
  try {
    const res = await deleteExportTask(task.id) as any
    if (res.code === 200) {
      tasks.value = tasks.value.filter(t => t.id !== task.id)
      total.value--
      message.success('删除成功')
    }
  } catch (e: any) {
    message.error('删除失败: ' + (e.message || '未知错误'))
  }
}

const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  
  return date.toLocaleDateString()
}

const formatFileSize = (size?: number) => {
  if (!size) return ''
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / 1024 / 1024).toFixed(1) + ' MB'
  return (size / 1024 / 1024 / 1024).toFixed(1) + ' GB'
}

// 轮询检查进行中的任务
const startPolling = () => {
  pollTimer = window.setInterval(async () => {
    const hasPending = tasks.value.some(t => t.status === 0 || t.status === 1)
    if (hasPending) {
      await fetchTasks()
    }
  }, 3000)
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onMounted(() => {
  fetchTasks()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})

// 暴露刷新方法供外部调用
defineExpose({
  refresh: refreshTasks
})
</script>

<style scoped>
.export-center {
  display: inline-flex;
}

.export-panel {
  padding: 8px 0;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 12px 8px;
  border-bottom: 1px solid var(--n-border-color);
}

.panel-header .title {
  font-weight: 600;
  font-size: 14px;
}

.loading-container,
.empty-container {
  padding: 24px;
  text-align: center;
}

.task-list {
  padding: 8px 0;
}

.task-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 8px 12px;
  border-bottom: 1px solid var(--n-border-color);
}

.task-item:last-child {
  border-bottom: none;
}

.task-info {
  flex: 1;
  min-width: 0;
}

.task-name {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.task-time {
  font-size: 12px;
  color: var(--n-text-color-3);
}

.task-progress {
  margin-top: 6px;
}

.task-result {
  font-size: 12px;
  color: var(--n-text-color-2);
  margin-top: 4px;
}

.task-error {
  font-size: 12px;
  color: var(--n-error-color);
  margin-top: 4px;
  word-break: break-all;
}

.task-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: 8px;
}

.panel-footer {
  padding: 8px 12px 0;
  border-top: 1px solid var(--n-border-color);
  text-align: center;
}
</style>
