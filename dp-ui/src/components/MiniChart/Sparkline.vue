<template>
  <div 
    class="mini-sparkline"
    :style="containerStyle"
  >
    <svg
      :width="effectiveWidth"
      :height="effectiveHeight"
      :viewBox="`0 0 ${effectiveWidth} ${effectiveHeight}`"
      class="mini-sparkline__svg"
    >
      <!-- Background -->
      <rect
        v-if="effectiveConfig.backgroundColor !== 'transparent'"
        x="0"
        y="0"
        :width="effectiveWidth"
        :height="effectiveHeight"
        :fill="effectiveConfig.backgroundColor"
      />
      
      <!-- Area fill (for line/area type) -->
      <path
        v-if="showArea && areaPath"
        :d="areaPath"
        :fill="effectiveConfig.fillColor"
        class="mini-sparkline__area"
      />
      
      <!-- Line path (for line/area type) -->
      <path
        v-if="isLineType && linePath"
        :d="linePath"
        :stroke="effectiveConfig.color"
        :stroke-width="effectiveConfig.strokeWidth"
        fill="none"
        stroke-linecap="round"
        stroke-linejoin="round"
        class="mini-sparkline__line"
      />
      
      <!-- Bar chart (for bar type) -->
      <g v-if="effectiveConfig.type === 'bar'">
        <rect
          v-for="(bar, index) in bars"
          :key="index"
          :x="bar.x"
          :y="bar.y"
          :width="bar.width"
          :height="bar.height"
          :fill="bar.fill"
          rx="1"
          class="mini-sparkline__bar"
        />
      </g>
      
      <!-- Data points -->
      <g v-if="effectiveConfig.showPoints && isLineType">
        <circle
          v-for="(point, index) in points"
          :key="index"
          :cx="point.x"
          :cy="point.y"
          :r="effectiveConfig.pointRadius"
          :fill="getPointColor(index)"
          class="mini-sparkline__point"
        />
      </g>
      
      <!-- Highlight min/max points -->
      <g v-if="(effectiveConfig.highlightMin || effectiveConfig.highlightMax) && isLineType">
        <circle
          v-if="effectiveConfig.highlightMin && minPointIndex !== -1"
          :cx="points[minPointIndex]?.x"
          :cy="points[minPointIndex]?.y"
          :r="effectiveConfig.pointRadius + 1"
          :fill="effectiveConfig.minColor"
          class="mini-sparkline__point mini-sparkline__point--min"
        />
        <circle
          v-if="effectiveConfig.highlightMax && maxPointIndex !== -1"
          :cx="points[maxPointIndex]?.x"
          :cy="points[maxPointIndex]?.y"
          :r="effectiveConfig.pointRadius + 1"
          :fill="effectiveConfig.maxColor"
          class="mini-sparkline__point mini-sparkline__point--max"
        />
      </g>
    </svg>
  </div>
</template>

<script setup lang="ts">
/**
 * Sparkline Component - 迷你折线图组件
 * 
 * 在单元格内显示趋势线，支持折线图、面积图、柱状图
 * Displays trend lines within cells, supports line, area, and bar charts
 * 
 * 参考: 帆软FineReport迷你图、PowerBI迷你图
 * Reference: FineReport mini charts, PowerBI sparklines
 * 
 * 需求: 14.4.17 - 迷你折线图 - 在单元格内显示趋势线
 */

import { computed } from 'vue'
import type { SparklineConfig, SparklineProps } from './types'
import { 
  DEFAULT_SPARKLINE_CONFIG, 
  calculateSparklinePoints, 
  generateSparklinePath,
  generateAreaPath 
} from './types'

const props = withDefaults(defineProps<SparklineProps>(), {
  config: () => ({}),
  width: 80,
  height: 24,
})

// Merge with default config
const effectiveConfig = computed(() => ({
  ...DEFAULT_SPARKLINE_CONFIG,
  ...props.config,
}))

// Effective dimensions
const effectiveWidth = computed(() => props.width)
const effectiveHeight = computed(() => props.height)

// Check if line type (line or area)
const isLineType = computed(() => 
  effectiveConfig.value.type === 'line' || effectiveConfig.value.type === 'area'
)

// Show area fill
const showArea = computed(() => 
  effectiveConfig.value.type === 'area' || effectiveConfig.value.showArea
)

// Calculate min/max Y values
const dataMinY = computed(() => {
  if (props.data.length === 0) return 0
  return Math.min(...props.data)
})

const dataMaxY = computed(() => {
  if (props.data.length === 0) return 100
  return Math.max(...props.data)
})

const effectiveMinY = computed(() => {
  const configMin = effectiveConfig.value.minY
  // If config min is default (0), use data min with some padding
  if (configMin === DEFAULT_SPARKLINE_CONFIG.minY) {
    return Math.min(0, dataMinY.value)
  }
  return configMin
})

const effectiveMaxY = computed(() => {
  const configMax = effectiveConfig.value.maxY
  // If config max is default (100), use data max with some padding
  if (configMax === DEFAULT_SPARKLINE_CONFIG.maxY) {
    const max = dataMaxY.value
    const padding = (max - effectiveMinY.value) * 0.1
    return max + padding
  }
  return configMax
})

// Calculate points for line/area chart
const points = computed(() => {
  if (props.data.length === 0) return []
  
  return calculateSparklinePoints(
    props.data,
    effectiveWidth.value,
    effectiveHeight.value,
    effectiveMinY.value,
    effectiveMaxY.value,
    2 // padding
  )
})

// Generate line path
const linePath = computed(() => {
  return generateSparklinePath(points.value, effectiveConfig.value.smooth)
})

// Generate area path
const areaPath = computed(() => {
  return generateAreaPath(points.value, effectiveHeight.value, effectiveConfig.value.smooth)
})

// Find min/max point indices
const minPointIndex = computed(() => {
  if (props.data.length === 0) return -1
  let minIndex = 0
  let minValue = props.data[0]
  props.data.forEach((val, idx) => {
    if (val < minValue) {
      minValue = val
      minIndex = idx
    }
  })
  return minIndex
})

const maxPointIndex = computed(() => {
  if (props.data.length === 0) return -1
  let maxIndex = 0
  let maxValue = props.data[0]
  props.data.forEach((val, idx) => {
    if (val > maxValue) {
      maxValue = val
      maxIndex = idx
    }
  })
  return maxIndex
})

// Calculate bars for bar chart
const bars = computed(() => {
  if (props.data.length === 0) return []
  
  const config = effectiveConfig.value
  const padding = 2
  const gap = 1
  const availableWidth = effectiveWidth.value - padding * 2
  const barWidth = Math.max(2, (availableWidth - gap * (props.data.length - 1)) / props.data.length)
  const range = effectiveMaxY.value - effectiveMinY.value || 1
  const zeroY = effectiveHeight.value - padding - ((0 - effectiveMinY.value) / range) * (effectiveHeight.value - padding * 2)
  
  return props.data.map((value, index) => {
    const x = padding + index * (barWidth + gap)
    const normalizedValue = (value - effectiveMinY.value) / range
    const barHeight = Math.abs(normalizedValue * (effectiveHeight.value - padding * 2))
    const isNegative = value < 0
    
    return {
      x,
      y: isNegative ? zeroY : effectiveHeight.value - padding - barHeight,
      width: barWidth,
      height: Math.max(1, barHeight),
      fill: isNegative ? config.negativeColor : config.color,
    }
  })
})

// Get point color (for highlighting)
function getPointColor(index: number): string {
  const config = effectiveConfig.value
  if (config.highlightMin && index === minPointIndex.value) {
    return config.minColor
  }
  if (config.highlightMax && index === maxPointIndex.value) {
    return config.maxColor
  }
  return config.color
}

// Container style
const containerStyle = computed(() => ({
  width: `${effectiveWidth.value}px`,
  height: `${effectiveHeight.value}px`,
}))
</script>

<style scoped>
.mini-sparkline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.mini-sparkline__svg {
  display: block;
}

.mini-sparkline__line {
  transition: stroke 0.2s ease;
}

.mini-sparkline__area {
  transition: fill 0.2s ease;
}

.mini-sparkline__bar {
  transition: fill 0.2s ease;
}

.mini-sparkline__point {
  transition: fill 0.2s ease, r 0.2s ease;
}

.mini-sparkline__point--min,
.mini-sparkline__point--max {
  /* Highlighted points */
}
</style>
