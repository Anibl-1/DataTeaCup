<template>
  <div 
    class="mini-progress-bar"
    :class="[
      `mini-progress-bar--label-${effectiveConfig.labelPosition}`,
      { 
        'mini-progress-bar--striped': effectiveConfig.striped,
        'mini-progress-bar--animated': effectiveConfig.animated 
      }
    ]"
  >
    <!-- Label (top position) -->
    <span 
      v-if="effectiveConfig.showLabel && effectiveConfig.labelPosition === 'top'"
      class="mini-progress-bar__label mini-progress-bar__label--top"
    >
      {{ formattedLabel }}
    </span>
    
    <!-- Progress track -->
    <div 
      class="mini-progress-bar__track"
      :style="trackStyle"
    >
      <!-- Progress fill -->
      <div 
        class="mini-progress-bar__fill"
        :style="fillStyle"
      >
        <!-- Striped overlay -->
        <div 
          v-if="effectiveConfig.striped"
          class="mini-progress-bar__stripes"
        />
        
        <!-- Label (inside position) -->
        <span 
          v-if="effectiveConfig.showLabel && effectiveConfig.labelPosition === 'inside' && percentage >= 20"
          class="mini-progress-bar__label mini-progress-bar__label--inside"
        >
          {{ formattedLabel }}
        </span>
      </div>
      
      <!-- Segment markers (for multi-segment progress) -->
      <template v-if="hasSegments">
        <div
          v-for="(segment, index) in segmentMarkers"
          :key="index"
          class="mini-progress-bar__segment-marker"
          :style="{ left: `${segment.position}%` }"
        />
      </template>
    </div>
    
    <!-- Label (outside/bottom position) -->
    <span 
      v-if="effectiveConfig.showLabel && (effectiveConfig.labelPosition === 'outside' || effectiveConfig.labelPosition === 'bottom')"
      class="mini-progress-bar__label"
      :class="`mini-progress-bar__label--${effectiveConfig.labelPosition}`"
    >
      {{ formattedLabel }}
    </span>
  </div>
</template>

<script setup lang="ts">
/**
 * ProgressBar Component - 进度条组件
 * 
 * 显示完成百分比，支持条纹、动画、分段等效果
 * Displays completion percentage, supports stripes, animation, segments, etc.
 * 
 * 参考: 帆软FineReport进度条、Bootstrap进度条
 * Reference: FineReport progress bars, Bootstrap progress bars
 * 
 * 需求: 14.4.19 - 进度条 - 显示完成百分比
 */

import { computed } from 'vue'
import type { ProgressBarConfig, ProgressBarProps, ProgressSegment } from './types'
import { DEFAULT_PROGRESS_BAR_CONFIG, formatProgressLabel } from './types'

const props = withDefaults(defineProps<ProgressBarProps>(), {
  config: () => ({}),
})

// Merge with default config
const effectiveConfig = computed(() => ({
  ...DEFAULT_PROGRESS_BAR_CONFIG,
  ...props.config,
}))

// Calculate percentage
const percentage = computed(() => {
  const max = effectiveConfig.value.max
  if (max <= 0) return 0
  return Math.min(100, Math.max(0, (props.value / max) * 100))
})

// Format the label
const formattedLabel = computed(() => {
  return formatProgressLabel(
    props.value,
    effectiveConfig.value.max,
    effectiveConfig.value.labelFormat
  )
})

// Check if has segments
const hasSegments = computed(() => {
  return effectiveConfig.value.segments && effectiveConfig.value.segments.length > 0
})

// Calculate segment markers
const segmentMarkers = computed(() => {
  const segments = effectiveConfig.value.segments
  if (!segments || segments.length === 0) return []
  
  const max = effectiveConfig.value.max
  return segments.map(segment => ({
    position: (segment.value / max) * 100,
    color: segment.color,
  }))
})

// Get fill color based on segments or default
const fillColor = computed(() => {
  const segments = effectiveConfig.value.segments
  if (!segments || segments.length === 0) {
    return effectiveConfig.value.color
  }
  
  // Find the appropriate segment color based on current value
  const sortedSegments = [...segments].sort((a, b) => b.value - a.value)
  for (const segment of sortedSegments) {
    if (props.value >= segment.value) {
      return segment.color
    }
  }
  
  return effectiveConfig.value.color
})

// Track style
const trackStyle = computed(() => ({
  backgroundColor: effectiveConfig.value.trackColor,
  borderRadius: `${effectiveConfig.value.borderRadius}px`,
  height: `${effectiveConfig.value.height}px`,
}))

// Fill style
const fillStyle = computed(() => ({
  width: `${percentage.value}%`,
  backgroundColor: fillColor.value,
  borderRadius: `${effectiveConfig.value.borderRadius}px`,
}))
</script>

<style scoped>
.mini-progress-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-width: 60px;
}

.mini-progress-bar--label-top,
.mini-progress-bar--label-bottom {
  flex-direction: column;
  align-items: stretch;
  gap: 4px;
}

.mini-progress-bar__track {
  position: relative;
  flex: 1;
  overflow: hidden;
  min-width: 40px;
}

.mini-progress-bar--label-top .mini-progress-bar__track,
.mini-progress-bar--label-bottom .mini-progress-bar__track {
  width: 100%;
}

.mini-progress-bar__fill {
  position: relative;
  height: 100%;
  transition: width 0.3s ease, background-color 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  overflow: hidden;
}

.mini-progress-bar__stripes {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: linear-gradient(
    45deg,
    rgba(255, 255, 255, 0.15) 25%,
    transparent 25%,
    transparent 50%,
    rgba(255, 255, 255, 0.15) 50%,
    rgba(255, 255, 255, 0.15) 75%,
    transparent 75%,
    transparent
  );
  background-size: 16px 16px;
}

.mini-progress-bar--animated .mini-progress-bar__stripes {
  animation: progress-bar-stripes 1s linear infinite;
}

@keyframes progress-bar-stripes {
  from {
    background-position: 16px 0;
  }
  to {
    background-position: 0 0;
  }
}

.mini-progress-bar__label {
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  flex-shrink: 0;
}

.mini-progress-bar__label--inside {
  color: #fff;
  padding-right: 4px;
  text-shadow: 0 0 2px rgba(0, 0, 0, 0.3);
  font-size: 10px;
}

.mini-progress-bar__label--outside {
  color: inherit;
}

.mini-progress-bar__label--top,
.mini-progress-bar__label--bottom {
  text-align: center;
}

.mini-progress-bar__segment-marker {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 1px;
  background-color: rgba(0, 0, 0, 0.2);
  pointer-events: none;
}
</style>
