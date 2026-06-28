<template>
  <div class="dynamic-chart-container">
    <!-- 页面头部统计 -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><BarChartOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ chartDefinition?.chartType || '-' }}</span>
          <span class="stat-label">图表类型</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><GridOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ chartData.length }}</span>
          <span class="stat-label">数据条数</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="24"><FunnelOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ chartParameters.length }}</span>
          <span class="stat-label">查询参数</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-info">
          <n-icon size="24"><TimeOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ lastUpdateTime || '-' }}</span>
          <span class="stat-label">更新时间</span>
        </div>
      </div>
    </div>

    <n-card class="main-card" :bordered="false">
      <template #header>
        <div class="chart-header">
          <div class="chart-title">
            <n-icon size="24" color="var(--color-primary)"><StatsChartOutline /></n-icon>
            <span>{{ chartDefinition?.chartName || '图表查看' }}</span>
          </div>
          <n-tag v-if="chartDefinition" type="info" size="small">
            {{ chartDefinition.chartCode }}
          </n-tag>
        </div>
      </template>
      <template #header-extra>
        <n-space :size="6">
          <n-button-group size="small">
            <n-button :type="viewMode === 'chart' ? 'primary' : 'default'" :disabled="isTableType" @click="viewMode = 'chart'">
              <template #icon><n-icon size="14"><StatsChartOutline /></n-icon></template>
            </n-button>
            <n-button :type="viewMode === 'table' ? 'primary' : 'default'" @click="viewMode = 'table'">
              <template #icon><n-icon size="14"><GridOutline /></n-icon></template>
            </n-button>
          </n-button-group>
          <n-button size="small" :loading="loading" @click="handleRefresh">
            <template #icon><n-icon size="14"><RefreshOutline /></n-icon></template>
            刷新
          </n-button>
          <n-button size="small" @click="handleExportCsv">
            <template #icon><n-icon size="14"><DownloadOutline /></n-icon></template>
            CSV
          </n-button>
          <n-button size="small" type="primary" :disabled="isTableType && viewMode === 'table'" @click="handleExportImage">
            <template #icon><n-icon size="14"><ImageOutline /></n-icon></template>
            图片
          </n-button>
          <n-button size="small" quaternary @click="toggleFullscreen">
            <template #icon><n-icon size="16"><ExpandOutline /></n-icon></template>
          </n-button>
        </n-space>
      </template>

      <!-- 参数输入区域 -->
      <div v-if="chartParameters.length > 0" class="param-panel">
        <n-form :model="paramValues" inline label-placement="left" size="small">
          <n-form-item v-for="param in chartParameters" :key="param.name" :label="param.label || param.name">
            <!-- 文本输入 -->
            <n-input
              v-if="param.type === 'text' || !param.type"
              v-model:value="paramValues[param.name]"
              :placeholder="'请输入' + (param.label || param.name)"
              clearable
              style="width: 220px; min-width: 220px"
            />
            <!-- 数字输入 -->
            <n-input-number
              v-else-if="param.type === 'number'"
              v-model:value="paramValues[param.name]"
              :placeholder="'请输入' + (param.label || param.name)"
              clearable
              style="width: 200px; min-width: 200px"
            />
            <!-- 日期选择 -->
            <n-date-picker
              v-else-if="param.type === 'date'"
              v-model:formatted-value="paramValues[param.name]"
              type="date"
              value-format="yyyy-MM-dd"
              clearable
              style="width: 220px; min-width: 220px"
            />
            <!-- 日期范围 -->
            <n-date-picker
              v-else-if="param.type === 'dateRange'"
              v-model:formatted-value="paramValues[param.name]"
              type="daterange"
              value-format="yyyy-MM-dd"
              clearable
              style="width: 320px; min-width: 320px"
            />
            <!-- 下拉选择 -->
            <n-select
              v-else-if="param.type === 'select'"
              v-model:value="paramValues[param.name]"
              :options="param.options || []"
              :placeholder="'请选择' + (param.label || param.name)"
              clearable
              style="width: 220px; min-width: 220px"
            />
            <!-- 默认文本 -->
            <n-input
              v-else
              v-model:value="paramValues[param.name]"
              :placeholder="'请输入' + (param.label || param.name)"
              clearable
              style="width: 220px; min-width: 220px"
            />
          </n-form-item>
          <n-form-item>
            <n-button type="primary" @click="handleQuery">
              <template #icon><n-icon><SearchOutline /></n-icon></template>
              查询
            </n-button>
          </n-form-item>
        </n-form>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="chart-loading">
        <n-spin size="large" />
        <p>加载中...</p>
      </div>

      <!-- 错误提示 -->
      <div v-else-if="errorMsg" class="chart-error">
        <n-result status="error" :title="errorMsg" />
      </div>

      <!-- 数据表格视图（表格类型或用户切换） -->
      <div v-else-if="isTableType || viewMode === 'table'" class="chart-table-wrapper">
        <div class="table-summary">
          <n-tag size="small" :bordered="false">{{ chartData.length }} 条记录</n-tag>
          <n-tag v-if="tableColumns.length" size="small" :bordered="false" type="info">{{ tableColumns.length }} 个字段</n-tag>
        </div>
        <n-data-table
          :columns="tableColumns"
          :data="tableRows"
          :row-key="getTableRowKey"
          :bordered="true"
          :single-line="false"
          striped
          size="small"
          :max-height="isFullscreen ? 'calc(100vh - 280px)' : '500px'"
          virtual-scroll
        />
      </div>

      <!-- ECharts图表 -->
      <div v-else ref="chartRef" :class="['chart-content', isFullscreen ? 'chart-fullscreen' : '']"></div>
    </n-card>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, onMounted, onBeforeUnmount, nextTick, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  NCard, NButton, NIcon, NSpace, NSpin, NResult, NTag, NForm, NFormItem,
  NInput, NInputNumber, NDatePicker, NSelect, NDataTable, useMessage
} from 'naive-ui'
import {
  BarChartOutline, GridOutline, FunnelOutline, TimeOutline,
  StatsChartOutline, RefreshOutline, ImageOutline, SearchOutline,
  DownloadOutline, ExpandOutline
} from '@vicons/ionicons5'
import echarts from '@/utils/echarts'
import { getChartDefinitionById, getChartData } from '@/api/chart'
import { buildChartOption } from '@/utils/chartRenderer'
import { registerThemes } from '@/utils/chartThemes'
import { formatCellValueSmart } from '@/utils/format'
import { resolveWatermarkText } from '@/utils/chartExport'
import { useUserStore } from '@/stores/user'
import type { ChartDefinition } from '@/types/chart'

registerThemes()

const route = useRoute()
const message = useMessage()

const loading = ref(true)
const errorMsg = ref('')
const chartDefinition = ref<ChartDefinition | null>(null)
const chartData = ref<any[]>([])
const chartRef = ref<HTMLDivElement | null>(null)
const chartParameters = ref<any[]>([])
const paramValues = ref<Record<string, any>>({})
const lastUpdateTime = ref('')
let chartInstance: echarts.ECharts | null = null
const viewMode = ref<'chart' | 'table'>('chart')
const isFullscreen = ref(false)

// 图表ID（支持路由参数和props）
const chartId = computed(() => {
  const id = route.params["id"] || route.query["id"]
  return id ? Number(id) : null
})

// 是否为表格类型
const isTableType = computed(() => {
  const t = chartDefinition.value?.chartType
  return t === 'table' || t === 'pivot_table' || t === 'summaryTable'
})

// 表格列定义
const tableColumns = computed(() => {
  if (!chartData.value.length) return []
  const keys = Object.keys(chartData.value[0])
  return keys.map(key => ({
    title: key,
    key,
    ellipsis: { tooltip: true },
    render: (row: any) => formatCellValueSmart ? formatCellValueSmart(row[key]) : String(row[key] ?? '')
  }))
})

const tableRows = computed(() => chartData.value.map((row, index) => ({
  ...row,
  __dpRowKey: row.id ?? row.ID ?? row._id ?? index
})))

const getTableRowKey = (row: any) => row.__dpRowKey

// 加载图表定义
const loadChartDefinition = async () => {
  if (!chartId.value) {
    errorMsg.value = '无效的图表ID'
    loading.value = false
    return
  }

  try {
    const res = await getChartDefinitionById(chartId.value)
    chartDefinition.value = res?.data as ChartDefinition

    if (!chartDefinition.value) {
      errorMsg.value = '图表不存在'
      return
    }

    // 解析查询参数
    parseChartParameters()

    // 加载数据
    await loadChartData()
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 403 ? '无权访问该图表' : '加载图表失败'
  } finally {
    loading.value = false
  }
}

// 解析图表参数
const parseChartParameters = () => {
  if (!chartDefinition.value?.chartConfig) return

  try {
    const config = JSON.parse(chartDefinition.value.chartConfig)
    const params = config.metadata?.chartParameters || config.queryParameters || []
    chartParameters.value = params

    // 初始化参数默认值
    params.forEach((param: any) => {
      if (param.defaultValue !== undefined) {
        paramValues.value[param.name] = param.defaultValue
      }
    })
  } catch (e) {
    console.warn('解析图表参数失败', e)
  }
}

// 加载图表数据
const loadChartData = async () => {
  if (!chartId.value) return

  loading.value = true
  try {
    // 构建有效参数
    const effectiveParams: Record<string, any> = {}
    for (const param of chartParameters.value) {
      const value = paramValues.value[param.name]
      if (value !== null && value !== undefined && value !== '') {
        effectiveParams[param.name] = {
          value: value,
          field: param.field || param.name,
          operator: param.operator || '='
        }
      }
    }

    const params = Object.keys(effectiveParams).length > 0 ? effectiveParams : {}
    const res = await getChartData(chartId.value, {
      limit: 10000,
      parameters: params,
      useCache: false
    })
    chartData.value = Array.isArray(res?.data) ? res.data : []
    lastUpdateTime.value = new Date().toLocaleTimeString()

    // 先设置loading为false，让图表容器渲染出来
    loading.value = false

    if (!isTableType.value) {
      // 等待DOM更新后再渲染图表
      await nextTick()
      await nextTick() // 双重nextTick确保v-else条件生效
      renderChart()
    }
  } catch (e: any) {
    message.error('加载数据失败: ' + (e?.message || '未知错误'))
    loading.value = false
  }
}

// 渲染图表（带重试逻辑）
let renderRetryCount = 0
const maxRenderRetries = 10

const renderChart = () => {
  // 检查chartDefinition是否存在
  if (!chartDefinition.value) {
    console.warn('[DynamicChart] chartDefinition not ready')
    return
  }

  // 检查chartRef是否存在（可能因为v-else条件还没生效）
  if (!chartRef.value) {
    if (renderRetryCount < maxRenderRetries) {
      renderRetryCount++
      setTimeout(() => renderChart(), 100)
      return
    }
    console.error('[DynamicChart] chartRef not available after max retries')
    return
  }

  // 确保容器有尺寸
  if (chartRef.value.offsetWidth === 0 || chartRef.value.offsetHeight === 0) {
    if (renderRetryCount < maxRenderRetries) {
      renderRetryCount++
      setTimeout(() => renderChart(), 100)
      return
    }
    console.error('[DynamicChart] chart container has no size after max retries')
    return
  }

  // 重置重试计数
  renderRetryCount = 0

  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(chartRef.value)

  // 构建图表配置
  let option = buildChartOption(chartDefinition.value, chartData.value)
  // 如果配置为空或无效，使用fallback
  if (!option || Object.keys(option).length === 0 || option._isTable) {
    console.warn('[DynamicChart] option is empty or invalid, using fallback')
    const keys = Object.keys(chartData.value[0] || {})
    if (keys.length >= 2) {
      option = {
        tooltip: { trigger: 'axis' },
        xAxis: {
          type: 'category',
          data: chartData.value.map(row => String(row[keys[0] as string] || ''))
        },
        yAxis: { type: 'value' },
        series: [{
          type: chartDefinition.value.chartType || 'bar',
          data: chartData.value.map(row => Number(row[keys[1] as string]) || 0)
        }]
      }
    }
  }
  
  if (option && !option._isTable) {
    chartInstance.setOption(option, true)
  }
}

// 刷新
const handleRefresh = () => {
  loadChartData()
}

// 查询
const handleQuery = () => {
  loadChartData()
}

// 获取水印文字（支持水印类型：none / user_ip / custom）
const getWatermarkText = (): string => {
  if (!chartDefinition.value) return ''
  
  const userStore = useUserStore()
  
  // 获取水印类型和自定义文本
  let watermarkType = chartDefinition.value.watermarkType || ''
  let customText = chartDefinition.value.pdfWatermark || ''
  
  // 兼容旧数据：从chartConfig中读取
  if (!watermarkType && chartDefinition.value.chartConfig) {
    try {
      const config = JSON.parse(chartDefinition.value.chartConfig)
      watermarkType = config.watermarkType || config.exportConfig?.watermarkType || ''
      customText = customText || config.pdfWatermark || config.exportConfig?.pdfWatermark || ''
    } catch { /* ignore */ }
  }
  
  // 兼容旧数据：如果没有watermarkType但有pdfWatermark文本，视为custom类型
  if (!watermarkType && customText) {
    watermarkType = 'custom'
  }
  
  return resolveWatermarkText(watermarkType, customText, userStore.username, '')
}

// 导出图片
const handleExportImage = () => {
  if (!chartInstance) {
    message.warning('图表尚未加载')
    return
  }

  const url = chartInstance.getDataURL({
    type: 'png',
    pixelRatio: 2,
    backgroundColor: '#fff'
  })

  const watermark = getWatermarkText()
  if (watermark) {
    // 有水印：用canvas叠加水印后导出
    const img = new Image()
    img.onload = () => {
      const canvas = document.createElement('canvas')
      canvas.width = img.width
      canvas.height = img.height
      const ctx = canvas.getContext('2d')!
      ctx.drawImage(img, 0, 0)
      
      // 绘制水印
      ctx.globalAlpha = 0.15
      ctx.font = '20px sans-serif'
      ctx.fillStyle = '#000'
      ctx.rotate(-20 * Math.PI / 180)
      for (let y = -canvas.height; y < canvas.height * 2; y += 80) {
        for (let x = -canvas.width; x < canvas.width * 2; x += 200) {
          ctx.fillText(watermark, x, y)
        }
      }
      
      const link = document.createElement('a')
      link.download = `${chartDefinition.value?.chartName || 'chart'}.png`
      link.href = canvas.toDataURL('image/png')
      link.click()
      message.success('图片导出成功')
    }
    img.src = url
  } else {
    const link = document.createElement('a')
    link.download = `${chartDefinition.value?.chartName || 'chart'}.png`
    link.href = url
    link.click()
    message.success('图片导出成功')
  }
}

// CSV导出
const handleExportCsv = () => {
  if (chartData.value.length === 0) { message.warning('暂无数据可导出'); return }
  const keys = Object.keys(chartData.value[0])
  let csv = keys.join(',') + '\n'
  chartData.value.forEach(row => {
    csv += keys.map(k => {
      const val = String(row[k] ?? '')
      return val.includes(',') || val.includes('"') || val.includes('\n') ? `"${val.replace(/"/g, '""')}"` : val
    }).join(',') + '\n'
  })
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `${chartDefinition.value?.chartName || 'chart_data'}.csv`
  link.click()
  message.success('CSV 导出成功')
}

// 全屏切换
const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
  nextTick(() => chartInstance?.resize())
}

// 窗口大小变化
const handleResize = () => {
  chartInstance?.resize()
}

// 监听路由变化
watch(chartId, () => {
  if (chartId.value) {
    loading.value = true
    errorMsg.value = ''
    loadChartDefinition()
  }
})

onMounted(() => {
  window.addEventListener('resize', handleResize)
  loadChartDefinition()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
/* 使用全局 page-common.css 统一样式 */

.dynamic-chart-container {
  padding: 20px;
  background: var(--bg-secondary, #f5f7fa);
  min-height: 100vh;
}

.chart-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chart-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary, #333);
}

.param-panel {
  padding: 16px;
  background: var(--bg-tertiary, #f9fafb);
  border-radius: 8px;
  margin-bottom: 16px;
}

.chart-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  gap: 16px;
  color: var(--text-secondary, #666);
}

.chart-error {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 400px;
}

.chart-content {
  height: 500px;
  width: 100%;
  transition: height 0.3s ease;
}

.chart-content.chart-fullscreen {
  height: calc(100vh - 280px);
}

.chart-table-wrapper {
  overflow: auto;
}

.table-summary {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .page-header-stats {
    flex-direction: row !important;
    overflow-x: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;
    gap: 8px;
  }
  .page-header-stats::-webkit-scrollbar { display: none; }
  .stat-item { min-width: 110px; flex-shrink: 0; }
  .chart-content { height: 60vh; }
  .chart-content.chart-fullscreen { height: calc(100vh - 200px); }
  .chart-loading { height: 250px; }
  .chart-error { height: 250px; }
  .param-panel { padding: 10px; }
  .chart-table-wrapper { overflow-x: auto; -webkit-overflow-scrolling: touch; }
  .chart-header { flex-direction: column; gap: 8px; }
  .chart-title { font-size: 15px; }
  .main-card { border-radius: 12px !important; }
}
</style>
