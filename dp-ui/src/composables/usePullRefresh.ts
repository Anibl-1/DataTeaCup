/**
 * 下拉刷新 Composable
 * Pull-to-refresh composable for mobile pages
 */
import { ref, onMounted, onUnmounted } from 'vue'

export interface UsePullRefreshOptions {
  /** 触发刷新的下拉距离 (px) */
  threshold?: number
  /** 刷新回调 */
  onRefresh: () => Promise<void>
  /** 是否启用 */
  enabled?: boolean
}

export function usePullRefresh(options: UsePullRefreshOptions) {
  const { threshold = 60, onRefresh, enabled = true } = options

  const refreshing = ref(false)
  const pullDistance = ref(0)
  const isPulling = ref(false)

  let startY = 0
  let containerEl: HTMLElement | null = null

  const onTouchStart = (e: TouchEvent) => {
    if (!enabled || refreshing.value) return
    // Only trigger when scrolled to top
    const scrollTop = window.scrollY || document.documentElement.scrollTop
    if (scrollTop > 0) return
    startY = e.touches[0].clientY
    isPulling.value = true
  }

  const onTouchMove = (e: TouchEvent) => {
    if (!isPulling.value || refreshing.value) return
    const currentY = e.touches[0].clientY
    const diff = currentY - startY
    if (diff > 0) {
      pullDistance.value = Math.min(diff * 0.5, threshold * 1.5)
    }
  }

  const onTouchEnd = async () => {
    if (!isPulling.value) return
    isPulling.value = false

    if (pullDistance.value >= threshold && !refreshing.value) {
      refreshing.value = true
      try {
        await onRefresh()
      } finally {
        refreshing.value = false
      }
    }
    pullDistance.value = 0
  }

  onMounted(() => {
    if (!enabled) return
    containerEl = document.querySelector('.mobile-content') || document.body
    containerEl.addEventListener('touchstart', onTouchStart, { passive: true })
    containerEl.addEventListener('touchmove', onTouchMove, { passive: true })
    containerEl.addEventListener('touchend', onTouchEnd, { passive: true })
  })

  onUnmounted(() => {
    if (!containerEl) return
    containerEl.removeEventListener('touchstart', onTouchStart)
    containerEl.removeEventListener('touchmove', onTouchMove)
    containerEl.removeEventListener('touchend', onTouchEnd)
  })

  return {
    refreshing,
    pullDistance,
    isPulling
  }
}

export default usePullRefresh
