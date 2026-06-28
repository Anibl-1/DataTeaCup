<template>
  <div class="chart-embed-page">
    <!-- 加载状态 -->
    <div v-if="loading" class="embed-loading">
      <n-spin size="large" />
      <p>图表加载中...</p>
    </div>
    <!-- 错误提示 -->
    <div v-else-if="errorMsg" class="embed-error">
      <n-result status="error" :title="errorMsg" description="无法加载图表数据" />
    </div>
    <!-- 表格类型图表 -->
    <template v-else-if="isTableType && chartDefinition">
      <div v-if="showTitle" class="embed-title">{{ chartDefinition.chartName }}</div>
      <div class="embed-table-wrapper">
        <n-data-table
          :columns="tableColumns"
          :data="tableRows"
          :row-key="getTableRowKey"
          :bordered="true"
          :single-line="false"
          striped
          size="small"
          :max-height="maxTableHeight"
          virtual-scroll
        />
      </div>
    </template>
    <!-- ECharts 图表 -->
    <template v-else>
      <div v-if="showTitle && chartDefinition" class="embed-title">{{ chartDefinition.chartName }}</div>
      <div ref="chartRef" class="embed-chart"></div>
    </template>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, onMounted, onBeforeUnmount, nextTick, computed, h } from 'vue'
import { useRoute } from 'vue-router'
import { NSpin, NResult, NDataTable, NTag } from 'naive-ui'
import echarts from '@/utils/echarts'
import { getChartDefinitionById, getChartData } from '@/api/chart'
import { buildChartOption } from '@/utils/chartRenderer'
import { registerThemes } from '@/utils/chartThemes'
import { formatCellValueSmart } from '@/utils/format'
import type { ChartDefinition } from '@/types/chart'

registerThemes()

const route = useRoute()

const loading = ref(true)
const errorMsg = ref('')
const chartDefinition = ref<ChartDefinition | null>(null)
const chartData = ref<any[]>([])
const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

// URL 参数
const showTitle = computed(() => route.query["title"] !== '0')
const theme = computed(() => (route.query["theme"] as string) || 'default')

const isTableType = computed(() => {
  const t = chartDefinition.value?.chartType
  return t === 'table' || t === 'pivot_table'
})

const maxTableHeight = computed(() => {
  return showTitle.value ? 'calc(100vh - 50px)' : '100vh'
})

// 动态生成表格列
const tableColumns = computed(() => {
  if (!chartData.value.length) return []
  const keys = Object.keys(chartData.value[0])
  return keys.map(key => ({
    title: key,
    key,
    ellipsis: { tooltip: true },
    render: (row: any) => {
      const v = row[key]
      return formatCellValueSmart ? formatCellValueSmart(v) : String(v ?? '')
    }
  }))
})

const tableRows = computed(() => chartData.value.map((row, index) => ({
  ...row,
  __dpRowKey: row.id ?? row.ID ?? row._id ?? index
})))

const getTableRowKey = (row: any) => row.__dpRowKey

const loadChart = async () => {
  const id = Number(route.params["id"])
  if (!id || isNaN(id)) {
    errorMsg.value = '无效的图表 ID'
    loading.value = false
    return
  }

  try {
    const chartRes = await getChartDefinitionById(id)
    chartDefinition.value = (chartRes?.data as unknown as ChartDefinition) || null

    if (!chartDefinition.value) {
      errorMsg.value = '图表不存在'
      return
    }

    const dataRes = await getChartData(id, { limit: 10000 })
    chartData.value = Array.isArray(dataRes?.data) ? dataRes.data : []

    if (!isTableType.value) {
      await nextTick()
      renderChart()
    }
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 403 ? '无权访问该图表' : '加载图表失败'
  } finally {
    loading.value = false
  }
}

const renderChart = () => {
  if (!chartRef.value || !chartDefinition.value) return
  if (chartInstance) chartInstance.dispose()

  chartInstance = echarts.init(chartRef.value, theme.value)
  const option = buildChartOption(chartDefinition.value, chartData.value)
  chartInstance.setOption(option, true)
}

const handleResize = () => {
  chartInstance?.resize()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  loadChart()
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
.chart-embed-page {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: #fff;
  display: flex;
  flex-direction: column;
}

.embed-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 16px;
  color: #666;
}

.embed-error {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.embed-title {
  padding: 12px 16px 4px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  flex-shrink: 0;
}

.embed-chart {
  flex: 1;
  min-height: 0;
}

.embed-table-wrapper {
  flex: 1;
  padding: 8px;
  overflow: hidden;
}

</style>

<style>
/* ChartEmbed 深色模式（非 scoped） */
html.dark .embed-container { background: #0f172a !important; }
</style>
