/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 虚拟滚动属性测试
 * Virtual Scroll Property-Based Tests
 *
 * **属性 44: 虚拟滚动 DOM 节点限制**
 * **Validates: Requirements 15.1**
 *
 * 测试内容:
 * 对于任意超过 100 行的数据集，渲染的 DOM 节点数应远小于数据总行数，
 * 仅渲染可视区域及缓冲区的行。具体来说，可见范围应被限制在
 * (visibleCount + 2 * bufferSize) 以内。
 */

import { describe, it, expect, vi } from 'vitest'
import * as fc from 'fast-check'
import { ref } from 'vue'
import { useVirtualScroll } from '../useVirtualScroll'

// Mock lifecycle hooks since we're testing outside component context
vi.mock('vue', async () => {
  const actual = await vi.importActual('vue')
  return {
    ...(actual as any),
    onMounted: vi.fn((cb: () => void) => cb()),
    onBeforeUnmount: vi.fn(),
  }
})

// Mock ResizeObserver
class MockResizeObserver {
  observe = vi.fn()
  unobserve = vi.fn()
  disconnect = vi.fn()
}
vi.stubGlobal('ResizeObserver', MockResizeObserver)

// Mock requestAnimationFrame to execute synchronously
vi.stubGlobal('requestAnimationFrame', (cb: FrameRequestCallback) => {
  cb(0)
  return 1
})
vi.stubGlobal('cancelAnimationFrame', vi.fn())

// ============================================================================
// Arbitraries (测试数据生成器)
// ============================================================================

/** 数据行数：超过 100 行（需求 15.1 的触发条件） */
const itemCountArb = fc.integer({ min: 101, max: 100_000 })

/** 行高（px）：合理的行高范围 */
const itemHeightArb = fc.integer({ min: 20, max: 120 })

/** 缓冲区大小：合理的缓冲行数 */
const bufferSizeArb = fc.integer({ min: 0, max: 20 })

/** 容器高度（px）：合理的视口高度 */
const containerHeightArb = fc.integer({ min: 100, max: 2000 })

/** 生成完整的虚拟滚动配置 */
const virtualScrollConfigArb = fc.record({
  itemCount: itemCountArb,
  itemHeight: itemHeightArb,
  bufferSize: bufferSizeArb,
  containerHeight: containerHeightArb,
})

// ============================================================================
// 属性 44: 虚拟滚动 DOM 节点限制
// ============================================================================

describe('属性 44: 虚拟滚动 DOM 节点限制', () => {
  it('visibleRange 长度应被 (visibleCount + 2 * bufferSize) 限制（初始位置）', () => {
    fc.assert(
      fc.property(virtualScrollConfigArb, (config) => {
        const { visibleRange } = useVirtualScroll({
          itemCount: ref(config.itemCount),
          itemHeight: config.itemHeight,
          bufferSize: config.bufferSize,
          containerHeight: ref(config.containerHeight),
        })

        const visibleCount = Math.ceil(config.containerHeight / config.itemHeight)
        const maxRendered = visibleCount + 2 * config.bufferSize

        // Core property: rendered nodes bounded by min(itemCount, visibleCount + 2*bufferSize)
        const expectedMax = Math.min(config.itemCount, maxRendered)
        expect(visibleRange.value.length).toBeLessThanOrEqual(expectedMax)

        // When container can't show all items, rendered count is significantly less than total
        if (maxRendered < config.itemCount) {
          expect(visibleRange.value.length).toBeLessThan(config.itemCount)
        }
      }),
      { numRuns: 200 },
    )
  })

  it('visibleRange 长度应被 (visibleCount + 2 * bufferSize) 限制（任意滚动位置）', () => {
    fc.assert(
      fc.property(
        virtualScrollConfigArb,
        fc.double({ min: 0, max: 1, noNaN: true, noDefaultInfinity: true }),
        (config, scrollFraction) => {
          const { visibleRange, onScroll } = useVirtualScroll({
            itemCount: ref(config.itemCount),
            itemHeight: config.itemHeight,
            bufferSize: config.bufferSize,
            containerHeight: ref(config.containerHeight),
          })

          // Scroll to a random position within the total scrollable area
          const totalHeight = config.itemCount * config.itemHeight
          const maxScrollTop = Math.max(0, totalHeight - config.containerHeight)
          const scrollTop = Math.floor(scrollFraction * maxScrollTop)

          onScroll({ target: { scrollTop } } as unknown as Event)

          const visibleCount = Math.ceil(config.containerHeight / config.itemHeight)
          const maxRendered = visibleCount + 2 * config.bufferSize
          const expectedMax = Math.min(config.itemCount, maxRendered)

          expect(visibleRange.value.length).toBeLessThanOrEqual(expectedMax)

          if (maxRendered < config.itemCount) {
            expect(visibleRange.value.length).toBeLessThan(config.itemCount)
          }
        },
      ),
      { numRuns: 300 },
    )
  })

  it('visibleRange 中的索引应在 [0, itemCount) 范围内', () => {
    fc.assert(
      fc.property(
        virtualScrollConfigArb,
        fc.double({ min: 0, max: 1, noNaN: true, noDefaultInfinity: true }),
        (config, scrollFraction) => {
          const { visibleRange, onScroll } = useVirtualScroll({
            itemCount: ref(config.itemCount),
            itemHeight: config.itemHeight,
            bufferSize: config.bufferSize,
            containerHeight: ref(config.containerHeight),
          })

          const totalHeight = config.itemCount * config.itemHeight
          const maxScrollTop = Math.max(0, totalHeight - config.containerHeight)
          const scrollTop = Math.floor(scrollFraction * maxScrollTop)

          onScroll({ target: { scrollTop } } as unknown as Event)

          for (const idx of visibleRange.value) {
            expect(idx).toBeGreaterThanOrEqual(0)
            expect(idx).toBeLessThan(config.itemCount)
          }
        },
      ),
      { numRuns: 200 },
    )
  })

  it('visibleRange 中的索引应连续递增', () => {
    fc.assert(
      fc.property(
        virtualScrollConfigArb,
        fc.double({ min: 0, max: 1, noNaN: true, noDefaultInfinity: true }),
        (config, scrollFraction) => {
          const { visibleRange, onScroll } = useVirtualScroll({
            itemCount: ref(config.itemCount),
            itemHeight: config.itemHeight,
            bufferSize: config.bufferSize,
            containerHeight: ref(config.containerHeight),
          })

          const totalHeight = config.itemCount * config.itemHeight
          const maxScrollTop = Math.max(0, totalHeight - config.containerHeight)
          const scrollTop = Math.floor(scrollFraction * maxScrollTop)

          onScroll({ target: { scrollTop } } as unknown as Event)

          const range = visibleRange.value
          for (let i = 1; i < range.length; i++) {
            expect(range[i]!).toBe(range[i - 1]! + 1)
          }
        },
      ),
      { numRuns: 200 },
    )
  })

  it('totalHeight 应等于 itemCount * itemHeight', () => {
    fc.assert(
      fc.property(virtualScrollConfigArb, (config) => {
        const { totalHeight } = useVirtualScroll({
          itemCount: ref(config.itemCount),
          itemHeight: config.itemHeight,
          bufferSize: config.bufferSize,
          containerHeight: ref(config.containerHeight),
        })

        expect(totalHeight.value).toBe(config.itemCount * config.itemHeight)
      }),
      { numRuns: 200 },
    )
  })
})
