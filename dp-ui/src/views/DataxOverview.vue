<template>
  <div class="datax-overview-page">
    <!-- Page_Header_Stats: DataX 概览统计 (Req 1.1, 10.1) -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><ServerOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ statistics.dsCount }}</span>
          <span class="stat-label">数据源总数</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><ListOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ statistics.jobCount }}</span>
          <span class="stat-label">任务总数</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-info">
          <n-icon size="24"><PlayCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ statistics.runningCount }}</span>
          <span class="stat-label">运行中任务</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="24"><TimeOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ statistics.todayCount }}</span>
          <span class="stat-label">今日执行次数</span>
        </div>
      </div>
    </div>

    <!-- 执行统计图表 -->
    <div class="chart-row">
      <n-card class="chart-card">
        <template #header>
          <div class="card-header-custom">
            <n-icon size="20" color="var(--color-primary)" class="header-icon"><BarChartOutline /></n-icon>
            <span>执行状态统计</span>
          </div>
        </template>
        <div class="status-chart">
          <div class="chart-bars">
            <div class="chart-bar-item">
              <div class="bar-label">成功</div>
              <div class="bar-track">
                <div class="bar-fill success" :style="{ width: getBarWidth(statusStats.success) }"></div>
              </div>
              <div class="bar-value">{{ statusStats.success }}</div>
            </div>
            <div class="chart-bar-item">
              <div class="bar-label">失败</div>
              <div class="bar-track">
                <div class="bar-fill error" :style="{ width: getBarWidth(statusStats.failed) }"></div>
              </div>
              <div class="bar-value">{{ statusStats.failed }}</div>
            </div>
            <div class="chart-bar-item">
              <div class="bar-label">运行中</div>
              <div class="bar-track">
                <div class="bar-fill running" :style="{ width: getBarWidth(statusStats.running) }"></div>
              </div>
              <div class="bar-value">{{ statusStats.running }}</div>
            </div>
          </div>
        </div>
      </n-card>
      <n-card class="chart-card">
        <template #header>
          <div class="card-header-custom">
            <n-icon size="20" color="var(--color-primary)" class="header-icon"><PieChartOutline /></n-icon>
            <span>数据源类型分布</span>
          </div>
        </template>
        <div class="pie-chart">
          <div class="pie-legend">
            <div v-for="item in dsTypeStats" :key="item.type" class="legend-item">
              <span class="legend-dot" :style="{ backgroundColor: item.color }"></span>
              <span class="legend-label">{{ item.type }}</span>
              <span class="legend-value">{{ item.count }}</span>
            </div>
            <n-empty v-if="dsTypeStats.length === 0" description="暂无数据源类型数据" />
          </div>
        </div>
      </n-card>
    </div>

    <div class="content-row">
      <!-- 左侧：快捷操作和使用说明 -->
      <div class="left-col">
        <!-- 快捷操作 -->
        <n-card>
          <template #header>
            <div class="card-header-custom">
              <n-icon size="20" color="var(--color-primary)" class="header-icon"><FlashOutline /></n-icon>
              <span>快捷操作</span>
            </div>
          </template>
          <div class="quick-actions">
            <n-button type="primary" block @click="router.push('/datax/datasource')">
              <template #icon><n-icon :component="AddOutline" /></template>
              新增数据源
            </n-button>
            <n-button type="success" block @click="handleAddJob">
              <template #icon><n-icon :component="AddOutline" /></template>
              新建传输任务
            </n-button>
            <n-button type="info" block @click="router.push('/datax/job')">
              <template #icon><n-icon :component="ListOutline" /></template>
              查看任务列表
            </n-button>
            <n-button block @click="router.push('/datax/log')">
              <template #icon><n-icon :component="DocumentTextOutline" /></template>
              查看执行日志
            </n-button>
          </div>
        </n-card>

        <!-- 使用说明 -->
        <n-card>
          <template #header>
            <div class="card-header-custom">
              <n-icon size="20" color="var(--color-primary)" class="header-icon"><InformationCircleOutline /></n-icon>
              <span>使用说明</span>
            </div>
          </template>
          <div class="usage-guide">
            <p><strong>1. 配置数据源</strong><br>首先在数据源管理中添加源端和目标端数据库连接</p>
            <p><strong>2. 创建传输任务</strong><br>选择源表和目标表，配置字段映射关系</p>
            <p><strong>3. 执行任务</strong><br>手动执行或配置定时调度自动执行</p>
            <p><strong>4. 查看日志</strong><br>在执行日志中查看任务执行详情</p>
          </div>
        </n-card>
      </div>

      <!-- 右侧：最近执行记录和数据源列表 -->
      <div class="right-col">
        <!-- 最近执行记录 -->
        <n-card>
          <template #header>
            <div class="card-header-custom">
              <n-icon size="20" color="var(--color-primary)" class="header-icon"><TimeOutline /></n-icon>
              <span>最近执行记录</span>
            </div>
          </template>
          <template #header-extra>
            <n-button size="small" text @click="router.push('/datax/log')">
              查看全部 <n-icon :component="ArrowForwardOutline" />
            </n-button>
          </template>
          <n-empty v-if="!loadingLogs && recentLogs.length === 0" description="暂无执行记录" class="empty-state" />
          <n-data-table
            v-else
            :columns="logColumns"
            :data="recentLogs"
            :loading="loadingLogs"
            size="small"
            :bordered="false"
            striped
            class="custom-table"
          />
        </n-card>

        <!-- 数据源列表 -->
        <n-card>
          <template #header>
            <div class="card-header-custom">
              <n-icon size="20" color="var(--color-primary)" class="header-icon"><ServerOutline /></n-icon>
              <span>数据源列表</span>
            </div>
          </template>
          <template #header-extra>
            <n-button size="small" text @click="router.push('/datax/datasource')">
              管理数据源 <n-icon :component="ArrowForwardOutline" />
            </n-button>
          </template>
          <n-empty v-if="!loadingDs && datasources.length === 0" description="暂无数据源" class="empty-state" />
          <n-data-table
            v-else
            :columns="dsColumns"
            :data="datasources"
            :loading="loadingDs"
            size="small"
            :bordered="false"
            striped
            class="custom-table"
          />
        </n-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { NTag, NIcon } from 'naive-ui'
import {
  ServerOutline, ListOutline, PlayCircleOutline, TimeOutline,
  FlashOutline, AddOutline, DocumentTextOutline, InformationCircleOutline, ArrowForwardOutline,
  BarChartOutline, PieChartOutline
} from '@vicons/ionicons5'
import { getJobList, getJobLogList, getOverviewStatistics, getLogStatistics } from '@/api/dataxJob'
import { getDataSourceList } from '@/api/dataSource'

const router = useRouter()

// 统计数据
const statistics = ref({
  dsCount: 0,
  jobCount: 0,
  runningCount: 0,
  todayCount: 0
})

// 执行状态统计
const statusStats = ref({
  success: 0,
  failed: 0,
  running: 0
})

// 数据源类型统计
const dsTypeStats = ref<{ type: string; count: number; color: string }[]>([])

// 最近执行记录
const recentLogs = ref<any[]>([])
const loadingLogs = ref(false)

// 数据源列表
const datasources = ref<any[]>([])
const loadingDs = ref(false)

// 计算柱状图宽度
const getBarWidth = (value: number) => {
  const total = statusStats.value.success + statusStats.value.failed + statusStats.value.running
  if (total === 0) return '0%'
  const percent = Math.max((value / total) * 100, value > 0 ? 5 : 0)
  return `${percent}%`
}

// 日志表格列
const logColumns = [
  { title: '任务名称', key: 'jobName', ellipsis: { tooltip: true } },
  {
    title: '状态', key: 'status', width: 80,
    render: (row: any) => {
      const status = row.executeStatus ?? row.status
      const type = status === 1 ? 'success' : status === 0 ? 'error' : 'warning'
      const text = status === 1 ? '成功' : status === 0 ? '失败' : '运行中'
      return h(NTag, { type, size: 'small' }, () => text)
    }
  },
  { title: '开始时间', key: 'startTime', width: 160 },
  { title: '耗时', key: 'executeTime', width: 70, render: (row: any) => (row.executeTime || 0) + 's' },
  {
    title: '读取/写入', key: 'count', width: 100,
    render: (row: any) => h('span', [
      h('span', { style: 'color: #10b981;' }, row.readCount || 0),
      ' / ',
      h('span', { style: 'color: #3b82f6;' }, row.writeCount || 0)
    ])
  }
]

// 数据源表格列
const dsColumns = [
  { title: '名称', key: 'name', ellipsis: { tooltip: true } },
  {
    title: '类型', key: 'dbType', width: 100,
    render: (row: any) => {
      const colors: Record<string, string> = { MYSQL: '#4479A1', ORACLE: '#F80000', POSTGRESQL: '#336791', SQLSERVER: '#CC2927' }
      return h(NTag, { size: 'small', style: { backgroundColor: colors[row.dbType] || '#666', color: '#fff' } }, () => row.dbType)
    }
  },
  { title: '主机', key: 'host', width: 140, render: (row: any) => `${row.host}:${row.port}` },
  { title: '数据库', key: 'database', width: 100, ellipsis: { tooltip: true } },
  {
    title: '状态', key: 'status', width: 70,
    render: (row: any) => h(NTag, { type: row.status === 1 ? 'success' : 'default', size: 'small' }, () => row.status === 1 ? '启用' : '禁用')
  }
]

const handleAddJob = () => {
  router.push('/datax/job')
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    const res = await getOverviewStatistics()
    const data = res.data as any
    if (data) {
      statistics.value.dsCount = data.dataSourceCount || 0
      statistics.value.jobCount = data.totalJobs || 0
      statistics.value.runningCount = data.runningJobs || 0
      statistics.value.todayCount = data.todayTotal || 0
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
    try {
      const dsRes = await getDataSourceList({ page: 1, pageSize: 1 })
      statistics.value.dsCount = (dsRes.data as any)?.total || 0
      const jobRes = await getJobList({ page: 1, pageSize: 1 })
      statistics.value.jobCount = (jobRes.data as any)?.total || 0
    } catch { /* fallback failed silently */ }
  }
}

// 加载执行状态统计
const loadStatusStats = async () => {
  try {
    const res = await getLogStatistics()
    const data = res.data as any
    if (data) {
      statusStats.value = {
        success: data.success || 0,
        failed: data.failed || 0,
        running: data.running || 0
      }
    }
  } catch (error) {
    console.error('加载状态统计失败:', error)
    try {
      const allLogs = await getJobLogList({ page: 1, pageSize: 100 })
      const logs = (allLogs.data as any)?.list || []
      let success = 0, failed = 0, running = 0
      logs.forEach((log: any) => {
        const status = log.executeStatus ?? log.status
        if (status === 1) success++
        else if (status === 0) failed++
        else running++
      })
      statusStats.value = { success, failed, running }
    } catch { /* fallback failed silently */ }
  }
}

// 加载数据源类型统计
const loadDsTypeStats = async () => {
  try {
    const res = await getDataSourceList({ page: 1, pageSize: 100 })
    const list = (res.data as any)?.list || []

    const typeColors: Record<string, string> = {
      MYSQL: '#4479A1',
      ORACLE: '#F80000',
      POSTGRESQL: '#336791',
      SQLSERVER: '#CC2927',
      OTHER: '#666666'
    }

    const typeCount: Record<string, number> = {}
    list.forEach((ds: any) => {
      const type = ds.dbType || 'OTHER'
      typeCount[type] = (typeCount[type] || 0) + 1
    })

    dsTypeStats.value = Object.entries(typeCount).map(([type, count]) => ({
      type,
      count,
      color: typeColors[type] || '#666666'
    }))
  } catch (error) {
    console.error('加载数据源类型统计失败:', error)
  }
}

// 加载最近执行记录
const loadRecentLogs = async () => {
  loadingLogs.value = true
  try {
    const res = await getJobLogList({ page: 1, pageSize: 5 })
    recentLogs.value = (res.data as any)?.list || []
  } catch (error) {
    console.error('加载执行记录失败:', error)
  } finally {
    loadingLogs.value = false
  }
}

// 加载数据源列表
const loadDatasources = async () => {
  loadingDs.value = true
  try {
    const res = await getDataSourceList({ page: 1, pageSize: 5 })
    datasources.value = (res.data as any)?.list || []
  } catch (error) {
    console.error('加载数据源失败:', error)
  } finally {
    loadingDs.value = false
  }
}

onMounted(() => {
  loadStatistics()
  loadStatusStats()
  loadDsTypeStats()
  loadRecentLogs()
  loadDatasources()
})
</script>

<style scoped>
.datax-overview-page { padding: 16px; min-height: 100%; }

/* 图表行 */
.chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
.chart-card { min-height: 200px; }

/* 柱状图 */
.status-chart { padding: 10px 0; }
.chart-bars { display: flex; flex-direction: column; gap: 16px; }
.chart-bar-item { display: flex; align-items: center; gap: 12px; }
.bar-label { width: 60px; font-size: 14px; color: #64748b; flex-shrink: 0; }
.bar-track { flex: 1; height: 24px; background: #f1f5f9; border-radius: 6px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 6px; transition: width 0.3s ease; }
.bar-fill.success { background: linear-gradient(90deg, #10b981, #34d399); }
.bar-fill.error { background: linear-gradient(90deg, #ef4444, #f87171); }
.bar-fill.running { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
.bar-value { width: 40px; font-size: 14px; font-weight: 600; color: #1e293b; text-align: right; flex-shrink: 0; }

/* 饼图图例 */
.pie-chart { padding: 10px 0; }
.pie-legend { display: flex; flex-direction: column; gap: 12px; }
.legend-item { display: flex; align-items: center; gap: 10px; padding: 8px 12px; background: #f8fafc; border-radius: 8px; }
.legend-dot { width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0; }
.legend-label { flex: 1; font-size: 14px; color: #64748b; }
.legend-value { font-size: 16px; font-weight: 600; color: #1e293b; }

/* 内容布局 */
.content-row { display: flex; gap: 16px; }
.left-col { width: 320px; flex-shrink: 0; display: flex; flex-direction: column; gap: 16px; }
.right-col { flex: 1; display: flex; flex-direction: column; gap: 16px; }

/* 快捷操作 */
.quick-actions { display: flex; flex-direction: column; gap: 12px; }

/* 使用说明 */
.usage-guide { font-size: 14px; color: #6b7280; line-height: 1.8; }
.usage-guide p { margin: 0 0 12px 0; }
.usage-guide p:last-child { margin-bottom: 0; }
.usage-guide strong { color: #1e293b; }

@media (max-width: 1200px) {
  .chart-row { grid-template-columns: 1fr; }
  .content-row { flex-direction: column; }
  .left-col { width: 100%; }
}

@media (max-width: 768px) {
  .datax-overview-page { padding: 10px; }
  .page-header-stats {
    flex-direction: row !important;
    overflow-x: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;
    gap: 8px;
  }
  .page-header-stats::-webkit-scrollbar { display: none; }
  .stat-item { min-width: 120px; flex-shrink: 0; }
  .chart-row { grid-template-columns: 1fr; gap: 10px; }
  .content-row { flex-direction: column; gap: 10px; }
  .left-col { width: 100%; }
  .main-card { border-radius: 12px !important; }
}

</style>

<style>
/* DataxOverview 深色模式（非 scoped） */
html.dark .bar-label { color: #94a3b8 !important; }
html.dark .bar-track { background: #1a2536 !important; }
html.dark .bar-value { color: #e2e8f0 !important; }
html.dark .legend-item { background: #1a2536 !important; }
html.dark .legend-label { color: #94a3b8 !important; }
html.dark .legend-value { color: #e2e8f0 !important; }
html.dark .usage-guide { color: #94a3b8 !important; }
html.dark .usage-guide strong { color: #e2e8f0 !important; }
</style>
