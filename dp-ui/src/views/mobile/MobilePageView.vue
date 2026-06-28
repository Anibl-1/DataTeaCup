<template>
  <div class="mobile-page-view">
    <!-- 顶部导航（复用 MobileHeader） -->
    <MobileHeader
      :title="pageDef?.pageName || '页面查看'"
      show-back
      :action-icon="RefreshOutline"
      @action="handleRefresh"
    />

    <MobilePageShell no-tab-bar>
    <!-- 骨架屏 -->
    <template v-if="loading && !pageDef">
      <div class="page-info-bar">
        <n-skeleton text style="width: 80px" :sharp="false" />
        <n-skeleton text style="width: 140px" :sharp="false" />
      </div>
      <div class="chart-list">
        <div v-for="i in 2" :key="i" class="chart-section">
          <div class="chart-section-header">
            <n-skeleton text style="width: 100px" :sharp="false" />
          </div>
          <div class="chart-section-body">
            <n-skeleton style="width: 100%; height: 200px" :sharp="false" />
          </div>
        </div>
      </div>
    </template>

    <!-- 页面信息 -->
    <template v-else>
    <div v-if="pageDef" class="page-info-bar">
      <n-tag type="info" size="small" round>{{ pageDef.pageCode }}</n-tag>
      <span v-if="pageDef.description" class="page-info-desc">{{ pageDef.description }}</span>
      <span class="page-info-chart-count">
        <n-icon size="12"><BarChartOutline /></n-icon>
        {{ sortedCharts.length }}
      </span>
    </div>

    <!-- 参数面板 -->
    <div v-if="hasParamPanel" class="param-section">
      <div class="param-toggle" @click="paramExpanded = !paramExpanded">
        <n-icon size="16"><FunnelOutline /></n-icon>
        <span>查询参数 ({{ paramComponents.length }})</span>
        <n-icon size="14" :style="{ transform: paramExpanded ? 'rotate(180deg)' : '' }"><ChevronDownOutline /></n-icon>
      </div>
      <Transition name="param-expand">
      <div v-if="paramExpanded" class="param-body">
        <div v-for="comp in paramComponents" :key="comp.id" class="param-item">
          <label>{{ comp.label }}<span v-if="comp.required" class="required">*</span></label>
          <n-input v-if="comp.type === 'text'" v-model:value="paramValues[comp.name]" :placeholder="comp.placeholder || '请输入'" size="small" clearable />
          <n-input-number v-else-if="comp.type === 'number'" v-model:value="paramValues[comp.name]" :placeholder="comp.placeholder || '请输入'" size="small" clearable style="width: 100%" />
          <n-date-picker v-else-if="comp.type === 'date'" v-model:formatted-value="paramValues[comp.name]" type="date" :value-format="comp.dateFormat || 'yyyy-MM-dd'" size="small" clearable style="width: 100%" />
          <n-date-picker v-else-if="comp.type === 'dateRange'" v-model:formatted-value="paramValues[comp.name]" type="daterange" :value-format="comp.dateFormat || 'yyyy-MM-dd'" size="small" clearable style="width: 100%" />
          <n-select v-else-if="comp.type === 'select'" v-model:value="paramValues[comp.name]" :options="(comp.options as any[]) || []" :placeholder="comp.placeholder || '请选择'" size="small" clearable />
          <n-select v-else-if="comp.type === 'multiSelect'" v-model:value="paramValues[comp.name]" :options="(comp.options as any[]) || []" :placeholder="comp.placeholder || '请选择'" size="small" multiple clearable />
          <n-input v-else v-model:value="paramValues[comp.name]" :placeholder="comp.placeholder || '请输入'" size="small" clearable />
        </div>
        <n-button type="primary" size="small" block @click="handleParamQuery">查询</n-button>
      </div>
      </Transition>
    </div>

    <!-- 错误状态 -->
    <div v-if="error" class="error-state">
      <n-result status="error" title="加载失败" :description="error" />
    </div>

    <!-- 图表列表（纵向单列排列） -->
    <div v-else-if="pageDef && sortedCharts.length > 0" class="chart-list">
      <template
        v-for="(chart, idx) in sortedCharts"
        :key="chart.id || idx"
      >
        <!-- 静态组件渲染 -->
        <div
          v-if="chart.mode === 'static'"
          class="static-section"
          :class="{ 'static-section-half': isCompactStaticKpi(chart) }"
        >
          <template v-if="chart.staticType === 'title'">
            <div class="mobile-static-title" :style="{ fontSize: (chart.staticConfig?.fontSize || 20) + 'px', fontWeight: chart.staticConfig?.fontWeight || 'bold', color: chart.staticConfig?.color || '#333', textAlign: chart.staticConfig?.align || 'left' }">{{ chart.staticConfig?.text || '' }}</div>
          </template>
          <template v-else-if="chart.staticType === 'text'">
            <div class="mobile-static-text" :style="{ fontSize: (chart.staticConfig?.fontSize || 14) + 'px', color: chart.staticConfig?.color || '#666', textAlign: chart.staticConfig?.align || 'left' }">{{ chart.staticConfig?.text || '' }}</div>
          </template>
          <template v-else-if="chart.staticType === 'kpiCard'">
            <div class="mobile-kpi-card" :style="{ background: chart.staticConfig?.bgColor || '#fff', borderColor: chart.staticConfig?.borderColor || '#e5e7eb' }">
              <div class="mobile-kpi-label" :style="{ color: chart.staticConfig?.labelColor || '#6b7280' }">{{ chart.staticConfig?.label || 'KPI' }}</div>
              <div class="mobile-kpi-value" :style="{ color: chart.staticConfig?.valueColor || '#111827', fontSize: (chart.staticConfig?.valueFontSize || 28) + 'px' }">{{ chart.staticConfig?.prefix || '' }}{{ chart.staticConfig?.value || '0' }}{{ chart.staticConfig?.suffix || '' }}</div>
            </div>
          </template>
          <template v-else-if="chart.staticType === 'divider'">
            <hr :style="{ border: 'none', borderTop: (chart.staticConfig?.thickness || 1) + 'px ' + (chart.staticConfig?.style || 'solid') + ' ' + (chart.staticConfig?.color || '#e0e0e0') }" />
          </template>
          <template v-else-if="chart.staticType === 'image'">
            <img v-if="chart.staticConfig?.src" :src="chart.staticConfig.src" :alt="chart.staticConfig?.alt || ''" style="max-width: 100%; border-radius: 8px;" />
          </template>
          <template v-else-if="chart.staticType === 'progressBar'">
            <div class="mobile-progress-bar">
              <div class="mobile-progress-label" :style="{ color: chart.staticConfig?.labelColor || '#6b7280' }">{{ chart.staticConfig?.label || '' }}</div>
              <div class="mobile-progress-track"><div class="mobile-progress-fill" :style="{ width: (chart.staticConfig?.percent || 60) + '%', background: chart.staticConfig?.barColor || 'linear-gradient(90deg, #3b82f6, #60a5fa)' }"></div></div>
              <div class="mobile-progress-value" :style="{ color: chart.staticConfig?.valueColor || '#3b82f6' }">{{ chart.staticConfig?.percent || 60 }}%</div>
            </div>
          </template>
        </div>
        <!-- 图表卡片渲染 -->
        <div v-else class="chart-section">
        <div class="chart-section-header">
          <span class="chart-section-title">
            {{ getChartName(chart) }}
          </span>
          <n-button text size="small" @click="toggleFullscreen(idx)">
            <template #icon>
              <n-icon size="16"><ExpandOutline /></n-icon>
            </template>
          </n-button>
        </div>
        <div
          class="chart-section-body"
          :class="{ 'chart-fullscreen': fullscreenIdx === idx }"
        >
          <!-- 全屏模式关闭按钮 -->
          <div v-if="fullscreenIdx === idx" class="fullscreen-close" @click="toggleFullscreen(idx)">
            <n-icon size="24"><CloseOutline /></n-icon>
          </div>
          <div :ref="(el) => setChartRef(idx, el as HTMLElement)" class="chart-render-box"></div>
          <div v-if="chartRenderErrors[idx]" class="chart-render-error">
            {{ chartRenderErrors[idx] }}
          </div>
        </div>
        </div>
      </template>
    </div>

    <!-- 无图表 -->
    <MobileEmpty v-else type="data" title="暂无图表" description="此页面暂无图表内容" />
    </template>
    </MobilePageShell>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import MobileHeader from '@/components/mobile/MobileHeader.vue'
import MobilePageShell from '@/components/mobile/MobilePageShell.vue'
import MobileEmpty from '@/components/mobile/MobileEmpty.vue'
import { NButton, NIcon, NTag, NInput, NInputNumber, NDatePicker, NSelect, NSkeleton, NResult } from 'naive-ui'
import { RefreshOutline, ExpandOutline, FunnelOutline, ChevronDownOutline, CloseOutline, BarChartOutline } from '@vicons/ionicons5'
import { getPageDefinitionById } from '@/api/page'
import { saveRecentVisit } from '@/utils/recentVisits'
import { getChartData } from '@/api/chart'
import { testInlineChartSql } from '@/api/pageDesigner'
import { buildChartOption, buildInlineChartOption } from '@/utils/chartRenderer'
import echarts, { ensureExtendedChartsForOption } from '@/utils/echarts'
import type { PageDefinition, PageChart } from '@/types/page'
import type { QueryComponent } from '@/types/pageParameter'

const route = useRoute()

const pageDef = ref<PageDefinition | null>(null)
const loading = ref(false)
const error = ref('')
const fullscreenIdx = ref(-1)
const chartRefs: Record<number, HTMLElement | null> = {}
const echartInstances: Record<number, any> = {}
const chartRenderErrors = ref<Record<number, string>>({})
const paramExpanded = ref(false)
const paramValues = ref<Record<string, any>>({})
let renderScheduleTimer: ReturnType<typeof setTimeout> | null = null

const hasParamPanel = computed(() => {
  const panel = pageDef.value?.parameterPanel as any
  if (!panel) return false
  // visible可能是true/false或1/0
  const isVisible = panel.visible === true || panel.visible === 1 || panel.visible === '1'
  const hasComponents = Array.isArray(panel.components) && panel.components.length > 0
  return isVisible && hasComponents
})

const paramComponents = computed<QueryComponent[]>(() => {
  if (!pageDef.value?.parameterPanel?.components) return []
  return [...pageDef.value.parameterPanel.components].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
})

const sortedCharts = computed<PageChart[]>(() => {
  if (!pageDef.value?.charts) return []
  return [...pageDef.value.charts].sort((a, b) => {
    const sa = a.sortOrder ?? 999
    const sb = b.sortOrder ?? 999
    if (sa !== sb) return sa - sb
    const ta = Number(a.top) || 0
    const tb = Number(b.top) || 0
    if (ta !== tb) return ta - tb
    return (Number(a.left) || 0) - (Number(b.left) || 0)
  })
})

function isCompactStaticKpi(chart: PageChart): boolean {
  return chart.mode === 'static' &&
    (chart as any).staticType === 'kpiCard' &&
    Number(chart.width || 0) > 0 &&
    Number(chart.width || 0) <= 190
}

function setChartRef(idx: number, el: HTMLElement | null) {
  if (chartRefs[idx] === el) return
  chartRefs[idx] = el
  if (el) {
    scheduleRenderAllCharts(80)
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

function resolveInlineConfig(chart: PageChart): any {
  return parseMaybeJson((chart as any).inlineConfig)
}

function normalizeArrayPayload(payload: any): any[] {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.rows)) return payload.rows
  if (Array.isArray(payload?.records)) return payload.records
  if (Array.isArray(payload?.list)) return payload.list
  return []
}

function getInlineStaticData(inlineCfg: any): any[] {
  if (!inlineCfg) return []
  return normalizeArrayPayload(parseMaybeJson(inlineCfg.staticData) ?? inlineCfg.staticData)
}

function normalizeInlineChartConfig(inlineCfg: any): string | undefined {
  if (!inlineCfg?.chartConfig) return undefined
  return typeof inlineCfg.chartConfig === 'string'
    ? inlineCfg.chartConfig
    : JSON.stringify(inlineCfg.chartConfig)
}

function getChartName(chart: PageChart): string {
  const inlineCfg = resolveInlineConfig(chart)
  if ((chart.mode === 'inline' || (inlineCfg && !chart.chartId)) && inlineCfg) {
    return inlineCfg.chartName || '内联图表'
  }
  if (chart.chart?.chartName) {
    return chart.chart.chartName
  }
  return `图表 ${chart.chartId || ''}`
}

function getEffectiveChartType(chart: PageChart): string {
  const inlineCfg = resolveInlineConfig(chart)
  const rawConfig = parseMaybeJson(inlineCfg?.chartConfig)
  const firstSeries = Array.isArray(rawConfig?.series) ? rawConfig.series[0] : rawConfig?.series
  return inlineCfg?.chartType || chart.chart?.chartType || firstSeries?.type || 'bar'
}

function scheduleRenderAllCharts(delay = 100) {
  if (renderScheduleTimer) {
    clearTimeout(renderScheduleTimer)
  }
  renderScheduleTimer = setTimeout(() => {
    renderScheduleTimer = null
    renderAllCharts()
  }, delay)
}

function initParamDefaults() {
  if (!pageDef.value?.parameterPanel?.components) return
  for (const comp of pageDef.value.parameterPanel.components) {
    if (paramValues.value[comp.name] !== undefined) continue
    if (comp.defaultValue !== undefined && comp.defaultValue !== null) {
      paramValues.value[comp.name] = comp.defaultValue
    } else {
      paramValues.value[comp.name] = null
    }
  }
}

function buildChartQueryParams(chartId: number): Record<string, any> {
  if (!pageDef.value?.parameterPanel?.components) return {}
  const result: Record<string, any> = {}
  for (const comp of pageDef.value.parameterPanel.components) {
    const val = paramValues.value[comp.name]
    if (val === null || val === undefined || val === '') continue
    for (const binding of comp.bindings) {
      if (binding.chartId === chartId) {
        result[comp.name] = {
          value: val,
          field: binding.field,
          operator: binding.operator || '='
        }
      }
    }
  }
  return result
}

async function handleParamQuery() {
  await renderAllCharts()
}

async function loadPage() {
  const id = Number(route.params['id'])
  if (!id) return

  loading.value = true
  error.value = ''
  try {
    const res = await getPageDefinitionById(id) as any
    pageDef.value = res?.data?.data || res?.data || null
    if (pageDef.value) {
      // 解析parameterPanel如果是JSON字符串
      if (typeof pageDef.value.parameterPanel === 'string') {
        try {
          pageDef.value.parameterPanel = JSON.parse(pageDef.value.parameterPanel)
        } catch { /* ignore */ }
      }
      saveRecentVisit(`/m/page/${id}`, pageDef.value.pageName || '页面')
      initParamDefaults()
      // 解析静态组件的 inlineConfig
      if (pageDef.value.charts) {
        pageDef.value.charts = pageDef.value.charts.map((chart: any) => {
          const parsed = parseMaybeJson(chart.inlineConfig)
          if (chart.mode === 'static' && parsed) {
            return { ...chart, inlineConfig: parsed, staticType: parsed.staticType, staticConfig: parsed.staticConfig || {} }
          }
          if ((chart.mode === 'inline' || (!chart.chartId && parsed)) && parsed) {
            return { ...chart, inlineConfig: parsed }
          }
          return chart
        })
      }
      if (pageDef.value.charts && pageDef.value.charts.length > 0) {
        await nextTick()
        await renderAllCharts()
      }
    }
  } catch (e: any) {
    // 忽略请求被取消的情况
    if (e?.message === 'canceled' || e?.code === 'ERR_CANCELED') {
      return
    }
    error.value = e?.message || '加载页面失败'
  } finally {
    loading.value = false
    if (pageDef.value?.charts?.length) {
      await nextTick()
      scheduleRenderAllCharts(120)
    }
  }
}

/** 移动端图表优化：缩小字号、紧凑图例、合理边距、触控tooltip */
function applyMobileChartOptions(config: any): any {
  const isDark = document.documentElement.classList.contains('dark')
  if (!config.grid) config.grid = {}
  config.grid = { left: 40, right: 16, top: 40, bottom: 36, containLabel: true, ...config.grid }
  if (config.legend) {
    config.legend.textStyle = { fontSize: 11, ...(config.legend.textStyle || {}) }
    config.legend.itemWidth = config.legend.itemWidth ?? 14
    config.legend.itemHeight = config.legend.itemHeight ?? 10
    config.legend.itemGap = config.legend.itemGap ?? 8
    if (!config.legend.type) config.legend.type = 'scroll'
    config.legend.pageIconSize = 10
  }
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
  // tooltip 优化
  if (!config.tooltip) config.tooltip = {}
  config.tooltip.confine = true
  config.tooltip.textStyle = { fontSize: 12, ...(config.tooltip.textStyle || {}) }
  config.tooltip.appendToBody = false
  // 饼图优化
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
  if (isDark) config.backgroundColor = 'transparent'
  return config
}

async function renderAllCharts(retryCount = 0) {
  const charts = sortedCharts.value
  if (!charts.length) return

  // 等待DOM完全渲染
  await nextTick()
  // 额外延迟确保CSS样式已应用
  await new Promise(resolve => setTimeout(resolve, 200))

  const isDark = document.documentElement.classList.contains('dark')
  let hasZeroSize = false
  
  for (let i = 0; i < charts.length; i++) {
    const chart = charts[i] as PageChart
    // 跳过静态组件
    if (chart.mode === 'static') continue
    const el = chartRefs[i]
    if (!el || !chart) continue

    // 检查容器尺寸
    const rect = el.getBoundingClientRect()
    if (rect.width === 0 || rect.height === 0) {
      hasZeroSize = true
      continue
    }

    try {
      delete chartRenderErrors.value[i]
      if (echartInstances[i]) {
        echartInstances[i].dispose()
      }
      echartInstances[i] = echarts.init(el, isDark ? 'dark' : undefined)

      let option: any = null
      let inlineCfg = chart.inlineConfig
      const refChart = chart.chart

      // 如果inlineConfig是字符串，先解析它
      if (typeof inlineCfg === 'string') {
        try {
          inlineCfg = JSON.parse(inlineCfg)
        } catch {
          inlineCfg = undefined
        }
      }

      // 判断是否为内联图表：有inlineConfig或mode为inline
      const isInline = (chart.mode === 'inline') || (inlineCfg && !chart.chartId)
      
      if (isInline && inlineCfg) {
        // 内联图表：解析chartConfig
        try {
          let rawCfg: any = {}
          if (inlineCfg.chartConfig) {
            try {
              rawCfg = typeof inlineCfg.chartConfig === 'string'
                ? JSON.parse(inlineCfg.chartConfig)
                : inlineCfg.chartConfig
            } catch { /* ignore */ }
          }
          
          // 如果配置中已有series数据，直接使用
          if (rawCfg && rawCfg.series && Array.isArray(rawCfg.series) && rawCfg.series.length > 0) {
            option = rawCfg
          } else if (getInlineStaticData(inlineCfg).length > 0) {
            option = buildInlineChartOption(
              inlineCfg.chartType || 'bar',
              getInlineStaticData(inlineCfg),
              inlineCfg.fieldMapping,
              inlineCfg.colorScheme,
              normalizeInlineChartConfig(inlineCfg)
            )
          } else if (inlineCfg.dataSourceId && inlineCfg.sqlContent) {
            // 需要执行SQL获取数据
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
                legend: chartType === 'pie' ? { bottom: 10 } : undefined,
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
        } catch { /* ignore */ }
      } else if (chart.chartId) {
        // 引用图表：使用 buildChartOption 合并配置+数据
        try {
          const queryParams = buildChartQueryParams(chart.chartId)
          const chartParams = Object.keys(queryParams).length > 0 ? queryParams : {}
          const dataRes = await getChartData(chart.chartId, {
            parameters: chartParams,
            useCache: false
          }) as any
          const data = Array.isArray(dataRes?.data) ? dataRes.data : (Array.isArray(dataRes?.data?.data) ? dataRes.data.data : [])

          const chartDefObj = refChart ? { ...refChart } : { chartConfig: null, chartType: 'bar' }
          option = buildChartOption(chartDefObj, data)

          // 如果 buildChartOption 返回空/无效，使用 fallback
          if (!option || Object.keys(option).length === 0 || (option as any)._isTable) {
            const keys = Object.keys(data[0] || {})
            if (keys.length >= 2) {
              option = {
                tooltip: { trigger: 'axis' },
                xAxis: { type: 'category', data: data.map((row: any) => String(row[keys[0] as string] || '')) },
                yAxis: { type: 'value' },
                series: [{ type: chartDefObj.chartType || 'bar', data: data.map((row: any) => Number(row[keys[1] as string]) || 0) }]
              }
            }
          }
        } catch { /* ignore */ }
      }

      if (option && Object.keys(option).length > 0 && !(option as any)._isTable) {
        option = applyMobileChartOptions(option)
        await ensureExtendedChartsForOption(option, getEffectiveChartType(chart))
        echartInstances[i].setOption(option, true)
        echartInstances[i].resize()
      } else {
        chartRenderErrors.value[i] = '图表暂无可渲染数据'
      }
    } catch (e) {
      chartRenderErrors.value[i] = '图表渲染失败'
      console.warn('Mobile page chart render failed', e)
    }
  }
  
  // 如果有容器尺寸为0且重试次数未超限，延迟重试
  if (hasZeroSize && retryCount < 5) {
    setTimeout(() => renderAllCharts(retryCount + 1), 300)
  }
}

function toggleFullscreen(idx: number) {
  fullscreenIdx.value = fullscreenIdx.value === idx ? -1 : idx
  nextTick(() => {
    if (echartInstances[idx]) {
      echartInstances[idx].resize()
    }
  })
}

function handleRefresh() {
  loadPage()
}

function handleResize() {
  Object.values(echartInstances).forEach(inst => inst?.resize())
}

function handleOrientationChange() {
  setTimeout(() => {
    Object.values(echartInstances).forEach(inst => inst?.resize())
  }, 300)
}

let themeObserver: MutationObserver | null = null

onMounted(() => {
  loadPage()
  window.addEventListener('resize', handleResize)
  window.addEventListener('orientationchange', handleOrientationChange)
  // 监听暗色模式切换，实时重新渲染图表
  themeObserver = new MutationObserver(() => {
    if (sortedCharts.value.length > 0) {
      renderAllCharts()
    }
  })
  themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('orientationchange', handleOrientationChange)
  themeObserver?.disconnect()
  if (renderScheduleTimer) {
    clearTimeout(renderScheduleTimer)
    renderScheduleTimer = null
  }
  Object.values(echartInstances).forEach(inst => inst?.dispose())
})

watch(() => route.params['id'], () => {
  loadPage()
})
</script>

<style scoped>
.mobile-page-view {
  display: flex;
  flex-direction: column;
}

.page-info-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 16px;
  margin: 0 12px 8px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.page-info-desc {
  order: 3;
  flex: 1 0 100%;
  font-size: 12px;
  line-height: 1.45;
  color: #94a3b8;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  white-space: normal;
}

.page-info-chart-count {
  display: flex;
  align-items: center;
  gap: 3px;
  margin-left: auto;
  font-size: 11px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 10px;
  flex-shrink: 0;
}

.chart-list {
  padding: 0 12px 12px;
  display: flex;
  flex-flow: row wrap;
  gap: 12px;
}

.chart-section {
  flex: 1 0 100%;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05), 0 1px 2px rgba(0, 0, 0, 0.03);
}

.chart-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.chart-section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.chart-section-body {
  position: relative;
  padding: 8px;
  transition: height 0.3s ease;
}

.chart-render-box {
  width: 100%;
  height: clamp(250px, 55vw, 360px);
  min-height: 220px;
  touch-action: pan-y;
}

.chart-render-error {
  position: absolute;
  inset: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 13px;
  pointer-events: none;
}

@media (orientation: landscape) {
  .chart-render-box {
    height: 50vh;
    min-height: 200px;
    max-height: 420px;
  }
  .chart-list {
    padding: 0 8px 8px;
    gap: 8px;
  }
  .page-info-bar {
    margin: 0 8px 6px;
  }
  .param-section {
    margin: 0 8px 6px;
  }
}

.chart-section-body.chart-fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  background: #fff;
  padding: 16px;
  padding-top: calc(16px + env(safe-area-inset-top, 0px));
  padding-bottom: calc(16px + env(safe-area-inset-bottom, 0px));
  border-radius: 0;
}

.chart-section-body.chart-fullscreen .chart-render-box {
  height: calc(100% - 50px);
  margin-top: 50px;
  max-height: none;
}

.fullscreen-close {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 1001;
  color: #475569;
  transition: all 0.2s;
  -webkit-tap-highlight-color: transparent;
}

.fullscreen-close:active {
  background: rgba(0, 0, 0, 0.15);
  transform: scale(0.9);
}

.error-state {
  padding: 40px 20px;
}

/* 参数面板 */
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

.param-expand-enter-active,
.param-expand-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
  max-height: 500px;
}
.param-expand-enter-from,
.param-expand-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
}

/* 深色模式 - 表单控件 */

/* 深色模式 - 展开按钮 */

/* ===== 静态组件样式 ===== */
.static-section {
  flex: 1 0 100%;
}
.static-section-half {
  flex: 1 1 calc(50% - 6px);
  min-width: 0;
}
.mobile-static-title {
  padding: 8px 0;
}
.mobile-static-text {
  padding: 4px 0;
  line-height: 1.6;
}
.mobile-kpi-card {
  min-height: 88px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.mobile-kpi-label {
  font-size: 13px;
  margin-bottom: 4px;
}
.mobile-kpi-value {
  font-weight: 700;
  line-height: 1.2;
  word-break: break-word;
}
.mobile-progress-bar {
  padding: 12px 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.mobile-progress-label {
  font-size: 13px;
  margin-bottom: 6px;
}
.mobile-progress-track {
  height: 8px;
  background: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
}
.mobile-progress-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s ease;
}
.mobile-progress-value {
  font-size: 13px;
  font-weight: 600;
  margin-top: 4px;
  text-align: right;
}
</style>

<style>
/* MobilePageView 深色模式（非 scoped） */
html.dark .page-info-bar {
  background: #1e293b !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important;
}
html.dark .page-info-desc { color: #64748b !important; }
html.dark .page-info-chart-count { background: #334155 !important; color: #94a3b8 !important; }
html.dark .param-section {
  background: #1e293b !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important;
}
html.dark .param-toggle { color: #e2e8f0 !important; }
html.dark .param-toggle:active { background: #334155 !important; }
html.dark .param-item label { color: #94a3b8 !important; }
html.dark .chart-section {
  background: #1e293b !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important;
}
html.dark .chart-section-header { border-bottom-color: #334155 !important; }
html.dark .chart-section-title { color: #e2e8f0 !important; }
html.dark .chart-section-body { background: #1e293b !important; }
html.dark .chart-render-box { background: transparent !important; }
html.dark .chart-section-body.chart-fullscreen { background: #0f172a !important; }
html.dark .fullscreen-close {
  background: rgba(255, 255, 255, 0.1) !important;
  color: #e2e8f0 !important;
}
html.dark .fullscreen-close:active {
  background: rgba(255, 255, 255, 0.2) !important;
}
html.dark .error-state { color: #94a3b8 !important; }
html.dark .param-item .n-input,
html.dark .param-item .n-input,
html.dark .param-item .n-input-number {
  background: #0f172a !important;
  border-color: #334155 !important;
}
html.dark .param-item .n-input .n-input__input-el,
html.dark .param-item .n-input .n-input__input-el,
html.dark .param-item .n-input .n-input__textarea-el {
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
html.dark .param-item .n-base-selection .n-base-selection-placeholder__inner {
  color: #475569 !important;
}
html.dark .chart-section-header .n-button {
  color: #94a3b8 !important;
}
html.dark .chart-section-header .n-button:hover {
  color: #60a5fa !important;
}
</style>
