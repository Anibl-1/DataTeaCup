<template>
  <n-data-table
    :columns="tableColumns"
    :data="chartList"
    :row-key="(row: any) => row.id"
    :bordered="false"
    striped
    @row-click="(rowData: any) => $emit('view-chart', rowData)"
  />
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { h } from 'vue'
import { NIcon, NButton, NTag } from 'naive-ui'
import { StarOutline, StarSharp, EyeOutline, FilterOutline } from '@vicons/ionicons5'
import { getChartTypeColor } from '@/utils/chartRenderer'
import type { ChartDefinition } from '@/types/chart'
import { getChartIconComponent, getChartTypeLabel, formatChartDate as formatDate, hasChartParameters } from '@/utils/chartUtils'

const props = defineProps<{
  chartList: ChartDefinition[]
  favorites: Set<number>
}>()

const emit = defineEmits<{
  'view-chart': [chart: ChartDefinition]
  'favorite': [chart: ChartDefinition]
}>()

const isFavorite = (id?: number) => id ? props.favorites.has(id) : false

const tableColumns = [
  {
    title: '图表名称',
    key: 'chartName',
    render: (row: ChartDefinition) => h('div', { style: 'display: flex; align-items: center; gap: 8px;' }, [
      h(NIcon, { size: 18, color: getChartTypeColor(row.chartType) }, () => h(getChartIconComponent(row.chartType))),
      h('span', { style: 'font-weight: 500;' }, row.chartName),
      hasChartParameters(row) ? h(NIcon, { size: 14, color: '#18a058', title: '支持参数查询' }, () => h(FilterOutline)) : null,
      isFavorite(row.id) ? h(NIcon, { size: 14, color: '#f0a020' }, () => h(StarSharp)) : null
    ])
  },
  {
    title: '类型',
    key: 'chartType',
    width: 120,
    render: (row: ChartDefinition) => h(NTag, { size: 'small', type: 'info' }, () => getChartTypeLabel(row.chartType))
  },
  { title: '编码', key: 'chartCode', width: 150, ellipsis: { tooltip: true } },
  {
    title: '描述',
    key: 'description',
    width: 180,
    ellipsis: { tooltip: true },
    render: (row: ChartDefinition) => row.description || '-'
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row: ChartDefinition) => h(NTag, { size: 'small', type: row.status === 1 ? 'success' : 'default' }, () => row.status === 1 ? '启用' : '禁用')
  },
  {
    title: '更新时间',
    key: 'updateTime',
    width: 120,
    render: (row: ChartDefinition) => formatDate(row.updateTime)
  },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    render: (row: ChartDefinition) => h('div', { style: 'display: flex; gap: 8px;', onClick: (e: Event) => e.stopPropagation() }, [
      h(NButton, { size: 'small', text: true, onClick: () => emit('view-chart', row) }, { icon: () => h(NIcon, null, () => h(EyeOutline)) }),
      h(NButton, { size: 'small', text: true, onClick: () => emit('favorite', row) }, { icon: () => h(NIcon, { color: isFavorite(row.id) ? '#f0a020' : '#999' }, () => h(StarOutline)) })
    ])
  }
]
</script>
