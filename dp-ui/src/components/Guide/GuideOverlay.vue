<template>
  <Teleport to="body">
    <Transition name="guide-overlay">
      <div
        v-if="isActive && currentStep"
        class="guide-overlay"
        role="dialog"
        aria-modal="true"
        :aria-label="currentGuide?.name || '功能引导'"
      >
        <!-- 遮罩层（带高亮区域镂空） -->
        <svg class="guide-overlay__mask" :viewBox="`0 0 ${windowSize.width} ${windowSize.height}`">
          <defs>
            <mask id="guide-highlight-mask">
              <rect x="0" y="0" :width="windowSize.width" :height="windowSize.height" fill="white" />
              <rect
                v-if="highlightRect"
                :x="highlightRect.left - highlightRect.padding"
                :y="highlightRect.top - highlightRect.padding"
                :width="highlightRect.width + highlightRect.padding * 2"
                :height="highlightRect.height + highlightRect.padding * 2"
                :rx="8"
                :ry="8"
                fill="black"
              />
            </mask>
          </defs>
          <rect
            x="0"
            y="0"
            :width="windowSize.width"
            :height="windowSize.height"
            fill="rgba(0, 0, 0, 0.5)"
            mask="url(#guide-highlight-mask)"
            @click="handleMaskClick"
          />
        </svg>

        <!-- 高亮边框 -->
        <div
          v-if="highlightRect"
          class="guide-overlay__highlight"
          :style="highlightStyle"
        />

        <!-- 提示框 -->
        <div
          v-if="tooltipPosition"
          ref="tooltipRef"
          class="guide-tooltip"
          :class="[`guide-tooltip--${tooltipPosition.placement}`]"
          :style="tooltipStyle"
        >
          <!-- 箭头 -->
          <div 
            class="guide-tooltip__arrow"
            :class="[`guide-tooltip__arrow--${tooltipPosition.arrowPosition}`]"
          />
          
          <!-- 内容 -->
          <div class="guide-tooltip__content">
            <!-- 标题 -->
            <div class="guide-tooltip__header">
              <h3 class="guide-tooltip__title">{{ currentStep.title }}</h3>
              <button
                class="guide-tooltip__close"
                aria-label="关闭引导"
                @click="handleSkip"
              >
                <NIcon :size="16"><CloseOutline /></NIcon>
              </button>
            </div>
            
            <!-- 描述 -->
            <div class="guide-tooltip__body">
              {{ currentStep.content }}
            </div>
            
            <!-- 底部操作 -->
            <div class="guide-tooltip__footer">
              <!-- 进度指示 -->
              <div class="guide-tooltip__progress">
                <span class="guide-tooltip__progress-text">
                  {{ progress.current }} / {{ progress.total }}
                </span>
                <div class="guide-tooltip__progress-dots">
                  <span
                    v-for="(_, index) in currentGuide?.steps || []"
                    :key="index"
                    class="guide-tooltip__progress-dot"
                    :class="{ 'guide-tooltip__progress-dot--active': index === currentStepIndex }"
                  />
                </div>
              </div>
              
              <!-- 按钮组 -->
              <div class="guide-tooltip__actions">
                <NButton
                  v-if="currentStepIndex > 0"
                  size="small"
                  quaternary
                  @click="handlePrev"
                >
                  上一步
                </NButton>
                <NButton
                  size="small"
                  quaternary
                  @click="handleSkip"
                >
                  跳过
                </NButton>
                <NButton
                  type="primary"
                  size="small"
                  @click="handleNext"
                >
                  {{ isLastStep ? '完成' : '下一步' }}
                </NButton>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
/**
 * 功能引导遮罩组件
 * Feature Guide Overlay Component
 * 
 * 显示功能引导的遮罩层、高亮区域和提示框。
 * 
 * 需求 19.1: WHEN 用户首次使用某功能时，THE DataTeaCup SHALL 显示功能引导教程
 */
import { 
  ref, 
  computed, 
  watch, 
  onMounted, 
  onUnmounted, 
  nextTick,
  type CSSProperties 
} from 'vue'
import { NButton, NIcon } from 'naive-ui'
import { CloseOutline } from '@vicons/ionicons5'
import { useGuide } from './GuideService'
import type { HighlightRect, TooltipPosition, GuidePlacement } from './types'

// ==================== Composables ====================

const {
  isActive,
  currentGuide,
  currentStep,
  currentStepIndex,
  progress,
  next,
  prev,
  skip
} = useGuide()

// ==================== Refs ====================

const tooltipRef = ref<HTMLElement | null>(null)
const highlightRect = ref<HighlightRect | null>(null)
const tooltipPosition = ref<TooltipPosition | null>(null)
const windowSize = ref({ width: window.innerWidth, height: window.innerHeight })

// ==================== Constants ====================

const TOOLTIP_MARGIN = 12
const TOOLTIP_WIDTH = 320
const HIGHLIGHT_PADDING = 8
const ARROW_SIZE = 8

// ==================== Computed ====================

const isLastStep = computed(() => {
  if (!currentGuide.value) return true
  return currentStepIndex.value >= currentGuide.value.steps.length - 1
})

const highlightStyle = computed<CSSProperties>(() => {
  if (!highlightRect.value) return {}
  
  const rect = highlightRect.value
  return {
    top: `${rect.top - rect.padding}px`,
    left: `${rect.left - rect.padding}px`,
    width: `${rect.width + rect.padding * 2}px`,
    height: `${rect.height + rect.padding * 2}px`
  }
})

const tooltipStyle = computed<CSSProperties>(() => {
  if (!tooltipPosition.value) return {}
  
  return {
    top: `${tooltipPosition.value.top}px`,
    left: `${tooltipPosition.value.left}px`
  }
})

// ==================== Methods ====================

/**
 * 获取目标元素的位置信息
 */
function getTargetRect(selector: string): DOMRect | null {
  try {
    const element = document.querySelector(selector)
    if (!element) {
      console.warn(`[GuideOverlay] 目标元素不存在: ${selector}`)
      return null
    }
    return element.getBoundingClientRect()
  } catch (e) {
    console.warn(`[GuideOverlay] 获取目标元素失败: ${selector}`, e)
    return null
  }
}

/**
 * 计算高亮区域
 */
function calculateHighlightRect(): HighlightRect | null {
  if (!currentStep.value) return null
  
  const targetRect = getTargetRect(currentStep.value.target)
  if (!targetRect) return null
  
  const padding = currentStep.value.highlightPadding ?? HIGHLIGHT_PADDING
  
  return {
    top: targetRect.top + window.scrollY,
    left: targetRect.left + window.scrollX,
    width: targetRect.width,
    height: targetRect.height,
    padding
  }
}

/**
 * 计算提示框位置
 */
function calculateTooltipPosition(): TooltipPosition | null {
  if (!currentStep.value || !highlightRect.value) return null
  
  const rect = highlightRect.value
  const placement = currentStep.value.placement
  const offset = currentStep.value.offset || { x: 0, y: 0 }
  
  let top = 0
  let left = 0
  const arrowPosition: 'start' | 'center' | 'end' = 'center'
  
  // 计算基础位置
  switch (placement) {
    case 'top':
      top = rect.top - rect.padding - TOOLTIP_MARGIN - getTooltipHeight()
      left = rect.left + rect.width / 2 - TOOLTIP_WIDTH / 2
      break
    case 'bottom':
      top = rect.top + rect.height + rect.padding + TOOLTIP_MARGIN
      left = rect.left + rect.width / 2 - TOOLTIP_WIDTH / 2
      break
    case 'left':
      top = rect.top + rect.height / 2 - getTooltipHeight() / 2
      left = rect.left - rect.padding - TOOLTIP_MARGIN - TOOLTIP_WIDTH
      break
    case 'right':
      top = rect.top + rect.height / 2 - getTooltipHeight() / 2
      left = rect.left + rect.width + rect.padding + TOOLTIP_MARGIN
      break
    case 'center':
      top = windowSize.value.height / 2 - getTooltipHeight() / 2
      left = windowSize.value.width / 2 - TOOLTIP_WIDTH / 2
      break
  }
  
  // 应用偏移
  top += offset.y
  left += offset.x
  
  // 边界检测和调整
  const adjustedPosition = adjustPositionForBounds(top, left, placement)
  
  return {
    top: adjustedPosition.top,
    left: adjustedPosition.left,
    placement: adjustedPosition.placement,
    arrowPosition
  }
}

/**
 * 获取提示框高度（估算）
 */
function getTooltipHeight(): number {
  // 基础高度 + 内容估算
  return 150
}

/**
 * 调整位置以适应边界
 */
function adjustPositionForBounds(
  top: number, 
  left: number, 
  placement: GuidePlacement
): { top: number; left: number; placement: GuidePlacement } {
  const padding = 16
  const maxLeft = windowSize.value.width - TOOLTIP_WIDTH - padding
  const maxTop = windowSize.value.height - getTooltipHeight() - padding
  
  // 水平边界调整
  if (left < padding) {
    left = padding
  } else if (left > maxLeft) {
    left = maxLeft
  }
  
  // 垂直边界调整
  if (top < padding) {
    top = padding
  } else if (top > maxTop) {
    top = maxTop
  }
  
  return { top, left, placement }
}

/**
 * 更新位置
 */
async function updatePosition(): Promise<void> {
  await nextTick()
  
  highlightRect.value = calculateHighlightRect()
  
  // 等待高亮区域渲染后再计算提示框位置
  await nextTick()
  tooltipPosition.value = calculateTooltipPosition()
}

/**
 * 处理窗口大小变化
 */
function handleResize(): void {
  windowSize.value = {
    width: window.innerWidth,
    height: window.innerHeight
  }
  updatePosition()
}

/**
 * 处理滚动
 */
function handleScroll(): void {
  updatePosition()
}

/**
 * 处理遮罩点击
 */
function handleMaskClick(): void {
  // 点击遮罩不做任何操作，防止误操作
}

/**
 * 处理下一步
 */
function handleNext(): void {
  next()
}

/**
 * 处理上一步
 */
function handlePrev(): void {
  prev()
}

/**
 * 处理跳过
 */
function handleSkip(): void {
  skip()
}

/**
 * 处理键盘事件
 */
function handleKeydown(e: KeyboardEvent): void {
  if (!isActive.value) return
  
  switch (e.key) {
    case 'Escape':
      handleSkip()
      break
    case 'ArrowRight':
    case 'Enter':
      handleNext()
      break
    case 'ArrowLeft':
      if (currentStepIndex.value > 0) {
        handlePrev()
      }
      break
  }
}

// ==================== Watchers ====================

// 监听步骤变化，更新位置
watch(
  [currentStep, currentStepIndex],
  () => {
    if (isActive.value) {
      updatePosition()
    }
  },
  { immediate: true }
)

// 监听激活状态
watch(isActive, (active) => {
  if (active) {
    // 禁止页面滚动
    document.body.style.overflow = 'hidden'
    updatePosition()
  } else {
    // 恢复页面滚动
    document.body.style.overflow = ''
    highlightRect.value = null
    tooltipPosition.value = null
  }
})

// ==================== Lifecycle ====================

onMounted(() => {
  window.addEventListener('resize', handleResize)
  window.addEventListener('scroll', handleScroll, true)
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('scroll', handleScroll, true)
  window.removeEventListener('keydown', handleKeydown)
  document.body.style.overflow = ''
})
</script>

<style scoped>
.guide-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 9999;
  pointer-events: none;
}

.guide-overlay__mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: auto;
}

.guide-overlay__highlight {
  position: absolute;
  border: 2px solid var(--primary-color, #1890ff);
  border-radius: 8px;
  box-shadow: 0 0 0 4px rgba(24, 144, 255, 0.2);
  pointer-events: none;
  transition: all 0.3s ease;
}

/* 提示框 */
.guide-tooltip {
  position: absolute;
  width: 320px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  pointer-events: auto;
  z-index: 10000;
  animation: guide-tooltip-enter 0.3s ease;
}

@keyframes guide-tooltip-enter {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 箭头 */
.guide-tooltip__arrow {
  position: absolute;
  width: 16px;
  height: 16px;
  background: #fff;
  transform: rotate(45deg);
  box-shadow: -2px -2px 4px rgba(0, 0, 0, 0.05);
}

.guide-tooltip--top .guide-tooltip__arrow {
  bottom: -8px;
  left: 50%;
  margin-left: -8px;
  box-shadow: 2px 2px 4px rgba(0, 0, 0, 0.05);
}

.guide-tooltip--bottom .guide-tooltip__arrow {
  top: -8px;
  left: 50%;
  margin-left: -8px;
}

.guide-tooltip--left .guide-tooltip__arrow {
  right: -8px;
  top: 50%;
  margin-top: -8px;
  box-shadow: 2px -2px 4px rgba(0, 0, 0, 0.05);
}

.guide-tooltip--right .guide-tooltip__arrow {
  left: -8px;
  top: 50%;
  margin-top: -8px;
  box-shadow: -2px 2px 4px rgba(0, 0, 0, 0.05);
}

.guide-tooltip--center .guide-tooltip__arrow {
  display: none;
}

/* 箭头位置调整 */
.guide-tooltip__arrow--start {
  left: 24px !important;
  margin-left: 0 !important;
}

.guide-tooltip__arrow--end {
  left: auto !important;
  right: 24px !important;
  margin-left: 0 !important;
}

/* 内容区域 */
.guide-tooltip__content {
  padding: 16px;
}

.guide-tooltip__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 8px;
}

.guide-tooltip__title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  line-height: 1.4;
}

.guide-tooltip__close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  background: transparent;
  color: #8c8c8c;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
  flex-shrink: 0;
  margin-left: 8px;
}

.guide-tooltip__close:hover {
  background: #f5f5f5;
  color: #262626;
}

.guide-tooltip__body {
  font-size: 14px;
  color: #595959;
  line-height: 1.6;
  margin-bottom: 16px;
}

.guide-tooltip__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

/* 进度指示 */
.guide-tooltip__progress {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.guide-tooltip__progress-text {
  font-size: 12px;
  color: #8c8c8c;
}

.guide-tooltip__progress-dots {
  display: flex;
  gap: 4px;
}

.guide-tooltip__progress-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #d9d9d9;
  transition: all 0.2s;
}

.guide-tooltip__progress-dot--active {
  background: var(--primary-color, #1890ff);
  transform: scale(1.2);
}

/* 操作按钮 */
.guide-tooltip__actions {
  display: flex;
  gap: 8px;
}

/* 过渡动画 */
.guide-overlay-enter-active,
.guide-overlay-leave-active {
  transition: opacity 0.3s ease;
}

.guide-overlay-enter-from,
.guide-overlay-leave-to {
  opacity: 0;
}
</style>

