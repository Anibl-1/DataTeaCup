<template>
  <div class="slow-query-container">
    <!-- Page_Header_Stats: 慢查询统计概览 (Req 8.7) -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><SearchOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ statsOverview.totalCount }}</span>
          <span class="stat-label">{{ t('slowQuery.totalCount') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="24"><TimeOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ statsOverview.avgDuration }}</span>
          <span class="stat-label">{{ t('slowQuery.avgDuration') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-error">
          <n-icon size="24"><FlashOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ statsOverview.maxDuration }}</span>
          <span class="stat-label">{{ t('slowQuery.maxDuration') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><TodayOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ statsOverview.todayCount }}</span>
          <span class="stat-label">{{ t('slowQuery.todayNew') }}</span>
        </div>
      </div>
    </div>

    <!-- Main_Card: 慢查询分析 (Req 1.1) -->
    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="22" color="var(--color-primary)" class="header-icon"><SearchOutline /></n-icon>
          <span>{{ t('slowQuery.title') }}</span>
        </div>
      </template>

      <n-tabs v-model:value="activeTab" type="line" animated>
        <!-- 慢查询列表 -->
        <n-tab-pane name="list" :tab="t('slowQuery.tabList')">
          <div class="toolbar">
            <n-select
              v-model:value="filterDataSourceId"
              :options="dataSourceOptions"
              :placeholder="t('slowQuery.allDataSources')"
              clearable
              style="width: 180px"
              @update:value="handleFilterChange"
            />
            <n-input-number
              v-model:value="filterMinTime"
              :min="0"
              :placeholder="t('slowQuery.minDuration')"
              clearable
              style="width: 160px"
              @update:value="handleFilterChange"
            />
            <n-date-picker
              v-model:value="dateRange"
              type="daterange"
              clearable
              @update:value="handleDateRangeChange"
            />
            <n-button :loading="listLoading" @click="loadList">
              <template #icon><n-icon><RefreshOutline /></n-icon></template>
              {{ t('common.refresh') }}
            </n-button>
            <n-popover v-if="thresholdMs > 0" trigger="click" placement="bottom">
              <template #trigger>
                <n-tag type="info" size="small" round style="cursor: pointer">
                  {{ t('slowQuery.currentThreshold') }}: {{ thresholdMs }}ms ✏️
                </n-tag>
              </template>
              <div style="display: flex; gap: 8px; align-items: center">
                <n-input-number v-model:value="newThreshold" :min="100" :max="60000" :step="500" size="small" style="width: 140px" />
                <span style="font-size: 12px; color: #94a3b8">ms</span>
                <n-button type="primary" size="small" :loading="savingThreshold" @click="handleUpdateThreshold">{{ t('common.save') }}</n-button>
              </div>
            </n-popover>
          </div>
          <n-data-table
            :columns="listColumns"
            :data="queryList"
            :loading="listLoading"
            :bordered="false"
            :scroll-x="1200"
            striped
            :max-height="500"
            class="custom-table"
          />
          <div class="pagination-wrapper">
            <div class="pagination-info">
              <n-tag type="info" size="small" round>
                {{ t('slowQuery.totalRecords', { total: listTotal }) }}
              </n-tag>
            </div>
            <n-pagination
              v-model:page="listPage"
              :page-size="listPageSize"
              :item-count="listTotal"
              :page-sizes="[10, 20, 50]"
              show-size-picker
              show-quick-jumper
              @update:page="loadList"
            />
          </div>
        </n-tab-pane>

        <!-- 趋势图 -->
        <n-tab-pane name="trend" :tab="t('slowQuery.tabTrend') || '趋势分析'">
          <div class="toolbar">
            <n-select
              v-model:value="trendDays"
              :options="trendDaysOptions"
              style="width: 150px"
              @update:value="loadTrend"
            />
            <n-select
              v-model:value="trendDataSourceId"
              :options="dataSourceOptions"
              :placeholder="t('slowQuery.allDataSources')"
              clearable
              style="width: 180px"
              @update:value="loadTrend"
            />
            <n-button :loading="trendLoading" @click="loadTrend">
              <template #icon><n-icon><RefreshOutline /></n-icon></template>
              {{ t('common.refresh') }}
            </n-button>
          </div>
          <div ref="trendChartRef" style="width: 100%; height: 350px"></div>
          <n-empty v-if="!trendLoading && trendData.length === 0" :description="t('common.noData')" style="margin-top: 60px" />
        </n-tab-pane>

        <!-- 统计分析 -->
        <n-tab-pane name="stats" :tab="t('slowQuery.tabStats')">
          <div class="toolbar">
            <n-select
              v-model:value="statsHours"
              :options="hoursOptions"
              style="width: 150px"
            />
            <n-button :loading="statsLoading" @click="loadStats">
              <template #icon><n-icon><RefreshOutline /></n-icon></template>
              {{ t('slowQuery.analyze') }}
            </n-button>
          </div>

          <!-- 按数据源统计 -->
          <n-card :title="t('slowQuery.dsStats')" size="small" style="margin-bottom: 16px">
            <n-data-table
              v-if="statsByDataSource.length > 0"
              :columns="dsStatsColumns"
              :data="statsByDataSource"
              :bordered="false"
              size="small"
              striped
              class="custom-table"
            />
            <n-empty v-else :description="t('common.noData')" />
          </n-card>

          <!-- TOP慢查询 -->
          <n-card :title="t('slowQuery.topSlowQueries')" size="small">
            <n-data-table
              v-if="topSlowQueries.length > 0"
              :columns="topQueryColumns"
              :data="topSlowQueries"
              :bordered="false"
              size="small"
              striped
              class="custom-table"
            />
            <n-empty v-else :description="t('common.noData')" />
          </n-card>
        </n-tab-pane>

        <!-- 优化建议 -->
        <n-tab-pane name="suggestions" :tab="t('slowQuery.tabSuggestions')">
          <div class="toolbar">
            <n-button :loading="statsLoading" @click="loadStats">
              <template #icon><n-icon><RefreshOutline /></n-icon></template>
              {{ t('slowQuery.refreshSuggestions') }}
            </n-button>
          </div>
          <div v-if="suggestions.length > 0">
            <n-card v-for="(item, idx) in suggestions" :key="idx" size="small" style="margin-bottom: 12px">
              <template #header>
                <div style="display: flex; align-items: center; gap: 8px">
                  <n-tag type="warning" size="small">{{ t('slowQuery.occurrence', { count: item.occurrence }) }}</n-tag>
                  <span style="font-size: 13px">{{ t('slowQuery.avgTime') }}: {{ item.avgTime }}ms</span>
                </div>
              </template>
              <n-code :code="item.sqlText" language="sql" style="margin-bottom: 12px; font-size: 12px" />
              <div>
                <n-tag
                  v-for="(tip, tipIdx) in item.suggestions"
                  :key="tipIdx"
                  type="info"
                  size="small"
                  style="margin: 2px 4px 2px 0"
                >
                  {{ tip }}
                </n-tag>
              </div>
            </n-card>
          </div>
          <n-empty v-else :description="t('slowQuery.noSuggestions')" />
        </n-tab-pane>
      </n-tabs>
    </n-card>

    <!-- SQL 详情弹窗 -->
    <n-modal v-model:show="sqlDetailVisible" :title="t('slowQuery.sqlDetail')" preset="dialog" style="width: 700px; border-radius: 16px;">
      <n-descriptions :column="2" bordered size="small" style="margin-bottom: 12px">
        <n-descriptions-item :label="t('slowQuery.dataSource')">{{ sqlDetail.dataSourceName }}</n-descriptions-item>
        <n-descriptions-item :label="t('slowQuery.database')">{{ sqlDetail.databaseName }}</n-descriptions-item>
        <n-descriptions-item :label="t('slowQuery.executionTime')">{{ sqlDetail.executionTime }}ms</n-descriptions-item>
        <n-descriptions-item :label="t('slowQuery.rowsExamined')">{{ sqlDetail.rowsExamined || '-' }}</n-descriptions-item>
        <n-descriptions-item :label="t('slowQuery.rowsReturned')">{{ sqlDetail.rowsReturned || '-' }}</n-descriptions-item>
        <n-descriptions-item :label="t('slowQuery.queryTime')">{{ formatTime(sqlDetail.queryTime) }}</n-descriptions-item>
        <n-descriptions-item :label="t('slowQuery.user')">{{ sqlDetail.userName || '-' }}</n-descriptions-item>
        <n-descriptions-item :label="t('slowQuery.clientIp')">{{ sqlDetail.clientIp || '-' }}</n-descriptions-item>
      </n-descriptions>
      <n-card :title="t('slowQuery.sqlStatement')" size="small">
        <n-code :code="sqlDetail.sqlText || ''" language="sql" word-wrap />
      </n-card>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, h, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { NButton, NIcon, NTag, useMessage } from 'naive-ui'
import { RefreshOutline, SearchOutline, TimeOutline, FlashOutline, TodayOutline } from '@vicons/ionicons5'
import { systemMonitorApi } from '@/api/systemMonitor'
import echarts from '@/utils/echarts'
import { useI18n } from '@/i18n'
import type { DataTableColumns } from 'naive-ui'

const { t } = useI18n()
const message = useMessage()
const activeTab = ref('list')

// ==================== 数据源筛选 & 阈值 ====================
const filterDataSourceId = ref<number | null>(null)
const filterMinTime = ref<number | null>(null)
const dataSourceOptions = ref<any[]>([])
const thresholdMs = ref(0)

const loadDataSources = async () => {
  try {
    const res = await systemMonitorApi.getSlowQueryDataSources() as any
    const list = res.data || []
    dataSourceOptions.value = list.map((ds: any) => ({
      label: ds.data_source_name || ('ID: ' + ds.data_source_id),
      value: ds.data_source_id
    }))
  } catch { /* ignore */ }
}

const loadThreshold = async () => {
  try {
    const res = await systemMonitorApi.getSlowQueryThreshold() as any
    thresholdMs.value = res.data || 3000
  } catch { thresholdMs.value = 3000 }
}

const handleFilterChange = () => {
  listPage.value = 1
  loadList()
}

// ==================== 阈值设置 ====================
const newThreshold = ref(3000)
const savingThreshold = ref(false)

const handleUpdateThreshold = async () => {
  if (!newThreshold.value || newThreshold.value < 100) {
    message.warning(t('slowQuery.thresholdMin') || '阈值不能小于100ms')
    return
  }
  savingThreshold.value = true
  try {
    await systemMonitorApi.updateSlowQueryThreshold(newThreshold.value)
    thresholdMs.value = newThreshold.value
    message.success(t('common.operationSuccess'))
  } catch (e: any) {
    message.error(e.message || t('common.operationFailed'))
  } finally {
    savingThreshold.value = false
  }
}

// ==================== 趋势分析 ====================
const trendChartRef = ref<HTMLElement | null>(null)
let trendChart: any = null
const trendLoading = ref(false)
const trendDays = ref(7)
const trendDataSourceId = ref<number | null>(null)
const trendData = ref<any[]>([])

const trendDaysOptions = [
  { label: t('slowQuery.last1Day') || '最近1天', value: 1 },
  { label: t('slowQuery.last7Days'), value: 7 },
  { label: t('slowQuery.last30Days'), value: 30 }
]

const loadTrend = async () => {
  trendLoading.value = true
  try {
    const hours = trendDays.value * 24
    const res = await systemMonitorApi.getSlowQueryStats(hours) as any
    const data = res.data || res
    const byDs = data.byDataSource || []
    // Use byDataSource to build trend-like data per datasource
    trendData.value = byDs
    await nextTick()
    renderTrendChart(byDs)
  } catch { /* handled */ } finally {
    trendLoading.value = false
  }
}

const renderTrendChart = (byDs: any[]) => {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  
  const filtered = trendDataSourceId.value
    ? byDs.filter((ds: any) => ds.data_source_id === trendDataSourceId.value)
    : byDs

  const names = filtered.map((ds: any) => ds.data_source_name || ('DS#' + ds.data_source_id))
  const counts = filtered.map((ds: any) => ds.query_count || 0)
  const avgTimes = filtered.map((ds: any) => ds.avg_time || 0)
  const maxTimes = filtered.map((ds: any) => ds.max_time || 0)

  const isDark = document.documentElement.classList.contains('dark')
  const textColor = isDark ? '#cbd5e1' : '#64748b'

  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: [t('slowQuery.slowQueryCount'), t('slowQuery.avgDuration'), t('slowQuery.maxDuration')], textStyle: { color: textColor } },
    grid: { left: 60, right: 40, top: 50, bottom: 40 },
    xAxis: { type: 'category', data: names, axisLabel: { color: textColor, rotate: names.length > 5 ? 30 : 0 } },
    yAxis: [
      { type: 'value', name: t('slowQuery.count') || '次数', axisLabel: { color: textColor }, nameTextStyle: { color: textColor } },
      { type: 'value', name: 'ms', axisLabel: { color: textColor }, nameTextStyle: { color: textColor } }
    ],
    series: [
      { name: t('slowQuery.slowQueryCount'), type: 'bar', data: counts, itemStyle: { color: '#3b82f6', borderRadius: [4, 4, 0, 0] } },
      { name: t('slowQuery.avgDuration'), type: 'line', yAxisIndex: 1, data: avgTimes, smooth: true, itemStyle: { color: '#f59e0b' } },
      { name: t('slowQuery.maxDuration'), type: 'line', yAxisIndex: 1, data: maxTimes, smooth: true, itemStyle: { color: '#ef4444' }, lineStyle: { type: 'dashed' } }
    ]
  }, true)
}

// ==================== 统计概览 ====================
const statsOverview = reactive({
  totalCount: '-' as string | number,
  avgDuration: '-' as string | number,
  maxDuration: '-' as string | number,
  todayCount: '-' as string | number
})

const loadStatsOverview = async () => {
  try {
    const res = await systemMonitorApi.getSlowQueryStats(24) as any
    const data = res.data || res
    const byDs = data.byDataSource || []
    const totalCount = byDs.reduce((sum: number, ds: any) => sum + (ds.query_count || 0), 0)
    const totalAvg = byDs.length > 0
      ? Math.round(byDs.reduce((sum: number, ds: any) => sum + (ds.avg_time || 0), 0) / byDs.length)
      : 0
    const maxTime = byDs.reduce((max: number, ds: any) => Math.max(max, ds.max_time || 0), 0)

    statsOverview.totalCount = totalCount
    statsOverview.avgDuration = totalAvg
    statsOverview.maxDuration = maxTime

    // Today's count: load list for today and use total
    const today = new Date()
    const todayStr = formatDate(today)
    const tomorrow = new Date(today)
    tomorrow.setDate(tomorrow.getDate() + 1)
    const tomorrowStr = formatDate(tomorrow)
    try {
      const todayRes = await systemMonitorApi.getSlowQueryList(1, 1, todayStr, tomorrowStr) as any
      const todayData = todayRes.data || todayRes
      statsOverview.todayCount = todayData.total || 0
    } catch {
      statsOverview.todayCount = '-'
    }
  } catch {
    statsOverview.totalCount = '-'
    statsOverview.avgDuration = '-'
    statsOverview.maxDuration = '-'
    statsOverview.todayCount = '-'
  }
}

// ==================== 日期范围筛选 ====================
const dateRange = ref<[number, number] | null>(null)

const handleDateRangeChange = (value: [number, number] | null) => {
  dateRange.value = value
  listPage.value = 1
  loadList()
}

// ==================== 慢查询列表 ====================
const queryList = ref<any[]>([])
const listLoading = ref(false)
const listPage = ref(1)
const listPageSize = 20
const listTotal = ref(0)

const sqlDetailVisible = ref(false)
const sqlDetail = ref<any>({})

const listColumns: DataTableColumns<any> = [
  {
    title: t('slowQuery.queryTime'), key: 'queryTime', width: 170,
    render: (row) => formatTime(row.queryTime)
  },
  { title: t('slowQuery.dataSource'), key: 'dataSourceName', width: 120 },
  { title: t('slowQuery.database'), key: 'databaseName', width: 100 },
  {
    title: t('slowQuery.sql'), key: 'sqlText', ellipsis: { tooltip: true },
    render: (row) => h('span', {
      style: 'cursor: pointer; color: var(--color-primary)',
      onClick: () => { sqlDetail.value = row; sqlDetailVisible.value = true }
    }, row.sqlText?.substring(0, 80) + (row.sqlText?.length > 80 ? '...' : ''))
  },
  {
    title: t('slowQuery.executionTime'), key: 'executionTime', width: 110,
    sorter: (a: any, b: any) => a.executionTime - b.executionTime,
    render: (row) => {
      const ms = row.executionTime
      const type = ms > 10000 ? 'error' : ms > 3000 ? 'warning' : 'info'
      return h(NTag, { type, size: 'small' }, { default: () => ms + 'ms' })
    }
  },
  {
    title: t('slowQuery.rowsExamined'), key: 'rowsExamined', width: 100,
    render: (row) => row.rowsExamined != null ? formatNumber(row.rowsExamined) : '-'
  },
  { title: t('slowQuery.user'), key: 'userName', width: 80 }
]

const loadList = async () => {
  listLoading.value = true
  try {
    let startDate: string | undefined
    let endDate: string | undefined
    if (dateRange.value) {
      startDate = formatDate(new Date(dateRange.value[0]))
      endDate = formatDate(new Date(dateRange.value[1] + 86400000)) // end of day
    }
    const res = await systemMonitorApi.getSlowQueryList(
      listPage.value, listPageSize, startDate, endDate,
      filterDataSourceId.value || undefined, filterMinTime.value || undefined
    ) as any
    const data = res.data || res
    queryList.value = data.list || []
    listTotal.value = data.total || 0
  } catch { /* handled */ } finally {
    listLoading.value = false
  }
}

// ==================== 统计分析 ====================
const statsLoading = ref(false)
const statsHours = ref(24)
const statsByDataSource = ref<any[]>([])
const topSlowQueries = ref<any[]>([])
const suggestions = ref<any[]>([])

const hoursOptions = [
  { label: t('slowQuery.last1Hour'), value: 1 },
  { label: t('slowQuery.last6Hours'), value: 6 },
  { label: t('slowQuery.last24Hours'), value: 24 },
  { label: t('slowQuery.last7Days'), value: 168 },
  { label: t('slowQuery.last30Days'), value: 720 }
]

const dsStatsColumns: DataTableColumns<any> = [
  { title: t('slowQuery.dataSource'), key: 'data_source_name' },
  { title: t('slowQuery.slowQueryCount'), key: 'query_count', width: 120, sorter: (a: any, b: any) => a.query_count - b.query_count },
  { title: t('slowQuery.avgDuration'), key: 'avg_time', width: 130 },
  { title: t('slowQuery.maxDuration'), key: 'max_time', width: 130 },
  { title: t('slowQuery.totalRowsExamined'), key: 'total_rows_examined', width: 130, render: (row) => formatNumber(row.total_rows_examined) }
]

const topQueryColumns: DataTableColumns<any> = [
  {
    title: t('slowQuery.sql'), key: 'sql_text', ellipsis: { tooltip: true },
    render: (row) => row.sql_text?.substring(0, 100) + (row.sql_text?.length > 100 ? '...' : '')
  },
  { title: t('slowQuery.occurrenceCount'), key: 'occurrence', width: 100, sorter: (a: any, b: any) => a.occurrence - b.occurrence },
  { title: t('slowQuery.avgDuration'), key: 'avg_time', width: 130 },
  { title: t('slowQuery.maxDuration'), key: 'max_time', width: 130 }
]

const loadStats = async () => {
  statsLoading.value = true
  try {
    const res = await systemMonitorApi.getSlowQueryStats(statsHours.value) as any
    const data = res.data || res
    statsByDataSource.value = data.byDataSource || []
    topSlowQueries.value = data.topSlowQueries || []
    suggestions.value = data.suggestions || []
  } catch { /* handled */ } finally {
    statsLoading.value = false
  }
}

// ==================== 工具函数 ====================
const formatTime = (t: string): string => {
  if (!t) return '-'
  try {
    return new Date(t).toLocaleString('zh-CN', { hour12: false })
  } catch { return t }
}

const formatNumber = (n: number | string): string => {
  if (n == null) return '-'
  return Number(n).toLocaleString()
}

const formatDate = (d: Date): string => {
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// ==================== 初始化 ====================
onMounted(() => {
  loadList()
  loadStats()
  loadStatsOverview()
  loadDataSources()
  loadThreshold().then(() => { newThreshold.value = thresholdMs.value })
})

onBeforeUnmount(() => {
  if (trendChart) { trendChart.dispose(); trendChart = null }
})

watch(activeTab, (tab) => {
  if (tab === 'trend') {
    nextTick(() => loadTrend())
  }
})
</script>

<style scoped>
.slow-query-container {
  padding: 16px;
}
</style>
