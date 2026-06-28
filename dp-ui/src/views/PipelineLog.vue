<template>
  <div class="pipeline-log">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-info">
        <h2 class="page-title">
          <n-icon :component="TimeOutline" class="title-icon" />
          执行日志
        </h2>
        <p class="page-desc">查看流程执行历史记录和详细日志</p>
      </div>
    </div>

    <!-- 筛选条件 -->
    <n-card class="filter-card">
      <n-space align="center" :wrap="false" @keyup.enter="loadExecutions">
        <n-select 
          v-model:value="searchPipelineId" 
          placeholder="选择流程" 
          clearable 
          filterable
          style="width: 220px" 
          :options="pipelineOptions"
        />
        <n-select 
          v-model:value="searchStatus" 
          placeholder="执行状态" 
          clearable 
          style="width: 130px" 
          :options="statusOptions" 
        />
        <n-select 
          v-model:value="searchTriggerType" 
          placeholder="触发方式" 
          clearable 
          style="width: 130px" 
          :options="triggerOptions" 
        />
        <n-date-picker 
          v-model:value="dateRange" 
          type="daterange" 
          clearable 
          :shortcuts="dateShortcuts"
        />
        <n-button type="primary" @click="loadExecutions">
          <template #icon><n-icon :component="SearchOutline" /></template>
          查询
        </n-button>
        <n-button @click="resetSearch">重置</n-button>
      </n-space>
    </n-card>

    <!-- 日志列表 -->
    <n-card class="log-card">
      <n-data-table
        :columns="columns"
        :data="executions"
        :loading="loading"
        :pagination="pagination"
        :row-key="(row: any) => row.id"
        :row-class-name="getRowClassName"
        :scroll-x="1100"
        striped
        class="custom-table"
      />
    </n-card>

    <!-- 日志详情弹窗 -->
    <n-modal v-model:show="showLogModal" preset="card" title="执行详情" style="width: 900px; max-height: 85vh">
      <template #header>
        <div class="modal-header">
          <span>执行详情</span>
          <n-tag :type="getStatusTag(currentExecution?.status).type" round>
            {{ getStatusTag(currentExecution?.status).text }}
          </n-tag>
        </div>
      </template>
      
      <div v-if="currentExecution" class="execution-detail">
        <n-grid :cols="4" :x-gap="16" :y-gap="16">
          <n-gi>
            <div class="detail-item">
              <label>执行编号</label>
              <span class="code">{{ currentExecution.executionNo }}</span>
            </div>
          </n-gi>
          <n-gi>
            <div class="detail-item">
              <label>流程名称</label>
              <span>{{ currentExecution.pipelineName }}</span>
            </div>
          </n-gi>
          <n-gi>
            <div class="detail-item">
              <label>触发方式</label>
              <n-tag size="small">{{ getTriggerText(currentExecution.triggerType) }}</n-tag>
            </div>
          </n-gi>
          <n-gi>
            <div class="detail-item">
              <label>执行耗时</label>
              <span>{{ formatDuration(currentExecution.duration) }}</span>
            </div>
          </n-gi>
          <n-gi>
            <div class="detail-item">
              <label>开始时间</label>
              <span>{{ currentExecution.startTime }}</span>
            </div>
          </n-gi>
          <n-gi>
            <div class="detail-item">
              <label>结束时间</label>
              <span>{{ currentExecution.endTime || '-' }}</span>
            </div>
          </n-gi>
          <n-gi>
            <div class="detail-item">
              <label>输入数据量</label>
              <span class="number">{{ currentExecution.inputCount || 0 }}</span>
            </div>
          </n-gi>
          <n-gi>
            <div class="detail-item">
              <label>输出数据量</label>
              <span class="number">{{ currentExecution.outputCount || 0 }}</span>
            </div>
          </n-gi>
        </n-grid>

        <n-divider />

        <n-tabs type="line" animated>
          <n-tab-pane name="log" tab="执行日志">
            <div class="log-content">
              <pre>{{ currentExecution.executeLog || '暂无日志' }}</pre>
            </div>
          </n-tab-pane>
          <n-tab-pane v-if="currentExecution.errorMessage" name="error" tab="错误信息">
            <n-alert type="error" :title="currentExecution.errorMessage" />
          </n-tab-pane>
        </n-tabs>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, h, onMounted, computed } from 'vue'
import { NTag, NButton, NSpace, NTooltip, useMessage } from 'naive-ui'
import { TimeOutline, SearchOutline, EyeOutline } from '@vicons/ionicons5'
import { getExecutions, getExecution, getPipelines } from '@/api/pipeline'

const message = useMessage()
const loading = ref(false)
const executions = ref<any[]>([])
const pipelines = ref<any[]>([])
const showLogModal = ref(false)
const currentExecution = ref<any>(null)

const searchPipelineId = ref(null)
const searchStatus = ref(null)
const searchTriggerType = ref(null)
const dateRange = ref<[number, number] | null>(null)

const pagination = { pageSize: 15, showSizePicker: true, pageSizes: [10, 15, 20, 50] }

const pipelineOptions = computed(() => pipelines.value.map(p => ({ label: p.pipelineName, value: p.id })))

const statusOptions = [
  { label: '失败', value: 0 },
  { label: '成功', value: 1 },
  { label: '运行中', value: 2 },
  { label: '已取消', value: 3 }
]

const triggerOptions = [
  { label: '手动触发', value: 1 },
  { label: '定时触发', value: 2 },
  { label: '事件触发', value: 3 }
]

const dateShortcuts = {
  '今天': () => {
    const now = Date.now()
    const start = new Date(new Date().setHours(0, 0, 0, 0)).getTime()
    return [start, now]
  },
  '最近7天': () => {
    const now = Date.now()
    return [now - 7 * 24 * 60 * 60 * 1000, now]
  },
  '最近30天': () => {
    const now = Date.now()
    return [now - 30 * 24 * 60 * 60 * 1000, now]
  }
}

const getTriggerText = (type: number) => {
  const map: any = { 1: '手动', 2: '定时', 3: '事件' }
  return map[type] || '未知'
}

const getStatusTag = (status: number) => {
  const map: any = { 0: 'error', 1: 'success', 2: 'warning', 3: 'default' }
  const text: any = { 0: '失败', 1: '成功', 2: '运行中', 3: '已取消' }
  return { type: map[status] || 'default', text: text[status] || '未知' }
}

const formatDuration = (ms: number) => {
  if (!ms) return '-'
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  return `${(ms / 60000).toFixed(1)}min`
}

const formatDate = (timestamp: number) => {
  if (!timestamp) return ''
  return new Date(timestamp).toISOString().split('T')[0]
}

const getRowClassName = (row: any) => {
  if (row.status === 0) return 'row-error'
  if (row.status === 2) return 'row-running'
  return ''
}

const columns = [
  { 
    title: '执行编号', 
    key: 'executionNo', 
    width: 150,
    render: (row: any) => h('span', { class: 'code-text' }, row.executionNo)
  },
  { title: '流程名称', key: 'pipelineName', width: 200, ellipsis: { tooltip: true } },
  { 
    title: '状态', 
    key: 'status', 
    width: 100,
    render: (row: any) => {
      const tag = getStatusTag(row.status)
      return h(NTag, { type: tag.type, size: 'small', round: true }, { default: () => tag.text })
    }
  },
  { 
    title: '触发方式', 
    key: 'triggerType', 
    width: 100, 
    render: (row: any) => h(NTag, { size: 'small', bordered: false }, { default: () => getTriggerText(row.triggerType) })
  },
  { title: '开始时间', key: 'startTime', width: 170 },
  { title: '结束时间', key: 'endTime', width: 170 },
  { 
    title: '耗时', 
    key: 'duration', 
    width: 100, 
    render: (row: any) => formatDuration(row.duration) 
  },
  { 
    title: '数据量', 
    key: 'count', 
    width: 120, 
    render: (row: any) => h(NTooltip, {}, {
      trigger: () => h('span', `${row.inputCount || 0} / ${row.outputCount || 0}`),
      default: () => `输入: ${row.inputCount || 0}, 输出: ${row.outputCount || 0}`
    })
  },
  {
    title: '操作', 
    key: 'actions', 
    width: 100,
    fixed: 'right',
    render: (row: any) => {
      return h(NButton, { 
        size: 'small', 
        type: 'primary',
        ghost: true,
        onClick: () => viewLog(row) 
      }, { 
        icon: () => h('span', { class: 'n-icon' }, h(EyeOutline)),
        default: () => '详情' 
      })
    }
  }
]

const loadPipelines = async () => {
  try {
    const res = await getPipelines()
    pipelines.value = res.data || []
  } catch (e) {
    console.error('加载流程列表失败', e)
  }
}

const loadExecutions = async () => {
  loading.value = true
  try {
    const params: any = {}
    if (searchPipelineId.value) params.pipelineId = searchPipelineId.value
    if (searchStatus.value !== null) params.status = searchStatus.value
    if (searchTriggerType.value) params.triggerType = searchTriggerType.value
    if (dateRange.value) {
      params.startDate = formatDate(dateRange.value[0])
      params.endDate = formatDate(dateRange.value[1])
    }
    
    const res = await getExecutions(params)
    executions.value = res.data || []
  } catch (e) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchPipelineId.value = null
  searchStatus.value = null
  searchTriggerType.value = null
  dateRange.value = null
  loadExecutions()
}

const viewLog = async (row: any) => {
  try {
    const res = await getExecution(row.id)
    currentExecution.value = res.data
    showLogModal.value = true
  } catch (e) {
    message.error('加载日志失败')
  }
}

onMounted(() => {
  loadPipelines()
  loadExecutions()
})
</script>

<style scoped>
.pipeline-log {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #1e293b;
}

.title-icon {
  color: #2080f0;
}

.page-desc {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.filter-card {
  margin-bottom: 16px;
  border-radius: 12px;
}

.log-card {
  border-radius: 12px;
}

.code-text {
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 13px;
  color: #64748b;
}

/* 表格行状态 */
:deep(.row-error) {
  background-color: rgba(208, 48, 80, 0.05) !important;
}

:deep(.row-running) {
  background-color: rgba(240, 160, 32, 0.05) !important;
}

/* 弹窗 */
.modal-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.execution-detail {
  padding: 8px 0;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-item label {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
}

.detail-item span {
  font-size: 14px;
  color: #1e293b;
}

.detail-item .code {
  font-family: 'SF Mono', Monaco, monospace;
  background: #f1f5f9;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 13px;
}

.detail-item .number {
  font-weight: 600;
  font-size: 18px;
  color: #2080f0;
}

.log-content {
  background: #1e293b;
  border-radius: 8px;
  padding: 16px;
  max-height: 400px;
  overflow: auto;
}

.log-content pre {
  margin: 0;
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #e2e8f0;
  white-space: pre-wrap;
  word-break: break-all;
}




:deep(html.dark .row-error) {
  background-color: rgba(248, 113, 113, 0.08) !important;
}

:deep(html.dark .row-running) {
  background-color: rgba(251, 191, 36, 0.08) !important;
}
</style>

<style>
/* PipelineLog 深色模式（非 scoped） */
html.dark .page-title { color: #f1f5f9 !important; }
html.dark .page-desc { color: #94a3b8 !important; }
html.dark .code-text { color: #94a3b8 !important; }
html.dark .detail-item label { color: #64748b !important; }
html.dark .detail-item span { color: #e2e8f0 !important; }
html.dark .detail-item .code {
  background: #0f172a !important;
  color: #e2e8f0 !important;
}
html.dark .log-content {
  background: #0f172a !important;
}
</style>
