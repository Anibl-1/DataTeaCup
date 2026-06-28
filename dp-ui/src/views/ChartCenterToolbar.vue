<template>
  <div class="toolbar">
    <div class="toolbar-left">
      <n-input
        :value="searchKeyword"
        :placeholder="t('common.search') + '...'"
        clearable
        style="width: 280px"
        @keydown.enter="$emit('search')"
        @clear="$emit('search')"
        @update:value="$emit('update:searchKeyword', $event)"
      >
        <template #prefix>
          <n-icon><SearchOutline /></n-icon>
        </template>
      </n-input>
      <n-select
        :value="filterChartType"
        :options="chartTypeOptions"
        :placeholder="t('chart.type')"
        clearable
        style="width: 140px"
        @update:value="(v) => { $emit('update:filterChartType', v); $emit('search') }"
      />
      <n-select
        :value="sortBy"
        :options="sortOptions"
        :placeholder="t('common.select')"
        style="width: 120px"
        @update:value="(v) => { $emit('update:sortBy', v); $emit('search') }"
      />
    </div>
    <div class="toolbar-right">
      <n-button-group size="small">
        <n-button :type="viewMode === 'grid' ? 'primary' : 'default'" @click="$emit('update:viewMode', 'grid')">
          <template #icon><n-icon><GridOutline /></n-icon></template>
        </n-button>
        <n-button :type="viewMode === 'list' ? 'primary' : 'default'" @click="$emit('update:viewMode', 'list')">
          <template #icon><n-icon><ListOutline /></n-icon></template>
        </n-button>
      </n-button-group>
      <n-button 
        :type="showFavoriteOnly ? 'warning' : 'default'" 
        @click="$emit('toggle-favorite-filter')"
      >
        <template #icon>
          <n-icon><StarOutline /></n-icon>
        </template>
        {{ t('chart.favorite') }}
      </n-button>
    </div>
  </div>

  <!-- 快速筛选标签 -->
  <div v-if="showQuickFilters" class="quick-filters">
    <n-space :size="8">
      <n-tag 
        v-for="tag in quickFilterTags" 
        :key="tag.value"
        :type="activeQuickFilter === tag.value ? 'primary' : 'default'"
        :bordered="activeQuickFilter !== tag.value"
        round
        checkable
        :checked="activeQuickFilter === tag.value"
        style="cursor: pointer;"
        @click="$emit('quick-filter', tag.value)"
      >
        <template #icon>
          <n-icon size="14"><component :is="tag.icon" /></n-icon>
        </template>
        {{ tag.label }}
      </n-tag>
    </n-space>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NInput, NSelect, NButton, NButtonGroup, NIcon, NTag, NSpace } from 'naive-ui'
import {
  SearchOutline, GridOutline, ListOutline, StarOutline,
  AppsOutline, BarChartOutline, TrendingUpOutline, PieChartOutline,
  SpeedometerOutline, StatsChartOutline, AnalyticsOutline, PulseOutline
} from '@vicons/ionicons5'
import { CHART_TYPES } from '@/types/chart'
import { useI18n } from '@/i18n'

const { t } = useI18n()

defineProps<{
  searchKeyword: string
  filterChartType: string | null
  sortBy: string
  viewMode: 'grid' | 'list'
  showFavoriteOnly: boolean
  activeQuickFilter: string | null
  showQuickFilters: boolean
}>()

defineEmits<{
  'update:searchKeyword': [value: string]
  'update:filterChartType': [value: string | null]
  'update:sortBy': [value: string]
  'update:viewMode': [value: 'grid' | 'list']
  'search': []
  'toggle-favorite-filter': []
  'quick-filter': [value: string]
}>()

const chartTypeOptions = CHART_TYPES.map(ct => ({ label: ct.label, value: ct.value }))

const sortOptions = computed(() => [
  { label: t('common.updateTime'), value: 'updateTime' },
  { label: t('common.createTime'), value: 'createTime' },
  { label: t('common.name'), value: 'name' }
])

const quickFilterTags = computed(() => [
  { label: t('common.all'), value: 'all', icon: AppsOutline },
  { label: t('chart.types.bar'), value: 'bar', icon: BarChartOutline },
  { label: t('chart.types.line'), value: 'line', icon: TrendingUpOutline },
  { label: t('chart.types.pie'), value: 'pie', icon: PieChartOutline },
  { label: t('chart.types.table'), value: 'table', icon: ListOutline },
  { label: t('chart.types.gauge'), value: 'gauge', icon: SpeedometerOutline },
  { label: t('chart.types.scatter'), value: 'scatter', icon: StatsChartOutline },
  { label: t('chart.types.radar'), value: 'radar', icon: AnalyticsOutline },
  { label: 'KPI', value: 'kpi', icon: PulseOutline }
])
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
  background: var(--card-bg, #fff);
  padding: 12px 16px;
  border-radius: 12px;
  box-shadow: var(--card-shadow, 0 1px 3px rgba(0, 0, 0, 0.06));
}

.toolbar-left {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  gap: 8px;
  align-items: center;
}

.quick-filters {
  margin-bottom: 12px;
  padding: 10px 16px;
  background: var(--card-bg, #fff);
  border-radius: 10px;
  box-shadow: var(--card-shadow, 0 1px 3px rgba(0, 0, 0, 0.04));
}

.quick-filters .n-tag {
  font-size: 13px;
  padding: 4px 12px;
  transition: all 0.2s ease;
}

.quick-filters .n-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-left, .toolbar-right {
    justify-content: center;
  }
}
</style>

<style>
/* ChartCenterToolbar 深色模式（非 scoped） */
html.dark .toolbar-btn { color: #94a3b8 !important; }
html.dark .toolbar-btn:hover { color: #e2e8f0 !important; }

html.dark .toolbar {
  background: var(--card-bg, #1e1e2e) !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important;
}

html.dark .quick-filters {
  background: var(--card-bg, #1e1e2e) !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2) !important;
}

html.dark .quick-filters .n-tag {
  background: rgba(255, 255, 255, 0.06) !important;
  color: #cbd5e1 !important;
  border-color: rgba(255, 255, 255, 0.08) !important;
}

html.dark .quick-filters .n-tag:hover {
  background: rgba(255, 255, 255, 0.1) !important;
  color: #e2e8f0 !important;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3) !important;
}

html.dark .quick-filters .n-tag--primary-type {
  background: rgba(99, 102, 241, 0.2) !important;
  color: #a5b4fc !important;
  border-color: rgba(99, 102, 241, 0.3) !important;
}

html.dark .quick-filters .n-tag--primary-type:hover {
  background: rgba(99, 102, 241, 0.3) !important;
  color: #c7d2fe !important;
}
</style>
