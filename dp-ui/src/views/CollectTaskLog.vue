<template>
  <!-- 任务详情模态框 -->
  <n-modal v-model:show="showDetailModal" preset="card" title="任务详情" style="width: 800px">
    <n-spin :show="detailLoading">
      <div v-if="selectedTask" class="task-detail">
        <n-descriptions :column="2" bordered>
          <n-descriptions-item label="任务ID">{{ selectedTask.id }}</n-descriptions-item>
          <n-descriptions-item label="任务名称">{{ selectedTask.taskName }}</n-descriptions-item>
          <n-descriptions-item label="源数据源">{{ selectedTask.dataSourceName }}</n-descriptions-item>
          <n-descriptions-item label="源表">{{ selectedTask.tableName || '-' }}</n-descriptions-item>
          <n-descriptions-item label="目标数据源">{{ selectedTask.targetDataSourceName || '本地数据库' }}</n-descriptions-item>
          <n-descriptions-item label="目标表">{{ selectedTask.targetTableName || selectedTask.tableName || '-' }}</n-descriptions-item>
          <n-descriptions-item label="采集模式">
            <n-tag :type="selectedTask.collectMode === 'full' ? 'info' : selectedTask.collectMode === 'incremental' ? 'warning' : 'success'" size="small">
              {{ selectedTask.collectMode === 'full' ? '全量采集' : selectedTask.collectMode === 'incremental' ? '增量采集' : '自定义SQL' }}
            </n-tag>
          </n-descriptions-item>
          <n-descriptions-item label="状态">
            <StatusTag :status="selectedTask.status" :status-map="statusMap" />
          </n-descriptions-item>
          <n-descriptions-item label="定时任务">
            <n-tag :type="selectedTask.scheduleEnabled ? 'success' : 'default'" size="small">
              {{ selectedTask.scheduleEnabled ? '已启用' : '未启用' }}
            </n-tag>
          </n-descriptions-item>
          <n-descriptions-item label="Cron表达式">{{ selectedTask.cronExpression || '-' }}</n-descriptions-item>
          <n-descriptions-item label="定时描述" :span="2">{{ selectedTask.scheduleDescription || '-' }}</n-descriptions-item>
          <n-descriptions-item label="创建时间">{{ formatDateTime(selectedTask.createTime) }}</n-descriptions-item>
          <n-descriptions-item label="更新时间">{{ formatDateTime(selectedTask.updateTime) }}</n-descriptions-item>
          <n-descriptions-item label="最后执行时间" :span="2">
            {{ taskDetail?.lastExecuteTime ? formatDateTime(taskDetail.lastExecuteTime) : '未执行' }}
          </n-descriptions-item>
          <n-descriptions-item label="最后执行结果" :span="2">
            <span :style="{ color: selectedTask.lastExecuteResult?.includes('失败') ? '#d03050' : '#18a058' }">
              {{ selectedTask.lastExecuteResult || '-' }}
            </span>
          </n-descriptions-item>
        </n-descriptions>

        <n-divider>执行统计</n-divider>
        <n-grid :cols="3" :x-gap="16" style="margin-top: 16px;">
          <n-gi>
            <n-card>
              <n-statistic label="执行次数" :value="taskDetail?.executeCount || 0">
                <template #prefix><n-icon size="20" color="#2080f0"><TimeOutline /></n-icon></template>
              </n-statistic>
            </n-card>
          </n-gi>
          <n-gi>
            <n-card>
              <n-statistic label="成功次数" :value="taskDetail?.successCount || 0">
                <template #prefix><n-icon size="20" color="#18a058"><CheckmarkCircleOutline /></n-icon></template>
              </n-statistic>
            </n-card>
          </n-gi>
          <n-gi>
            <n-card>
              <n-statistic label="失败次数" :value="taskDetail?.failCount || 0">
                <template #prefix><n-icon size="20" color="#d03050"><WarningOutline /></n-icon></template>
              </n-statistic>
            </n-card>
          </n-gi>
        </n-grid>

        <n-divider>操作</n-divider>
        <n-space>
          <n-button type="success" :disabled="selectedTask.status === 'running'" @click="handleStart(selectedTask.id)">
            {{ selectedTask.scheduleEnabled ? '启用定时' : '启动任务' }}
          </n-button>
          <n-button v-if="selectedTask.scheduleEnabled" type="primary" :disabled="selectedTask.status === 'running'" @click="handleExecuteOnce(selectedTask.id)">
            立即执行
          </n-button>
          <n-button type="warning" :disabled="selectedTask.status === 'stopped'" @click="handleStop(selectedTask.id)">
            停止任务
          </n-button>
          <n-button @click="handleEditFromDetail">编辑任务</n-button>
          <n-button type="info" @click="goToTaskLogs(selectedTask.id)">
            <template #icon><n-icon><ListOutline /></n-icon></template>
            查看日志
          </n-button>
          <n-button type="error" @click="handleDelete(selectedTask.id)">删除任务</n-button>
        </n-space>
      </div>
    </n-spin>
  </n-modal>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage, useDialog } from 'naive-ui'
import {
  WarningOutline, TimeOutline, CheckmarkCircleOutline, ListOutline
} from '@vicons/ionicons5'
import { startCollectTask, stopCollectTask, executeTaskOnce, deleteCollectTask } from '@/api/dataCollect'
import { TASK_STATUS_MAP } from '@/constants'
import type { CollectTask } from '@/types/collectTask'
import { formatDateTime } from '@/utils/format'
import { handleApiError } from '@/utils/error'
import StatusTag, { type StatusConfig } from '@/components/common/StatusTag.vue'

const emit = defineEmits<{
  (e: 'edit', task: CollectTask): void
  (e: 'refresh'): void
}>()

const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const showDetailModal = ref(false)
const selectedTask = ref<CollectTask | null>(null)
const taskDetail = ref<any>(null)
const detailLoading = ref(false)

// StatusTag 映射
const statusMap: Record<string, StatusConfig> = {
  running: { label: TASK_STATUS_MAP.running.text, type: TASK_STATUS_MAP.running.type },
  stopped: { label: TASK_STATUS_MAP.stopped.text, type: TASK_STATUS_MAP.stopped.type },
  error: { label: TASK_STATUS_MAP.error.text, type: TASK_STATUS_MAP.error.type }
}

const openDetail = (row: CollectTask) => {
  showDetailModal.value = true
  selectedTask.value = row
  loadTaskDetail()
}

const loadTaskDetail = async () => {
  if (!selectedTask.value) return
  detailLoading.value = true
  try {
    taskDetail.value = {
      ...selectedTask.value,
      lastExecuteTime: selectedTask.value?.updateTime,
      executeCount: 0, successCount: 0, failCount: 0
    }
  } catch (error) {
    handleApiError(error, '加载任务详情')
  } finally {
    detailLoading.value = false
  }
}

const handleStart = (id: number) => {
  const task = selectedTask.value
  const isScheduled = task?.scheduleEnabled
  dialog.info({
    title: isScheduled ? '确认启用定时' : '确认启动',
    content: isScheduled
      ? '确定要启用这个定时任务吗？任务将按照设定的时间自动执行。'
      : '确定要启动这个采集任务吗？任务将立即执行。',
    positiveText: isScheduled ? '确认启用' : '确认启动',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await startCollectTask(id)
        message.success(isScheduled ? '定时任务已启用' : '任务已启动')
        emit('refresh')
        if (selectedTask.value?.id === id) selectedTask.value.status = 'running'
      } catch (error) {
        message.error(handleApiError(error, '启动采集任务'))
      }
    }
  })
}

const handleExecuteOnce = (id: number) => {
  dialog.info({
    title: '确认执行',
    content: '确定要立即执行一次这个任务吗？这不会影响定时调度。',
    positiveText: '确认执行', negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await executeTaskOnce(id)
        message.success('任务已开始执行')
        emit('refresh')
        if (selectedTask.value?.id === id) selectedTask.value.status = 'running'
      } catch (error) {
        message.error(handleApiError(error, '执行采集任务'))
      }
    }
  })
}

const handleStop = (id: number) => {
  dialog.warning({
    title: '确认停止', content: '确定要停止这个采集任务吗？',
    positiveText: '确认停止', negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await stopCollectTask(id)
        message.success('任务已停止')
        emit('refresh')
        if (selectedTask.value?.id === id) selectedTask.value.status = 'stopped'
      } catch (error) {
        message.error(handleApiError(error, '停止采集任务'))
      }
    }
  })
}

const handleDelete = (id: number) => {
  dialog.warning({
    title: '确认删除', content: '确定要删除这个采集任务吗？删除后无法恢复。',
    positiveText: '确认删除', negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteCollectTask(id)
        message.success('删除成功')
        emit('refresh')
        if (selectedTask.value?.id === id) showDetailModal.value = false
      } catch (error) {
        message.error(handleApiError(error, '删除采集任务'))
      }
    }
  })
}

const handleEditFromDetail = () => {
  if (!selectedTask.value) return
  showDetailModal.value = false
  emit('edit', selectedTask.value)
}

const goToTaskLogs = (taskId: number) => {
  showDetailModal.value = false
  router.push({ path: '/collect-log', query: { taskId: String(taskId) } })
}

defineExpose({ openDetail })
</script>

<style scoped>
.task-detail { padding: 8px 0; }
</style>
