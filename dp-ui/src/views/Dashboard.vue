<template>
  <div class="dashboard-container">
    <!-- 滚动公告栏 -->
    <div v-if="announcements.length > 0" class="announcement-bar">
      <div class="announcement-icon">
        <n-icon size="18"><MegaphoneOutline /></n-icon>
      </div>
      <div class="announcement-content">
        <div ref="scrollRef" class="announcement-scroll">
          <div class="announcement-list" :style="{ transform: `translateX(-${scrollOffset}px)` }">
            <div
              v-for="(item, index) in displayAnnouncements"
              :key="index"
              class="announcement-item"
              :class="`announcement-${item.type}`"
              @click="showAnnouncementDetail(item)"
            >
              <n-tag :type="item.type" size="small" style="margin-right: 8px;">
                {{ item.type === 'info' ? t('announcement.notice') : item.type === 'success' ? t('announcement.success') : item.type === 'warning' ? t('announcement.warning') : t('announcement.urgent') }}
              </n-tag>
              <span class="announcement-title">{{ item.title }}</span>
              <span v-if="item.isTop" class="top-badge">{{ t('announcement.pinned') }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="announcement-actions">
        <n-button quaternary size="small" @click="togglePause">
          <template #icon>
            <n-icon><component :is="isPaused ? PlayOutline : PauseOutline" /></n-icon>
          </template>
        </n-button>
        <n-button quaternary size="small" @click="showAllAnnouncements">
          {{ t('announcement.viewAll') }}
        </n-button>
      </div>
    </div>

    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-content">
        <div class="welcome-text">
          <h1>{{ userNickname }}，{{ t('dashboard.welcome') }} 👋</h1>
          <p>{{ t('dashboard.welcomeDesc') }}</p>
          <div class="welcome-meta">
            <span class="welcome-time">
              <n-icon :size="14"><TimeOutline /></n-icon>
              {{ currentDateTime }}
            </span>
          </div>
        </div>
        <div class="welcome-actions">
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button
                :type="autoRefreshEnabled ? 'primary' : 'default'"
                size="small"
                ghost
                style="color: white; border-color: rgba(255,255,255,0.4);"
                @click="toggleAutoRefresh"
              >
                <template #icon>
                  <n-icon><SyncOutline /></n-icon>
                </template>
              </n-button>
            </template>
            {{ autoRefreshEnabled ? t('dashboard.stopAutoRefresh') : t('dashboard.startAutoRefresh') }}
          </n-tooltip>
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button
                size="small"
                ghost
                style="color: white; border-color: rgba(255,255,255,0.4);"
                @click="refreshData"
              >
                <template #icon>
                  <n-icon><RefreshOutline /></n-icon>
                </template>
              </n-button>
            </template>
            {{ t('dashboard.refresh') }}
          </n-tooltip>
          <n-button
            size="small"
            ghost
            style="color: white; border-color: rgba(255,255,255,0.4);"
            class="layout-btn"
            @click="layoutEditing = !layoutEditing"
          >
            <template #icon>
              <n-icon><SettingsOutline /></n-icon>
            </template>
            {{ layoutEditing ? t('dashboard.exitEdit') : t('dashboard.customLayout') }}
          </n-button>
        </div>
      </div>
    </div>

    <!-- 布局编辑面板 -->
    <div v-if="layoutEditing" class="layout-editor-panel">
      <n-space>
        <n-checkbox v-model:checked="cardVisible.stats">{{ t('dashboard.statCards') }}</n-checkbox>
        <n-checkbox v-model:checked="cardVisible.chart">{{ t('dashboard.collectTrend') }}</n-checkbox>
        <n-checkbox v-model:checked="cardVisible.pie">{{ t('dashboard.dataSourceDist') }}</n-checkbox>
        <n-checkbox v-model:checked="cardVisible.overview">{{ t('dashboard.dataOverview') }}</n-checkbox>
        <n-button type="primary" size="small" @click="saveLayout">{{ t('common.save') }}</n-button>
        <n-button size="small" @click="resetLayout">{{ t('common.reset') }}</n-button>
      </n-space>
    </div>

    <!-- 移动端快速入口 -->
    <div v-if="isMobile" class="mobile-quick-entries">
      <div class="quick-entry" @click="router.push('/m/charts')">
        <div class="quick-entry-icon" :style="{ background: `linear-gradient(135deg, ${themeStore.primaryColor}, var(--color-primary-hover))` }">
          <n-icon :size="22"><BarChartOutline /></n-icon>
        </div>
        <span>{{ t('mobile.chartCenter') }}</span>
      </div>
      <div class="quick-entry" @click="router.push('/m/reports')">
        <div class="quick-entry-icon" style="background: linear-gradient(135deg, #10b981, #059669)">
          <n-icon :size="22"><DocumentOutline /></n-icon>
        </div>
        <span>{{ t('mobile.reportCenter') }}</span>
      </div>
      <div class="quick-entry" @click="router.push('/m/pages')">
        <div class="quick-entry-icon" style="background: linear-gradient(135deg, #06b6d4, #0891b2)">
          <n-icon :size="22"><PieChartOutline /></n-icon>
        </div>
        <span>{{ t('mobile.pageCenter') }}</span>
      </div>
      <div class="quick-entry" @click="router.push('/export-center')">
        <div class="quick-entry-icon" style="background: linear-gradient(135deg, #f59e0b, #d97706)">
          <n-icon :size="22"><DownloadOutline /></n-icon>
        </div>
        <span>{{ t('mobile.exportCenter') }}</span>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="statsLoading" class="loading-container">
      <n-spin size="large" />
    </div>

    <!-- 错误状态 -->
    <div v-else-if="loadError" class="error-container">
      <n-empty :description="loadErrorMessage || t('dashboard.loadFailed')">
        <template #extra>
          <n-button type="primary" @click="refreshData">{{ t('common.retry') }}</n-button>
        </template>
      </n-empty>
    </div>

    <template v-else>
      <!-- 统计卡片 -->
      <div v-if="cardVisible.stats" class="stat-cards-grid">
        <div class="stat-card-enhanced stat-card-primary" @click="router.push('/data-source')">
          <div class="stat-card-icon">
            <n-icon :size="24"><ServerOutline /></n-icon>
          </div>
          <div class="stat-card-content">
            <div class="stat-card-value">{{ animatedDataSourceCount }}</div>
            <div class="stat-card-label">{{ t('dashboard.dataSources') }}</div>
          </div>
          <div class="stat-card-badge">
            <n-icon :size="14"><ArrowForwardOutline /></n-icon>
          </div>
        </div>
        <div class="stat-card-enhanced stat-card-info" @click="router.push('/data-collect')">
          <div class="stat-card-icon">
            <n-icon :size="24"><CloudDownloadOutline /></n-icon>
          </div>
          <div class="stat-card-content">
            <div class="stat-card-value">{{ animatedCollectTaskCount }}</div>
            <div class="stat-card-label">{{ t('dashboard.collectTasks') }}</div>
          </div>
          <div class="stat-card-badge">
            <n-icon :size="14"><ArrowForwardOutline /></n-icon>
          </div>
        </div>
        <div class="stat-card-enhanced stat-card-warning">
          <div class="stat-card-icon">
            <n-icon :size="24"><BarChartOutline /></n-icon>
          </div>
          <div class="stat-card-content">
            <div class="stat-card-value">{{ animatedTodayCollectCount }}</div>
            <div class="stat-card-label">{{ t('dashboard.todayCollected') }}</div>
          </div>
          <div class="stat-card-trend trend-up" v-if="stats.todayCollectCount > 0">
            <n-icon :size="12"><TrendingUpOutline /></n-icon>
            {{ t('dashboard.active') }}
          </div>
        </div>
        <div class="stat-card-enhanced stat-card-success">
          <div class="stat-card-icon">
            <n-icon :size="24"><CheckmarkCircleOutline /></n-icon>
          </div>
          <div class="stat-card-content">
            <div class="stat-card-value">{{ stats.successRate || '99.5%' }}</div>
            <div class="stat-card-label">{{ t('dashboard.successRate') }}</div>
          </div>
        </div>
      </div>

      <!-- 主内容区：图表 + 侧边栏 -->
      <div class="main-grid">
        <!-- 左侧：图表区域 -->
        <div class="main-left">
          <!-- 数据概览 + 数据源分布 并排 -->
          <div class="chart-row-inner">
            <!-- 数据源分布 -->
            <n-card v-if="cardVisible.pie" class="chart-card">
              <template #header>
                <div class="card-header-custom">
                  <div class="card-title">
                    <n-icon size="20" :color="themeStore.primaryColor"><PieChartOutline /></n-icon>
                    <span>{{ t('dashboard.dataSourceDist') }}</span>
                  </div>
                </div>
              </template>
              <div ref="pieChartRef" class="chart-container chart-container-sm"></div>
            </n-card>

            <!-- 数据概览 -->
            <n-card v-if="cardVisible.overview" class="chart-card">
              <template #header>
                <div class="card-header-custom">
                  <div class="card-title">
                    <n-icon size="20" :color="themeStore.primaryColor"><StatsChartOutline /></n-icon>
                    <span>{{ t('dashboard.dataOverview') }}</span>
                  </div>
                </div>
              </template>
              <div class="overview-grid-compact">
                <div class="overview-item-compact">
                  <div class="overview-icon-sm overview-icon-primary">
                    <n-icon :size="18"><DocumentOutline /></n-icon>
                  </div>
                  <div class="overview-info-compact">
                    <div class="overview-value-compact">{{ animatedReportCount }}</div>
                    <div class="overview-label-compact">{{ t('dashboard.reports') }}</div>
                  </div>
                </div>
                <div class="overview-item-compact">
                  <div class="overview-icon-sm overview-icon-success">
                    <n-icon :size="18"><BarChartOutline /></n-icon>
                  </div>
                  <div class="overview-info-compact">
                    <div class="overview-value-compact">{{ animatedChartCount }}</div>
                    <div class="overview-label-compact">{{ t('dashboard.charts') }}</div>
                  </div>
                </div>
                <div class="overview-item-compact">
                  <div class="overview-icon-sm overview-icon-warning">
                    <n-icon :size="18"><CloudDownloadOutline /></n-icon>
                  </div>
                  <div class="overview-info-compact">
                    <div class="overview-value-compact">{{ animatedWeekCollectCount }}</div>
                    <div class="overview-label-compact">{{ t('dashboard.weekCollected') }}</div>
                  </div>
                </div>
                <div class="overview-item-compact">
                  <div class="overview-icon-sm overview-icon-info">
                    <n-icon :size="18"><PeopleOutline /></n-icon>
                  </div>
                  <div class="overview-info-compact">
                    <div class="overview-value-compact">{{ animatedUserCount }}</div>
                    <div class="overview-label-compact">{{ t('dashboard.users') }}</div>
                  </div>
                </div>
              </div>
            </n-card>
          </div>

          <!-- 采集趋势 -->
          <n-card v-if="cardVisible.chart" class="chart-card">
            <template #header>
              <div class="card-header-custom">
                <div class="card-title">
                  <n-icon size="20" :color="themeStore.primaryColor"><AnalyticsOutline /></n-icon>
                  <span>{{ t('dashboard.collectTrend') }}</span>
                </div>
                <n-tag type="info" size="small" round>{{ t('dashboard.sevenDays') }}</n-tag>
              </div>
            </template>
            <div ref="collectChartRef" class="chart-container"></div>
          </n-card>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, onMounted, onUnmounted, nextTick, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useDialog, useMessage } from 'naive-ui'
import * as echarts from 'echarts'
import {
  ServerOutline,
  CloudDownloadOutline,
  BarChartOutline,
  DocumentOutline,
  PieChartOutline,
  AnalyticsOutline,
  DownloadOutline,
  SettingsOutline,
  CheckmarkCircleOutline,
  PeopleOutline,
  RefreshOutline,
  MegaphoneOutline,
  PlayOutline,
  PauseOutline,
  SyncOutline,
  ArrowForwardOutline,
  TrendingUpOutline,
  TimeOutline,
  StatsChartOutline,
} from '@vicons/ionicons5'
import { getDashboardStats, getDataSourceDistribution, getCollectTrend } from '@/api/dashboard'
import { useCountAnimation } from '@/utils/countAnimation'
import { getActiveAnnouncements } from '@/api/announcement'
import type { Announcement } from '@/api/announcement'
import type { DashboardStats } from '@/types/dashboard'
import { useI18n } from '@/i18n'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { useThemeStore } from '@/stores/theme'

const { t } = useI18n()
const userStore = useUserStore()
const themeStore = useThemeStore()

const appStore = useAppStore()
const isMobile = computed(() => appStore.isMobileView)

const userNickname = computed(() => {
  return userStore.userInfo?.nickname || userStore.userInfo?.username || t('dashboard.welcome')
})

const router = useRouter()
const dialog = useDialog()
const message = useMessage()

// 当前时间
const currentDateTime = ref('')
let clockTimer: ReturnType<typeof setInterval> | null = null
const updateClock = () => {
  const now = new Date()
  currentDateTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', weekday: 'short'
  })
}


// 公告相关
const announcements = ref<Announcement[]>([])
const displayAnnouncements = computed(() => {
  return [...announcements.value, ...announcements.value]
})
const scrollRef = ref<HTMLElement>()
const scrollOffset = ref(0)
const isPaused = ref(false)
let scrollAnimationId: number | null = null

const togglePause = () => {
  isPaused.value = !isPaused.value
  if (!isPaused.value) startScroll()
}

const showAnnouncementDetail = (item: Announcement) => {
  dialog.info({
    title: item.title,
    content: item.content,
    positiveText: '知道了'
  })
}

const showAllAnnouncements = () => {
  dialog.info({
    title: '系统公告',
    content: () => {
      return announcements.value.map(a => `【${a.type === 'info' ? '通知' : a.type === 'success' ? '成功' : a.type === 'warning' ? '警告' : '紧急'}】${a.title}`).join('\n\n')
    },
    positiveText: '关闭'
  })
}

const statsLoading = ref(true)
const loadError = ref(false)
const loadErrorMessage = ref('')

const stats = ref<DashboardStats>({
  dataSourceCount: 0,
  collectTaskCount: 0,
  todayCollectCount: 0,
  userCount: 0
})

const animatedDataSourceCount = useCountAnimation(computed(() => stats.value.dataSourceCount))
const animatedCollectTaskCount = useCountAnimation(computed(() => stats.value.collectTaskCount))
const animatedTodayCollectCount = useCountAnimation(computed(() => stats.value.todayCollectCount))
const animatedUserCount = useCountAnimation(computed(() => stats.value.userCount))
const animatedReportCount = useCountAnimation(computed(() => stats.value.reportCount || 0))
const animatedChartCount = useCountAnimation(computed(() => stats.value.chartCount || 0))
const animatedWeekCollectCount = useCountAnimation(computed(() => stats.value.weekCollectCount || 0))

// 自动刷新
const autoRefreshEnabled = ref(false)
let autoRefreshTimer: ReturnType<typeof setInterval> | null = null

const stopAutoRefresh = () => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = null
  }
}

const toggleAutoRefresh = () => {
  autoRefreshEnabled.value = !autoRefreshEnabled.value
  if (autoRefreshEnabled.value) {
    autoRefreshTimer = setInterval(refreshData, 30000)
  } else {
    stopAutoRefresh()
  }
}

// 布局编辑
const layoutEditing = ref(false)
const LAYOUT_STORAGE_KEY = 'dp-dashboard-layout'
const defaultCardVisible = { stats: true, chart: true, pie: true, overview: true }

function loadLayout() {
  try {
    const saved = localStorage.getItem(LAYOUT_STORAGE_KEY)
    return saved ? { ...defaultCardVisible, ...JSON.parse(saved) } : { ...defaultCardVisible }
  } catch { return { ...defaultCardVisible } }
}

const cardVisible = reactive(loadLayout())

const saveLayout = () => {
  localStorage.setItem(LAYOUT_STORAGE_KEY, JSON.stringify({ ...cardVisible }))
  layoutEditing.value = false
  message.success('布局已保存')
}

const resetLayout = () => {
  Object.assign(cardVisible, defaultCardVisible)
  localStorage.removeItem(LAYOUT_STORAGE_KEY)
  message.success('已重置为默认布局')
}

// 加载公告
const loadAnnouncements = async () => {
  try {
    const res: any = await getActiveAnnouncements()
    announcements.value = res.data || []
    if (announcements.value.length > 0) {
      await nextTick()
      startScroll()
    }
  } catch (error) {
    console.warn('加载公告失败', error)
  }
}

const startScroll = async () => {
  if (scrollAnimationId) cancelAnimationFrame(scrollAnimationId)
  if (announcements.value.length === 0) return

  const scrollEl = scrollRef.value
  if (!scrollEl) return

  const totalWidth = scrollEl.scrollWidth / 2
  const speed = 0.5

  const step = () => {
    if (isPaused.value) {
      scrollAnimationId = requestAnimationFrame(step)
      return
    }
    scrollOffset.value += speed
    if (scrollOffset.value >= totalWidth) {
      scrollOffset.value = 0
    }
    scrollAnimationId = requestAnimationFrame(step)
  }
  scrollAnimationId = requestAnimationFrame(step)
}

// ECharts
const collectChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
let collectTaskChart: echarts.ECharts | null = null
let dataSourceChart: echarts.ECharts | null = null

const refreshData = async () => {
  statsLoading.value = true
  loadError.value = false
  try {
    const res: any = await getDashboardStats()
    if (res.code === 200 && res.data) {
      stats.value = res.data
    }
    statsLoading.value = false
    await nextTick()
    await Promise.allSettled([loadCharts(), loadAnnouncements()])
  } catch (error: any) {
    statsLoading.value = false
    loadError.value = true
    loadErrorMessage.value = error?.message || ''
  }
}

const loadCharts = async () => {
  try {
    const [distRes, trendRes]: any[] = await Promise.allSettled([
      getDataSourceDistribution(),
      getCollectTrend()
    ])

    // 数据源分布饼图
    if (distRes.status === 'fulfilled' && pieChartRef.value) {
      const distData = distRes.value?.data || []
      if (!dataSourceChart) {
        dataSourceChart = echarts.init(pieChartRef.value)
      }
      const dataSourceOption = {
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)',
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: '#e2e8f0',
          borderWidth: 1,
          textStyle: { color: '#1e293b' },
          extraCssText: 'box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); border-radius: 8px;'
        },
        legend: {
          orient: 'vertical',
          left: 'left',
          top: 'center',
          textStyle: { color: '#64748b' }
        },
        color: [themeStore.primaryColor, '#00D4FF', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6'],
        series: [
          {
            name: '数据源',
            type: 'pie',
            radius: ['45%', '75%'],
            center: ['60%', '50%'],
            avoidLabelOverlap: false,
            itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
            label: { show: false },
            emphasis: {
              label: { show: true, fontSize: '14', fontWeight: 'bold' }
            },
            data: distData.map((item: any) => ({
              name: item.type || item.name,
              value: item.count || item.value
            }))
          }
        ]
      }
      dataSourceChart.setOption(dataSourceOption)
    }

    // 采集趋势折线图
    if (trendRes.status === 'fulfilled' && collectChartRef.value) {
      const rawTrend = trendRes.value?.data
      const trendData = Array.isArray(rawTrend) ? rawTrend : (Array.isArray(rawTrend?.data) ? rawTrend.data : [])
      if (!collectTaskChart) {
        collectTaskChart = echarts.init(collectChartRef.value)
      }
      const xData = trendData.map((item: any) => item.date || item.day)
      const seriesData = trendData.map((item: any) => item.count || item.value || 0)

      const collectTaskOption = {
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: '#e2e8f0',
          borderWidth: 1,
          textStyle: { color: '#1e293b' },
          extraCssText: 'box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); border-radius: 8px;'
        },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: xData,
          axisLine: { lineStyle: { color: '#e2e8f0' } },
          axisLabel: { color: '#64748b' }
        },
        yAxis: {
          type: 'value',
          axisLine: { show: false },
          splitLine: { lineStyle: { color: '#f1f5f9' } },
          axisLabel: { color: '#64748b' }
        },
        series: [
          {
            name: '采集量',
            type: 'line',
            smooth: true,
            symbolSize: 8,
            data: seriesData,
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: themeStore.primaryColor + '40' },
                { offset: 1, color: themeStore.primaryColor + '05' }
              ])
            },
            lineStyle: {
              width: 3,
              color: themeStore.primaryColor
            },
            itemStyle: {
              color: themeStore.primaryColor,
              borderWidth: 2,
              borderColor: '#fff'
            }
          }
        ]
      }
      collectTaskChart.setOption(collectTaskOption)
    }
  } catch (error) {
    console.warn('加载图表失败', error)
  }
}

const handleResize = () => {
  collectTaskChart?.resize()
  dataSourceChart?.resize()
}

onMounted(async () => {
  updateClock()
  clockTimer = setInterval(updateClock, 30000)
  await refreshData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  stopAutoRefresh()
  if (clockTimer) clearInterval(clockTimer)
  if (scrollAnimationId) cancelAnimationFrame(scrollAnimationId)
  collectTaskChart?.dispose()
  dataSourceChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
  max-width: 1440px;
  margin: 0 auto;
}

/* ========== 公告栏 ========== */
.announcement-bar {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #FEF3C7 0%, #FDE68A 100%);
  border: 1px solid #F59E0B;
  border-radius: var(--radius-lg, 12px);
  padding: 10px 16px;
  margin-bottom: 16px;
  gap: 12px;
}

.announcement-icon { color: #D97706; flex-shrink: 0; }
.announcement-content { flex: 1; overflow: hidden; }
.announcement-scroll { overflow: hidden; width: 100%; }
.announcement-list { display: flex; gap: 40px; white-space: nowrap; will-change: transform; }
.announcement-item { display: inline-flex; align-items: center; cursor: pointer; padding: 2px 0; flex-shrink: 0; }
.announcement-title { font-size: 13px; color: #92400E; }
.top-badge { background: #EF4444; color: white; font-size: 10px; padding: 1px 6px; border-radius: 4px; margin-left: 6px; }
.announcement-actions { display: flex; gap: 4px; flex-shrink: 0; }

/* ========== 欢迎横幅 ========== */
.welcome-banner {
  background: linear-gradient(135deg, #050d1f 0%, #0a1628 30%, var(--dp-color-primary, #2563eb) 65%, var(--dp-color-primary-hover, #3b82f6) 100%);
  border-radius: 20px;
  padding: 28px 36px;
  margin-bottom: 20px;
  color: white;
  position: relative;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(13, 71, 161, 0.3), 0 2px 8px rgba(0, 0, 0, 0.08), inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

.welcome-banner::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(var(--dp-color-primary-light, rgba(37, 99, 235, 0.08)) 1px, transparent 1px),
    linear-gradient(90deg, var(--dp-color-primary-light, rgba(37, 99, 235, 0.08)) 1px, transparent 1px);
  background-size: 40px 40px;
  animation: bannerGridScroll 20s linear infinite;
  pointer-events: none;
  border-radius: 20px;
}

.welcome-banner::after {
  content: '';
  position: absolute;
  top: -40%;
  right: -8%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(56, 189, 248, 0.12) 0%, transparent 65%);
  border-radius: 50%;
  animation: bannerFloat 10s ease-in-out infinite alternate;
  pointer-events: none;
}

@keyframes bannerGridScroll { 0% { background-position: 0 0; } 100% { background-position: 40px 40px; } }
@keyframes bannerFloat { 0% { transform: translate(0, 0) scale(1); } 100% { transform: translate(-20px, 15px) scale(1.1); } }

.welcome-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 1;
}

.welcome-text h1 { font-size: 22px; font-weight: 700; margin: 0 0 6px 0; letter-spacing: 0.3px; }
.welcome-text p { font-size: 14px; opacity: 0.8; margin: 0 0 8px 0; }

.welcome-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  opacity: 0.7;
}

.welcome-time {
  display: flex;
  align-items: center;
  gap: 4px;
}

.welcome-status {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  display: inline-block;
}

.status-healthy { background: #10b981; box-shadow: 0 0 6px rgba(16, 185, 129, 0.6); }
.status-warning { background: #f59e0b; box-shadow: 0 0 6px rgba(245, 158, 11, 0.6); }
.status-error { background: #ef4444; box-shadow: 0 0 6px rgba(239, 68, 68, 0.6); animation: pulse-dot 1.5s infinite; }
.status-unknown { background: #94a3b8; }

@keyframes pulse-dot { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }

.welcome-actions { display: flex; gap: 8px; flex-shrink: 0; }

/* ========== 布局编辑面板 ========== */
.layout-editor-panel {
  background: var(--dp-bg-primary, #fff);
  border: 1px solid var(--dp-border-default, #e2e8f0);
  border-radius: 12px;
  padding: 12px 16px;
  margin-bottom: 16px;
}

/* ========== 加载/错误状态 ========== */
.loading-container,
.error-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

/* ========== 移动端快速入口 ========== */
.mobile-quick-entries {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.quick-entry {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  background: var(--dp-bg-primary, #fff);
  border-radius: 12px;
  border: 1px solid var(--dp-border-light, #f1f5f9);
  cursor: pointer;
  transition: all 0.2s;
}

.quick-entry:active { transform: scale(0.95); }
.quick-entry-icon { width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: white; }
.quick-entry span { font-size: 12px; color: #64748b; }

/* ========== 统计卡片 ========== */
.stat-cards-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card-enhanced {
  background: var(--dp-bg-primary, #fff);
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid var(--dp-border-light, #f1f5f9);
  position: relative;
  overflow: hidden;
  transition: all 0.3s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  cursor: pointer;
}

.stat-card-enhanced:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 32px -8px rgba(0, 0, 0, 0.15);
}

.stat-card-enhanced::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 3px;
}

.stat-card-primary::before { background: var(--dp-gradient-primary, linear-gradient(90deg, #2563eb, #1d4ed8)); }
.stat-card-info::before { background: var(--dp-gradient-secondary, linear-gradient(90deg, #06b6d4, #0891b2)); }
.stat-card-warning::before { background: var(--dp-gradient-warning, linear-gradient(90deg, #f59e0b, #d97706)); }
.stat-card-success::before { background: linear-gradient(90deg, #10b981, #059669); }

.stat-card-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card-primary .stat-card-icon { background: var(--dp-gradient-primary, linear-gradient(135deg, #2563eb, #1d4ed8)); color: #fff; box-shadow: 0 4px 12px rgba(37, 99, 235, 0.35); }
.stat-card-info .stat-card-icon { background: var(--dp-gradient-secondary, linear-gradient(135deg, #06b6d4, #0891b2)); color: #fff; box-shadow: 0 4px 12px rgba(6, 182, 212, 0.35); }
.stat-card-warning .stat-card-icon { background: var(--dp-gradient-warning, linear-gradient(135deg, #f59e0b, #d97706)); color: #fff; box-shadow: 0 4px 12px rgba(245, 158, 11, 0.35); }
.stat-card-success .stat-card-icon { background: linear-gradient(135deg, #10b981, #059669); color: #fff; box-shadow: 0 4px 12px rgba(16, 185, 129, 0.35); }

.stat-card-content { flex: 1; }
.stat-card-value { font-size: 28px; font-weight: 700; color: var(--dp-text-primary, #1e293b); line-height: 1.2; }
.stat-card-label { font-size: 13px; color: var(--dp-text-secondary, #64748b); margin-top: 4px; }

.stat-card-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  opacity: 0;
  color: var(--dp-text-tertiary, #94a3b8);
  transition: opacity 0.2s;
}

.stat-card-enhanced:hover .stat-card-badge { opacity: 1; }

.stat-card-trend {
  position: absolute;
  bottom: 10px;
  right: 12px;
  font-size: 11px;
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 2px 8px;
  border-radius: 20px;
}

.trend-up { background: rgba(16, 185, 129, 0.12); color: #059669; }

/* ========== 快捷操作 ========== */
.quick-actions-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.quick-action-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: var(--dp-bg-primary, #fff);
  border: 1px solid var(--dp-border-light, #f1f5f9);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.25s;
  position: relative;
  overflow: hidden;
}

.quick-action-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px -4px rgba(0, 0, 0, 0.12);
  border-color: var(--dp-color-primary, #2563eb);
}

.quick-action-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.quick-action-info { flex: 1; min-width: 0; }
.quick-action-title { font-size: 13px; font-weight: 600; color: var(--dp-text-primary, #1e293b); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.quick-action-desc { font-size: 11px; color: var(--dp-text-tertiary, #94a3b8); margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.quick-action-arrow { color: var(--dp-text-tertiary, #94a3b8); flex-shrink: 0; opacity: 0; transition: opacity 0.2s, transform 0.2s; }
.quick-action-card:hover .quick-action-arrow { opacity: 1; transform: translateX(2px); }

/* ========== 主内容网格 ========== */
.main-grid {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.main-left { display: flex; flex-direction: column; gap: 20px; }

/* ========== 图表 ========== */
.chart-row-inner {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.chart-card { border-radius: 16px; overflow: hidden; }
.chart-container { width: 100%; height: 300px; }
.chart-container-sm { height: 260px; }

.card-header-custom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
}

/* ========== 数据概览（紧凑） ========== */
.overview-grid-compact {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.overview-item-compact {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px;
  border-radius: 10px;
  background: var(--dp-bg-secondary, #f8fafc);
  transition: all 0.2s;
}

.overview-item-compact:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.overview-icon-sm {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.overview-icon-primary { background: var(--dp-gradient-primary, linear-gradient(135deg, #2563eb, #1e40af)); color: white; box-shadow: 0 3px 8px rgba(37, 99, 235, 0.25); }
.overview-icon-success { background: var(--dp-gradient-success, linear-gradient(135deg, #10b981, #059669)); color: white; box-shadow: 0 3px 8px rgba(16, 185, 129, 0.25); }
.overview-icon-warning { background: var(--dp-gradient-warning, linear-gradient(135deg, #f59e0b, #d97706)); color: white; box-shadow: 0 3px 8px rgba(245, 158, 11, 0.25); }
.overview-icon-info { background: var(--dp-gradient-secondary, linear-gradient(135deg, #06b6d4, #0891b2)); color: white; box-shadow: 0 3px 8px rgba(6, 182, 212, 0.25); }

.overview-info-compact { flex: 1; }
.overview-value-compact { font-size: 20px; font-weight: 700; color: var(--dp-text-primary, #1e293b); line-height: 1.2; }
.overview-label-compact { font-size: 12px; color: var(--dp-text-secondary, #64748b); margin-top: 2px; }

/* ========== 响应式 ========== */

@media (max-width: 768px) {
  .dashboard-container { padding: 12px; }
  .stat-cards-grid { grid-template-columns: repeat(2, 1fr); gap: 10px; }
  .chart-row-inner { grid-template-columns: 1fr; }
  .overview-grid-compact { grid-template-columns: 1fr; }
  .welcome-banner { border-radius: 14px !important; padding: 16px !important; margin-bottom: 12px; }
  .welcome-text h1 { font-size: 16px; }
  .welcome-text p { font-size: 12px; }
  .welcome-meta { flex-direction: column; align-items: flex-start; gap: 4px; }
  .welcome-actions { flex-wrap: wrap; }
  .layout-btn span { display: none; }
  .stat-card-enhanced { padding: 14px; }
  .stat-card-value { font-size: 22px; }
}
</style>

<style>
/* Dashboard 深色模式（非 scoped） */
html.dark .quick-entry { background: var(--dp-bg-elevated, #1e293b) !important; border-color: var(--dp-border-light, rgba(255,255,255,0.06)) !important; }
html.dark .quick-entry:active { background: var(--dp-bg-tertiary, #1e2d45) !important; }
html.dark .quick-entry span { color: var(--dp-text-secondary, #94a3b8) !important; }
html.dark .welcome-banner {
  background: linear-gradient(135deg, #020810 0%, #050d1f 30%, var(--dp-color-primary, #1e40af) 65%, var(--dp-color-primary-hover, #3b82f6) 100%) !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.6), inset 0 1px 0 rgba(255, 255, 255, 0.04) !important;
}
html.dark .stat-card-enhanced {
  background: var(--dp-bg-elevated, #1e293b) !important;
  border-color: var(--dp-border-light, rgba(255,255,255,0.06)) !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.4) !important;
}
html.dark .stat-card-enhanced:hover {
  box-shadow: 0 16px 32px -8px rgba(0, 0, 0, 0.6) !important;
  border-color: rgba(59, 130, 246, 0.25) !important;
}
html.dark .stat-card-value { color: #f1f5f9 !important; }
html.dark .stat-card-label { color: #94a3b8 !important; }
html.dark .layout-editor-panel { background: var(--dp-bg-elevated, #1e293b) !important; border-color: var(--dp-border-light, rgba(255,255,255,0.06)) !important; }
html.dark .announcement-bar {
  background: linear-gradient(135deg, #422006 0%, #451a03 100%) !important;
  border-color: #92400E !important;
}
html.dark .announcement-title { color: #fbbf24 !important; }
html.dark .overview-item-compact { background: var(--dp-bg-tertiary, #162032) !important; }
html.dark .overview-value-compact { color: #f1f5f9 !important; }
html.dark .overview-label-compact { color: #94a3b8 !important; }
</style>
