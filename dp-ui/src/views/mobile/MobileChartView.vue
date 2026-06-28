<template>
  <div class="mobile-chart-view">
    <!-- 顶部导航（复用 MobileHeader） -->
    <MobileHeader
      :title="chartDef?.chartName || '图表查看'"
      show-back
      :action-icon="EllipsisVerticalOutline"
      @action="showActions = true"
    />

    <MobilePageShell no-tab-bar>
    <!-- 骨架屏 -->
    <template v-if="loading && !chartDef">
      <div class="chart-info-bar">
        <n-skeleton text style="width: 60px" :sharp="false" />
        <n-skeleton text style="width: 100px" :sharp="false" />
        <n-skeleton text style="width: 80px; margin-left: auto" :sharp="false" />
      </div>
      <div class="chart-render-area" style="padding: 20px;">
        <n-skeleton text :repeat="2" :sharp="false" />
        <n-skeleton text style="width: 60%; margin-top: 8px" :sharp="false" />
        <div style="margin-top: 20px;">
          <n-skeleton style="width: 100%; height: 240px" :sharp="false" />
        </div>
      </div>
    </template>

    <!-- 图表信息 -->
    <div v-else-if="chartDef" class="chart-info-bar">
      <n-tag :type="chartDef.status === 1 ? 'success' : 'default'" size="small" round>
        {{ chartDef.chartType }}
      </n-tag>
      <span class="chart-info-code">{{ chartDef.chartCode }}</span>
      <span v-if="chartDef.updateTime" class="chart-info-time">{{ chartDef.updateTime }}</span>
    </div>

    <!-- 参数面板 -->
    <div v-if="chartDef?.parameters && chartDef.parameters.length > 0" class="param-section">
      <div class="param-toggle" @click="paramExpanded = !paramExpanded">
        <n-icon size="16"><FunnelOutline /></n-icon>
        <span>查询参数 ({{ chartDef.parameters.length }})</span>
        <n-icon size="14" :style="{ transform: paramExpanded ? 'rotate(180deg)' : '' }"><ChevronDownOutline /></n-icon>
      </div>
      <Transition name="param-expand">
      <div v-if="paramExpanded" class="param-body">
        <div v-for="param in chartDef.parameters" :key="param.name" class="param-item">
          <label>{{ param.label }}<span v-if="(param as any).required" class="required">*</span></label>
          <n-select
            v-if="(param as any).type === 'select' && (param as any).options"
            v-model:value="paramValues[param.name]"
            :options="(param as any).options"
            :placeholder="param.placeholder || '请选择'"
            size="small"
            clearable
          />
          <n-date-picker
            v-else-if="(param as any).type === 'date'"
            v-model:formatted-value="paramValues[param.name]"
            type="date"
            size="small"
            clearable
            style="width: 100%"
          />
          <n-date-picker
            v-else-if="(param as any).type === 'dateRange'"
            v-model:formatted-value="paramValues[param.name]"
            type="daterange"
            size="small"
            clearable
            style="width: 100%"
          />
          <n-input-number
            v-else-if="(param as any).type === 'number'"
            v-model:value="paramValues[param.name]"
            :placeholder="param.placeholder || '请输入'"
            size="small"
            clearable
            style="width: 100%"
          />
          <n-input
            v-else
            v-model:value="paramValues[param.name]"
            :placeholder="param.placeholder || '请输入'"
            size="small"
            clearable
          />
        </div>
        <n-button type="primary" size="small" block @click="refreshData">查询</n-button>
      </div>
      </Transition>
    </div>

    <!-- 图表渲染区 -->
    <div class="chart-render-area">
      <n-spin :show="loading">
        <div v-if="error" class="chart-error">
          <n-result status="error" title="加载失败" :description="error" />
        </div>
        <div v-else ref="chartContainerRef" class="chart-container">
          <div v-if="chartDef?.chartType === 'table'" class="table-wrapper">
            <n-data-table
              :columns="tableColumns"
              :data="chartData"
              :bordered="false"
              size="small"
              :scroll-x="tableScrollX"
              max-height="60vh"
            />
          </div>
          <div v-else ref="echartRef" class="echart-box"></div>
        </div>
      </n-spin>
    </div>

    <!-- 数据概览 -->
    <div v-if="chartData.length > 0" class="data-summary">
      <div class="data-summary-info">
        <n-icon size="14"><BarChartOutline /></n-icon>
        <span>共 {{ chartData.length }} 条数据</span>
      </div>
      <span v-if="lastRefreshTime" class="data-summary-time">{{ lastRefreshTime }}</span>
    </div>

    </MobilePageShell>

    <!-- 操作菜单 -->
    <n-drawer v-model:show="showActions" placement="bottom" :height="200" :trap-focus="false">
      <n-drawer-content title="操作">
        <div class="action-list">
          <div class="action-item" @click="handleRefresh">
            <n-icon size="20"><RefreshOutline /></n-icon>
            <span>刷新数据</span>
          </div>
          <div class="action-item" @click="handleFullscreen">
            <n-icon size="20"><ExpandOutline /></n-icon>
            <span>全屏查看</span>
          </div>
          <div class="action-item" @click="handleShare">
            <n-icon size="20"><ShareSocialOutline /></n-icon>
            <span>分享图表</span>
          </div>
        </div>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import MobileHeader from '@/components/mobile/MobileHeader.vue'
import MobilePageShell from '@/components/mobile/MobilePageShell.vue'
import { NButton, NIcon, NTag, NInput, NInputNumber, NDatePicker, NSelect, NSpin, NResult, NDataTable, NDrawer, NDrawerContent, NSkeleton, useMessage } from 'naive-ui'
import {
  EllipsisVerticalOutline, FunnelOutline, BarChartOutline,
  ChevronDownOutline, RefreshOutline, ExpandOutline, ShareSocialOutline
} from '@vicons/ionicons5'
import { getChartDefinitionById, getChartData } from '@/api/chart'
import { buildChartOption } from '@/utils/chartRenderer'
import { saveRecentVisit } from '@/utils/recentVisits'
import echarts from '@/utils/echarts'
import type { ChartDefinition } from '@/types/chart'

const route = useRoute()
const message = useMessage()

const chartDef = ref<ChartDefinition | null>(null)
const chartData = ref<any[]>([])
const loading = ref(false)
const error = ref('')
const paramExpanded = ref(false)
const paramValues = ref<Record<string, any>>({})
const showActions = ref(false)
const echartRef = ref<HTMLElement | null>(null)
const chartContainerRef = ref<HTMLElement | null>(null)

let echartInstance: any = null
let resizeTimer: ReturnType<typeof setTimeout> | null = null
let renderRetryCount = 0
const MAX_RENDER_RETRIES = 5
let themeObserver: MutationObserver | null = null
const lastRefreshTime = ref('')

const tableColumns = ref<any[]>([])
const tableScrollX = ref<number>(800)

async function loadChart() {
  const id = Number(route.params['id'])
  if (!id) return

  loading.value = true
  error.value = ''
  try {
    const res = await getChartDefinitionById(id) as any
    chartDef.value = res?.data?.data || res?.data || null
    if (chartDef.value) {
      saveRecentVisit(`/m/chart/${id}`, chartDef.value.chartName || '图表')
      await loadData()
    }
  } catch (e: any) {
    error.value = e?.message || '加载图表失败'
  } finally {
    loading.value = false
  }
}

async function loadData() {
  if (!chartDef.value) return
  renderRetryCount = 0
  try {
    const res = await getChartData(chartDef.value.id, { parameters: paramValues.value }) as any
    chartData.value = Array.isArray(res?.data) ? res.data : (Array.isArray(res?.data?.data) ? res.data.data : [])

    // 记录刷新时间
    const now = new Date()
    lastRefreshTime.value = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`

    if (chartDef.value.chartType === 'table' && chartData.value.length > 0) {
      const keys = Object.keys(chartData.value[0])
      tableColumns.value = keys.map(k => ({ title: k, key: k, ellipsis: { tooltip: true }, minWidth: 110, resizable: true }))
      tableScrollX.value = Math.max(keys.length * 130, 600)
    } else {
      await nextTick()
      renderEChart()
    }
  } catch (e: any) {
    console.error('加载数据失败', e)
  }
}

/** 移动端图表优化：缩小字号、紧凑图例、合理边距、触控tooltip */
function applyMobileChartOptions(config: any): any {
  const isDark = document.documentElement.classList.contains('dark')
  // 紧凑网格
  if (!config.grid) config.grid = {}
  config.grid = { left: 40, right: 16, top: 40, bottom: 36, containLabel: true, ...config.grid }
  // 图例优化
  if (config.legend) {
    config.legend.textStyle = { fontSize: 11, ...(config.legend.textStyle || {}) }
    config.legend.itemWidth = config.legend.itemWidth ?? 14
    config.legend.itemHeight = config.legend.itemHeight ?? 10
    config.legend.itemGap = config.legend.itemGap ?? 8
    // 图例过多时滚动
    if (!config.legend.type) config.legend.type = 'scroll'
    config.legend.pageIconSize = 10
  }
  // 坐标轴字号
  const axisFont = { fontSize: 11 }
  if (config.xAxis) {
    const axes = Array.isArray(config.xAxis) ? config.xAxis : [config.xAxis]
    axes.forEach((a: any) => {
      a.axisLabel = { ...axisFont, ...(a.axisLabel || {}) }
      if (a.axisLabel.rotate === undefined && a.type !== 'value') a.axisLabel.rotate = 30
    })
  }
  if (config.yAxis) {
    const axes = Array.isArray(config.yAxis) ? config.yAxis : [config.yAxis]
    axes.forEach((a: any) => { a.axisLabel = { ...axisFont, ...(a.axisLabel || {}) } })
  }
  // 移动端 tooltip 优化：限制在容器内、触摸友好
  if (!config.tooltip) config.tooltip = {}
  config.tooltip.confine = true
  config.tooltip.textStyle = { fontSize: 12, ...(config.tooltip.textStyle || {}) }
  config.tooltip.appendToBody = false
  // 饼图优化：缩小标签字号、调整radius
  if (config.series) {
    const series = Array.isArray(config.series) ? config.series : [config.series]
    series.forEach((s: any) => {
      if (s.type === 'pie') {
        if (!s.radius) s.radius = ['35%', '65%']
        if (!s.label) s.label = {}
        s.label.fontSize = s.label.fontSize ?? 11
        s.label.overflow = 'truncate'
        s.label.ellipsis = '...'
      }
    })
  }
  // 暗色背景透明
  if (isDark) {
    config.backgroundColor = 'transparent'
  }
  return config
}

async function renderEChart() {
  if (!echartRef.value || !chartDef.value) return
  
  // 检查容器尺寸
  const rect = echartRef.value.getBoundingClientRect()
  if (rect.width === 0 || rect.height === 0) {
    // 延迟重试，设上限防止无限循环
    if (renderRetryCount < MAX_RENDER_RETRIES) {
      renderRetryCount++
      setTimeout(() => renderEChart(), 150)
    } else {
      console.warn('图表容器尺寸为0，已达到最大重试次数')
    }
    return
  }
  renderRetryCount = 0
  
  try {
    if (echartInstance) {
      echartInstance.dispose()
    }
    const isDark = document.documentElement.classList.contains('dark')
    echartInstance = echarts.init(echartRef.value, isDark ? 'dark' : undefined)

    // 使用 buildChartOption 将图表配置和数据合并（与桌面端 DynamicChart 保持一致）
    let option = buildChartOption(chartDef.value, chartData.value)

    // 如果 buildChartOption 返回空或无效，使用 fallback
    if (!option || Object.keys(option).length === 0 || (option as any)._isTable) {
      const keys = Object.keys(chartData.value[0] || {})
      if (keys.length >= 2) {
        option = {
          tooltip: { trigger: 'axis' },
          xAxis: {
            type: 'category',
            data: chartData.value.map((row: any) => String(row[keys[0] as string] || ''))
          },
          yAxis: { type: 'value' },
          series: [{
            type: chartDef.value.chartType || 'bar',
            data: chartData.value.map((row: any) => Number(row[keys[1] as string]) || 0)
          }]
        }
      } else {
        // 数据不足，尝试使用原始配置
        let config: any = {}
        if (chartDef.value.chartConfig) {
          try {
            config = typeof chartDef.value.chartConfig === 'string'
              ? JSON.parse(chartDef.value.chartConfig)
              : chartDef.value.chartConfig
          } catch { /* ignore */ }
        }
        option = config
      }
    }

    option = applyMobileChartOptions(option)
    echartInstance.setOption(option, true)
  } catch (e) {
    console.error('渲染图表失败', e)
  }
}

function refreshData() {
  loadData()
}

function handleRefresh() {
  showActions.value = false
  loadChart()
}

function handleFullscreen() {
  showActions.value = false
  if (chartContainerRef.value?.requestFullscreen) {
    chartContainerRef.value.requestFullscreen()
  }
}

function handleShare() {
  showActions.value = false
  if (navigator.share) {
    navigator.share({
      title: chartDef.value?.chartName || '图表',
      url: window.location.href
    }).catch(() => { /* share cancelled */ })
  } else {
    navigator.clipboard?.writeText(window.location.href)
    message.success('链接已复制')
  }
}

function handleResize() {
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => {
    echartInstance?.resize()
  }, 150)
}

function handleOrientationChange() {
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => {
    echartInstance?.resize()
  }, 350)
}

onMounted(() => {
  loadChart()
  window.addEventListener('resize', handleResize)
  window.addEventListener('orientationchange', handleOrientationChange)
  // 监听暗色模式切换，实时重新渲染图表
  themeObserver = new MutationObserver(() => {
    if (echartInstance && chartDef.value && chartData.value.length > 0) {
      renderRetryCount = 0
      renderEChart()
    }
  })
  themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('orientationchange', handleOrientationChange)
  if (resizeTimer) clearTimeout(resizeTimer)
  themeObserver?.disconnect()
  echartInstance?.dispose()
})

watch(() => route.params['id'], () => {
  loadChart()
})
</script>

<style scoped>
.mobile-chart-view {
  display: flex;
  flex-direction: column;
}

.chart-info-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  margin: 0 12px 8px;
  background: #fff;
  border-radius: 14px;
  font-size: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.chart-info-code { color: #64748b; font-weight: 500; }
.chart-info-time { color: #94a3b8; margin-left: auto; font-size: 11px; }

.param-section {
  background: #fff;
  margin: 0 12px 8px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.param-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.param-toggle .n-icon:last-child {
  margin-left: auto;
  transition: transform 0.2s;
}

.param-body {
  padding: 0 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.param-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.param-item label {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.param-item .required {
  color: #ef4444;
  margin-left: 2px;
}

.chart-render-area {
  flex: 1;
  margin: 0 12px;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  min-height: 300px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.chart-container {
  width: 100%;
  min-height: 300px;
}

.echart-box {
  width: 100%;
  height: clamp(280px, 65vw, 450px);
  min-height: 260px;
  touch-action: pan-y;
}

@media (orientation: landscape) {
  .echart-box {
    height: 55vh;
    min-height: 220px;
    max-height: 500px;
  }
  .chart-render-area {
    margin: 0 8px;
  }
  .chart-info-bar {
    margin: 0 8px 6px;
  }
  .param-section {
    margin: 0 8px 6px;
  }
}

.table-wrapper {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  padding: 10px;
  position: relative;
}

/* 表格横滑提示渐变 */
.table-wrapper::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 24px;
  height: 100%;
  background: linear-gradient(to right, transparent, rgba(255,255,255,0.8));
  pointer-events: none;
  opacity: 1;
  transition: opacity 0.3s;
}

.chart-error {
  padding: 40px 20px;
}

.data-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  margin: 8px 12px 0;
  font-size: 12px;
  color: #94a3b8;
  background: #f8fafc;
  border-radius: 12px;
}

.data-summary-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  color: #64748b;
}

.data-summary-time {
  font-size: 11px;
  color: #94a3b8;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 14px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 500;
  color: #1e293b;
  transition: transform 0.15s ease, background 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.action-item:active {
  background: #f1f5f9;
  transform: scale(0.97);
}

/* 参数面杼展开/收起动画 */
.param-expand-enter-active,
.param-expand-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
  max-height: 400px;
}
.param-expand-enter-from,
.param-expand-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
}

/* 深色模式 - 表格横滑提示 */


/* 深色模式 - 表格 */

/* 深色模式 - 表单控件 */

/* 深色模式 - 骨架屏 */

/* 深色模式 - 操作抽屉 */
</style>

<style>
/* MobileChartView 深色模式（非 scoped） */
html.dark .chart-info-bar { background: #1e293b !important; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important; }
html.dark .chart-info-code { color: #94a3b8 !important; }
html.dark .chart-info-time { color: #64748b !important; }
html.dark .param-section { background: #1e293b !important; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important; }
html.dark .param-toggle { color: #e2e8f0 !important; }
html.dark .param-toggle:active { background: #334155 !important; }
html.dark .param-item label { color: #94a3b8 !important; }
html.dark .chart-render-area { background: #1e293b !important; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important; }
html.dark .echart-box { background: transparent !important; }
html.dark .data-summary { background: #1a2536 !important; color: #64748b !important; }
html.dark .data-summary-info { color: #94a3b8 !important; }
html.dark .data-summary-time { color: #475569 !important; }
html.dark .table-wrapper::after {
  background: linear-gradient(to right, transparent, rgba(30,41,59,0.8)) !important;
}
html.dark .chart-error { color: #94a3b8 !important; }
html.dark .action-item { color: #e2e8f0 !important; }
html.dark .action-item:active { background: #334155 !important; }
html.dark .table-wrapper { background: #1e293b !important; }
html.dark .table-wrapper .n-data-table { background: #1e293b !important; }
html.dark .table-wrapper .n-data-table-wrapper { background: #1e293b !important; }
html.dark .table-wrapper .n-data-table-th {
  background: #334155 !important;
  color: #f1f5f9 !important;
  border-color: #475569 !important;
}
html.dark .table-wrapper .n-data-table-td {
  background: #1e293b !important;
  color: #e2e8f0 !important;
  border-color: #334155 !important;
}
html.dark .table-wrapper .n-data-table-tr:nth-child(even) .n-data-table-td {
  background: #263449 !important;
}
html.dark .param-item .n-input {
  background: #0f172a !important;
  border-color: #334155 !important;
}
html.dark .param-item .n-input .n-input__input-el {
  color: #e2e8f0 !important;
  caret-color: #60a5fa !important;
}
html.dark .param-item .n-input .n-input__placeholder {
  color: #475569 !important;
}
html.dark .param-item .n-base-selection {
  background: #0f172a !important;
  border-color: #334155 !important;
}
html.dark .param-item .n-base-selection .n-base-selection-label {
  color: #e2e8f0 !important;
}
html.dark .chart-info-bar .n-skeleton,
html.dark .chart-info-bar .n-skeleton,
html.dark .chart-render-area .n-skeleton {
  background: #334155 !important;
}
html.dark .action-list .action-item .n-icon {
  color: #94a3b8 !important;
}
</style>
