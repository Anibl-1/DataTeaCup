/**
 * Unit tests and Property-Based Tests for countAnimation.ts
 * Validates: Requirement 2.6 - 统计数字使用动画计数效果，从 0 平滑过渡到目标值
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { ref, nextTick } from 'vue'
import * as fc from 'fast-check'
import { useCountAnimation } from '../countAnimation'

describe('useCountAnimation', () => {
  let rafCallbacks: Array<(time: number) => void> = []
  let currentTime = 0

  beforeEach(() => {
    rafCallbacks = []
    currentTime = 0
    vi.spyOn(performance, 'now').mockImplementation(() => currentTime)
    vi.spyOn(window, 'requestAnimationFrame').mockImplementation((cb: FrameRequestCallback) => {
      rafCallbacks.push(cb as (time: number) => void)
      return rafCallbacks.length
    })
    vi.spyOn(window, 'cancelAnimationFrame').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function flushFrame(time: number) {
    currentTime = time
    const pending = [...rafCallbacks]
    rafCallbacks = []
    for (const cb of pending) {
      cb(time)
    }
  }

  function collectAnimationValues(duration: number): number[] {
    const values: number[] = []
    // Collect values at many intermediate steps
    const stepCount = 20
    for (let i = 1; i <= stepCount; i++) {
      const time = (duration * i) / stepCount
      flushFrame(time)
      values.push(rafCallbacks.length > 0 || i === stepCount ? 0 : 0) // placeholder
    }
    return values
  }

  it('should start from 0', () => {
    const target = ref(500)
    const current = useCountAnimation(target, 800)
    // Before any animation frame runs, value should be 0
    expect(current.value).toBe(0)
  })

  it('should reach the target value after animation completes', () => {
    const target = ref(1000)
    const current = useCountAnimation(target, 800)

    // Run animation to completion
    for (const fraction of [0.25, 0.5, 0.75, 1.0, 1.1]) {
      flushFrame(800 * fraction)
    }

    expect(current.value).toBe(1000)
  })

  it('should produce monotonically increasing values during animation', () => {
    const target = ref(100)
    const current = useCountAnimation(target, 800)

    const values: number[] = [current.value] // starts at 0

    // Collect values at many intermediate steps
    for (let i = 1; i <= 40; i++) {
      flushFrame((800 * i) / 40)
      values.push(current.value)
    }

    // Verify monotonic increase: each value >= previous value
    for (let i = 1; i < values.length; i++) {
      expect(values[i]).toBeGreaterThanOrEqual(values[i - 1])
    }

    // Final value should be the target
    expect(values[values.length - 1]).toBe(100)
  })

  it('should always restart from 0 when target changes', async () => {
    const target = ref(500)
    const current = useCountAnimation(target, 800)

    // Run animation halfway
    flushFrame(400)
    const midValue = current.value
    expect(midValue).toBeGreaterThan(0)

    // Change target — watcher is async, need to flush
    target.value = 1000
    await nextTick()
    expect(current.value).toBe(0)
  })

  it('should handle target value of 0', () => {
    const target = ref(0)
    const current = useCountAnimation(target, 800)
    expect(current.value).toBe(0)
  })

  it('should handle duration of 0 by setting value immediately', () => {
    const target = ref(999)
    const current = useCountAnimation(target, 0)
    expect(current.value).toBe(999)
  })

  it('should clamp negative targets to 0', () => {
    const target = ref(-10)
    const current = useCountAnimation(target, 800)
    expect(current.value).toBe(0)
  })
})


// ==================== Property-Based Tests ====================

/**
 * Property 5: 统计数字动画从零开始
 * Feature: page-audit-optimization, Property 5
 *
 * **Validates: Requirements 2.6**
 *
 * For any 非负整数目标值 n，countAnimation 函数应从 0 开始，最终到达 n，且中间值单调递增。
 */
describe('Property 5: countAnimation monotonically increases to target', () => {
  let rafCallbacks: Array<(time: number) => void> = []
  let currentTime = 0

  beforeEach(() => {
    rafCallbacks = []
    currentTime = 0
    vi.spyOn(performance, 'now').mockImplementation(() => currentTime)
    vi.spyOn(window, 'requestAnimationFrame').mockImplementation((cb: FrameRequestCallback) => {
      rafCallbacks.push(cb as (time: number) => void)
      return rafCallbacks.length
    })
    vi.spyOn(window, 'cancelAnimationFrame').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function flushFrame(time: number) {
    currentTime = time
    const pending = [...rafCallbacks]
    rafCallbacks = []
    for (const cb of pending) {
      cb(time)
    }
  }

  it('should start from 0, monotonically increase, and reach target for any non-negative integer', () => {
    fc.assert(
      fc.property(fc.nat({ max: 10000 }), (target) => {
        // Reset mocks for each iteration
        rafCallbacks = []
        currentTime = 0

        const duration = 800
        const targetRef = ref(target)
        const current = useCountAnimation(targetRef, duration)

        // Property: starts from 0
        expect(current.value).toBe(0)

        // Collect values through animation
        const values: number[] = [current.value]
        const steps = 20
        for (let i = 1; i <= steps; i++) {
          flushFrame((duration * i) / steps)
          values.push(current.value)
        }
        // Flush one more frame past duration to ensure completion
        flushFrame(duration + 100)
        values.push(current.value)

        // Property: monotonically increasing
        for (let i = 1; i < values.length; i++) {
          expect(values[i]).toBeGreaterThanOrEqual(values[i - 1])
        }

        // Property: final value equals target
        expect(values[values.length - 1]).toBe(target)
      }),
      { numRuns: 100 }
    )
  })
})
