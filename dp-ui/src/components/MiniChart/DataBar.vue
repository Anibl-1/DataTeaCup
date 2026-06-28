<template>
  <div 
    class="mini-data-bar"
    :class="[
      `mini-data-bar--${effectiveConfig.direction}`,
      { 'mini-data-bar--negative': isNegative }
    ]"
    :style="containerStyle"
  >
    <!-- Track/Background -->
    <div class="mini-data-bar__track" :style="trackStyle">
      <!-- Positive bar -->
      <div 
        v-if="!isNegative"
        class="mini-data-bar__fill mini-data-bar__fill--positive"
        :style="fillStyle"
      />
      <!-- Negative bar -->
      <div 
        v-else
        class="mini-data-bar__fill mini-data-bar__fill--negative"
        :style="negativeFillStyle"
      />
    </div>
    
    <!-- Value label -->
    <span 
      v-if="effectiveConfig.showValue && effectiveConfig.valuePosition !== 'hidden'"
      class="mini-data-bar__value"
      :class="`mini-data-bar__value--${effectiveConfig.valuePosition}`"
      :style="valueStyle"
    >
      {{ formattedValue }}
    </span>
  </div>
</template>

<script setup lang="ts">
/**
 * DataBar Component - 数据条组件
 * 
 * 根据数值大小显示条形图，支持正负值双向显示
 * Displays a bar chart based on value magnitude, supports positive/negative bidirectional display
 * 
 * 参考: 帆软FineReport数据条、Excel条件格式数据条
 * Reference: FineReport data bars, Excel conditional formatting data bars
 * 
 * 需求: 14.4.16 - 数据条 - 根据数值大小显示条形图
 */

import { computed } from 'vue'
import type { DataBarConfig, DataBarProps } from './types'
import { DEFAULT_DATA_BAR_CONFIG, calculatePercentage } from './types'

const props = withDefaults(defineProps<DataBarProps>(), {
  config: () => ({}),
})

// Merge with default config
const effectiveConfig = computed(() => ({
  ...DEFAULT_DATA_BAR_CONFIG,
  ...props.config,
}))

// Check if value is negative
const isNegative = computed(() => props.value < 0)

// Calculate the absolute value for percentage calculation
const absValue = computed(() => Math.abs(props.value))

// Calculate percentage width
const percentage = computed(() => {
  const { min, max } = effectiveConfig.value
  // For bidirectional bars, we calculate based on absolute values
  const effectiveMin = Math.min(min, 0)
  const effectiveMax = Math.max(max, 0)
  const range = effectiveMax - effectiveMin
  
  if (range === 0) return 0
  
  return Math.min(100, (absValue.value / Math.max(Math.abs(effectiveMin), effectiveMax)) * 100)
})

// Format the displayed value
const formattedValue = computed(() => {
  const val = props.value
  if (Number.isInteger(val)) {
    return val.toLocaleString()
  }
  return val.toLocaleString(undefined, { maximumFractionDigits: 2 })
})

// Container style
const containerStyle = computed(() => ({
  '--data-bar-border-radius': `${effectiveConfig.value.borderRadius}px`,
}))

// Track style
const trackStyle = computed(() => ({
  backgroundColor: effectiveConfig.value.backgroundColor,
  borderRadius: `${effectiveConfig.value.borderRadius}px`,
}))

// Fill style for positive values
const fillStyle = computed(() => {
  const config = effectiveConfig.value
  const width = `${percentage.value}%`
  
  let background: string
  if (config.fillType === 'gradient') {
    background = `linear-gradient(90deg, ${config.color}, ${config.gradientEndColor})`
  } else {
    background = config.color
  }
  
  return {
    width,
    background,
    borderRadius: `${config.borderRadius}px`,
  }
})

// Fill style for negative values
const negativeFillStyle = computed(() => {
  const config = effectiveConfig.value
  const width = `${percentage.value}%`
  
  let background: string
  if (config.fillType === 'gradient') {
    background = `linear-gradient(90deg, ${config.negativeColor}, ${config.negativeColor}88)`
  } else {
    background = config.negativeColor
  }
  
  return {
    width,
    background,
    borderRadius: `${config.borderRadius}px`,
  }
})

// Value label style
const valueStyle = computed(() => {
  const config = effectiveConfig.value
  const isInside = config.valuePosition === 'inside'
  
  return {
    color: isInside ? '#fff' : (isNegative.value ? config.negativeColor : 'inherit'),
  }
})
</script>

<style scoped>
.mini-data-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-width: 60px;
  height: 100%;
  min-height: 16px;
}

.mini-data-bar--vertical {
  flex-direction: column;
  justify-content: flex-end;
}

.mini-data-bar__track {
  position: relative;
  flex: 1;
  height: 12px;
  overflow: hidden;
}

.mini-data-bar--vertical .mini-data-bar__track {
  width: 12px;
  height: 100%;
  flex: none;
}

.mini-data-bar__fill {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  transition: width 0.3s ease;
}

.mini-data-bar--vertical .mini-data-bar__fill {
  bottom: 0;
  top: auto;
  width: 100%;
  height: auto;
  transition: height 0.3s ease;
}

.mini-data-bar__fill--positive {
  /* Color set via inline style */
}

.mini-data-bar__fill--negative {
  /* Color set via inline style */
}

.mini-data-bar__value {
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  flex-shrink: 0;
}

.mini-data-bar__value--inside {
  position: absolute;
  right: 4px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 10px;
  text-shadow: 0 0 2px rgba(0, 0, 0, 0.3);
}

.mini-data-bar__value--outside {
  /* Default positioning via flex */
}

.mini-data-bar__value--hidden {
  display: none;
}

/* Negative value styling */
.mini-data-bar--negative .mini-data-bar__fill {
  right: 0;
  left: auto;
}
</style>
