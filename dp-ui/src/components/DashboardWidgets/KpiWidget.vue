<template>
  <WidgetWrapper
    :title="config?.title || 'KPI'"
    :selected="selected"
    :loading="loading"
    :error="error"
    :readonly="readonly"
    :show-header="showHeader"
    @click="$emit('select')"
    @refresh="handleRefresh"
    @remove="$emit('remove')"
  >
    <div class="kpi-container" :style="containerStyle">
      <!-- Main Value -->
      <div class="kpi-value" :style="valueStyle">
        <span v-if="config?.prefix" class="kpi-prefix">{{ config.prefix }}</span>
        <span class="kpi-number">{{ formattedValue }}</span>
        <span v-if="config?.unit" class="kpi-unit">{{ config.unit }}</span>
        <span v-if="config?.suffix" class="kpi-suffix">{{ config.suffix }}</span>
      </div>
      
      <!-- Trend Indicator -->
      <div v-if="showTrend" class="kpi-trend" :class="trendClass">
        <n-icon size="16">
          <TrendingUpOutline v-if="trendDirection === 'up'" />
          <TrendingDownOutline v-else-if="trendDirection === 'down'" />
          <RemoveOutline v-else />
        </n-icon>
        <span class="trend-value">{{ trendText }}</span>
      </div>
      
      <!-- Comparison Section -->
      <div v-if="showComparison" class="kpi-comparison">
        <div v-if="yoyChange !== null" class="comparison-item">
          <span class="comparison-label">同比</span>
          <span class="comparison-value" :class="getChangeClass(yoyChange)">
            {{ formatChange(yoyChange) }}
          </span>
        </div>
        <div v-if="momChange !== null" class="comparison-item">
          <span class="comparison-label">环比</span>
          <span class="comparison-value" :class="getChangeClass(momChange)">
            {{ formatChange(momChange) }}
          </span>
        </div>
      </div>
    </div>
  </WidgetWrapper>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, watch, onMounted } from 'vue'
import { NIcon } from 'naive-ui'
import { TrendingUpOutline, TrendingDownOutline, RemoveOutline } from '@vicons/ionicons5'
import WidgetWrapper from './WidgetWrapper.vue'
import type { KpiWidgetConfig } from '@/types/dashboard'
import { executeSql } from '@/api/tableData'

const props = withDefaults(defineProps<{
  config: KpiWidgetConfig
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
}>()

const loading = ref(false)
const error = ref<string | null>(null)

// Data values
const currentValue = ref<number>(0)
const previousValue = ref<number | null>(null)
const periodPreviousValue = ref<number | null>(null)

// Computed properties
const formattedValue = computed(() => {
  const value = currentValue.value
  if (value === null || value === undefined) return '--'
  
  // Format large numbers
  if (Math.abs(value) >= 1000000000) {
    return (value / 1000000000).toFixed(2) + 'B'
  } else if (Math.abs(value) >= 1000000) {
    return (value / 1000000).toFixed(2) + 'M'
  } else if (Math.abs(value) >= 1000) {
    return (value / 1000).toFixed(2) + 'K'
  }
  
  // Format with appropriate decimal places
  if (Number.isInteger(value)) {
    return value.toLocaleString()
  }
  return value.toLocaleString(undefined, { maximumFractionDigits: 2 })
})

const trendDirection = computed(() => {
  if (props.config?.trend) return props.config.trend
  
  if (previousValue.value !== null && previousValue.value !== 0) {
    const change = currentValue.value - previousValue.value
    if (change > 0) return 'up'
    if (change < 0) return 'down'
  }
  return 'neutral'
})

const trendClass = computed(() => {
  return {
    'trend-up': trendDirection.value === 'up',
    'trend-down': trendDirection.value === 'down',
    'trend-neutral': trendDirection.value === 'neutral'
  }
})

const showTrend = computed(() => {
  return props.config?.trend || previousValue.value !== null
})

const trendText = computed(() => {
  if (previousValue.value === null || previousValue.value === 0) return '--'
  
  const change = ((currentValue.value - previousValue.value) / previousValue.value) * 100
  const sign = change >= 0 ? '+' : ''
  return `${sign}${change.toFixed(1)}%`
})

// Year-over-year change (同比)
const yoyChange = computed(() => {
  if (previousValue.value === null || previousValue.value === 0) return null
  return ((currentValue.value - previousValue.value) / previousValue.value) * 100
})

// Month-over-month change (环比)
const momChange = computed(() => {
  if (periodPreviousValue.value === null || periodPreviousValue.value === 0) return null
  return ((currentValue.value - periodPreviousValue.value) / periodPreviousValue.value) * 100
})

const showComparison = computed(() => {
  return yoyChange.value !== null || momChange.value !== null
})

const containerStyle = computed(() => ({
  backgroundColor: props.config?.backgroundColor || 'transparent'
}))

const valueStyle = computed(() => ({
  color: props.config?.color || '#1890ff'
}))

// Methods
const formatChange = (change: number | null): string => {
  if (change === null) return '--'
  const sign = change >= 0 ? '+' : ''
  return `${sign}${change.toFixed(1)}%`
}

const getChangeClass = (change: number | null): string => {
  if (change === null) return ''
  if (change > 0) return 'change-up'
  if (change < 0) return 'change-down'
  return 'change-neutral'
}

const loadKpiData = async () => {
  if (!props.config?.dataSourceId || !props.config?.sql) {
    // Use placeholder data
    currentValue.value = 0
    return
  }
  
  loading.value = true
  error.value = null
  
  try {
    // Substitute filter parameters in SQL
    let sql = props.config.sql
    if (props.filters) {
      Object.entries(props.filters).forEach(([key, value]) => {
        const placeholder = '${' + key + '}'
        sql = sql.split(placeholder).join(String(value ?? ''))
      })
    }
    
    const res = await executeSql({
      dataSourceId: props.config.dataSourceId,
      sql
    })
    
    const data = res.data?.data || res.data
    if (Array.isArray(data) && data.length > 0) {
      const row = data[0]
      
      // Extract current value
      const valueField = props.config.valueField || 'value'
      currentValue.value = Number(row[valueField]) || 0
      
      // Extract previous value (for YoY)
      if (props.config.previousValueField && row[props.config.previousValueField] !== undefined) {
        previousValue.value = Number(row[props.config.previousValueField]) || null
      }
      
      // Extract period previous value (for MoM)
      if (props.config.periodPreviousField && row[props.config.periodPreviousField] !== undefined) {
        periodPreviousValue.value = Number(row[props.config.periodPreviousField]) || null
      }
    }
  } catch (err: any) {
    error.value = err.message || '加载KPI数据失败'
    console.error('Failed to load KPI data:', err)
  } finally {
    loading.value = false
  }
}

const handleRefresh = () => {
  loadKpiData()
}

// Watch config changes
watch(() => props.config, () => {
  loadKpiData()
}, { deep: true })

// Watch filters changes
watch(() => props.filters, () => {
  loadKpiData()
}, { deep: true })

// Lifecycle
onMounted(() => {
  loadKpiData()
})

// Expose methods
defineExpose({
  refresh: handleRefresh
})
</script>

<style scoped>
.kpi-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 16px;
  text-align: center;
}

.kpi-value {
  display: flex;
  align-items: baseline;
  justify-content: center;
  flex-wrap: wrap;
  gap: 4px;
}

.kpi-prefix {
  font-size: 16px;
  color: #666;
}

.kpi-number {
  font-size: 36px;
  font-weight: 600;
  line-height: 1.2;
}

.kpi-unit {
  font-size: 14px;
  color: #666;
  margin-left: 4px;
}

.kpi-suffix {
  font-size: 14px;
  color: #666;
}

.kpi-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  font-size: 14px;
}

.kpi-trend.trend-up {
  color: #52c41a;
}

.kpi-trend.trend-down {
  color: #ff4d4f;
}

.kpi-trend.trend-neutral {
  color: #999;
}

.trend-value {
  font-weight: 500;
}

.kpi-comparison {
  display: flex;
  gap: 16px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.comparison-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.comparison-label {
  font-size: 12px;
  color: #999;
}

.comparison-value {
  font-size: 14px;
  font-weight: 500;
}

.comparison-value.change-up {
  color: #52c41a;
}

.comparison-value.change-down {
  color: #ff4d4f;
}

.comparison-value.change-neutral {
  color: #999;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .kpi-number {
    font-size: 28px;
  }
}
</style>
