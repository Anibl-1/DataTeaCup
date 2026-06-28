<template>
  <div ref="chartContainerRef" class="combo-chart-container">
    <div v-if="loading" class="loading-overlay">
      <n-spin size="large" />
    </div>
    <div v-if="error" class="error-overlay">
      <n-result status="error" :title="error" size="small">
        <template #footer>
          <n-button size="small" @click="handleRetry">重试</n-button>
        </template>
      </n-result>
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import echarts from '@/utils/echarts'
import { NSpin, NResult, NButton } from 'naive-ui'
import {
  buildComboOption,
  type ComboDataItem,
  type ComboSeriesConfig,
  type ComboYAxisConfig,
  type ComboChartOptions
} from '@/utils/chartCombo'

/**
 * 组合图 Props
 */
export interface ComboChartProps {
  /** 图表数据 */
  data: ComboDataItem[]
  /** 系列配置 */
  seriesConfig: ComboSeriesConfig[]
  /** 图表标题 */
  title?: string
  /** 左 Y 轴标签 */
  leftAxisLabel?: string
  /** 右 Y 轴标签 */
  rightAxisLabel?: string
  /** 左 Y 轴配置 */
  leftYAxis?: ComboYAxisConfig
  /** 右 Y 轴配置 */
  rightYAxis?: ComboYAxisConfig
  /** 是否显示图例 */
  showLegend?: boolean
  /** 图例位置 */
  legendPosition?: 'top' | 'bottom' | 'left' | 'right'
  /** 是否显示工具箱 */
  showToolbox?: boolean
  /** 是否显示数据缩放 */
  showDataZoom?: boolean
  /** X 轴标签旋转角度 */
  xAxisLabelRotate?: number
  /** 颜色数组 */
  colors?: string[]
}

const props = withDefaults(defineProps<ComboChartProps>(), {
  data: () => [],
  seriesConfig: () => [],
  showLegend: true,
  legendPosition: 'top',
  showToolbox: false,
  showDataZoom: false,
  xAxisLabelRotate: 0,
  colors: () => ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']
})

const emit = defineEmits<{
  /** 数据点点击事件 */
  (e: 'data-click', params: { seriesName: string; name: string; value: number; dataIndex: number }): void
  /** 图表就绪事件 */
  (e: 'chart-ready', instance: echarts.ECharts): void
}>()

// Refs
const chartContainerRef = ref<HTMLElement | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

// Chart instance
let chartInstance: echarts.ECharts | null = null

/**
 * 初始化图表
 */
const initChart = () => {
  if (!chartContainerRef.value) return

  if (chartInstance) {
    chartInstance.dispose()
  }

  chartInstance = echarts.init(chartContainerRef.value)

  // 绑定点击事件
  chartInstance.on('click', handleChartClick)

  // 绑定窗口大小变化
  window.addEventListener('resize', handleResize)

  emit('chart-ready', chartInstance)
}

/**
 * 渲染图表
 */
const renderChart = () => {
  if (!chartInstance) {
    initChart()
  }

  if (!chartInstance) return

  loading.value = true
  error.value = null

  try {
    const chartOptions: ComboChartOptions = {
      title: props.title,
      leftYAxis: {
        name: props.leftAxisLabel,
        ...props.leftYAxis
      },
      rightYAxis: {
        name: props.rightAxisLabel,
        ...props.rightYAxis
      },
      showLegend: props.showLegend,
      legendPosition: props.legendPosition,
      showToolbox: props.showToolbox,
      showDataZoom: props.showDataZoom,
      xAxisLabelRotate: props.xAxisLabelRotate,
      colors: props.colors
    }

    const option = buildComboOption(props.data, props.seriesConfig, chartOptions)
    chartInstance.setOption(option, true)
  } catch (err: any) {
    error.value = err.message || '渲染图表失败'
    console.error('Failed to render combo chart:', err)
  } finally {
    loading.value = false
  }
}

/**
 * 处理图表点击事件
 */
const handleChartClick = (params: any) => {
  if (!params.name) return

  emit('data-click', {
    seriesName: params.seriesName,
    name: params.name,
    value: params.value ?? 0,
    dataIndex: params.dataIndex
  })
}

/**
 * 处理重试
 */
const handleRetry = () => {
  renderChart()
}

/**
 * 处理窗口大小变化
 */
const handleResize = () => {
  nextTick(() => {
    chartInstance?.resize()
  })
}

/**
 * 刷新图表
 */
const refresh = () => {
  renderChart()
}

/**
 * 获取图表实例
 */
const getChartInstance = () => chartInstance

// Watch props changes
watch(
  () => [props.data, props.seriesConfig, props.title, props.leftAxisLabel, props.rightAxisLabel],
  () => {
    renderChart()
  },
  { deep: true }
)

// Lifecycle
onMounted(() => {
  nextTick(() => {
    initChart()
    renderChart()
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)

  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})

// Expose methods
defineExpose({
  refresh,
  getChartInstance,
  resize: handleResize
})
</script>

<style scoped>
.combo-chart-container {
  width: 100%;
  height: 100%;
  min-height: 300px;
  position: relative;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.8);
  z-index: 10;
}

.error-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.9);
  z-index: 10;
}
</style>
