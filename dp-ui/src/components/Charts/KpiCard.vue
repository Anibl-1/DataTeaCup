<template>
  <div
    class="kpi-card"
    :class="[`kpi-card--${size}`, `kpi-card--${colorTheme}`, { 'kpi-card--clickable': clickable }]"
    :style="cardStyle"
    @click="handleClick"
  >
    <!-- Loading State -->
    <div v-if="loading" class="kpi-card__loading">
      <n-spin size="small" />
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="kpi-card__error">
      <n-icon size="24" color="#ff4d4f">
        <AlertCircleOutline />
      </n-icon>
      <span class="error-text">{{ error }}</span>
    </div>

    <!-- Content -->
    <template v-else>
      <!-- Header with Icon and Title -->
      <div class="kpi-card__header">
        <div v-if="icon" class="kpi-card__icon" :style="iconStyle">
          <n-icon :size="sizeConfig.iconSize">
            <component :is="iconComponent" />
          </n-icon>
        </div>
        <span class="kpi-card__title" :style="titleStyle">{{ title }}</span>
      </div>

      <!-- Main Value -->
      <div class="kpi-card__value" :style="valueStyle">
        <span v-if="prefix" class="kpi-card__prefix">{{ prefix }}</span>
        <span class="kpi-card__number">{{ displayValue }}</span>
        <span v-if="unit" class="kpi-card__unit">{{ unit }}</span>
        <span v-if="suffix" class="kpi-card__suffix">{{ suffix }}</span>
      </div>

      <!-- Trend Indicator -->
      <div v-if="showTrend" class="kpi-card__trend" :class="trendClass">
        <n-icon :size="sizeConfig.trendFontSize">
          <TrendingUpOutline v-if="computedTrend === 'up'" />
          <TrendingDownOutline v-else-if="computedTrend === 'down'" />
          <RemoveOutline v-else />
        </n-icon>
        <span class="trend-value">{{ trendText }}</span>
        <span v-if="comparisonLabel" class="trend-label">{{ comparisonLabel }}</span>
      </div>

      <!-- Comparison Section (YoY / MoM) -->
      <div v-if="showComparison" class="kpi-card__comparison">
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

      <!-- Sparkline Mini Chart -->
      <div v-if="sparklineData && sparklineData.length > 0" ref="sparklineRef" class="kpi-card__sparkline">
        <!-- ECharts sparkline will be rendered here -->
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick, type Component } from 'vue'
import { NIcon, NSpin } from 'naive-ui'
import {
  TrendingUpOutline,
  TrendingDownOutline,
  RemoveOutline,
  AlertCircleOutline,
  CashOutline,
  PeopleOutline,
  CartOutline,
  StatsChartOutline,
  TrendingUpSharp,
  WalletOutline,
  BarChartOutline,
  PieChartOutline,
  PulseOutline,
  TimeOutline,
  CheckmarkCircleOutline,
  CloseCircleOutline
} from '@vicons/ionicons5'
import echarts from '@/utils/echarts'
import {
  calculateTrend,
  formatKpiValue,
  buildSparklineOption,
  buildKpiCardConfig,
  getTrendColor,
  formatChangePercent,
  getKpiSizeConfig,
  type TrendDirection,
  type ValueFormat,
  type KpiConfig,
  type SparklineOptions
} from '@/utils/kpiCard'

/**
 * KPI 卡片 Props
 */
export interface KpiCardProps {
  /** 当前值 */
  value: number | null
  /** 标题 */
  title: string
  /** 上期值（用于同比计算） */
  previousValue?: number | null
  /** 上周期值（用于环比计算） */
  periodPreviousValue?: number | null
  /** 趋势方向（手动指定） */
  trend?: TrendDirection
  /** 趋势百分比（手动指定） */
  trendPercent?: number | null
  /** 迷你图数据 */
  sparklineData?: number[]
  /** 图标名称 */
  icon?: string
  /** 尺寸 */
  size?: 'small' | 'medium' | 'large'
  /** 颜色主题 */
  colorTheme?: 'default' | 'positive' | 'negative' | 'neutral' | 'primary' | 'warning'
  /** 值格式 */
  format?: ValueFormat
  /** 单位 */
  unit?: string
  /** 前缀 */
  prefix?: string
  /** 后缀 */
  suffix?: string
  /** 小数位数 */
  decimals?: number
  /** 货币符号 */
  currencySymbol?: string
  /** 正向趋势是否为好 */
  positiveIsGood?: boolean
  /** 是否可点击 */
  clickable?: boolean
  /** 是否加载中 */
  loading?: boolean
  /** 错误信息 */
  error?: string | null
  /** 背景颜色 */
  backgroundColor?: string
  /** 值颜色 */
  valueColor?: string
  /** 比较标签 */
  comparisonLabel?: string
  /** 迷你图配置 */
  sparklineOptions?: SparklineOptions
}

const props = withDefaults(defineProps<KpiCardProps>(), {
  value: null,
  title: 'KPI',
  previousValue: null,
  periodPreviousValue: null,
  trend: undefined,
  trendPercent: null,
  sparklineData: () => [],
  icon: undefined,
  size: 'medium',
  colorTheme: 'default',
  format: 'number',
  unit: '',
  prefix: '',
  suffix: '',
  decimals: 2,
  currencySymbol: '¥',
  positiveIsGood: true,
  clickable: false,
  loading: false,
  error: null,
  backgroundColor: undefined,
  valueColor: undefined,
  comparisonLabel: '',
  sparklineOptions: () => ({})
})

const emit = defineEmits<{
  /** 点击事件 */
  (e: 'click', data: { value: number | null; title: string }): void
  /** 下钻事件 */
  (e: 'drill-down', data: { value: number | null; title: string }): void
}>()

// Refs
const sparklineRef = ref<HTMLElement | null>(null)
let sparklineChart: echarts.ECharts | null = null

// Icon mapping
const iconMap: Record<string, Component> = {
  cash: CashOutline,
  people: PeopleOutline,
  cart: CartOutline,
  stats: StatsChartOutline,
  trending: TrendingUpSharp,
  wallet: WalletOutline,
  bar: BarChartOutline,
  pie: PieChartOutline,
  pulse: PulseOutline,
  time: TimeOutline,
  success: CheckmarkCircleOutline,
  error: CloseCircleOutline
}

// Computed properties
const sizeConfig = computed(() => getKpiSizeConfig(props.size))

const iconComponent = computed(() => {
  if (!props.icon) return null
  return iconMap[props.icon] || StatsChartOutline
})

const kpiConfig = computed<KpiConfig>(() => ({
  format: props.format,
  currencySymbol: props.currencySymbol,
  decimals: props.decimals,
  unit: props.unit,
  prefix: props.prefix,
  suffix: props.suffix,
  positiveIsGood: props.positiveIsGood
}))

const kpiData = computed(() => {
  if (props.value === null) {
    return null
  }
  return buildKpiCardConfig(
    props.value,
    props.previousValue ?? null,
    props.periodPreviousValue ?? null,
    kpiConfig.value
  )
})

const displayValue = computed(() => {
  if (props.value === null || props.value === undefined) {
    return '--'
  }
  return formatKpiValue(props.value, props.format, {
    decimals: props.decimals,
    currencySymbol: props.currencySymbol
  })
})

const computedTrend = computed<TrendDirection>(() => {
  // 优先使用手动指定的趋势
  if (props.trend) {
    return props.trend
  }
  // 使用计算的趋势
  return kpiData.value?.trend || 'neutral'
})

const showTrend = computed(() => {
  return props.trend || props.previousValue !== null || props.trendPercent !== null
})

const trendText = computed(() => {
  // 优先使用手动指定的趋势百分比
  if (props.trendPercent !== null) {
    return formatChangePercent(props.trendPercent, props.decimals)
  }
  // 使用计算的趋势百分比
  return formatChangePercent(kpiData.value?.trendPercent ?? null, props.decimals)
})

const trendClass = computed(() => {
  const trend = computedTrend.value
  const isGood = props.positiveIsGood ? trend === 'up' : trend === 'down'
  
  return {
    'trend-up': trend === 'up',
    'trend-down': trend === 'down',
    'trend-neutral': trend === 'neutral',
    'trend-good': trend !== 'neutral' && isGood,
    'trend-bad': trend !== 'neutral' && !isGood
  }
})

// YoY and MoM changes
const yoyChange = computed(() => kpiData.value?.yoyChange ?? null)
const momChange = computed(() => kpiData.value?.momChange ?? null)

const showComparison = computed(() => {
  return (yoyChange.value !== null || momChange.value !== null) && !showTrend.value
})

// Styles
const cardStyle = computed(() => {
  const style: Record<string, string> = {}
  if (props.backgroundColor) {
    style.backgroundColor = props.backgroundColor
  }
  return style
})

const valueStyle = computed(() => {
  const style: Record<string, string> = {
    fontSize: `${sizeConfig.value.valueFontSize}px`
  }
  if (props.valueColor) {
    style.color = props.valueColor
  } else if (props.colorTheme !== 'default') {
    style.color = getThemeColor(props.colorTheme)
  }
  return style
})

const titleStyle = computed(() => ({
  fontSize: `${sizeConfig.value.titleFontSize}px`
}))

const iconStyle = computed(() => {
  const color = props.valueColor || getThemeColor(props.colorTheme)
  return {
    color,
    backgroundColor: `${color}15`
  }
})

// Methods
function getThemeColor(theme: string): string {
  const colors: Record<string, string> = {
    default: '#1890ff',
    primary: '#1890ff',
    positive: '#52c41a',
    negative: '#ff4d4f',
    neutral: '#666666',
    warning: '#faad14'
  }
  return colors[theme] || colors.default
}

function formatChange(change: number | null): string {
  return formatChangePercent(change, props.decimals)
}

function getChangeClass(change: number | null): string {
  if (change === null) return ''
  const isGood = props.positiveIsGood ? change > 0 : change < 0
  if (change > 0) return isGood ? 'change-good' : 'change-bad'
  if (change < 0) return isGood ? 'change-good' : 'change-bad'
  return 'change-neutral'
}

function handleClick() {
  if (!props.clickable) return
  
  const data = { value: props.value, title: props.title }
  emit('click', data)
  emit('drill-down', data)
}

// Sparkline chart
function initSparkline() {
  if (!sparklineRef.value || !props.sparklineData || props.sparklineData.length === 0) {
    return
  }

  if (sparklineChart) {
    sparklineChart.dispose()
  }

  sparklineChart = echarts.init(sparklineRef.value)
  
  const trendColor = getTrendColor(computedTrend.value, props.positiveIsGood)
  const options = buildSparklineOption(props.sparklineData, {
    color: trendColor,
    areaColor: `${trendColor}30`,
    ...props.sparklineOptions
  })
  
  sparklineChart.setOption(options)
}

function resizeSparkline() {
  nextTick(() => {
    sparklineChart?.resize()
  })
}

// Watchers
watch(
  () => [props.sparklineData, props.sparklineOptions, computedTrend.value],
  () => {
    nextTick(() => {
      initSparkline()
    })
  },
  { deep: true }
)

// Lifecycle
onMounted(() => {
  nextTick(() => {
    initSparkline()
  })
  window.addEventListener('resize', resizeSparkline)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeSparkline)
  if (sparklineChart) {
    sparklineChart.dispose()
    sparklineChart = null
  }
})

// Expose methods
defineExpose({
  refresh: initSparkline,
  resize: resizeSparkline
})
</script>

<style scoped>
.kpi-card {
  display: flex;
  flex-direction: column;
  padding: 16px;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  height: 100%;
  min-height: 120px;
}

.kpi-card--clickable {
  cursor: pointer;
}

.kpi-card--clickable:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

/* Size variants */
.kpi-card--small {
  padding: 12px;
  min-height: 100px;
}

.kpi-card--large {
  padding: 24px;
  min-height: 160px;
}

/* Color theme variants */
.kpi-card--positive {
  border-left: 4px solid #52c41a;
}

.kpi-card--negative {
  border-left: 4px solid #ff4d4f;
}

.kpi-card--warning {
  border-left: 4px solid #faad14;
}

.kpi-card--primary {
  border-left: 4px solid #1890ff;
}

/* Loading state */
.kpi-card__loading {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
}

/* Error state */
.kpi-card__error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  gap: 8px;
}

.kpi-card__error .error-text {
  font-size: 12px;
  color: #999;
  text-align: center;
}

/* Header */
.kpi-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.kpi-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
}

.kpi-card--small .kpi-card__icon {
  width: 24px;
  height: 24px;
}

.kpi-card--large .kpi-card__icon {
  width: 40px;
  height: 40px;
}

.kpi-card__title {
  font-size: 14px;
  color: #666;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Value */
.kpi-card__value {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 8px;
}

.kpi-card__prefix {
  font-size: 16px;
  color: #666;
}

.kpi-card__number {
  font-size: 36px;
  font-weight: 600;
  line-height: 1.2;
  color: #1890ff;
}

.kpi-card--small .kpi-card__number {
  font-size: 24px;
}

.kpi-card--large .kpi-card__number {
  font-size: 48px;
}

.kpi-card__unit {
  font-size: 14px;
  color: #666;
  margin-left: 4px;
}

.kpi-card__suffix {
  font-size: 14px;
  color: #666;
}

/* Trend */
.kpi-card__trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  margin-bottom: 8px;
}

.kpi-card__trend.trend-up {
  color: #52c41a;
}

.kpi-card__trend.trend-down {
  color: #ff4d4f;
}

.kpi-card__trend.trend-neutral {
  color: #999;
}

.kpi-card__trend.trend-bad.trend-up {
  color: #ff4d4f;
}

.kpi-card__trend.trend-bad.trend-down {
  color: #52c41a;
}

.trend-value {
  font-weight: 500;
}

.trend-label {
  font-size: 12px;
  color: #999;
  margin-left: 4px;
}

/* Comparison */
.kpi-card__comparison {
  display: flex;
  gap: 16px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.comparison-item {
  display: flex;
  flex-direction: column;
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

.comparison-value.change-good {
  color: #52c41a;
}

.comparison-value.change-bad {
  color: #ff4d4f;
}

.comparison-value.change-neutral {
  color: #999;
}

/* Sparkline */
.kpi-card__sparkline {
  flex: 1;
  min-height: 40px;
  margin-top: auto;
}

/* Responsive */
@media (max-width: 768px) {
  .kpi-card__number {
    font-size: 28px;
  }
  
  .kpi-card--large .kpi-card__number {
    font-size: 36px;
  }
}
</style>
