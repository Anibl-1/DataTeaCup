/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * Unit tests for useVirtualScroll composable
 * Validates: Requirements 15.1
 */
import { describe, it, expect, vi } from 'vitest'
import { ref, nextTick } from 'vue'
import { useVirtualScroll } from '../useVirtualScroll'

// Mock lifecycle hooks since we're testing outside component context
vi.mock('vue', async () => {
  const actual = await vi.importActual('vue')
  return {
    ...actual as any,
    onMounted: vi.fn((cb: () => void) => cb()),
    onBeforeUnmount: vi.fn()
  }
})

// Mock ResizeObserver
class MockResizeObserver {
  observe = vi.fn()
  unobserve = vi.fn()
  disconnect = vi.fn()
}
vi.stubGlobal('ResizeObserver', MockResizeObserver)

// Mock requestAnimationFrame
vi.stubGlobal('requestAnimationFrame', (cb: FrameRequestCallback) => {
  cb(0)
  return 1
})
vi.stubGlobal('cancelAnimationFrame', vi.fn())

describe('useVirtualScroll', () => {
  const defaultOptions = () => ({
    itemCount: ref(1000),
    itemHeight: 48,
    bufferSize: 5,
    containerHeight: ref(480)
  })

  it('should compute totalHeight based on itemCount and itemHeight', () => {
    const { totalHeight } = useVirtualScroll(defaultOptions())
    expect(totalHeight.value).toBe(1000 * 48)
  })

  it('should start with visibleStart at 0', () => {
    const { visibleStart } = useVirtualScroll(defaultOptions())
    expect(visibleStart.value).toBe(0)
  })

  it('should compute correct visibleEnd with buffer', () => {
    const opts = defaultOptions()
    const { visibleEnd } = useVirtualScroll(opts)
    // containerHeight=480, itemHeight=48 => 10 visible items
    // visibleEnd = min(1000, 0 + 10 + 5) = 15
    expect(visibleEnd.value).toBe(15)
  })

  it('should compute visibleRange as array of indices', () => {
    const { visibleRange } = useVirtualScroll(defaultOptions())
    // visibleStart=0, visibleEnd=15
    expect(visibleRange.value).toHaveLength(15)
    expect(visibleRange.value[0]).toBe(0)
    expect(visibleRange.value[14]).toBe(14)
  })

  it('should update visible range on scroll', async () => {
    const opts = defaultOptions()
    const { onScroll, visibleStart, visibleEnd, offsetY } = useVirtualScroll(opts)

    // Simulate scrolling to row 100 (scrollTop = 100 * 48 = 4800)
    const mockEvent = {
      target: { scrollTop: 4800 }
    } as unknown as Event

    onScroll(mockEvent)
    await nextTick()

    // rawStart = floor(4800/48) = 100
    // visibleStart = max(0, 100 - 5) = 95
    expect(visibleStart.value).toBe(95)
    // visibleEnd = min(1000, 100 + 10 + 5) = 115
    expect(visibleEnd.value).toBe(115)
    // offsetY = 95 * 48 = 4560
    expect(offsetY.value).toBe(95 * 48)
  })

  it('should clamp visibleStart to 0 when near top', () => {
    const opts = defaultOptions()
    const { onScroll, visibleStart } = useVirtualScroll(opts)

    // Scroll to row 2 (scrollTop = 96)
    onScroll({ target: { scrollTop: 96 } } as unknown as Event)

    // rawStart = floor(96/48) = 2
    // visibleStart = max(0, 2 - 5) = 0
    expect(visibleStart.value).toBe(0)
  })

  it('should clamp visibleEnd to itemCount when near bottom', () => {
    const opts = {
      itemCount: ref(20),
      itemHeight: 48,
      bufferSize: 5,
      containerHeight: ref(480)
    }
    const { onScroll, visibleEnd } = useVirtualScroll(opts)

    // Scroll to near bottom
    onScroll({ target: { scrollTop: 15 * 48 } } as unknown as Event)

    // rawEnd = 15 + 10 + 5 = 30, clamped to 20
    expect(visibleEnd.value).toBe(20)
  })

  it('should render far fewer DOM nodes than total items', () => {
    const opts = defaultOptions()
    const { visibleRange } = useVirtualScroll(opts)

    // With 1000 items, only ~20 (10 visible + 2*5 buffer) should be rendered
    expect(visibleRange.value.length).toBeLessThan(opts.itemCount.value)
    expect(visibleRange.value.length).toBeLessThanOrEqual(20)
  })

  it('should handle empty data', () => {
    const opts = {
      itemCount: ref(0),
      itemHeight: 48,
      bufferSize: 5,
      containerHeight: ref(480)
    }
    const { totalHeight, visibleRange, visibleStart, visibleEnd } = useVirtualScroll(opts)

    expect(totalHeight.value).toBe(0)
    expect(visibleRange.value).toHaveLength(0)
    expect(visibleStart.value).toBe(0)
    expect(visibleEnd.value).toBe(0)
  })

  it('should scrollToIndex correctly', () => {
    const opts = defaultOptions()
    const { containerRef, scrollToIndex, scrollTop } = useVirtualScroll(opts)

    // Create a mock element
    const mockEl = { scrollTop: 0, clientHeight: 480 } as HTMLElement
    ;(containerRef as any).value = mockEl

    scrollToIndex(50)
    expect(mockEl.scrollTop).toBe(50 * 48)
    expect(scrollTop.value).toBe(50 * 48)
  })

  it('should scrollToTop correctly', () => {
    const opts = defaultOptions()
    const { containerRef, scrollToTop, scrollTop } = useVirtualScroll(opts)

    const mockEl = { scrollTop: 5000, clientHeight: 480 } as HTMLElement
    ;(containerRef as any).value = mockEl

    scrollToTop()
    expect(mockEl.scrollTop).toBe(0)
    expect(scrollTop.value).toBe(0)
  })

  it('should trigger onLoadMore when scrolled near bottom', async () => {
    const loadMore = vi.fn().mockResolvedValue(undefined)
    const opts = {
      itemCount: ref(100),
      itemHeight: 48,
      bufferSize: 5,
      containerHeight: ref(480),
      onLoadMore: loadMore,
      loadMoreThreshold: 200
    }
    const { onScroll } = useVirtualScroll(opts)

    // Total height = 100 * 48 = 4800
    // Scroll near bottom: scrollTop = 4800 - 480 - 100 = 4220
    // distanceToBottom = 4800 - 4220 - 480 = 100 < 200
    onScroll({ target: { scrollTop: 4220 } } as unknown as Event)

    expect(loadMore).toHaveBeenCalledTimes(1)
  })

  it('should not trigger onLoadMore when far from bottom', () => {
    const loadMore = vi.fn().mockResolvedValue(undefined)
    const opts = {
      itemCount: ref(100),
      itemHeight: 48,
      bufferSize: 5,
      containerHeight: ref(480),
      onLoadMore: loadMore,
      loadMoreThreshold: 200
    }
    const { onScroll } = useVirtualScroll(opts)

    // Scroll to top area
    onScroll({ target: { scrollTop: 100 } } as unknown as Event)

    expect(loadMore).not.toHaveBeenCalled()
  })

  it('should react to itemCount changes', async () => {
    const itemCount = ref(100)
    const opts = {
      itemCount,
      itemHeight: 48,
      bufferSize: 5,
      containerHeight: ref(480)
    }
    const { totalHeight } = useVirtualScroll(opts)

    expect(totalHeight.value).toBe(100 * 48)

    itemCount.value = 500
    await nextTick()

    expect(totalHeight.value).toBe(500 * 48)
  })

  it('should support numeric containerHeight', () => {
    const opts = {
      itemCount: ref(100),
      itemHeight: 48,
      bufferSize: 5,
      containerHeight: 600
    }
    const { visibleEnd } = useVirtualScroll(opts)

    // 600/48 = 12.5 => ceil = 13 visible
    // visibleEnd = min(100, 0 + 13 + 5) = 18
    expect(visibleEnd.value).toBe(18)
  })
})
