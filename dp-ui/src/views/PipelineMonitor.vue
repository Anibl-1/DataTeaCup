<template>
  <div class="pipeline-monitor">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-info">
        <h2 class="page-title">
          <n-icon :component="PulseOutline" class="title-icon" />
          执行监控
        </h2>
        <p class="page-desc">实时监控流程执行状态，查看运行中的任务</p>
      </div>
      <n-button @click="loadAll">
        <template #icon><n-icon :component="RefreshOutline" /></template>
        刷新
      </n-button>
    </div>

    <!-- 统计卡片 -->
    <n-grid :cols="4" :x-gap="16" :y-gap="16" class="stats-row">
      <n-gi>
        <div class="stat-card">
          <div class="stat-icon icon-total"><n-icon :component="LayersOutline" size="24" /></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalPipelines || 0 }}</div>
            <div class="stat-label">流程总数</div>
          </div>
        </div>
      </n-gi>
      <n-gi>
        <div class="stat-card">
          <div class="stat-icon icon-published"><n-icon :component="CheckmarkCircleOutline" size="24" /></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.publishedPipelines || 0 }}</div>
            <div class="stat-label">已发布</div>
          </div>
        </div>
      </n-gi>
      <n-gi>
        <div class="stat-card">
          <div class="stat-icon icon-today"><n-icon :component="CalendarOutline" size="24" /></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.todayExecutions || 0 }}</div>
            <div class="stat-label">今日执行</div>
          </div>
        </div>
      </n-gi>
      <n-gi>
        <div class="stat-card">
          <div class="stat-icon icon-running"><n-icon :component="SyncOutline" size="24" /></div>
          <div class="stat-info">
            <div class="stat-value animated-number">{{ stats.runningExecutions || 0 }}</div>
            <div class="stat-label">运行中</div>
          </div>
        </div>
      </n-gi>
    </n-grid>

    <!-- 运行中的任务 -->
    <n-card class="running-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <n-icon :component="FlashOutline" color="#f0a020" />
            <span>运行中的任务</span>
            <n-badge :value="runningExecutions.length" :max="99" />
          </div>
        </div>
      </template>

      <n-empty v-if="runningExecutions.length === 0" description="暂无运行中的任务">
        <template #icon>
          <n-icon :component="CheckmarkDoneOutline" size="48" color="#18a058" />
        </template>
      </n-empty>

      <div v-else class="running-list">
        <div v-for="exec in runningExecutions" :key="exec.id" class="running-item" @click="viewExecution(exec)">
          <div class="item-main">
            <div class="item-icon">
              <n-icon :component="GitNetworkOutline" size="20" />
            </div>
            <div class="item-info">
              <div class="item-title">{{ exec.pipelineName }}</div>
              <div class="item-meta">
                <span class="meta-tag">{{ exec.executionNo }}</span>
                <span>开始于 {{ exec.startTime }}</span>
              </div>
            </div>
          </div>
          <div class="item-status">
            <div class="status-indicator">
              <span class="status-dot"></span>
              <span>运行中</span>
            </div>
            <n-button size="small" @click.stop="viewExecution(exec)">
              <template #icon><n-icon :component="EyeOutline" /></template>
              查看
            </n-button>
            <n-button size="small" type="error" ghost @click.stop="handleStop(exec.id)">
              停止
            </n-button>
          </div>
        </div>
      </div>
    </n-card>

    <!-- 执行统计 -->
    <n-grid :cols="2" :x-gap="16" style="margin-top: 16px">
      <n-gi>
        <n-card title="执行状态分布">
          <div ref="pieChartRef" style="height: 240px"></div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="近7天执行趋势">
          <div ref="lineChartRef" style="height: 240px"></div>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 最近执行记录 -->
    <n-card title="最近执行记录" style="margin-top: 16px">
      <template #header-extra>
        <n-space :size="8" align="center">
          <n-select v-model:value="filterStatus" :options="statusFilterOptions" placeholder="执行状态" clearable style="width: 120px" size="small" @update:value="loadRecent" />
          <n-select v-model:value="filterTrigger" :options="triggerFilterOptions" placeholder="触发方式" clearable style="width: 120px" size="small" @update:value="loadRecent" />
        </n-space>
      </template>
      <n-data-table :columns="columns" :data="filteredExecutions.slice(0, 20)" :pagination="false" size="small" :row-props="rowProps" striped class="custom-table" :scroll-x="800" />
    </n-card>

    <!-- 执行详情弹窗 -->
    <n-modal v-model:show="showDetailModal" preset="card" style="width: 900px; max-height: 90vh" :title="currentExecution?.pipelineName + ' - 执行详情'">
      <div v-if="currentExecution" class="execution-detail">
        <!-- 基本信息 -->
        <div class="detail-header">
          <div class="detail-info-row">
            <div class="info-item">
              <label>执行编号</label>
              <span class="code">{{ currentExecution.executionNo }}</span>
            </div>
            <div class="info-item">
              <label>状态</label>
              <n-tag :type="getStatusTag(currentExecution.status).type" round>
                {{ getStatusTag(currentExecution.status).text }}
              </n-tag>
            </div>
            <div class="info-item">
              <label>开始时间</label>
              <span>{{ currentExecution.startTime }}</span>
            </div>
            <div class="info-item">
              <label>运行时长</label>
              <span class="duration">{{ getRunningDuration() }}</span>
            </div>
          </div>
        </div>

        <!-- 节点执行状态 -->
        <div class="nodes-section">
          <div class="section-title">
            <n-icon :component="GitNetworkOutline" />
            <span>节点执行状态</span>
            <n-tag v-if="currentExecution.status === 2" type="warning" size="small">实时更新中</n-tag>
          </div>
          <div class="nodes-timeline">
            <div v-for="(node, index) in executionNodes" :key="index" class="node-item" :class="node.status">
              <div class="node-indicator">
                <div class="node-dot">
                  <n-icon v-if="node.status === 'success'" :component="CheckmarkOutline" size="14" />
                  <n-icon v-else-if="node.status === 'failed'" :component="CloseOutline" size="14" />
                  <n-icon v-else-if="node.status === 'running'" :component="SyncOutline" size="14" class="spinning" />
                  <n-icon v-else :component="TimeOutline" size="14" />
                </div>
                <div v-if="index < executionNodes.length - 1" class="node-line"></div>
              </div>
              <div class="node-content">
                <div class="node-header">
                  <span class="node-name">{{ node.name }}</span>
                  <span class="node-type">{{ node.type }}</span>
                  <span v-if="node.duration" class="node-time">{{ node.duration }}</span>
                </div>
                <div v-if="node.message" class="node-message">{{ node.message }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 实时日志 -->
        <div class="log-section">
          <div class="section-title">
            <n-icon :component="TerminalOutline" />
            <span>执行日志</span>
            <n-switch v-model:value="autoScroll" size="small">
              <template #checked>自动滚动</template>
              <template #unchecked>手动</template>
            </n-switch>
          </div>
          <div ref="logContainerRef" class="log-container">
            <pre class="log-content">{{ currentExecution.executeLog || '等待日志...' }}</pre>
          </div>
        </div>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, h, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { NTag, NButton, useMessage } from 'naive-ui'
import { 
  PulseOutline, RefreshOutline, LayersOutline, CheckmarkCircleOutline,
  CalendarOutline, SyncOutline, FlashOutline, CheckmarkDoneOutline,
  GitNetworkOutline, EyeOutline, CheckmarkOutline, CloseOutline, TimeOutline, TerminalOutline
} from '@vicons/ionicons5'
import { getPipelineStatistics, getExecutions, getRunningExecutions, stopExecution, getExecution, getExecutionTrend } from '@/api/pipeline'
import { logger } from '@/utils/logger'
import echarts from '@/utils/echarts'

interface PipelineStats {
  totalPipelines?: number
  publishedPipelines?: number
  todayExecutions?: number
  runningExecutions?: number
  totalExecutions?: number
  successExecutions?: number
  failedExecutions?: number
}

interface Execution {
  id: number
  pipelineName?: string
  executionNo?: string
  status: number
  triggerType?: number
  startTime?: string
  endTime?: string
  duration?: number
  executeLog?: string
  [key: string]: unknown
}

interface ExecutionNode {
  name: string
  type: string
  startTime?: string
  endTime?: string
  duration?: string
  status: 'running' | 'success' | 'failed' | 'skipped'
  message?: string
}

interface TrendItem {
  day?: string
  successCount?: number | string
  failedCount?: number | string
}

const message = useMessage()
const stats = ref<PipelineStats>({})
const runningExecutions = ref<Execution[]>([])
const recentExecutions = ref<Execution[]>([])
const showDetailModal = ref(false)
const currentExecution = ref<Execution | null>(null)
const executionNodes = ref<ExecutionNode[]>([])
const autoScroll = ref(true)
const logContainerRef = ref<HTMLElement>()
const trendData = ref<TrendItem[]>([])

// 图表
const pieChartRef = ref<HTMLElement>()
const lineChartRef = ref<HTMLElement>()
let pieChart: echarts.ECharts | null = null
let lineChart: echarts.ECharts | null = null

// 初始化饼图
const initPieChart = () => {
  if (!pieChartRef.value) return
  pieChart = echarts.init(pieChartRef.value)
  updatePieChart()
}

// 更新饼图
const updatePieChart = () => {
  if (!pieChart) return
  const option = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 10, left: 'center' },
    color: ['#10b981', '#ef4444', '#f59e0b'],
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, position: 'center', formatter: () => `${stats.value.totalExecutions || 0}\n总执行`, fontSize: 16, fontWeight: 'bold', lineHeight: 24 },
      emphasis: { label: { show: true, fontSize: 18, fontWeight: 'bold' } },
      data: [
        { value: stats.value.successExecutions || 0, name: '成功' },
        { value: stats.value.failedExecutions || 0, name: '失败' },
        { value: stats.value.runningExecutions || 0, name: '运行中' }
      ]
    }]
  }
  pieChart.setOption(option)
}

// 初始化折线图
const initLineChart = () => {
  if (!lineChartRef.value) return
  lineChart = echarts.init(lineChartRef.value)
  updateLineChart()
}

// 更新折线图（使用真实数据）
const updateLineChart = () => {
  if (!lineChart) return
  const days = trendData.value.map((item) => {
    const d = item.day ? item.day.substring(5) : ''
    return d.replace('-', '/')
  })
  const successData = trendData.value.map((item) => Number(item.successCount) || 0)
  const failedData = trendData.value.map((item) => Number(item.failedCount) || 0)
  const option = {
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0 },
    grid: { left: 40, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: days, axisLine: { lineStyle: { color: '#e5e7eb' } }, axisLabel: { color: '#6b7280' } },
    yAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { color: '#f3f4f6' } }, axisLabel: { color: '#6b7280' } },
    color: ['#10b981', '#ef4444'],
    series: [
      { name: '成功', type: 'line', smooth: true, data: successData, areaStyle: { opacity: 0.1 } },
      { name: '失败', type: 'line', smooth: true, data: failedData, areaStyle: { opacity: 0.1 } }
    ]
  }
  lineChart.setOption(option)
}

// 窗口大小变化时重绘图表
const handleResize = () => {
  pieChart?.resize()
  lineChart?.resize()
}

let refreshTimer: ReturnType<typeof setInterval> | null = null
let detailRefreshTimer: ReturnType<typeof setInterval> | null = null

const getStatusTag = (status: number) => {
  const map: Record<number, 'error' | 'success' | 'warning' | 'default'> = { 0: 'error', 1: 'success', 2: 'warning', 3: 'default' }
  const text: Record<number, string> = { 0: '失败', 1: '成功', 2: '运行中', 3: '已取消' }
  return { type: map[status] || 'default', text: text[status] || '未知' }
}

const formatDuration = (ms: number) => {
  if (!ms) return '-'
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  return `${(ms / 60000).toFixed(1)}min`
}

const getRunningDuration = () => {
  if (!currentExecution.value?.startTime) return '-'
  const start = new Date(currentExecution.value.startTime).getTime()
  const end = currentExecution.value.endTime ? new Date(currentExecution.value.endTime).getTime() : Date.now()
  return formatDuration(end - start)
}

// 解析日志获取节点执行状态
const parseExecutionNodes = (log: string): ExecutionNode[] => {
  if (!log) return []
  const nodes: ExecutionNode[] = []
  const lines = log.split('\n')
  let currentNode: ExecutionNode | null = null
  
  for (const line of lines) {
    // 匹配节点开始: [HH:mm:ss] ▶ 节点名称 (类型)
    const startMatch = line.match(/\[(\d{2}:\d{2}:\d{2})\] ▶ (.+?) \((.+?)\)/)
    if (startMatch) {
      if (currentNode) {
        currentNode.status = 'success'
        nodes.push(currentNode)
      }
      currentNode = {
        name: startMatch[2] ?? '',
        type: startMatch[3] ?? '',
        startTime: startMatch[1] ?? '',
        status: 'running',
        message: ''
      }
      continue
    }
    
    // 匹配节点完成: [HH:mm:ss] ✓ 完成
    const completeMatch = line.match(/\[(\d{2}:\d{2}:\d{2})\] ✓ 完成/)
    if (completeMatch && currentNode) {
      currentNode.status = 'success'
      currentNode.endTime = completeMatch[1] ?? ''
      currentNode.duration = `${currentNode.startTime} - ${completeMatch[1] ?? ''}`
      nodes.push(currentNode)
      currentNode = null
      continue
    }
    
    // 匹配跳过: [跳过]
    const skipMatch = line.match(/\[跳过\] (.+)/)
    if (skipMatch) {
      nodes.push({
        name: skipMatch[1]?.split(' ')[0] ?? '',
        type: '-',
        status: 'skipped',
        message: skipMatch[1] ?? ''
      })
      continue
    }
    
    // 匹配失败
    if (line.includes('ERROR') || line.includes('失败')) {
      if (currentNode) {
        currentNode.status = 'failed'
        currentNode.message = line.trim()
      }
    }
  }
  
  // 处理最后一个正在运行的节点
  if (currentNode) {
    nodes.push(currentNode)
  }
  
  return nodes
}

const filterStatus = ref<number | null>(null)
const filterTrigger = ref<number | null>(null)

const statusFilterOptions = [
  { label: '成功', value: 1 },
  { label: '失败', value: 0 },
  { label: '运行中', value: 2 },
  { label: '已取消', value: 3 }
]

const triggerFilterOptions = [
  { label: '手动执行', value: 1 },
  { label: '定时调度', value: 2 },
  { label: 'API调用', value: 3 }
]

const filteredExecutions = computed(() => {
  let list = recentExecutions.value
  if (filterStatus.value !== null) {
    list = list.filter((e) => e.status === filterStatus.value)
  }
  if (filterTrigger.value !== null) {
    list = list.filter((e) => e.triggerType === filterTrigger.value)
  }
  return list
})

const getTriggerLabel = (type: number) => {
  const map: Record<number, string> = { 1: '手动', 2: '定时', 3: 'API' }
  return map[type] || '未知'
}

const formatDateTime = (dt: string) => {
  if (!dt) return '-'
  try {
    return new Date(dt).toLocaleString('zh-CN', { hour12: false, month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' })
  } catch { return dt }
}

const columns = [
  { title: '流程', key: 'pipelineName', ellipsis: true, minWidth: 140 },
  { title: '执行编号', key: 'executionNo', width: 140, ellipsis: true,
    render: (row: Execution) => h('span', { style: 'font-family: monospace; font-size: 12px' }, (row.executionNo as string) || '-')
  },
  { 
    title: '状态', key: 'status', width: 80,
    render: (row: Execution) => {
      const tag = getStatusTag(row.status)
      return h(NTag, { type: tag.type, size: 'small', round: true }, { default: () => tag.text })
    }
  },
  { title: '触发', key: 'triggerType', width: 70,
    render: (row: Execution) => h(NTag, { size: 'small', bordered: false }, { default: () => getTriggerLabel(row.triggerType as number) })
  },
  { title: '开始时间', key: 'startTime', width: 150,
    render: (row: Execution) => formatDateTime(row.startTime as string)
  },
  { title: '耗时', key: 'duration', width: 80, render: (row: Execution) => formatDuration(row.duration as number) }
]

const rowProps = (row: Execution) => ({
  style: 'cursor: pointer',
  onClick: () => viewExecution(row)
})

const loadStats = async () => {
  try {
    const res = await getPipelineStatistics()
    stats.value = (res.data || {}) as PipelineStats
  } catch (e) {
    logger.warn('加载统计失败', e)
  }
}

const loadRunning = async () => {
  try {
    const res = await getRunningExecutions()
    runningExecutions.value = (res.data || []) as Execution[]
  } catch (e) {
    logger.warn('加载运行中任务失败', e)
  }
}

const loadRecent = async () => {
  try {
    const res = await getExecutions({})
    recentExecutions.value = (res.data || []) as Execution[]
  } catch (e) {
    logger.warn('加载执行记录失败', e)
  }
}

const viewExecution = async (exec: Execution) => {
  currentExecution.value = exec
  showDetailModal.value = true
  await refreshExecutionDetail()
  
  // 如果是运行中的任务，启动定时刷新
  if (exec.status === 2) {
    detailRefreshTimer = setInterval(refreshExecutionDetail, 2000)
  }
}

const refreshExecutionDetail = async () => {
  if (!currentExecution.value?.id) return
  try {
    const res = await getExecution(currentExecution.value.id)
    currentExecution.value = res.data
    executionNodes.value = parseExecutionNodes(res.data?.executeLog || '')
    
    // 自动滚动到底部
    if (autoScroll.value) {
      nextTick(() => {
        if (logContainerRef.value) {
          logContainerRef.value.scrollTop = logContainerRef.value.scrollHeight
        }
      })
    }
    
    // 如果执行完成，停止刷新
    if (res.data?.status !== 2 && detailRefreshTimer) {
      clearInterval(detailRefreshTimer)
      detailRefreshTimer = null
      loadAll() // 刷新列表
    }
  } catch (e) {
    logger.warn('刷新执行详情失败', e)
  }
}

const handleStop = async (id: number) => {
  try {
    await stopExecution(id)
    message.success('已停止执行')
    loadAll()
  } catch (e) {
    message.error('停止失败')
  }
}

const loadTrend = async () => {
  try {
    const res = await getExecutionTrend(7)
    trendData.value = (res.data || []) as TrendItem[]
  } catch (e) {
    logger.warn('加载趋势数据失败', e)
  }
}

const loadAll = () => {
  loadStats()
  loadRunning()
  loadRecent()
  loadTrend()
}

// 关闭弹窗时清理定时器
watch(showDetailModal, (val) => {
  if (!val && detailRefreshTimer) {
    clearInterval(detailRefreshTimer)
    detailRefreshTimer = null
  }
})

onMounted(() => {
  loadAll()
  refreshTimer = setInterval(loadAll, 30000)
  nextTick(() => {
    initPieChart()
    initLineChart()
  })
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (detailRefreshTimer) clearInterval(detailRefreshTimer)
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
  lineChart?.dispose()
})

// 监听统计数据变化，更新图表
watch(stats, () => {
  updatePieChart()
}, { deep: true })

watch(trendData, () => {
  updateLineChart()
}, { deep: true })
</script>

<style scoped>
.pipeline-monitor { padding: 0; }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-info { display: flex; flex-direction: column; gap: 4px; }

.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #1e293b;
}

.title-icon { color: #f0a020; }
.page-desc { margin: 0; color: #64748b; font-size: 14px; }

/* 统计卡片 */
.stats-row { margin-bottom: 20px; }

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: var(--bg-primary);
  border-radius: 16px;
  box-shadow: var(--shadow-sm);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.icon-total { background: var(--dp-gradient-primary, linear-gradient(135deg, #2563eb, #1e40af)); }
.icon-published { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); }
.icon-today { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
.icon-running { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); }

.stat-info { flex: 1; }
.stat-value { font-size: 32px; font-weight: 700; color: #1e293b; line-height: 1; }
.stat-label { font-size: 14px; color: #64748b; margin-top: 6px; }

.animated-number { animation: pulse-number 2s infinite; }
@keyframes pulse-number { 0%, 100% { opacity: 1; } 50% { opacity: 0.6; } }

/* 运行中卡片 */
.running-card { border-radius: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-title { display: flex; align-items: center; gap: 8px; font-weight: 600; }

.running-list { display: flex; flex-direction: column; gap: 12px; }

.running-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: linear-gradient(135deg, var(--bg-secondary) 0%, var(--bg-secondary) 100%);
  border-radius: 12px;
  border: 1px solid var(--border-light);
  cursor: pointer;
  transition: all 0.2s;
}

.running-item:hover {
  transform: translateX(4px);
  box-shadow: 0 4px 12px rgba(240, 160, 32, 0.2);
}

.item-main { display: flex; align-items: center; gap: 12px; }

.item-icon {
  width: 40px;
  height: 40px;
  background: var(--bg-primary);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #f0a020;
}

.item-title { font-weight: 600; color: #1e293b; }
.item-meta { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #64748b; margin-top: 4px; }
.meta-tag { background: var(--bg-secondary); padding: 2px 8px; border-radius: 4px; font-family: monospace; font-size: 12px; }

.item-status { display: flex; align-items: center; gap: 12px; }
.status-indicator { display: flex; align-items: center; gap: 6px; color: #f0a020; font-weight: 500; }
.status-dot { width: 8px; height: 8px; background: #f0a020; border-radius: 50%; animation: blink 1s infinite; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }

/* 执行详情弹窗 */
.execution-detail { display: flex; flex-direction: column; gap: 20px; }

.detail-header {
  background: #f8fafc;
  border-radius: 12px;
  padding: 16px;
}

.detail-info-row {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.info-item { display: flex; flex-direction: column; gap: 4px; }
.info-item label { font-size: 12px; color: #94a3b8; }
.info-item span { font-size: 14px; color: #1e293b; }
.info-item .code { font-family: monospace; background: #e2e8f0; padding: 2px 8px; border-radius: 4px; }
.info-item .duration { font-weight: 600; color: #2080f0; }

/* 节点执行状态 */
.nodes-section { background: var(--bg-primary); border: 1px solid var(--border-light); border-radius: 12px; padding: 16px; }

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 16px;
}

.nodes-timeline { display: flex; flex-direction: column; }

.node-item {
  display: flex;
  gap: 12px;
  padding: 8px 0;
}

.node-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 24px;
}

.node-dot {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e2e8f0;
  color: #64748b;
  flex-shrink: 0;
}

.node-item.success .node-dot { background: #dcfce7; color: #16a34a; }
.node-item.failed .node-dot { background: #fee2e2; color: #dc2626; }
.node-item.running .node-dot { background: #fef3c7; color: #f59e0b; }
.node-item.skipped .node-dot { background: #f1f5f9; color: #94a3b8; }

.node-line {
  width: 2px;
  flex: 1;
  min-height: 20px;
  background: #e2e8f0;
  margin: 4px 0;
}

.node-content { flex: 1; }

.node-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.node-name { font-weight: 500; color: #1e293b; }
.node-type { font-size: 12px; color: #94a3b8; background: #f1f5f9; padding: 2px 6px; border-radius: 4px; }
.node-time { font-size: 12px; color: #64748b; margin-left: auto; }
.node-message { font-size: 13px; color: #64748b; margin-top: 4px; }

.spinning { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

/* 日志区域 */
.log-section {
  background: #1e293b;
  border-radius: 12px;
  overflow: hidden;
}

.log-section .section-title {
  color: #e2e8f0;
  padding: 12px 16px;
  margin-bottom: 0;
  background: #0f172a;
  border-bottom: 1px solid #334155;
}

.log-container {
  max-height: 300px;
  overflow: auto;
  padding: 16px;
}

.log-content {
  margin: 0;
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #e2e8f0;
  white-space: pre-wrap;
  word-break: break-all;
}

/* 滚动条 */
.log-container::-webkit-scrollbar { width: 8px; }
.log-container::-webkit-scrollbar-track { background: #334155; }
.log-container::-webkit-scrollbar-thumb { background: #64748b; border-radius: 4px; }

/* 移动端适配 */
@media (max-width: 768px) {
  .pipeline-monitor-page { padding: 10px; }
  .page-header-stats {
    flex-direction: row !important;
    overflow-x: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;
    gap: 8px;
    padding-bottom: 4px;
  }
  .page-header-stats::-webkit-scrollbar { display: none; }
  .stat-item { min-width: 120px; flex-shrink: 0; }
  .stats-grid { grid-template-columns: repeat(2, 1fr) !important; gap: 8px; }
  .monitor-grid { grid-template-columns: 1fr !important; }
  .execution-list .n-data-table { font-size: 12px; }
  .log-container { max-height: 200px; padding: 10px; }
  .log-content { font-size: 11px; }
  .section-title { font-size: 14px; }
  .main-card { border-radius: 12px !important; }
}














</style>

<style>
/* PipelineMonitor 深色模式（非 scoped） */
html.dark .pipeline-monitor { color: #e2e8f0 !important; }
html.dark .page-title { color: #f1f5f9 !important; }
html.dark .page-desc { color: #94a3b8 !important; }
html.dark .stat-card {
  background: #1e293b !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important;
}
html.dark .stat-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4) !important;
}
html.dark .stat-value { color: #f1f5f9 !important; }
html.dark .stat-label { color: #94a3b8 !important; }
html.dark .running-item {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.12) 0%, rgba(245, 158, 11, 0.06) 100%) !important;
  border-color: rgba(245, 158, 11, 0.3) !important;
}
html.dark .running-item:hover {
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.2) !important;
}
html.dark .item-icon {
  background: #243044 !important;
  color: #fbbf24 !important;
}
html.dark .item-title { color: #f1f5f9 !important; }
html.dark .item-meta { color: #94a3b8 !important; }
html.dark .meta-tag { background: #243044 !important; color: #cbd5e1 !important; }
html.dark .detail-header {
  background: #1a2536 !important;
}
html.dark .info-item label { color: #64748b !important; }
html.dark .info-item span { color: #e2e8f0 !important; }
html.dark .info-item .code { background: #0f172a !important; color: #e2e8f0 !important; }
html.dark .nodes-section {
  background: #1e293b !important;
  border-color: #334155 !important;
}
html.dark .section-title { color: #e2e8f0 !important; }
html.dark .node-name { color: #e2e8f0 !important; }
html.dark .node-type { background: #243044 !important; color: #94a3b8 !important; }
html.dark .node-message { color: #94a3b8 !important; }
html.dark .node-dot { background: #334155 !important; color: #94a3b8 !important; }
html.dark .node-line { background: #334155 !important; }
html.dark .node-item.success .node-dot { background: rgba(22, 163, 74, 0.2) !important; color: #34d399 !important; }
html.dark .node-item.failed .node-dot { background: rgba(220, 38, 38, 0.2) !important; color: #f87171 !important; }
html.dark .node-item.running .node-dot { background: rgba(245, 158, 11, 0.2) !important; color: #fbbf24 !important; }
html.dark .node-item.skipped .node-dot { background: #243044 !important; color: #64748b !important; }
</style>
