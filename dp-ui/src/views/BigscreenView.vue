<template>
  <div class="bigscreen-view-root">
    <!-- 预览模式工具栏（仅从设计器预览时显示） -->
    <div v-if="isPreviewMode" class="preview-mode-bar">
      <div class="preview-bar-content">
        <div class="preview-bar-left">
          <n-icon size="16" color="#f59e0b"><CreateOutline /></n-icon>
          <span class="preview-label">预览模式</span>
        </div>
        <div class="preview-bar-right">
          <n-button size="small" type="primary" @click="returnToDesigner">
            <template #icon><n-icon><ArrowBackOutline /></n-icon></template>
            返回设计
          </n-button>
        </div>
      </div>
    </div>

    <!-- 大屏控制工具栏（右侧可隐藏） -->
    <div class="screen-toolbar" :class="{ 'is-visible': toolbarVisible }">
      <div class="toolbar-panel">
        <!-- 自动刷新 -->
        <n-tooltip trigger="hover" placement="left">
          <template #trigger>
            <button class="toolbar-btn" :class="{ active: autoRefreshEnabled }" @click="toggleAutoRefresh">
              <n-icon size="16"><TimeOutline /></n-icon>
              <span v-if="autoRefreshEnabled" class="countdown-text">{{ autoRefreshCountdown }}s</span>
            </button>
          </template>
          <span>{{ autoRefreshEnabled ? '关闭自动刷新' : '开启自动刷新' }}</span>
        </n-tooltip>

        <!-- 刷新间隔选择 -->
        <n-dropdown v-if="autoRefreshEnabled" :options="refreshIntervalOptions" placement="left" @select="setAutoRefreshInterval">
          <button class="toolbar-btn interval-btn">
            {{ autoRefreshInterval }}s
            <n-icon size="12"><ChevronDownOutline /></n-icon>
          </button>
        </n-dropdown>

        <!-- 手动刷新 -->
        <n-tooltip trigger="hover" placement="left">
          <template #trigger>
            <button class="toolbar-btn" :class="{ 'is-loading': isRefreshing }" @click="refreshAllCharts">
              <n-icon size="16" :class="{ 'spin-animation': isRefreshing }"><RefreshOutline /></n-icon>
            </button>
          </template>
          <span>刷新所有图表</span>
        </n-tooltip>

        <!-- 分隔线 -->
        <span class="toolbar-divider"></span>

        <!-- 全屏按钮 -->
        <n-tooltip trigger="hover" placement="left">
          <template #trigger>
            <button class="toolbar-btn fullscreen-btn" @click="toggleFullscreen">
              <n-icon size="18">
                <ContractOutline v-if="isFullscreen" />
                <ExpandOutline v-else />
              </n-icon>
              <span class="btn-text">{{ isFullscreen ? '退出' : '全屏' }}</span>
            </button>
          </template>
          <span>{{ isFullscreen ? '退出全屏' : '全屏显示' }}</span>
        </n-tooltip>

        <!-- 隐藏按钮 -->
        <button class="toolbar-btn hide-btn" @click="toolbarVisible = false">
          <n-icon size="14"><ChevronDownOutline /></n-icon>
        </button>
      </div>
    </div>

    <!-- 显示工具栏的触发器 -->
    <div v-if="!toolbarVisible" class="toolbar-trigger" @click="toolbarVisible = true">
      <n-icon size="14"><ChevronDownOutline /></n-icon>
    </div>

    <BigscreenPreview
      ref="previewRef"
      :config="bigscreenConfig"
      :page-name="pageName"
      :show-scale-info="showScaleInfo"
      @close="handleClose"
    >
      <template #header-center>
        <div v-if="hasParameterPanel" class="bigscreen-param-trigger">
          <n-button quaternary size="small" @click="showParamDrawer = true">
            <template #icon><n-icon><FilterOutline /></n-icon></template>
            参数设置
            <n-badge v-if="activeParamCount > 0" :value="activeParamCount" :max="9" dot />
          </n-button>
        </div>
      </template>

      <!-- 图表网格 -->
      <div class="bigscreen-charts" :style="chartsGridStyle">
        <template v-for="chart in sortedCharts" :key="chart.id || chart.chartId || ('s-' + chart.sortOrder)">
          <!-- 静态组件 -->
          <div v-if="chart.mode === 'static'" class="bigscreen-static-item" :style="getChartStyle(chart)">
            <template v-if="chart.staticType === 'headerBar'">
              <div class="bs-pv-header-bar" :style="{ background: chart.staticConfig?.bgColor || 'linear-gradient(90deg, rgba(6,30,60,0.9) 0%, rgba(12,21,39,0.95) 50%, rgba(6,30,60,0.9) 100%)' }">
                <div class="bs-pv-header-title" :style="{ color: chart.staticConfig?.titleColor || '#00d4ff', fontSize: (chart.staticConfig?.titleFontSize || 28) + 'px' }">{{ chart.staticConfig?.title || '' }}</div>
                <div v-if="chart.staticConfig?.showTime !== false" class="bs-pv-header-time" :style="{ color: chart.staticConfig?.timeColor || '#5b8fae' }">{{ clockTime }}</div>
              </div>
            </template>
            <template v-else-if="chart.staticType === 'kpiCard'">
              <div class="bs-pv-kpi-card" :style="{ background: chart.staticConfig?.bgColor || 'rgba(6,30,60,0.8)', borderColor: chart.staticConfig?.borderColor || 'rgba(64,158,255,0.4)' }">
                <div class="bs-pv-kpi-label" :style="{ color: chart.staticConfig?.labelColor || '#8ca8c8' }">{{ chart.staticConfig?.label || 'KPI' }}</div>
                <div class="bs-pv-kpi-value" :style="{ color: chart.staticConfig?.valueColor || '#00d4ff', fontSize: (chart.staticConfig?.valueFontSize || 32) + 'px' }">
                  {{ chart.staticConfig?.prefix || '' }}{{ chart.staticConfig?.value || '0' }}{{ chart.staticConfig?.suffix || '' }}
                </div>
              </div>
            </template>
            <template v-else-if="chart.staticType === 'numberFlipper'">
              <div class="bs-pv-number-flipper" :style="{ background: chart.staticConfig?.bgColor || 'rgba(6,30,60,0.8)' }">
                <div class="bs-pv-flipper-label" :style="{ color: chart.staticConfig?.labelColor || '#8ca8c8' }">{{ chart.staticConfig?.label || '' }}</div>
                <div class="bs-pv-flipper-digits">
                  <span v-for="(digit, di) in String(chart.staticConfig?.value || '00000').split('')" :key="di" class="bs-pv-digit" :style="{ color: chart.staticConfig?.digitColor || '#00ffcc', fontSize: (chart.staticConfig?.digitFontSize || 48) + 'px' }">{{ digit }}</span>
                </div>
                <div v-if="chart.staticConfig?.unit" class="bs-pv-flipper-unit" :style="{ color: chart.staticConfig?.unitColor || '#5b8fae' }">{{ chart.staticConfig.unit }}</div>
              </div>
            </template>
            <template v-else-if="chart.staticType === 'decorBorder'">
              <div class="bs-pv-decor-border" :style="{ borderColor: chart.staticConfig?.borderColor || 'rgba(64,158,255,0.6)', background: chart.staticConfig?.bgColor || 'rgba(6,30,60,0.5)' }">
                <div class="bs-pv-decor-corner tl"></div><div class="bs-pv-decor-corner tr"></div>
                <div class="bs-pv-decor-corner bl"></div><div class="bs-pv-decor-corner br"></div>
                <div v-if="chart.staticConfig?.title" class="bs-pv-decor-title" :style="{ color: chart.staticConfig?.titleColor || '#00d4ff' }">{{ chart.staticConfig.title }}</div>
              </div>
            </template>
            <template v-else-if="chart.staticType === 'progressBar'">
              <div class="bs-pv-progress-bar" :style="{ background: chart.staticConfig?.bgColor || 'rgba(6,30,60,0.8)' }">
                <div class="bs-pv-progress-label" :style="{ color: chart.staticConfig?.labelColor || '#8ca8c8' }">{{ chart.staticConfig?.label || '' }}</div>
                <div class="bs-pv-progress-track"><div class="bs-pv-progress-fill" :style="{ width: (chart.staticConfig?.percent || 60) + '%', background: chart.staticConfig?.barColor || 'linear-gradient(90deg, #00d4ff, #00ffcc)' }"></div></div>
                <div class="bs-pv-progress-value" :style="{ color: chart.staticConfig?.valueColor || '#00d4ff' }">{{ chart.staticConfig?.percent || 60 }}%</div>
              </div>
            </template>
            <template v-else-if="chart.staticType === 'title'">
              <div :style="{ fontSize: (chart.staticConfig?.fontSize || 24) + 'px', fontWeight: chart.staticConfig?.fontWeight || 'bold', color: chart.staticConfig?.color || '#00d4ff', textAlign: chart.staticConfig?.align || 'left', lineHeight: 1.3, height: '100%', display: 'flex', alignItems: 'center' }">{{ chart.staticConfig?.text || '' }}</div>
            </template>
            <template v-else-if="chart.staticType === 'text'">
              <div :style="{ fontSize: (chart.staticConfig?.fontSize || 14) + 'px', color: chart.staticConfig?.color || '#8ca8c8', textAlign: chart.staticConfig?.align || 'left', lineHeight: 1.6, whiteSpace: 'pre-wrap', height: '100%', display: 'flex', alignItems: 'center' }">{{ chart.staticConfig?.text || '' }}</div>
            </template>
            <template v-else-if="chart.staticType === 'divider'">
              <div style="display: flex; align-items: center; height: 100%;"><hr :style="{ width: '100%', border: 'none', borderTop: (chart.staticConfig?.thickness || 1) + 'px ' + (chart.staticConfig?.style || 'solid') + ' ' + (chart.staticConfig?.color || 'rgba(64,158,255,0.4)') }" /></div>
            </template>
            <template v-else-if="chart.staticType === 'image'">
              <div style="display: flex; align-items: center; justify-content: center; height: 100%;"><img v-if="chart.staticConfig?.src" :src="chart.staticConfig.src" :alt="chart.staticConfig?.alt || ''" :style="{ maxWidth: '100%', maxHeight: '100%', objectFit: chart.staticConfig?.objectFit || 'contain' }" /></div>
            </template>
          </div>
          <!-- 图表卡片 -->
          <div v-else class="bigscreen-chart-item" :style="getChartStyle(chart)">
            <div class="chart-wrapper" :class="{ 'table-wrapper': isTableChart(chart) }">
              <template v-if="isTableChart(chart)">
                <div class="bigscreen-table-view">
                  <div class="bigscreen-table-header">
                    <span>{{ getChartName(chart) }}</span>
                    <span class="bigscreen-table-count">{{ getTableData(chart).length }} 条</span>
                  </div>
                  <div class="bigscreen-table-body">
                    <table>
                      <thead>
                        <tr>
                          <th v-for="column in getTableColumns(chart)" :key="column.key">{{ column.title }}</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="(row, rowIndex) in getTableRows(chart)" :key="rowIndex">
                          <td v-for="column in getTableColumns(chart)" :key="column.key">{{ formatTableCell(row[column.key]) }}</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </template>
              <div
                v-else
                :ref="(el: any) => setChartRef(chart, el)"
                class="chart-render-area"
              />
            </div>
          </div>
        </template>
      </div>
    </BigscreenPreview>

    <!-- 参数抽屉 -->
    <n-drawer v-model:show="showParamDrawer" :width="360" placement="right">
      <n-drawer-content title="参数设置">
        <div class="param-list">
          <div v-for="param in parameterList" :key="param.name" class="param-item">
            <label class="param-label">{{ param.label || param.name }}</label>
            <n-input
              v-if="param.type === 'text' || !param.type"
              v-model:value="paramValues[param.name]"
              :placeholder="param.placeholder || `请输入${param.label || param.name}`"
              size="small"
            />
            <n-date-picker
              v-else-if="param.type === 'date'"
              v-model:formatted-value="paramValues[param.name]"
              type="date"
              value-format="yyyy-MM-dd"
              size="small"
              style="width: 100%"
            />
            <n-select
              v-else-if="param.type === 'select'"
              v-model:value="paramValues[param.name]"
              :options="param.options || []"
              size="small"
            />
          </div>
        </div>
        <template #footer>
          <n-space>
            <n-button size="small" @click="handleResetParams">重置</n-button>
            <n-button type="primary" size="small" @click="handleApplyParams">应用</n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>

    <!-- 加载状态 -->
    <div v-if="loading" class="bigscreen-loading">
      <n-spin size="large" />
      <span>正在加载大屏数据...</span>
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, onMounted, onBeforeUnmount, nextTick, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { 
  FilterOutline, ArrowBackOutline, CreateOutline,
  RefreshOutline, ExpandOutline, ContractOutline, 
  TimeOutline, ChevronDownOutline
} from '@vicons/ionicons5'
import BigscreenPreview from '@/components/PageDesigner/BigscreenPreview.vue'
import { getPageDefinitionById } from '@/api/page'
import { getChartData } from '@/api/chart'
import { testInlineChartSql } from '@/api/pageDesigner'
import { handleApiError } from '@/utils/error'
import { buildChartOption } from '@/utils/chartRenderer'
import type { PageDefinition, PageChart, BigscreenConfig } from '@/types/page'
import { createDefaultBigscreenConfig } from '@/types/page'
import echarts, { ensureExtendedChartsForOption } from '@/utils/echarts'
import type { ECharts } from '@/utils/echarts'

const route = useRoute()
const router = useRouter()
const message = useMessage()

// 预览模式（从设计器跳转时）
const isPreviewMode = computed(() => route.query['preview'] === '1')
const returnToDesigner = () => {
  const pageId = route.params['id']
  // 大屏页面返回时使用 params 传递 id
  router.push({ name: 'PageDesigner', params: { id: String(pageId) } })
}

// ===== 全屏与自动刷新功能 =====
const isFullscreen = ref(false)
const isRefreshing = ref(false)
const toolbarVisible = ref(false)
const autoRefreshEnabled = ref(false)
const autoRefreshInterval = ref(30)
const autoRefreshCountdown = ref(30)
let autoRefreshTimer: ReturnType<typeof setInterval> | null = null

const refreshIntervalOptions = [
  { label: '10秒', key: 10 },
  { label: '30秒', key: 30 },
  { label: '60秒', key: 60 },
  { label: '5分钟', key: 300 }
]

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

const toggleAutoRefresh = () => {
  autoRefreshEnabled.value = !autoRefreshEnabled.value
  if (autoRefreshEnabled.value) {
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
}

const startAutoRefresh = () => {
  stopAutoRefresh()
  autoRefreshCountdown.value = autoRefreshInterval.value
  autoRefreshTimer = setInterval(() => {
    autoRefreshCountdown.value--
    if (autoRefreshCountdown.value <= 0) {
      refreshAllCharts()
      autoRefreshCountdown.value = autoRefreshInterval.value
    }
  }, 1000)
}

const stopAutoRefresh = () => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = null
  }
}

const setAutoRefreshInterval = (key: number) => {
  autoRefreshInterval.value = key
  autoRefreshCountdown.value = key
  if (autoRefreshEnabled.value) {
    startAutoRefresh()
  }
}

const refreshAllCharts = async () => {
  if (isRefreshing.value) return
  isRefreshing.value = true
  try {
    const promises = sortedCharts.value
      .filter(c => c.mode !== 'static' && !isTableChart(c))
      .map(chart => {
        const key = getChartKey(chart)
        const el = chartRefEls.get(key)
        if (el) return renderChart(key, el, chart)
        return Promise.resolve()
      })
    await Promise.all(promises)
  } finally {
    isRefreshing.value = false
  }
}

const loading = ref(true)
const showScaleInfo = ref(false)
const showParamDrawer = ref(false)
const pageName = ref('')
const pageData = ref<PageDefinition | null>(null)
const bigscreenConfig = ref<BigscreenConfig>(createDefaultBigscreenConfig())
const paramValues = reactive<Record<string, any>>({})
const chartInstances = new Map<string | number, ECharts>()
const renderedKeys = new Set<string | number>()
const chartRefEls = new Map<string | number, HTMLElement>()

let refreshTimer: ReturnType<typeof setInterval> | null = null
let clockTimer: ReturnType<typeof setInterval> | null = null
const clockTime = ref(new Date().toLocaleString())

const sortedCharts = computed(() => {
  if (!pageData.value?.charts) return []
  return [...pageData.value.charts].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
})

const parameterList = computed(() => {
  if (!pageData.value?.parameterPanel?.components) return []
  return pageData.value.parameterPanel.components
})

const hasParameterPanel = computed(() => parameterList.value.length > 0)

const activeParamCount = computed(() =>
  Object.values(paramValues).filter(v => v !== null && v !== undefined && v !== '').length
)

const chartsGridStyle = computed(() => ({
  width: '100%',
  height: '100%',
  position: 'relative' as const
}))

function getChartStyle(chart: PageChart): Record<string, string> {
  if (chart.left !== undefined && chart.top !== undefined) {
    return {
      position: 'absolute',
      left: `${chart.left}px`,
      top: `${chart.top}px`,
      width: `${chart.width || 400}px`,
      height: `${chart.height || 300}px`
    }
  }
  const w = chart.w || 6
  const h = chart.h || 4
  return {
    width: `${(w / 12) * 100}%`,
    height: `${h * 80}px`,
    display: 'inline-block',
    verticalAlign: 'top'
  }
}

function parseMaybeJson<T = any>(value: unknown): T | undefined {
  if (!value) return undefined
  if (typeof value === 'string') {
    try {
      return JSON.parse(value) as T
    } catch {
      return undefined
    }
  }
  return value as T
}

function getChartKey(chart: PageChart): string | number {
  return chart.id || chart.chartId || `inline-${chart.sortOrder ?? 0}-${chart.left ?? 0}-${chart.top ?? 0}`
}

function getInlineConfig(chart: PageChart): any {
  return parseMaybeJson((chart as any).inlineConfig) || {}
}

function getEffectiveChartType(chart: PageChart): string {
  if (chart.mode === 'inline') return getInlineConfig(chart).chartType || ''
  return chart.chart?.chartType || ''
}

function getChartName(chart: PageChart): string {
  if (chart.mode === 'inline') return getInlineConfig(chart).chartName || '数据图表'
  return chart.chart?.chartName || '数据图表'
}

function normalizeArrayPayload(payload: any): any[] {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.rows)) return payload.rows
  if (Array.isArray(payload?.records)) return payload.records
  if (Array.isArray(payload?.list)) return payload.list
  return []
}

function getInlineChartConfigObject(chart: PageChart): any {
  const config = getInlineConfig(chart)
  return parseMaybeJson(config.chartConfig) || config.chartConfig || {}
}

function isTableChart(chart: PageChart): boolean {
  const type = getEffectiveChartType(chart)
  return type === 'table' || type === 'summaryTable' || type === 'pivotTable'
}

function getTableData(chart: PageChart): any[] {
  const config = getInlineConfig(chart)
  const staticData = normalizeArrayPayload(parseMaybeJson(config.staticData) ?? config.staticData)
  if (staticData.length > 0) return staticData
  return [
    { name: '华东一区', category: '重点客户', amount: 128, rate: '32%' },
    { name: '华南一区', category: '增长客户', amount: 116, rate: '29%' },
    { name: '华北一区', category: '稳定客户', amount: 104, rate: '26%' },
    { name: '西南一区', category: '潜力客户', amount: 88, rate: '22%' }
  ]
}

function getTableColumns(chart: PageChart) {
  const tableStyle = getInlineChartConfigObject(chart)?.tableStyle || {}
  const data = getTableData(chart)
  const keys = tableStyle.displayColumns?.length
    ? tableStyle.displayColumns
    : Object.keys(data[0] || {})
  const labels = tableStyle.columnLabels || {}
  return keys.slice(0, 6).map((key: string) => ({
    key,
    title: labels[key] || key
  }))
}

function getTableRows(chart: PageChart): any[] {
  const tableStyle = getInlineChartConfigObject(chart)?.tableStyle || {}
  const pageSize = Number(tableStyle.pageSize) || 6
  return getTableData(chart).slice(0, Math.max(3, Math.min(10, pageSize)))
}

function formatTableCell(value: unknown): string {
  if (value === null || value === undefined) return '-'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function enhanceBigscreenChartOption(option: any, chartType?: string) {
  if (!option || typeof option !== 'object') return option
  if (option.radar || chartType === 'radar') {
    const radar = option.radar || {}
    option.radar = {
      ...radar,
      radius: radar.radius || '58%',
      center: radar.center || ['50%', '50%'],
      axisName: { color: '#9fd7ff', fontSize: 11, ...(radar.axisName || {}) },
      splitLine: { lineStyle: { color: ['rgba(64,158,255,0.45)', 'rgba(64,158,255,0.24)'], ...(radar.splitLine?.lineStyle || {}) }, ...(radar.splitLine || {}) },
      splitArea: { areaStyle: { color: ['rgba(0,212,255,0.04)', 'rgba(0,255,204,0.025)'], ...(radar.splitArea?.areaStyle || {}) }, ...(radar.splitArea || {}) },
      axisLine: { lineStyle: { color: 'rgba(64,158,255,0.35)', ...(radar.axisLine?.lineStyle || {}) }, ...(radar.axisLine || {}) }
    }
    const series = Array.isArray(option.series) ? option.series : option.series ? [option.series] : []
    series.forEach((item: any, index: number) => {
      if (item?.type !== 'radar') return
      item.symbol = item.symbol || 'circle'
      item.symbolSize = item.symbolSize || 4
      item.lineStyle = { width: 2, ...(item.lineStyle || {}) }
      item.areaStyle = { opacity: index === 0 ? 0.22 : 0.1, ...(item.areaStyle || {}) }
    })
  }
  return option
}

function setChartRef(chart: PageChart, el: HTMLElement | null) {
  if (chart.mode === 'static') return
  if (isTableChart(chart)) return
  const key = getChartKey(chart)
  if (el) {
    chartRefEls.set(key, el)
    if (!renderedKeys.has(key)) {
      nextTick(async () => {
        const rendered = await renderChart(key, el, chart)
        if (rendered) renderedKeys.add(key)
      })
    }
  } else {
    chartRefEls.delete(key)
  }
}

async function renderChart(key: string | number, el: HTMLElement, chart: PageChart): Promise<boolean> {
  if (!el || isTableChart(chart)) return false
  if (el.clientWidth === 0 || el.clientHeight === 0) {
    setTimeout(() => {
      if (el.clientWidth > 0 && el.clientHeight > 0) {
        renderChart(key, el, chart)
      }
    }, 120)
    return false
  }

  const existing = chartInstances.get(key)
  if (existing) {
    existing.dispose()
  }

  const theme = (bigscreenConfig.value.backgroundType === 'color' &&
    bigscreenConfig.value.backgroundValue?.startsWith('#f')) ? undefined : 'dark'
  const instance = echarts.init(el, theme)
  if (!instance) return false
  chartInstances.set(key, instance)

  let option: any = null
  const inlineCfg = getInlineConfig(chart)
  const isInline = (chart.mode === 'inline') || (inlineCfg && !chart.chartId)

  if (isInline && inlineCfg) {
    // 内联图表
    try {
      let rawCfg: any = {}
      if (inlineCfg.chartConfig) {
        try {
          rawCfg = typeof inlineCfg.chartConfig === 'string'
            ? JSON.parse(inlineCfg.chartConfig) : inlineCfg.chartConfig
        } catch { /* ignore */ }
      }
      if (rawCfg?.series && Array.isArray(rawCfg.series) && rawCfg.series.length > 0) {
        option = rawCfg
      } else if (inlineCfg.dataSourceId && inlineCfg.sqlContent) {
        // 执行SQL获取数据
        const sqlRes = await testInlineChartSql({
          dataSourceId: inlineCfg.dataSourceId,
          sqlContent: inlineCfg.sqlContent,
          limit: inlineCfg.dataLimit || 1000
        }) as any
        const chartData = Array.isArray(sqlRes?.data) ? sqlRes.data : (Array.isArray(sqlRes?.data?.data) ? sqlRes.data.data : [])
        if (chartData.length > 0) {
          const mapping = inlineCfg.fieldMapping || {}
          const chartType = inlineCfg.chartType || 'bar'
          const keys = Object.keys(chartData[0] || {})
          const xField: string = (typeof mapping.xAxis === 'string' ? mapping.xAxis : keys[0]) || keys[0] || ''
          const yFieldRaw = mapping.yAxis || mapping.valueField || keys[1]
          const yField: string = Array.isArray(yFieldRaw) ? (yFieldRaw[0] || '') : (yFieldRaw || keys[1] || '')
          option = {
            tooltip: { trigger: chartType === 'pie' ? 'item' : 'axis' },
            legend: chartType === 'pie' ? { bottom: 10, textStyle: { color: '#8ca8c8' } } : undefined,
            xAxis: chartType !== 'pie' ? { type: 'category', data: chartData.map((row: any) => String(row[xField] || '')) } : undefined,
            yAxis: chartType !== 'pie' ? { type: 'value' } : undefined,
            series: [{
              type: chartType,
              name: inlineCfg.chartName || '数据',
              data: chartType === 'pie'
                ? chartData.map((row: any) => ({ name: String(row[xField] || ''), value: Number(row[yField] || 0) }))
                : chartData.map((row: any) => Number(row[yField] || 0)),
              ...(chartType === 'pie' ? { radius: ['40%', '70%'] } : {})
            }]
          }
        }
      } else if (Object.keys(rawCfg).length > 0) {
        option = rawCfg
      }
    } catch (e) { console.warn('内联图表渲染失败:', e) }
  } else if (chart.chartId) {
    // 引用图表
    try {
      const dataRes = await getChartData(chart.chartId, { useCache: false }) as any
      const chartData = dataRes?.data?.data || dataRes?.data || []
      const chartDef = chart.chart || dataRes?.data?.chart || {}
      let chartCfg: any = {}
      if (chartDef.chartConfig) {
        try { chartCfg = typeof chartDef.chartConfig === 'string' ? JSON.parse(chartDef.chartConfig) : chartDef.chartConfig } catch { /* ignore */ }
      }
      if (chartCfg && Object.keys(chartCfg).length > 0) {
        option = chartCfg
      }
      if (!option && Array.isArray(chartData) && chartData.length > 0) {
        option = buildChartOption(chartDef, chartData)
      }
    } catch (e) { console.warn('引用图表渲染失败:', e) }
  }

  if (option && Object.keys(option).length > 0) {
    option = enhanceBigscreenChartOption(option, getEffectiveChartType(chart))
    await ensureExtendedChartsForOption(option, getEffectiveChartType(chart))
    option.backgroundColor = 'transparent'
    instance.setOption(option, true)
    setTimeout(() => {
      safeResizeChart(instance)
    }, 80)
    return true
  }
  return false
}

function safeResizeChart(instance?: ECharts) {
  if (!instance || instance.isDisposed()) return
  try {
    instance.resize()
  } catch (error) {
    requestAnimationFrame(() => {
      try {
        if (!instance.isDisposed()) instance.resize()
      } catch (retryError) {
        console.warn('澶у睆鍥捐〃 resize 澶辫触:', retryError)
      }
    })
  }
}

function resizeAllCharts() {
  chartInstances.forEach(instance => {
    safeResizeChart(instance)
  })
}

async function loadPage() {
  const pageId = Number(route.params['id'])
  if (!pageId || isNaN(pageId)) {
    message.error('页面ID无效')
    loading.value = false
    return
  }

  loading.value = true
  try {
    const res = await getPageDefinitionById(pageId)
    let page: PageDefinition | null = null
    if (res && typeof res === 'object') {
      if ((res as any).data && typeof (res as any).data === 'object') {
        page = (res as any).data
      } else if ('pageName' in res) {
        page = res as unknown as PageDefinition
      }
    }

    if (!page) {
      message.error('页面不存在')
      loading.value = false
      return
    }

    pageData.value = page
    pageName.value = page.pageName || '数据大屏'

    // 解析所有图表的 inlineConfig（后端返回 JSON 字符串）
    if (page.charts) {
      page.charts = page.charts.map((chart: any) => {
        let parsed = chart.inlineConfig
        if (typeof parsed === 'string') {
          try { parsed = JSON.parse(parsed) } catch { parsed = null }
        }
        if (chart.mode === 'static' && parsed) {
          return { ...chart, staticType: parsed.staticType, staticConfig: parsed.staticConfig || {} }
        }
        return { ...chart, inlineConfig: parsed || chart.inlineConfig }
      })
    }

    if (page.bigscreenConfig) {
      const config = typeof page.bigscreenConfig === 'string'
        ? JSON.parse(page.bigscreenConfig)
        : page.bigscreenConfig
      bigscreenConfig.value = { ...createDefaultBigscreenConfig(), ...config }
    }

    if (page.parameterPanel?.components) {
      for (const comp of page.parameterPanel.components) {
        if (comp.defaultValue !== undefined) {
          paramValues[comp.name] = comp.defaultValue
        }
      }
    }

    await nextTick()
    setupAutoRefresh()
    setTimeout(() => {
      for (const chart of sortedCharts.value) {
        if (chart.mode === 'static' || isTableChart(chart)) continue
        const key = getChartKey(chart)
        const el = chartRefEls.get(key)
        if (el && !chartInstances.has(key)) {
          renderChart(key, el, chart).then(rendered => {
            if (rendered) renderedKeys.add(key)
          })
        }
      }
      resizeAllCharts()
    }, 300)
  } catch (error: any) {
    const errorMsg = handleApiError(error, '加载大屏页面')
    message.error(errorMsg)
  } finally {
    loading.value = false
  }
}

function setupAutoRefresh() {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
  const interval = bigscreenConfig.value.refreshInterval
  if (interval && interval > 0 && interval >= 5) {
    refreshTimer = setInterval(() => {
      refreshChartsOnly()
    }, interval * 1000)
  }
}

async function refreshChartsOnly() {
  if (!pageData.value?.charts) return
  for (const chart of pageData.value.charts) {
    if (chart.mode === 'static' || isTableChart(chart)) continue
    const key = getChartKey(chart)
    const el = chartRefEls.get(key)
    if (el) {
      await renderChart(key, el, chart)
    }
  }
}

function handleClose() {
  router.back()
}

function handleResetParams() {
  Object.keys(paramValues).forEach(key => {
    paramValues[key] = ''
  })
}

function handleApplyParams() {
  showParamDrawer.value = false
  loadPage()
}

const resizeHandler = () => {
  requestAnimationFrame(resizeAllCharts)
}

const handleFullscreenChange = () => {
  isFullscreen.value = !!document.fullscreenElement
}

onMounted(async () => {
  await loadPage()
  window.addEventListener('resize', resizeHandler)
  document.addEventListener('fullscreenchange', handleFullscreenChange)

  // Update clock every second instead of on every render
  clockTimer = setInterval(() => {
    clockTime.value = new Date().toLocaleString()
  }, 1000)

  if (route.query['debug'] === '1') {
    showScaleInfo.value = true
  }
})

onBeforeUnmount(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (clockTimer) clearInterval(clockTimer)
  stopAutoRefresh()
  window.removeEventListener('resize', resizeHandler)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  chartInstances.forEach(instance => {
    if (instance && !instance.isDisposed()) {
      instance.dispose()
    }
  })
  chartInstances.clear()
})
</script>

<style scoped>
.bigscreen-view-root {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  position: fixed;
  top: 0;
  left: 0;
  background: transparent;
}

.bigscreen-charts {
  padding: 0;
}

.bigscreen-chart-item {
  box-sizing: border-box;
  padding: 4px;
}

.chart-wrapper {
  width: 100%;
  height: 100%;
  border-radius: 4px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.04);
}

.chart-render-area {
  width: 100%;
  height: 100%;
  min-height: 200px;
}

.table-wrapper {
  padding: 0;
}

.bigscreen-table-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 4px;
  background: rgba(6, 30, 60, 0.28);
}

.bigscreen-table-header {
  height: 38px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  color: #00d4ff;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0;
  border-bottom: 1px solid rgba(64, 158, 255, 0.22);
  background: rgba(10, 38, 76, 0.42);
}

.bigscreen-table-count {
  color: #8ca8c8;
  font-size: 12px;
  font-weight: 400;
}

.bigscreen-table-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.bigscreen-table-body table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.bigscreen-table-body th,
.bigscreen-table-body td {
  height: 34px;
  padding: 0 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  border-bottom: 1px solid rgba(64, 158, 255, 0.12);
  font-size: 12px;
  text-align: left;
  letter-spacing: 0;
}

.bigscreen-table-body th {
  color: #9fd7ff;
  font-weight: 600;
  background: rgba(64, 158, 255, 0.08);
}

.bigscreen-table-body td {
  color: #c8d8e8;
}

.bigscreen-table-body tr:nth-child(even) td {
  background: rgba(255, 255, 255, 0.025);
}

.bigscreen-param-trigger {
  display: flex;
  align-items: center;
}

.bigscreen-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: rgba(0, 0, 0, 0.85);
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  z-index: 100;
}

.param-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.param-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.param-label {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

/* ===== 大屏静态组件 ===== */
.bigscreen-static-item {
  box-sizing: border-box;
  overflow: hidden;
}
.bs-pv-header-bar {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  padding: 0 24px;
}
.bs-pv-header-title { font-weight: 700; text-align: center; letter-spacing: 4px; }
.bs-pv-header-time { position: absolute; right: 24px; font-size: 13px; }
.bs-pv-kpi-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  border: 1px solid rgba(64,158,255,0.4);
  padding: 12px;
  box-sizing: border-box;
}
.bs-pv-kpi-label { font-size: 13px; margin-bottom: 4px; }
.bs-pv-kpi-value { font-weight: 700; line-height: 1.2; }
.bs-pv-number-flipper {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  padding: 12px;
  box-sizing: border-box;
}
.bs-pv-flipper-label { font-size: 13px; margin-bottom: 8px; }
.bs-pv-flipper-digits { display: flex; gap: 4px; }
.bs-pv-digit {
  font-weight: 700;
  font-family: 'Courier New', monospace;
  background: rgba(0,0,0,0.2);
  padding: 4px 8px;
  border-radius: 4px;
  line-height: 1;
}
.bs-pv-flipper-unit { font-size: 13px; margin-top: 6px; }
.bs-pv-decor-border {
  height: 100%;
  border: 1px solid;
  border-radius: 4px;
  position: relative;
  padding: 12px;
  box-sizing: border-box;
}
.bs-pv-decor-corner { position: absolute; width: 12px; height: 12px; border-color: inherit; }
.bs-pv-decor-corner.tl { top: -1px; left: -1px; border-top: 2px solid; border-left: 2px solid; }
.bs-pv-decor-corner.tr { top: -1px; right: -1px; border-top: 2px solid; border-right: 2px solid; }
.bs-pv-decor-corner.bl { bottom: -1px; left: -1px; border-bottom: 2px solid; border-left: 2px solid; }
.bs-pv-decor-corner.br { bottom: -1px; right: -1px; border-bottom: 2px solid; border-right: 2px solid; }
.bs-pv-decor-title { font-size: 15px; font-weight: 600; text-align: center; }
.bs-pv-progress-bar {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-radius: 8px;
  padding: 12px 16px;
  box-sizing: border-box;
}
.bs-pv-progress-label { font-size: 13px; margin-bottom: 8px; }
.bs-pv-progress-track { height: 8px; background: rgba(255,255,255,0.1); border-radius: 4px; overflow: hidden; }
.bs-pv-progress-fill { height: 100%; border-radius: 4px; transition: width 0.6s ease; }
.bs-pv-progress-value { font-size: 14px; font-weight: 600; margin-top: 6px; text-align: right; }

/* 预览模式工具栏 */
.preview-mode-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 99999;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-bottom: 1px solid #f59e0b;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.3);
}
.preview-bar-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
}
.preview-bar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.preview-label {
  font-size: 13px;
  font-weight: 600;
  color: #92400e;
}
.preview-bar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* ===== 大屏控制工具栏 ===== */
.screen-toolbar {
  position: fixed !important;
  top: 50% !important;
  right: 12px !important;
  transform: translateY(-50%) translateX(20px);
  z-index: 999999 !important;
  opacity: 0;
  pointer-events: none;
  transition: all 0.3s ease;
}
.screen-toolbar.is-visible {
  transform: translateY(-50%) translateX(0);
  opacity: 1;
  pointer-events: auto;
}
.toolbar-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 10px 8px;
  background: rgba(15, 23, 42, 0.95);
  backdrop-filter: blur(12px);
  border-radius: 12px;
  border: 1px solid rgba(99, 102, 241, 0.3);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.4);
}
.toolbar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 36px;
  min-width: 36px;
  padding: 0 10px;
  border-radius: 8px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: #94a3b8;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}
.toolbar-btn:hover {
  background: rgba(99, 102, 241, 0.3);
  color: #e2e8f0;
}
.toolbar-btn.active {
  background: rgba(16, 185, 129, 0.3);
  color: #4ade80;
}
.countdown-text {
  font-size: 11px;
  color: #4ade80;
  font-weight: 600;
}
.toolbar-divider {
  width: 24px;
  height: 1px;
  background: rgba(255, 255, 255, 0.15);
  margin: 4px 0;
}
.fullscreen-btn {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%) !important;
  color: #fff !important;
  padding: 0 14px !important;
}
.fullscreen-btn:hover {
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%) !important;
}
.fullscreen-btn .btn-text {
  font-size: 12px;
  font-weight: 600;
  color: #fff !important;
}
.hide-btn {
  padding: 0 8px;
  min-width: 28px;
  color: #64748b;
}
.hide-btn:hover {
  color: #94a3b8;
}

/* 工具栏触发器 */
.toolbar-trigger {
  position: fixed !important;
  top: 50% !important;
  right: 0 !important;
  transform: translateY(-50%);
  z-index: 999999 !important;
  width: 28px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border-radius: 28px 0 0 28px;
  box-shadow: -3px 0 16px rgba(99, 102, 241, 0.5);
  color: #fff;
  cursor: pointer;
  transition: all 0.2s ease;
}
.toolbar-trigger:hover {
  width: 36px;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
}
.toolbar-trigger .n-icon {
  transform: rotate(90deg);
}

/* 旋转动画 */
.spin-animation {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
