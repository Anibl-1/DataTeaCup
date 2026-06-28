<template>
  <div 
    class="heatmap-legend"
    :class="[
      `heatmap-legend--${config.position}`,
      { 'heatmap-legend--horizontal': isHorizontal },
      { 'heatmap-legend--vertical': !isHorizontal }
    ]"
    :style="legendContainerStyle"
  >
    <!-- Legend title -->
    <div v-if="config.title" class="heatmap-legend__title">
      {{ config.title }}
    </div>

    <!-- Legend bar with gradient -->
    <div class="heatmap-legend__bar-container">
      <!-- Max label (top/left for vertical, right for horizontal) -->
      <span 
        v-if="!isHorizontal" 
        class="heatmap-legend__label heatmap-legend__label--max"
      >
        {{ formatTickValue(max) }}
      </span>

      <!-- Gradient bar -->
      <div 
        class="heatmap-legend__bar"
        :style="barStyle"
      />

      <!-- Min label (bottom/right for vertical, left for horizontal) -->
      <span 
        v-if="!isHorizontal" 
        class="heatmap-legend__label heatmap-legend__label--min"
      >
        {{ formatTickValue(min) }}
      </span>
    </div>

    <!-- Tick marks for horizontal legend -->
    <div v-if="isHorizontal" class="heatmap-legend__ticks">
      <span 
        v-for="(tick, index) in ticks" 
        :key="index"
        class="heatmap-legend__tick"
        :style="getTickStyle(tick)"
      >
        {{ formatTickValue(tick) }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * HeatmapLegend Component - 热力图图例组件
 * 
 * 显示色阶范围和数值映射的图例
 * Legend component showing color gradient and value range
 * 
 * 需求: 22.1.3 - 支持热力图图例显示
 */

import { computed } from 'vue'
import type { LegendConfig, ColorScaleConfig } from './types'
import { 
  generateLegendTicks, 
  formatValue,
  interpolateColor,
  interpolateColorStops,
} from './types'

interface Props {
  config: LegendConfig
  colorScale: ColorScaleConfig
  min: number
  max: number
}

const props = defineProps<Props>()

// ============================================================================
// Computed Properties
// ============================================================================

const isHorizontal = computed(() => 
  props.config.position === 'top' || props.config.position === 'bottom'
)

const ticks = computed(() => 
  generateLegendTicks(
    props.min, 
    props.max, 
    props.config.ticks ?? 5,
    props.config.tickValues
  )
)

// ============================================================================
// Styles
// ============================================================================

const legendContainerStyle = computed(() => {
  const style: Record<string, string> = {}
  
  if (isHorizontal.value) {
    style.width = '100%'
    style.height = `${(props.config.size ?? 20) + 30}px`
  } else {
    style.width = `${(props.config.size ?? 20) + 50}px`
    style.height = '100%'
    style.minHeight = '100px'
  }
  
  return style
})

const barStyle = computed(() => {
  const gradient = generateGradient()
  
  if (isHorizontal.value) {
    return {
      background: `linear-gradient(to right, ${gradient})`,
      height: `${props.config.size ?? 20}px`,
      width: '100%',
    }
  } else {
    return {
      background: `linear-gradient(to top, ${gradient})`,
      width: `${props.config.size ?? 20}px`,
      height: '100%',
      minHeight: '80px',
    }
  }
})

// ============================================================================
// Helper Functions
// ============================================================================

function generateGradient(): string {
  const { colorScale } = props
  
  // Use custom color stops if provided
  if (colorScale.colorStops && colorScale.colorStops.length >= 2) {
    return colorScale.colorStops
      .map(stop => `${stop.color} ${stop.position * 100}%`)
      .join(', ')
  }
  
  // Use 3-color scale if midColor is provided
  if (colorScale.midColor) {
    const midPosition = colorScale.midValue !== undefined
      ? ((colorScale.midValue - props.min) / (props.max - props.min)) * 100
      : 50
    
    return `${colorScale.minColor} 0%, ${colorScale.midColor} ${midPosition}%, ${colorScale.maxColor} 100%`
  }
  
  // Use 2-color scale
  return `${colorScale.minColor} 0%, ${colorScale.maxColor} 100%`
}

function formatTickValue(value: number): string {
  const format = props.config.valueFormat
  
  if (typeof format === 'function') {
    return format(value)
  }
  
  if (typeof format === 'string') {
    return format.replace('{value}', value.toLocaleString())
  }
  
  // Default formatting
  if (Math.abs(value) >= 1000000) {
    return `${(value / 1000000).toFixed(1)}M`
  }
  if (Math.abs(value) >= 1000) {
    return `${(value / 1000).toFixed(1)}K`
  }
  if (Number.isInteger(value)) {
    return value.toLocaleString()
  }
  return value.toLocaleString(undefined, { maximumFractionDigits: 2 })
}

function getTickStyle(tick: number) {
  const position = ((tick - props.min) / (props.max - props.min)) * 100
  return {
    left: `${position}%`,
  }
}
</script>

<style scoped>
.heatmap-legend {
  display: flex;
  flex-direction: column;
  padding: 8px;
  flex-shrink: 0;
}

/* Horizontal legend (top/bottom) */
.heatmap-legend--horizontal {
  flex-direction: column;
  align-items: stretch;
}

.heatmap-legend--horizontal .heatmap-legend__bar-container {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.heatmap-legend--horizontal .heatmap-legend__bar {
  border-radius: 2px;
}

.heatmap-legend--horizontal .heatmap-legend__ticks {
  position: relative;
  height: 20px;
  margin-top: 4px;
}

.heatmap-legend--horizontal .heatmap-legend__tick {
  position: absolute;
  transform: translateX(-50%);
  font-size: 10px;
  color: #6b7280;
  white-space: nowrap;
}

/* Vertical legend (left/right) */
.heatmap-legend--vertical {
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.heatmap-legend--vertical .heatmap-legend__bar-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  gap: 4px;
}

.heatmap-legend--vertical .heatmap-legend__bar {
  border-radius: 2px;
  flex: 1;
}

.heatmap-legend--vertical .heatmap-legend__label {
  font-size: 10px;
  color: #6b7280;
  white-space: nowrap;
}

/* Position-specific styles */
.heatmap-legend--left {
  margin-right: 8px;
}

.heatmap-legend--right {
  margin-left: 8px;
}

.heatmap-legend--top {
  margin-bottom: 8px;
}

.heatmap-legend--bottom {
  margin-top: 8px;
}

/* Title */
.heatmap-legend__title {
  font-size: 11px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 4px;
  text-align: center;
}
</style>
