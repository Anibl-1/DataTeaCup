<template>
  <div ref="chartContainerRef" class="map-chart-container">
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
    <div v-if="showBackButton && drillLevel > 0" class="back-button">
      <n-button size="small" quaternary @click="handleDrillUp">
        <template #icon>
          <n-icon><arrow-back-outline /></n-icon>
        </template>
        返回上级
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, watch, onMounted, onBeforeUnmount, nextTick, computed } from 'vue'
import echarts from '@/utils/echarts'
import { NSpin, NResult, NButton, NIcon } from 'naive-ui'
import { ArrowBackOutline } from '@vicons/ionicons5'
import {
  buildChinaMapOption,
  buildWorldMapOption,
  buildProvinceMapOption,
  type MapDataItem,
  type VisualMapConfig,
  type MapChartOptions
} from '@/utils/chartMap'

/**
 * 地图类型
 */
export type MapType = 'china' | 'world'

/**
 * 地图图表 Props
 */
export interface MapChartProps {
  /** 图表数据 */
  chartData: MapDataItem[]
  /** 地图类型 */
  mapType?: MapType
  /** 视觉映射配置 */
  visualMapConfig?: VisualMapConfig
  /** 图表标题 */
  title?: string
  /** 是否显示标签 */
  showLabel?: boolean
  /** 是否启用下钻 */
  enableDrillDown?: boolean
  /** 是否显示返回按钮 */
  showBackButton?: boolean
  /** 是否启用缩放 */
  roam?: boolean | 'scale' | 'move'
  /** 地图缩放比例 */
  zoom?: number
  /** 区域默认颜色 */
  areaColor?: string
  /** 高亮区域颜色 */
  emphasisAreaColor?: string
}

const props = withDefaults(defineProps<MapChartProps>(), {
  chartData: () => [],
  mapType: 'china',
  showLabel: true,
  enableDrillDown: true,
  showBackButton: true,
  roam: true,
  zoom: 1.2
})

const emit = defineEmits<{
  /** 区域点击事件 */
  (e: 'region-click', params: { name: string; value: number; data: MapDataItem }): void
  /** 下钻事件 */
  (e: 'drill-down', params: { province: string; level: number }): void
  /** 上钻事件 */
  (e: 'drill-up', params: { level: number }): void
  /** 图表就绪事件 */
  (e: 'chart-ready', instance: echarts.ECharts): void
}>()

// Refs
const chartContainerRef = ref<HTMLElement | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

// Chart instance
let chartInstance: echarts.ECharts | null = null

// Drill down state
const drillLevel = ref(0)
const drillStack = ref<string[]>([])
const currentProvince = ref<string | null>(null)

// Map registration status
const registeredMaps = new Set<string>()

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
 * 注册地图数据
 */
const registerMap = async (mapName: string): Promise<boolean> => {
  if (registeredMaps.has(mapName)) {
    return true
  }

  try {
    // 动态加载地图 JSON 数据
    // 注意：实际项目中需要根据项目配置调整路径
    let geoJson: any

    if (mapName === 'china') {
      // 中国地图
      const response = await fetch('/map/china.json')
      if (!response.ok) {
        throw new Error(`Failed to load china map: ${response.status}`)
      }
      geoJson = await response.json()
    } else if (mapName === 'world') {
      // 世界地图
      const response = await fetch('/map/world.json')
      if (!response.ok) {
        throw new Error(`Failed to load world map: ${response.status}`)
      }
      geoJson = await response.json()
    } else {
      // 省级地图
      const response = await fetch(`/map/province/${mapName}.json`)
      if (!response.ok) {
        throw new Error(`Failed to load province map: ${response.status}`)
      }
      geoJson = await response.json()
    }

    echarts.registerMap(mapName, geoJson)
    registeredMaps.add(mapName)
    return true
  } catch (err) {
    console.error(`Failed to register map: ${mapName}`, err)
    return false
  }
}

/**
 * 渲染图表
 */
const renderChart = async () => {
  if (!chartInstance) {
    initChart()
  }

  if (!chartInstance) return

  loading.value = true
  error.value = null

  try {
    let option: echarts.EChartsOption
    let mapName: string

    const chartOptions: MapChartOptions = {
      title: props.title,
      visualMapConfig: props.visualMapConfig,
      showLabel: props.showLabel,
      roam: props.roam,
      zoom: props.zoom,
      areaColor: props.areaColor,
      emphasisAreaColor: props.emphasisAreaColor
    }

    if (drillLevel.value > 0 && currentProvince.value) {
      // 省级地图（下钻状态）
      mapName = currentProvince.value
      option = buildProvinceMapOption(currentProvince.value, props.chartData, chartOptions)
    } else if (props.mapType === 'world') {
      // 世界地图
      mapName = 'world'
      option = buildWorldMapOption(props.chartData, chartOptions)
    } else {
      // 中国地图
      mapName = 'china'
      option = buildChinaMapOption(props.chartData, chartOptions)
    }

    // 注册地图
    const registered = await registerMap(mapName)
    if (!registered) {
      error.value = `无法加载地图数据: ${mapName}`
      return
    }

    chartInstance.setOption(option, true)
  } catch (err: any) {
    error.value = err.message || '渲染图表失败'
    console.error('Failed to render map chart:', err)
  } finally {
    loading.value = false
  }
}

/**
 * 处理图表点击事件
 */
const handleChartClick = (params: any) => {
  if (!params.name) return

  const dataItem = props.chartData.find(item => item.name === params.name)
  const clickData: MapDataItem = dataItem || {
    name: params.name,
    value: params.value ?? 0
  }

  // 触发点击事件
  emit('region-click', {
    name: params.name,
    value: clickData.value,
    data: clickData
  })

  // 处理下钻（仅中国地图支持）
  if (props.enableDrillDown && props.mapType === 'china' && drillLevel.value === 0) {
    handleDrillDown(params.name)
  }
}

/**
 * 处理下钻
 */
const handleDrillDown = async (provinceName: string) => {
  drillStack.value.push(provinceName)
  currentProvince.value = provinceName
  drillLevel.value = 1

  emit('drill-down', {
    province: provinceName,
    level: drillLevel.value
  })

  await renderChart()
}

/**
 * 处理上钻
 */
const handleDrillUp = async () => {
  if (drillLevel.value <= 0) return

  drillStack.value.pop()
  drillLevel.value = 0
  currentProvince.value = null

  emit('drill-up', {
    level: drillLevel.value
  })

  await renderChart()
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
 * 重置下钻状态
 */
const resetDrill = async () => {
  drillStack.value = []
  drillLevel.value = 0
  currentProvince.value = null
  await renderChart()
}

/**
 * 获取图表实例
 */
const getChartInstance = () => chartInstance

// Watch props changes
watch(
  () => [props.chartData, props.mapType, props.title, props.visualMapConfig],
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
  resetDrill,
  getChartInstance,
  resize: handleResize
})
</script>

<style scoped>
.map-chart-container {
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

.back-button {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 5;
}
</style>
