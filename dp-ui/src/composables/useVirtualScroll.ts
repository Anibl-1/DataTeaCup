/**
 * 虚拟滚动 Hook
 * 使用虚拟滚动技术渲染可见区域，确保大数据量表格流畅滚动
 * 需求: 15.1, 15.5
 */
import { ref, computed, onMounted, onBeforeUnmount, type Ref, type ComputedRef } from 'vue'

export interface VirtualScrollOptions {
  /** 数据总数 */
  itemCount: Ref<number> | ComputedRef<number>
  /** 每行高度（px） */
  itemHeight: number
  /** 缓冲区行数（上下各额外渲染的行数） */
  bufferSize?: number
  /** 容器高度（px），不传则自动检测 */
  containerHeight?: Ref<number> | number
  /** 加载更多回调 */
  onLoadMore?: () => Promise<void>
  /** 触发加载更多的距离阈值（px） */
  loadMoreThreshold?: number
}

export interface UseVirtualScrollReturn {
  /** 容器 ref */
  containerRef: Ref<HTMLElement | null>
  /** 可见区域起始索引 */
  visibleStart: ComputedRef<number>
  /** 可见区域结束索引 */
  visibleEnd: ComputedRef<number>
  /** 可见项索引列表 */
  visibleRange: ComputedRef<number[]>
  /** 内容总高度 */
  totalHeight: ComputedRef<number>
  /** 可见区域偏移量 */
  offsetY: ComputedRef<number>
  /** 当前滚动位置 */
  scrollTop: Ref<number>
  /** 滚动事件处理 */
  onScroll: (e: Event) => void
  /** 滚动到指定索引 */
  scrollToIndex: (index: number) => void
  /** 滚动到顶部 */
  scrollToTop: () => void
  /** 是否正在加载更多 */
  isLoadingMore: Ref<boolean>
}

/**
 * 虚拟滚动 composable
 * 仅渲染可见区域及缓冲区的行，确保 DOM 节点数远小于数据总行数
 */
export function useVirtualScroll(options: VirtualScrollOptions): UseVirtualScrollReturn {
  const {
    itemCount,
    itemHeight,
    bufferSize = 5,
    onLoadMore,
    loadMoreThreshold = 200
  } = options

  const containerRef = ref<HTMLElement | null>(null)
  const scrollTop = ref(0)
  const measuredContainerHeight = ref(0)
  const isLoadingMore = ref(false)

  // Resolve container height: explicit prop or measured
  const resolvedContainerHeight = computed(() => {
    if (options.containerHeight != null) {
      return typeof options.containerHeight === 'number'
        ? options.containerHeight
        : options.containerHeight.value
    }
    return measuredContainerHeight.value
  })

  // Total scrollable height
  const totalHeight = computed(() => {
    const count = typeof itemCount === 'number' ? itemCount : itemCount.value
    return count * itemHeight
  })

  // Number of items visible in the viewport
  const visibleCount = computed(() => {
    if (resolvedContainerHeight.value <= 0) return 0
    return Math.ceil(resolvedContainerHeight.value / itemHeight)
  })

  // Start index (with buffer)
  const visibleStart = computed(() => {
    const rawStart = Math.floor(scrollTop.value / itemHeight)
    return Math.max(0, rawStart - bufferSize)
  })

  // End index (with buffer)
  const visibleEnd = computed(() => {
    const count = typeof itemCount === 'number' ? itemCount : itemCount.value
    const rawStart = Math.floor(scrollTop.value / itemHeight)
    const rawEnd = rawStart + visibleCount.value
    return Math.min(count, rawEnd + bufferSize)
  })

  // Array of visible indices
  const visibleRange = computed(() => {
    const result: number[] = []
    for (let i = visibleStart.value; i < visibleEnd.value; i++) {
      result.push(i)
    }
    return result
  })

  // Y offset for the visible content block
  const offsetY = computed(() => {
    return visibleStart.value * itemHeight
  })

  // Scroll handler using requestAnimationFrame for smooth 30fps+
  let rafId: number | null = null

  const onScroll = (e: Event) => {
    if (rafId !== null) return

    rafId = requestAnimationFrame(() => {
      const target = e.target as HTMLElement
      if (target) {
        scrollTop.value = target.scrollTop

        // Check if we need to load more
        if (onLoadMore && !isLoadingMore.value) {
          const distanceToBottom = totalHeight.value - target.scrollTop - resolvedContainerHeight.value
          if (distanceToBottom < loadMoreThreshold) {
            isLoadingMore.value = true
            onLoadMore().finally(() => {
              isLoadingMore.value = false
            })
          }
        }
      }
      rafId = null
    })
  }

  const scrollToIndex = (index: number) => {
    if (containerRef.value) {
      const targetTop = index * itemHeight
      containerRef.value.scrollTop = targetTop
      scrollTop.value = targetTop
    }
  }

  const scrollToTop = () => {
    scrollToIndex(0)
  }

  // Measure container height on mount
  let resizeObserver: ResizeObserver | null = null

  onMounted(() => {
    if (containerRef.value) {
      measuredContainerHeight.value = containerRef.value.clientHeight

      resizeObserver = new ResizeObserver((entries) => {
        for (const entry of entries) {
          measuredContainerHeight.value = entry.contentRect.height
        }
      })
      resizeObserver.observe(containerRef.value)
    }
  })

  onBeforeUnmount(() => {
    if (rafId !== null) {
      cancelAnimationFrame(rafId)
      rafId = null
    }
    if (resizeObserver) {
      resizeObserver.disconnect()
      resizeObserver = null
    }
  })

  return {
    containerRef,
    visibleStart,
    visibleEnd,
    visibleRange,
    totalHeight,
    offsetY,
    scrollTop,
    onScroll,
    scrollToIndex,
    scrollToTop,
    isLoadingMore
  }
}
