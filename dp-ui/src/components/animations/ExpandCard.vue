<template>
  <div
    :class="[
      'expand-card',
      { 'expand-card--expanded': modelValue }
    ]"
    :style="cardStyle"
  >
    <!-- 卡片头部 -->
    <div
      class="expand-card__header"
      role="button"
      tabindex="0"
      :aria-expanded="modelValue"
      @click="toggleExpand"
      @keydown.enter="toggleExpand"
      @keydown.space.prevent="toggleExpand"
    >
      <div class="expand-card__header-content">
        <slot name="header">
          <span class="expand-card__title">{{ title }}</span>
        </slot>
      </div>
      <span
        class="expand-card__icon"
        :class="{ 'expand-card__icon--rotated': modelValue }"
      >
        <slot name="icon">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <polyline points="6 9 12 15 18 9" />
          </svg>
        </slot>
      </span>
    </div>

    <!-- 卡片内容 - 展开/收起动画 -->
    <div
      ref="contentRef"
      class="expand-card__content-wrapper"
      :style="contentWrapperStyle"
    >
      <div ref="innerContentRef" class="expand-card__content">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * ExpandCard - 展开动效卡片
 * 
 * 支持平滑的展开/收起动画效果
 * 
 * Validates: Requirements 1.4
 * WHEN 用户展开或收起卡片组件时，THE Theme_Engine SHALL 显示平滑的展开/收起动画
 */
import { ref, computed, watch, onMounted, nextTick, type CSSProperties } from 'vue'
import { themeConfig } from '@/config/theme'

// Props 定义
export interface ExpandCardProps {
  /** 展开状态 (v-model) */
  modelValue?: boolean
  /** 卡片标题 */
  title?: string
  /** 动画持续时间（毫秒），默认使用主题配置的300ms */
  animationDuration?: number
  /** 是否禁用动画 */
  disableAnimation?: boolean
}

const props = withDefaults(defineProps<ExpandCardProps>(), {
  modelValue: false,
  title: '',
  animationDuration: undefined,
  disableAnimation: false
})

// Emits 定义
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'expand'): void
  (e: 'collapse'): void
}>()

// Refs
const contentRef = ref<HTMLDivElement | null>(null)
const innerContentRef = ref<HTMLDivElement | null>(null)
const contentHeight = ref(0)
const isAnimating = ref(false)

// 从主题配置获取动画参数
const expandConfig = themeConfig.animations.expand
const animationDuration = computed(() => 
  props.animationDuration ? `${props.animationDuration}ms` : expandConfig.duration
)

// 卡片样式
const cardStyle = computed<CSSProperties>(() => ({
  '--expand-duration': animationDuration.value,
  '--expand-easing': expandConfig.easing
} as CSSProperties))

// 内容包装器样式
const contentWrapperStyle = computed<CSSProperties>(() => {
  if (props.disableAnimation) {
    return {
      height: props.modelValue ? 'auto' : '0',
      overflow: 'hidden'
    }
  }

  return {
    height: props.modelValue ? `${contentHeight.value}px` : '0',
    overflow: 'hidden',
    transition: `height ${animationDuration.value} ${expandConfig.easing}`
  }
})

// 计算内容高度
const updateContentHeight = async () => {
  await nextTick()
  if (innerContentRef.value) {
    contentHeight.value = innerContentRef.value.offsetHeight
  }
}

// 切换展开状态
const toggleExpand = () => {
  if (isAnimating.value) return
  
  const newValue = !props.modelValue
  emit('update:modelValue', newValue)
  
  if (newValue) {
    emit('expand')
  } else {
    emit('collapse')
  }
  
  // 标记动画状态
  if (!props.disableAnimation) {
    isAnimating.value = true
    setTimeout(() => {
      isAnimating.value = false
    }, parseInt(animationDuration.value))
  }
}

// 监听展开状态变化，更新内容高度
watch(() => props.modelValue, async (newValue) => {
  if (newValue) {
    await updateContentHeight()
  }
}, { immediate: true })

// 监听内容变化，更新高度
onMounted(() => {
  updateContentHeight()
  
  // 使用 ResizeObserver 监听内容尺寸变化
  if (innerContentRef.value && typeof ResizeObserver !== 'undefined') {
    const observer = new ResizeObserver(() => {
      if (props.modelValue) {
        updateContentHeight()
      }
    })
    observer.observe(innerContentRef.value)
  }
})

// 暴露方法
defineExpose({
  toggleExpand,
  updateContentHeight,
  isAnimating
})
</script>

<style scoped>
.expand-card {
  background-color: v-bind('themeConfig.colors.background.primary');
  border: 1px solid v-bind('themeConfig.colors.border.light');
  border-radius: v-bind('themeConfig.borderRadius.lg');
  overflow: hidden;
  box-shadow: v-bind('themeConfig.shadows.sm');
}

.expand-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  cursor: pointer;
  user-select: none;
  transition: background-color var(--expand-duration, 300ms) var(--expand-easing, cubic-bezier(0.4, 0, 0.2, 1));
}

.expand-card__header:hover {
  background-color: v-bind('themeConfig.colors.background.secondary');
}

.expand-card__header:focus-visible {
  outline: none;
  box-shadow: inset 0 0 0 2px v-bind('themeConfig.accessibility.focusRingColor');
}

.expand-card__header-content {
  flex: 1;
  min-width: 0;
}

.expand-card__title {
  font-size: v-bind('themeConfig.typography.fontSize.base');
  font-weight: v-bind('themeConfig.typography.fontWeight.medium');
  color: v-bind('themeConfig.colors.text.primary');
}

.expand-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  color: v-bind('themeConfig.colors.text.secondary');
  transition: transform var(--expand-duration, 300ms) var(--expand-easing, cubic-bezier(0.4, 0, 0.2, 1));
}

.expand-card__icon--rotated {
  transform: rotate(180deg);
}

.expand-card__content-wrapper {
  will-change: height;
}

.expand-card__content {
  padding: 0 16px 16px;
  color: v-bind('themeConfig.colors.text.secondary');
  font-size: v-bind('themeConfig.typography.fontSize.sm');
  line-height: 1.6;
}

/* 展开状态样式 */
.expand-card--expanded {
  box-shadow: v-bind('themeConfig.shadows.md');
}

.expand-card--expanded .expand-card__header {
  border-bottom: 1px solid v-bind('themeConfig.colors.border.light');
}
</style>
