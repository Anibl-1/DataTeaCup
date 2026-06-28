<template>
  <div 
    class="mini-icon-set"
    :style="containerStyle"
  >
    <!-- Icon -->
    <span 
      class="mini-icon-set__icon"
      :style="iconStyle"
      v-html="iconSvg"
    />
    
    <!-- Value (optional) -->
    <span 
      v-if="effectiveConfig.showValue"
      class="mini-icon-set__value"
    >
      {{ formattedValue }}
    </span>
  </div>
</template>

<script setup lang="ts">
/**
 * IconSet Component - 图标集组件
 * 
 * 根据阈值显示不同图标（箭头、星级、信号等）
 * Displays different icons based on thresholds (arrows, stars, signals, etc.)
 * 
 * 参考: 帆软FineReport图标集、Excel条件格式图标集、PowerBI图标
 * Reference: FineReport icon sets, Excel conditional formatting icons, PowerBI icons
 * 
 * 需求: 14.4.18 - 图标集 - 根据阈值显示不同图标（箭头、星级、信号等）
 */

import { computed } from 'vue'
import type { IconSetConfig, IconSetProps, IconSetItem } from './types'
import { DEFAULT_ICON_SET_CONFIG, getIconForValue } from './types'

const props = withDefaults(defineProps<IconSetProps>(), {
  config: () => ({}),
})

// Merge with default config
const effectiveConfig = computed(() => ({
  ...DEFAULT_ICON_SET_CONFIG,
  ...props.config,
}))

// Get the matching icon for the current value
const matchedIcon = computed((): IconSetItem | null => {
  return getIconForValue(props.value, effectiveConfig.value.icons)
})

// Format the displayed value
const formattedValue = computed(() => {
  const val = props.value
  if (effectiveConfig.value.valueFormat === 'percent') {
    return `${Math.round(val * 100)}%`
  }
  if (Number.isInteger(val)) {
    return val.toLocaleString()
  }
  return val.toLocaleString(undefined, { maximumFractionDigits: 2 })
})

// Get SVG for the icon
const iconSvg = computed(() => {
  const icon = matchedIcon.value
  if (!icon) return ''
  
  return getIconSvg(icon.icon, icon.color)
})

// Icon style
const iconStyle = computed(() => ({
  width: `${effectiveConfig.value.iconSize}px`,
  height: `${effectiveConfig.value.iconSize}px`,
}))

// Container style
const containerStyle = computed(() => ({
  gap: `${effectiveConfig.value.gap}px`,
}))

/**
 * Get SVG markup for an icon
 */
function getIconSvg(iconName: string, color: string): string {
  const size = effectiveConfig.value.iconSize
  
  // Built-in icon library
  const icons: Record<string, string> = {
    // Arrows
    'arrow-up': `<svg viewBox="0 0 24 24" fill="${color}"><path d="M12 4l-8 8h5v8h6v-8h5z"/></svg>`,
    'arrow-down': `<svg viewBox="0 0 24 24" fill="${color}"><path d="M12 20l8-8h-5V4H9v8H4z"/></svg>`,
    'arrow-right': `<svg viewBox="0 0 24 24" fill="${color}"><path d="M20 12l-8-8v5H4v6h8v5z"/></svg>`,
    'arrow-left': `<svg viewBox="0 0 24 24" fill="${color}"><path d="M4 12l8 8v-5h8V9h-8V4z"/></svg>`,
    
    // Circles (traffic lights)
    'circle-filled': `<svg viewBox="0 0 24 24" fill="${color}"><circle cx="12" cy="12" r="10"/></svg>`,
    'circle-empty': `<svg viewBox="0 0 24 24" fill="none" stroke="${color}" stroke-width="2"><circle cx="12" cy="12" r="9"/></svg>`,
    
    // Stars
    'star-filled': `<svg viewBox="0 0 24 24" fill="${color}"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>`,
    'star-half': `<svg viewBox="0 0 24 24" fill="${color}"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77V2z"/><path d="M12 2v15.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" fill="none" stroke="${color}" stroke-width="1"/></svg>`,
    'star-empty': `<svg viewBox="0 0 24 24" fill="none" stroke="${color}" stroke-width="1.5"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>`,
    
    // Flags
    'flag': `<svg viewBox="0 0 24 24" fill="${color}"><path d="M4 2v20h2v-8h14l-4-6 4-6H4z"/></svg>`,
    
    // Ratings (bars)
    'rating-5': `<svg viewBox="0 0 24 24" fill="${color}"><rect x="2" y="4" width="4" height="16" rx="1"/><rect x="7" y="4" width="4" height="16" rx="1"/><rect x="12" y="4" width="4" height="16" rx="1"/><rect x="17" y="4" width="4" height="16" rx="1"/></svg>`,
    'rating-4': `<svg viewBox="0 0 24 24"><rect x="2" y="4" width="4" height="16" rx="1" fill="${color}"/><rect x="7" y="4" width="4" height="16" rx="1" fill="${color}"/><rect x="12" y="4" width="4" height="16" rx="1" fill="${color}"/><rect x="17" y="4" width="4" height="16" rx="1" fill="#e0e0e0"/></svg>`,
    'rating-3': `<svg viewBox="0 0 24 24"><rect x="2" y="4" width="4" height="16" rx="1" fill="${color}"/><rect x="7" y="4" width="4" height="16" rx="1" fill="${color}"/><rect x="12" y="4" width="4" height="16" rx="1" fill="#e0e0e0"/><rect x="17" y="4" width="4" height="16" rx="1" fill="#e0e0e0"/></svg>`,
    'rating-2': `<svg viewBox="0 0 24 24"><rect x="2" y="4" width="4" height="16" rx="1" fill="${color}"/><rect x="7" y="4" width="4" height="16" rx="1" fill="#e0e0e0"/><rect x="12" y="4" width="4" height="16" rx="1" fill="#e0e0e0"/><rect x="17" y="4" width="4" height="16" rx="1" fill="#e0e0e0"/></svg>`,
    'rating-1': `<svg viewBox="0 0 24 24"><rect x="2" y="4" width="4" height="16" rx="1" fill="${color}"/><rect x="7" y="4" width="4" height="16" rx="1" fill="#e0e0e0"/><rect x="12" y="4" width="4" height="16" rx="1" fill="#e0e0e0"/><rect x="17" y="4" width="4" height="16" rx="1" fill="#e0e0e0"/></svg>`,
    
    // Signals
    'signal-full': `<svg viewBox="0 0 24 24" fill="${color}"><rect x="2" y="16" width="4" height="6" rx="1"/><rect x="8" y="12" width="4" height="10" rx="1"/><rect x="14" y="8" width="4" height="14" rx="1"/><rect x="20" y="4" width="4" height="18" rx="1"/></svg>`,
    'signal-high': `<svg viewBox="0 0 24 24"><rect x="2" y="16" width="4" height="6" rx="1" fill="${color}"/><rect x="8" y="12" width="4" height="10" rx="1" fill="${color}"/><rect x="14" y="8" width="4" height="14" rx="1" fill="${color}"/><rect x="20" y="4" width="4" height="18" rx="1" fill="#e0e0e0"/></svg>`,
    'signal-medium': `<svg viewBox="0 0 24 24"><rect x="2" y="16" width="4" height="6" rx="1" fill="${color}"/><rect x="8" y="12" width="4" height="10" rx="1" fill="${color}"/><rect x="14" y="8" width="4" height="14" rx="1" fill="#e0e0e0"/><rect x="20" y="4" width="4" height="18" rx="1" fill="#e0e0e0"/></svg>`,
    'signal-low': `<svg viewBox="0 0 24 24"><rect x="2" y="16" width="4" height="6" rx="1" fill="${color}"/><rect x="8" y="12" width="4" height="10" rx="1" fill="#e0e0e0"/><rect x="14" y="8" width="4" height="14" rx="1" fill="#e0e0e0"/><rect x="20" y="4" width="4" height="18" rx="1" fill="#e0e0e0"/></svg>`,
    
    // Quarters (pie segments)
    'quarter-full': `<svg viewBox="0 0 24 24" fill="${color}"><circle cx="12" cy="12" r="10"/></svg>`,
    'quarter-three': `<svg viewBox="0 0 24 24"><path d="M12 2a10 10 0 1 1 0 20V12h10a10 10 0 0 0-10-10z" fill="${color}"/><path d="M12 2v10h10a10 10 0 0 0-10-10z" fill="#e0e0e0"/></svg>`,
    'quarter-half': `<svg viewBox="0 0 24 24"><path d="M12 2a10 10 0 0 1 0 20V2z" fill="${color}"/><path d="M12 2a10 10 0 0 0 0 20V2z" fill="#e0e0e0"/></svg>`,
    'quarter-one': `<svg viewBox="0 0 24 24"><path d="M12 2v10h10a10 10 0 0 0-10-10z" fill="${color}"/><path d="M12 2a10 10 0 1 0 10 10H12V2z" fill="#e0e0e0"/></svg>`,
    
    // Checkmarks
    'check': `<svg viewBox="0 0 24 24" fill="${color}"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>`,
    'cross': `<svg viewBox="0 0 24 24" fill="${color}"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>`,
    
    // Triangles
    'triangle-up': `<svg viewBox="0 0 24 24" fill="${color}"><path d="M12 4L4 20h16z"/></svg>`,
    'triangle-down': `<svg viewBox="0 0 24 24" fill="${color}"><path d="M12 20L4 4h16z"/></svg>`,
  }
  
  // Check if it's a built-in icon
  if (icons[iconName]) {
    return icons[iconName]
  }
  
  // Check if it's already SVG content
  if (iconName.startsWith('<svg')) {
    return iconName.replace(/fill="[^"]*"/g, `fill="${color}"`)
  }
  
  // Fallback to circle
  return icons['circle-filled']
}
</script>

<style scoped>
.mini-icon-set {
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
}

.mini-icon-set__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.mini-icon-set__icon :deep(svg) {
  width: 100%;
  height: 100%;
}

.mini-icon-set__value {
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
</style>
