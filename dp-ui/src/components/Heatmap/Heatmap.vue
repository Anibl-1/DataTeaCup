<template>
  <div 
    class="heatmap"
    :class="[
      `heatmap--legend-${effectiveLegend.position}`,
      { 'heatmap--has-x-labels': hasXLabels },
      { 'heatmap--has-y-labels': hasYLabels }
    ]"
    :style="containerStyle"
  >
    <!-- Y-axis labels -->
    <div v-if="hasYLabels" class="heatmap__y-labels">
      <div 
        v-for="(label, index) in effectiveYLabels" 
        :key="`y-${index}`"
        class="heatmap__y-label"
        :style="yLabelStyle"
        :title="label"
      >
        {{ label }}
      </div>
    </div>

    <!-- Main grid area -->
    <div class="heatmap__main">
      <!-- X-axis labels -->
      <div v-if="hasXLabels" class="heatmap__x-labels">
        <div 
          v-for="(label, index) in effectiveXLabels" 
          :key="`x-${index}`"
          class="heatmap__x-label"
          :style="xLabelStyle"
          :title="label"
        >
          {{ label }}
        </div>
      </div>

      <!-- Heatmap grid -->
      <div 
        class="heatmap__grid"
        :style="gridStyle"
      >
        <template v-for="(row, rowIndex) in normalizedData" :key="`row-${rowIndex}`">
          <div
            v-for="(value, colIndex) in row"
            :key="`cell-${rowIndex}-${colIndex}`"
            class="heatmap__cell"
            :style="getCellStyle(value, rowIndex, colIndex)"
            @mouseenter="handleCellHover(rowIndex, colIndex, value, $event)"
            @mouseleave="handleCellLeave"
          >
            <span 
              v-if="effectiveCell.showValue" 
              class="heatmap__cell-value"
              :style="getCellValueStyle(value)"
            >
              {{ formatCellValue(value) }}
            </span>
          </div>
        </template>
      </div>
    </div>

    <!-- Legend -->
    <HeatmapLegend
      v-if="effectiveLegend.show && effectiveLegend.position !== 'none'"
      :config="effectiveLegend"
      :color-scale="effectiveColorScale"
      :min="dataRange.min"
      :max="dataRange.max"
    />

    <!-- Tooltip -->
    <Teleport to="body">
      <div
        v-if="tooltipVisible && effectiveTooltip.show"
        class="heatmap__tooltip"
        :style="tooltipStyle"
      >
        <div v-html="tooltipContent" />
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
/**
 * Heatmap Component - 热力图组件
 * 
 * 基于数值大小显示颜色深浅的二维数据可视化组件
 * 2D data visualization component that displays color intensity based on value magnitude
 * 
 * 参考: Tableau/PowerBI 热力图设计
 * Reference: Tableau/PowerBI heatmap design
 * 
 * 需求: 22.1.1 - 支持数值热力图（基于数值大小显示颜色深浅）
 * 需求: 22.1.2 - 支持自定义热力图色阶（支持多色渐变）
 * 需求: 22.1.3 - 支持热力图图例显示
 */

import { computed, ref, reactive } from 'vue'
import type { 
  HeatmapProps, 
  ColorScaleConfig, 
  LegendConfig, 
  CellConfig,
  TooltipConfig,
  TooltipParams 
} from './types'
import {
  DEFAULT_COLOR_SCALE,
  DEFAULT_LEGEND_CONFIG,
  DEFAULT_CELL_CONFIG,
  DEFAULT_TOOLTIP_CONFIG,
  normalizeData,
  calculateDataRange,
  getColorForValue,
  getContrastColor,
  formatValue,
} from './types'
import HeatmapLegend from './HeatmapLegend.vue'

const props = withDefaults(defineProps<HeatmapProps>(), {
  xLabels: () => [],
  yLabels: () => [],
  colorScale: () => ({}),
  legend: () => ({}),
  cell: () => ({}),
  tooltip: () => ({}),
  nullColor: '#f3f4f6',
  nullText: '-',
})

// ============================================================================
// Computed Configurations
// ============================================================================

const effectiveColorScale = computed<ColorScaleConfig>(() => ({
  ...DEFAULT_COLOR_SCALE,
  ...props.colorScale,
}))

const effectiveLegend = computed<LegendConfig>(() => ({
  ...DEFAULT_LEGEND_CONFIG,
  ...props.legend,
}))

const effectiveCell = computed<CellConfig>(() => ({
  ...DEFAULT_CELL_CONFIG,
  ...props.cell,
}))

const effectiveTooltip = computed<TooltipConfig>(() => ({
  ...DEFAULT_TOOLTIP_CONFIG,
  ...props.tooltip,
}))

// ============================================================================
// Data Processing
// ============================================================================

const normalizedData = computed(() => normalizeData(props.data))

const dataRange = computed(() => {
  const range = calculateDataRange(normalizedData.value)
  return {
    min: effectiveColorScale.value.minValue ?? range.min,
    max: effectiveColorScale.value.maxValue ?? range.max,
  }
})

const rowCount = computed(() => normalizedData.value.length)
const colCount = computed(() => normalizedData.value[0]?.length ?? 0)

// ============================================================================
// Labels
// ============================================================================

const hasXLabels = computed(() => props.xLabels && props.xLabels.length > 0)
const hasYLabels = computed(() => props.yLabels && props.yLabels.length > 0)

const effectiveXLabels = computed(() => {
  if (props.xLabels && props.xLabels.length > 0) {
    return props.xLabels
  }
  return Array.from({ length: colCount.value }, (_, i) => String(i + 1))
})

const effectiveYLabels = computed(() => {
  if (props.yLabels && props.yLabels.length > 0) {
    return props.yLabels
  }
  return Array.from({ length: rowCount.value }, (_, i) => String(i + 1))
})

// ============================================================================
// Styles
// ============================================================================

const containerStyle = computed(() => {
  const style: Record<string, string> = {}
  
  if (props.width) {
    style.width = typeof props.width === 'number' ? `${props.width}px` : props.width
  }
  if (props.height) {
    style.height = typeof props.height === 'number' ? `${props.height}px` : props.height
  }
  
  return style
})

const gridStyle = computed(() => {
  const cell = effectiveCell.value
  return {
    gridTemplateColumns: `repeat(${colCount.value}, ${cell.width}px)`,
    gridTemplateRows: `repeat(${rowCount.value}, ${cell.height}px)`,
    gap: `${cell.gap}px`,
  }
})

const xLabelStyle = computed(() => ({
  width: `${effectiveCell.value.width}px`,
}))

const yLabelStyle = computed(() => ({
  height: `${effectiveCell.value.height}px`,
}))

// ============================================================================
// Cell Rendering
// ============================================================================

function getCellStyle(value: number | null, _rowIndex: number, _colIndex: number) {
  const cell = effectiveCell.value
  const backgroundColor = getColorForValue(
    value,
    effectiveColorScale.value,
    dataRange.value.min,
    dataRange.value.max,
    props.nullColor
  )
  
  return {
    backgroundColor,
    borderRadius: `${cell.borderRadius}px`,
    border: cell.borderWidth ? `${cell.borderWidth}px solid ${cell.borderColor}` : 'none',
  }
}

function getCellValueStyle(value: number | null) {
  const cell = effectiveCell.value
  
  let color = cell.fontColor
  if (!color && cell.autoContrast) {
    const bgColor = getColorForValue(
      value,
      effectiveColorScale.value,
      dataRange.value.min,
      dataRange.value.max,
      props.nullColor
    )
    color = getContrastColor(bgColor)
  }
  
  return {
    fontSize: `${cell.fontSize}px`,
    color,
  }
}

function formatCellValue(value: number | null): string {
  return formatValue(value, effectiveCell.value.valueFormat, props.nullText)
}

// ============================================================================
// Tooltip
// ============================================================================

const tooltipVisible = ref(false)
const tooltipPosition = reactive({ x: 0, y: 0 })
const tooltipContent = ref('')

const tooltipStyle = computed(() => ({
  left: `${tooltipPosition.x + 10}px`,
  top: `${tooltipPosition.y + 10}px`,
}))

function handleCellHover(
  rowIndex: number, 
  colIndex: number, 
  value: number | null,
  event: MouseEvent
) {
  if (!effectiveTooltip.value.show) return
  
  const params: TooltipParams = {
    rowIndex,
    colIndex,
    value,
    xLabel: effectiveXLabels.value[colIndex] ?? String(colIndex + 1),
    yLabel: effectiveYLabels.value[rowIndex] ?? String(rowIndex + 1),
  }
  
  if (effectiveTooltip.value.formatter) {
    tooltipContent.value = effectiveTooltip.value.formatter(params)
  } else {
    tooltipContent.value = defaultTooltipFormatter(params)
  }
  
  tooltipPosition.x = event.clientX
  tooltipPosition.y = event.clientY
  tooltipVisible.value = true
}

function handleCellLeave() {
  tooltipVisible.value = false
}

function defaultTooltipFormatter(params: TooltipParams): string {
  const valueStr = params.value !== null 
    ? formatValue(params.value, effectiveCell.value.valueFormat, props.nullText)
    : props.nullText
  
  return `
    <div class="heatmap-tooltip-content">
      <div><strong>${params.yLabel}</strong> × <strong>${params.xLabel}</strong></div>
      <div>值: ${valueStr}</div>
    </div>
  `
}
</script>

<style scoped>
.heatmap {
  display: inline-flex;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 12px;
  user-select: none;
}

/* Legend positioning */
.heatmap--legend-right {
  flex-direction: row;
}

.heatmap--legend-left {
  flex-direction: row-reverse;
}

.heatmap--legend-top {
  flex-direction: column-reverse;
}

.heatmap--legend-bottom {
  flex-direction: column;
}

/* Main area */
.heatmap__main {
  display: flex;
  flex-direction: column;
}

/* Y-axis labels */
.heatmap__y-labels {
  display: flex;
  flex-direction: column;
  margin-right: 8px;
  flex-shrink: 0;
}

.heatmap__y-label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100px;
  color: #374151;
}

/* X-axis labels */
.heatmap__x-labels {
  display: flex;
  flex-direction: row;
  margin-bottom: 4px;
}

.heatmap--has-y-labels .heatmap__x-labels {
  margin-left: 108px; /* 100px max-width + 8px margin */
}

.heatmap__x-label {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #374151;
  transform: rotate(-45deg);
  transform-origin: center;
  height: 40px;
}

/* Grid */
.heatmap__grid {
  display: grid;
}

/* Cells */
.heatmap__cell {
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.heatmap__cell:hover {
  transform: scale(1.05);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 1;
}

.heatmap__cell-value {
  font-variant-numeric: tabular-nums;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 2px;
}

/* Wrapper for y-labels + main content */
.heatmap--has-y-labels {
  flex-direction: row;
  flex-wrap: wrap;
}

.heatmap--has-y-labels.heatmap--legend-right,
.heatmap--has-y-labels.heatmap--legend-left {
  flex-wrap: nowrap;
}

.heatmap--has-y-labels > .heatmap__y-labels {
  order: 1;
}

.heatmap--has-y-labels > .heatmap__main {
  order: 2;
}

.heatmap--has-y-labels.heatmap--legend-right > :deep(.heatmap-legend) {
  order: 3;
}

.heatmap--has-y-labels.heatmap--legend-left > :deep(.heatmap-legend) {
  order: 0;
}
</style>

<style>
/* Global tooltip styles */
.heatmap__tooltip {
  position: fixed;
  z-index: 10000;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.85);
  color: #fff;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.5;
  pointer-events: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  max-width: 300px;
}

.heatmap-tooltip-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.heatmap-tooltip-content strong {
  color: #93c5fd;
}
</style>
