<template>
  <div
    class="m-page-shell"
    :class="{ 'm-page-shell--no-tabbar': noTabBar }"
    @touchstart.passive="onTouchStart"
    @touchmove.passive="onTouchMove"
    @touchend.passive="onTouchEnd"
  >
    <!-- 下拉刷新指示器 -->
    <Transition name="m-pull">
      <div v-if="pullDistance > 0" class="m-pull-indicator" :style="{ height: `${Math.min(pullDistance, 80)}px` }">
        <div class="m-pull-content" :class="{ 'm-pull-ready': pullDistance >= pullThreshold, 'm-pull-refreshing': refreshing }">
          <n-icon :size="18" class="m-pull-icon" :style="{ transform: `rotate(${Math.min(pullDistance / pullThreshold, 1) * 180}deg)` }">
            <RefreshOutline v-if="!refreshing" />
            <SyncOutline v-else />
          </n-icon>
          <span class="m-pull-text">
            {{ refreshing ? '刷新中...' : pullDistance >= pullThreshold ? '释放刷新' : '下拉刷新' }}
          </span>
        </div>
      </div>
    </Transition>

    <slot></slot>

    <!-- 回到顶部 -->
    <Transition name="m-fab">
      <div v-if="showScrollTop" class="m-scroll-top" @click="scrollToTop">
        <n-icon :size="20"><ChevronUpOutline /></n-icon>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { NIcon } from 'naive-ui'
import { ChevronUpOutline, RefreshOutline, SyncOutline } from '@vicons/ionicons5'

defineProps<{
  noTabBar?: boolean
}>()

const emit = defineEmits<{
  (e: 'refresh'): void
}>()

const showScrollTop = ref(false)
let scrollEl: HTMLElement | null = null
let scrollTicking = false

// 下拉刷新状态
const pullDistance = ref(0)
const refreshing = ref(false)
const pullThreshold = 60
let startY = 0
let isPulling = false

function getScrollTop(): number {
  if (scrollEl) return scrollEl.scrollTop
  return window.scrollY || document.documentElement.scrollTop
}

function onTouchStart(e: TouchEvent) {
  if (refreshing.value) return
  if (getScrollTop() > 5) return
  const touch = e.touches[0]
  if (!touch) return
  startY = touch.clientY
  isPulling = true
}

function onTouchMove(e: TouchEvent) {
  if (!isPulling || refreshing.value) return
  const touch = e.touches[0]
  if (!touch) return
  const currentY = touch.clientY
  const diff = currentY - startY
  if (diff < 0) {
    isPulling = false
    pullDistance.value = 0
    return
  }
  // 使用阻尼效果：拉越远阻力越大
  pullDistance.value = Math.min(diff * 0.5, 100)
}

function onTouchEnd() {
  if (!isPulling) return
  isPulling = false
  if (pullDistance.value >= pullThreshold && !refreshing.value) {
    refreshing.value = true
    pullDistance.value = pullThreshold
    emit('refresh')
    // 自动结束刷新（最长 3 秒）
    setTimeout(() => {
      refreshing.value = false
      pullDistance.value = 0
    }, 3000)
  } else {
    pullDistance.value = 0
  }
}

// 外部调用：结束刷新
defineExpose({
  endRefresh() {
    refreshing.value = false
    pullDistance.value = 0
  }
})

function handleScroll() {
  if (scrollTicking) return
  scrollTicking = true
  requestAnimationFrame(() => {
    const st = getScrollTop()
    showScrollTop.value = st > 400
    scrollTicking = false
  })
}

function scrollToTop() {
  const el = scrollEl || document.documentElement
  el.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  scrollEl = document.querySelector('.n-layout-scroll-container') as HTMLElement
  const target = scrollEl || window
  target.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  const target = scrollEl || window
  target.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.m-page-shell {
  padding: 12px;
  padding-bottom: calc(70px + env(safe-area-inset-bottom, 0px));
  min-height: 100vh;
  background: var(--bg-secondary, #f5f7fa);
  animation: mPageIn 0.35s ease;
}

.m-page-shell--no-tabbar {
  padding-bottom: calc(20px + env(safe-area-inset-bottom, 0px));
}

@keyframes mPageIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.m-scroll-top {
  position: fixed;
  right: 14px;
  bottom: calc(72px + env(safe-area-inset-bottom, 0px));
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #475569;
  -webkit-tap-highlight-color: transparent;
  z-index: 998;
}

.m-scroll-top:active {
  transform: scale(0.9);
}

.m-fab-enter-active,
.m-fab-leave-active {
  transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.m-fab-enter-from,
.m-fab-leave-to {
  opacity: 0;
  transform: scale(0.5) translateY(20px);
}

/* 下拉刷新指示器 */
.m-pull-indicator {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  overflow: hidden;
  transition: height 0.2s ease;
}

.m-pull-content {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 0;
  color: #94a3b8;
  font-size: 12px;
  transition: color 0.2s;
}

.m-pull-content.m-pull-ready {
  color: #3b82f6;
}

.m-pull-content.m-pull-refreshing {
  color: #3b82f6;
}

.m-pull-icon {
  transition: transform 0.2s ease;
}

.m-pull-refreshing .m-pull-icon {
  animation: m-spin 0.8s linear infinite;
}

@keyframes m-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.m-pull-enter-active,
.m-pull-leave-active {
  transition: all 0.25s ease;
}
.m-pull-enter-from,
.m-pull-leave-to {
  opacity: 0;
  height: 0 !important;
}

</style>

<style>
/* MobilePageShell 深色模式（非 scoped） */
html.dark .m-page-shell { background: #0f172a !important; }
html.dark .m-scroll-top {
  background: rgba(30, 41, 59, 0.92) !important;
  color: #94a3b8 !important;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3) !important;
}
html.dark .m-pull-content { color: #64748b !important; }
html.dark .m-pull-content.m-pull-ready,
html.dark .m-pull-content.m-pull-ready,
html.dark .m-pull-content.m-pull-refreshing { color: #60a5fa !important; }
</style>
