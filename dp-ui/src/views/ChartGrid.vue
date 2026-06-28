<template>
  <div class="chart-grid-wrapper">
  <!-- 最近查看 -->
  <div v-if="showRecentSection" class="recent-section">
    <div class="section-header">
      <span class="section-title">
        <n-icon size="16" color="#18a058"><TimeOutline /></n-icon>
        最近查看
      </span>
      <n-button text size="small" @click="$emit('clear-recent')">清除记录</n-button>
    </div>
    <div class="recent-charts">
      <div 
        v-for="chart in recentCharts.slice(0, 5)" 
        :key="chart.id" 
        class="recent-chart-item"
        @click="$emit('view-recent', chart)"
      >
        <n-icon size="20" :color="getChartTypeColor(chart.chartType)">
          <component :is="getChartIconComponent(chart.chartType)" />
        </n-icon>
        <div class="recent-chart-info">
          <span class="recent-chart-name">{{ chart.chartName }}</span>
          <span class="recent-chart-time">{{ formatRelativeTime(chart.viewedAt) }}</span>
        </div>
      </div>
    </div>
  </div>

  <!-- 骨架屏加载 -->
  <div v-if="loading" class="grid-view">
    <div v-for="i in 8" :key="i" class="chart-card skeleton-card">
      <div class="card-preview skeleton-shimmer" style="background: var(--bg-tertiary);"></div>
      <div class="card-info">
        <div class="skeleton-line" style="width: 60%; height: 16px; margin-bottom: 8px;"></div>
        <div class="skeleton-line" style="width: 90%; height: 12px; margin-bottom: 4px;"></div>
        <div class="skeleton-line" style="width: 40%; height: 12px;"></div>
      </div>
    </div>
  </div>

  <template v-else>
    <!-- 网格视图 -->
    <div class="grid-view">
      <div 
        v-for="chart in chartList" 
        :key="chart.id" 
        class="chart-card"
        @click="$emit('view-chart', chart)"
      >
        <div class="card-preview" :style="{ background: getChartBgColor(chart.chartType) }">
          <n-icon size="42" color="rgba(255,255,255,0.85)">
            <component :is="getChartIconComponent(chart.chartType)" />
          </n-icon>
          <span class="chart-type-label">{{ getChartTypeLabel(chart.chartType) }}</span>
          <div class="card-badges">
            <span v-if="hasChartParameters(chart)" class="card-badge badge-param" title="支持参数查询">
              <n-icon size="12"><FilterOutline /></n-icon>
            </span>
            <span v-if="favorites.has(chart.id)" class="card-badge badge-fav">
              <n-icon size="12"><StarSharp /></n-icon>
            </span>
          </div>
        </div>
        <div class="card-info">
          <div class="card-title"><span>{{ chart.chartName }}</span></div>
          <div class="card-desc">{{ chart.description || '暂无描述' }}</div>
          <div class="card-footer">
            <span class="meta-time">
              <n-icon size="12"><TimeOutline /></n-icon>
              {{ formatDate(chart.updateTime || chart.createTime) }}
            </span>
            <div class="card-actions" @click.stop>
              <n-button text size="tiny" :style="{ color: favorites.has(chart.id) ? '#f0a020' : undefined }" @click.stop="$emit('favorite', chart)">
                <n-icon size="15"><component :is="favorites.has(chart.id) ? StarSharp : StarOutline" /></n-icon>
              </n-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="chartList.length === 0" class="empty-state">
      <div class="empty-illustration">
        <n-icon size="64" color="#cbd5e1"><BarChartOutline /></n-icon>
      </div>
      <div class="empty-title">还没有图表</div>
      <div class="empty-desc">试试调整筛选条件，或创建一个新图表</div>
      <n-button type="primary" @click="$emit('reset-filter')">重置筛选</n-button>
    </div>
  </template>

  <!-- 分页 -->
  <div v-if="chartList.length > 0" class="pagination">
    <n-pagination
      :page="page"
      :page-size="pageSize"
      :item-count="itemCount"
      :page-sizes="[12, 24, 48]"
      show-size-picker
      :prefix="() => `共 ${itemCount} 条`"
      @update:page="$emit('page-change', $event)"
      @update:page-size="$emit('page-size-change', $event)"
    />
  </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { NIcon, NButton, NPagination } from 'naive-ui'
import { StarOutline, StarSharp, FilterOutline, TimeOutline, BarChartOutline } from '@vicons/ionicons5'
import { getChartTypeColor } from '@/utils/chartRenderer'
import type { ChartDefinition } from '@/types/chart'
import { getChartIconComponent, getChartTypeLabel, formatChartDate, formatRelativeTime, hasChartParameters, getChartBgColor } from '@/utils/chartUtils'

interface RecentChart {
  id: number
  chartName: string
  chartType: string
  viewedAt: number
}

const props = defineProps<{
  loading: boolean
  chartList: ChartDefinition[]
  recentCharts: RecentChart[]
  showRecentSection: boolean
  favorites: Set<number>
  page: number
  pageSize: number
  itemCount: number
}>()

defineEmits<{
  'view-chart': [chart: ChartDefinition]
  'view-recent': [chart: RecentChart]
  'favorite': [chart: ChartDefinition]
  'clear-recent': []
  'reset-filter': []
  'page-change': [page: number]
  'page-size-change': [size: number]
}>()

const formatDate = formatChartDate
</script>

<style scoped>
.recent-section { margin-bottom: 16px; padding: 14px 16px; background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04); }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.section-title { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; color: #475569; }
.recent-charts { display: flex; gap: 10px; overflow-x: auto; padding-bottom: 4px; }
.recent-chart-item { display: flex; align-items: center; gap: 10px; padding: 8px 14px; background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0; cursor: pointer; transition: all 0.2s ease; min-width: 170px; flex-shrink: 0; }
.recent-chart-item:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08); border-color: #93c5fd; background: #eff6ff; }
.recent-chart-info { display: flex; flex-direction: column; gap: 2px; overflow: hidden; }
.recent-chart-name { font-size: 13px; font-weight: 500; color: #334155; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 120px; }
.recent-chart-time { font-size: 11px; color: #94a3b8; }
.grid-view { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
@media (max-width: 1400px) { .grid-view { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 1024px) { .grid-view { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 640px) { .grid-view { grid-template-columns: 1fr; } }
.chart-card { background: #fff; border-radius: 12px; overflow: hidden; cursor: pointer; transition: all 0.3s ease; border: 1px solid #e8eaed; position: relative; }
.chart-card::before { display: none; }
.chart-card:hover { transform: translateY(-4px); box-shadow: 0 12px 28px rgba(0, 0, 0, 0.1); border-color: #93c5fd; }
.card-preview { height: 120px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px; position: relative; overflow: hidden; }
.card-preview::after { content: ''; position: absolute; inset: 0; background: linear-gradient(180deg, transparent 50%, rgba(0,0,0,0.08) 100%); pointer-events: none; }
.chart-card:hover .card-preview::after { background: linear-gradient(180deg, transparent 40%, rgba(0,0,0,0.12) 100%); }
.card-badges { position: absolute; top: 8px; right: 8px; display: flex; gap: 4px; z-index: 1; }
.card-badge { width: 22px; height: 22px; border-radius: 6px; display: flex; align-items: center; justify-content: center; backdrop-filter: blur(8px); }
.badge-param { background: rgba(255, 255, 255, 0.35); color: #fff; }
.badge-fav { background: rgba(255, 255, 255, 0.35); color: #fbbf24; }
.chart-type-label { color: rgba(255, 255, 255, 0.9); font-size: 11px; font-weight: 500; letter-spacing: 0.5px; }
.card-info { padding: 14px 16px 12px; }
.card-title { font-weight: 600; font-size: 14px; color: #1e293b; margin-bottom: 6px; transition: color 0.2s ease; line-height: 1.4; }
.chart-card:hover .card-title { color: #2563eb; }
.card-title span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block; }
.card-desc { font-size: 12px; color: #94a3b8; line-height: 1.5; height: 36px; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; margin-bottom: 10px; }
.card-footer { display: flex; justify-content: space-between; align-items: center; padding-top: 10px; border-top: 1px solid #f1f5f9; }
.card-footer .meta-time { font-size: 11px; color: #b0b8c9; display: flex; align-items: center; gap: 4px; }
.card-actions { display: flex; gap: 2px; opacity: 0; transition: opacity 0.2s ease; }
.chart-card:hover .card-actions { opacity: 1; }
.skeleton-card { pointer-events: none; }
.skeleton-shimmer { animation: shimmer 1.5s infinite; background: linear-gradient(90deg, #e2e8f0 25%, #f1f5f9 50%, #e2e8f0 75%) !important; background-size: 200% 100% !important; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
.skeleton-line { background: #e2e8f0; border-radius: 4px; animation: shimmer 1.5s infinite; background: linear-gradient(90deg, #e2e8f0 25%, #f1f5f9 50%, #e2e8f0 75%); background-size: 200% 100%; }
.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 80px 0; gap: 12px; }
.empty-illustration { width: 120px; height: 120px; border-radius: 50%; background: #f1f5f9; display: flex; align-items: center; justify-content: center; margin-bottom: 8px; }
.empty-title { font-size: 18px; font-weight: 600; color: #334155; }
.empty-desc { font-size: 14px; color: #94a3b8; margin-bottom: 8px; }
.pagination { display: flex; justify-content: flex-end; padding: 16px 0; }

</style>

<style>
/* ChartGrid 深色模式（非 scoped） */
html.dark .grid-item { background: #1e293b !important; border-color: #334155 !important; }
html.dark .grid-item:hover { border-color: var(--color-primary) !important; }
html.dark .item-name { color: #e2e8f0 !important; }
html.dark .item-desc { color: #64748b !important; }
html.dark .item-meta { color: #475569 !important; }
html.dark .item-thumb { background: #1a2536 !important; }
</style>
