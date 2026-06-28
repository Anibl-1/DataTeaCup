<template>
  <div class="usage-stats-page">
    <!-- DAU/WAU/MAU 统计卡片 (Req 8.10) -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><PersonOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ dauCount ?? '-' }}</span>
          <span class="stat-label">{{ t('usageStats.dau') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><PeopleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ wauCount ?? '-' }}</span>
          <span class="stat-label">{{ t('usageStats.wau') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-info">
          <n-icon size="24"><TrendingUpOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ mauCount ?? '-' }}</span>
          <span class="stat-label">{{ t('usageStats.mau') }}</span>
        </div>
      </div>
    </div>

    <!-- 日期范围选择器 + CSV 导出 (Req 8.10) -->
    <n-card style="margin-bottom: 16px;">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="22" color="var(--color-primary)" class="header-icon"><CalendarOutline /></n-icon>
          <span>{{ t('usageStats.dateRangeFilter') }}</span>
        </div>
      </template>
      <n-space align="center" justify="space-between">
        <n-space align="center">
          <span style="font-weight: 500; color: var(--text-secondary);">{{ t('usageStats.dateRange') }}</span>
          <n-date-picker
            v-model:value="dateRange"
            type="daterange"
            clearable
            @update:value="handleDateRangeChange"
          />
        </n-space>
        <n-button type="primary" :loading="exportLoading" @click="handleExportCSV">
          <template #icon><n-icon><DownloadOutline /></n-icon></template>
          {{ t('usageStats.exportCSV') }}
        </n-button>
      </n-space>
    </n-card>

    <!-- 功能使用频率 (Req 8.10) -->
    <n-card class="main-card" style="margin-bottom: 16px;">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="22" color="var(--color-primary)" class="header-icon"><BarChartOutline /></n-icon>
          <span>{{ t('usageStats.featureUsage') }}</span>
        </div>
      </template>
      <n-spin :show="featureLoading">
        <EmptyState v-if="featureUsageData.length === 0" :description="t('usageStats.noFeatureData')" />
        <div v-else class="bar-chart">
          <div v-for="item in featureUsageData" :key="item.feature" class="bar-row">
            <span class="bar-label">{{ item.feature }}</span>
            <div class="bar-track">
              <div class="bar-fill" :style="{ width: getBarWidth(item.count, maxFeatureCount) + '%' }" />
            </div>
            <span class="bar-value">{{ item.count }}</span>
          </div>
        </div>
      </n-spin>
    </n-card>

    <!-- 资源访问排行 -->
    <n-card class="main-card" style="margin-bottom: 16px;">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="22" color="var(--color-primary)" class="header-icon"><PodiumOutline /></n-icon>
          <span>{{ t('usageStats.resourceRank') }}</span>
        </div>
      </template>
      <n-data-table
        :columns="resourceColumns"
        :data="resourceRankData"
        :loading="resourceLoading"
        :pagination="false"
        :scroll-x="900"
        striped
        class="custom-table"
      />
    </n-card>

    <!-- 查询性能分布 + 错误率统计 -->
    <div class="two-col-grid">
      <n-card class="main-card">
        <template #header>
          <div class="card-header-custom">
            <n-icon size="22" color="var(--color-primary)" class="header-icon"><SpeedometerOutline /></n-icon>
            <span>{{ t('usageStats.queryPerformance') }}</span>
          </div>
        </template>
        <n-spin :show="perfLoading">
          <EmptyState v-if="perfData.length === 0" :description="t('usageStats.noPerfData')" />
          <div v-else class="bar-chart">
            <div v-for="item in perfData" :key="item.range" class="bar-row">
              <span class="bar-label">{{ item.range }}</span>
              <div class="bar-track">
                <div class="bar-fill bar-fill-perf" :style="{ width: getBarWidth(item.count, maxPerfCount) + '%' }" />
              </div>
              <span class="bar-value">{{ item.count }}</span>
            </div>
          </div>
        </n-spin>
      </n-card>

      <n-card class="main-card">
        <template #header>
          <div class="card-header-custom">
            <n-icon size="22" color="#EF4444" class="header-icon"><WarningOutline /></n-icon>
            <span>{{ t('usageStats.errorRate') }}</span>
          </div>
        </template>
        <n-spin :show="errorLoading">
          <EmptyState v-if="errorData.length === 0" :description="t('usageStats.noErrorData')" />
          <div v-else class="bar-chart">
            <div v-for="item in errorData" :key="item.code" class="bar-row">
              <span class="bar-label">{{ item.code }}</span>
              <div class="bar-track">
                <div class="bar-fill bar-fill-error" :style="{ width: getBarWidth(item.count, maxErrorCount) + '%' }" />
              </div>
              <span class="bar-value">{{ item.count }}</span>
            </div>
          </div>
        </n-spin>
      </n-card>
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, onMounted, h } from 'vue'
import { NTag, useMessage } from 'naive-ui'
import {
  PersonOutline,
  PeopleOutline,
  TrendingUpOutline,
  DownloadOutline,
  BarChartOutline,
  PodiumOutline,
  SpeedometerOutline,
  WarningOutline,
  CalendarOutline
} from '@vicons/ionicons5'
import EmptyState from '@/components/common/EmptyState.vue'
import {
  getDAU,
  getWAU,
  getMAU,
  getFeatureUsage,
  getResourceRank,
  getQueryPerformance,
  getErrorRate,
  exportCSV,
  type ResourceAccessRank
} from '@/api/usageStats'
import { handleApiError } from '@/utils/error'
import { initMessage } from '@/utils/message'
import { useExport } from '@/composables/useExport'
import { useI18n } from '@/i18n'
import dayjs from 'dayjs'

const { t } = useI18n()
const message = useMessage()
initMessage(message)
const { exporting: exportLoading } = useExport({ defaultFilename: 'usage-stats' })

// DAU/WAU/MAU
const dauCount = ref<number | null>(null)
const wauCount = ref<number | null>(null)
const mauCount = ref<number | null>(null)

// 日期范围 (默认最近7天)
const now = Date.now()
const dateRange = ref<[number, number]>([now - 7 * 24 * 3600 * 1000, now])

// 功能使用频率
const featureLoading = ref(false)
const featureUsageData = ref<{ feature: string; count: number }[]>([])
const maxFeatureCount = computed(() => Math.max(...featureUsageData.value.map(d => d.count), 1))

// 资源访问排行
const resourceLoading = ref(false)
const resourceRankData = ref<ResourceAccessRank[]>([])

// 查询性能分布
const perfLoading = ref(false)
const perfData = ref<{ range: string; count: number }[]>([])
const maxPerfCount = computed(() => Math.max(...perfData.value.map(d => d.count), 1))

// 错误率统计
const errorLoading = ref(false)
const errorData = ref<{ code: string; count: number }[]>([])
const maxErrorCount = computed(() => Math.max(...errorData.value.map(d => d.count), 1))

// 资源排行表格列
const resourceColumns = [
  {
    title: '#',
    key: 'rank',
    width: 70,
    render: (_row: ResourceAccessRank, index: number) => {
      const rank = index + 1
      if (rank <= 3) {
        const colors = ['#FFD700', '#C0C0C0', '#CD7F32']
        return h('span', { style: `font-weight: 700; color: ${colors[rank - 1]}` }, `#${rank}`)
      }
      return `#${rank}`
    }
  },
  { title: t('usageStats.resourceId') || 'Resource ID', key: 'resourceId', ellipsis: { tooltip: true } },
  {
    title: t('usageStats.resourceType') || 'Resource Type',
    key: 'resourceType',
    width: 120,
    render: (row: ResourceAccessRank) => h(NTag, { size: 'small', type: 'info' }, { default: () => row.resourceType })
  },
  { title: t('usageStats.accessCount') || 'Access Count', key: 'accessCount', width: 110 },
  { title: t('usageStats.uniqueUsers') || 'Unique Users', key: 'uniqueUsers', width: 110 }
]

// 工具函数
const getDateStrings = () => {
  const [start, end] = dateRange.value
  return {
    startDate: dayjs(start).format('YYYY-MM-DD'),
    endDate: dayjs(end).format('YYYY-MM-DD')
  }
}

const getBarWidth = (value: number, max: number) => {
  if (max <= 0) return 0
  return Math.max((value / max) * 100, 2)
}

// 数据加载
const fetchDAU = async () => {
  try {
    const today = dayjs().format('YYYY-MM-DD')
    const res = await getDAU(today)
    dauCount.value = (res as any).data ?? res ?? 0
  } catch (error) {
    console.error('获取DAU失败:', error)
  }
}

const fetchWAU = async () => {
  try {
    const weekStart = dayjs().startOf('week').format('YYYY-MM-DD')
    const res = await getWAU(weekStart)
    wauCount.value = (res as any).data ?? res ?? 0
  } catch (error) {
    console.error('获取WAU失败:', error)
  }
}

const fetchMAU = async () => {
  try {
    const res = await getMAU(dayjs().year(), dayjs().month() + 1)
    mauCount.value = (res as any).data ?? res ?? 0
  } catch (error) {
    console.error('获取MAU失败:', error)
  }
}

const fetchFeatureUsage = async () => {
  featureLoading.value = true
  try {
    const { startDate, endDate } = getDateStrings()
    const res = await getFeatureUsage(startDate, endDate)
    const data: Record<string, number> = (res as any).data || res || {}
    featureUsageData.value = Object.entries(data)
      .map(([feature, count]) => ({ feature, count }))
      .sort((a, b) => b.count - a.count)
  } catch (error) {
    message.error(handleApiError(error, t('usageStats.fetchFeatureFailed')))
  } finally {
    featureLoading.value = false
  }
}

const fetchResourceRank = async () => {
  resourceLoading.value = true
  try {
    const { startDate, endDate } = getDateStrings()
    const res = await getResourceRank({ resourceType: 'all', topN: 10, startDate, endDate })
    resourceRankData.value = (res as any).data || res || []
  } catch (error) {
    message.error(handleApiError(error, t('usageStats.fetchResourceFailed')))
  } finally {
    resourceLoading.value = false
  }
}

const fetchQueryPerformance = async () => {
  perfLoading.value = true
  try {
    const { startDate, endDate } = getDateStrings()
    const res = await getQueryPerformance(startDate, endDate)
    const data: Record<string, number> = (res as any).data || res || {}
    perfData.value = Object.entries(data)
      .map(([range, count]) => ({ range, count }))
  } catch (error) {
    message.error(handleApiError(error, t('usageStats.fetchPerfFailed')))
  } finally {
    perfLoading.value = false
  }
}

const fetchErrorRate = async () => {
  errorLoading.value = true
  try {
    const { startDate, endDate } = getDateStrings()
    const res = await getErrorRate(startDate, endDate)
    const data: Record<string, number> = (res as any).data || res || {}
    errorData.value = Object.entries(data)
      .map(([code, count]) => ({ code, count }))
      .sort((a, b) => b.count - a.count)
  } catch (error) {
    message.error(handleApiError(error, t('usageStats.fetchErrorFailed')))
  } finally {
    errorLoading.value = false
  }
}

const fetchAllStats = () => {
  fetchFeatureUsage()
  fetchResourceRank()
  fetchQueryPerformance()
  fetchErrorRate()
}

const handleDateRangeChange = (value: [number, number] | null) => {
  if (value) {
    dateRange.value = value
    fetchAllStats()
  }
}

const handleExportCSV = async () => {
  try {
    const { startDate, endDate } = getDateStrings()
    const res = await exportCSV(startDate, endDate)
    const blob = new Blob([res as any], { type: 'text/csv;charset=utf-8;' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `usage-stats-${startDate}-${endDate}.csv`
    link.click()
    window.URL.revokeObjectURL(url)
    message.success(t('usageStats.exportSuccess'))
  } catch (error) {
    message.error(handleApiError(error, t('usageStats.exportFailed')))
  }
}

onMounted(() => {
  fetchDAU()
  fetchWAU()
  fetchMAU()
  fetchAllStats()
})
</script>

<style scoped>
.usage-stats-page {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* 两列布局 */
.two-col-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

/* 简易柱状图 */
.bar-chart {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.bar-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bar-label {
  width: 120px;
  flex-shrink: 0;
  font-size: 13px;
  color: var(--text-primary);
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bar-track {
  flex: 1;
  height: 22px;
  background: var(--bg-tertiary, #f5f5f5);
  border-radius: 4px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--color-primary), #3399FF);
  border-radius: 4px;
  transition: width 0.4s ease;
  min-width: 2px;
}

.bar-fill-perf {
  background: linear-gradient(90deg, #10B981, #34D399);
}

.bar-fill-error {
  background: linear-gradient(90deg, #EF4444, #F87171);
}

.bar-value {
  width: 60px;
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

/* 深色模式 */



/* 响应式 */
@media (max-width: 768px) {
  .two-col-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<style>
/* UsageStats 深色模式（非 scoped） */
html.dark .bar-label {
  color: #e2e8f0 !important;
}
html.dark .bar-track {
  background: #1e293b !important;
}
html.dark .bar-value {
  color: #e2e8f0 !important;
}
</style>
