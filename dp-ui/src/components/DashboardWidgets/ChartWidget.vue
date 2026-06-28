<template>
  <WidgetWrapper
    :title="config?.title || '图表'"
    :selected="selected"
    :loading="loading"
    :error="error"
    :readonly="readonly"
    :show-header="showHeader"
    @click="$emit('select')"
    @refresh="handleRefresh"
    @remove="$emit('remove')"
    @resize="handleResize"
  >
    <div ref="chartContainerRef" class="chart-container"></div>
  </WidgetWrapper>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import echarts from '@/utils/echarts'
import WidgetWrapper from './WidgetWrapper.vue'
import type { ChartWidgetConfig } from '@/types/dashboard'
import { getChartData, getChartDefinitionById, getChartDefinitionByCode } from '@/api/chart'

const props = withDefaults(defineProps<{
  config: ChartWidgetConfig
  selected?: boolean
  readonly?: boolean
  showHeader?: boolean
  filters?: Record<string, any>
}>(), {
  selected: false,
  readonly: false,
  showHeader: true,
  filters: () => ({})
})

const emit = defineEmits<{
  (e: 'select'): void
  (e: 'remove'): void
  (e: 'chart-click', params: { dimensionValue: any; seriesName: string }): void
}>()

const chartContainerRef = ref<HTMLElement | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

let chartInstance: echarts.ECharts | null = null
let refreshTimer: ReturnType<typeof setInterval> | null = null

// Initialize chart
const initChart = () => {
  if (!chartContainerRef.value) return
  
  if (chartInstance) {
    chartInstance.dispose()
  }
  
  chartInstance = echarts.init(chartContainerRef.value)
  
  // Handle chart click events for linking
  chartInstance.on('click', (params: any) => {
    if (params.data !== undefined) {
      emit('chart-click', {
        dimensionValue: params.name || params.data?.name || params.value,
        seriesName: params.seriesName || ''
      })
    }
  })
}

// Load chart data
const loadChartData = async () => {
  if (!props.config?.chartId && !props.config?.chartCode) {
    // Show placeholder chart
    renderPlaceholderChart()
    return
  }
  
  loading.value = true
  error.value = null
  
  try {
    let chartId = props.config.chartId
    
    // If only chartCode is provided, get the chart definition first
    if (!chartId && props.config.chartCode) {
      const defRes = await getChartDefinitionByCode(props.config.chartCode)
      chartId = defRes.data?.data?.id || defRes.data?.id
    }
    
    if (!chartId) {
      renderPlaceholderChart()
      return
    }
    
    // Get chart data with filters
    const res = await getChartData(chartId, {
      filters: props.filters ? JSON.stringify(props.filters) : undefined
    })
    
    const chartData = res.data?.data || res.data
    
    // Get chart definition for config
    const defRes = await getChartDefinitionById(chartId)
    const chartDef = defRes.data?.data || defRes.data
    
    if (chartData && chartDef) {
      // Parse chart config
      let chartConfig: any = {}
      if (chartDef.chartConfig) {
        try {
          chartConfig = typeof chartDef.chartConfig === 'string' 
            ? JSON.parse(chartDef.chartConfig) 
            : chartDef.chartConfig
        } catch (e) {
          console.warn('Failed to parse chart config:', e)
        }
      }
      
      // Build option from data and config
      const option = buildChartOption(chartData, {
        chartType: chartDef.chartType,
        xField: chartConfig.dataMapping?.xField || 'name',
        yField: chartConfig.dataMapping?.yField || 'value'
      })
      renderChart(option)
    } else {
      renderPlaceholderChart()
    }
  } catch (err: any) {
    error.value = err.message || '加载图表数据失败'
    console.error('Failed to load chart data:', err)
  } finally {
    loading.value = false
  }
}

// Render chart with option
const renderChart = (option: echarts.EChartsOption) => {
  if (!chartInstance) {
    initChart()
  }
  
  if (chartInstance) {
    chartInstance.setOption(option, true)
  }
}

// Render placeholder chart for design mode
const renderPlaceholderChart = () => {
  if (!chartInstance) {
    initChart()
  }
  
  if (chartInstance) {
    chartInstance.setOption({
      title: {
        text: props.config?.title || '图表',
        left: 'center',
        top: 10,
        textStyle: { fontSize: 14, color: '#666' }
      },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
      },
      yAxis: { type: 'value' },
      series: [{
        type: 'bar',
        data: [120, 200, 150, 80, 70, 110, 130],
        itemStyle: { color: '#1890ff' }
      }]
    }, true)
  }
}

// Build chart option from data and config
const buildChartOption = (data: any[], config: any): echarts.EChartsOption => {
  const chartType = config.chartType || 'bar'
  const xField = config.xField || 'name'
  const yField = config.yField || 'value'
  
  const baseOption: echarts.EChartsOption = {
    tooltip: { trigger: chartType === 'pie' ? 'item' : 'axis' },
    grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true }
  }
  
  if (chartType === 'pie') {
    return {
      ...baseOption,
      legend: { bottom: '5%', left: 'center' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        data: data.map(row => ({
          name: String(row[xField] ?? ''),
          value: Number(row[yField]) || 0
        }))
      }]
    }
  }
  
  return {
    ...baseOption,
    xAxis: {
      type: 'category',
      data: data.map(row => String(row[xField] ?? ''))
    },
    yAxis: { type: 'value' },
    series: [{
      type: chartType as any,
      smooth: chartType === 'line',
      data: data.map(row => Number(row[yField]) || 0)
    }]
  }
}

// Handle refresh
const handleRefresh = () => {
  loadChartData()
}

// Handle resize
const handleResize = () => {
  nextTick(() => {
    chartInstance?.resize()
  })
}

// Setup auto refresh
const setupAutoRefresh = () => {
  clearAutoRefresh()
  
  const interval = props.config?.refreshInterval
  if (interval && interval > 0) {
    refreshTimer = setInterval(() => {
      loadChartData()
    }, interval * 1000)
  }
}

const clearAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

// Watch config changes
watch(() => props.config, () => {
  loadChartData()
  setupAutoRefresh()
}, { deep: true })

// Watch filters changes
watch(() => props.filters, () => {
  loadChartData()
}, { deep: true })

// Lifecycle
onMounted(() => {
  nextTick(() => {
    initChart()
    loadChartData()
    setupAutoRefresh()
  })
  
  // Handle window resize
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  clearAutoRefresh()
  
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
  
  window.removeEventListener('resize', handleResize)
})

// Expose methods
defineExpose({
  refresh: handleRefresh,
  resize: handleResize,
  getChartInstance: () => chartInstance
})
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 100%;
  min-height: 150px;
}
</style>
