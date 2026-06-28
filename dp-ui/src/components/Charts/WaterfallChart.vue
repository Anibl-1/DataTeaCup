<template>
  <div ref="chartContainerRef" class="waterfall-chart-container">
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
  buildWaterfallOption,
  type WaterfallDataItem,
  type WaterfallChartOptions
} from '@/utils/chartWaterfall'

/**
 * 瀑布图 Props
 */
export interface WaterfallChartProps {
  /** 图表数据 */
  data: WaterfallDataItem[]
  /** 图表标题 */
  title?: string
  /** 是否显示总计 */
  showTotal?: boolean
  /** 总计标签 */
  totalLabel?: string
  /** 起始值 */
  startValue?: number
  /** 起始标签 */
  startLabel?: string
  /** 正值颜色 */
  positiveColor?: string
  /** 负值颜色 */
  negativeColor?: string
  /** 总计颜色 */
  totalColor?: string
  /** 是否显示标签 */
  showLabel?: boolean
  /** 标签位置 */
  labelPosition?: 'top' | 'inside' | 'bottom'
  /** 柱子宽度 */
  barWidth?: number | string
}

const props = withDefaults(defineProps<WaterfallChartProps>(), {
  data: () => [],
  showTotal: true,
  totalLabel: '总计',
  startValue: 0,
  startLabel: '起始',
  positiveColor: '#52c41a',
  negativeColor: '#ff4d4f',
  totalColor: '#1890ff',
  showLabel: true,
  labelPosition: 'top',
  barWidth: '40%'
})

const emit = defineEmits<{
  /** 柱子点击事件 */
  (e: 'bar-click', params: { name: string; value: number; dataIndex: number }): void
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
    const chartOptions: WaterfallChartOptions = {
      title: props.title,
      showTotal: props.showTotal,
      totalLabel: props.totalLabel,
      startValue: props.startValue,
      startLabel: props.startLabel,
      positiveColor: props.positiveColor,
      negativeColor: props.negativeColor,
      totalColor: props.totalColor,
      showLabel: props.showLabel,
      labelPosition: props.labelPosition,
      barWidth: props.barWidth
    }

    const option = buildWaterfallOption(props.data, chartOptions)
    chartInstance.setOption(option, true)
  } catch (err: any) {
    error.value = err.message || '渲染图表失败'
    console.error('Failed to render waterfall chart:', err)
  } finally {
    loading.value = false
  }
}

/**
 * 处理图表点击事件
 */
const handleChartClick = (params: any) => {
  if (!params.name) return

  emit('bar-click', {
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
  () => [props.data, props.title, props.showTotal, props.startValue],
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
.waterfall-chart-container {
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
