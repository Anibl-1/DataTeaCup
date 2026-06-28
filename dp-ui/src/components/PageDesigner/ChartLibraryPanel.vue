<template>
  <div class="chart-library-panel">
    <n-input
      v-model:value="searchKeyword"
      placeholder="搜索已有图表..."
      clearable
      size="small"
      style="margin-bottom: 8px"
    >
      <template #prefix>
        <n-icon><SearchOutline /></n-icon>
      </template>
    </n-input>

    <n-select
      v-model:value="filterType"
      :options="typeFilterOptions"
      placeholder="全部类型"
      size="small"
      clearable
      style="margin-bottom: 12px"
    />

    <n-scrollbar style="max-height: calc(100vh - 330px)">
      <n-spin :show="loading">
        <div v-if="filteredCharts.length > 0" class="chart-list">
          <div
            v-for="chart in filteredCharts"
            :key="chart.id"
            class="chart-card"
            draggable="true"
            @dragstart="handleDragStart($event, chart)"
          >
            <div class="chart-card-icon" :style="{ background: getTypeBg(chart.chartType) }">
              <n-icon size="20" :color="getTypeColor(chart.chartType)">
                <component :is="getTypeIcon(chart.chartType)" />
              </n-icon>
            </div>
            <div class="chart-card-info">
              <div class="chart-card-name">{{ chart.chartName }}</div>
              <div class="chart-card-meta">
                <n-tag size="tiny" :bordered="false">{{ getTypeLabel(chart.chartType) }}</n-tag>
              </div>
            </div>
            <n-button size="tiny" quaternary circle class="chart-add-btn" @click.stop="handleClickAdd(chart)">
              <template #icon><n-icon size="14"><AddOutline /></n-icon></template>
            </n-button>
          </div>
        </div>
        <n-empty v-else-if="!loading" description="暂无可用图表" size="small" />
      </n-spin>
    </n-scrollbar>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed } from 'vue'
import { NInput, NIcon, NScrollbar, NSelect, NSpin, NEmpty, NTag, NButton } from 'naive-ui'
import { SearchOutline, AddOutline, BarChartOutline, PieChartOutline, TrendingUpOutline, StatsChartOutline, PulseOutline, GridOutline, SpeedometerOutline, FunnelOutline } from '@vicons/ionicons5'
import { CHART_TYPES } from '@/types/chart'
import type { ChartDefinition } from '@/types/chart'

const props = defineProps<{
  charts: ChartDefinition[]
  loading: boolean
}>()

const searchKeyword = ref('')
const filterType = ref<string | null>(null)

const typeFilterOptions = computed(() => [
  { label: '全部类型', value: '' },
  ...Array.from(new Set(props.charts.map(c => c.chartType))).map(t => ({
    label: CHART_TYPES.find(ct => ct.value === t)?.label || t,
    value: t
  }))
])

const filteredCharts = computed(() => {
  let list = props.charts.filter(c => c.status === 1)
  if (filterType.value) {
    list = list.filter(c => c.chartType === filterType.value)
  }
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(c =>
      c.chartName.toLowerCase().includes(kw) ||
      c.chartCode?.toLowerCase().includes(kw) ||
      c.chartType.toLowerCase().includes(kw)
    )
  }
  return list
})

const iconMap: Record<string, any> = {
  line: TrendingUpOutline, bar: BarChartOutline, pie: PieChartOutline,
  table: GridOutline, scatter: StatsChartOutline, radar: PulseOutline,
  gauge: SpeedometerOutline, funnel: FunnelOutline
}
const getTypeIcon = (type: string) => iconMap[type] || BarChartOutline
const getTypeLabel = (type: string) => CHART_TYPES.find(t => t.value === type)?.label || type

const colorMap: Record<string, string> = {
  line: '#5470c6', bar: '#91cc75', pie: '#fac858', table: '#36cfc9',
  scatter: '#73c0de', radar: '#9a60b4', gauge: '#ee6666', funnel: '#fc8452'
}
const getTypeColor = (type: string) => colorMap[type] || '#5470c6'
const getTypeBg = (type: string) => `${getTypeColor(type)}18`

const emit = defineEmits<{
  (e: 'add-chart', chart: ChartDefinition): void
}>()

const handleDragStart = (e: DragEvent, chart: ChartDefinition) => {
  e.dataTransfer?.setData('chart', JSON.stringify({
    id: chart.id,
    name: chart.chartName,
    type: chart.chartType
  }))
  e.dataTransfer!.effectAllowed = 'copy'
}

const handleClickAdd = (chart: ChartDefinition) => {
  emit('add-chart', chart)
}
</script>

<style scoped>
.chart-library-panel {
  padding: 4px 0;
}
.chart-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.chart-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  cursor: grab;
  transition: all 0.2s;
}
.chart-card:hover {
  border-color: #5470c6;
  background: #f8faff;
  box-shadow: 0 2px 8px rgba(84, 112, 198, 0.12);
  transform: translateX(2px);
}
.chart-add-btn {
  opacity: 0;
  transition: opacity 0.15s;
  flex-shrink: 0;
  color: #5470c6;
}
.chart-card:hover .chart-add-btn {
  opacity: 1;
}
.chart-card:active {
  cursor: grabbing;
}
.chart-card-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.chart-card-info {
  flex: 1;
  min-width: 0;
}
.chart-card-name {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;
}
.chart-card-meta {
  display: flex;
  gap: 4px;
}
</style>
