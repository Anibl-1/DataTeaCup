<template>
  <button
    ref="buttonRef"
    :class="[
      'hover-button',
      `hover-button--${type}`,
      `hover-button--${animationType}`,
      { 'hover-button--disabled': disabled }
    ]"
    :disabled="disabled"
    :style="buttonStyle"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
    @click="handleClick"
  >
    <span class="hover-button__content">
      <slot />
    </span>
    <span
      v-if="animationType === 'ripple'"
      ref="rippleRef"
      class="hover-button__ripple"
      :style="rippleStyle"
    />
  </button>
</template>

<script setup lang="ts">
/**
 * HoverButton - 悬停动效按钮
 * 
 * 支持三种动画类型：
 * - scale: 缩放效果
 * - glow: 发光效果
 * - ripple: 涟漪效果
 * 
 * Validates: Requirements 1.3
 * WHEN 用户悬停在按钮上时，THE Theme_Engine SHALL 显示平滑的悬停动画效果（过渡时间不超过200毫秒）
 */
import { ref, computed, type CSSProperties } from 'vue'
import { themeConfig } from '@/config/theme'

// Props 定义
export interface HoverButtonProps {
  /** 按钮类型 */
  type?: 'primary' | 'secondary' | 'default'
  /** 动画类型 */
  animationType?: 'scale' | 'glow' | 'ripple'
  /** 动画持续时间（毫秒），默认使用主题配置的200ms */
  duration?: number
  /** 是否禁用 */
  disabled?: boolean
}

const props = withDefaults(defineProps<HoverButtonProps>(), {
  type: 'default',
  animationType: 'scale',
  duration: undefined,
  disabled: false
})

// Emits 定义
const emit = defineEmits<{
  (e: 'click', event: MouseEvent): void
  (e: 'hover', isHovering: boolean): void
}>()

// Refs
const buttonRef = ref<HTMLButtonElement | null>(null)
const rippleRef = ref<HTMLSpanElement | null>(null)
const isHovering = ref(false)
const ripplePosition = ref({ x: 0, y: 0 })
const showRipple = ref(false)

// 从主题配置获取动画参数
const hoverConfig = themeConfig.animations.hover
const animationDuration = computed(() => 
  props.duration ? `${props.duration}ms` : hoverConfig.duration
)

// 按钮样式
const buttonStyle = computed<CSSProperties>(() => {
  const baseStyle: CSSProperties = {
    '--hover-duration': animationDuration.value,
    '--hover-easing': hoverConfig.easing,
    '--hover-scale': hoverConfig.scale,
    '--hover-opacity': hoverConfig.opacity,
    '--hover-translate-y': hoverConfig.translateY
  } as CSSProperties

  return baseStyle
})

// 涟漪样式
const rippleStyle = computed<CSSProperties>(() => ({
  left: `${ripplePosition.value.x}px`,
  top: `${ripplePosition.value.y}px`,
  opacity: showRipple.value ? 1 : 0
}))

// 事件处理
const handleMouseEnter = () => {
  if (props.disabled) return
  isHovering.value = true
  emit('hover', true)
}

const handleMouseLeave = () => {
  if (props.disabled) return
  isHovering.value = false
  showRipple.value = false
  emit('hover', false)
}

const handleClick = (event: MouseEvent) => {
  if (props.disabled) return
  
  // 涟漪效果位置计算
  if (props.animationType === 'ripple' && buttonRef.value) {
    const rect = buttonRef.value.getBoundingClientRect()
    ripplePosition.value = {
      x: event.clientX - rect.left,
      y: event.clientY - rect.top
    }
    showRipple.value = true
    
    // 动画结束后隐藏涟漪
    setTimeout(() => {
      showRipple.value = false
    }, parseInt(animationDuration.value))
  }
  
  emit('click', event)
}

// 暴露方法
defineExpose({
  isHovering,
  buttonRef
})
</script>

<style scoped>
.hover-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.5;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  overflow: hidden;
  transition: all var(--hover-duration, 200ms) var(--hover-easing, cubic-bezier(0.4, 0, 0.2, 1));
  outline: none;
}

.hover-button:focus-visible {
  box-shadow: 0 0 0 2px v-bind('themeConfig.accessibility.focusRingColor');
}

/* 按钮类型样式 */
.hover-button--default {
  background-color: #f5f5f5;
  color: #333;
  border-color: #d9d9d9;
}

.hover-button--primary {
  background-color: v-bind('themeConfig.colors.primary');
  color: #fff;
  border-color: v-bind('themeConfig.colors.primary');
}

.hover-button--secondary {
  background-color: transparent;
  color: v-bind('themeConfig.colors.primary');
  border-color: v-bind('themeConfig.colors.primary');
}

/* 禁用状态 */
.hover-button--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Scale 动画效果 */
.hover-button--scale:not(.hover-button--disabled):hover {
  transform: scale(var(--hover-scale, 1.02)) translateY(var(--hover-translate-y, -2px));
}

.hover-button--scale:not(.hover-button--disabled):active {
  transform: scale(0.98);
}

/* Glow 动画效果 */
.hover-button--glow:not(.hover-button--disabled):hover {
  box-shadow: 0 0 20px rgba(0, 102, 255, 0.4);
}

.hover-button--glow.hover-button--primary:not(.hover-button--disabled):hover {
  box-shadow: 0 0 20px v-bind('themeConfig.colors.primary + "66"');
}

.hover-button--glow.hover-button--secondary:not(.hover-button--disabled):hover {
  box-shadow: 0 0 20px v-bind('themeConfig.colors.primary + "33"');
}

/* Ripple 动画效果 */
.hover-button__ripple {
  position: absolute;
  width: 0;
  height: 0;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.4);
  transform: translate(-50%, -50%);
  pointer-events: none;
  animation: ripple-effect var(--hover-duration, 200ms) var(--hover-easing, cubic-bezier(0.4, 0, 0.2, 1));
}

@keyframes ripple-effect {
  0% {
    width: 0;
    height: 0;
    opacity: 0.5;
  }
  100% {
    width: 200px;
    height: 200px;
    opacity: 0;
  }
}

/* 内容容器 */
.hover-button__content {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
</style>
