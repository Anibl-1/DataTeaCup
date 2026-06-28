<template>
  <div ref="chartContainerRef" class="wordcloud-chart-container">
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
import 'echarts-wordcloud'
import { NSpin, NResult, NButton } from 'naive-ui'
import {
  buildWordCloudOption,
  type WordCloudDataItem,
  type WordCloudShape,
  type WordCloudChartOptions
} from '@/utils/chartWordCloud'

/**
 * 词云图 Props
 */
export interface WordCloudChartProps {
  /** 图表数据 */
  data: WordCloudDataItem[]
  /** 图表标题 */
  title?: string
  /** 词云形状 */
  shape?: WordCloudShape
  /** 颜色范围 */
  colorRange?: string[]
  /** 最小字体大小 */
  minFontSize?: number
  /** 最大字体大小 */
  maxFontSize?: number
  /** 字体 */
  fontFamily?: string
  /** 字体粗细 */
  fontWeight?: 'normal' | 'bold' | 'bolder' | number
  /** 网格大小 */
  gridSize?: number
  /** 旋转角度范围 */
  rotationRange?: [number, number]
  /** 旋转步进 */
  rotationStep?: number
}

const props = withDefaults(defineProps<WordCloudChartProps>(), {
  data: () => [],
  shape: 'circle',
  colorRange: () => ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2', '#eb2f96'],
  minFontSize: 12,
  maxFontSize: 60,
  fontFamily: 'sans-serif',
  fontWeight: 'bold',
  gridSize: 8,
  rotationRange: () => [-45, 45],
  rotationStep: 15
})

const emit = defineEmits<{
  /** 词语点击事件 */
  (e: 'word-click', params: { name: string; value: number }): void
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
    const chartOptions: WordCloudChartOptions = {
      title: props.title,
      shape: props.shape,
      colorRange: props.colorRange,
      minFontSize: props.minFontSize,
      maxFontSize: props.maxFontSize,
      fontFamily: props.fontFamily,
      fontWeight: props.fontWeight,
      gridSize: props.gridSize,
      rotationRange: props.rotationRange,
      rotationStep: props.rotationStep
    }

    const option = buildWordCloudOption(props.data, chartOptions)
    chartInstance.setOption(option, true)
  } catch (err: any) {
    error.value = err.message || '渲染图表失败'
    console.error('Failed to render word cloud chart:', err)
  } finally {
    loading.value = false
  }
}

/**
 * 处理图表点击事件
 */
const handleChartClick = (params: any) => {
  if (!params.name) return

  emit('word-click', {
    name: params.name,
    value: params.value ?? 0
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
  () => [props.data, props.title, props.shape, props.colorRange],
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
.wordcloud-chart-container {
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
