<template>
  <div
    ref="containerRef"
    class="bigscreen-preview"
    :class="{ 'bigscreen-fullscreen': isFullscreen }"
    :style="containerStyle"
  >
    <!-- 大屏头部 -->
    <div v-if="config.showHeader" class="bigscreen-header" :style="headerStyle">
      <div class="header-left">
        <h1 class="header-title">{{ config.headerTitle || pageName || '数据大屏' }}</h1>
      </div>
      <div class="header-center">
        <slot name="header-center" />
      </div>
      <div class="header-right">
        <span v-if="config.showClock" class="header-clock">{{ currentTime }}</span>
        <button
          v-if="config.showFullscreen"
          class="header-btn"
          :title="isFullscreen ? '退出全屏' : '全屏显示'"
          @click="toggleFullscreen"
        >
          <svg v-if="!isFullscreen" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M8 3H5a2 2 0 00-2 2v3m18 0V5a2 2 0 00-2-2h-3m0 18h3a2 2 0 002-2v-3M3 16v3a2 2 0 002 2h3" />
          </svg>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M8 3v3a2 2 0 01-2 2H3m18 0h-3a2 2 0 01-2-2V3m0 18v-3a2 2 0 012-2h3M3 16h3a2 2 0 012 2v3" />
          </svg>
        </button>
        <button v-if="!isFullscreen" class="header-btn" title="关闭预览" @click="$emit('close')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>
    </div>

    <!-- 缩放容器 -->
    <div
      ref="scaleRef"
      class="bigscreen-scale-wrapper"
      :style="scaleWrapperStyle"
    >
      <div
        class="bigscreen-canvas"
        :style="canvasStyle"
      >
        <slot />
      </div>
    </div>

    <!-- 缩放信息 (开发模式) -->
    <div v-if="showScaleInfo" class="bigscreen-scale-info">
      {{ Math.round(currentScale * 100) }}% | {{ config.designWidth }}×{{ config.designHeight }}
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import type { BigscreenConfig, BigscreenScaleMode } from '@/types/page'
import { createDefaultBigscreenConfig } from '@/types/page'

interface Props {
  config?: BigscreenConfig
  pageName?: string
  showScaleInfo?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  config: () => createDefaultBigscreenConfig(),
  pageName: '',
  showScaleInfo: false
})

defineEmits<{
  close: []
}>()

const containerRef = ref<HTMLElement | null>(null)
const isFullscreen = ref(false)
const currentScale = ref(1)
const currentTime = ref('')
const containerWidth = ref(0)
const containerHeight = ref(0)

let clockTimer: ReturnType<typeof setInterval> | null = null
let resizeObserver: ResizeObserver | null = null

const headerH = computed(() => props.config.showHeader ? (props.config.headerHeight || 64) : 0)

const containerStyle = computed(() => ({
  background: getBackgroundCSS(),
  '--header-height': `${headerH.value}px`
}))

const headerStyle = computed(() => ({
  height: `${headerH.value}px`
}))

const scaleWrapperStyle = computed(() => ({
  width: '100%',
  height: props.config.showHeader ? `calc(100% - ${headerH.value}px)` : '100%',
  overflow: 'hidden',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center'
}))

const canvasStyle = computed(() => ({
  width: `${props.config.designWidth}px`,
  height: `${props.config.designHeight - headerH.value}px`,
  transform: `scale(${currentScale.value})`,
  transformOrigin: 'center center',
  position: 'relative' as const,
  flexShrink: 0
}))

function getBackgroundCSS(): string {
  const { backgroundType, backgroundValue } = props.config
  if (!backgroundValue) return 'linear-gradient(135deg, #0f0f23 0%, #1a1a3e 50%, #0d0d1f 100%)'
  switch (backgroundType) {
    case 'color': return backgroundValue
    case 'gradient': return backgroundValue
    case 'image': return `url(${backgroundValue}) center/cover no-repeat`
    default: return backgroundValue
  }
}

function calculateScale(
  containerW: number,
  containerH: number,
  designW: number,
  designH: number,
  mode: BigscreenScaleMode
): number {
  const canvasH = designH - headerH.value
  const availH = containerH - headerH.value
  if (containerW <= 0 || availH <= 0 || designW <= 0 || canvasH <= 0) return 1
  const scaleX = containerW / designW
  const scaleY = availH / canvasH
  switch (mode) {
    case 'fit': return Math.min(scaleX, scaleY)
    case 'fill': return Math.max(scaleX, scaleY)
    case 'width': return scaleX
    case 'height': return scaleY
    case 'none': return 1
    default: return Math.min(scaleX, scaleY)
  }
}

function updateScale() {
  if (!containerRef.value) return
  const rect = containerRef.value.getBoundingClientRect()
  containerWidth.value = rect.width
  containerHeight.value = rect.height
  currentScale.value = calculateScale(
    rect.width,
    rect.height,
    props.config.designWidth,
    props.config.designHeight,
    props.config.scaleMode
  )
}

function updateClock() {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
}

async function toggleFullscreen() {
  if (!containerRef.value) return
  try {
    if (!document.fullscreenElement) {
      await containerRef.value.requestFullscreen()
      isFullscreen.value = true
    } else {
      await document.exitFullscreen()
      isFullscreen.value = false
    }
  } catch (e) {
    console.warn('全屏操作失败:', e)
  }
}

function onFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
  requestAnimationFrame(updateScale)
}

watch(() => [props.config.designWidth, props.config.designHeight, props.config.scaleMode], updateScale)

onMounted(() => {
  updateScale()
  updateClock()
  clockTimer = setInterval(updateClock, 1000)

  resizeObserver = new ResizeObserver(() => {
    requestAnimationFrame(updateScale)
  })
  if (containerRef.value) {
    resizeObserver.observe(containerRef.value)
  }

  document.addEventListener('fullscreenchange', onFullscreenChange)
})

onBeforeUnmount(() => {
  if (clockTimer) clearInterval(clockTimer)
  if (resizeObserver) resizeObserver.disconnect()
  document.removeEventListener('fullscreenchange', onFullscreenChange)
})

defineExpose({ toggleFullscreen, updateScale, isFullscreen })
</script>

<style scoped>
.bigscreen-preview {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
  color: #e0e0e0;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.bigscreen-fullscreen {
  position: fixed;
  inset: 0;
  z-index: 9999;
}

.bigscreen-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: relative;
  z-index: 10;
  background: linear-gradient(180deg, rgba(0,0,0,0.6) 0%, rgba(0,0,0,0.1) 100%);
  border-bottom: 1px solid rgba(255,255,255,0.06);
}

.header-title {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 2px;
  background: linear-gradient(90deg, #667eea, #00d4ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin: 0;
  white-space: nowrap;
}

.header-center {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-clock {
  font-size: 16px;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  color: rgba(255,255,255,0.8);
  letter-spacing: 1px;
}

.header-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid rgba(255,255,255,0.15);
  border-radius: 6px;
  background: rgba(255,255,255,0.05);
  color: rgba(255,255,255,0.7);
  cursor: pointer;
  transition: all 0.2s;
}
.header-btn:hover {
  background: rgba(255,255,255,0.12);
  color: #fff;
  border-color: rgba(255,255,255,0.3);
}

.bigscreen-scale-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
}

.bigscreen-canvas {
  transition: transform 0.3s ease;
}

.bigscreen-scale-info {
  position: absolute;
  bottom: 8px;
  right: 12px;
  font-size: 11px;
  color: rgba(255,255,255,0.35);
  pointer-events: none;
  font-variant-numeric: tabular-nums;
}
</style>
