<template>
  <div class="chart-center">
    <!-- 左侧分类面板 -->
    <ChartCategoryPanel
      :collapsed="leftPanelCollapsed"
      :active-category="filterCategory"
      :total-count="totalCount"
      :category-list="categoryList"
      @toggle-collapse="leftPanelCollapsed = !leftPanelCollapsed"
      @select-category="handleCategorySelect"
    />

    <!-- 右侧主内容区 -->
    <div class="main-content">
      <!-- 列表模式 -->
      <template v-if="!chartId">
        <!-- 工具栏 + 快速筛选 -->
        <ChartCenterToolbar
          v-model:search-keyword="searchKeyword"
          v-model:filter-chart-type="filterChartType"
          v-model:sort-by="sortBy"
          v-model:view-mode="viewMode"
          :show-favorite-only="showFavoriteOnly"
          :active-quick-filter="activeQuickFilter"
          :show-quick-filters="!chartId"
          @search="handleSearch"
          @toggle-favorite-filter="showFavoriteOnly = !showFavoriteOnly; handleSearch()"
          @quick-filter="handleQuickFilter"
        />

        <!-- 网格视图 -->
        <div class="chart-list">
          <ChartGrid
            v-if="viewMode === 'grid'"
            :loading="loading"
            :chart-list="chartList"
            :recent-charts="recentCharts"
            :show-recent-section="recentCharts.length > 0 && !searchKeyword && !filterChartType"
            :favorites="favoritesCache"
            :page="paginationPage"
            :page-size="paginationPageSize"
            :item-count="paginationItemCount"
            @view-chart="handleViewChart"
            @view-recent="handleViewRecentChart"
            @favorite="handleFavoriteChart"
            @clear-recent="clearRecentCharts"
            @reset-filter="handleResetFilter"
            @page-change="handlePageChange"
            @page-size-change="handlePageSizeChange"
          />

          <!-- 列表视图 -->
          <template v-else>
            <ChartListView
              v-if="!loading"
              :chart-list="chartList"
              :favorites="favoritesCache"
              @view-chart="handleViewChart"
              @favorite="handleFavoriteChart"
            />

            <!-- 空状态 -->
            <div v-if="!loading && chartList.length === 0" class="empty-state">
              <div class="empty-illustration">
                <n-icon size="64" color="#cbd5e1"><BarChartOutline /></n-icon>
              </div>
              <div class="empty-title">{{ t('chart.noChart') }}</div>
              <div class="empty-desc">{{ t('chart.noChartDesc') }}</div>
              <n-button type="primary" @click="handleResetFilter">{{ t('chart.resetFilter') }}</n-button>
            </div>

            <!-- 分页 -->
            <div v-if="chartList.length > 0" class="pagination">
              <n-pagination
                v-model:page="paginationPage"
                v-model:page-size="paginationPageSize"
                :item-count="paginationItemCount"
                :page-sizes="[12, 24, 48]"
                show-size-picker
                :prefix="() => `${t('common.total')} ${paginationItemCount} ${t('common.items')}`"
                @update:page="handlePageChange"
                @update:page-size="handlePageSizeChange"
              />
            </div>
          </template>
        </div>
      </template>

      <!-- 图表详情 -->
      <div v-else class="chart-detail">
        <!-- 顶部导航栏 -->
        <div class="detail-nav-bar">
          <div class="nav-left">
            <n-button text class="back-btn" @click="handleBackToList">
              <template #icon><n-icon size="18"><ArrowBackOutline /></n-icon></template>
              {{ fromPage === 'chart-manage' ? t('chart.backToManage') : t('chart.backToList') }}
            </n-button>
            <span class="nav-divider">/</span>
            <div class="nav-chart-info">
              <n-icon size="18" :color="getChartTypeColor(currentChart?.chartType || '')">
                <component :is="getChartIconComponent(currentChart?.chartType)" />
              </n-icon>
              <h2 class="chart-title-text">{{ currentChart?.chartName }}</h2>
              <n-tag :type="currentChart?.status === 1 ? 'success' : 'default'" size="small" round>
                {{ currentChart?.status === 1 ? t('chart.enabled') : t('chart.disabled') }}
              </n-tag>
            </div>
          </div>
          <div class="nav-actions">
            <n-button 
              v-if="chartParameters.length > 0"
              :type="showParameterPanel ? 'primary' : 'default'"
              size="small"
              round
              @click="showParameterPanel = !showParameterPanel"
            >
              <template #icon><n-icon size="15"><FilterOutline /></n-icon></template>
              {{ showParameterPanel ? t('chart.hideParams') : t('chart.params') }}
            </n-button>
            <n-select
              v-if="!isTableType"
              v-model:value="currentTheme"
              :options="themeOptions"
              size="small"
              style="width: 95px"
              @update:value="handleThemeChange"
            />
            <n-button-group size="small">
              <n-button :type="isFavorite(currentChart?.id) ? 'warning' : 'default'" @click="handleFavoriteChart(currentChart!)">
                <template #icon><n-icon size="15"><component :is="isFavorite(currentChart?.id) ? StarSharp : StarOutline" /></n-icon></template>
              </n-button>
            </n-button-group>
            <n-dropdown :options="detailExportOptions" @select="handleExportSelect">
              <n-button size="small">
                <template #icon><n-icon size="15"><DownloadOutline /></n-icon></template>
                {{ t('common.export') }}
                <n-icon size="12" style="margin-left: 2px;"><ChevronDownOutline /></n-icon>
              </n-button>
            </n-dropdown>
            <n-button size="small" @click="handleFullscreenChart">
              <template #icon><n-icon size="15"><ExpandOutline /></n-icon></template>
            </n-button>
            <n-button size="small" :loading="clearingCache" @click="handleClearCache">
              <template #icon><n-icon size="15"><TrashOutline /></n-icon></template>
            </n-button>
            <n-select
              v-model:value="autoRefreshInterval"
              :options="autoRefreshOptions"
              size="small"
              style="width: 110px"
              @update:value="handleAutoRefreshChange"
            />
            <n-button type="primary" size="small" :loading="chartLoading" @click="handleRefreshChart">
              <template #icon><n-icon size="15"><RefreshOutline /></n-icon></template>
              {{ t('common.refresh') }}
            </n-button>
          </div>
        </div>

        <!-- 信息栏 -->
        <div class="detail-info-bar">
          <div class="info-bar-left">
            <n-tag size="small" :bordered="false" :color="{ color: getChartTypeTagBg(currentChart?.chartType), textColor: getChartTypeColor(currentChart?.chartType || '') }">
              <template #icon>
                <n-icon size="13"><component :is="getChartIconComponent(currentChart?.chartType)" /></n-icon>
              </template>
              {{ getChartTypeLabel(currentChart?.chartType) }}
            </n-tag>
            <span class="meta-chip">
              <n-icon size="13"><CodeOutline /></n-icon>
              {{ currentChart?.chartCode }}
            </span>
            <span class="meta-chip">
              <n-icon size="13"><TimeOutline /></n-icon>
              {{ formatDate(currentChart?.updateTime) }}
            </span>
            <span v-if="currentChart?.dataSourceId" class="meta-chip">
              {{ t('chart.dataSourceId') }} #{{ currentChart.dataSourceId }}
            </span>
            <n-tag v-if="dataStats" size="small" :bordered="false" type="success">
              {{ dataStats.rowCount }} {{ t('chart.rows') }} × {{ dataStats.columnCount }} {{ t('chart.columns') }}
            </n-tag>
            <n-tag v-if="loadDuration > 0" size="small" :bordered="false" type="info">
              {{ t('chart.duration') }} {{ loadDuration }}ms
            </n-tag>
          </div>
          <div class="info-bar-desc">
            <n-icon size="14" color="#94a3b8"><InformationCircleOutline /></n-icon>
            <span>{{ currentChart?.description || t('chart.noDesc') }}</span>
          </div>
        </div>

        <!-- 参数输入面板 -->
        <div v-if="chartParameters.length > 0" class="parameter-panel" :class="{ collapsed: !showParameterPanel }">
          <div class="param-panel-header" style="cursor: pointer;" @click="showParameterPanel = !showParameterPanel">
            <div class="param-panel-title">
              <n-icon size="18" color="#18a058"><FilterOutline /></n-icon>
              <span>{{ t('chart.params') }}</span>
              <n-tag size="small" :bordered="false" type="info">{{ t('chart.paramCount', { count: chartParameters.length }) }}</n-tag>
            </div>
            <n-icon size="16" :style="{ transform: showParameterPanel ? 'rotate(0)' : 'rotate(180deg)', transition: 'transform 0.3s' }">
              <ChevronUpOutline />
            </n-icon>
          </div>
          <n-collapse-transition :show="showParameterPanel">
            <div class="param-panel-body">
              <div class="param-inputs">
                <div v-for="param in chartParameters" :key="param.name" class="param-input-item">
                  <label :class="{ required: param.required }">
                    {{ param.label || param.name }}
                    <n-tooltip v-if="param.description" trigger="hover">
                      <template #trigger>
                        <n-icon size="12" class="text-muted" style="cursor: help;"><HelpCircleOutline /></n-icon>
                      </template>
                      {{ param.description }}
                    </n-tooltip>
                  </label>
                  <n-input
                    v-if="param.type === 'text'"
                    :value="String(parameterValues[param.name] ?? '')"
                    :placeholder="param.placeholder || t('chart.inputPlaceholder', { label: param.label })"
                    size="small"
                    clearable
                    @update:value="(v: string) => parameterValues[param.name] = v"
                    @keydown.enter="handleApplyParameters"
                  />
                  <n-input-number
                    v-else-if="param.type === 'number'"
                    :value="typeof parameterValues[param.name] === 'number' ? parameterValues[param.name] as number : null"
                    :placeholder="param.placeholder || t('chart.inputPlaceholder', { label: param.label })"
                    :min="param.min ?? 0"
                    :max="param.max ?? 999999999"
                    size="small"
                    clearable
                    @update:value="(v: number | null) => parameterValues[param.name] = v"
                  />
                  <n-date-picker
                    v-else-if="param.type === 'date'"
                    :formatted-value="typeof parameterValues[param.name] === 'string' ? parameterValues[param.name] as string : null"
                    type="date"
                    :placeholder="param.placeholder || t('chart.selectPlaceholder', { label: param.label })"
                    value-format="yyyy-MM-dd"
                    size="small"
                    clearable
                    :shortcuts="datePickerShortcuts"
                    @update:formatted-value="(v: string | null) => parameterValues[param.name] = v"
                  />
                  <n-date-picker
                    v-else-if="param.type === 'dateRange'"
                    :formatted-value="Array.isArray(parameterValues[param.name]) ? parameterValues[param.name] as [string, string] : null"
                    type="daterange"
                    :start-placeholder="t('chart.startDate')"
                    :end-placeholder="t('chart.endDate')"
                    value-format="yyyy-MM-dd"
                    size="small"
                    clearable
                    :shortcuts="dateRangeShortcuts"
                    @update:formatted-value="(v: [string, string] | null) => parameterValues[param.name] = v"
                  />
                  <!-- eslint-disable vue/no-deprecated-filter -->
                  <n-select
                    v-else-if="param.type === 'select'"
                    :value="parameterValues[param.name] as string | number | null"
                    :options="param.options || []"
                    :placeholder="param.placeholder || t('chart.selectPlaceholder', { label: param.label })"
                    size="small"
                    clearable
                    @update:value="(v: string | number | null) => parameterValues[param.name] = v"
                  />
                  <!-- eslint-enable vue/no-deprecated-filter -->
                  <n-select
                    v-else-if="param.type === 'multiSelect'"
                    :value="Array.isArray(parameterValues[param.name]) ? parameterValues[param.name] as string[] : []"
                    :options="param.options || []"
                    :placeholder="param.placeholder || t('chart.selectPlaceholder', { label: param.label })"
                    size="small"
                    multiple
                    clearable
                    max-tag-count="responsive"
                    @update:value="(v: string[]) => parameterValues[param.name] = v"
                  />
                </div>
              </div>
              <div class="param-actions">
                <n-button size="small" @click="handleResetParameters">
                  <template #icon><n-icon size="14"><RefreshOutline /></n-icon></template>
                  {{ t('common.reset') }}
                </n-button>
                <n-button size="small" type="primary" :loading="chartLoading" @click="handleApplyParameters">
                  <template #icon><n-icon size="14"><SearchOutline /></n-icon></template>
                  {{ t('common.search') }}
                </n-button>
              </div>
            </div>
          </n-collapse-transition>
        </div>

        <n-spin :show="chartLoading">
          <!-- 表格类型 -->
          <template v-if="isTableType">
            <div class="table-wrapper" :style="chartDisplayStyle">
              <div class="table-toolbar">
                <div class="table-toolbar-left">
                  <n-icon size="16" color="#3b82f6"><ListOutline /></n-icon>
                  <span class="table-toolbar-title">{{ t('chart.dataTable') }}</span>
                  <n-tag v-if="dataStats" size="small" :bordered="false" round type="info">
                    {{ dataStats.rowCount }} {{ t('chart.records') }}
                  </n-tag>
                </div>
                <div class="table-toolbar-right">
                  <n-tag size="small" :bordered="false" type="default">
                    {{ tableColumns2.length }} {{ t('chart.columns') }}
                  </n-tag>
                </div>
              </div>
              <div class="table-container custom-header-table" :style="tableHeaderStyle">
                <n-data-table
                  :columns="tableColumns2"
                  :data="paginatedChartData"
                  :bordered="false"
                  :single-line="false"
                  :striped="tableStyleConfig.striped !== false"
                  :size="tableStyleConfig.size || 'medium'"
                  :scroll-x="tableScrollX"
                  flex-height
                >
                  <template #empty>
                    <n-empty :description="t('common.noData')" />
                  </template>
                </n-data-table>
              </div>
              <div v-if="showTablePagination" class="table-pagination">
                <n-pagination
                  v-model:page="tableCurrentPage"
                  :page-size="tablePageSize"
                  :item-count="chartData.length"
                  :page-sizes="[10, 20, 50, 100]"
                  show-size-picker
                  :prefix="() => `${t('common.total')} ${chartData.length} ${t('common.items')}`"
                  @update:page-size="(s) => { tablePageSize = s; tableCurrentPage = 1 }"
                />
              </div>
            </div>
          </template>
          <!-- ECharts 图表 -->
          <template v-else>
            <div ref="chartRef" class="chart-container" :style="chartDisplayStyle"></div>
          </template>
        </n-spin>
      </div>
    </div>

    <!-- 全屏模态框 -->
    <n-modal v-model:show="showFullscreen" preset="card" :title="t('chart.fullscreenView')" style="width: 95vw; height: 95vh;">
      <!-- 全屏模式下的参数面板 -->
      <div v-if="chartParameters.length > 0" class="fullscreen-param-bar">
        <n-space :size="12" align="center" wrap>
          <template v-for="param in chartParameters" :key="param.name">
            <div class="fullscreen-param-item">
              <span class="param-label">{{ param.label }}:</span>
              <n-input
                v-if="param.type === 'text'"
                :value="String(parameterValues[param.name] ?? '')"
                :placeholder="param.placeholder || param.label"
                size="small"
                style="width: 200px"
                clearable
                @update:value="(v: string) => parameterValues[param.name] = v"
              />
              <n-input-number
                v-else-if="param.type === 'number'"
                :value="typeof parameterValues[param.name] === 'number' ? parameterValues[param.name] as number : null"
                :placeholder="param.placeholder || param.label"
                :min="param.min ?? 0"
                :max="param.max ?? 999999999"
                size="small"
                style="width: 180px"
                clearable
                @update:value="(v: number | null) => parameterValues[param.name] = v"
              />
              <n-date-picker
                v-else-if="param.type === 'date'"
                :formatted-value="typeof parameterValues[param.name] === 'string' ? parameterValues[param.name] as string : null"
                type="date"
                value-format="yyyy-MM-dd"
                size="small"
                style="width: 200px"
                clearable
                :shortcuts="datePickerShortcuts"
                @update:formatted-value="(v: string | null) => parameterValues[param.name] = v"
              />
              <n-date-picker
                v-else-if="param.type === 'dateRange'"
                :formatted-value="Array.isArray(parameterValues[param.name]) ? parameterValues[param.name] as [string, string] : null"
                type="daterange"
                value-format="yyyy-MM-dd"
                size="small"
                style="width: 320px"
                clearable
                :shortcuts="dateRangeShortcuts"
                @update:formatted-value="(v: [string, string] | null) => parameterValues[param.name] = v"
              />
              <!-- eslint-disable vue/no-deprecated-filter -->
              <n-select
                v-else-if="param.type === 'select'"
                :value="parameterValues[param.name] as string | number | null"
                :options="param.options || []"
                size="small"
                style="width: 200px"
                clearable
                @update:value="(v: string | number | null) => parameterValues[param.name] = v"
              />
              <!-- eslint-enable vue/no-deprecated-filter -->
              <n-select
                v-else-if="param.type === 'multiSelect'"
                :value="Array.isArray(parameterValues[param.name]) ? parameterValues[param.name] as string[] : []"
                :options="param.options || []"
                size="small"
                style="width: 240px"
                multiple
                max-tag-count="responsive"
                clearable
                @update:value="(v: string[]) => parameterValues[param.name] = v"
              />
            </div>
          </template>
          <n-button size="small" type="primary" :loading="chartLoading" @click="handleFullscreenQuery">
            <template #icon><n-icon size="14"><SearchOutline /></n-icon></template>
            {{ t('chart.query') }}
          </n-button>
        </n-space>
      </div>
      <!-- 全屏表格 -->
      <template v-if="isTableType">
        <div class="fullscreen-table-wrapper" :style="{ height: chartParameters.length > 0 ? 'calc(95vh - 150px)' : 'calc(95vh - 100px)' }">
          <div class="fullscreen-table-container custom-header-table" :style="tableHeaderStyle">
            <n-data-table
              :columns="tableColumns2"
              :data="paginatedChartData"
              :bordered="tableStyleConfig.bordered !== false"
              :single-line="false"
              :striped="tableStyleConfig.striped !== false"
              :size="tableStyleConfig.size || 'medium'"
              :scroll-x="tableScrollX"
              flex-height
            >
              <template #empty>
                <n-empty :description="t('common.noData')" />
              </template>
            </n-data-table>
          </div>
          <div v-if="showTablePagination" class="table-pagination">
            <n-pagination
              v-model:page="tableCurrentPage"
              :page-size="tablePageSize"
              :item-count="chartData.length"
              :page-sizes="[10, 20, 50, 100]"
              show-size-picker
              :prefix="() => `${t('common.total')} ${chartData.length} ${t('common.items')}`"
              @update:page-size="(s) => { tablePageSize = s; tableCurrentPage = 1 }"
            />
          </div>
        </div>
      </template>
      <!-- 全屏图表 -->
      <template v-else>
        <div ref="fullscreenChartRef" :style="{ width: '100%', height: chartParameters.length > 0 ? 'calc(95vh - 150px)' : 'calc(95vh - 100px)' }"></div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { logger } from '@/utils/logger'
import { ref, reactive, computed, onMounted, onBeforeUnmount, onActivated, nextTick, watch, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NIcon, NButton, NTag, NInput, NInputNumber, NDatePicker, NSelect, NSpace, NCollapseTransition, NTooltip, NDropdown, NDataTable, NEmpty, NPagination, useMessage } from 'naive-ui'
import echarts from '@/utils/echarts'
import {
  SearchOutline, StarOutline, StarSharp,
  DownloadOutline, ExpandOutline, RefreshOutline,
  ArrowBackOutline, InformationCircleOutline, AppsOutline,
  ChevronUpOutline, ChevronDownOutline,
  BarChartOutline, TrendingUpOutline, StatsChartOutline,
  GitNetworkOutline,
  FilterOutline, HelpCircleOutline, TrashOutline,
  DocumentOutline, ImageOutline, CodeOutline, TimeOutline, ListOutline
} from '@vicons/ionicons5'
import { getChartDefinitionList, getChartDefinitionById, getChartData, clearChartCache } from '@/api/chart'
import { buildChartOption, getChartTypeColor } from '@/utils/chartRenderer'
import { registerThemes, themeOptions } from '@/utils/chartThemes'
import { exportToPng, exportToExcel, exportToCsv, exportToJson, copyChartToClipboard, getChartDataSummary, resolveWatermarkText } from '@/utils/chartExport'
import { chartCache } from '@/utils/chartCache'
import { formatCellValueSmart } from '@/utils/format'
import type { ChartDefinition, ChartParameter, ChartParameterValue } from '@/types/chart'
import { CHART_TYPES, CHART_CATEGORY_LABELS, type ChartCategory } from '@/types/chart'
import { getChartIconComponent, getChartTypeLabel, formatChartDate } from '@/utils/chartUtils'
import type { PageResult } from '@/types/api'
import { useTabsStore } from '@/stores/tabs'
import { useUserStore } from '@/stores/user'
import { useI18n } from '@/i18n'
import ChartCategoryPanel from './ChartCategoryPanel.vue'
import ChartCenterToolbar from './ChartCenterToolbar.vue'
import ChartGrid from './ChartGrid.vue'
import ChartListView from './ChartListView.vue'

const { t } = useI18n()

// 注册图表主题
registerThemes()

const route = useRoute()
const router = useRouter()
const message = useMessage()
const tabsStore = useTabsStore()

// 记录来源页面
const fromPage = ref<string | null>(null)

// 状态
const loading = ref(false)
const chartLoading = ref(false)
const loadDuration = ref(0)
const leftPanelCollapsed = ref(false)
const viewMode = ref<'grid' | 'list'>('grid')
const showFavoriteOnly = ref(false)
const showFullscreen = ref(false)
const clearingCache = ref(false)
const currentTheme = ref('default')

// 快速筛选
const activeQuickFilter = ref<string | null>(null)

// 最近查看记录
interface RecentChart {
  id: number
  chartName: string
  chartType: string
  viewedAt: number
}
const recentCharts = ref<RecentChart[]>([])

const loadRecentCharts = () => {
  try {
    const saved = localStorage.getItem('recent_charts')
    if (saved) recentCharts.value = JSON.parse(saved)
  } catch (_) { /* recent_charts parse failed */ }
}

const saveToRecentCharts = (chart: ChartDefinition) => {
  const existing = recentCharts.value.filter(c => c.id !== chart.id)
  const newRecent: RecentChart = {
    id: chart.id,
    chartName: chart.chartName || t('chart.name'),
    chartType: chart.chartType || 'bar',
    viewedAt: Date.now()
  }
  recentCharts.value = [newRecent, ...existing].slice(0, 10)
  localStorage.setItem('recent_charts', JSON.stringify(recentCharts.value))
}

const clearRecentCharts = () => {
  recentCharts.value = []
  localStorage.removeItem('recent_charts')
}

const handleViewRecentChart = (chart: RecentChart) => {
  if (chartInstance) { chartInstance.dispose(); chartInstance = null }
  chartId.value = chart.id
  tabsStore.replaceTab(route.fullPath, {
    key: `/chart-center/${chart.id}`,
    title: chart.chartName || t('chart.title'),
    closable: true
  })
  router.replace(`/chart-center/${chart.id}`)
  loadChartData(chart.id)
}

const handleQuickFilter = (value: string) => {
  if (value === activeQuickFilter.value || value === 'all') {
    activeQuickFilter.value = null
    filterChartType.value = null
  } else {
    activeQuickFilter.value = value
    filterChartType.value = value
  }
  handleSearch()
}

// 数据统计
const dataStats = computed(() => {
  if (chartData.value.length === 0) return null
  return getChartDataSummary(chartData.value)
})

const DEFAULT_CHART_HEIGHT = 400

const parseVisualSizeValue = (value: unknown): number | null => {
  if (value === null || value === undefined || value === '') return null
  const num = typeof value === 'number' ? value : Number(value)
  return Number.isFinite(num) && num > 0 ? num : null
}

const currentChartConfig = computed(() => {
  if (!currentChart.value?.chartConfig) return null
  try {
    return JSON.parse(currentChart.value.chartConfig)
  } catch (e) {
    console.warn('解析chartConfig失败:', e)
    return null
  }
})

const chartVisualSize = computed(() => {
  const config = currentChartConfig.value
  if (!config) return { width: null as number | null, height: DEFAULT_CHART_HEIGHT }
  const echartsConfig = config.echarts || config
  const metadataSize = config.metadata?.visualSize
  const styleSize = config.styleConfig || config.metadata?.styleConfig
  return {
    width: parseVisualSizeValue(metadataSize?.width ?? styleSize?.width ?? echartsConfig?.width ?? config.width),
    height: parseVisualSizeValue(metadataSize?.height ?? styleSize?.height ?? echartsConfig?.height ?? config.height) ?? DEFAULT_CHART_HEIGHT
  }
})

const chartDisplayStyle = computed(() => {
  const { width, height } = chartVisualSize.value
  const style: Record<string, string | number> = {
    flex: '0 0 auto',
    height: `${height}px`,
    minHeight: `${height}px`
  }
  if (width) {
    style.width = `min(${width}px, calc(100% - 40px))`
    style.alignSelf = 'center'
  }
  return style
})

// 是否为表格类型
const isTableType = computed(() => {
  const chartType = currentChart.value?.chartType
  return chartType === 'table' || chartType === 'summaryTable' || chartType === 'pivotTable'
})

// 表格样式配置
const tableStyleConfig = computed(() => {
  let config: any = {}
  if (currentChartConfig.value) {
    const parsed = currentChartConfig.value
    config = parsed.echarts?.tableStyle || parsed.echarts || {}
  }
  return config
})

// 表格列定义
const tableColumns2 = computed(() => {
  if (chartData.value.length === 0) return []
  const firstRow = chartData.value[0]
  const ts = tableStyleConfig.value
  const columns: any[] = []
  if (ts.showIndex) {
    columns.push({ title: '#', key: '_index', width: 60, align: 'center', render: (_: any, index: number) => index + 1 })
  }
  const displayFields = ts.displayColumns && ts.displayColumns.length > 0 ? ts.displayColumns : Object.keys(firstRow)
  displayFields.forEach((key: string) => {
    if (Object.prototype.hasOwnProperty.call(firstRow, key)) {
      const title = (ts.columnLabels && ts.columnLabels[key]) ? ts.columnLabels[key] : key
      columns.push({
        title, key, ellipsis: { tooltip: true }, resizable: true, sorter: true,
        align: ts.headerAlign || 'left',
        render: (row: any) => {
          const value = row[key]
          if (value === null || value === undefined) return '-'
          return formatCellValueSmart(value, { fieldName: key, fieldTitle: title })
        }
      })
    }
  })
  return columns
})

const tableScrollX = computed(() => {
  const cols = tableColumns2.value
  if (cols.length === 0) return 0
  let totalWidth = 0
  cols.forEach((col: any) => { totalWidth += col.width || 150 })
  return Math.max(totalWidth, 800)
})

const tableHeaderStyle = computed(() => {
  const ts = tableStyleConfig.value
  return {
    '--header-bg-color': ts.headerBgColor || '#f5f7fa',
    '--header-text-color': ts.headerTextColor || '#303133',
    '--header-font-weight': ts.headerFontWeight === 'bold' ? '600' : '400'
  }
})

const tableCurrentPage = ref(1)
const tablePageSize = ref(20)

const showTablePagination = computed(() => {
  const ts = tableStyleConfig.value
  return ts.showPagination !== false && chartData.value.length > 0
})

const paginatedChartData = computed(() => {
  if (!showTablePagination.value) return chartData.value
  const start = (tableCurrentPage.value - 1) * tablePageSize.value
  return chartData.value.slice(start, start + tablePageSize.value)
})

// 导出选项
const detailExportOptions = computed(() => {
  const options: any[] = []
  if (!isTableType.value) {
    options.push({ label: t('chart.exportPng'), key: 'png', icon: () => h(NIcon, null, () => h(ImageOutline)) })
  }
  options.push(
    { label: t('chart.exportExcel'), key: 'excel', icon: () => h(NIcon, null, () => h(DocumentOutline)) },
    { label: t('chart.exportCsv'), key: 'csv', icon: () => h(NIcon, null, () => h(DocumentOutline)) },
    { label: t('chart.exportJson'), key: 'json', icon: () => h(NIcon, null, () => h(CodeOutline)) }
  )
  if (!isTableType.value) {
    options.push({ type: 'divider', key: 'd1' }, { label: t('common.copy'), key: 'copy' })
  }
  return options
})

// 筛选
const searchKeyword = ref('')
const filterCategory = ref<string | null>(null)
const filterChartType = ref<string | null>(null)
const sortBy = ref<string>('updateTime')

// 数据
const chartList = ref<ChartDefinition[]>([])
const chartId = ref<number | undefined>(route.params["id"] ? Number(route.params["id"]) : undefined)
const currentChart = ref<ChartDefinition | null>(null)
const chartData = ref<any[]>([])
const totalCount = ref(0)

// 图表参数
const chartParameters = ref<ChartParameter[]>([])
const parameterValues = reactive<ChartParameterValue>({})
const showParameterPanel = ref(false)

// 日期选择器快捷选项
const datePickerShortcuts = computed(() => ({
  [t('common.today')]: () => Date.now(),
  [t('common.yesterday')]: () => Date.now() - 24 * 60 * 60 * 1000,
  [t('common.lastMonday')]: () => {
    const d = new Date(); const day = d.getDay(); const diff = day === 0 ? 13 : day + 6
    d.setDate(d.getDate() - diff); d.setHours(0, 0, 0, 0); return d.getTime()
  },
  [t('common.thisMonthStart')]: () => { const d = new Date(); d.setDate(1); d.setHours(0, 0, 0, 0); return d.getTime() },
  [t('common.lastMonthStart')]: () => { const d = new Date(); d.setMonth(d.getMonth() - 1); d.setDate(1); d.setHours(0, 0, 0, 0); return d.getTime() },
  [t('common.lastMonthEnd')]: () => { const d = new Date(); d.setDate(0); d.setHours(0, 0, 0, 0); return d.getTime() }
}))

const dateRangeShortcuts = computed(() => ({
  [t('common.today')]: () => { const now = Date.now(); return [now, now] as [number, number] },
  [t('common.yesterday')]: () => { const y = Date.now() - 86400000; return [y, y] as [number, number] },
  [t('common.thisWeek')]: () => {
    const now = new Date(); const day = now.getDay()
    const monday = new Date(now); monday.setDate(now.getDate() - (day === 0 ? 6 : day - 1)); monday.setHours(0, 0, 0, 0)
    const sunday = new Date(monday); sunday.setDate(monday.getDate() + 6); sunday.setHours(23, 59, 59, 999)
    return [monday.getTime(), sunday.getTime()] as [number, number]
  },
  [t('common.lastWeek')]: () => {
    const now = new Date(); const day = now.getDay()
    const lastMonday = new Date(now); lastMonday.setDate(now.getDate() - (day === 0 ? 13 : day + 6)); lastMonday.setHours(0, 0, 0, 0)
    const lastSunday = new Date(lastMonday); lastSunday.setDate(lastMonday.getDate() + 6); lastSunday.setHours(23, 59, 59, 999)
    return [lastMonday.getTime(), lastSunday.getTime()] as [number, number]
  },
  [t('common.thisMonth')]: () => {
    const now = new Date()
    const start = new Date(now.getFullYear(), now.getMonth(), 1)
    const end = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59, 999)
    return [start.getTime(), end.getTime()] as [number, number]
  },
  [t('common.lastMonth')]: () => {
    const now = new Date()
    const start = new Date(now.getFullYear(), now.getMonth() - 1, 1)
    const end = new Date(now.getFullYear(), now.getMonth(), 0, 23, 59, 59, 999)
    return [start.getTime(), end.getTime()] as [number, number]
  },
  [t('common.last7Days')]: () => { const end = Date.now(); return [end - 6 * 86400000, end] as [number, number] },
  [t('common.last30Days')]: () => { const end = Date.now(); return [end - 29 * 86400000, end] as [number, number] }
}))

// 分页
const paginationPage = ref(1)
const paginationPageSize = ref(12)
const paginationItemCount = ref(0)

// 图表实例
const chartRef = ref<HTMLDivElement | null>(null)
const fullscreenChartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null
let fullscreenChartInstance: echarts.ECharts | null = null

// 收藏
const favoritesCache = ref<Set<number>>(new Set())


// 分类列表
const categoryList = computed(() => {
  const categories = new Map<string, { label: string; icon: any; count: number }>()
  CHART_TYPES.forEach(type => {
    if (type.category) {
      if (!categories.has(type.category)) {
        categories.set(type.category, { label: CHART_CATEGORY_LABELS[type.category as ChartCategory] || type.category, icon: getCategoryIcon(type.category), count: 0 })
      }
    }
  })
  chartList.value.forEach(chart => {
    const chartType = CHART_TYPES.find(t => t.value === chart.chartType)
    if (chartType?.category && categories.has(chartType.category)) {
      categories.get(chartType.category)!.count++
    }
  })
  return Array.from(categories.entries()).map(([value, data]) => ({ value, ...data }))
})

const getCategoryIcon = (category: string) => {
  const iconMap: Record<string, any> = {
    basic: BarChartOutline, advanced: StatsChartOutline, special: TrendingUpOutline,
    geographic: AppsOutline, statistical: StatsChartOutline, financial: TrendingUpOutline,
    relationship: GitNetworkOutline
  }
  return iconMap[category] || AppsOutline
}

const getChartTypeTagBg = (chartType?: string) => {
  const bgMap: Record<string, string> = {
    bar: '#eef2ff', line: '#ecfdf5', pie: '#fff1f2', scatter: '#eff6ff',
    radar: '#fef3c7', gauge: '#f0fdf4', funnel: '#faf5ff', heatmap: '#fdf2f8',
    table: '#eff6ff', summaryTable: '#eef2ff', pivotTable: '#f5f3ff'
  }
  return bgMap[chartType || ''] || '#f1f5f9'
}

const formatDate = formatChartDate

const isFavorite = (id?: number) => id ? favoritesCache.value.has(id) : false

const isCanceledRequest = (error: any) => {
  return error?.code === 'ERR_CANCELED' || error?.name === 'CanceledError' || error?.message === 'canceled'
}

// 分类选择
const handleCategorySelect = (value: string | null) => {
  filterCategory.value = value
  handleSearch()
}

// 加载数据
const loadChartList = async () => {
  if (loading.value) return
  loading.value = true
  try {
    const params: any = { page: paginationPage.value, pageSize: paginationPageSize.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterChartType.value) params.chartType = filterChartType.value

    const res = await getChartDefinitionList(params)
    let pageResult: PageResult<ChartDefinition> | null = null
    if (res?.data && typeof res.data === 'object' && 'list' in res.data) {
      pageResult = res.data as unknown as PageResult<ChartDefinition>
    } else if (res && typeof res === 'object' && 'list' in res) {
      pageResult = res as unknown as PageResult<ChartDefinition>
    }

    if (pageResult) {
      let list = (pageResult.list || []).filter(c => c.status === 1)
      if (filterCategory.value) {
        list = list.filter(c => {
          const type = CHART_TYPES.find(t => t.value === c.chartType)
          return type?.category === filterCategory.value
        })
      }
      if (showFavoriteOnly.value) {
        list = list.filter(c => favoritesCache.value.has(c.id))
      }
      list.sort((a, b) => {
        switch (sortBy.value) {
          case 'name': return (a.chartName || '').localeCompare(b.chartName || '')
          case 'createTime': return new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime()
          default: return new Date(b.updateTime || 0).getTime() - new Date(a.updateTime || 0).getTime()
        }
      })
      chartList.value = list
      const hasClientFilter = filterCategory.value || showFavoriteOnly.value
      paginationItemCount.value = hasClientFilter ? list.length : (pageResult.total || 0)
      totalCount.value = pageResult.total || 0
    } else {
      chartList.value = []
      paginationItemCount.value = 0
      totalCount.value = 0
    }
  } catch (error) {
    if (isCanceledRequest(error)) return
    message.error(t('common.operationFailed'))
  } finally {
    loading.value = false
  }
}

const loadChartData = async (id: number) => {
  chartLoading.value = true
  loadDuration.value = 0
  const startTime = Date.now()
  try {
    const chartRes = await getChartDefinitionById(id)
    currentChart.value = (chartRes?.data as unknown as ChartDefinition) || null
    
    if (currentChart.value?.chartConfig) {
      try {
        const config = JSON.parse(currentChart.value.chartConfig)
        const params = config.queryParameters || config.metadata?.chartParameters
        if (params && params.length > 0) {
          chartParameters.value = params
          chartParameters.value.forEach(param => {
            if (parameterValues[param.name] === undefined) {
              parameterValues[param.name] = param.defaultValue ?? null
            }
          })
          showParameterPanel.value = true
        } else {
          chartParameters.value = []
          showParameterPanel.value = false
        }
      } catch (e) {
        chartParameters.value = []
        showParameterPanel.value = false
      }
    }
    
    const effectiveParams: Record<string, any> = {}
    for (const param of chartParameters.value) {
      const value = parameterValues[param.name]
      if (value !== null && value !== undefined && value !== '') {
        effectiveParams[param.name] = { value, field: param.field || param.name, operator: param.operator || '=' }
      }
    }
    const dataRes = await getChartData(id, { limit: 10000, parameters: effectiveParams, useCache: false })
    chartData.value = Array.isArray(dataRes?.data) ? dataRes.data : []
    tableCurrentPage.value = 1
    await nextTick()
    renderChart()
  } catch (error: any) {
    logger.error('Load chart data failed:', error)
    message.error(t('common.operationFailed') + ': ' + (error?.response?.data?.msg || error?.message || t('common.unknownError')))
  } finally {
    chartLoading.value = false
    loadDuration.value = Date.now() - startTime
  }
}

const renderChart = () => {
  if (!chartRef.value || !currentChart.value) return
  if (isTableType.value) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(chartRef.value, currentTheme.value)
  const option = buildChartOption(currentChart.value, chartData.value)
  chartInstance.setOption(option, true)
}

// 事件处理
const handleSearch = () => { paginationPage.value = 1; loadChartList() }

const handleResetFilter = () => {
  searchKeyword.value = ''; filterCategory.value = null; filterChartType.value = null
  showFavoriteOnly.value = false; activeQuickFilter.value = null; handleSearch()
}

const handlePageChange = (page: number) => { paginationPage.value = page; loadChartList() }
const handlePageSizeChange = (size: number) => { paginationPageSize.value = size; paginationPage.value = 1; loadChartList() }

const handleViewChart = (chart: ChartDefinition) => {
  chartId.value = chart.id
  saveToRecentCharts(chart)
  if (!route.params["id"]) {
    tabsStore.replaceTab(route.fullPath, { key: `/chart-center/${chart.id}`, title: chart.chartName || t('chart.title'), closable: true })
    router.replace(`/chart-center/${chart.id}`)
  } else {
    tabsStore.updateTabTitle(route.fullPath, chart.chartName || t('chart.title'))
    router.replace(`/chart-center/${chart.id}`)
  }
  loadChartData(chart.id)
}

const handleBackToList = () => {
  if (chartInstance) { chartInstance.dispose(); chartInstance = null }
  currentChart.value = null; chartParameters.value = []
  Object.keys(parameterValues).forEach(key => delete parameterValues[key])
  showParameterPanel.value = false
  if (fromPage.value === 'chart-manage') {
    fromPage.value = null
    tabsStore.replaceTab(route.fullPath, { key: '/chart-manage', title: t('chart.title'), closable: true })
    router.replace('/chart-manage'); return
  }
  chartId.value = undefined; fromPage.value = null
  tabsStore.replaceTab(route.fullPath, { key: '/chart-center', title: t('chart.title'), closable: true })
  router.replace('/chart-center'); loadChartList()
}

const handleRefreshChart = () => { if (chartId.value) loadChartData(chartId.value) }

// 自动刷新
const autoRefreshInterval = ref(0)
const autoRefreshOptions = computed(() => [
  { label: t('common.disable'), value: 0 }, { label: '30s', value: 30 },
  { label: '1min', value: 60 }, { label: '5min', value: 300 }
])
let autoRefreshTimer: ReturnType<typeof setInterval> | null = null

const handleAutoRefreshChange = (val: number) => {
  if (autoRefreshTimer) { clearInterval(autoRefreshTimer); autoRefreshTimer = null }
  if (val > 0 && chartId.value) {
    autoRefreshTimer = setInterval(() => {
      if (chartId.value && !chartLoading.value) loadChartData(chartId.value)
    }, val * 1000)
    message.success(t('dashboard.autoRefresh') + `: ${val}s`)
  }
}

// 应用参数查询
const handleApplyParameters = async () => {
  for (const param of chartParameters.value) {
    if (param.required) {
      const value = parameterValues[param.name]
      if (value === null || value === undefined || value === '') {
        message.warning(`${t('form.required')}: ${param.label}`); return
      }
    }
  }
  if (chartId.value) {
    chartLoading.value = true
    try {
      const effectiveParams: Record<string, any> = {}
      for (const param of chartParameters.value) {
        const value = parameterValues[param.name]
        if (value !== null && value !== undefined && value !== '') {
          effectiveParams[param.name] = { value, field: param.field || param.name, operator: param.operator || '=' }
        }
      }
      const dataRes = await getChartData(chartId.value, { limit: 10000, parameters: effectiveParams, useCache: false })
      chartData.value = Array.isArray(dataRes?.data) ? dataRes.data : []
      tableCurrentPage.value = 1
      await nextTick(); renderChart(); message.success(t('common.operationSuccess'))
    } catch (error) { message.error(t('common.operationFailed')) }
    finally { chartLoading.value = false }
  }
}

const handleResetParameters = () => {
  Object.keys(parameterValues).forEach(key => delete parameterValues[key])
  chartParameters.value.forEach(param => {
    parameterValues[param.name] = (param.defaultValue !== undefined && param.defaultValue !== null) ? param.defaultValue : null
  })
}

const handleFullscreenQuery = async () => {
  for (const param of chartParameters.value) {
    if (param.required) {
      const value = parameterValues[param.name]
      if (value === null || value === undefined || value === '') {
        message.warning(`${t('form.required')}: ${param.label}`); return
      }
    }
  }
  if (chartId.value) {
    chartLoading.value = true
    try {
      const dataRes = await getChartData(chartId.value, { limit: 10000, parameters: parameterValues })
      chartData.value = Array.isArray(dataRes?.data) ? dataRes.data : []
      tableCurrentPage.value = 1
      if (fullscreenChartRef.value && currentChart.value) {
        if (fullscreenChartInstance) fullscreenChartInstance.dispose()
        fullscreenChartInstance = echarts.init(fullscreenChartRef.value, currentTheme.value)
        fullscreenChartInstance.setOption(buildChartOption(currentChart.value, chartData.value), true)
      }
      message.success(t('common.operationSuccess'))
    } catch (error) { message.error(t('common.operationFailed')) }
    finally { chartLoading.value = false }
  }
}

// 导出处理
const handleExportSelect = async (key: string) => {
  if (!currentChart.value) return
  switch (key) {
    case 'png':
      if (chartInstance) {
        const userStore = useUserStore()
        const wType = currentChart.value.watermarkType || ''
        const wText = currentChart.value.pdfWatermark || ''
        const watermark = resolveWatermarkText(wType, wText, userStore.username, '') || (wText ? wText : '')
        exportToPng(chartInstance, { filename: currentChart.value.chartName, watermark })
        message.success(t('common.operationSuccess'))
      }
      break
    case 'excel':
      if (chartData.value.length > 0) { exportToExcel(chartData.value, { filename: currentChart.value.chartName }); message.success(t('common.operationSuccess')) }
      else { message.warning(t('common.noData')) }
      break
    case 'csv':
      if (chartData.value.length > 0) { exportToCsv(chartData.value, { filename: currentChart.value.chartName }); message.success(t('common.operationSuccess')) }
      else { message.warning(t('common.noData')) }
      break
    case 'json':
      if (chartData.value.length > 0) { exportToJson(chartData.value, { filename: currentChart.value.chartName + '_data' }); message.success(t('common.operationSuccess')) }
      else { message.warning(t('common.noData')) }
      break
    case 'copy':
      if (chartInstance) {
        const success = await copyChartToClipboard(chartInstance)
        message[success ? 'success' : 'error'](success ? t('common.copySuccess') : t('common.copyFailed'))
      }
      break
  }
}

const handleThemeChange = (theme: string) => { currentTheme.value = theme; renderChart() }

const handleClearCache = async () => {
  if (!chartId.value) return
  clearingCache.value = true
  try {
    await clearChartCache(chartId.value); chartCache.clearChart(chartId.value)
    message.success(t('common.operationSuccess')); await loadChartData(chartId.value)
  } catch (error) { message.error(t('common.operationFailed')) }
  finally { clearingCache.value = false }
}

const handleFullscreenChart = () => {
  showFullscreen.value = true
  nextTick(() => {
    if (isTableType.value) return
    if (fullscreenChartRef.value && currentChart.value) {
      if (fullscreenChartInstance) fullscreenChartInstance.dispose()
      fullscreenChartInstance = echarts.init(fullscreenChartRef.value, currentTheme.value)
      fullscreenChartInstance.setOption(buildChartOption(currentChart.value, chartData.value), true)
    }
  })
}

const handleFavoriteChart = (chart: ChartDefinition) => {
  if (favoritesCache.value.has(chart.id)) { favoritesCache.value.delete(chart.id); message.success('已取消收藏') }
  else { favoritesCache.value.add(chart.id); message.success('已收藏') }
  localStorage.setItem('chart_favorites', JSON.stringify([...favoritesCache.value]))
  if (showFavoriteOnly.value) loadChartList()
}

// 监听路由
watch(() => route.params["id"], (newId, oldId) => {
  if (newId === oldId) return
  if (newId) {
    const id = Number(newId)
    if (!isNaN(id)) { chartId.value = id; loadChartData(id) }
  } else if (chartId.value && route.path === '/chart-center') {
    chartId.value = undefined; currentChart.value = null; chartParameters.value = []
    Object.keys(parameterValues).forEach(key => delete parameterValues[key])
    showParameterPanel.value = false
    if (chartInstance) { chartInstance.dispose(); chartInstance = null }
    loadChartList()
  }
})

onMounted(() => {
  try { const favorites = JSON.parse(localStorage.getItem('chart_favorites') || '[]'); favoritesCache.value = new Set(favorites) } catch (_) { /* chart_favorites parse failed */ }
  loadRecentCharts()
  window.addEventListener('resize', handleResize)
  if (route.query["from"] === 'chart-manage') fromPage.value = 'chart-manage'
  if (chartId.value) loadChartData(chartId.value)
  else loadChartList()
})

onActivated(() => {
  const routeId = route.params["id"] ? Number(route.params["id"]) : undefined
  const isValidRouteId = routeId && !isNaN(routeId)
  if (isValidRouteId && routeId !== chartId.value) { chartId.value = routeId; loadChartData(routeId) }
  else if (!isValidRouteId && chartId.value) {
    chartId.value = undefined; currentChart.value = null; chartParameters.value = []
    Object.keys(parameterValues).forEach(key => delete parameterValues[key])
    showParameterPanel.value = false
    if (chartInstance) { chartInstance.dispose(); chartInstance = null }
    loadChartList()
  } else if (!isValidRouteId && !chartId.value && chartList.value.length === 0) { loadChartList() }
})

const handleResize = () => { chartInstance?.resize(); fullscreenChartInstance?.resize() }

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose(); fullscreenChartInstance?.dispose()
  if (autoRefreshTimer) { clearInterval(autoRefreshTimer); autoRefreshTimer = null }
})
</script>

<style scoped>
.chart-center {
  display: flex;
  height: calc(100vh - 178px);
  background: #f0f2f5;
  border-radius: 8px;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 20px 24px;
  min-width: 0;
}

.chart-list {
  flex: 1;
  overflow-y: auto;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  gap: 12px;
}

.empty-illustration {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.empty-title { font-size: 18px; font-weight: 600; color: #334155; }
.empty-desc { font-size: 14px; color: #94a3b8; margin-bottom: 8px; }

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0;
}

/* 图表详情 */
.chart-detail {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.chart-detail > :deep(.n-spin-container) {
  flex: 1; display: flex; flex-direction: column; min-height: 0;
}

.chart-detail :deep(.n-spin-content) {
  flex: 1; display: flex; flex-direction: column; min-height: 0; overflow: auto;
}

/* 导航栏 */
.detail-nav-bar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 20px; border-bottom: 1px solid #f1f5f9; flex-wrap: wrap; gap: 10px; flex-shrink: 0;
}
.nav-left { display: flex; align-items: center; gap: 8px; min-width: 0; }
.back-btn { font-size: 13px; color: #64748b !important; flex-shrink: 0; }
.back-btn:hover { color: #3b82f6 !important; }
.nav-divider { color: #cbd5e1; font-size: 14px; flex-shrink: 0; }
.nav-chart-info { display: flex; align-items: center; gap: 8px; min-width: 0; }
.chart-title-text { margin: 0; font-size: 16px; font-weight: 600; color: #1e293b; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 300px; }
.nav-actions { display: flex; gap: 6px; align-items: center; flex-wrap: wrap; flex-shrink: 0; }

/* 信息栏 */
.detail-info-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 20px; background: #f8fafc; border-bottom: 1px solid #f1f5f9; gap: 16px; flex-shrink: 0; flex-wrap: wrap;
}
.info-bar-left { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.meta-chip { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #64748b; padding: 2px 8px; background: #f1f5f9; border-radius: 6px; }
.info-bar-desc { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #94a3b8; max-width: 400px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* 图表/表格渲染区 */
.chart-container { flex: 1; min-height: 400px; margin: 16px 20px; border-radius: 10px; border: 1px solid #e8eaed; background: #fff; }

.table-wrapper { flex: 1; display: flex; flex-direction: column; margin: 12px 20px 16px; border: 1px solid #e8eaed; border-radius: 10px; overflow: hidden; background: #fff; }
.table-toolbar { display: flex; justify-content: space-between; align-items: center; padding: 10px 16px; background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%); border-bottom: 1px solid #e8eaed; flex-shrink: 0; }
.table-toolbar-left { display: flex; align-items: center; gap: 8px; }
.table-toolbar-title { font-size: 13px; font-weight: 600; color: #334155; }
.table-toolbar-right { display: flex; align-items: center; gap: 8px; }
.table-container { flex: 1; overflow: hidden; min-height: 0; }
.table-pagination { display: flex; justify-content: flex-end; align-items: center; padding: 10px 16px; border-top: 1px solid #e8eaed; background: #fafbfc; flex-shrink: 0; }

/* 自定义表头 */
.custom-header-table :deep(.n-data-table-th) { background-color: var(--header-bg-color, #f8fafc) !important; color: var(--header-text-color, #334155) !important; font-weight: var(--header-font-weight, 600) !important; font-size: 13px; padding: 10px 12px !important; border-bottom: 2px solid #e2e8f0 !important; }
.custom-header-table :deep(.n-data-table-th .n-data-table-th__title) { color: var(--header-text-color, #334155) !important; }
.custom-header-table :deep(.n-data-table-th .n-data-table-sorter) { color: var(--header-text-color, #64748b) !important; }
.custom-header-table :deep(.n-data-table-td) { font-size: 13px; padding: 8px 12px !important; color: #475569; }
.custom-header-table :deep(.n-data-table .n-data-table-tr:hover .n-data-table-td) { background: #f0f7ff !important; }
.custom-header-table :deep(.n-data-table .n-data-table-tr--striped .n-data-table-td) { background: #fafbfc !important; }

/* 参数面板 */
.parameter-panel { background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; margin: 0 20px 12px; overflow: hidden; transition: box-shadow 0.3s; }
.parameter-panel:hover { box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06); }
.parameter-panel.collapsed { box-shadow: none; }
.param-panel-header { display: flex; align-items: center; justify-content: space-between; padding: 10px 16px; background: #f8fafc; border-bottom: 1px solid #e2e8f0; user-select: none; }
.parameter-panel.collapsed .param-panel-header { border-bottom: none; }
.param-panel-title { display: flex; align-items: center; gap: 8px; font-weight: 600; font-size: 13px; color: #334155; }
.param-panel-body { padding: 16px; background: #fff; }
.param-inputs { display: flex; flex-wrap: wrap; gap: 16px; margin-bottom: 12px; }
.param-input-item { display: flex; flex-direction: column; gap: 6px; min-width: 200px; flex: 1 1 auto; max-width: 320px; }
.param-input-item label { font-size: 12px; color: #64748b; font-weight: 500; display: flex; align-items: center; gap: 4px; }
.param-input-item label.required::before { content: '*'; color: #ef4444; font-weight: 600; }
.param-input-item :deep(.n-input), .param-input-item :deep(.n-input-number), .param-input-item :deep(.n-select), .param-input-item :deep(.n-date-picker) { min-width: 180px; width: 100%; }
.param-actions { display: flex; justify-content: flex-end; align-items: center; gap: 8px; padding-top: 12px; border-top: 1px solid #f1f5f9; }

/* 全屏模式 */
.fullscreen-param-bar { padding: 10px 16px; background: #f8fafc; border-radius: 8px; margin-bottom: 12px; border: 1px solid #e2e8f0; }
.fullscreen-param-item { display: flex; align-items: center; gap: 6px; }
.fullscreen-param-item .param-label { font-size: 13px; color: #475569; white-space: nowrap; }
.fullscreen-table-wrapper { display: flex; flex-direction: column; overflow: hidden; }
.fullscreen-table-container { flex: 1; padding: 16px; overflow: hidden; min-height: 0; }

/* 响应式 */
@media (max-width: 768px) {
  .detail-nav-bar { flex-direction: column; align-items: flex-start; }
  .nav-actions { width: 100%; overflow-x: auto; flex-wrap: nowrap; }
  .chart-title-text { max-width: 180px; font-size: 14px; }
}

.is-mobile { padding: 0; }
.is-mobile .main-content { padding: 12px; }


</style>

<style>
/* ChartCenter 深色模式（非 scoped，避免被 Vue SFC 编译器丢弃） */
html.dark .chart-center-page { background: #0f172a !important; }
html.dark .toolbar-section { background: #1e293b !important; }
html.dark .search-title { color: #e2e8f0 !important; }
html.dark .search-desc { color: #94a3b8 !important; }
html.dark .category-sidebar { background: #1e293b !important; }
html.dark .category-item { color: #94a3b8 !important; }
html.dark .category-item:hover { background: #243044 !important; color: #e2e8f0 !important; }
html.dark .category-item.active { background: #243044 !important; color: #60a5fa !important; }
html.dark .chart-grid-item { background: #1e293b !important; border-color: #334155 !important; }
html.dark .chart-grid-item:hover { border-color: var(--color-primary) !important; }
html.dark .chart-name { color: #e2e8f0 !important; }
html.dark .chart-desc { color: #64748b !important; }
html.dark .chart-meta { color: #475569 !important; }
html.dark .chart-thumb { background: #1a2536 !important; }
html.dark .empty-placeholder { color: #64748b !important; }
html.dark .main-content { background: #1e293b !important; border-color: #334155 !important; }
html.dark .chart-container { background: #1e293b !important; border-color: #334155 !important; }
html.dark .table-wrapper { background: #1e293b !important; border-color: #334155 !important; }
html.dark .parameter-panel { background: #1e293b !important; border-color: #334155 !important; }
html.dark .param-panel-header { background: #243044 !important; border-color: #334155 !important; }
html.dark .param-panel-body { background: #1e293b !important; }
html.dark .param-panel-title { color: #e2e8f0 !important; }
html.dark .table-toolbar { background: #243044 !important; border-color: #334155 !important; }
html.dark .table-toolbar-title { color: #e2e8f0 !important; }
html.dark .info-bar { background: #1e293b !important; border-color: #334155 !important; }
html.dark .info-bar-title { color: #e2e8f0 !important; }
html.dark .info-bar-desc { color: #94a3b8 !important; }
html.dark .custom-header-table .n-data-table .n-data-table-tr--striped .n-data-table-td { background: #1a2536 !important; }
</style>
